import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class Expre {
    Token token;

    abstract Object visit();
    
    boolean tokenIs(TokenType type){
        return token.type.equals(type);
    }

}

class Literal extends Expre {
    Object literal;
    Runtime lang;
    Literal(Token token, Object literal, Runtime lang){
        this.token = token;
        this.literal = literal;
        this.lang = lang;
    }

    @Override
    Object visit() {
        if(token.type.equals(TokenType.STRING))
            return string();
        return literal;
        
    }

    String string(){

        // No interpolation needed:-
        String og_string = (String) literal;
        if( ! og_string.contains("{") && ! og_string.contains("\\"))
            return og_string;
        
        String output = "";
        int i = 0;
        while (i < og_string.length()) {
            if(og_string.charAt(i) != '{' && og_string.charAt(i) != '\\'){
                output += og_string.charAt(i);
                i++;
                continue;
            }
            // Else it is '{':
            if(og_string.charAt(i) == '{'){
                String expression = "";
                do{
                    i++;
                    if(i >= og_string.length())
                        Runtime.error("unterminated '{', Perhaps you want an escape character '\\{' instead", token.line, token.file_name);
                    expression += og_string.charAt(i);
                } while(og_string.charAt(i) != '}');

                // We found the closing '}':
                Lexer lexer = new Lexer(expression, token.file_name);
                lexer.line = token.line;
                ArrayList<Token> tokens = lexer.lex();
                Parser parser = new Parser(tokens, lang);
                Expre ast = parser.expression();
                Object result = ast.visit();
                output += Util.stringify(result);
                // Move past the '}':
                i++;
            }
            // Else it is '\':
            else if(og_string.charAt(i) == '\\'){
                i++;
                if(i >= og_string.length())
                    Runtime.error("escape char '\\' must be followed by another char (\\n,\\t,\\{)", token.line, token.file_name);
                switch (og_string.charAt(i)) {
                    case 'n'-> output += System.lineSeparator();
                    case 't' -> output += '\t';
                    case '{' -> output += '{';
                    case '}' -> output += '}';
                    case '\\' -> output += '\\';
                    default -> Runtime.error("invalid escape character: \\" + og_string.charAt(i), token.line, token.file_name);
                }
                i++;
            }

        }

        return output;
    }
    

    @Override
    public String toString() {
        if(literal == null) return "nil";
        return literal.toString();
        }
    }

class ListLiteral extends Expre {
    List<Expre> elements;
    Runtime lang;
    ListLiteral(Token token, List<Expre> elements, Runtime lang){
        this.token = token;
        this.elements = elements;
        this.lang = lang;
    }
    @Override
    ListInstance visit() {
        ArrayList<Object> elementsVisited = new ArrayList<>();
        for (Expre element : elements) 
            elementsVisited.add(element.visit());
        ListInstance list =  new ListInstance(elementsVisited, lang);
        return list;
    }
}

class DictLiteral extends Expre {
    Map<Expre, Expre> map;
    Runtime lang;
    DictLiteral(Token token, Map<Expre, Expre> map, Runtime lang){
        this.token = token;
        this.map = map;
        this.lang = lang;
    }
    @Override
    Object visit() {
        var output = new HashMap<Object, Object>();
        for (Map.Entry<Expre, Expre> pair : map.entrySet()) {
            Object key = pair.getKey().visit();
            Object value = pair.getValue().visit();
            output.put(key, value);
        }
        return new DictInstance(output, lang);
    }
}

class BinOp extends Expre {
    Expre left_node;
    Expre right_node;
    Runtime lang;

    BinOp(Expre left_node, Token op_token, Expre right_node, Runtime lang){
        this.left_node = left_node;
        this.token = op_token;
        this.right_node = right_node;
        this.lang = lang;
    }

