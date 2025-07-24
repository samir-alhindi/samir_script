import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;


public class Main {
    public static void main(String[] args) {

        
        String samir_script_filepath = args[0];
        Language lang = new Language(samir_script_filepath);
        lang.run();
        

        //Language lang = new Language("samir_script_programs\\short_prog.smr");
        //lang.run();
    }
}

class Language {

    Environment globals = new Environment();
    Environment environment = globals;
    Stack<Environment> enviStack = new Stack<>();

    boolean aboutToRunMethod = false;
    int currentRunningLine = 1;

    int currentLine = 0;
    String samir_script_filepath;
    Scanner scanner;

    Language(String samir_script_filepath){
        this.samir_script_filepath = samir_script_filepath;
        scanner = new Scanner(System.in);
    }

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
                return new ListInstance(new ArrayList<>(), Language.this);
            }

            @Override
            public String toString() {
                return "<class List>";
            }

        });

        globals.define("Dict", new SamirCallable() {

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(List<Object> arguments) {
                return new DictInstance(new HashMap<Object, Object>(), Language.this);
            }

            @Override
            public String toString() {
                return "<class Dict>";
            }
            
        });

        globals.define("Pair", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                return new SamirPair(arguments.get(0), arguments.get(1), Language.this);
            }
            
        });

        // PI:
        globals.define("PI", Math.PI);

        // newline:
        globals.define("ln", "\n");

        // Tab:
        globals.define("tab", "\t");

        // Take input:
        globals.define("input", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                System.out.print(Language.stringify(arg));
                String input = scanner.nextLine();
                return input;
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
                    Language.error("Cannot cast " + arg.toString() + " to a number", currentRunningLine);

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
                else if(arg instanceof SamirPair)
                    return "pair";
                else if(arg instanceof SamirInstance)
                    return ((SamirInstance)arg).class_.class_.name.value.toString();
                else if(arg instanceof SamirFunction)
                    return "function";
                else if(arg instanceof SamirLambda)
                    return "lambda";
                else if(arg instanceof SamirClass)
                    return "class";
                else if(arg instanceof SamirPairList)
                    return "pair list";
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
            public Object call(List<Object> arguments) {return ((Long) System.currentTimeMillis()).doubleValue();}

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
            public Double call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg instanceof String)
                    return int_to_Double(((String)arg).length());
                else if(arg instanceof ListInstance)
                    return int_to_Double(((ListInstance)arg).arrayList.size());
                else if(arg instanceof DictInstance)
                    return int_to_Double(((DictInstance)arg).hashMap.size());
                else if(arg instanceof SamirPairList)
                    return int_to_Double(((SamirPairList) arg).list.size());
                
                Language.error("len() argument must be a list, string or Dict", currentRunningLine);
                // unreachable code:
                return null;
            }
            
        });

        globals.define("exit", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Void call(List<Object> arguments) {
                if(arguments.get(0).equals(0.0))
                    System.exit(0);;
                System.out.println("Error: " + arguments.get(0));
                System.exit(1);
                return null;
            }
            
        });

        
        globals.define("stop", new SamirCallable() {

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return null;
            }
            
        });

        globals.define("fileExists", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Boolean call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false)
                    Language.error("fileExists() arg must be a file path", currentRunningLine);
                
                // Check if file is in local dir:
                String parent_dir = Paths.get(samir_script_filepath, "").getParent().toString();
                String file_path = parent_dir + "\\" + arguments.get(0);
                File file = new File(file_path);
                if(file.exists())
                    return true;
                // Check if in abs dir:
                File file_abs = new File((String)arguments.get(0)); 
                if(file_abs.exists())
                    return true;
                return false;
                
            }
            
        });
        

        globals.define("read", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false)
                    Language.error("read() arg must be a file path", currentRunningLine);
                String path = (String) arguments.get(0);
                
                // Check if file is in local dir:
                //String parent_dir = Paths.get(samir_script_filepath, "").getParent().toString();
                //String file_path = parent_dir + "\\" + arguments.get(0);
                
                try{
                    byte[] bytes = Files.readAllBytes(Paths.get(path));
                    String text = new String(bytes, Charset.defaultCharset());
                    return text;
                }
                catch(FileNotFoundException e){}
                catch(IOException e){}

                /*
                // Ckeck if file is in abs dir:...
                String path = (String) arguments.get(0);
                try{
                    byte[] bytes = Files.readAllBytes(Paths.get(path));
                    String text = new String(bytes, Charset.defaultCharset());
                    return text;
                }
                catch(FileNotFoundException e){
                    Language.error("could not find file in path: " + path, currentRunningLine);
                }
                catch(IOException e){
                    Language.error("could not find file in path: " + path, currentRunningLine);
                }
                    */

                // unreachable:
                return null;

            }
            
        });

        globals.define("write", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Void call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false)
                    Language.error("write() arg must be a file path", currentRunningLine);
                
                String path = (String) arguments.get(0);
                String content = Language.stringify(arguments.get(1));
                
                File file = new File(path);
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    System.out.println("couldn't create file: " + path);
                }
                try (FileWriter writer = new FileWriter(path)) {
                    writer.write(content);
                } catch (IOException e) {
                    System.out.println("couldn't write to file: " + path);
                    e.printStackTrace();
                }
                return null;
            }
            
        });

        globals.define("enumarate", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg instanceof ListInstance == false)
                    Language.error("enumarate() arg must be a list", currentRunningLine);
                ListInstance listInstance = (ListInstance) arg;
                // Generate indices:
                Double[] indices_array = new Double[listInstance.arrayList.size()];
                for(int i = 0; i < listInstance.arrayList.size(); i++)
                    indices_array[i] = Language.int_to_Double(i);
                ListInstance indices_list = ListInstance.create_filled_list(indices_array, Language.this);
                return new SamirPairList(indices_list, listInstance, Language.this);
            }
            
        });

        globals.define("zip", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof ListInstance == false
                || arguments.get(1) instanceof ListInstance == false)
                    Language.error("zip args must both be lists", currentRunningLine);
                ListInstance a = (ListInstance) arguments.get(0);
                ListInstance b = (ListInstance) arguments.get(1);
                return new SamirPairList(a, b, Language.this);
            }
            
        });

        globals.define("split", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public ListInstance call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false)
                    Language.error("first arg of split() must be a string", currentRunningLine);
                if(arguments.get(1) instanceof String == false || ((String) arguments.get(1)).length() != 1)
                    Language.error("second arg of split() must be a string of length 1", currentRunningLine);
                String word = (String) arguments.get(0);
                String split = (String) arguments.get(1);
                return ListInstance.create_filled_list(word.split(split), Language.this);
            }
            
        });

        globals.define("substring", new SamirCallable() {

            @Override
            public int arity() {return 3;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false
                || arguments.get(1) instanceof Double == false
                || arguments.get(2) instanceof Double == false)
                    Language.error("substring args must be (string, number, number)", currentRunningLine);
                String word = (String) arguments.get(0);
                Double start = (Double) arguments.get(1);
                Double end = (Double) arguments.get(2);
                if(start % 1 != 0 || end % 1 != 0)
                    Language.error("substring indices must be whole numbers", currentRunningLine);
                return word.substring(start.intValue(), end.intValue());
            }
            
        });

        globals.define("eval", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false)
                    Language.error("eval() arg must be a string", currentRunningLine);
                String source = (String) arguments.get(0);
                Lexer lexer = new Lexer(source);
                lexer.line = currentRunningLine;
                ArrayList<Token> tokens = lexer.lex();
                Parser parser = new Parser(tokens, Language.this);
                Expre expre = parser.expression();
                return expre.visit();
            }
            
        });
    }


    void run(){
        init();
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(samir_script_filepath));
            String source = new String(bytes, Charset.defaultCharset());
            Lexer lexer = new Lexer(source);
            ArrayList<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens, this);
            List<Stmt> program = parser.parse();
            for (Stmt stmt : program) {
                stmt.visit();
            }

        
        }
        catch(IOException d){
            Language.error("File: " + samir_script_filepath + " not found", currentRunningLine);
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
            Language.error("1st argument of this string method must be a number", currentRunningLine);
        Double index = (Double) index_object;

        if(index % 1 != 0)
            Language.error("1st argument must be a whole number", currentRunningLine);

        if(string_object instanceof String == false)
            Language.error("2nd argument of this string method must be a string", currentRunningLine);
        String string = (String) string_object;

        if(index >= string.length())
            Language.error("index " + index.intValue() + " out of bounds for length " + string.length(), currentRunningLine);
        
        // Negative index:
        if(index < 0){
            Double actualIndex = index + string.length();
            if(actualIndex < 0)
                Language.error("index " + index.intValue() + " out of bounds for length " + string.length(), currentRunningLine);
            index = actualIndex;
        }

        List<Object> output = new ArrayList<>();
        output.add(index.intValue());
        output.add(string);
        return output;
    }

    static Double int_to_Double(int i){
        return (Double) ((Integer) i).doubleValue();
    }

    static List<Object> to_list(Object object){
        ArrayList<Object> list = new ArrayList<>();
        list.add(object);
        return list;
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
        Environment clone = (Environment) super.clone();
        clone.variables = (HashMap<String, Object>) variables.clone();
        return clone;
    }

    @Override
    public String toString() {
        String output = " Environment {\n";
        for (String key : variables.keySet())
            output += key + " : " + variables.get(key) + "\n";
        

        return output + "}";
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

    // Compound assignment operators (+=, -=...):
    PLUS_EQUAL, MINUS_EQUAL, MULTIPLY_EQUAL, DIVIDE_EQUAL, MOD_EQUAL,

    // Boolean opperators:
    AND, OR, NOT, 

    // Relational opperators:
    DOUBLE_EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN,
    GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, 

    // Grouping:
    L_PAR, R_PAR, L_CUR, R_CUR, L_BRACKET, R_BRACKET,
    
    // keywords:
    VAR, IF, ELSE, ELIF, FUNC, WHILE, PRINT, PRINT_LN, THEN, DO,
    RETURN, CLASS, BREAK, CONTINUE, LAMBDA, MATCH, WITH, CASE,
    FOR, IN, ENUM, IMPORT, AS, 

    // Other:
    IDENTIFIER, EQUALS, EOS, EOF, COMMA, DOT, ARROW, COLON,
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
    Language lang;
    LiteralExpre(Token token, Object literal, Language lang){
        this.token = token;
        this.literal = literal;
        this.lang = lang;
    }

    @Override
    Object visit() {
        if(token.type.equals(TokenType.STRING))
            return string();
        return literal;
        
    }

    String string(){

        // No interpolation needed:-
        String og_string = (String) literal;
        if( ! og_string.contains("{") && ! og_string.contains("\\"))
            return og_string;
        
        String output = "";
        int i = 0;
        while (i < og_string.length()) {
            if(og_string.charAt(i) != '{' && og_string.charAt(i) != '\\'){
                output += og_string.charAt(i);
                i++;
                continue;
            }
            // Else it is '{':
            if(og_string.charAt(i) == '{'){
                String expression = "";
                do{
                    i++;
                    if(i >= og_string.length())
                        Language.error("unterminated '{', Perhaps you want an escape character '\\{' instead", token.line);
                    expression += og_string.charAt(i);
                } while(og_string.charAt(i) != '}');

                // We found the closing '}':
                Lexer lexer = new Lexer(expression);
                lexer.line = token.line;
                ArrayList<Token> tokens = lexer.lex();
                Parser parser = new Parser(tokens, lang);
                Expre ast = parser.expression();
                Object result = ast.visit();
                output += Language.stringify(result);
                // Move past the '}':
                i++;
            }
            // Else it is '\':
            else if(og_string.charAt(i) == '\\'){
                i++;
                if(i >= og_string.length())
                    Language.error("escape char '\\' must be followed by another char (\\n,\\t,\\{)", token.line);
                switch (og_string.charAt(i)) {
                    case 'n'-> output += System.lineSeparator();
                    case 't' -> output += '\t';
                    case '{' -> output += '{';
                    case '}' -> output += '}';
                    case '\\' -> output += '\\';
                    default -> Language.error("invalid escape character: \\" + og_string.charAt(i), token.line);
                }
                i++;
            }

        }

        return output;
    }
    

    @Override
    public String toString() {
        if(literal == null) return "nil";
        return literal.toString();
        }
    }

