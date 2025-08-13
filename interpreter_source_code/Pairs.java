import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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