import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Main {
    public static void main(String[] args) {

        Language lang = new Language();

        //Testing
        lang.run("blocks.smr");

    }
}

class Language {

    static Environment environment = new Environment();

    void run(String file_path){
        try {
            FileReader reader = new FileReader(file_path);
            byte[] bytes = Files.readAllBytes(Paths.get(file_path));
            String source = new String(bytes, Charset.defaultCharset());
            Lexer lexer = new Lexer(source);
            ArrayList<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens);
            List<Stmt> program = parser.parse();
            for (Stmt stmt : program) {
                stmt.visit();
            }

            reader.close();
        
        }
        catch(FileNotFoundException e){
        }
        catch(IOException e){
        }
        
    }

    static String stringify(Object object) {
        if (object == null) return "nil";
    
        if (object instanceof Double) {
          String text = object.toString();
          if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
          }
          return text;
        }
    
        return object.toString();
      }

    static void error(String log, int line){
        System.out.printf("Error at line %d: %s", line + 1, log);
        System.exit(1);
    }
            
    }

class Environment {

    HashMap <String, Object> variables = new HashMap<>();
    Environment outer;

    Environment(){
        outer = null;
    }

    Environment(Environment outerEnvironment){
        this.outer = outerEnvironment;
    }

    void define(Token identifier, Object varValue){
        if(variables.containsKey(identifier.value))
            Language.error("variable '" + identifier.value + "' has already been defined", identifier.line);
        variables.put(identifier.value.toString(), varValue);
    }

    void assign(Token name, Object value){
        if(variables.containsKey(name.value)){
            variables.put(name.value.toString(), value);
            return;
        }

        if(outer != null){
            outer.assign(name, value);
            return;
        }


        Language.error( "Undefined variable '" + name.value.toString() + "'", name.line);
    }

    Object get(Token identifier){
        String varName = identifier.value.toString();
        if(variables.containsKey(varName))
            return variables.get(varName);
        
        if(outer != null)
            return outer.get(identifier);
        
        Language.error("variable '" + identifier.value + "' has NOT been defined", identifier.line);
        return null;
    }

}

class Token {
    TokenType type;
    Object value;
    int line;
    Token(TokenType type, Object value, int line){
        this.type = type;
        this.value = value;
        this.line = line;
    }
    //Constructer for things without values:
    Token(TokenType type, int line){
        this.type = type;
        this.line = line;
    }

    @Override
    public String toString() {
        return  "[" + type + " : " + value + "]";
    }
}

enum TokenType{

    // Primary literals:
    Double, STRING, TRUE, FALSE, NIL,

    // Numeric opperators:
    PLUS, MINUS, MULTIPLY, DIVIDE, POWER,

    // Boolean opperators:
    AND, OR, NOT, 

    // Relational opperators:
    DOUBLE_EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN,
    GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, 

    // Grouping:
    L_PAR, R_PAR, L_CUR, R_CUR,
    
    // statements:
    VAR, IF, ELSE, ELIF, FUNC, WHILE, PRINT,

    // Other:
    IDENTIFIER, EQUALS, EOS, EOF, COMMA
    }

abstract class Expre {
    Token token;

    abstract Object visit();
    
    boolean tokenIs(TokenType type){
        return token.type.equals(type);
    }

}

class LiteralExpre extends Expre {
    Object literal;
    LiteralExpre(Token token, Object literal){
        this.token = token;
        this.literal = literal;
    }

    @Override
    Object visit() {
        return literal;
    }

    @Override
    public String toString() {
        if(literal == null) return "nil";
        return literal.toString();
        }
    }

class BinOpExpre extends Expre {
    Expre left_node;
    Expre right_node;

    BinOpExpre(Expre left_node, Token op_token, Expre right_node){
        this.left_node = left_node;
        this.token = op_token;
        this.right_node = right_node;
    }