class ListLiteral extends Expre {
    List<Expre> elements;
    Language lang;
    ListLiteral(Token token, List<Expre> elements, Language lang){
        this.token = token;
        this.elements = elements;
        this.lang = lang;
    }
    @Override
    ListInstance visit() {
        ArrayList<Object> elementsVisited = new ArrayList<>();
        for (Expre element : elements) 
            elementsVisited.add(element.visit());
        ListInstance list =  new ListInstance(elementsVisited, lang);
        list.environment.variables.put("size", (Double) ((double) elements.size()));
        return list;
    }
}

class DictLiteral extends Expre {
    Map<Expre, Expre> map;
    Language lang;
    DictLiteral(Token token, Map<Expre, Expre> map, Language lang){
        this.token = token;
        this.map = map;
        this.lang = lang;
    }
    @Override
    Object visit() {
        var output = new HashMap<Object, Object>();
        for (Map.Entry<Expre, Expre> pair : map.entrySet()) {
            Object key = pair.getKey().visit();
            Object value = pair.getValue().visit();
            output.put(key, value);
        }
        return new DictInstance(output, lang);
    }
}

class BinOpExpre extends Expre {
    Expre left_node;
    Expre right_node;
    Language lang;

    BinOpExpre(Expre left_node, Token op_token, Expre right_node, Language lang){
        this.left_node = left_node;
        this.token = op_token;
        this.right_node = right_node;
        this.lang = lang;
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
            if(left == null && right != null) return false;
            else if(left == null && right == null) return true;
            return left.equals(right);
        }

