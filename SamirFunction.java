import java.util.List;

class SamirFunction implements SamirCallable {
    final Function declaration;
    private final Environment closure;
    SamirFunction(Function declaration, Environment closure){
        this.declaration = declaration;
        this.closure = closure;
    }
    @Override
    public int arity() {
        return declaration.parameters.size();
    }
    @Override
    public Object call(List<Object> arguments) {

        Environment environment = new Environment(closure);

        for (int i = 0; i < declaration.parameters.size(); i++) {
            String paraName = declaration.parameters.get(i).value.toString();
            Object argValue = arguments.get(i);

            if(argValue instanceof SamirInstance){
                try{
                    SamirInstance newInstance = (SamirInstance) ((SamirInstance)argValue).clone();
                    newInstance.environment.outer = environment;
                    environment.define(paraName, newInstance);
                }
                catch(CloneNotSupportedException e){
                    System.out.println("Can't clone.");
                }
            }
            else
                environment.define(paraName, argValue);
        }

        declaration.body.environment = environment;
        try{
            declaration.body.visit();
        }
        catch(ReturnException returnValue){
            // Restore prevoius environment:
            Language.environment = Language.enviStack.pop();
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