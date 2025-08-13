import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SamirClass implements SamirCallable{
    Environment closure;
    ClassDeclre declaration;
    List<Token> parameters;
    Function to_string;
    Language lang;
    SamirClass(ClassDeclre declaration, Environment closure, Language lang){
        this.declaration = declaration;
        this.parameters = declaration.parameters;
        this.to_string = declaration.to_string;
        this.closure = closure;
        this.lang = lang;
    }

    @Override
    public String toString() {
        return "<class " + declaration.name.value + ">";
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
    SamirClass samir_class;
    String class_name;

    SamirInstance(SamirClass samir_class, List<Object> constructer_args, Language lang){
        this.samir_class = samir_class;
        this.environment = new Environment(samir_class.closure);
        this.class_name = samir_class.declaration.name.value.toString();
        Environment prev = lang.environment;
        lang.environment = this.environment;
        List<Stmt> bodyStatements = samir_class.declaration.classBody;


        environment.define("self", this);
        for (int i = 0; i < constructer_args.size(); i++)
            this.environment.define(samir_class.parameters.get(i).value.toString(), constructer_args.get(i));
        for (Stmt stmt : bodyStatements)
            stmt.visit();

        if(samir_class.to_string != null)
            samir_class.to_string.visit();
        

        lang.environment = prev;
    }


    // For inherting native classes:
    SamirInstance(Language lang){
        this.environment = new Environment(lang.environment);
    }

    @Override
    public String toString() {
        if(this.samir_class.to_string == null)
            return "<" + samir_class.declaration.name + " instance " + this.hashCode() + ">";
        SamirCallable to_string = (SamirCallable) environment.variables.get("_toString");
        return Language.stringify(to_string.call(null));
    }
}

class ListInstance extends SamirInstance {

    ArrayList<Object> arrayList;
    Language lang;

    ListInstance(ArrayList<Object> arrayList, Language lang){
        super(lang);
        this.arrayList = arrayList;
        this.lang = lang;
        this.class_name = "List";

        // Create append/add method for list object:
        this.environment.define("add", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Void call(List<Object> arguments) {

                arrayList.add(arguments.get(0));
                return null;
            }

            @Override
            public String toString() {
                return "<function add>";
            }
            
        });


        this.environment.define("removeAt", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                int index = checkValidIndex(arguments.get(0));
                Object removed = arrayList.get(index);
                arrayList.remove(index);;
                return removed;
        }});

        this.environment.define("pop", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                if(arrayList.size() == 0)
                    Language.error("Can't pop() from an empty list", lang.currentRunningLine);;
                return arrayList.removeLast();
            }
        });

        this.environment.define("popFront", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                if(arrayList.size() == 0)
                    Language.error("Can't popFront() from an empty list", lang.currentRunningLine);;
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
                return null;
        }});

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
        class_name = "Dict";

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
        list = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : dict.hashMap.entrySet()){
            SamirPair pair = new SamirPair(entry.getKey(), entry.getValue(), lang);
            list.add(pair);
        }
        
    }

    SamirPairList(ListInstance a, ListInstance b, Language lang){
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
        this.class_name = "Pair";
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
        this.class_name = import_name.value.toString();
    }
 }