        else if(tokenIs(TokenType.NOT_EQUAL)){
            if(left == null && right != null) return false;
            else if(left == null && right == null) return true;
            return ! left.equals(right);
        }

        else if(tokenIs(TokenType.PLUS)){
            if(left instanceof Double && right instanceof Double){
                return (double) left + (double) right;
            }
            else if(left instanceof String && right instanceof String){
                return (String) left + (String) right;
            }
            else if(left instanceof ListInstance){
                if(right instanceof ListInstance){
                    ListInstance combined = new ListInstance(new ArrayList<>(), lang);
                    for (Object item : ((ListInstance)left).arrayList)
                        combined.arrayList.add(item);
                    for (Object item : ((ListInstance)right).arrayList)
                        combined.arrayList.add(item);
                    combined.environment.variables.put("size", ((ListInstance) left).getSize() + ((ListInstance) right).getSize());
                    return combined;
                }

            }
            else{
                Language.error("'+' operands must both be strings or numbers or Lists", token.line);
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
    Language lang;
    Variable(Token token, Language lang){
        this.token = token;
        this.lang = lang;
    }

    @Override
    Object visit() {
        return lang.environment.get(token);
    }
}

class AssignExpre extends Expre {
    Expre right;
    Token opp;
    Token varName;
    Language lang;

    AssignExpre(Token varName, Expre right, Token opp, Language lang){
        this.varName = varName;
        this.right = right;
        this.opp = opp;
        this.lang = lang;
    }
    @Override
    Object visit() {

        Object newValue = null;

        Object rightVisited = right.visit();
        Object leftVisited = lang.environment.get(varName);

        switch(opp.type){
            case TokenType.EQUALS -> {
                newValue = rightVisited;
            } 
            case TokenType.MINUS_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited - (Double) rightVisited;
            }
            case TokenType.MULTIPLY_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited * (Double) rightVisited;
            }
            case TokenType.DIVIDE_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited / (Double) rightVisited;
            }
            case TokenType.MOD_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited % (Double) rightVisited;
            }
            case TokenType.PLUS_EQUAL -> {

                if(leftVisited instanceof Double && rightVisited instanceof Double)
                    newValue = (Double) leftVisited + (Double) rightVisited;

                else if(leftVisited instanceof String && rightVisited instanceof String)
                    newValue = (String) leftVisited + (String) rightVisited;
        
                else if(leftVisited instanceof ListInstance){
                    if(rightVisited instanceof ListInstance){
                        ListInstance combined = new ListInstance(new ArrayList<>(), lang);
                        for (Object item : ((ListInstance)leftVisited).arrayList)
                            combined.arrayList.add(item);
                        for (Object item : ((ListInstance)rightVisited).arrayList)
                            combined.arrayList.add(item);
                        combined.environment.variables.put("size", ((ListInstance) leftVisited).getSize() + ((ListInstance) rightVisited).getSize());
                        newValue = combined;
                    }

                }
                else
                    Language.error("'+=' opperands must both be numbers or strings or Lists", opp.line);
            }
        }

        lang.environment.assign(varName, newValue);
        return newValue;
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Language.error("both operands must be numbers", token.line);
    }
}

