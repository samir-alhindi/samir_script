import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SamirClass implements SamirCallable{
    Environment closure;
    ClassDeclre class_;
    List<Token> parameters;
    Function to_string;
    Language lang;
    SamirClass(ClassDeclre class_, Environment closure, Language lang){
        this.class_ = class_;
        this.parameters = class_.parameters;
        this.to_string = class_.to_string;
        this.closure = closure;
        this.lang = lang;
    }

    @Override
    public String toString() {
        return "<class " + class_.name.value + ">";
    }

    @Override
    public int arity() {
        return parameters.size();
    }

    @Override
    public Object call(List<Object> arguments) {
        // Check stack overflow:
        return new SamirInstance(this, arguments, lang);
    }
    
}

class SamirInstance{
    Environment environment;
    SamirClass class_;
    SamirCallable to_string_method_as_callable;

    SamirInstance(SamirClass class_, List<Object> constructer_args, Language lang){
        this.class_ = class_;
        this.environment = new Environment(class_.closure);
        Environment prev = lang.environment;
        lang.environment = this.environment;
        List<Stmt> bodyStatements = class_.class_.classBody;


        environment.define("self", this);
        for (Stmt stmt : bodyStatements)
            stmt.visit();

        for (int i = 0; i < constructer_args.size(); i++)
            this.environment.define(class_.parameters.get(i).value.toString(), constructer_args.get(i));
        

        if(class_.to_string != null){
            class_.to_string.visit();
            to_string_method_as_callable = (SamirCallable) environment.get(new Token(null, "_toString", 0));
        }
            
        lang.environment = prev;
    }


    // For inherting native classes:
    SamirInstance(Language lang){
        this.environment = new Environment(lang.environment);
    }

    @Override
    public String toString() {
        if(this.class_.to_string == null)
            return "<" + class_.class_.name.value + " instance " + this.hashCode() + ">";
        return Language.stringify(to_string_method_as_callable.call(null));
    }
}

interface NativeMethod {}

class ListInstance extends SamirInstance {

    ArrayList<Object> arrayList;
    Language lang;

    ListInstance(ArrayList<Object> arrayList, Language lang){
        super(lang);
        this.arrayList = arrayList;
        this.lang = lang;
        // This line exists so we can error report if the user tries to accsses a member that isn't in the List class:
        this.class_ = new SamirClass(new ClassDeclre(null, new Token(null, "List", 0), null, null, lang), null, lang);
        this.environment.define("size", 0.0);

        // Create append/add method for list object:
        this.environment.define("add", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Void call(List<Object> arguments) {

                arrayList.add(arguments.get(0));
                changeSize(1);
                return null;
            }

            @Override
            public String toString() {
                return "<function add>";
            }
            
        });

        // Create "set" method for list object:
        this.environment.define("set", new SamirCallable(){

            @Override
            public int arity() {return 2;}

            @Override
            public Void call(List<Object> arguments) {
                
                int index = checkValidIndex(arguments.get(0));
                arrayList.set(index, arguments.get(1));
                return null;
            }

        });

        // Create "get" method for list object:
        this.environment.define("get", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                int index = checkValidIndex(arguments.get(0));
                return arrayList.get(index);
            }

