import java.util.ArrayList;
import java.util.List;

public class Parser{
    static int pos = 0;
    static Token current;
    static ArrayList<Token> tokens = new ArrayList<>();

    Parser(ArrayList<Token> tokens){
        Parser.tokens = tokens;
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
            

        return statement();
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
            Language.error("expected ';' after end of statement", current.line);
        advance();

        return new VarDeclare(varName, initializer);
        
    }
    
    Stmt statement(){
        if(currentIs(TokenType.PRINT, TokenType.PRINT_LN)){
            return printStatement();
        }

        else if (currentIs(TokenType.L_CUR)){
            advance();
            return new Block(block());
        }

        return expresionStatement();
    }

    List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while( ! currentIs(TokenType.R_CUR))
            statements.add(declaration());
        
        if( ! currentIs(TokenType.R_CUR))
            Language.error("Expected '}' after block.", current.line);
        
        advance();

        return statements;

    }

    Stmt printStatement(){
        Token print_type = current;
        advance();
        Expre value = expression();
        if( ! current.type.equals(TokenType.EOS))
            Language.error("Expected closing ';' to end statement", current.line);
        advance();
        return new Print(value, print_type);
        
    }

    Stmt expresionStatement(){
        Expre expre = expression();
        if( ! current.type.equals(TokenType.EOS))
            Language.error("Expected closing ';' to end statement", current.line);
        advance();
        return new ExpressionStmt(expre);
    }
    
    
    Expre expression(){
        return assignment();
    }

    Expre assignment(){
        Expre expre = equality();

        if(currentIs(TokenType.EQUALS)){
            Token equals = current;
            advance();
            Expre newValue = assignment();

            if(expre instanceof Variable){
                Token varName = ( (Variable) expre ).token;
                return new AssignExpre(varName, newValue);
            }

            Language.error("Invalid assignment target", equals.line);

        }

        return expre;
    }

    Expre equality(){
        Expre expre = comparison();

        while(currentIs(TokenType.DOUBLE_EQUAL, TokenType.NOT_EQUAL)){
            Token opToken = current;
            advance();
            Expre right = comparison();
            expre = new BinOpExpre(expre, opToken, right);
        }

        return expre;
    }

    Expre comparison(){
        Expre expre = term();

        while(currentIs(TokenType.GREATER_THAN, TokenType.GREATER_THAN_OR_EQUAL, TokenType.LESS_THAN, TokenType.LESS_THAN_OR_EQUAL)){
            Token opToken = current;
            advance();
            Expre right = term();
            expre = new BinOpExpre(expre, opToken, right);
        }

        return expre;
    }

    Expre term(){
        Expre expre = factor();

        while (currentIs(TokenType.PLUS, TokenType.MINUS)) {
            Token opToken = current;
            advance();
            Expre right = factor();
            expre = new BinOpExpre(expre, opToken, right);
        }
        return expre;
    }
    
    Expre factor(){
        Expre expre = unary();

        while(currentIs(TokenType.MULTIPLY, TokenType.DIVIDE)){
            Token opToken = current;
            advance();
            Expre right = unary();
            expre = new BinOpExpre(expre, opToken, right);
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

        return primary();
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
            return new Variable(tok);
        }

        else if(currentIs(TokenType.INPUT_NUM, TokenType.INPUT_STR)){
            advance();
            return new Input(tok);
        }
        
        else if(curType.equals(TokenType.L_PAR)){
            advance();
            Expre expre = expression();
            if( ! current.type.equals(TokenType.R_PAR))
                Language.error("Expected closing ')' !", current.line);
            advance();
            return new GroupingExpre(expre);
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


    }