class CollectionAssign extends Expre {
    Subscript subscript;
    Expre newValue;
    CollectionAssign(Subscript subscript, Expre newValue, Token token){
        this.subscript = subscript;
        this.newValue = newValue;
        this.token = token;
    }
    @Override
    Object visit() {
        
        Object collectionVisited = subscript.collection.visit();
        Object indexVisited = subscript.index.visit();
        Object newValueVisited = newValue.visit();

        if(collectionVisited instanceof ListInstance){
            int final_index = ((ListInstance)collectionVisited).checkValidIndex(indexVisited);
            ((ListInstance)collectionVisited).arrayList.set(final_index, newValueVisited);
        }

        else if(collectionVisited instanceof String){

            if(newValueVisited instanceof String == false || ((String) newValueVisited).length() != 1)
                Language.error("string[index] must be set to a string of length 1", token.line);

            int finalIndex = Subscript.Inner.checkValidIndex(indexVisited, collectionVisited.toString(), token);
            char[] chars = ((String) collectionVisited).toCharArray();
            chars[finalIndex] = ((String) newValueVisited).charAt(0);
            collectionVisited = new String(chars);
        }

        else if(collectionVisited instanceof DictInstance){
            ( (DictInstance) collectionVisited).hashMap.put(indexVisited, newValueVisited);
        }

        else
            Language.error("Invalid assignment target", token.line);

        

        return collectionVisited;
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
    Language lang;

    Call(Expre callee, Token paren, List<Expre> arguments, Language lang){
        this.callee = callee;
        this.paren = paren;
        this.arguments = arguments;
        this.token = callee.token;
        this.lang = lang;
    }

    @Override
    Object visit() {
        Object callee = this.callee.visit();
        List<Object> arguments = new ArrayList<>();

        if(lang.aboutToRunMethod){
            lang.environment = lang.enviStack.pop();
            lang.aboutToRunMethod = false;
        }
            
            
        for (Expre arg : this.arguments) 
            arguments.add(arg.visit());
        
        if(lang.aboutToRunMethod){
            lang.aboutToRunMethod = false;
        }
        
        if(callee instanceof SamirCallable == false)
            Language.error("Can only call functions and classes !", paren.line);

        SamirCallable thingToCall = (SamirCallable)callee;

        if(arguments.size() != thingToCall.arity())
            Language.error("Expected " + thingToCall.arity() + " args but got " + arguments.size(), paren.line);

        lang.currentRunningLine = paren.line;

        return thingToCall.call(arguments);
        
    }

    @Override
    public String toString() {
        return callee + "(" + arguments + ")";
    }
    
}

class Subscript extends Expre {

