import java.util.List;

class SamirClass implements SamirCallable{
    Environment closure;
    ClassDeclre declaration;
    List<Token> parameters;
    Function to_string;
    Language lang;
    SamirClass(ClassDeclre declaration, Environment closure, Language lang){
        this.declaration = declaration;
        this.parameters = declaration.parameters;
        this.to_string = declaration.to_string;
        this.closure = closure;
        this.lang = lang;
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
        return new SamirInstance(this, arguments, lang);
    }
    
}

class SamirInstance{
    Environment environment;
    SamirClass samir_class;
    String class_name;

    SamirInstance(SamirClass samir_class, List<Object> constructer_args, Language lang){
        this.samir_class = samir_class;
        this.environment = new Environment(samir_class.closure);
        this.class_name = samir_class.declaration.name.value.toString();
        Environment prev = lang.environment;
        lang.environment = this.environment;
        List<Stmt> bodyStatements = samir_class.declaration.classBody;


        environment.define("self", this);
        for (int i = 0; i < constructer_args.size(); i++)
            this.environment.define(samir_class.parameters.get(i).value.toString(), constructer_args.get(i));
        for (Stmt stmt : bodyStatements)
            stmt.visit();

        if(samir_class.to_string != null)
            samir_class.to_string.visit();
        

        lang.environment = prev;
    }


    // For inherting native classes:
    SamirInstance(Language lang){
        this.environment = new Environment(lang.environment);
    }

    @Override
    public String toString() {
        if(this.samir_class.to_string == null)
            return "<" + samir_class.declaration.name + " instance " + this.hashCode() + ">";
        SamirCallable to_string = (SamirCallable) environment.variables.get("_toString");
        return Language.stringify(to_string.call(null));
    }
}

 class Importinstance extends SamirInstance {
    Importinstance(Language lang, Token import_name){
        super(lang);
        this.environment = lang.environment;
        this.class_name = import_name.value.toString();
    }
 }