import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SamirPairList implements Subscriptable{
    List<SamirPair> list;
    Runtime lang;
    SamirPairList(DictInstance dict, Runtime lang){
        list = new ArrayList<>();
        this.lang = lang;
        for (Map.Entry<Object, Object> entry : dict.hashMap.entrySet()){
            SamirPair pair = new SamirPair(entry.getKey(), entry.getValue(), lang);
            list.add(pair);
        }
        
    }

    SamirPairList(ListInstance a, ListInstance b, Runtime lang){
        list = new ArrayList<>();
        this.lang = lang;
        int size = Math.min(a.arrayList.size(), b.arrayList.size());
        for(int i = 0; i < size; i++)
            list.add(new SamirPair(a.arrayList.get(i), b.arrayList.get(i), lang));
    }

    @Override
    public String toString() {
        String result = "(";
        for (SamirPair pair : list){
            result += "(" + Util.stringify(pair.first) + ", " + Util.stringify(pair.second) + ")" + ", ";
        }
            
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + ")";
    }

    @Override
    public SamirPair get_item(Object index) {
        int i = Util.checkValidIndex(index, list.size(), lang);
        return list.get(i);
    }

    @Override
    public SamirPair set_item(Object index, Object item) {
        int i = Util.checkValidIndex(index, list.size(), lang);
        SamirPair pair = NativeFunctions.check_type(item, SamirPair.class, "PairLists can only contain Pairs", lang.line, lang.cur_file_name);
        list.set(i, pair);
        return pair;
    }
}


 class SamirPair extends SamirInstance {
    Object first;
    Object second;
    SamirPair(Object first, Object second, Runtime lang){
        super(lang);
        this.first = first;
        this.second = second;
        this.class_name = "Pair";
        environment.define("first", first);
        environment.define("second", second);
    }

    @Override
    public String toString() {
        return "(" + Util.stringify(first) + ", " + Util.stringify(second) + ")";
    }
 }