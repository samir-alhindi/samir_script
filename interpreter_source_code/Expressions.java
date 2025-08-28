import java.util.ArrayList;
import java.util.LinkedHashMap;
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

        String og_string = (String) literal;
        // No interpolation needed:-
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
            else if(og_string.charAt(i) == '{'){
                String expression = "";
                do{
                    i++;
                    if(i >= og_string.length())
                        Runtime.error("unterminated '{', Perhaps you want an escape character '\\{' instead", token.line, token.file_name);
                    expression += og_string.charAt(i);
                } while(og_string.charAt(i) != '}');

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
    SamirList visit() {
        ArrayList<Object> elements = new ArrayList<>();
        for (Expre element : this.elements) 
            elements.add(element.visit());
        return new SamirList(elements, lang);
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
        var output = new LinkedHashMap<Object, Object>();
        for (Map.Entry<Expre, Expre> pair : map.entrySet()) {
            Object key = pair.getKey().visit();
            Object value = pair.getValue().visit();
            output.put(key, value);
        }
        return new SamirDict(output, lang);
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

        // Numeric opperations:-
        if(Util.token_is(token,
            TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MOD, TokenType.GREATER_THAN,
            TokenType.GREATER_THAN_OR_EQUAL,TokenType.LESS_THAN, TokenType.LESS_THAN_OR_EQUAL)){
                checkNumberOperands(left, right);
                switch(token.type){
                    case PLUS -> {return (double) left + (double) right;}
                    case MINUS -> {return (double) left - (double) right;}
                    case MULTIPLY -> {return (double) left * (double) right;}
                    case DIVIDE -> {return (double) left / (double) right;}
                    case MOD -> {return (double) left % (double) right;}
                    case GREATER_THAN -> {return (double) left > (double) right;}
                    case GREATER_THAN_OR_EQUAL -> {return (double) left >= (double) right;}
                    case LESS_THAN -> {return (double) left < (double) right;}
                    case LESS_THAN_OR_EQUAL -> {return (double) left <= (double) right;}
                }
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
            if(left instanceof Double && right instanceof Double)
                return (double) left + (double) right;
            
            else if(left instanceof String && right instanceof String)
                return (String) left + (String) right;
            
            else if(left instanceof SamirList && right instanceof SamirList)
                return SamirList.combine_2_lists((SamirList) left, (SamirList) right, lang);

            else
                Runtime.error("'+' operands must both be strings or numbers or Lists", token.line, token.file_name);
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

        return switch(token.type){
            case MINUS -> right instanceof Double ? -(double) right : Runtime.error("operand must be a number", token.line, token.file_name);
            case NOT -> right instanceof Boolean ? ! (boolean) right : Runtime.error("operand must be a boolean", token.line, token.file_name);
            default ->  "This condition will never run :)";
        };
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
        this.token = opp;
        this.lang = lang;
    }
    @Override
    Object visit() {

        Object newValue = null;

        Object right = this.right.visit();
        Object left = lang.environment.get(varName);

        if(Util.token_is(opp, TokenType.MINUS_EQUAL, TokenType.MULTIPLY_EQUAL, TokenType.DIVIDE_EQUAL, TokenType.MOD_EQUAL)){
            checkNumberOperands(left, right);
            newValue = switch(opp.type){
                case MINUS_EQUAL -> (double) left - (double) right;
                case MULTIPLY_EQUAL -> (double) left * (double) right;
                case DIVIDE_EQUAL -> (double) left / (double) right;
                default -> (double) left % (double) right;
            };
        }

        else if(tokenIs(TokenType.EQUALS))
            newValue = right;

        else if(tokenIs(TokenType.PLUS_EQUAL)){
            if(left instanceof Double && right instanceof Double)
                newValue = (Double) left + (Double) right;
            else if(left instanceof String && right instanceof String)
                newValue = (String) left + (String) right;
            else if(left instanceof SamirList && right instanceof SamirList)
                newValue = SamirList.combine_2_lists((SamirList) left, (SamirList) right, lang);
            else
                Runtime.error("'+=' opperands must both be numbers or strings or Lists", opp.line, opp.file_name);
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
        String val = Util.check_type(new_value, String.class, "string[index] new value must be a string and not of type: "+Util.typeOf(new_value), runtime.line, runtime.cur_file_name);
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
    Runtime runtime;

    Get(Expre instanceVar, Token dot, Token memberVar, Runtime runtime){
        this.instanceVar = instanceVar;
        this.token = dot;
        this.memberVar = memberVar;
        this.runtime = runtime;
    }

    @Override
    Object visit() {

        Object visited = instanceVar.visit();
        SamirObject object = Util.check_type(visited, SamirObject.class, "Can only get members from objects and not: "+Util.typeOf(visited), token.line, token.file_name);

        String memberName = memberVar.value.toString();
        runtime.line = token.line;

        if(object.environment.variables.containsKey(memberName)){
            Object value = object.environment.variables.get(memberName);
            return value;
        }
        
        return Runtime.error(memberName + " not found in object of type: " + object.type, token.line, token.file_name);
    }
}

class Set extends Expre {
    Get get;
    Expre right_side;
    Token opp;
    Runtime lang;

    Set(Get get, Expre right_side, Token opp, Runtime lang){
        this.get = get;
        this.right_side = right_side;
        this.opp = opp;
        this.lang = lang;
    }

    Object visit(){

        Object visited = get.instanceVar.visit();
        SamirObject object = Util.check_type(visited, SamirObject.class, "can only set members of objects not of type: "+Util.typeOf(visited), opp.line, opp.file_name);

        Object og_value = object.environment.variables.containsKey(get.memberVar.value) ?
            og_value = object.environment.get(get.memberVar)
            : Runtime.error(get.memberVar.value.toString() + " not found in object of type: " + object.type, opp.line, opp.file_name);

        Object right_side = this.right_side.visit();
        Object new_val = null;
        switch(opp.type){
            case TokenType.EQUALS -> {
                new_val = right_side;
            } 
            case TokenType.MINUS_EQUAL ->{
                checkNumberOperands(og_value, right_side);
                new_val = (Double) og_value - (Double) right_side;
            }
            case TokenType.MULTIPLY_EQUAL ->{
                checkNumberOperands(og_value, right_side);
                new_val = (Double) og_value * (Double) right_side;
            }
            case TokenType.DIVIDE_EQUAL ->{
                checkNumberOperands(og_value, right_side);
                new_val = (Double) og_value / (Double) right_side;
            }
            case TokenType.MOD_EQUAL ->{
                checkNumberOperands(og_value, right_side);
                new_val = (Double) og_value % (Double) right_side;
            }
            case TokenType.PLUS_EQUAL -> {

                if(og_value instanceof Double && right_side instanceof Double)
                    new_val = (Double) og_value + (Double) right_side;

                else if(og_value instanceof String && right_side instanceof String)
                    new_val = (String) og_value + (String) right_side;
        
                else if(og_value instanceof SamirList){
                    if(right_side instanceof SamirList){
                        SamirList combined = new SamirList(new ArrayList<>(), lang);
                        for (Object item : ((SamirList)og_value).arrayList)
                            combined.arrayList.add(item);
                        for (Object item : ((SamirList)right_side).arrayList)
                            combined.arrayList.add(item);
                        combined.environment.variables.put("size", ((SamirList) og_value).getSize() + ((SamirList) right_side).getSize());
                        new_val = combined;
                    }

                }
                else
                    Runtime.error("'+=' opperands must both be numbers or strings or Lists", opp.line, opp.file_name);
            }
        }

        
        object.environment.variables.put(get.memberVar.value.toString(), new_val);
        return new_val;
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
