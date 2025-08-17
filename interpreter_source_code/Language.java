import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class Language {

    Environment globals = new Environment();
    Environment environment = globals;
    Stack<Environment> enviStack = new Stack<>();

    int line = 1;
    String cur_file_name;

    String samir_script_filepath;
    Scanner scanner;

    Language(String samir_script_filepath){
        this.samir_script_filepath = samir_script_filepath;
        scanner = new Scanner(System.in);
    }

    void run(){
        NativeFunctions.init(globals, this);
        String source = read_source(samir_script_filepath, this);
        List<Stmt> program = lex_then_parse(source, this, Paths.get(samir_script_filepath).getFileName().toString());
        for (Stmt stmt : program) 
            stmt.visit();
    }

    static String read_source(String path, Language lang){
        String source = null;
        try{
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            source = new String(bytes, Charset.defaultCharset());
        }
        catch(IOException d){
            Language.error("File: " + path + " not found", lang.line, path);
        }
        return source;
    }

    static List<Stmt> lex_then_parse(String source, Language lang, String file_name){
            Lexer lexer = new Lexer(source, file_name);
            lexer.line = lang.line;
            ArrayList<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens, lang);
            List<Stmt> program = parser.parse();
            return program;
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

        if(object instanceof SamirCallable && object.getClass().isAnonymousClass())
            return "<native callable>";
    
        return object.toString();
      }

      static void error(String log, int line, String file){
        System.out.printf("Error in file: %s at line %d: %s", file, line + 1, log);
        System.exit(1);
    }

    static void error(String log, int fromLine, int toLine){
        System.out.printf("Error from line %d to %d: %s", fromLine + 1, toLine + 1, log);
        System.exit(1);
    }

    static boolean isNumeric(String string){
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
            Language.error("1st argument of this string method must be a number", line, cur_file_name);
        Double index = (Double) index_object;

        if(index % 1 != 0)
            Language.error("1st argument must be a whole number", line, cur_file_name);

        if(string_object instanceof String == false)
            Language.error("2nd argument of this string method must be a string", line, cur_file_name);
        String string = (String) string_object;

        if(index >= string.length())
            Language.error("index " + index.intValue() + " out of bounds for length " + string.length(), line, cur_file_name);
        
        // Negative index:
        if(index < 0){
            Double actualIndex = index + string.length();
            if(actualIndex < 0)
                Language.error("index " + index.intValue() + " out of bounds for length " + string.length(), line, cur_file_name);
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