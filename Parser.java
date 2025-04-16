import java.util.ArrayList;

public class Parser{
    static int tok_idx = 0;
    static ArrayList<Token> tokens = new ArrayList<>();

    Parser(ArrayList<Token> tokens){
        Parser.tokens = tokens;
        parse();
    }

    void parse(){

        tok_idx = 0;
        Node ast = new Node();
        
        //Check if there's variable declaration and assignment:
        if(tokens.get(tok_idx).type == TokenType.LET){
            tok_idx ++;
            if(tok_idx >= tokens.size()){Language.error("Expected an identifier after 'let' !!!");}
            //Find identfier token:
            if(tokens.get(tok_idx).type == TokenType.IDENTIFIER){
                Token identifier_token = tokens.get(tok_idx);
                //Make sure it's not already declared:
                if(Language.variables.containsKey(identifier_token.value)){Language.error(identifier_token.value + " Has already been declared before !");}
                //Find 'equals' sign:
                tok_idx ++;
                if(tokens.get(tok_idx).type == TokenType.EQUALS){
                    tok_idx ++;
                    ast = new VarAssignmentNode(identifier_token.value.toString(), boolean_expression());
                }
                //We don't find equal sign:
                else {
                    Language.error("Expected '=' after identfier !!!");
                }
            }
                
            //We don't find an identfier which is an Language.error:
            else {
                Language.error("Expected identfier after 'let' !!!");
            }
        }

        //Check if there's variable re-asignment:
        else if(tokens.get(tok_idx).type == TokenType.IDENTIFIER){
            Token identifier_token = tokens.get(tok_idx);
            //Check if variable has been declared before:
            if(!Language.variables.containsKey(identifier_token.value)){Language.error(identifier_token.value + " must be declared with 'let' before using it !!!"); return;}
            //Find 'equals' sign:
            tok_idx ++;
            //We reached the end of the statement so we just wanna print the variable:
            if(tok_idx >= tokens.size()) {System.out.println(Language.variables.get(identifier_token.value)); return;}
            if(tokens.get(tok_idx).type == TokenType.EQUALS){
                tok_idx ++;
                ast = new VarAssignmentNode(identifier_token.value.toString(), boolean_expression());
            }
            //We don't find equal sign, We're just accsesing:
            else {
                tok_idx --;
                ast = boolean_expression();
            }
        }

        // Check if there's an if statement:
        else if(tokens.get(tok_idx).type == TokenType.IF){
            tok_idx ++;
            // Create and store our "if" condition:
            Node condition = boolean_expression();
            // Check if there's a colon ":":
            if(tokens.get(tok_idx).type == TokenType.EOS){
                tok_idx ++;
                // Colon found, Now we create all our if branches:
                ArrayList<IfBranch> branchs = new ArrayList<>();
                // Keep creating branches until we're done:
                while (tokens.get(tok_idx).type != TokenType.IF) {
                    // Create list for the statements in this branch:
                    ArrayList<Node> statements_in_body = new ArrayList<>();
                    while (tokens.get(tok_idx).type != TokenType.ELIF && tokens.get(tok_idx).type != TokenType.IF) {
                        statements_in_body.add(boolean_expression());
                    }
                    // Create our 1st branch:
                    IfBranch branch = new IfBranch(statements_in_body);
                    // Add this branch
                    branchs.add(branch);
            }

                
            }
            // No colon, Language.Error:
            else{

            }

        }

        
        else 
            ast = boolean_expression();

        }

///Helper parsing methods///
Node factor(){
    //Making number nodes:
    if(tokens.get(tok_idx).type == TokenType.Double){
        return new NumberNode(tokens.get(tok_idx), new Double(tokens.get(tok_idx).value.toString()));
    }

    //Making variable access nodes:
    else if(tokens.get(tok_idx).type == TokenType.IDENTIFIER){
        Token identfier_token = tokens.get(tok_idx);
        //Check if variable has been declared and can be accessed:
        if(Language.variables.containsKey(identfier_token.value)){
            return new VarAccessNode(identfier_token.value.toString());
        }
        //Variable doesn't exist !
        else{
            Language.error("Variable: "+ tokens.get(tok_idx).value + " not declared !!!");
            return null;
        } 
    }

    //Making unary opperator nodes:
    else if(tokens.get(tok_idx).type == TokenType.MINUS){

        UnaryOpNode unary_op = null;

        while (tokens.get(tok_idx).type == TokenType.MINUS) {
            Token op_Token = tokens.get(tok_idx);
            tok_idx ++;
            if(tok_idx >= tokens.size()){ Language.error("Invalid syntax !!!"); break;}
            Node child_node = factor();
            unary_op = new UnaryOpNode(child_node, op_Token);
            if (child_node == null){ // Check if the input is "--"
                Language.error("Expected a number after \"-\" !!!");
                return null;
            }
            if(child_node.token.type == TokenType.Double){
                tok_idx ++;
                break;
            }
            tok_idx ++; // Should i increment here ? Yes.
            if(tok_idx >= tokens.size()) return unary_op;
        }
        tok_idx--;
        return unary_op;
    }

    //Handling parentheses:
    else if(tokens.get(tok_idx).type == TokenType.L_PAR){
        tok_idx ++;
        if(tok_idx >= tokens.size()){ Language.error("parenthesis never closed !"); return null;}
        Node expr_inside_parentheses = boolean_expression();
        if(tok_idx >= tokens.size()){ Language.error("parenthesis never closed !"); return null;}
        if(tokens.get(tok_idx).type == TokenType.R_PAR){
            return expr_inside_parentheses;}
        else{
            Language.error("parenthesis never closed !");
            return null;}
    }

    else {
        Token tok = tokens.get(tok_idx); // Debugging.
        Language.error(tokens.get(tok_idx).type +  " is not a number !!!");
        return null;
    }
}

Node term(){
    Node left = factor();
    tok_idx ++;
    if(tok_idx >= tokens.size()) return left;

    while (tokens.get(tok_idx).type == TokenType.MULTIPLY || tokens.get(tok_idx).type == TokenType.DIVIDE || tokens.get(tok_idx).type == TokenType.POWER) {
        Token op_token = tokens.get(tok_idx);
        tok_idx ++;
        if(tok_idx >= tokens.size()){ Language.error("Invalid syntax !!!"); break;}
        Node right = factor();
        tok_idx ++;
        left = new BinOpNode(left, op_token, right);
        if(tok_idx >= tokens.size()) return left;
    }
    return left;
}

Node arithmetic_expresion(){
    Node left = term();
    if(tok_idx >= tokens.size()) return left;

    while (tokens.get(tok_idx).type == TokenType.PLUS || tokens.get(tok_idx).type == TokenType.MINUS) {
        Token op_token = tokens.get(tok_idx);
        tok_idx ++;
        if(tok_idx >= tokens.size()){ Language.error("Invalid syntax !!!"); return null;}
        Node right = term();
        left = new BinOpNode(left, op_token, right);
        if(tok_idx >= tokens.size()) return left;
    }
    return left;
}

Node comparison_expression(){
    Node left = arithmetic_expresion();
    if(tok_idx >= tokens.size()) return left;

    while (tokens.get(tok_idx).type == TokenType.GREATER_THAN || tokens.get(tok_idx).type == TokenType.LESS_THAN
    || tokens.get(tok_idx).type == TokenType.GREATER_THAN_OR_EQUAL || tokens.get(tok_idx).type == TokenType.LESS_THAN_OR_EQUAL
    ||tokens.get(tok_idx).type == TokenType.DOUBLE_EQUAL || tokens.get(tok_idx).type == TokenType.NOT_EQUAL) {
        Token op_token = tokens.get(tok_idx);
        tok_idx ++;
        if(tok_idx >= tokens.size()){ Language.error("Invalid syntax !!!"); break;}
        Node right = arithmetic_expresion();
        left = new BinOpNode(left, op_token, right);
        if(tok_idx >= tokens.size()) return left;
    }
    return left;
}

Node boolean_expression(){
    Node left = comparison_expression();
    if(tok_idx >= tokens.size()) return left;

    while (tokens.get(tok_idx).type == TokenType.AND || tokens.get(tok_idx).type == TokenType.OR) {
        Token op_token = tokens.get(tok_idx);
        tok_idx ++;
        if(tok_idx >= tokens.size()){ Language.error("Invalid syntax !!!"); break;}
        Node right = comparison_expression();
        left = new BinOpNode(left, op_token, right);
        if(tok_idx >= tokens.size()) return left;
    }
    return left;
}

}