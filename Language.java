import java.util.ArrayList;

class Language {

    static ArrayList<Token> tokens = new ArrayList<>();
    static int tok_idx = 0;

    static final String[] keywords = {"var",};

    //// running ////
    static double run (String input){

        ///Tokenization///
        
        tokens.clear();
        
        for (int i = 0; i < input.length(); i++) {
        
        //Check if char is white space:
        if(input.charAt(i) == ' ') continue;
        
        //Check if char is digit:
        else if(Character.isDigit(input.charAt(i))){

            //Creating Double token:
            String num_str = "";
            boolean found_dot = false;
            
            while (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.') {
                if(Character.isDigit(input.charAt(i))){
                    num_str += input.charAt(i);
                    i++;
                    if(i >= input.length()) break;
                }
                else if(input.charAt(i) == '.' && !found_dot){
                    found_dot = true;
                    num_str += input.charAt(i);
                    i++;
                    if(i >= input.length()) break;
                }
                //Error cases:
                else if(input.charAt(i) == '.' && found_dot){
                    error("Number can't have more than 1 dot !!!");
                    return 0.0;
                }
            }
            i--;
            Token token = new Token(TokenType.Double, Double.parseDouble(num_str));
            tokens.add(token);
        }
        
        //Check if char is opperator:
        else if(input.charAt(i) == '+'){
            Token token = new Token(TokenType.PLUS, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '-'){
            Token token = new Token(TokenType.MINUS, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '*'){
            Token token = new Token(TokenType.MULTIPLY, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '/'){
            Token token = new Token(TokenType.DIVIDE, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '^'){
            Token token = new Token(TokenType.POWER, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '='){
            Token token = new Token(TokenType.EQUALS, 0);
            tokens.add(token);
        }

        //Check if char is parentheses:
        else if (input.charAt(i) == '('){
            Token token = new Token(TokenType.L_PAR, 0);
            tokens.add(token);
        }
        else if (input.charAt(i) == ')'){
            Token token = new Token(TokenType.R_PAR, 0);
            tokens.add(token);
        }

        //Check if char is identifier/keyword:
        else if(Character.isLetter(input.charAt(i)) || input.charAt(i) == '_'){
            String word = "";
        
            while (Character.isLetter(input.charAt(i)) || input.charAt(i) == '_'){
                word += input.charAt(i);
                i++;
                if(i >= input.length()) break;
            }

            //Check if word is keyword and tokenize it:
            Token token = new Token(null, null);
            if(is_keyword(word)){
                switch (word) {
                    case "var" -> token.type = TokenType.VAR;
                }
            }
            else
                token = new Token(TokenType.IDENTIFIER, word);
            tokens.add(token);
            
        }

        else {
            error(input.charAt(i) + " is Illegal char !");
            return 0.0;
        }

        }

        System.out.println(tokens);

        ///Parsing///
        tok_idx = 0;
        Node ast = expresion();

        ///Interpreting//
        if(ast != null)
            return ast.visit();
        else
            return 9999999.0;
        
    }

    static void error(String log){
        System.out.println("Error: " + log);
    }

    ///Helper parsing methods///
    static Node factor(){
        //Making number nodes:
        if(tokens.get(tok_idx).type == TokenType.Double){
            return new NumberNode(tokens.get(tok_idx), tokens.get(tok_idx).value);
        }

        //Making unary opperator:
        else if(tokens.get(tok_idx).type == TokenType.MINUS){

            UnaryOpNode unary_op = new UnaryOpNode(null, null);

            while (tokens.get(tok_idx).type == TokenType.MINUS) {
                Token op_Token = tokens.get(tok_idx);
                tok_idx ++;
                if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); break;}
                Node child_node = factor();
                tok_idx ++; // Should i increment here ? Yes.
                unary_op = new UnaryOpNode(child_node, op_Token);
                if(tok_idx >= tokens.size()) return unary_op;
            }
            tok_idx--;
            return unary_op;
        }

        //Handling parentheses:
        else if(tokens.get(tok_idx).type == TokenType.L_PAR){
            tok_idx ++;
            if(tok_idx >= tokens.size()){ error("parenthesis never closed !"); return null;}
            Node expr_inside_parentheses = expresion();
            if(tok_idx >= tokens.size()){ error("parenthesis never closed !"); return null;}
            if(tokens.get(tok_idx).type == TokenType.R_PAR){
                return expr_inside_parentheses;}
            else{
                error("parenthesis never closed !");
                return null;}
        }

        else {
            error(tokens.get(tok_idx).type +  " is not a number !!!");
            return new NumberNode(null, 0.0);
        }
    }

    static Node term(){
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

    static Node expresion(){
        Node left = term();
        if(tok_idx >= tokens.size()) return left;

        while (tokens.get(tok_idx).type == TokenType.PLUS || tokens.get(tok_idx).type == TokenType.MINUS) {
            Token op_token = tokens.get(tok_idx);
            tok_idx ++;
            if(tok_idx >= tokens.size()){ error("Invalid syntax !!!"); break;}
            Node right = term();
            left = new BinOpNode(left, op_token, right);
            if(tok_idx >= tokens.size()) return left;
        }
        return left;
    }

    //Method to check if word is keyword:
    static boolean is_keyword(String word){
        for (int i = 0; i < keywords.length; i++) 
            if(keywords[i] .equals(word)) return true;
        return false;
        
    }
}

class Token {
    TokenType type;
    double value;
    String word = "";
    Token(TokenType type, double value){
        this.type = type;
        this.value = value;
    }
    //Constructer for identifiers:
    Token(TokenType type, String word){
        this.type = type;
        this.word = word;
    }

    @Override
    public String toString() {
        return  "[" + type + " : " + value + " : " + word + "]";
    }
}

enum TokenType{Double, PLUS, MINUS, MULTIPLY, DIVIDE, POWER, L_PAR, R_PAR, VAR, IDENTIFIER, EQUALS}

class Node {
    Token token;
    double visit(){
        return 999999999.0;
    }
}

class NumberNode extends Node {
    double value_;
    NumberNode(Token token, double value_){
        this.token = token;
        this.value_ = value_;
    }

    double visit(){
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

    double visit(){
        return switch (token.type) {
            case TokenType.PLUS -> left_node.visit() + right_node.visit();
            case TokenType.MINUS -> left_node.visit() - right_node.visit();
            case TokenType.MULTIPLY -> left_node.visit() * right_node.visit();
            case TokenType.DIVIDE -> left_node.visit() / right_node.visit();
            case TokenType.POWER -> Math.pow(left_node.visit(), right_node.visit());
            default -> 0.0;
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

    double visit(){
        if (child_node == null) {Language.error("No number for the minus !!!"); return 0.0;}
        return child_node.visit() * -1;
    }

    @Override
    public String toString() {
        return "(" + child_node + ", " + token.type + ")";
        }
    }