            @Override
            public String toString() {
                return "<function get>";
            }
            
        });

        this.environment.define("removeAt", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                int index = checkValidIndex(arguments.get(0));
                Object removed = arrayList.get(index);
                arrayList.remove(index);
                changeSize(-1);
                return removed;
        }});

        this.environment.define("pop", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                if(arrayList.size() == 0)
                    Language.error("Can't pop() from an empty list", lang.currentRunningLine);
                changeSize(-1);
                return arrayList.removeLast();
            }
        });

        this.environment.define("popFront", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                if(arrayList.size() == 0)
                    Language.error("Can't popFront() from an empty list", lang.currentRunningLine);
                changeSize(-1);
                return arrayList.removeFirst();
            }
        });

        this.environment.define("insert", new SamirCallable(){

            @Override
            public int arity() {return 2;}

            @Override
            public Void call(List<Object> arguments) {

                int index = checkValidIndex(arguments.get(0));
                arrayList.add(index, arguments.get(1));
                changeSize(1);
                return null;
            }

        });

        this.environment.define("copy", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public ListInstance call(List<Object> arguments) {
                ListInstance new_list = new ListInstance(new ArrayList<>(), lang);
                Object add_method_uncast = new_list.environment.variables.get("add");
                SamirCallable add_method = (SamirCallable) add_method_uncast;
                for (Object item : arrayList) {
                    List<Object> args = new ArrayList<>();
                    args.add(item);
                    add_method.call(args);
                }
                return new_list;
            }

        });

        this.environment.define("swap", new SamirCallable(){

            @Override
            public int arity() {return 2;}

            @Override
            public Object call(List<Object> arguments) {
                int index1 = checkValidIndex(arguments.get(0));
                int index2 = checkValidIndex(arguments.get(1));
                Object temp = arrayList.get(index2);
                arrayList.set(index2, arrayList.get(index1));
                arrayList.set(index1, temp);
                return null;
            }
            
        });

        this.environment.define("clear", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Void call(List<Object> arguments) {
                arrayList.clear();
                environment.assign(new Token(null,"size", 0), 0);
                return null;
        }});

    

        this.environment.define("fillRange", new SamirCallable(){

            @Override
            public int arity() {return 3;}

            @Override
            public Void call(List<Object> arguments) {
                Object arg1 = arguments.get(0);
                Object arg2 = arguments.get(1);
                Object arg3 = arguments.get(2);
                if(arg1 instanceof Double == false || arg2 instanceof Double == false || arg3 instanceof Double == false)
                    Language.error("all fillRange() args must be numbers", lang.currentRunningLine);
                int from = ((Double) arg1).intValue();
                int to = ((Double) arg2).intValue();
                int step = ((Double) arg3).intValue();
                int sizeChange = 0;
                if(step > 0)
                    for (   ; from < to; from += step, sizeChange ++)
                        arrayList.add( (Double) (double) from);
                else if(step < 0)
                    for (; from > to; from += step, sizeChange ++)
                        arrayList.add((Double) (double) from);
                
                changeSize(sizeChange);
                return null;
            }

        });

        this.environment.define("sortCustom", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Void call(List<Object> arguments) {
                if(arguments.get(0) instanceof SamirCallable == false)
                    Language.error("sortCustom() arg must be a callable", lang.currentRunningLine);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 1)
                    Language.error("sortCustom() arg must be a callable that takes exactly 1 arg", lang.currentRunningLine);
                for (int i = 0; i < arrayList.size(); i++) {
                    for (int j = 0; j < arrayList.size() - 1; j++) {
                        Object a = callable.call(Language.to_list(arrayList.get(j)));
                        Object b = callable.call(Language.to_list(arrayList.get(j+1)));
                        if(a instanceof Double == false || b instanceof Double == false)
                            Language.error("sortCustom() must return a number", lang.currentRunningLine);
                        if((Double) a > (Double) b){
                            Object temp = arrayList.get(j);
                            arrayList.set(j, arrayList.get(j+1));
                            arrayList.set(j+1, temp);
                        }
                    }
                }
                return null;
            }

        });

        this.environment.define("map", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public ListInstance call(List<Object> arguments) {
                if(arguments.get(0) instanceof SamirCallable == false)
                    Language.error("map() arg must be a callable (function name, lambda)", lang.currentRunningLine);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 1)
                    Language.error("map() arg must be a callable that takes exactly 1 arg", lang.currentRunningLine);
                for (int i = 0; i < arrayList.size(); i++)
                    arrayList.set(i, callable.call(Arrays.asList(arrayList.get(i))));
                return ListInstance.this;
            }

        });

        this.environment.define("filter", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof SamirCallable == false)
                    Language.error("filter() arg must be a callable (function name, lambda) ", lang.currentRunningLine);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 1)
                    Language.error("filter() arg must be a callable that takes exactly 1 arg", lang.currentRunningLine);
                var output = new ArrayList<Object>();
                for (Object item : arrayList){
                    Object result = callable.call(Arrays.asList(item));
                    if(result instanceof Boolean == false)
                        Language.error("filter() arg must be a callable that returns a boolean value (true or false)", lang.currentRunningLine);
                    if(((Boolean) result).equals(true))
                        output.add(item);
                }
                return ListInstance.create_filled_list(output.toArray(), lang);
            }

        });

        
        this.environment.define("reduce", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                if(arguments.get(0) instanceof SamirCallable == false)
                    Language.error("reduce() arg must be a callable (function, lambda)", lang.currentRunningLine);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 2)
                    Language.error("reduce() arg must be a callable that takes 2 args", lang.currentRunningLine);
                if(arrayList.size() < 2)
                    Language.error("List must be of at least length 2 in order to reduce", lang.currentRunningLine);
                Object output;
                output = callable.call(Arrays.asList(arrayList.get(0), arrayList.get(1)));
                for (int i = 2; i < arrayList.size(); i++)
                    output = callable.call(Arrays.asList(output, arrayList.get(i)));
                return output; 
            }

        });
        
        
    }

    @Override
    public String toString() {

        String result = "[";
        for (Object element : arrayList)
            result += Language.stringify(element) + ", ";
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + "]";
    }

    int checkValidIndex(Object object){
        if(object instanceof Double == false)
            Language.error("argument of this List method must be a number", lang.currentRunningLine);
        Double index = (Double) object;

        if(index % 1 != 0)
            Language.error("argument must be a whole number", lang.currentRunningLine);

        if(index >= arrayList.size())
            Language.error("index " + index.intValue() + " out of bounds for size " + arrayList.size(), lang.currentRunningLine);
        
        // Negative index:
        if(index < 0){
            Double actualIndex = index + arrayList.size();
            if(actualIndex < 0)
                Language.error("index " + index.intValue() + " out of bounds for size " + arrayList.size(), lang.currentRunningLine);
            index = actualIndex;
        }
        
        return index.intValue();
    }

    void changeSize(int amount){
        Object temp = environment.get(new Token(null, "size", 0));
        Double oldSize = (Double) temp;
        environment.assign(new Token(null, "size", 0), oldSize + amount);
    }

    Double getSize(){
        return (Double) (double) arrayList.size();
    }

    static ListInstance create_filled_list(Object[] items, Language lang){
        ListInstance list = new ListInstance(new ArrayList<>(), lang);
        Object add_method_uncast = list.environment.variables.get("add");
        SamirCallable add_method = (SamirCallable) add_method_uncast;
        for (Object item : items) {
            List<Object> arg = new ArrayList<>();
            arg.add(item);
            add_method.call(arg);
        }
        return list;
    }

}