    Expre index;
    Expre collection;

    Subscript(Token bracket, Expre collection, Expre index){
        this.token = bracket;
        this.collection = collection;
        this.index = index;
    }

    @Override
    Object visit() {

        Object collectionVisited = collection.visit();
        Object indexVisited = index.visit();

        if(collectionVisited instanceof ListInstance){
            int final_index = ((ListInstance)collectionVisited).checkValidIndex(indexVisited);
            return ((ListInstance)collectionVisited).arrayList.get(final_index);
        }

        else if(collectionVisited instanceof String){
            int final_index = Inner.checkValidIndex(indexVisited, (String) collectionVisited, token);
            return ((String) collectionVisited).charAt(final_index) + "";
        }

        else if(collectionVisited instanceof DictInstance){
            if(((DictInstance)collectionVisited).hashMap.containsKey(indexVisited) == false)
                Language.error("Key: " + indexVisited.toString() + " not found in Dict", token.line);
            return ((DictInstance)collectionVisited).hashMap.get(indexVisited);
        }

        else if(collectionVisited instanceof SamirPairList){
            int final_index = Inner.checkValidIndex(indexVisited, (SamirPairList) collectionVisited, token);
            return ((SamirPairList)collectionVisited).list.get(final_index);
        }


        return null;
    }

        class Inner {
            static int checkValidIndex(Object object, String string, Token token){
                if(object instanceof Double == false)
                    Language.error("string index must be a number", token.line);
                Double index = (Double) object;

                if(index % 1 != 0)
                    Language.error("argument must be a whole number", token.line);

                if(index >= string.length())
                    Language.error("index " + index.intValue() + " out of bounds for size " + string.length(), token.line);
                
                // Negative index:
                if(index < 0){
                    Double actualIndex = index + string.length();
                    if(actualIndex < 0)
                        Language.error("index " + index.intValue() + " out of bounds for size " + string.length(), token.line);
                    index = actualIndex;
        }
        
        return index.intValue();

            }
        
        static int checkValidIndex(Object object, SamirPairList samir_pair_list, Token token){
                if(object instanceof Double == false)
                    Language.error("string index must be a number", token.line);
                Double index = (Double) object;

                if(index % 1 != 0)
                    Language.error("argument must be a whole number", token.line);

                if(index >= samir_pair_list.list.size())
                    Language.error("index " + index.intValue() + " out of bounds for size " + samir_pair_list.list.size(), token.line);
                
                // Negative index:
                if(index < 0){
                    Double actualIndex = index + samir_pair_list.list.size();
                    if(actualIndex < 0)
                        Language.error("index " + index.intValue() + " out of bounds for size " + samir_pair_list.list.size(), token.line);
                    index = actualIndex;
        }
        
        return index.intValue();
        }

        }
}

class MemberAccess extends Expre {

    Expre instanceVar;
    Expre memberVar;
    Language lang;

