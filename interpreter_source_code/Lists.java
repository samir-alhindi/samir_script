import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ListInstance extends SamirInstance implements Subscriptable{

    ArrayList<Object> arrayList;
    Runtime lang;

    ListInstance(ArrayList<Object> arrayList, Runtime lang){
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
            
        });


        this.environment.define("removeAt", new SamirCallable(){

            @Override
            public int arity() {return 1;}

            @Override
            public Object call(List<Object> arguments) {
                int index = Util.checkValidIndex(arguments.get(0), arrayList.size(), lang);
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
                    Runtime.error("Can't pop() from an empty list", lang.line, lang.cur_file_name);
                return arrayList.removeLast();
            }
        });

        this.environment.define("popFront", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                if(arrayList.size() == 0)
                    Runtime.error("Can't popFront() from an empty list", lang.line, lang.cur_file_name);;
                return arrayList.removeFirst();
            }
        });

        this.environment.define("insert", new SamirCallable(){

            @Override
            public int arity() {return 2;}

            @Override
            public Void call(List<Object> arguments) {

                int index = Util.checkValidIndex(arguments.get(0), arrayList.size(), lang);
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
                int index1 = Util.checkValidIndex(arguments.get(0), arrayList.size(), lang);
                int index2 = Util.checkValidIndex(arguments.get(1), arrayList.size(), lang);
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
                    Runtime.error("sortCustom() arg must be a callable that takes exactly 1 arg", lang.line, lang.cur_file_name);
                
                for (int i = 0; i < arrayList.size(); i++) {
                    for (int j = 0; j < arrayList.size() - 1; j++) {
                        Object a = callable.call(Util.to_list(arrayList.get(j)));
                        Object b = callable.call(Util.to_list(arrayList.get(j+1)));
                        if(a instanceof Double == false || b instanceof Double == false)
                            Runtime.error("sortCustom() must return a number", lang.line, lang.cur_file_name);
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
                    Runtime.error("map() arg must be a callable that takes exactly 1 arg", lang.line, lang.cur_file_name);
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
                    Runtime.error("filter() arg must be a callable that takes exactly 1 arg", lang.line, lang.cur_file_name);
                var output = new ArrayList<Object>();
                for (Object item : arrayList){
                    Object result = callable.call(Arrays.asList(item));
                    NativeFunctions.check_type(result, Boolean.class, "filter() arg must be a callable that returns a boolean value (true or false)", lang.line, lang.cur_file_name);
                    if(((Boolean) result).equals(true))
                        output.add(item);
                }
                return ListInstance.create_filled_list(output, lang);
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
                    Runtime.error("reduce() arg must be a callable that takes 2 args", lang.line, lang.cur_file_name);
                if(arrayList.size() < 2)
                    Runtime.error("List must be of at least length 2 in order to reduce", lang.line, lang.cur_file_name);
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
            result += Util.stringify(element) + ", ";
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + "]";
    }

    Double getSize(){
        return (Double) (double) arrayList.size();
    }

    static <T> ListInstance create_filled_list(Iterable<T> items, Runtime lang){
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

    static <T> ListInstance create_filled_list(T[] items, Runtime lang){
        return ListInstance.create_filled_list(Arrays.asList(items), lang);
    }

    @Override
    public Object get_item(Object index) {
        int i = Util.checkValidIndex(index, arrayList.size(), lang);
        return arrayList.get(i);
    }

    @Override
    public Object set_item(Object index, Object item) {
        int i = Util.checkValidIndex(index, arrayList.size(), lang);
        arrayList.set(i, item);
        return item;
    }

}