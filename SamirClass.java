import java.util.ArrayList;
import java.util.List;

class SamirClass implements SamirCallable{
    Environment closure;
    ClassDeclre class_;
    SamirClass(ClassDeclre class_){
        this.class_ = class_;
    }

    @Override
    public String toString() {
        return "<class " + class_.name.value + ">";
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

class SamirInstance implements Cloneable{
    Environment environment;
    SamirClass class_;

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

    @Override
    protected SamirInstance clone() throws CloneNotSupportedException {
        SamirInstance copy = (SamirInstance) super.clone();
        copy.environment = (Environment) environment.clone();
        return copy;
    }


    // For inherting native classes:
    SamirInstance(){
        this.environment = new Environment(Language.environment);
    }

    @Override
    public String toString() {
        return "<" + class_.class_.name.value + " instance " + this.hashCode() + ">";
    }
}

class ListInstance extends SamirInstance {

    ArrayList<Object> arrayList;

    ListInstance(ArrayList<Object> arrayList){
        super();
        this.arrayList = arrayList;

        this.environment.define("size", 0.0);

        // Create append/add method for list object:
        this.environment.define("add", new SamirCallable() {

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Void call(List<Object> arguments) {

                Object arg = arguments.get(0);
                arrayList.add(arg);

                Object temp = environment.get(new Token(null, "size", 0));
                Double oldSize = (Double) temp;
                environment.assign(new Token(null, "size", 0), oldSize + 1);
                
                return null;
            }

            @Override
            public String toString() {
                return "<function add>";
            }
            
        });

        // Create "get" method for list object:
        this.environment.define("get", new SamirCallable() {

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(List<Object> arguments) {
                Object arg = arguments.get(0);
                if(arg instanceof Double == false)
                    Language.error("get() arg must be a whole number", -1);
                Double temp = (Double) arg;
                if(temp % 1 != 0)
                    Language.error("get() arg must be a whole number", -1);
                return arrayList.get(temp.intValue());
            }

            @Override
            public String toString() {
                return "<function get>";
            }
            
        });
        
    }

    @Override
    public String toString() {

        String result = "[";
        for (Object element : arrayList)
            result += Language.stringify(element) + ", ";
        if(result.length() > 2)
            result = result.substring(0, result.length() - 2);
        return result + "]";
    }
}