    @Override
    Object visit(){

        Object left = left_node.visit();
        Object right = right_node.visit();

        if(tokenIs(TokenType.MINUS)){
            checkNumberOperands(left, right);
            return (double) left - (double) right;
        }

        else if(tokenIs(TokenType.MULTIPLY)){
            checkNumberOperands(left, right);
            return (double) left * (double) right;
        }

        else if(tokenIs(TokenType.DIVIDE)){
            checkNumberOperands(left, right);
            return (double) left / (double) right;
        }

        else if(tokenIs(TokenType.MOD)){
            checkNumberOperands(left, right);
            return (double) left % (double) right;
        }

        else if(tokenIs(TokenType.GREATER_THAN)){
            checkNumberOperands(left, right);
            return (double) left > (double) right;
        }

        else if(tokenIs(TokenType.GREATER_THAN_OR_EQUAL)){
            checkNumberOperands(left, right);
            return (double) left >= (double) right;
        }

        else if(tokenIs(TokenType.LESS_THAN)){
            checkNumberOperands(left, right);
            return (double) left < (double) right;
        }

        else if(tokenIs(TokenType.LESS_THAN_OR_EQUAL)){
            checkNumberOperands(left, right);
            return (double) left <= (double) right;
        }

        else if(tokenIs(TokenType.DOUBLE_EQUAL)){
            if(left == null && right != null) return false;
            else if(left == null && right == null) return true;
            return left.equals(right);
        }

        else if(tokenIs(TokenType.NOT_EQUAL)){
            if(left == null && right != null) return false;
            else if(left == null && right == null) return true;
            return ! left.equals(right);
        }

        else if(tokenIs(TokenType.PLUS)){
            if(left instanceof Double && right instanceof Double){
                return (double) left + (double) right;
            }
            else if(left instanceof String && right instanceof String){
                return (String) left + (String) right;
            }
            else if(left instanceof ListInstance){
                if(right instanceof ListInstance){
                    ListInstance combined = new ListInstance(new ArrayList<>(), lang);
                    for (Object item : ((ListInstance)left).arrayList)
                        combined.arrayList.add(item);
                    for (Object item : ((ListInstance)right).arrayList)
                        combined.arrayList.add(item);
                    combined.environment.variables.put("size", ((ListInstance) left).getSize() + ((ListInstance) right).getSize());
                    return combined;
                }

            }
            else{
                Runtime.error("'+' operands must both be strings or numbers or Lists", token.line, token.file_name);
            }
        }

        // Unreachable code:
        return null;


    }

    @Override
    public String toString() {
        return "(" + left_node + ", " + token.type + ", " + right_node + ")";
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Runtime.error("both operands must be numbers", token.line, token.file_name);
    }

}

class UnaryOp extends Expre {
    Expre child_node;
    UnaryOp(Expre child_node, Token op_token){
        this.child_node = child_node;
        this.token = op_token;
        }


    @Override
    public String toString() {
        return "(" + token.type  + ", " + child_node + ")";
        }


    @Override
    Object visit() {
        Object right = child_node.visit();

        if(tokenIs(TokenType.MINUS)){
            checkNumberOperand(right);
            return -(double) right;
        }
        else if(tokenIs(TokenType.NOT)){
            checkBooleanoperand(right);
            return ! (boolean) right;
        }

        // Unreachable:
        return null;
    }

    private void checkNumberOperand(Object child){
        if(child instanceof Double) return;
        Runtime.error("operand must be a number", token.line, token.file_name);
    }

    private void checkBooleanoperand(Object child){
        if(child instanceof Boolean) return;
        Runtime.error("operand must be a boolean", token.line, token.file_name);
    }


    }

    class Grouping extends Expre {
        Expre child_node;
        Grouping(Expre node){
            this.child_node = node;
        }

        @Override
        public String toString() {
            return "[" + child_node + "]";
            }

        @Override
        Object visit() {
            return child_node.visit();
        }
        }

class Variable extends Expre {
    Runtime lang;
    Variable(Token token, Runtime lang){
        this.token = token;
        this.lang = lang;
    }

    @Override
    Object visit() {
        return lang.environment.get(token);
    }
}

class Assignment extends Expre {
    Expre right;
    Token opp;
    Token varName;
    Runtime lang;

    Assignment(Token varName, Expre right, Token opp, Runtime lang){
        this.varName = varName;
        this.right = right;
        this.opp = opp;
        this.lang = lang;
    }
    @Override
    Object visit() {

        Object newValue = null;

        Object rightVisited = right.visit();
        Object leftVisited = lang.environment.get(varName);

        switch(opp.type){
            case TokenType.EQUALS -> {
                newValue = rightVisited;
            } 
            case TokenType.MINUS_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited - (Double) rightVisited;
            }
            case TokenType.MULTIPLY_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited * (Double) rightVisited;
            }
            case TokenType.DIVIDE_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited / (Double) rightVisited;
            }
            case TokenType.MOD_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValue = (Double) leftVisited % (Double) rightVisited;
            }
            case TokenType.PLUS_EQUAL -> {

                if(leftVisited instanceof Double && rightVisited instanceof Double)
                    newValue = (Double) leftVisited + (Double) rightVisited;

                else if(leftVisited instanceof String && rightVisited instanceof String)
                    newValue = (String) leftVisited + (String) rightVisited;
        
                else if(leftVisited instanceof ListInstance){
                    if(rightVisited instanceof ListInstance){
                        ListInstance combined = new ListInstance(new ArrayList<>(), lang);
                        for (Object item : ((ListInstance)leftVisited).arrayList)
                            combined.arrayList.add(item);
                        for (Object item : ((ListInstance)rightVisited).arrayList)
                            combined.arrayList.add(item);
                        combined.environment.variables.put("size", ((ListInstance) leftVisited).getSize() + ((ListInstance) rightVisited).getSize());
                        newValue = combined;
                    }

                }
                else
                    Runtime.error("'+=' opperands must both be numbers or strings or Lists", opp.line, opp.file_name);
            }
        }

        lang.environment.assign(varName, newValue);
        return newValue;
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Runtime.error("both operands must be numbers", token.line, token.file_name);
    }
}

