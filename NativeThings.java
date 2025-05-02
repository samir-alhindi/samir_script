import java.util.ArrayList;
import java.util.List;

public class NativeThings {
    
    void createListClass(){

        NativeSamirClass listClass = new NativeSamirClass(new ArrayList<Object>());
        
        listClass.environment.define("append", new SamirCallable(){

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Void call(List<Object> arguments) {
                Object item = arguments.get(0);
                ((List<Object>) listClass.data_struct).add(item);
                return null;
            }

        });
        Language.globals.define("List", listClass);
    }
}
