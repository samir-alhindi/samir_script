import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NativeFunctions {
    // Native functions, classes and variables:
    static void init(Environment globals, Language lang){
        
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
                    Language.error("Cannot cast " + arg.toString() + " to a number", lang.currentRunningLine);

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
                    return ((SamirInstance)arg).samir_class.declaration.name.value.toString();
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
            public Object call(List<Object> arguments) {return ((Long) System.currentTimeMillis()).doubleValue();}

        });

        // string methods:

        // getChar string method:
        globals.define("getChar", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                List<Object> index_and_string = lang.checkStringIndex(arguments.get(0), arguments.get(1));
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
                    return Language.int_to_Double(((String)arg).length());
                else if(arg instanceof ListInstance)
                    return Language.int_to_Double(((ListInstance)arg).arrayList.size());
                else if(arg instanceof DictInstance)
                    return Language.int_to_Double(((DictInstance)arg).hashMap.size());
                else if(arg instanceof SamirPairList)
                    return Language.int_to_Double(((SamirPairList) arg).list.size());
                
                Language.error("len() argument must be a list, string or Dict", lang.currentRunningLine);
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
                    Language.error("fileExists() arg must be a file path", lang.currentRunningLine);
                
                // Look in global dir:
                String string_path = (String) arguments.get(0);
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
                if(arguments.get(0) instanceof String == false)
                    Language.error("read() arg must be a file path", lang.currentRunningLine);
                String path = (String) arguments.get(0);
                
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
                        Language.error("Could not find file: " + path, lang.currentRunningLine);
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
                if(arguments.get(0) instanceof String == false)
                    Language.error("write() arg must be a file path", lang.currentRunningLine);
                
                String string_path = (String) arguments.get(0);
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
                    Language.error("Couldn't write to file: " + path.toString(), lang.currentRunningLine);
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
                    Language.error("enumarate() arg must be a list", lang.currentRunningLine);
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
                if(arguments.get(0) instanceof ListInstance == false
                || arguments.get(1) instanceof ListInstance == false)
                    Language.error("zip args must both be lists", lang.currentRunningLine);
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
                if(arguments.get(0) instanceof String == false || arguments.get(0) instanceof String == false)
                    Language.error("args of split() must be a string", lang.currentRunningLine);
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
                if(arguments.get(0) instanceof String == false
                || arguments.get(1) instanceof Double == false
                || arguments.get(2) instanceof Double == false)
                    Language.error("substring args must be (string, number, number)", lang.currentRunningLine);
                String word = (String) arguments.get(0);
                Double start = (Double) arguments.get(1);
                Double end = (Double) arguments.get(2);
                if(start % 1 != 0 || end % 1 != 0)
                    Language.error("substring indices must be whole numbers", lang.currentRunningLine);
                return word.substring(start.intValue(), end.intValue());
            }
            
        });

        globals.define("eval", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof String == false)
                    Language.error("eval() arg must be a string", lang.currentRunningLine);
                String source = (String) arguments.get(0);
                Lexer lexer = new Lexer(source);
                lexer.line = lang.currentRunningLine;
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
                if(arguments.get(0) instanceof String == false)
                    Language.error("exec() arg must be a string", lang.currentRunningLine);
                if(Thread.currentThread().getStackTrace().length > 500)
                    Language.error("exec() caused a stack overflow, Remove any circular dependency.", lang.currentRunningLine);
                String source = (String) arguments.get(0);
                Lexer lexer = new Lexer(source);
                lexer.line = lang.currentRunningLine;
                ArrayList<Token> tokens = lexer.lex();
                Parser parser = new Parser(tokens, lang);
                List<Stmt> program = parser.parse();
                for (Stmt stmt : program)
                    stmt.visit();
                return null;
                
            }

        });

        globals.define("range", new SamirCallable() {

            @Override
            public int arity() {return 3;}

            @Override
            public Object call(List<Object> arguments) {
                for (Object item : arguments)
                    if(item instanceof Double == false)
                        Language.error("range() args must all be numbers", lang.currentRunningLine);
                int from = ((Double) arguments.get(0)).intValue();
                int to = ((Double) arguments.get(1)).intValue();
                int step = ((Double) arguments.get(2)).intValue();
                if(step == 0)
                    Language.error("range() step cannot be 0", lang.currentRunningLine);
                var arraylist = new ArrayList<Double>();
                for (; step > 0 ? from < to : to < from; from += step)
                    arraylist.add(Language.int_to_Double(from));
                return ListInstance.create_filled_list(arraylist.toArray(), lang);
            }
            
        });
    }

}