    @Override
    Object visit(){

        Object left = left_node.visit();
        Object right = right_node.visit();

        if(tokenIs(TokenType.MINUS)){
            checkNumberOperands(left, right);
            return (double) left - (double) right;
        }

        else if(tokenIs(TokenType.MULTIPLY)){
            checkNumberOperands(left, right);
            return (double) left * (double) right;
        }

        else if(tokenIs(TokenType.DIVIDE)){
            checkNumberOperands(left, right);
            return (double) left / (double) right;
        }

        else if(tokenIs(TokenType.GREATER_THAN)){
            checkNumberOperands(left, right);
            return (double) left > (double) right;
        }

        else if(tokenIs(TokenType.GREATER_THAN_OR_EQUAL)){
            checkNumberOperands(left, right);
            return (double) left >= (double) right;
        }

        else if(tokenIs(TokenType.LESS_THAN)){
            checkNumberOperands(left, right);
            return (double) left < (double) right;
        }

        else if(tokenIs(TokenType.LESS_THAN_OR_EQUAL)){
            checkNumberOperands(left, right);
            return (double) left <= (double) right;
        }

        else if(tokenIs(TokenType.DOUBLE_EQUAL)){
            return left.equals(right);
        }

        else if(tokenIs(TokenType.NOT_EQUAL)){
            return ! left.equals(right);
        }

        else if(tokenIs(TokenType.PLUS)){
            if(left instanceof Double && right instanceof Double){
                return (double) left + (double) right;
            }
            else if(left instanceof String && right instanceof String){
                return (String) left + (String) right;
            }
            else{
                Language.error("'+' operands must both be strings or numbers", token.line);
            }
        }

        // Unreachable code:
        return null;


    }

    @Override
    public String toString() {
        return "(" + left_node + ", " + token.type + ", " + right_node + ")";
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Language.error("both operands must be numbers", token.line);
    }

}

class UnaryOpExpre extends Expre {
    Expre child_node;
    UnaryOpExpre(Expre child_node, Token op_token){
        this.child_node = child_node;
        this.token = op_token;
        }


    @Override
    public String toString() {
        return "(" + token.type  + ", " + child_node + ")";
        }


    @Override
    Object visit() {
        Object right = child_node.visit();

        if(tokenIs(TokenType.MINUS)){
            checkNumberOperand(right);
            return -(double) right;
        }
        else if(tokenIs(TokenType.NOT)){
            checkBooleanoperand(right);
            return ! (boolean) right;
        }

        // Unreachable:
        return null;
    }

    private void checkNumberOperand(Object child){
        if(child instanceof Double) return;
        Language.error("operand must be a number", token.line);
    }

    private void checkBooleanoperand(Object child){
        if(child instanceof Boolean) return;
        Language.error("operand must be a boolean", token.line);
    }


    }

    class GroupingExpre extends Expre {
        Expre child_node;
        GroupingExpre(Expre node){
            this.child_node = node;
        }

        @Override
        public String toString() {
            return "[" + child_node + "]";
            }

        @Override
        Object visit() {
            return child_node.visit();
        }
        }

class Variable extends Expre {
    Variable(Token token){
        this.token = token;
    }

    @Override
    Object visit() {
        return Language.environment.get(token);
    }
}

class AssignExpre extends Expre {
    Expre value;
    AssignExpre(Token name, Expre value){
        this.token = name;
        this.value = value;
    }
    @Override
    Object visit() {
        Object val = value.visit();
        Language.environment.assign(token, val);
        return val;
    }
}

abstract class Stmt {
    abstract Object visit();

}

class ExpressionStmt extends Stmt {
    Expre expresion;
    ExpressionStmt(Expre expresion){
        this.expresion = expresion;
    }
    Void visit(){
        expresion.visit();
        return null;
    }

}

class Print extends Stmt {
    Expre expresion;
    Print(Expre expresion){
        this.expresion = expresion;
    }

    Void visit(){
        Object value = expresion.visit();
        System.out.println(Language.stringify(value));
        return null;
    }

}

class VarDeclare extends Stmt {
    Token varName;
    Expre initializer;

    VarDeclare(Token varName, Expre initializer){
        this.varName = varName;
        this.initializer = initializer;
    }

    @Override
    Void visit() {
        Object value = null;
        if(initializer != null)
            value = initializer.visit();
        Language.environment.define(varName, value);
        return null;
    }

}

class Block extends Stmt {
    List<Stmt> statements;
    Block(List<Stmt> statements){
        this.statements = statements;
    }

    @Override
    Void visit() {
        Environment previous = Language.environment;
        Language.environment = new Environment(previous);

        for (Stmt stmt : statements) 
            stmt.visit();
        
        Language.environment = previous;
        
        return null;
    }
}