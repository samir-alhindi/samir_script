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
            statements.add(statement());
        }

        return statements;
    }


    ///Helper parsing methods///
    
    Stmt statement(){
        if(currentIs(TokenType.PRINT)){
            advance();
            return printStatement();
        } 

        return expresionStatement();
    }

    Stmt printStatement(){
        Expre value = expression();
        if( ! current.type.equals(TokenType.EOS))
            Language.error("Expected closing ';' to end statement", current.line);
        advance();
        return new Print(value);
        
    }

    Stmt expresionStatement(){
        Expre expre = expression();
        if( ! current.type.equals(TokenType.EOS))
            Language.error("Expected closing ';' to end statement", current.line);
        advance();
        return new ExpressionStmt(expre);
    }
    
    
    Expre expression(){
        return equality();
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