    MemberAccess(Expre instanceVar, Token dot, Expre memberVar, Language lang){
        this.instanceVar = instanceVar;
        this.token = dot;
        this.memberVar = memberVar;
        this.lang = lang;
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
                lang.currentRunningLine = token.line;
                Environment prev = lang.environment;
                lang.enviStack.add(prev);
                lang.environment = instance.environment;
                lang.aboutToRunMethod = true;
                Object callResult =  memberVar.visit();
    
                while(lang.environment != prev)
                    lang.environment = lang.enviStack.pop();
                return callResult;
                
            }
            else
                return value;
        }
        
        String instanceType = (instance instanceof Importinstance) ? "import" : "class";
        Language.error(memberName + " not found in " +  instanceType + ": " + instance.class_.class_.name.value, token.line);

        // Unreachable code:
        return null;
    }
}

class MemberAssign extends Expre {
    MemberAccess member;
    Expre newValue;
    Token opp;
    Language lang;

    MemberAssign(MemberAccess member, Expre newValue, Token opp, Language lang){
        this.member = member;
        this.newValue = newValue;
        this.opp = opp;
        this.lang = lang;
    }

    Object visit(){

        SamirInstance instance = (SamirInstance) member.instanceVar.visit();

        Object rightVisited = newValue.visit();
        Object leftVisited = null;

        Object newValueObject = null;

        if(instance.environment.variables.containsKey(member.memberVar.token.value))
            leftVisited = instance.environment.get(member.memberVar.token);
        else
            Language.error(member.memberVar.token.value + " not found in instance of class: " + instance.class_.class_.name.value, opp.line);

        switch(opp.type){
            case TokenType.EQUALS -> {
                newValueObject = rightVisited;
            } 
            case TokenType.MINUS_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited - (Double) rightVisited;
            }
            case TokenType.MULTIPLY_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited * (Double) rightVisited;
            }
            case TokenType.DIVIDE_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited / (Double) rightVisited;
            }
            case TokenType.MOD_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited % (Double) rightVisited;
            }
            case TokenType.PLUS_EQUAL -> {

                if(leftVisited instanceof Double && rightVisited instanceof Double)
                    newValueObject = (Double) leftVisited + (Double) rightVisited;

                else if(leftVisited instanceof String && rightVisited instanceof String)
                    newValueObject = (String) leftVisited + (String) rightVisited;
        
                else if(leftVisited instanceof ListInstance){
                    if(rightVisited instanceof ListInstance){
                        ListInstance combined = new ListInstance(new ArrayList<>(), lang);
                        for (Object item : ((ListInstance)leftVisited).arrayList)
                            combined.arrayList.add(item);
                        for (Object item : ((ListInstance)rightVisited).arrayList)
                            combined.arrayList.add(item);
                        combined.environment.variables.put("size", ((ListInstance) leftVisited).getSize() + ((ListInstance) rightVisited).getSize());
                        newValueObject = combined;
                    }

                }
                else
                    Language.error("'+=' opperands must both be numbers or strings or Lists", opp.line);
            }
        }

        
        instance.environment.variables.put(member.memberVar.token.value.toString(), newValueObject);
        return newValueObject;
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Language.error("both operands must be numbers", token.line);
    }
}

class Lambda extends Expre {
    List<Token> parameters;
    Expre body;
    Language lang;

    Lambda(Token keyword, List<Token> parameters, Expre body, Language lang){
        this.token = keyword;
        this.parameters = parameters;
        this.body = body;
        this.lang = lang;
    }

    @Override
    Object visit() {
        return new SamirLambda(this, lang.environment, lang);
    }
}

class Ternary extends Expre {
    Expre left;
    Expre middle;
    Expre right;
    Ternary(Expre left, Expre middle, Expre right, Token keyword){
        this.left = left;
        this.middle = middle;
        this.right = right;
        this.token = keyword;
    }
    @Override
    Object visit() {
        Object condition = middle.visit();
        if(condition instanceof Boolean == false)
            Language.error("Ternary condition must be a boolean expression", token.line);
        Object result = (Boolean) condition ? left.visit() : right.visit();
        return result;
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
    Language lang;

    VarDeclare(Token varName, Expre initializer, Language lang){
        this.varName = varName;
        this.initializer = initializer;
        this.lang = lang;
    }

    @Override
    Void visit() {
        Object value = null;
        if(initializer != null)
            value = initializer.visit();
        lang.environment.define(varName.value.toString(), value);
        return null;
    }

}

class Block extends Stmt {

    List<Stmt> statements;
    Environment environment;
    Language lang;

    Block(List<Stmt> statements, Language lang){
        this.statements = statements;
        this.lang = lang;
    }

