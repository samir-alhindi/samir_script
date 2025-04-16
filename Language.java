import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

class Language {

    static String source;
    static HashMap<String, Double> variables = new HashMap<>();

    static int pos = 0;
    static char current;

    static ArrayList<Token> tokens = new ArrayList<>();
    static int tok_idx = 0;

    static final String[] keywords = {"let", "and", "or", "if", "elif", "else", "endif"};

    Language(){

    }

    Language(String source_code){
        Language.source = source_code;
    }

    //// running ////
   
    void file(String file_path){
        try {
            FileReader reader = new FileReader(file_path);
            byte[] bytes = Files.readAllBytes(Paths.get(file_path));
            source = new String(bytes, Charset.defaultCharset());
            run();
        }
        catch(FileNotFoundException e){

        }
        catch(IOException e){

        }
        
    }

    void run (){

        //Check if input is nothing:
        if(source.length() == 0)
            return;

        ///Tokenization///

        tokens.clear();
        current = source.charAt(0);

        while (current != '\0') {

            //Check if char is space or tab or newline:
            if(isNewlineChar(current) || current == ' ' || current == '\t')
                advance();

            //Check if char is a semicolon (end of statement):
            else if(current == ';'){
                addToken(TokenType.EOS);
                advance();
            }

            //Check if char is comment:
            else if(current == '#'){
                while ( ! isNewlineChar(current)) {
                    advance();
                }
            }
                
        
            //Check if char is digit:
            else if(Character.isDigit(current)){

                //Creating Double token:
                String num_str = "";
                boolean found_dot = false;
            
                while (Character.isDigit(current) || current == '.') {
                    if(Character.isDigit(current)){
                        num_str += current;
                        advance();
                    }
                    else if(current == '.' && ! found_dot){
                        found_dot = true;
                        num_str += current;
                        advance();
                    }
                    //Error cases:
                    else if(current == '.' && found_dot){
                        error("Number can't have more than 1 dot !!!");
                        return;
                    }
                }
                Double num_val = Double.valueOf(num_str);
                addToken(TokenType.Double, num_val);
        }
        
            //Check if char is opperator:

            else if(current == '+'){
                addToken(TokenType.PLUS);
                advance();}
            else if(current == '-'){
                addToken(TokenType.MINUS);
                advance();}
            else if(current == '*'){
                addToken(TokenType.MULTIPLY);
                advance();}
            else if(current == '/'){
                addToken(TokenType.DIVIDE);
                advance();}
            else if(current == '^'){
                addToken(TokenType.POWER);
                advance();}

        //check if char is equals sign:
        else if(current == '='){
            //Check if '==':
            if(currentFollowedBy('=')){
                addToken(TokenType.DOUBLE_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.EQUALS);
                advance();
            }
        }

        //Check if char is '!' equals sign:
        else if(current == '!'){
            if(currentFollowedBy('!')){
                addToken(TokenType.NOT_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.NOT);
                advance();
            }
        }
        
        //Check if char is '>' than:
        else if(current == '>'){
            if(currentFollowedBy('=')){
                addToken(TokenType.GREATER_THAN_OR_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.GREATER_THAN);
                advance();
            }
        }

        //Check if char is '<' than:
        else if(current == '<'){
            if(currentFollowedBy('=')){
                addToken(TokenType.LESS_THAN_OR_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.LESS_THAN);
                advance();
            }
        }

        //Check if char is parentheses:
        else if (current == '('){
            addToken(TokenType.L_PAR);
            advance();
        }
        else if (current == ')'){
            addToken(TokenType.R_PAR);
            advance();
        }

        //Check if char is curly braces:
        else if (current == '{'){
            addToken(TokenType.L_CUR);
            advance();
        }
        else if (current == '}'){
            addToken(TokenType.R_CUR);
            advance();
        }

        //Check if char is identifier/keyword:
        else if(Character.isLetter(current) || current == '_'){
            String word = "";
        
            while (Character.isLetter(current) || current == '_' || Character.isDigit(current)){
                word += current;
                advance();
            }

            //Check if word is keyword and tokenize it:
            Token token = new Token(null);
            if(is_keyword(word)){
                switch (word) {
                    case "let" -> token.type = TokenType.LET;
                    case "and" -> token.type = TokenType.AND;
                    case "or" -> token.type = TokenType.OR;
                    case "if" -> token.type = TokenType.IF;
                    case "else" -> token.type = TokenType.ELSE;
                    case "elif" -> token.type = TokenType.ELIF;
                    case "not" -> token.type = TokenType.NOT;
                    case "func" -> token.type = TokenType.FUNC;
                }
                tokens.add(token);
            }
            else
                addToken(TokenType.IDENTIFIER, word);
        }

        else{
            error(current + "Is Illegal char !!!");
            return;
        }

        }

        // End of file token:
        addToken(TokenType.EOF);

        System.out.println(tokens);


        ///Parsing///
        tok_idx = 0;
        Node ast = new Node();
        
        //Check if there's variable declaration and assignment:
        if(tokens.get(tok_idx).type == TokenType.LET){
            tok_idx ++;
            if(tok_idx >= tokens.size()){error("Expected an identifier after 'let' !!!");}
            //Find identfier token:
            if(tokens.get(tok_idx).type == TokenType.IDENTIFIER){
                Token identifier_token = tokens.get(tok_idx);
                //Make sure it's not already declared:
                if(variables.containsKey(identifier_token.value)){error(identifier_token.value + " Has already been declared before !");}
                //Find 'equals' sign:
                tok_idx ++;
                if(tokens.get(tok_idx).type == TokenType.EQUALS){
                    tok_idx ++;
                    ast = new VarAssignmentNode(identifier_token.value.toString(), boolean_expression());
                }
                //We don't find equal sign:
                else {
                    error("Expected '=' after identfier !!!");
                }
            }
                
            //We don't find an identfier which is an error:
            else {
                error("Expected identfier after 'let' !!!");
            }
        }

        //Check if there's variable re-asignment:
        else if(tokens.get(tok_idx).type == TokenType.IDENTIFIER){
            Token identifier_token = tokens.get(tok_idx);
            //Check if variable has been declared before:
            if(!variables.containsKey(identifier_token.value)){error(identifier_token.value + " must be declared with 'let' before using it !!!"); return;}
            //Find 'equals' sign:
            tok_idx ++;
            //We reached the end of the statement so we just wanna print the variable:
            if(tok_idx >= tokens.size()) {System.out.println(variables.get(identifier_token.value)); return;}
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
            // No colon, Error:
            else{

            }

        }

        
        else 
            ast = boolean_expression();

        ///Interpreting//
        if(ast != null){
            Double output = ast.visit();
            if(output != null)
                System.out.println(output);
        }
            
    }

