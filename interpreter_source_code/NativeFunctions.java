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

    <T> T check_type(Object object, Class<T> type, String log){
        if(object == null || object.getClass() != type)
            Language.error(log, lang.line, lang.cur_file_name);
        return (T) object;
    }

    static <T> T check_type(Object object, Class<?> type, String log, int line, String file_name){
        if(object == null || object.getClass() != type)
            Language.error(log, line, file_name);
        return (T) object;
    }

    static <T> T check_interface(Object object, Class<?> the_interface, String log, int line, String file_name){
            for (Class<?> c : object.getClass().getInterfaces())
                if (c == the_interface)
                    return (T) object;
            Language.error(log, line, file_name);
            return null;
        }

    <T> T check_interface(Object object, Class<T> the_interface, String log){
        for (Class<?> c : object.getClass().getInterfaces())
            if (c == the_interface)
                return (T) object;
        Language.error(log, lang.line, lang.cur_file_name);
        return null;
        
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
                    return Language.typeOf(arguments.get(0));
                };
            }

        );

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
                String string_path = check_type(arg, String.class, "fileExists() arg must be a file path");
                
                // Look in global dir:
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
                String path = check_type(arg, String.class, "read() arg must be a file path");
                
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
                String string_path =  check_type(arg, String.class, "write() arg must be a file path");
                
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
                ListInstance listInstance = check_type(arg, ListInstance.class, "enumarate() arg must be a list");
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
                ListInstance a =  check_type(arguments.get(0), ListInstance.class, "first zip arg must be a list");
                ListInstance b = check_type(arguments.get(1), ListInstance.class, "second zip arg must be be list");
                return new SamirPairList(a, b, lang);
            }
            
        });

        globals.define("split", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public ListInstance call(List<Object> arguments) {
                String word = check_type(arguments.get(0), String.class, "first arg of split() must be a string");
                String split = check_type(arguments.get(1), String.class, "second arg of split() must be a string");
                return ListInstance.create_filled_list(word.split(split), lang);
            }
            
        });

        globals.define("substring", new SamirCallable() {

            @Override
            public int arity() {return 3;}

            @Override
            public Object call(List<Object> arguments) {
                String word = check_type(arguments.get(0), String.class, "first substring() arg must be a string");
                Double start = check_type(arguments.get(1), Double.class, "second substring() arg must be a number");
                Double end = check_type(arguments.get(2), Double.class, "third substring() arg must be a number");
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
                String source = check_type(arguments.get(0), String.class, "eval() arg must be a string");
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
                String source = check_type(arguments.get(0), String.class, "exec() arg must be a string");
                if(Thread.currentThread().getStackTrace().length > 500)
                    Language.error("exec() caused a stack overflow, Remove any circular dependency.", lang.line, lang.cur_file_name);
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
                int from = check_type(arguments.get(0), Double.class, "arg number 1 of range() must be a number").intValue();
                int to = check_type(arguments.get(1), Double.class, "arg number 2 of range() must be a number").intValue();;
                int step = check_type(arguments.get(2), Double.class, "arg number 3 of range() must be a number").intValue();
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
                Double num = check_type(arguments.get(0), Double.class, "abs() arg must be a number");
                return Math.abs(num);
            }

        });

        globals.define("pow", new SamirCallable(){

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                Double base = check_type(arguments.get(0), Double.class, "1st abs() arg must be a number");
                Double power = check_type(arguments.get(1), Double.class, "2nd abs() arg must be a number");
                return Math.pow(base, power);
            }

        });

        globals.define("sqrt", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Double call(List<Object> arguments) {
                Double num = check_type(arguments.get(0), Double.class, "sqrt() arg must be a number");
                if(num < 0)
                    Language.error("can't square root of negtaive number", lang.line, lang.cur_file_name);
                return Math.sqrt(num);
            }

        });

        globals.define("arity", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Double call(List<Object> arguments) {
                SamirCallable callable = check_interface(arguments.get(0), SamirCallable.class, "arity() takes a callable (function, lambda, or class)");
                return Language.int_to_Double(callable.arity());
            }
            
        });

        globals.define("func_name", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public String call(List<Object> arguments) {
                SamirFunction func = check_type(arguments.get(0), SamirFunction.class, "func_name() arg must be a function");
                return func.declaration.name.value.toString();
            }

        });

        globals.define("get_closure", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public DictInstance call(List<Object> arguments) {
                Object arg = arguments.get(0);
                return switch(arg){
                    case SamirFunction f -> new DictInstance(new HashMap<>(f.closure.variables), lang);
                    case SamirLambda l -> new DictInstance(new HashMap<>(l.closure.variables), lang);
                    default -> (DictInstance) Language.error("Cannot get closure for the type: " + Language.typeOf(arg), lang.line, lang.cur_file_name);
                };
            }
        });

        globals.define("parameter_names", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public ListInstance call(List<Object> arguments) {
                Object arg = arguments.get(0);
                switch(arg){
                    case SamirFunction f -> {return ListInstance.create_filled_list(f.parameter_names, lang);}
                    case SamirLambda l -> {return ListInstance.create_filled_list(l.parameter_names, lang);}
                    default -> {return (ListInstance) Language.error("Cannot get parameter names for type: " + Language.typeOf(arg), lang.line, lang.cur_file_name);}
                }
            }

        });

        globals.define("bind", new SamirCallable() {

            @Override
            public int arity() {return 2;}

            @Override
            public SamirCallable call(List<Object> arguments) {
                Object arg = arguments.get(0);
                Object bound = arguments.get(1);
                switch(arg){
                    case SamirFunction f -> {
                        if(f.arity() == 0)
                            Language.error("cannot bind the function '" + f.declaration.name.value + "' if it takes zero parameters.", lang.line, lang.cur_file_name);
                        Environment new_closure = new Environment(f.closure);
                        Function new_declre = new Function(f.declaration.name, f.declaration.parameters.subList(1, f.declaration.parameters.size()), f.declaration.body, lang);
                        SamirFunction new_func = new SamirFunction(new_declre, new_closure, lang);
                        new_closure.define(f.parameter_names.get(0), bound);
                        return new_func;
                    }
                    case SamirLambda l -> {
                        if(l.arity() == 0)
                            Language.error("cannot bind a lambda if it takes zero parameters.", lang.line, lang.cur_file_name);
                        Environment new_closure = new Environment(l.closure);
                        Lambda new_declre = new Lambda(l.declaration.token, l.declaration.parameters.subList(1, l.declaration.parameters.size()), l.declaration.body, lang);
                        SamirLambda new_lambda = new SamirLambda(new_declre, new_closure, lang);
                        new_closure.define(l.parameter_names.get(0), bound);
                        return new_lambda;
                    }
                    default -> {
                        return (SamirCallable) Language.error("bind() arg must be a function or lambda, Not a:"+Language.typeOf(arg), lang.line, lang.cur_file_name);
                    }
                }
            }
        });
    }

}
