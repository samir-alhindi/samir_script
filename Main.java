import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;


public class Main {
    public static void main(String[] args) {

        Language lang = new Language();

        //Testing
        lang.run("programs\\to_string.smr");

    }
}

class Language {

    static Environment globals = new Environment();
    static Environment environment = globals;
    static Stack<Environment> enviStack = new Stack<>();

    static boolean runningMethod = false;
    static boolean aboutToRunFunction = false;
    static int currentRunningLine = 1;

    int currentLine = 0;

    // Native functions, classes and variables:
    void init(){
        
        // List class:
        globals.define("List", new SamirCallable(){
            
            @Override
            public int arity() {
               return 0;
            }

            @Override
            public Object call(List<Object> arguments) {
                return new ListInstance(new ArrayList<>());
            }

            @Override
            public String toString() {
                return "<class List>";
            }

        });

        // PI:
        globals.define("PI", Math.PI);

        // Take input:
        globals.define("input", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                System.out.print(arg.toString());
                Scanner scanner = new Scanner(System.in);
                return scanner.nextLine();
            }
        });


        // cast to string function:
        globals.define("str", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                return stringify(arg);}
        });

        // cast to number function:
        globals.define("num", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(isNumeric(arg.toString()))
                    return Double.parseDouble(arg.toString());
                else
                    Language.error("Cannot cast " + arg.toString() + " to a number", Language.currentRunningLine);

                // unreachable:
                return null;

                }

            

        });

        // return type of variable:
        globals.define("typeOf", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg instanceof String)
                    return "string";
                else if(arg instanceof Double || arg instanceof Integer)
                    return "number";
                else if(arg instanceof Boolean)
                    return "boolean";
                else if(arg instanceof ListInstance)
                    return "List";
                else if(arg instanceof SamirInstance)
                    return ((SamirInstance)arg).class_.class_.name.value.toString();
                else if(arg instanceof SamirFunction)
                    return "function";
                else if(arg instanceof SamirClass)
                    return "class";
                else if(arg == null)
                    return "nil";
                else
                    return "native callable";

            }
        });

        // Check if value is a digit (012345...):
        globals.define("isNumeric", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg instanceof Double)
                    return true;
                else if(arg instanceof String)
                    return isNumeric(arg.toString());
                else
                    return false;
            }
        });

        // The getTime() function:
        globals.define("getTime", new SamirCallable() {
            @Override
            public int arity() {return 0;}
            @Override
            public Object call(List<Object> arguments) {return LocalTime.now().toString().substring(0, 8);}

        });

        // string methods:

        // getChar string method:
        globals.define("getChar", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                List<Object> index_and_string = checkStringIndex(arguments.get(0), arguments.get(1));
                int index = (int) index_and_string.get(0);
                String string = (String) index_and_string.get(1);
                return "" + string.charAt(index);
            }
            
        });

        globals.define("len", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg instanceof String)
                    return (Double) ((Integer) ((String) arg).length()).doubleValue();
                else if(arg instanceof ListInstance)
                    return (Double) ((Integer) ((ListInstance) arg).arrayList.size()).doubleValue();
                
                Language.error("len() argument must be a list or a string", Language.currentRunningLine);
                // unreachable code:
                return null;
            }
            
        });

    }

    void run(String file_path){
        init();
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
            System.out.println("File not found.");
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

    static void error(String log, int fromLine, int toLine){
        System.out.printf("Error from line %d to %d: %s", fromLine + 1, toLine + 1, log);
        System.exit(1);
    }

    boolean isNumeric(String string){
        try{
            Double.parseDouble(string);
            return true;
        }
        catch(NumberFormatException e){
            return false;
        }
    }

    
    List<Object> checkStringIndex(Object index_object, Object string_object){
        if(index_object instanceof Double == false)
            Language.error("1st argument of this string method must be a number", Language.currentRunningLine);
        Double index = (Double) index_object;

        if(index % 1 != 0)
            Language.error("1st argument must be a whole number", Language.currentRunningLine);

        if(string_object instanceof String == false)
            Language.error("2nd argument of this string method must be a string", Language.currentRunningLine);
        String string = (String) string_object;

        if(index >= string.length())
            Language.error("index " + index.intValue() + " out of bounds for length " + string.length(), Language.currentRunningLine);
        
        // Negative index:
        if(index < 0){
            Double actualIndex = index + string.length();
            if(actualIndex < 0)
                Language.error("index " + index.intValue() + " out of bounds for length " + string.length(), Language.currentRunningLine);
            index = actualIndex;
        }

        List<Object> output = new ArrayList<>();
        output.add(index.intValue());
        output.add(string);
        return output;
    }
        
            
    }