    static void error(String log){
        System.out.println("Error: " + log);
    }

    //Helper lexing methods:

    void addToken(TokenType tokenType){
        Token token = new Token(tokenType);
        tokens.add(token);
    }

    void addToken(TokenType tokenType, Object value){
        Token token = new Token(tokenType, value);
        tokens.add(token);
    }

    void advance(){
        // Check if reached end of line:
        if (pos + 1 >= source.length())
            current = '\0';
        else {
            pos += 1;
            current = source.charAt(pos);
        }
    }

    void advanceTwo(){
        // Check if reached end of line:
        if (pos + 2 >= source.length())
            current = '\0';
        else {
            pos += 2;
            current = source.charAt(pos);
        }
    }

    boolean isNewlineChar(char c){
        switch(c){
            case '\r' : return true;
            case '\n' : return true;
            default : return false;
        }
    }

    char peekNext(){
        if(pos + 1 >= source.length())
            return '\0';
        return source.charAt(pos + 1);
    }

    boolean currentFollowedBy(char c){
        if(peekNext() == c)
            return true;
        return false;
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
            if(variables.containsKey(identfier_token.value)){
                return new VarAccessNode(identfier_token.value.toString());
            }
            //Variable doesn't exist !
            else{
                error("Variable: "+ tokens.get(tok_idx).value + " not declared !!!");
                return null;
            } 
        }

        //Making unary opperator nodes:
        else if(tokens.get(tok_idx).type == TokenType.MINUS){

            UnaryOpNode unary_op = null;

            while (tokens.get(tok_idx).type == TokenType.MINUS) {
                Token op_Token = tokens.get(tok_idx);
                tok_idx ++;
                if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); break;}
                Node child_node = factor();
                unary_op = new UnaryOpNode(child_node, op_Token);
                if (child_node == null){ // Check if the input is "--"
                    error("Expected a number after \"-\" !!!");
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
            if(tok_idx >= tokens.size()){ error("parenthesis never closed !"); return null;}
            Node expr_inside_parentheses = boolean_expression();
            if(tok_idx >= tokens.size()){ error("parenthesis never closed !"); return null;}
            if(tokens.get(tok_idx).type == TokenType.R_PAR){
                return expr_inside_parentheses;}
            else{
                error("parenthesis never closed !");
                return null;}
        }

        else {
            Token tok = tokens.get(tok_idx); // Debugging.
            error(tokens.get(tok_idx).type +  " is not a number !!!");
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
            if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); break;}
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
            if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); return null;}
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
            if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); break;}
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
            if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); break;}
            Node right = comparison_expression();
            left = new BinOpNode(left, op_token, right);
            if(tok_idx >= tokens.size()) return left;
        }
        return left;
    }

    //Method to check if word is keyword:
    boolean is_keyword(String word){
        for (int i = 0; i < keywords.length; i++) 
            if(keywords[i] .equals(word)) return true;
        return false;
        
    }
}

