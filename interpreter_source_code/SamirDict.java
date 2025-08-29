import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamirDict extends SamirObject implements Subscriptable {

    HashMap<Object, Object> hashMap;
    Runtime lang;

    SamirDict(HashMap<Object, Object> hashMap, Runtime runtime){
        super(new Environment(runtime.environment), "Dict");
        this.hashMap = hashMap;
        this.lang = runtime;

        // Methods:
        this.environment.define("keys", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public SamirList call(List<Object> arguments) {
                return SamirList.create_filled_list(hashMap.keySet(), runtime);
            }
        });

        this.environment.define("items", new SamirCallable(){

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return new SamirPairList(SamirDict.this, runtime);
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
    public Object set_item(Object key, Object right_side, Token opp) {

        Object new_val = hashMap.containsKey(key) ?
        Util.run_compound_assignment(hashMap.get(key), right_side, opp, lang) :
        right_side;

        hashMap.put(key, new_val);
        return new_val;
    }


}
