import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

class Language {

    static HashMap<String, Double> variables = new HashMap<>();

    static ArrayList<Token> tokens = new ArrayList<>();
    static int tok_idx = 0;

    static final String[] keywords = {"let",};

    //// running ////
    static void run (String input){

        //Check if we're running an external file:
        if(input.length() >= 5){
            if(input.substring(input.length() - 4, input.length()).equals(".txt")){
                try {
                    String code = "";
                    FileReader reader = new FileReader(input);
                    int data = reader.read();
                    while (data != -1) {
                        code += (char) data;
                        data = reader.read();
                    }
                    ArrayList<String> statements = new ArrayList<>();
                    String a_statement = "";
                    for (int i = 0; i < code.length(); i++) {
                        if(code.charAt(i) != ';'){
                            a_statement += code.charAt(i);
                        }
                        else{
                            run(a_statement);
                            a_statement = "";
                            i += 2;
                        }
                    }
                    return;
                }
                catch (FileNotFoundException e) {
                    error(input + " not found !!!");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

        ///Tokenization///
        
        tokens.clear();
        
        for (int i = 0; i < input.length(); i++) {
        
        //Check if char is white space:
        if(input.charAt(i) == ' ') continue;

        //Check if char is a semicolon (end of statement):
        if(input.charAt(i) == ';') break;
        
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
                    case "let" -> token.type = TokenType.LET;
                }
            }
            else
                token = new Token(TokenType.IDENTIFIER, word);
            tokens.add(token);
            
        }

        else {
            error(input.charAt(i) + " is Illegal char !");
            
        }

        }

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
                if(variables.containsKey(identifier_token.word)){error(identifier_token.word + " Has already been declared before !");}
                //Find 'equals' sign:
                tok_idx ++;
                if(tokens.get(tok_idx).type == TokenType.EQUALS){
                    tok_idx ++;
                    ast = new VarAssignmentNode(identifier_token.word, expresion());
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
            if(!variables.containsKey(identifier_token.word)){error(identifier_token.word + " must be declared with 'let' before using it !!!");}
            //Find 'equals' sign:
            tok_idx ++;
            //We reached the end of the statement so we just wanna print the variable:
            if(tok_idx >= tokens.size()) {System.out.println(variables.get(identifier_token.word)); return;}
            if(tokens.get(tok_idx).type == TokenType.EQUALS){
                tok_idx ++;
                ast = new VarAssignmentNode(identifier_token.word, expresion());
            }
            //We don't find equal sign, We're just accsesing:
            else {
                tok_idx --;
                ast = expresion();
            }
        }

        //No variable asignment:
        else 
            ast = expresion();
        
        ///Interpreting//
        if(ast != null)
            System.out.println(ast.visit());
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

        //Making variable access nodes:
        else if(tokens.get(tok_idx).type == TokenType.IDENTIFIER){
            Token identfier_token = tokens.get(tok_idx);
            //Check if variable has been declared and can be accessed:
            if(variables.containsKey(identfier_token.word)){
                return new VarAccessNode(identfier_token.word);
            }
            //Variable doesn't exist !
            else{
                error("Variable: "+ tokens.get(tok_idx).word + " not declared !!!");
                return new VarAccessNode(null);
            } 
        }

        //Making unary opperator nodes:
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
            return null;
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

enum TokenType{Double, PLUS, MINUS, MULTIPLY, DIVIDE, POWER, L_PAR, R_PAR, LET, IDENTIFIER, EQUALS}

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
    double value_;
    VarAssignmentNode(String identifier, Node expresion){
        this.identifier = identifier;
        this.expresion = expresion;
    }

    Double visit(){
        value_ = expresion.visit();
        Language.variables.put(identifier, value_);
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