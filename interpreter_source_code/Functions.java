import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

interface SamirCallable {
    int arity();
    Object call(List<Object> arguments);
}

class SamirFunction extends SamirInstance implements SamirCallable {
    final Function declaration;
    final Environment closure;
    Language lang;
    List<String> parameter_names;
    SamirFunction(Function declaration, Environment closure, Language lang){
        super(lang);
        this.declaration = declaration;
        this.closure = closure;
        this.lang = lang;
        class_name = "function";

        parameter_names = new ArrayList<>();
        for (int i = 0; i < declaration.parameters.size(); i++)
            parameter_names.add(declaration.parameters.get(i).value.toString());
        

        environment.define("arity", arity());
        environment.define("name", declaration.name.value.toString());
        environment.define("get_closure", new SamirCallable() {

            @Override
            public int arity() {return 0;}

            @Override
            public Object call(List<Object> arguments) {
                return new DictInstance(new HashMap<>(closure.variables), lang);
            }
            
        });

        environment.define("parameter_names", ListInstance.create_filled_list(parameter_names.toArray(), lang));

        environment.define("bind", new SamirCallable() {

            @Override
            public int arity() {return 1;}

            @Override
            public SamirFunction call(List<Object> arguments) {
                if(SamirFunction.this.arity() == 0)
                    Language.error("cannot bind the function '" + declaration.name.value + "' if it takes zero parameters.", lang.line, lang.cur_file_name);
                Environment new_closure = new Environment(closure);
                Function new_declre = new Function(declaration.name, declaration.parameters.subList(1, declaration.parameters.size()), declaration.body, lang);
                SamirFunction new_func = new SamirFunction(new_declre, new_closure, lang);
                new_closure.define(parameter_names.get(0), arguments.get(0));
                return new_func;
            }
            
        });

    }
    @Override
    public int arity() {
        return declaration.parameters.size();
    }
    @Override
    public Object call(List<Object> arguments) {

        Environment environment = new Environment(closure);

        for (int i = 0; i < parameter_names.size(); i++) {
            String paraName = parameter_names.get(i);
            Object argValue = arguments.get(i);
            environment.define(paraName, argValue);
        }

        
        // We use this variable to return to the last point since the call,
        // It's important when we want to return from say an if statement inside a while loop inside a function.
        Environment lasEnvi = lang.environment;
        lang.enviStack.add(lasEnvi);
        lang.environment = environment;
        // Check if stack overflow:
        if(lang.enviStack.size() > 1024)
            Language.error("function caused a stack overflow", declaration.name.line, declaration.name.file_name);
        try{
            for (Stmt stmt : declaration.body)
                stmt.visit();
            
            // Restore previous environment:
            while (lang.environment != lasEnvi)
                lang.environment = lang.enviStack.pop();
        }
        catch(ReturnException returnValue){
            // Restore previous environment:
            while (lang.environment != lasEnvi)
                lang.environment = lang.enviStack.pop();
            
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

class SamirLambda implements SamirCallable {
    final Lambda declaration;
    final Environment closure;
    Language lang;
    SamirLambda(Lambda declaration, Environment closure, Language lang){
        this.declaration = declaration;
        this.closure = closure;
        this.lang = lang;
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
            environment.define(paraName, argValue);
        }

        
        // We use this variable to return to the last point since the call,
        // It's important when we want to return from say an if statement inside a while loop inside a function.
        Environment lasEnvi = lang.environment;
        lang.enviStack.add(lasEnvi);
        lang.environment = environment;
        // Check if stack overflow:
        if(lang.enviStack.size() > 1024)
            Language.error("lambda caused a stack overflow", declaration.token.line, declaration.token.file_name);
        
        Object value = declaration.body.visit();
            
        // Restore previous environment:
        while (lang.environment != lasEnvi)
            lang.environment = lang.enviStack.pop();

        return value;
    }

    @Override
    public String toString() {
        return "<lambda>";
    }
}