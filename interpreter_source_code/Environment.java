import java.util.HashMap;

public class Environment implements Cloneable{

    HashMap <String, Object> variables = new HashMap<>();
    Environment outer;

    Environment(){
        outer = null;
    }

    Environment(Environment outerEnvironment){
        this.outer = outerEnvironment;
    }

    void define(String identifier, Object value){
        variables.put(identifier, value);
    }

    void assign(Token name, Object value){
        if(variables.containsKey(name.value)){
            variables.put(name.value.toString(), value);
            return;
        }

        if(outer != null){
            outer.assign(name, value);
            return;
        }


        Language.error( "Undefined variable '" + name.value.toString() + "'", name.line);
    }

    Object get(Token identifier){
        String varName = identifier.value.toString();
        if(variables.containsKey(varName))
            return variables.get(varName);
        
        if(outer != null)
            return outer.get(identifier);
        
        Language.error("variable '" + identifier.value + "' has NOT been defined", identifier.line);
        return null;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Environment clone = (Environment) super.clone();
        clone.variables = (HashMap<String, Object>) variables.clone();
        return clone;
    }

    @Override
    public String toString() {
        String output = " Environment {\n";
        for (String key : variables.keySet())
            output += key + " : " + variables.get(key) + "\n";
        

        return output + "}";
    }

}