class SubscriptAssign extends Expre {
    Subscript subscript;
    Expre newValue;
    Runtime runtime;
    SubscriptAssign(Subscript subscript, Expre newValue, Token token, Runtime runtime){
        this.subscript = subscript;
        this.newValue = newValue;
        this.token = token;
        this.runtime = runtime;
    }
    @Override
    Object visit() {

        Object collection = subscript.collection.visit();
        Object index = subscript.index.visit();
        Object new_value = newValue.visit();

        return switch(collection){
            case Subscriptable s -> s.set_item(index, new_value);
            case String s -> string(s, index, new_value);
            default -> Runtime.error("Cannot do subscript assignment on value of type: "+Util.typeOf(collection), runtime.line, runtime.cur_file_name);
        };

    }

    String string(String s, Object index, Object new_value){
        String val = NativeFunctions.check_type(new_value, String.class, "string[index] new value must be a string and not of type: "+Util.typeOf(new_value), runtime.line, runtime.cur_file_name);
        if(val.length() != 1)
            Runtime.error("string[index] new value must be of length 1", runtime.line, runtime.cur_file_name);
        int i = Util.checkValidIndex(index, s.length(), runtime);
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(i, val.charAt(0));
        return sb.toString();
    }
}

class BinBoolOp extends Expre {
    Expre left;
    Expre right;
    BinBoolOp(Expre left, Token op, Expre right){
        this.left = left;
        this.token = op;
        this.right = right;
    }
    @Override
    Object visit() {
        Object left = this.left.visit();
        if(left instanceof Boolean == false)
            Runtime.error("(and, or) operands must be boolean values ", token.line, token.file_name);

        if(tokenIs(TokenType.OR) && left.equals(true))
            return left;
        else if(tokenIs(TokenType.AND) && left.equals(false))
            return left;
        
        return right.visit();
    }
}

class Call extends Expre {
    Expre callee;
    Token paren;
    List<Expre> arguments;
    Runtime lang;

    Call(Expre callee, Token paren, List<Expre> arguments, Runtime lang){
        this.callee = callee;
        this.paren = paren;
        this.arguments = arguments;
        this.token = callee.token;
        this.lang = lang;
    }

    @Override
    Object visit() {
        Object callee = this.callee.visit();
        List<Object> arguments = new ArrayList<>();
            
            
        for (Expre arg : this.arguments) 
            arguments.add(arg.visit());
        
        if(callee instanceof SamirCallable == false)
            Runtime.error("Can only call functions, Lambdas and classes !", paren.line, paren.file_name);

        SamirCallable thingToCall = (SamirCallable)callee;

        if(arguments.size() != thingToCall.arity())
            Runtime.error("Expected " + thingToCall.arity() + " args but got " + arguments.size(), paren.line, paren.file_name);

        lang.line = paren.line;
        lang.cur_file_name = paren.file_name;

        return thingToCall.call(arguments);
        
    }

    @Override
    public String toString() {
        return callee + "(" + arguments + ")";
    }
    
}

class Subscript extends Expre {

    Expre index;
    Expre collection;
    Runtime lang;

    Subscript(Token bracket, Expre collection, Expre index, Runtime lang){
        this.token = bracket;
        this.collection = collection;
        this.index = index;
        this.lang = lang;
    }

    @Override
    Object visit() {

        Object collection = this.collection.visit();
        Object index = this.index.visit();

        lang.line = token.line;
        return switch(collection){
            case Subscriptable s -> s.get_item(index);
            case String s -> "" + s.charAt(Util.checkValidIndex(index, s.length(), lang));
            default -> Runtime.error("The value: '" + collection.toString() + "'' of type: '" + Util.typeOf(collection) + "' is not subscriptable", token.line, token.file_name);
        };
    }

}