    @Override
    Void visit() {

        Environment prev = lang.environment;
        lang.enviStack.add(prev);
        lang.environment = new Environment(prev);
        
        for (Stmt stmt : statements) 
            stmt.visit();
        
        while(lang.environment != prev)
            lang.environment = lang.enviStack.pop();
        
        
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

class BreakException extends RuntimeException {
    BreakException(Token keyword){
        super("at line " + keyword.line +": break statement must be inside a while loop block", null, false, false);
    }
}

class ContinueException extends RuntimeException {
    ContinueException(Token keyword){
        super("at line " + keyword.line + ": continue statement must be inside a while loop block", null, false, false);
    }
}

class While extends Stmt {
    Expre condition;
    Stmt body;
    Language lang;
    While(Expre condition, Stmt body, Language lang){
        this.condition = condition;
        this.body = body;
        this.lang = lang;
    }
    @Override
    Void visit() {
        Environment lasEnvi = lang.environment;
        // Try block for the break statement:
        try {
            while (condition.visit().equals(true)){
                // Try block for the continue statement:
                try {
                    body.visit();
                }
                catch(ContinueException e){
                    while (lang.environment != lasEnvi) 
                        lang.environment = lang.enviStack.pop();
                }
            }

                
        }
        catch(BreakException e){
            while (lang.environment != lasEnvi) 
                lang.environment = lang.enviStack.pop();
        }

    return null;
    }
}

class For extends Stmt {
    Token identfier;
    Expre iterable;
    Stmt body;
    Language lang;
    Token second_identfier;
    For(Token identfier, Expre iterable, Stmt body, Token second_identfier, Language lang){
        this.identfier = identfier;
        this.iterable = iterable;
        this.body = body;
        this.second_identfier = second_identfier;
        this.lang = lang;
    }

    @Override
    Void visit() {

    class Inner {
        static void iterate(String string, Stmt body, Token identfier, Language lang){
            Environment lasEnvi = lang.environment;
            Environment newEnvi = new Environment(lasEnvi);
            newEnvi.define((String) identfier.value, null);
            lang.enviStack.add(lasEnvi);
            lang.environment = newEnvi;
            
            // Try block for the break statement:
            try {
                for(char c : string.toCharArray()){
                    newEnvi.define((String) identfier.value, c + "");
                    // Try block for the continue statement:
                    try {
                        body.visit();
                    }
                    catch(ContinueException e){
                        while (lang.environment != lasEnvi) 
                            lang.environment = lang.enviStack.pop();
                        lang.enviStack.add(lasEnvi);
                        lang.environment = newEnvi;


                    }
                }

                    
            }
            catch(BreakException e){
                while (lang.environment != lasEnvi) 
                    lang.environment = lang.enviStack.pop();
            }

            while (lang.environment != lasEnvi) 
                lang.environment = lang.enviStack.pop();

        }

        static void iterate(ListInstance list, Stmt body, Token identfier, Language lang){
            Environment lasEnvi = lang.environment;
            Environment newEnvi = new Environment(lasEnvi);
            newEnvi.define((String) identfier.value, null);
            lang.enviStack.add(lasEnvi);
            lang.environment = newEnvi;
            
            // Try block for the break statement:
            try {
                for(Object c : list.arrayList){
                    newEnvi.define((String) identfier.value, c);
                    // Try block for the continue statement:
                    try {
                        body.visit();
                    }
                    catch(ContinueException e){
                        while (lang.environment != lasEnvi) 
                            lang.environment = lang.enviStack.pop();
                        lang.enviStack.add(lasEnvi);
                        lang.environment = newEnvi;
                    }
                }

                    
            }
            catch(BreakException e){
                while (lang.environment != lasEnvi) 
                    lang.environment = lang.enviStack.pop();
            }

            while (lang.environment != lasEnvi) 
                lang.environment = lang.enviStack.pop();

        }

        static void iterate(SamirPairList samir_pair_list, Stmt body, Token identfier, Token second_identfier, Language lang){
            boolean unpack_pairs = second_identfier != null;
            Environment lasEnvi = lang.environment;
            Environment newEnvi = new Environment(lasEnvi);
            newEnvi.define((String) identfier.value, null);
            if(unpack_pairs)
                newEnvi.define((String) second_identfier.value, null);
            lang.enviStack.add(lasEnvi);
            lang.environment = newEnvi;
            
            // Try block for the break statement:
            try {
                for(SamirPair pair : samir_pair_list.list){
                    if(unpack_pairs){
                        newEnvi.define((String) identfier.value, pair.first);
                        newEnvi.define((String) second_identfier.value, pair.second);
                    }
                    else
                        newEnvi.define((String) identfier.value, pair);
                    // Try block for the continue statement:
                    try {
                        body.visit();
                    }
                    catch(ContinueException e){
                        while (lang.environment != lasEnvi) 
                            lang.environment = lang.enviStack.pop();
                        lang.enviStack.add(lasEnvi);
                        lang.environment = newEnvi;
                    }
                }

                    
            }
            catch(BreakException e){
                while (lang.environment != lasEnvi) 
                    lang.environment = lang.enviStack.pop();
            }

            while (lang.environment != lasEnvi) 
                lang.environment = lang.enviStack.pop();

        }
    }

    Object iterableVisited = iterable.visit();
    if((iterableVisited instanceof String || iterableVisited instanceof ListInstance) && second_identfier != null)
        Language.error("Can only use 2 variables in a for loop to iterate over PairList", identfier.line);
    if(iterableVisited instanceof String)
        Inner.iterate( (String) iterableVisited, body, identfier, lang);
    else if(iterableVisited instanceof ListInstance)
        Inner.iterate((ListInstance) iterableVisited, body, identfier, lang);
    else if(iterableVisited instanceof SamirPairList)
        Inner.iterate((SamirPairList) iterableVisited, body, identfier, second_identfier, lang);
    else
        Language.error("can only iterate over Lists and strings", identfier.line);

    return null;
    }

    
}

class Match extends Stmt {

    Token keyword;
    Expre mainExpre;
    LinkedHashMap<Expre, Stmt> branches;
    Stmt elseBranch;

    Match(Token keyword, Expre mainExpre, LinkedHashMap<Expre, Stmt> branches, Stmt elseBranch){
        this.keyword = keyword;
        this.branches = branches;
        this.mainExpre = mainExpre;
        this.elseBranch = elseBranch;
    }

    @Override
    Void visit() {
        Object mainExpreVisitd = mainExpre.visit();
        if(mainExpreVisitd == null)
            Language.error("Can't match null", keyword.line);
        for (Map.Entry<Expre, Stmt> entry : branches.entrySet()) {
            Expre condition = entry.getKey();
            Stmt stmt = entry.getValue();

            Object result = condition.visit();
            if(mainExpreVisitd.equals(result)){
                stmt.visit();
                return null;
            }
                
        }
        
        if (elseBranch != null)
            elseBranch.visit();
        
        return null;
    }

    
}

class Function extends Stmt {
    Token name;
    List<Token> parameters;
    List<Stmt> body;
    Language lang;

    Function(Token name, List<Token> parameters, List<Stmt> body, Language lang){
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.lang = lang;
    }

    @Override
    Void visit() {
        SamirFunction function = new SamirFunction(this, lang.environment, lang);
        lang.environment.define(name.value.toString(), function);
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

       throw new ReturnException(value, keyword);
    }
}

class Continue extends Stmt {
    Token keyword;
    Continue(Token keyword){
        this.keyword = keyword;
    }

    @Override
    Object visit() {
        throw new ContinueException(keyword);
    }
}

class Break extends Stmt {
    Token keyword;
    Break(Token keyword){
        this.keyword = keyword;
    }

    @Override
    Object visit() {
        throw new BreakException(keyword);
    }
}

class ClassDeclre extends Stmt {
    List<Stmt> classBody;
    Function constructer;
    Function to_string;
    Token name;
    Language lang;
    ClassDeclre(List<Stmt> classBody, Token name, Function constructer, Function to_string, Language lang){
        this.classBody = classBody;
        this.name = name;
        this.constructer = constructer;
        this.to_string = to_string;
        this.lang = lang;
    }

    Void visit(){
        SamirClass class_ = new SamirClass(this, lang.environment, lang);
        lang.environment.define(name.value.toString(), class_);
        return null;
    }
}

class EnumDecl extends Stmt {
    Token keyword;
    List<Token> identfiers;
    Language lang;
    EnumDecl(Token keyword, List<Token> identfiers, Language lang){
        this.keyword = keyword;
        this.identfiers = identfiers;
        this.lang = lang;
    }
    @Override
    Void visit() {
        for (int i = 0; i < identfiers.size(); i++)
            lang.environment.define(identfiers.get(i).value.toString(), Language.int_to_Double(i));
        return null;
    }
}

class Import extends Stmt {
    Token keyword;
    String path;
    Token identfier;
    Language lang;
    Import(Token keyword, String path, Token identfier, Language lang){
        this.keyword = keyword;
        this.path = path;
        this.identfier = identfier;
        this.lang = lang;
    }
    @Override
    Void visit() {
        Language new_lang = new Language(path);
        new_lang.run();
        SamirInstance import_instance = new Importinstance(new_lang, identfier);
        lang.environment.define((String) identfier.value, import_instance);
        return null;
    }
}