class DictInstance extends SamirInstance {

    HashMap<Object, Object> hashMap;
    Language lang;

    DictInstance(HashMap<Object, Object> hashMap, Language lang){
        super(lang);
        this.hashMap = hashMap;
        this.lang = lang;
        // This line exists so we can error report if the user tries to accsses a member that isn't in the Dict class:
        this.class_ = new SamirClass(new ClassDeclre(null, new Token(null, "Dict", 0), null, null, lang), null, lang);

        // Methods:
        this.environment.define("keys", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public ListInstance call(List<Object> arguments) {
                return ListInstance.create_filled_list(hashMap.keySet().toArray(), lang);
            }
        });

        this.environment.define("items", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return new SamirPairList(DictInstance.this, lang);
            }

        });
    }

    @Override
    public String toString() {
        String result = "{";
        for (Map.Entry<Object, Object> pair : hashMap.entrySet()){
            Object key = pair.getKey();
            Object value = pair.getValue();
            result += Language.stringify(key) + " : " + Language.stringify(value) + ", ";
        }
            
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + "}";
    }


}

class SamirPairList {
    List<SamirPair> list;
    SamirPairList(DictInstance dict, Language lang){
        super();
        list = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : dict.hashMap.entrySet()){
            SamirPair pair = new SamirPair(entry.getKey(), entry.getValue(), lang);
            list.add(pair);
        }
        
    }

    SamirPairList(ListInstance a, ListInstance b, Language lang){
        super();
        list = new ArrayList<>();
        int size = Math.min(a.arrayList.size(), b.arrayList.size());
        for(int i = 0; i < size; i++)
            list.add(new SamirPair(a.arrayList.get(i), b.arrayList.get(i), lang));
    }

    @Override
    public String toString() {
        String result = "(";
        for (SamirPair pair : list){
            result += "(" + Language.stringify(pair.first) + ", " + Language.stringify(pair.second) + ")" + ", ";
        }
            
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + ")";
    }
}


 class SamirPair extends SamirInstance{
    Object first;
    Object second;
    SamirPair(Object first, Object second, Language lang){
        super(lang);
        this.first = first;
        this.second = second;
        environment.define("first", first);
        environment.define("second", second);
    }

    @Override
    public String toString() {
        return "(" + Language.stringify(first) + ", " + Language.stringify(second) + ")";
    }
 }

 class Importinstance extends SamirInstance {
    Importinstance(Language lang, Token import_name){
        super(lang);
        this.environment = lang.environment;
        // This line exists so we can error report if the user tries to accsses a member that isn't in the file:
        this.class_ = new SamirClass(new ClassDeclre(null, new Token(null, (String) import_name.value, 0), null, null, lang), null, lang); 
    }
 }