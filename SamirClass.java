import java.util.ArrayList;
import java.util.List;

class SamirClass implements SamirCallable{
    Environment closure;
    ClassDeclre class_;
    Object data_struct;
    SamirClass(ClassDeclre class_, Environment closure){
        this.class_ = class_;
        this.closure = closure;
    }

    @Override
    public String toString() {
        return class_.name.value.toString();
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(List<Object> arguments) {
        return new SamirInstance(this);
    }
}

class SamirInstance {
    Environment environment;
    SamirClass class_;

    // For native classes only:
    Object dataStruct;

    SamirInstance(SamirClass class_){
        this.class_ = class_;
        this.environment = new Environment(Language.environment);
        Environment prev = Language.environment;
        Language.environment = this.environment;
        List<Stmt> bodyStatements = class_.class_.classBody;

        environment.define("self", this);
        for (Stmt stmt : bodyStatements) {
            stmt.visit();
        }
        Language.environment = prev;
    }

    // Constructer for native classes:
    SamirInstance(Object dataStruct){
        this.environment = new Environment(Language.environment);
        this.dataStruct = dataStruct;
    }

    @Override
    public String toString() {
        return class_ + " instance.";
    }
}