class Environment implements Cloneable{

    HashMap <String, Object> variables = new HashMap<>();
    Environment outer;

    Environment(){
        outer = null;
    }

    Environment(Environment outerEnvironment){
        this.outer = outerEnvironment;
    }

    void define(String identifier, Object value){
        variables.put(identifier, value);
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
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
    PLUS, MINUS, MULTIPLY, DIVIDE, MOD,

    // Boolean opperators:
    AND, OR, NOT, 

    // Relational opperators:
    DOUBLE_EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN,
    GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, 

    // Grouping:
    L_PAR, R_PAR, L_CUR, R_CUR,
    
    // keywords:
    VAR, IF, ELSE, ELIF, FUNC, WHILE, PRINT, PRINT_LN, THEN, DO, RETURN, CLASS,

    // Other:
    IDENTIFIER, EQUALS, EOS, EOF, COMMA, DOT,
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

        else if(tokenIs(TokenType.MOD)){
            checkNumberOperands(left, right);
            return (double) left % (double) right;
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

class BinBoolOp extends Expre {
    Expre left;
    Expre right;
    BinBoolOp(Expre left, Token op, Expre right){
        this.left = left;
        this.token = op;
        this.right = right;
    }
    @Override
    Object visit() {
        Object left = this.left.visit();
        if(left instanceof Boolean == false)
            Language.error("(and, or) operands must be boolean values ", token.line);

        if(tokenIs(TokenType.OR) && left.equals(true))
            return left;
        else if(tokenIs(TokenType.AND) && left.equals(false))
            return left;
        
        return right.visit();
    }
}

interface SamirCallable {
    int arity();
    Object call(List<Object> arguments);
}

class Call extends Expre {
    Expre callee;
    Token paren;
    List<Expre> arguments;

    Call(Expre callee, Token paren, List<Expre> arguments){
        this.callee = callee;
        this.paren = paren;
        this.arguments = arguments;
        this.token = callee.token;
    }

    @Override
    Object visit() {
        Object callee = this.callee.visit();
        List<Object> arguments = new ArrayList<>();

        if(Language.runningMethod){
            Language.environment = Language.enviStack.pop();
        }
            
        for (Expre arg : this.arguments) 
            arguments.add(arg.visit());
        
        if(Language.runningMethod){
            Language.runningMethod = false;
        }
        
        if(callee instanceof SamirCallable == false)
            Language.error("Can only call functions and classes !", paren.line);

        SamirCallable thingToCall = (SamirCallable)callee;

        if(arguments.size() != thingToCall.arity())
            Language.error("Expected " + thingToCall.arity() + " args but got " + arguments.size(), paren.line);

        Language.currentRunningLine = paren.line;

        return thingToCall.call(arguments);
        
    }

    @Override
    public String toString() {
        return callee + "(" + arguments + ")";
    }
    
}

class MemberAccess extends Expre {

    Expre instanceVar;
    Expre memberVar;

    MemberAccess(Expre instanceVar, Token dot, Expre memberVar){
        this.instanceVar = instanceVar;
        this.token = dot;
        this.memberVar = memberVar;
    }

    @Override
    Object visit() {
        Object instance_no_cast = instanceVar.visit();
        if(instance_no_cast instanceof SamirInstance == false)
            Language.error("can only access members from class instances", token.line);
        SamirInstance instance = (SamirInstance) instance_no_cast;

        String memberName = memberVar.token.value.toString();

        if(instance.environment.variables.containsKey(memberName)){
            Object value = instance.environment.variables.get(memberName);
            // See if member is method
            if(value instanceof SamirCallable){
                Language.currentRunningLine = token.line;
                Language.enviStack.add(Language.environment);
                Language.environment = instance.environment;
                Language.runningMethod = true;
                Object callResult =  memberVar.visit();
                // No need to pop from enviStack, The pop happnes in memberVar.visit().
                return callResult;
                
            }
            else
                return value;
        }
            

        Language.error(memberName + " not found in class: " + instance.class_.class_.name.value, token.line);

        // Unreachable code:
        return null;
    }
}

class MemberAssign extends Expre {
    MemberAccess member;
    Expre newValue;
    MemberAssign(MemberAccess member, Expre newValue){
        this.member = member;
        this.newValue = newValue;
    }

    Object visit(){
        SamirInstance instance = (SamirInstance) member.instanceVar.visit();
        Object newValueObject = newValue.visit();

        
        // Add new member asign code here that resembles the accses code:
        if(instance.environment.variables.containsKey(member.memberVar.token.value))
            instance.environment.variables.put(member.memberVar.token.value.toString(), newValueObject);
        else
            Language.error(member.memberVar.token.value + " not found in instance of class: " + instance.class_.class_.name, 0);
            
        return newValueObject;
    }
}

abstract class Stmt{
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
    Token printType;
    Print(Expre expresion, Token printType){
        this.expresion = expresion;
        this.printType = printType;
    }

    Void visit(){
        Object value = expresion.visit();
        if(printType.type.equals(TokenType.PRINT_LN))
            System.out.println(Language.stringify(value));
        else if(printType.type.equals(TokenType.PRINT))
            System.out.print(Language.stringify(value));
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
        Language.environment.define(varName.value.toString(), value);
        return null;
    }

}

class Block extends Stmt {

    List<Stmt> statements;
    Environment environment;

    Block(List<Stmt> statements){
        this.statements = statements;
        this.environment = new Environment();
    }

    @Override
    Void visit() {

        Language.enviStack.add(Language.environment);

        // No function call, just a normal block:
        if(Language.aboutToRunFunction == false)
            this.environment.outer = Language.environment;
        // if it's a function call then  leave it's outer as it is, It already has it's "closure".
        Language.environment = this.environment;
        Language.aboutToRunFunction = false;

        for (Stmt stmt : statements) 
            stmt.visit();
        
        
        Language.environment = Language.enviStack.pop();
        
        return null;
    }
}

class If extends Stmt {

    LinkedHashMap<Expre, Stmt> branches = new LinkedHashMap<>();
    Stmt elseBranch;

    If(LinkedHashMap<Expre, Stmt> branches, Stmt elseBranch){
        this.branches = branches;
        this.elseBranch = elseBranch;
    }

    @Override
    Void visit() {

        for (Map.Entry<Expre, Stmt> entry : branches.entrySet()) {
            Expre condition = entry.getKey();
            Stmt thenBranch = entry.getValue();

            Object result = condition.visit();
            if( result instanceof Boolean == false)
                Language.error("if statement condition should result in a boolean value", condition.token.line);
            if(result.equals(true)){
                thenBranch.visit();
                return null;
            }
                
        }
        
        if (elseBranch != null)
            elseBranch.visit();
        
        return null;
        
        
    }
    
}

class While extends Stmt {
    Expre condition;
    Stmt body;
    While(Expre condition, Stmt body){
        this.condition = condition;
        this.body = body;
    }
    @Override
    Void visit() {
        while (condition.visit().equals(true))
            body.visit();
        

    return null;
    }
}

class Function extends Stmt {
    Token name;
    List<Token> parameters;
    Block body;

    Function(Token name, List<Token> parameters, Block body){
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    Void visit() {
        SamirFunction function = new SamirFunction(this, Language.environment);
        Language.environment.define(name.value.toString(), function);
        return null;
    }
}

class Return extends Stmt {
    Token keyword;
    Expre value;
    Return(Token keyword, Expre value){
        this.keyword = keyword;
        this.value = value;
    }
    @Override
    Void visit() throws ReturnException {
       Object value = null;
       if (this.value != null) value = this.value.visit();

       throw new ReturnException(value);
    }
}

class ClassDeclre extends Stmt {
    List<Stmt> classBody;
    Function constructer;
    Function to_string;
    Token name;
    ClassDeclre(List<Stmt> classBody, Token name, Function constructer, Function to_string){
        this.classBody = classBody;
        this.name = name;
        this.constructer = constructer;
        this.to_string = to_string;
    }

    Void visit(){
        SamirClass class_ = new SamirClass(this);
        Language.environment.define(name.value.toString(), class_);
        return null;
    }
}