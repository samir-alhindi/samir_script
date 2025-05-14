import java.util.List;

class SamirFunction implements SamirCallable {
    final Function declaration;
    final Environment closure;
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
                    //newInstance.environment.outer = environment;
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
        Language.aboutToRunFunction = true;
        // We use this variable to return to the last point since the call,
        // It's important when we want to return from say an if statement inside a while loop inside a function.
        Environment lasEnvi = Language.environment;
        // Check if stack overflow:
        if(Language.enviStack.size() > 1024)
            Language.error("Stack overflow !", -2);
        try{
            declaration.body.visit();
        }
        catch(ReturnException returnValue){
            // Restore previous environment:
            while (Language.environment != lasEnvi)
                Language.environment = Language.enviStack.pop();
            
            Language.aboutToRunFunction = false;
            return returnValue.value;
        }
        

        return null;
    }

    @Override
    public String toString() {
        return "<function " + declaration.name.value +  ">";
    }

}

class ReturnException extends RuntimeException {
    final Object value;

    ReturnException(Object value, Token keyword){
        super("at line " + keyword.line +": Return statement must be inside a function", null, false, false);
        this.value = value;
    }
}