class Token {
    TokenType type;
    Object value;
    Token(TokenType type, Object value){
        this.type = type;
        this.value = value;
    }
    //Constructer for things without values:
    Token(TokenType type){
        this.type = type;
    }

    @Override
    public String toString() {
        return  "[" + type + " : " + value + " : " + value + "]";
    }
}

enum TokenType{

    // Data types:
    Double, STRING,

    // Numeric opperators:
    PLUS, MINUS, MULTIPLY, DIVIDE, POWER,

    // Boolean opperators:
    AND, OR, NOT, 

    // Relational opperators:
    DOUBLE_EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN,
    GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, 

    // Grouping:
    L_PAR, R_PAR, L_CUR, R_CUR,
    
    // Keywords:
    LET, IF, ELSE, ELIF, FUNC, WHILE,

    // Other:
    IDENTIFIER, EQUALS, EOS, EOF, COMMA
    }

class Node {
    Token token;
    Double visit(){
        return null;
    }
}

class NumberNode extends Node {
    Double value_;
    NumberNode(Token token, Double value_){
        this.token = token;
        this.value_ = value_;
    }

    Double visit(){
        return this.value_;
    }

    @Override
    public String toString() {
        return ""+value_+"";
    }
}

class BinOpNode extends Node {
    Node left_node;
    Node right_node;
    BinOpNode(Node left_node, Token op_token, Node right_node){
        this.left_node = left_node;
        this.token = op_token;
        this.right_node = right_node;
    }

    Double visit(){
        if(left_node == null || right_node == null) return null;
        return switch (token.type) {
            case TokenType.PLUS -> left_node.visit() + right_node.visit();
            case TokenType.MINUS -> left_node.visit() - right_node.visit();
            case TokenType.MULTIPLY -> left_node.visit() * right_node.visit();
            case TokenType.DIVIDE -> left_node.visit() / right_node.visit();
            case TokenType.POWER -> Math.pow(left_node.visit(), right_node.visit());
            case TokenType.DOUBLE_EQUAL -> left_node.visit().equals(right_node.visit()) ? 1.0 : 0;
            case TokenType.NOT_EQUAL -> left_node.visit().equals(right_node.visit()) ? 0 : 1.0;
            case TokenType.GREATER_THAN -> left_node.visit() > right_node.visit() ? 1.0 : 0;
            case TokenType.LESS_THAN -> left_node.visit() < right_node.visit() ? 1.0 : 0;
            case TokenType.GREATER_THAN_OR_EQUAL -> left_node.visit() >= right_node.visit() ? 1.0 : 0;
            case TokenType.LESS_THAN_OR_EQUAL -> left_node.visit() <= right_node.visit() ? 1.0 : 0;
            case TokenType.AND -> (!left_node.visit().equals(0.0) && !right_node.visit().equals(0.0)) ? 1.0 : 0;
            case TokenType.OR -> (!left_node.visit().equals(0.0) || !right_node.visit().equals(0.0)) ? 1.0 : 0;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return "(" + left_node + ", " + token.type + ", " + right_node + ")";
    }

}

class UnaryOpNode extends Node {
    Node child_node;
    UnaryOpNode(Node child_node, Token op_token){
        this.child_node = child_node;
        this.token = op_token;
        }

    Double visit(){
        if (child_node == null) {Language.error("No number for the minus !!!"); return null;}
        return child_node.visit() * -1;
    }

    @Override
    public String toString() {
        return "(" + child_node + ", " + token.type + ")";
        }
    }

class VarAssignmentNode extends Node {
    String identifier;
    Node expresion;
    Double value_;
    VarAssignmentNode(String identifier, Node expresion){
        this.identifier = identifier;
        this.expresion = expresion;
    }

    Double visit(){
        if (expresion != null){
            value_ = expresion.visit();
            Language.variables.put(identifier, value_);}
        return null;
    }

    @Override
    public String toString() {
        return "(" + identifier + " = " + expresion + ")";
    }
}

class VarAccessNode extends Node {
    String identifier;
    VarAccessNode(String identifier){
        this.identifier = identifier;
    }

    Double visit(){
        if(identifier != null){
            double value_ = Language.variables.get(identifier);
            return value_;
        }
        return null;

    }

    @Override
    public String toString() {
        return "(" + identifier + " : " + Language.variables.get(identifier) + ")";
    }    
}

class IfBranch extends Node {
    Node[] statements;
    IfBranch(ArrayList<Node> statements){
        this.statements = new Node[statements.size()];
        for(int i = 0; i < statements.size(); i++)
            this.statements[i] = statements.get(i);
    }

    
}

class EntireIfStatement extends Node {
    IfBranch[] branchs;
    EntireIfStatement(ArrayList<IfBranch> branchs){
        this.branchs = new IfBranch[branchs.size()];
        for (int i = 0; i < branchs.size(); i++) 
            this.branchs[i] = branchs.get(i);
        
    }
}