import java.util.ArrayList;
import java.util.List;


public class SamirClass extends SamirObject implements SamirCallable{
    Environment closure;
    ClassDeclre declaration;
    List<Token> parameters;
    Function to_string;
    Function eq;
    Function call;
    Runtime lang;
    SamirList all_Instances;
    SamirClass(ClassDeclre declaration, Environment closure, Runtime lang){
        super(new Environment(), "class");
        this.declaration = declaration;
        this.parameters = declaration.parameters;
        this.to_string = declaration.to_string;
        this.eq = declaration.eq;
        this.call = declaration.call;
        this.closure = closure;
        this.lang = lang;
        all_Instances = new SamirList(new ArrayList<>(), lang);

        environment.define("name", declaration.name.value.toString());
        environment.define("get_all_instances", new SamirCallable(){

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public SamirList call(List<Object> arguments) {
                return all_Instances;
            }

        });

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
        SamirInstance instance = new SamirInstance(this, arguments, lang);
        all_Instances.arrayList.add(instance);
        return instance;
    }
    
}

 class SamirImport extends SamirObject {
    SamirImport(Runtime runtime, Token import_name){
        super(new Environment(runtime.environment), import_name.value.toString());
    }
}