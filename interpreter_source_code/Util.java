import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Util {

    static String read_source(String path, Runtime runtime){
        String source = null;
        try{
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            source = new String(bytes, Charset.defaultCharset());
        }
        catch(IOException d){
            Runtime.error("File: " + path + " not found", runtime.line, path);
        }
        return source;
    }

    static List<Stmt> lex_then_parse(String source, Runtime runtime, String file_name){
            Lexer lexer = new Lexer(source, file_name);
            lexer.line = runtime.line;
            ArrayList<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens, runtime);
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

    static boolean isNumeric(String string){
        try{
            Double.parseDouble(string);
            return true;
        }
        catch(NumberFormatException e){
            return false;
        }
    }

    static int checkValidIndex(Object object, int len, Runtime lang){
        Util.check_type(object, Double.class, "index must be a number", lang.line, lang.cur_file_name);
        Double index = (Double) object;

        if(index % 1 != 0)
            Runtime.error("index must be a whole number", lang.line, lang.cur_file_name);

        if(index >= len)
            Runtime.error("index " + index.intValue() + " out of bounds for size " + len, lang.line, lang.cur_file_name);
        
        // Negative index:
        if(index < 0){
            Double actualIndex = index + len;
            if(actualIndex < 0)
                Runtime.error("index " + index.intValue() + " out of bounds for size " + len, lang.line, lang.cur_file_name);
            index = actualIndex;
        }
        
        return index.intValue();
    }
    static Double int_to_Double(int i){
        return (Double) ((Integer) i).doubleValue();
    }

    static List<Object> to_list(Object object){
        ArrayList<Object> list = new ArrayList<>();
        list.add(object);
        return list;
    }

    static String typeOf(Object arg){
        if(arg == null) return "nil";
        return switch(arg){
            case String x -> "string";
            case Double d -> "number";
            case Boolean b -> "boolean";
            case SamirFunction f -> "function";
            case SamirLambda l -> "lambda";
            case SamirObject o -> o.type;
            default -> "native callable";
    };
}

    static <T> T check_type(Object object, Class<?> type, String log, int line, String file_name){
        if(object != null)
            if(type.isInstance(object))
                return (T) object;
        return (T) Runtime.error(log, line, file_name);
        
    }

    static <T> T check_interface(Object object, Class<?> the_interface, String log, int line, String file_name){
            for (Class<?> c : object.getClass().getInterfaces())
                if (c == the_interface)
                    return (T) object;
            Runtime.error(log, line, file_name);
            return null;
        }

    static boolean token_is(Token tok, TokenType... allTypes){
        TokenType curType = tok.type;
        for (TokenType type : allTypes)
            if(curType.equals(type))
                return true;
        return false;
    }


}

