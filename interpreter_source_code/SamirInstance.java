import java.util.Arrays;
import java.util.List;

public class SamirInstance extends SamirObject implements SamirCallable{
    SamirClass samir_class;

    SamirInstance(SamirClass samir_class, List<Object> constructer_args, Runtime lang){
        super(new Environment(samir_class.closure),
            samir_class.declaration.name.value.toString());
        this.samir_class = samir_class;

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

    @Override
    public String toString() {
        if(this.samir_class.to_string == null)
            return "<" + "change me" + " instance " + this.hashCode() + ">";
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
        Util.check_type(result, Boolean.class, "__eq__() must return a boolean value", samir_class.lang.line, samir_class.lang.cur_file_name);
        return (Boolean) result;
    }


    // For class that have the "__call__" method:-
    @Override
    public int arity() {
        if(samir_class.call == null)
            Runtime.error("cannot call instance of class '" + type + "' because it doesn't implement __call__() ", samir_class.lang.line, samir_class.lang.cur_file_name);
        return ((SamirCallable) environment.variables.get("__call__")).arity();
    }


    @Override
    public Object call(List<Object> arguments) {
        SamirCallable call = (SamirCallable) environment.variables.get("__call__");
        return call.call(arguments);
        
    }
}