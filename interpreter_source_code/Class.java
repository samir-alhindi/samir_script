import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
class SamirClass extends SamirInstance implements SamirCallable{
    Environment closure;
    ClassDeclre declaration;
    List<Token> parameters;
    Function to_string;
    Function eq;
    Function call;
    Runtime lang;
    ListInstance all_Instances;
    SamirClass(ClassDeclre declaration, Environment closure, Runtime lang){
        super(lang);
        this.class_name = declaration.name.value.toString();
        this.declaration = declaration;
        this.parameters = declaration.parameters;
        this.to_string = declaration.to_string;
        this.eq = declaration.eq;
        this.call = declaration.call;
        this.closure = closure;
        this.lang = lang;
        all_Instances = new ListInstance(new ArrayList<>(), lang);

        environment.define("name", class_name);
        environment.define("get_all_instances", new SamirCallable(){

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public ListInstance call(List<Object> arguments) {
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

class SamirInstance implements SamirCallable{
    Environment environment;
    SamirClass samir_class;
    String class_name;

    SamirInstance(SamirClass samir_class, List<Object> constructer_args, Runtime lang){
        this.samir_class = samir_class;
        this.environment = new Environment(samir_class.closure);
        this.class_name = samir_class.declaration.name.value.toString();
        Environment prev = lang.environment;
        lang.environment = this.environment;
        List<Stmt> bodyStatements = samir_class.declaration.classBody;


        environment.define("self", this);
        environment.define("class_", samir_class);
        for (int i = 0; i < constructer_args.size(); i++)
            this.environment.define(samir_class.parameters.get(i).value.toString(), constructer_args.get(i));
        for (Stmt stmt : bodyStatements)
            stmt.visit();

        if(samir_class.to_string != null)
            samir_class.to_string.visit();
        if(samir_class.eq != null)
            samir_class.eq.visit();
        if(samir_class.call != null)
            samir_class.call.visit();
        

        lang.environment = prev;
    }


    // For inherting native classes:
    SamirInstance(Runtime lang){
        this.environment = new Environment(lang.environment);
    }

    @Override
    public String toString() {
        if(this.samir_class.to_string == null)
            return "<" + samir_class.class_name + " instance " + this.hashCode() + ">";
        SamirCallable to_string = (SamirCallable) environment.variables.get("__str__");
        return Util.stringify(to_string.call(null));
    }

    @Override
    public boolean equals(Object obj) {
        if(samir_class == null || samir_class.eq == null)
            return super.equals(obj);
        SamirCallable eq = (SamirCallable) environment.variables.get("__eq__");
        if(eq.arity() != 1)
            Runtime.error("__eq__() must take exactly 1 argument", samir_class.lang.line, samir_class.declaration.name.file_name);
        Object result = eq.call(Arrays.asList(obj));
        NativeFunctions.check_type(result, Boolean.class, "__eq__() must return a boolean value", samir_class.lang.line, samir_class.lang.cur_file_name);
        return (Boolean) result;
    }


    // For class that have the "__call__" method:-
    @Override
    public int arity() {
        if(samir_class.call == null)
            Runtime.error("cannot call instance of class '" + class_name + "' because it doesn't implement __call__() ", samir_class.lang.line, samir_class.lang.cur_file_name);
        return ((SamirCallable) environment.variables.get("__call__")).arity();
    }


    @Override
    public Object call(List<Object> arguments) {
        SamirCallable call = (SamirCallable) environment.variables.get("__call__");
        return call.call(arguments);
        
    }
}

 class Importinstance extends SamirInstance {
    Importinstance(Runtime lang, Token import_name){
        super(lang);
        this.class_name = import_name.value.toString();
    }
 }