class Get extends Expre {

    Expre instanceVar;
    Token memberVar;
    Runtime lang;

    Get(Expre instanceVar, Token dot, Token memberVar, Runtime lang){
        this.instanceVar = instanceVar;
        this.token = dot;
        this.memberVar = memberVar;
        this.lang = lang;
    }

    @Override
    Object visit() {
        Object object = instanceVar.visit();
        if(object instanceof SamirInstance == false)
            Runtime.error("can only access members from class instances", token.line, token.file_name);
        SamirInstance instance = (SamirInstance) object;

        String memberName = memberVar.value.toString();
        lang.line = token.line;

        if(instance.environment.variables.containsKey(memberName)){
            Object value = instance.environment.variables.get(memberName);
                return value;
        }
        
        String instanceType = (instance instanceof Importinstance) ? "import" : "class";
        Runtime.error(memberName + " not found in " +  instanceType + ": " + instance.class_name, token.line, token.file_name);

        // Unreachable code:
        return null;
    }
}

class Set extends Expre {
    Get member;
    Expre newValue;
    Token opp;
    Runtime lang;

    Set(Get member, Expre newValue, Token opp, Runtime lang){
        this.member = member;
        this.newValue = newValue;
        this.opp = opp;
        this.lang = lang;
    }

    Object visit(){

        SamirInstance instance = (SamirInstance) member.instanceVar.visit();

        Object rightVisited = newValue.visit();
        Object leftVisited = null;

        Object newValueObject = null;

        if(instance.environment.variables.containsKey(member.memberVar.value))
            leftVisited = instance.environment.get(member.memberVar);
        else
            Runtime.error(member.memberVar.value + " not found in instance of class: " + instance.samir_class.declaration.name.value, opp.line, opp.file_name);

        switch(opp.type){
            case TokenType.EQUALS -> {
                newValueObject = rightVisited;
            } 
            case TokenType.MINUS_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited - (Double) rightVisited;
            }
            case TokenType.MULTIPLY_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited * (Double) rightVisited;
            }
            case TokenType.DIVIDE_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited / (Double) rightVisited;
            }
            case TokenType.MOD_EQUAL ->{
                checkNumberOperands(leftVisited, rightVisited);
                newValueObject = (Double) leftVisited % (Double) rightVisited;
            }
            case TokenType.PLUS_EQUAL -> {

                if(leftVisited instanceof Double && rightVisited instanceof Double)
                    newValueObject = (Double) leftVisited + (Double) rightVisited;

                else if(leftVisited instanceof String && rightVisited instanceof String)
                    newValueObject = (String) leftVisited + (String) rightVisited;
        
                else if(leftVisited instanceof ListInstance){
                    if(rightVisited instanceof ListInstance){
                        ListInstance combined = new ListInstance(new ArrayList<>(), lang);
                        for (Object item : ((ListInstance)leftVisited).arrayList)
                            combined.arrayList.add(item);
                        for (Object item : ((ListInstance)rightVisited).arrayList)
                            combined.arrayList.add(item);
                        combined.environment.variables.put("size", ((ListInstance) leftVisited).getSize() + ((ListInstance) rightVisited).getSize());
                        newValueObject = combined;
                    }

                }
                else
                    Runtime.error("'+=' opperands must both be numbers or strings or Lists", opp.line, opp.file_name);
            }
        }

        
        instance.environment.variables.put(member.memberVar.value.toString(), newValueObject);
        return newValueObject;
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Runtime.error("both operands must be numbers", token.line, token.file_name);
    }
}

class Lambda extends Expre {
    List<Token> parameters;
    Expre body;
    Runtime lang;

    Lambda(Token keyword, List<Token> parameters, Expre body, Runtime lang){
        this.token = keyword;
        this.parameters = parameters;
        this.body = body;
        this.lang = lang;
    }

    @Override
    Object visit() {
        return new SamirLambda(this, lang.environment, lang);
    }
}

class Ternary extends Expre {
    Expre left;
    Expre middle;
    Expre right;
    Ternary(Expre left, Expre middle, Expre right, Token keyword){
        this.left = left;
        this.middle = middle;
        this.right = right;
        this.token = keyword;
    }
    @Override
    Object visit() {
        Object condition = middle.visit();
        if(condition instanceof Boolean == false)
            Runtime.error("Ternary condition must be a boolean expression", token.line, token.file_name);
        Object result = (Boolean) condition ? left.visit() : right.visit();
        return result;
    }
}
