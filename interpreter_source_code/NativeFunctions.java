import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NativeFunctions {

    Environment globals;
    Language lang;
    NativeFunctions(Environment globals, Language lang){
        this.globals = globals;
        this.lang = lang;
    }

    void check_type(Object object, Class<?> type, String log){
        if(object == null || object.getClass() != type)
            Language.error(log, lang.line, lang.cur_file_name);
    }

    static void check_type(Object object, Class<?> type, String log, int line, String file_name){
        if(object == null || object.getClass() != type)
            Language.error(log, line, file_name);
    }

    static void check_interface(Object object, Class<?> the_interface, String log, int line, String file_name){
            for (Class<?> c : object.getClass().getInterfaces())
                if (c == the_interface)
                    return;
            Language.error(log, line, file_name);
        }

    void check_interface(Object object, Class<?> the_interface, String log){
        for (Class<?> c : object.getClass().getInterfaces())
            if (c == the_interface)
                return;
        Language.error(log, lang.line, lang.cur_file_name);
        
    }

    void init(){

        // List class:
        globals.define("List", new SamirCallable(){
            
            @Override
            public int arity() {
               return 0;
            }

            @Override
            public Object call(List<Object> arguments) {
                return new ListInstance(new ArrayList<>(), lang);
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
                return new DictInstance(new HashMap<Object, Object>(), lang);
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
                return new SamirPair(arguments.get(0), arguments.get(1), lang);
            }

            @Override
            public String toString() {
                return "<class Pair>";
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
                System.out.print(Language.stringify(arg));
                String input = lang.scanner.nextLine();
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
                return Language.stringify(arg);}
        });

        // cast to number function:
        globals.define("num", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(Language.isNumeric(arg.toString()))
                    return Double.parseDouble(arg.toString());
                else
                    Language.error("Cannot cast " + arg.toString() + " to a number", lang.line, lang.cur_file_name);

                // unreachable:
                return null;

                }

            

        });

        // return type of variable:
        globals.define("typeOf", new SamirCallable() {
            @Override
            public int arity() {return 1;}
            @Override
            public String call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg == null) return "nil";
                return switch(arg){
                    case String x -> "string";
                    case Double d -> "number";
                    case Boolean b -> "boolean";
                    case ListInstance l -> "List";
                    case SamirPair s -> "Pair";
                    case SamirClass c -> "class";
                    case SamirFunction f -> "function";
                    case SamirInstance s -> s.class_name;
                    case SamirLambda l -> "lambda";
                    case SamirPairList p -> "PairList";
                    default -> "native callable";
                };
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
                    return Language.isNumeric(arg.toString());
                else
                    return false;
            }
        });

        // The getTime() function:
        globals.define("getTime", new SamirCallable() {
            @Override
            public int arity() {return 0;}
            @Override
            public Double call(List<Object> arguments) {return ((Long) System.currentTimeMillis()).doubleValue();}

        });

        globals.define("len", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Double call(List<Object> arguments) {
                Object arg = arguments.get(0);
                int result = switch(arg){
                    case String s -> s.length();
                    case ListInstance l -> l.arrayList.size();
                    case DictInstance d -> d.hashMap.size();
                    case SamirPairList p -> p.list.size();
                    default -> -1;
                };

                if (result == -1)
                    Language.error("len() argument must be a list, string or Dict", lang.line, lang.cur_file_name);
                
                return Language.int_to_Double(result);
            }
            
        });

        globals.define("exit", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Void call(List<Object> arguments) {
                if(arguments.get(0).equals(0.0))
                    System.exit(0);
                System.out.println("Error: " + arguments.get(0));
                System.exit(1);
                return null;
            }
            
        });

        // Debug function:
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
                Object arg = arguments.get(0);
                check_type(arg, String.class, "fileExists() arg must be a file path");
                
                // Look in global dir:
                String string_path = (String) arg;
                Path path = Paths.get(string_path);
                if(path.isAbsolute() && Files.exists(path))
                    return true;
                
                // Look in local dir:
                Path parent_dir = Paths.get(lang.samir_script_filepath).getParent();
                path = parent_dir.resolve(path);
                return Files.exists(path);
            }
            
        });
        

        globals.define("read", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public String call(List<Object> arguments) {
                Object arg = arguments.get(0);
                check_type(arg, String.class, "read() arg must be a file path");
                String path = (String) arg;
                
                // Check if file is abs path:
                try{
                    byte[] bytes = Files.readAllBytes(Paths.get(path));
                    String text = new String(bytes, Charset.defaultCharset());
                    return text;
                }
                catch(IOException e){
                    try{
                        // Let's see if file is local:
                        String parent_dir = Paths.get(lang.samir_script_filepath, "").getParent().toString();
                        String local_path = parent_dir + "\\" + arguments.get(0);
                        byte[] bytes = Files.readAllBytes(Paths.get(local_path));
                        String text = new String(bytes, Charset.defaultCharset());
                        return text;
                    }
                    catch(IOException f){
                        Language.error("Could not find file: " + path, lang.line, lang.cur_file_name);
                    }
                }
                return null;

            }
            
        });

        globals.define("write", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Void call(List<Object> arguments) {
                Object arg = arguments.get(0);
                check_type(arg, String.class, "write() arg must be a file path");
                
                String string_path = (String) arg;
                String content = Language.stringify(arguments.get(1));
                
                Path path = Paths.get(string_path);
                // For local files:
                if( ! path.isAbsolute()){
                    Path parent_dir = Paths.get(lang.samir_script_filepath).getParent();
                    path = parent_dir.resolve(path);
                }
                try {
                    Files.write(path, content.getBytes());
                } catch (IOException e) {
                    Language.error("Couldn't write to file: " + path.toString(), lang.line, lang.cur_file_name);
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
                check_type(arg, ListInstance.class, "enumarate() arg must be a list");
                ListInstance listInstance = (ListInstance) arg;
                // Generate indices:
                Double[] indices_array = new Double[listInstance.arrayList.size()];
                for(int i = 0; i < listInstance.arrayList.size(); i++)
                    indices_array[i] = Language.int_to_Double(i);
                ListInstance indices_list = ListInstance.create_filled_list(indices_array, lang);
                return new SamirPairList(indices_list, listInstance, lang);
            }
            
        });

        globals.define("zip", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                check_type(arguments.get(0), ListInstance.class, "first zip arg must be a list");
                check_type(arguments.get(1), ListInstance.class, "second zip arg must be be list");
                ListInstance a = (ListInstance) arguments.get(0);
                ListInstance b = (ListInstance) arguments.get(1);
                return new SamirPairList(a, b, lang);
            }
            
        });

        globals.define("split", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public ListInstance call(List<Object> arguments) {
                check_type(arguments.get(0), String.class, "first arg of split() must be a string");
                check_type(arguments.get(1), String.class, "second arg of split() must be a string");
                String word = (String) arguments.get(0);
                String split = (String) arguments.get(1);
                return ListInstance.create_filled_list(word.split(split), lang);
            }
            
        });

        globals.define("substring", new SamirCallable() {

            @Override
            public int arity() {return 3;}

            @Override
            public Object call(List<Object> arguments) {
                check_type(arguments.get(0), String.class, "first substring() arg must be a string");
                check_type(arguments.get(1), Double.class, "second substring() arg must be a number");
                check_type(arguments.get(2), Double.class, "third substring() arg must be a number");
                String word = (String) arguments.get(0);
                Double start = (Double) arguments.get(1);
                Double end = (Double) arguments.get(2);
                if(start % 1 != 0 || end % 1 != 0)
                    Language.error("substring indices must be whole numbers", lang.line, lang.cur_file_name);
                return word.substring(start.intValue(), end.intValue());
            }
            
        });

        globals.define("eval", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                check_type(arguments.get(0), String.class, "eval() arg must be a string");
                String source = (String) arguments.get(0);
                Lexer lexer = new Lexer(source, lang.cur_file_name);
                lexer.line = lang.line;
                ArrayList<Token> tokens = lexer.lex();
                Parser parser = new Parser(tokens, lang);
                Expre expre = parser.expression();
                return expre.visit();
            }
            
        });

        globals.define("exec", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Void call(List<Object> arguments) {
                check_type(arguments.get(0), String.class, "exec() arg must be a string");
                if(Thread.currentThread().getStackTrace().length > 500)
                    Language.error("exec() caused a stack overflow, Remove any circular dependency.", lang.line, lang.cur_file_name);
                String source = (String) arguments.get(0);
                List<Stmt> statements = Language.lex_then_parse(source, lang, lang.cur_file_name);
                for (Stmt stmt : statements)
                    stmt.visit();
                return null;
            }

        });

        globals.define("range", new SamirCallable() {

            @Override
            public int arity() {return 3;}

            @Override
            public Object call(List<Object> arguments) {
                for (int i = 0; i <= 2; i++)
                    check_type(arguments.get(i), Double.class, "arg number " + (i+1) + " of range() must be a number");
                int from = ((Double) arguments.get(0)).intValue();
                int to = ((Double) arguments.get(1)).intValue();
                int step = ((Double) arguments.get(2)).intValue();
                if(step == 0)
                    Language.error("range() step cannot be 0", lang.line, lang.cur_file_name);
                var arraylist = new ArrayList<Double>();
                for (; step > 0 ? from < to : to < from; from += step)
                    arraylist.add(Language.int_to_Double(from));
                return ListInstance.create_filled_list(arraylist.toArray(), lang);
            }
        });

        globals.define("globals", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return new DictInstance(new HashMap<>(globals.variables), lang);
            }

        });

        globals.define("locals", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return new DictInstance(new HashMap<>(lang.environment.variables), lang);
            }

        });

        globals.define("abs", new SamirCallable(){

            @Override
            public int arity() { return 1;}

            @Override
            public Double call(List<Object> arguments) {
                check_type(arguments.get(0), Double.class, "abs() arg must be a number");
                return Math.abs( (Double) arguments.get(0));
            }

        });

        globals.define("pow", new SamirCallable(){

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                check_type(arguments.get(0), Double.class, "1st abs() arg must be a number");
                check_type(arguments.get(1), Double.class, "2nd abs() arg must be a number");
                return Math.pow((Double)arguments.get(0), (Double)arguments.get(1));
            }

        });

        globals.define("sqrt", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Double call(List<Object> arguments) {
                Double arg = (Double) arguments.get(0);
                check_type(arg, Double.class, "sqrt() arg must be a number");
                if(arg < 0)
                    Language.error("can't square root of negtaive number", lang.line, lang.cur_file_name);
                return Math.sqrt((Double)arg);
            }

        });
    }

}
