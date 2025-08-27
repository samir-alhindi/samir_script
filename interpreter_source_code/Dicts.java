import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DictInstance extends SamirInstance implements Subscriptable {

    HashMap<Object, Object> hashMap;
    Runtime lang;

    DictInstance(HashMap<Object, Object> hashMap, Runtime runtime){
        super(runtime);
        this.hashMap = hashMap;
        this.lang = runtime;
        class_name = "Dict";

        // Methods:
        this.environment.define("keys", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public ListInstance call(List<Object> arguments) {
                return ListInstance.create_filled_list(hashMap.keySet(), runtime);
            }
        });

        this.environment.define("items", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return new SamirPairList(DictInstance.this, runtime);
            }

        });
    }

    @Override
    public String toString() {
        String result = "{";
        for (Map.Entry<Object, Object> pair : hashMap.entrySet()){
            Object key = pair.getKey();
            Object value = pair.getValue();
            result += Util.stringify(key) + " : " + Util.stringify(value) + ", ";
        }
            
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + "}";
    }

    @Override
    public Object get_item(Object key) {
        return (hashMap.containsKey(key)) ? hashMap.get(key) : Runtime.error("key '" + key + "' not found in Dict", lang.line, lang.cur_file_name);
    }

    @Override
    public Object set_item(Object key, Object item) {
        hashMap.put(key, item);
        return item;
    }


}
