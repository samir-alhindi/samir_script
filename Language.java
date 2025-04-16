import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class Language {

    static HashMap<String, Double> variables = new HashMap<>();

    void run(String file_path){
        try {
            FileReader reader = new FileReader(file_path);
            byte[] bytes = Files.readAllBytes(Paths.get(file_path));
            String source = new String(bytes, Charset.defaultCharset());
            Lexer lexer = new Lexer(source);
            //Parser parser = new Parser(Lexer.tokens);
            
            
        }
        catch(FileNotFoundException e){

        }
        catch(IOException e){

        }
        
    }

    static void error(String log){
        System.out.println("Error: " + log);
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
        return  "[" + type + " : " + value + "]";
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