import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class Parser{
    int pos = 0;
    Token current;
    ArrayList<Token> tokens = new ArrayList<>();
    Language lang;

    Parser(ArrayList<Token> tokens, Language lang){
        this.tokens = tokens;
        this.lang = lang;
    }

    List<Stmt> parse(){
        pos = 0;
        current = tokens.get(0);
        List<Stmt> statements = new ArrayList<>();
        while( ! currentIs(TokenType.EOF)){
            statements.add(declaration());
        }

        return statements;
    }


    ///Helper parsing methods///
    
    Stmt declaration(){
        if(currentIs(TokenType.VAR)){
            advance();
            return varDeclaration();
        }

        else if(currentIs(TokenType.FUNC)){
            advance();
            return function();
        }

        else if(currentIs(TokenType.CLASS)){
            advance();
            return classDeclaration();
        }

        else if(currentIs(TokenType.ENUM)){
            advance();
            return enumDeclaration();
        }

        else if(currentIs(TokenType.IMPORT)){
            return importDeclaration();
        }
            

        return statement();
    }

    Stmt importDeclaration(){
        Token keyword = current;
        advance();
        if( ! currentIs(TokenType.STRING))
            Language.error("expected file path after 'import' keyword", current.line);
        String path = (String) current.value;
        advance();
        Token identifier = null;
        if(currentIs(TokenType.AS)){
            advance();
            if( ! currentIs(TokenType.IDENTIFIER))
                Language.error("expected identifier after 'as' keyword", current.line);
            identifier = current;
            advance();
        }
        return new Import(keyword, path, identifier, lang);
    }

    Stmt enumDeclaration(){
        if( ! currentIs(TokenType.L_CUR))
            Language.error("expected '{' after enum keyword", current.line);
        Token keyword = current;
        advance();

        if( ! currentIs(TokenType.IDENTIFIER))
            Language.error("expected at least 1 identifier inside enum body", current.line);
        var identfiers = new ArrayList<Token>();
        identfiers.add(current);
        advance();

        while(currentIs(TokenType.COMMA)){
            advance();
            if( ! currentIs(TokenType.IDENTIFIER))
                Language.error("expected identfier after ','", current.line);
            identfiers.add(current);
            advance();
        }
        if( ! currentIs(TokenType.R_CUR))
            Language.error("expected closing '}' after enum body", current.line);
        advance();

        return new EnumDecl(keyword, identfiers, lang);


    }

    Stmt classDeclaration(){
        if( ! currentIs(TokenType.IDENTIFIER))
            Language.error("Expected identifier after 'class' keyword", current.line - 1, current.line);
        Token name = current;
        advance();

        if( ! currentIs(TokenType.L_CUR))
            Language.error("Expected '{' after class name", current.line - 1, current.line);
        advance();

        List<Stmt> classBody = new ArrayList<>();
        Function constructer = null;
        Function to_string = null;

        while (currentIs(TokenType.FUNC, TokenType.VAR)){
            if(currentIs(TokenType.FUNC)){
                advance();
                Function method = (Function) function();
                // Check if constructer:
                if(method.name.value.equals("_init"))
                    constructer = method;
                // Check if toString():
                else if(method.name.value.equals("_toString"))
                    to_string = method;
                else
                    classBody.add(method);
            }

            else if(currentIs(TokenType.VAR)){
                advance();
                classBody.add(varDeclaration());
            }
        }

        if( ! currentIs(TokenType.R_CUR))
            Language.error("Expected '}' after class '" + name.value + "' body", name.line);
        advance();

        return new ClassDeclre(classBody, name, constructer, to_string, lang);
    }


    Stmt function(){
        if( ! currentIs(TokenType.IDENTIFIER))
            Language.error("Expected identifier after 'func' keyword", current.line);
        Token name = current;
        advance();

        if( ! currentIs(TokenType.L_PAR))
            Language.error("Expected '(' after function name", name.line, current.line);
        advance();

        List<Token> parameters = new ArrayList<>();

        
        if( ! currentIs(TokenType.R_PAR)){
            if( ! currentIs(TokenType.IDENTIFIER))
                Language.error("Expected parameter name", current.line);
            parameters.add(current);
            advance();
            while(currentIs(TokenType.COMMA)){
                advance();
                if( ! currentIs(TokenType.IDENTIFIER))
                    Language.error("Expected parameter name", current.line);
                parameters.add(current);
                advance();
            }

            if(parameters.size() >= 255)
                Language.error("Can't have more than 255 parameters", current.line);
                
        }

        if( ! currentIs(TokenType.R_PAR))
            Language.error("Expected closing ')'", current.line);
        advance();

        if( ! currentIs(TokenType.L_CUR))
            Language.error("Expected '{' before function body", current.line -1, current.line);
        advance();

        List<Stmt> body = block();

        return new Function(name, parameters, body, lang);


    }

    Stmt varDeclaration(){
        if( ! currentIs(TokenType.IDENTIFIER))
            Language.error("expected identifier after var keyword", current.line);
        Token varName = current;
        advance();

        // Optional varibale initializer:
        Expre initializer = null;
        if(currentIs(TokenType.EQUALS)){
            advance();
            initializer = expression();
        }

        if( ! currentIs(TokenType.EOS))
            Language.error("expected ';' after end of statement", current.line - 1);
        advance();

        return new VarDeclare(varName, initializer, lang);
        
    }
    
    Stmt statement(){
        if(currentIs(TokenType.PRINT, TokenType.PRINT_LN)){
            return printStatement();
        }

        else if(currentIs(TokenType.IF)){
            advance();
            return ifStatement();
        }

        else if(currentIs(TokenType.WHILE)){
            advance();
            return whileStatement();
        }

        else if(currentIs(TokenType.FOR)){
            advance();
            return forStmt();
        }

        else if (currentIs(TokenType.L_CUR)){
            advance();
            return new Block(block(), lang);
        }

        else if(currentIs(TokenType.RETURN))
            return returnStatement();
        
        else if(currentIs(TokenType.CONTINUE))
            return continueStmt();

        else if(currentIs(TokenType.BREAK))
            return breakStmt();
        
        else if(currentIs(TokenType.MATCH))
            return matchStmt();
        

        return expresionStatement();
    }

    Stmt forStmt(){

        if( ! currentIs(TokenType.IDENTIFIER))
            Language.error("expected identfier after 'for' keyword", current.line);
        Token forVar = current;
        Token secondForVar = null;
        advance();

        // for thing, otherThing in iterable...
        if(currentIs(TokenType.COMMA)){
            advance();
            if( ! currentIs(TokenType.IDENTIFIER))
                Language.error("expected identfier after ','", current.line);
            secondForVar = current;
            advance();
        }

        if( ! currentIs(TokenType.IN))
            Language.error("expected 'in' keyword after identfier", current.line);
        advance();

        Expre iterable = expression();

        if( ! currentIs(TokenType.DO))
            Language.error("expected 'do' keyword after iterable", current.line);
        advance();
        Stmt body = statement();

        return new For(forVar, iterable, body, secondForVar, lang);
    }

    Stmt matchStmt(){
        Token keyword = current;
        advance();
        Expre mainExpre = expression();
        if( ! currentIs(TokenType.WITH))
            Language.error("expected 'with' keyword after match expression", current.line);
        advance();

        LinkedHashMap<Expre, Stmt> branches = new LinkedHashMap<>();

        while (currentIs(TokenType.CASE)) {
            advance();
            Expre condition = expression();
            if( ! currentIs(TokenType.ARROW))
                Language.error("expected '->' after match case", current.line);
            advance();
            Stmt branch = statement();
            branches.put(condition, branch);
        }

        Stmt elseBranch = null;
        if(currentIs(TokenType.ELSE)){
            advance();
            if( ! currentIs(TokenType.ARROW))
                Language.error("expected '->' after else match case", current.line);
            advance();
            elseBranch = statement();
        }

        return new Match(keyword, mainExpre, branches, elseBranch);

    }

    Stmt breakStmt(){
        Token keyword = current;
        advance();
        if( ! currentIs(TokenType.EOS))
            Language.error("Expected ';' after break statement", keyword.line);
        advance();

        return new Break(keyword);
    }

    Stmt continueStmt(){
        Token keyword = current;
        advance();
        if( ! currentIs(TokenType.EOS))
            Language.error("Expected ';' after continue statement", keyword.line);
        advance();

        return new Continue(keyword);
    }

    Stmt returnStatement(){
        Token keyword = current;
        advance();
        Expre value = null;
        if ( ! currentIs(TokenType.EOS))
            value = expression();
        
        if( ! currentIs(TokenType.EOS))
            Language.error("Expected ';' after return statement", keyword.line);
        
        advance();

        return new Return(keyword, value);
        
    }

    Stmt whileStatement(){
        Expre condition = expression();
        if( ! currentIs(TokenType.DO))
            Language.error("expected 'do' keyword after while condition", current.line);
        advance();
        Stmt body = statement();

        return new While(condition, body, lang);
    }

    Stmt ifStatement(){

        LinkedHashMap<Expre, Stmt> branches = new LinkedHashMap<>();

        Expre condition = expression();
        if( ! currentIs(TokenType.THEN))
            Language.error("expected 'then' keyword after if condition", current.line);
        advance();
        Stmt thenBranch = statement();
        branches.put(condition, thenBranch);

        while (currentIs(TokenType.ELIF)) {
            advance();
            condition = expression();
            if( ! currentIs(TokenType.THEN))
                Language.error("expected 'then' keyword after elif condition", current.line);
            advance();
            thenBranch = statement();
            branches.put(condition, thenBranch);
        }

        Stmt elseBranch = null;
        if(currentIs(TokenType.ELSE)){
            advance();
            elseBranch = statement();
        }
        return new If(branches, elseBranch);
    }

    List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();
        Token blockStart = tokens.get(pos - 1);

        while( ! currentIs(TokenType.R_CUR))
            statements.add(declaration());
        
        if( ! currentIs(TokenType.R_CUR))
            Language.error("Expected '}' after block.", blockStart.line);
        
        advance();

        return statements;

    }

    Stmt printStatement(){
        Token print_type = current;
        advance();
        Expre value = expression();
        if( ! current.type.equals(TokenType.EOS)){
            if(print_type.line == current.line){
                // The user probably wanted to concatenate strings but forgot the '+':
                Language.error("Expected closing ';' to end print statement\nReminder: you must use '+' to print multiple things", current.line);
            }
            Language.error("Expected closing ';' to end print statement", current.line - 1);
        }
            
        advance();
        return new Print(value, print_type);
        
    }

    Stmt expresionStatement(){
        Expre expre = expression();
        if( ! current.type.equals(TokenType.EOS))
            Language.error("Expected closing ';' to end statement", current.line - 1);
        advance();
        return new ExpressionStmt(expre);
    }
    
    
    Expre expression(){
        return assignment();
    }

    Expre assignment(){
        Expre expre = lambda();

        if(currentIs(TokenType.EQUALS, TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL,
        TokenType.MULTIPLY_EQUAL, TokenType.DIVIDE_EQUAL, TokenType.MOD_EQUAL)){
            Token opp = current;
            advance();
            Expre newValue = assignment();

            if(expre instanceof Variable){
                Token varName = ( (Variable) expre ).token;
                return new AssignExpre(varName, newValue, opp, lang);
            }

            else if(expre instanceof MemberAccess){
                MemberAccess memberToChange = (MemberAccess) expre;
                return new MemberAssign(memberToChange, newValue, opp, lang);
            }

            else if(expre instanceof Subscript)
                return new CollectionAssign( (Subscript) expre, newValue, opp);
            

            Language.error("Invalid assignment target", opp.line);

        }

        return expre;
    }

    Expre lambda(){

        if (currentIs(TokenType.LAMBDA)){
            Token keyword = current;
            advance();
            
            // See if there's parameters:
            List<Token> parameters = new ArrayList<>();

            if( ! currentIs(TokenType.ARROW)){
                if( ! currentIs(TokenType.IDENTIFIER))
                    Language.error("Expected parameter name after lambda delaration", current.line);
                parameters.add(current);
                advance();
                while(currentIs(TokenType.COMMA)){
                    advance();
                    if( ! currentIs(TokenType.IDENTIFIER))
                        Language.error("Expected parameter name", current.line);
                    parameters.add(current);
                    advance();
                }

                if(parameters.size() >= 255)
                    Language.error("Can't have more than 255 parameters", current.line);
                    
            }

            if( ! currentIs(TokenType.ARROW))
                Language.error("Expected closing '->' after lambda parameters", current.line);
            advance();

            Expre body = expression();

            return new Lambda(keyword, parameters, body, lang);
        }
        

        return ternary();

    }

    Expre ternary(){

        Expre left = or();

        if(currentIs(TokenType.IF)){
            Token keyword = current;
            advance();
            Expre middle = expression();
            if(! currentIs(TokenType.ELSE))
                Language.error("expected else after ternary expression condition", current.line);
            advance();
            Expre right = expression();
            left = new Ternary(left, middle, right, keyword);
        }


        return left;


    }

    Expre or(){
        Expre expre = and();
        while (currentIs(TokenType.OR)) {
            Token opToken = current;
            advance();
            Expre right = and();
            expre = new BinBoolOp(expre, opToken, right);
        }

        return expre;
    }

    Expre and(){
        Expre expre = equality();
        while (currentIs(TokenType.AND)) {
            Token opToken = current;
            advance();
            Expre right = equality();
            expre = new BinBoolOp(expre, opToken, right);
        }

        return expre;
    }

    Expre equality(){
        Expre expre = comparison();

        while(currentIs(TokenType.DOUBLE_EQUAL, TokenType.NOT_EQUAL)){
            Token opToken = current;
            advance();
            Expre right = comparison();
            expre = new BinOpExpre(expre, opToken, right, lang);
        }

        return expre;
    }

    Expre comparison(){
        Expre expre = term();

        while(currentIs(TokenType.GREATER_THAN, TokenType.GREATER_THAN_OR_EQUAL, TokenType.LESS_THAN, TokenType.LESS_THAN_OR_EQUAL)){
            Token opToken = current;
            advance();
            Expre right = term();
            expre = new BinOpExpre(expre, opToken, right, lang);
        }

        return expre;
    }

    Expre term(){
        Expre expre = factor();

        while (currentIs(TokenType.PLUS, TokenType.MINUS)) {
            Token opToken = current;
            advance();
            Expre right = factor();
            expre = new BinOpExpre(expre, opToken, right, lang);
        }
        return expre;
    }
    
    Expre factor(){
        Expre expre = unary();

        while(currentIs(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MOD)){
            Token opToken = current;
            advance();
            Expre right = unary();
            expre = new BinOpExpre(expre, opToken, right, lang);
        }

        return expre;
    }

    Expre unary(){
        if(currentIs(TokenType.NOT, TokenType.MINUS)){
            Token opToken = current;
            advance();
            Expre right = unary();
            return new UnaryOpExpre(right, opToken);
        }

        return memberAaccess();
    }

    Expre memberAaccess(){
        Expre expre = call();

        while (currentIs(TokenType.DOT)) {
            Token dot = current;
            advance();
            Expre member = call();
            expre = new MemberAccess(expre, dot, member, lang);
        }

        return expre;
    }

    Expre call(){
        Expre expre = subscript();

        while(true){
            if(currentIs(TokenType.L_PAR)){
                advance();
                expre = finishCall(expre);
            }
            else{
                break;
            }
        }

        return expre;
    }

    Expre finishCall(Expre callee){
        List<Expre> arguments = new ArrayList<>();

        // No args found:
        if(currentIs(TokenType.R_PAR)){
            advance();
            return new Call(callee, current, arguments, lang);
        }

        Expre arg = expression();
        arguments.add(arg);
        while(currentIs(TokenType.COMMA)){

            if(arguments.size() >= 255)
                Language.error("Can't have more than 255 args !", current.line);

            advance();
            arg = expression();
            arguments.add(arg);
        }

        if( ! currentIs(TokenType.R_PAR))
            Language.error("Expected ')' to close call.", current.line);
        advance();

        return new Call(callee, current, arguments, lang);
    }

    Expre subscript(){
        Expre expre = primary();

        while(true){
            if(currentIs(TokenType.L_BRACKET)){
                Token bracket = current;
                advance();
                Expre index = expression();
                if(! currentIs(TokenType.R_BRACKET))
                    Language.error("Expected closing ']' to end index", bracket.line);
                advance();
                expre = new Subscript(bracket, expre, index);
            }
            else{
                break;
            }
        }

        return expre;
    }

    Expre primary(){
        Token tok = current;
        TokenType curType = current.type;

        if(curType.equals(TokenType.TRUE)){
            advance();
            return new LiteralExpre(tok, true);
        }
        else if(curType.equals(TokenType.FALSE)){
            advance();
            return new LiteralExpre(tok, false);
        }
        else if(curType.equals(TokenType.NIL)){
            advance();
            return new LiteralExpre(tok, null);
        }

        else if(currentIs(TokenType.Double, TokenType.STRING)){
            advance();
            return new LiteralExpre(tok, tok.value);
        }
            
        else if(currentIs(TokenType.IDENTIFIER)){
            advance();
            return new Variable(tok, lang);
        }
        
        else if(curType.equals(TokenType.L_PAR)){
            advance();
            Expre expre = expression();
            if( ! current.type.equals(TokenType.R_PAR))
                Language.error("Expected closing ')' !", current.line);
            advance();
            return new GroupingExpre(expre);
        }

        else if(curType.equals(TokenType.L_BRACKET)){
            Token token = current;
            advance();
            List<Expre> listContents = new ArrayList<>();
            if(! currentIs(TokenType.R_BRACKET))
                listContents.add(expression());
            while( ! currentIs(TokenType.R_BRACKET)){
                if( ! currentIs(TokenType.COMMA))
                    Language.error("Expected ',' between list elements", current.line);
                advance();
                listContents.add(expression());
            }
            advance();
            return new ListLiteral(token, listContents, lang);
        }

        else if(curType.equals(TokenType.L_CUR)){

            class Inner {
                Token tok;
                Inner(Token tok){
                    this.tok = tok;
                }

                HashMap<Expre, Expre> dictContents(){

                    AbstractMap.SimpleEntry<Expre, Expre> pair = keyValuePair();
                    HashMap<Expre, Expre> output = new HashMap<>();
                    output.put(pair.getKey(), pair.getValue());

                    while(! currentIs(TokenType.R_CUR)){
                        if( ! currentIs(TokenType.COMMA))
                            Language.error("Expected ',' between Dict elements", tok.line);
                        advance();
                        pair = keyValuePair();
                        output.put(pair.getKey(), pair.getValue());
                    }

                    advance();
                    return output;
                }
                AbstractMap.SimpleEntry<Expre, Expre> keyValuePair(){
                    Expre key = Parser.this.expression();
                    if(! currentIs(TokenType.COLON))
                        Language.error("key value pairs must be separated by ':'", tok.line);
                    advance();
                    Expre value = Parser.this.expression();

                    return new AbstractMap.SimpleEntry<Expre, Expre>(key, value);
                    
                }
            }


            Token token = current;
            advance();
            HashMap<Expre, Expre> hashMap;
            if( ! currentIs(TokenType.R_CUR)){
                Inner inner = new Inner(current);
                hashMap = inner.dictContents();
                return new DictLiteral(token, hashMap, lang);
            }
            if(currentIs(TokenType.R_CUR)){
                advance();
                return new DictLiteral(token, new HashMap<>(), lang);
            }
                

        }

        Language.error("Invalid syntax !", current.line);
        return null;
    }


    void advance(){
        TokenType type = current.type;
        if(type.equals(TokenType.EOF))
            Language.error("cannot advance anymore, already reached EOF !", current.line);
        pos += 1;
        current = tokens.get(pos);
    }

    boolean currentIs(TokenType... allTypes){
        TokenType curType = current.type;
        for (TokenType type : allTypes)
            if(curType.equals(type))
                return true;
        return false;
        
    }

    Token peekNext(){
        return tokens.get(pos + 1);
    }


    }