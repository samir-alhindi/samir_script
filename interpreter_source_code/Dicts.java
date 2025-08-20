import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                return ListInstance.create_filled_list(hashMap.keySet(), lang);
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
