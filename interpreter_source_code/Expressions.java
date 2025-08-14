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

class LiteralExpre extends Expre {
    Object literal;
    Language lang;
    LiteralExpre(Token token, Object literal, Language lang){
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
                        Language.error("unterminated '{', Perhaps you want an escape character '\\{' instead", token.line);
                    expression += og_string.charAt(i);
                } while(og_string.charAt(i) != '}');

                // We found the closing '}':
                Lexer lexer = new Lexer(expression);
                lexer.line = token.line;
                ArrayList<Token> tokens = lexer.lex();
                Parser parser = new Parser(tokens, lang);
                Expre ast = parser.expression();
                Object result = ast.visit();
                output += Language.stringify(result);
                // Move past the '}':
                i++;
            }
            // Else it is '\':
            else if(og_string.charAt(i) == '\\'){
                i++;
                if(i >= og_string.length())
                    Language.error("escape char '\\' must be followed by another char (\\n,\\t,\\{)", token.line);
                switch (og_string.charAt(i)) {
                    case 'n'-> output += System.lineSeparator();
                    case 't' -> output += '\t';
                    case '{' -> output += '{';
                    case '}' -> output += '}';
                    case '\\' -> output += '\\';
                    default -> Language.error("invalid escape character: \\" + og_string.charAt(i), token.line);
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
    Language lang;
    ListLiteral(Token token, List<Expre> elements, Language lang){
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
    Language lang;
    DictLiteral(Token token, Map<Expre, Expre> map, Language lang){
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

class BinOpExpre extends Expre {
    Expre left_node;
    Expre right_node;
    Language lang;

    BinOpExpre(Expre left_node, Token op_token, Expre right_node, Language lang){
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
                Language.error("'+' operands must both be strings or numbers or Lists", token.line);
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
        Language.error("both operands must be numbers", token.line);
    }

}

class UnaryOpExpre extends Expre {
    Expre child_node;
    UnaryOpExpre(Expre child_node, Token op_token){
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
        Language.error("operand must be a number", token.line);
    }

    private void checkBooleanoperand(Object child){
        if(child instanceof Boolean) return;
        Language.error("operand must be a boolean", token.line);
    }


    }

    class GroupingExpre extends Expre {
        Expre child_node;
        GroupingExpre(Expre node){
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
    Language lang;
    Variable(Token token, Language lang){
        this.token = token;
        this.lang = lang;
    }

    @Override
    Object visit() {
        return lang.environment.get(token);
    }
}

class AssignExpre extends Expre {
    Expre right;
    Token opp;
    Token varName;
    Language lang;

    AssignExpre(Token varName, Expre right, Token opp, Language lang){
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
                    Language.error("'+=' opperands must both be numbers or strings or Lists", opp.line);
            }
        }

        lang.environment.assign(varName, newValue);
        return newValue;
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Language.error("both operands must be numbers", token.line);
    }
}

class CollectionAssign extends Expre {
    Subscript subscript;
    Expre newValue;
    CollectionAssign(Subscript subscript, Expre newValue, Token token){
        this.subscript = subscript;
        this.newValue = newValue;
        this.token = token;
    }
    @Override
    Object visit() {
        
        Object collectionVisited = subscript.collection.visit();
        Object indexVisited = subscript.index.visit();
        Object newValueVisited = newValue.visit();

        if(collectionVisited instanceof ListInstance){
            int final_index = ((ListInstance)collectionVisited).checkValidIndex(indexVisited);
            ((ListInstance)collectionVisited).arrayList.set(final_index, newValueVisited);
        }

        else if(collectionVisited instanceof String){

            if(newValueVisited instanceof String == false || ((String) newValueVisited).length() != 1)
                Language.error("string[index] must be set to a string of length 1", token.line);

            int finalIndex = Subscript.Inner.checkValidIndex(indexVisited, collectionVisited.toString(), token);
            char[] chars = ((String) collectionVisited).toCharArray();
            chars[finalIndex] = ((String) newValueVisited).charAt(0);
            collectionVisited = new String(chars);
        }

        else if(collectionVisited instanceof DictInstance){
            ( (DictInstance) collectionVisited).hashMap.put(indexVisited, newValueVisited);
        }

        else
            Language.error("Invalid assignment target", token.line);

        

        return collectionVisited;
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
            Language.error("(and, or) operands must be boolean values ", token.line);

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
    Language lang;

    Call(Expre callee, Token paren, List<Expre> arguments, Language lang){
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
            Language.error("Can only call functions, Lambdas and classes !", paren.line);

        SamirCallable thingToCall = (SamirCallable)callee;

        if(arguments.size() != thingToCall.arity())
            Language.error("Expected " + thingToCall.arity() + " args but got " + arguments.size(), paren.line);

        lang.line = paren.line;

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

    Subscript(Token bracket, Expre collection, Expre index){
        this.token = bracket;
        this.collection = collection;
        this.index = index;
    }

    @Override
    Object visit() {

        Object collectionVisited = collection.visit();
        Object indexVisited = index.visit();

        if(collectionVisited instanceof ListInstance){
            int final_index = ((ListInstance)collectionVisited).checkValidIndex(indexVisited);
            return ((ListInstance)collectionVisited).arrayList.get(final_index);
        }

        else if(collectionVisited instanceof String){
            int final_index = Inner.checkValidIndex(indexVisited, (String) collectionVisited, token);
            return ((String) collectionVisited).charAt(final_index) + "";
        }

        else if(collectionVisited instanceof DictInstance){
            if(((DictInstance)collectionVisited).hashMap.containsKey(indexVisited) == false)
                Language.error("Key: " + indexVisited.toString() + " not found in Dict", token.line);
            return ((DictInstance)collectionVisited).hashMap.get(indexVisited);
        }

        else if(collectionVisited instanceof SamirPairList){
            int final_index = Inner.checkValidIndex(indexVisited, (SamirPairList) collectionVisited, token);
            return ((SamirPairList)collectionVisited).list.get(final_index);
        }


        return null;
    }

        class Inner {
            static int checkValidIndex(Object object, String string, Token token){
                if(object instanceof Double == false)
                    Language.error("string index must be a number", token.line);
                Double index = (Double) object;

                if(index % 1 != 0)
                    Language.error("argument must be a whole number", token.line);

                if(index >= string.length())
                    Language.error("index " + index.intValue() + " out of bounds for size " + string.length(), token.line);
                
                // Negative index:
                if(index < 0){
                    Double actualIndex = index + string.length();
                    if(actualIndex < 0)
                        Language.error("index " + index.intValue() + " out of bounds for size " + string.length(), token.line);
                    index = actualIndex;
        }
        
        return index.intValue();

            }
        
        static int checkValidIndex(Object object, SamirPairList samir_pair_list, Token token){
                if(object instanceof Double == false)
                    Language.error("string index must be a number", token.line);
                Double index = (Double) object;

                if(index % 1 != 0)
                    Language.error("argument must be a whole number", token.line);

                if(index >= samir_pair_list.list.size())
                    Language.error("index " + index.intValue() + " out of bounds for size " + samir_pair_list.list.size(), token.line);
                
                // Negative index:
                if(index < 0){
                    Double actualIndex = index + samir_pair_list.list.size();
                    if(actualIndex < 0)
                        Language.error("index " + index.intValue() + " out of bounds for size " + samir_pair_list.list.size(), token.line);
                    index = actualIndex;
        }
        
        return index.intValue();
        }

        }
}

class Get extends Expre {

    Expre instanceVar;
    Token memberVar;
    Language lang;

    Get(Expre instanceVar, Token dot, Token memberVar, Language lang){
        this.instanceVar = instanceVar;
        this.token = dot;
        this.memberVar = memberVar;
        this.lang = lang;
    }

    @Override
    Object visit() {
        Object object = instanceVar.visit();
        if(object instanceof SamirInstance == false)
            Language.error("can only access members from class instances", token.line);
        SamirInstance instance = (SamirInstance) object;

        String memberName = memberVar.value.toString();
        lang.line = token.line;

        if(instance.environment.variables.containsKey(memberName)){
            Object value = instance.environment.variables.get(memberName);
                return value;
        }
        
        String instanceType = (instance instanceof Importinstance) ? "import" : "class";
        Language.error(memberName + " not found in " +  instanceType + ": " + instance.class_name, token.line);

        // Unreachable code:
        return null;
    }
}

class Set extends Expre {
    Get member;
    Expre newValue;
    Token opp;
    Language lang;

    Set(Get member, Expre newValue, Token opp, Language lang){
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
            Language.error(member.memberVar.value + " not found in instance of class: " + instance.samir_class.declaration.name.value, opp.line);

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
                    Language.error("'+=' opperands must both be numbers or strings or Lists", opp.line);
            }
        }

        
        instance.environment.variables.put(member.memberVar.value.toString(), newValueObject);
        return newValueObject;
    }

    private void checkNumberOperands(Object left, Object right){
        if(left instanceof Double && right instanceof Double) return;
        Language.error("both operands must be numbers", token.line);
    }
}

class Lambda extends Expre {
    List<Token> parameters;
    Expre body;
    Language lang;

    Lambda(Token keyword, List<Token> parameters, Expre body, Language lang){
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
            Language.error("Ternary condition must be a boolean expression", token.line);
        Object result = (Boolean) condition ? left.visit() : right.visit();
        return result;
    }
}
