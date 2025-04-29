import java.util.List;

class SamirFunction implements SamirCallable {
    final Function declaration;
    SamirFunction(Function declaration){
        this.declaration = declaration;
    }
    @Override
    public int arity() {
        return declaration.parameters.size();
    }
    @Override
    public Object call(List<Object> arguments) {

        Environment environment = new Environment(Language.globals);

        for (int i = 0; i < declaration.parameters.size(); i++) {
            String paraName = declaration.parameters.get(i).value.toString();
            Object argValue = arguments.get(i);
            environment.define(paraName, argValue);
        }

        declaration.body.environment = environment;
        try{
            declaration.body.visit();
        }
        catch(ReturnException returnValue){
            return returnValue.value;
        }
        

        return null;
    }

}

class ReturnException extends RuntimeException {
    final Object value;

    ReturnException(Object value){
        super(null, null, false, false);
        this.value = value;
    }
}