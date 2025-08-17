import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                    Language.error("Can't pop() from an empty list", lang.line, lang.cur_file_name);
                return arrayList.removeLast();
            }
        });

        this.environment.define("popFront", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                if(arrayList.size() == 0)
                    Language.error("Can't popFront() from an empty list", lang.line, lang.cur_file_name);;
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
                for (Object item : arrayList)
                    new_list.arrayList.add(item);
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
                Collections.swap(arrayList, index1, index2);
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
            public ListInstance call(List<Object> arguments) {
                NativeFunctions.check_interface(arguments.get(0), SamirCallable.class, "sortCustom() arg must be a callable", lang.line, lang.cur_file_name);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 1)
                    Language.error("sortCustom() arg must be a callable that takes exactly 1 arg", lang.line, lang.cur_file_name);
                
                for (int i = 0; i < arrayList.size(); i++) {
                    for (int j = 0; j < arrayList.size() - 1; j++) {
                        Object a = callable.call(Language.to_list(arrayList.get(j)));
                        Object b = callable.call(Language.to_list(arrayList.get(j+1)));
                        if(a instanceof Double == false || b instanceof Double == false)
                            Language.error("sortCustom() must return a number", lang.line, lang.cur_file_name);
                        if((Double) a > (Double) b)
                            Collections.swap(arrayList, j, j+1);
                    }
                }
                return ListInstance.this;
            }

        });

        this.environment.define("map", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public ListInstance call(List<Object> arguments) {
                NativeFunctions.check_interface(arguments.get(0), SamirCallable.class, "map() arg must be a callable (function name, lambda)", lang.line, lang.cur_file_name);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 1)
                    Language.error("map() arg must be a callable that takes exactly 1 arg", lang.line, lang.cur_file_name);
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
                NativeFunctions.check_interface(arguments.get(0), SamirCallable.class, "filter() arg must be a callable (function name, lambda)", lang.line, lang.cur_file_name);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 1)
                    Language.error("filter() arg must be a callable that takes exactly 1 arg", lang.line, lang.cur_file_name);
                var output = new ArrayList<Object>();
                for (Object item : arrayList){
                    Object result = callable.call(Arrays.asList(item));
                    NativeFunctions.check_type(result, Boolean.class, "filter() arg must be a callable that returns a boolean value (true or false)", lang.line, lang.cur_file_name);
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
                NativeFunctions.check_interface(arguments.get(0), SamirCallable.class, "reduce() arg must be a callable (function, lambda)", lang.line, lang.cur_file_name);
                SamirCallable callable = (SamirCallable) arguments.get(0);
                if(callable.arity() != 2)
                    Language.error("reduce() arg must be a callable that takes 2 args", lang.line, lang.cur_file_name);
                if(arrayList.size() < 2)
                    Language.error("List must be of at least length 2 in order to reduce", lang.line, lang.cur_file_name);
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
        NativeFunctions.check_type(object, Double.class, "argument of this List method must be a number", lang.line, lang.cur_file_name);
        Double index = (Double) object;

        if(index % 1 != 0)
            Language.error("argument must be a whole number", lang.line, lang.cur_file_name);

        if(index >= arrayList.size())
            Language.error("index " + index.intValue() + " out of bounds for size " + arrayList.size(), lang.line, lang.cur_file_name);
        
        // Negative index:
        if(index < 0){
            Double actualIndex = index + arrayList.size();
            if(actualIndex < 0)
                Language.error("index " + index.intValue() + " out of bounds for size " + arrayList.size(), lang.line, lang.cur_file_name);
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