import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SamirPairList extends SamirObject implements Subscriptable{
    List<SamirPair> list;
    Runtime runtime;
    SamirPairList(SamirDict dict, Runtime runtime){
        super(new Environment(runtime.environment), "PairList");
        list = new ArrayList<>();
        this.runtime = runtime;
        for (Map.Entry<Object, Object> entry : dict.hashMap.entrySet()){
            SamirPair pair = new SamirPair(entry.getKey(), entry.getValue(), runtime);
            list.add(pair);
        }
        
    }

    SamirPairList(SamirList a, SamirList b, Runtime runtime){
        super(new Environment(runtime.environment), "PairList");
        list = new ArrayList<>();
        this.runtime = runtime;
        int size = Math.min(a.arrayList.size(), b.arrayList.size());
        for(int i = 0; i < size; i++)
            list.add(new SamirPair(a.arrayList.get(i), b.arrayList.get(i), runtime));
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
        int i = Util.checkValidIndex(index, list.size(), runtime);
        return list.get(i);
    }

    @Override
    public SamirPair set_item(Object index, Object item) {
        int i = Util.checkValidIndex(index, list.size(), runtime);
        SamirPair pair = Util.check_type(item, SamirPair.class, "PairLists can only contain Pairs", runtime.line, runtime.cur_file_name);
        list.set(i, pair);
        return pair;
    }
}


 class SamirPair extends SamirObject {
    Object first;
    Object second;
    SamirPair(Object first, Object second, Runtime runtime){
        super(new Environment(runtime.environment), "Pair");
        this.first = first;
        this.second = second;
        environment.define("first", first);
        environment.define("second", second);
    }

    @Override
    public String toString() {
        return "(" + Util.stringify(first) + ", " + Util.stringify(second) + ")";
    }
 }