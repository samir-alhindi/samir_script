import java.util.ArrayList;

public class Lexer {

    String source;
    int pos = 0;
    int line = 0;
    char current;
    ArrayList<Token> tokens = new ArrayList<>();

    // NOTE: true and false aren't keywords but rather boolean literals, nil is just null:
    // I put them with the keywords to make things easier:
    final String[] keywords = {"var", "and", "or", "not", "if", "then", "elif", "else",
    "func", "false", "true", "nil", "print", "println", "do", "while", "return", "class", "break", "continue",
    "lambda", "match", "with", "case", "for", "in", "enum", "import", "as"};

    Lexer(String source){
        this.source = source;
    }

    ArrayList<Token> lex (){

        if(source.length() == 0)
            Language.error("empty file !", 0);

        tokens.clear();
        current = source.charAt(0);

        while (current != '\0') {

            //Check if char is space or tab or 'CR' (carriage return):
            if(current == ' ' || current == '\t' || current == '\r')
                advance();
            
            //Increase line counter if we hit a newline char:
            else if(isNewlineChar(current)){
                line += 1;
                advance();
            }
            
            //Check if char is a semicolon (end of statement):
            else if(current == ';'){
                addToken(TokenType.EOS);
                advance();
            }

            //Check if char is comment:
            else if(current == '#'){
                while ( ! isNewlineChar(current) && current != '\0') {
                    advance();
                }
            }

            // Comma:
            else if(current == ','){
                addToken(TokenType.COMMA);
                advance();
            }

            // Dot
            else if(current == '.'){
                addToken(TokenType.DOT);
                advance();
            }

            // Colon
            else if(current == ':'){
                addToken(TokenType.COLON);
                advance();;
            }
                
        
            //Check if char is digit:
            else if(Character.isDigit(current)){

                //Creating Double token:
                String num_str = "";
                boolean found_dot = false;
            
                while (Character.isDigit(current) || current == '.') {
                    if(Character.isDigit(current)){
                        num_str += current;
                        advance();
                    }
                    else if(current == '.' && ! found_dot){
                        found_dot = true;
                        num_str += current;
                        advance();
                    }
                    //Error cases:
                    else if(current == '.' && found_dot)
                        Language.error("Number can't have more than 1 dot !!!", line);
                    
                }
                Double num_val = Double.valueOf(num_str);
                addToken(TokenType.Double, num_val);
        }

            else if(current == '"'){
                String string = "";
                int stringStart = line;
                advance();
                while (current != '"' && current != '\0') {
                    string += current;
                    if(isNewlineChar(current))
                        line += 1;
                    advance();
                }
                if(current == '\0')
                    Language.error("unterminated string", stringStart);
                addToken(TokenType.STRING, string);
                advance();
            }

            // Check if char is Compound assignment operator:

            else if(current == '+' && currentFollowedBy('=')){
                addToken(TokenType.PLUS_EQUAL);
                advanceTwo();
            }
            else if(current == '-' && currentFollowedBy('=')){
                addToken(TokenType.MINUS_EQUAL);
                advanceTwo();
            }
            else if(current == '*' && currentFollowedBy('=')){
                addToken(TokenType.MULTIPLY_EQUAL);
                advanceTwo();
            }
            else if(current == '/' && currentFollowedBy('=')){
                addToken(TokenType.DIVIDE_EQUAL);
                advanceTwo();
            }
            else if(current == '%' && currentFollowedBy('=')){
                addToken(TokenType.MOD_EQUAL);
                advanceTwo();
            }

            // Check if char is lambda arrow '->':
            else if(current == '-' && currentFollowedBy('>')){
                addToken(TokenType.ARROW);
                advanceTwo();
            }
        

        
            //Check if char is opperator:

            else if(current == '+'){
                addToken(TokenType.PLUS);
                advance();}
            else if(current == '-'){
                addToken(TokenType.MINUS);
                advance();}
            else if(current == '*'){
                addToken(TokenType.MULTIPLY);
                advance();}
            else if(current == '/'){
                addToken(TokenType.DIVIDE);
                advance();}
            else if(current == '%'){
                addToken(TokenType.MOD);
                advance();
            }

        //check if char is equals sign:
        else if(current == '='){
            //Check if '==':
            if(currentFollowedBy('=')){
                addToken(TokenType.DOUBLE_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.EQUALS);
                advance();
            }
        }

        //Check if char is '!' equals sign:
        else if(current == '!'){
            if(currentFollowedBy('=')){
                addToken(TokenType.NOT_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.NOT);
                advance();
            }
        }
        
        //Check if char is '>' than:
        else if(current == '>'){
            if(currentFollowedBy('=')){
                addToken(TokenType.GREATER_THAN_OR_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.GREATER_THAN);
                advance();
            }
        }

        //Check if char is '<' than:
        else if(current == '<'){
            if(currentFollowedBy('=')){
                addToken(TokenType.LESS_THAN_OR_EQUAL);
                advanceTwo();
            }
            else{
                addToken(TokenType.LESS_THAN);
                advance();
            }
        }

        //Check if char is parentheses:
        else if (current == '('){
            addToken(TokenType.L_PAR);
            advance();
        }
        else if (current == ')'){
            addToken(TokenType.R_PAR);
            advance();
        }

        //Check if char is curly braces:
        else if (current == '{'){
            addToken(TokenType.L_CUR);
            advance();
        }
        else if (current == '}'){
            addToken(TokenType.R_CUR);
            advance();
        }

        else if (current == '['){
            addToken(TokenType.L_BRACKET);
            advance();
        }

        else if (current == ']'){
            addToken(TokenType.R_BRACKET);
            advance();
        }

        //Check if char is identifier/keyword:
        else if(Character.isLetter(current) || current == '_'){
            String word = "";
        
            while (Character.isLetter(current) || current == '_' || Character.isDigit(current)){
                word += current;
                advance();
            }

            //Check if word is keyword and tokenize it:
            Token token = new Token(null, line);
            if(is_keyword(word)){
                switch (word) {
                    case "var" -> token.type = TokenType.VAR;
                    case "and" -> token.type = TokenType.AND;
                    case "or" -> token.type = TokenType.OR;
                    case "if" -> token.type = TokenType.IF;
                    case "then" -> token.type = TokenType.THEN;
                    case "while" -> token.type = TokenType.WHILE;
                    case "do" -> token.type = TokenType.DO;
                    case "else" -> token.type = TokenType.ELSE;
                    case "elif" -> token.type = TokenType.ELIF;
                    case "not" -> token.type = TokenType.NOT;
                    case "func" -> token.type = TokenType.FUNC;
                    case "true" -> token.type = TokenType.TRUE;
                    case "false" -> token.type = TokenType.FALSE;
                    case "nil" -> token.type = TokenType.NIL;
                    case "print" -> token.type = TokenType.PRINT;
                    case "println" -> token.type = TokenType.PRINT_LN;
                    case "return" -> token.type = TokenType.RETURN;
                    case "class" -> token.type = TokenType.CLASS;
                    case "break" -> token.type = TokenType.BREAK;
                    case "continue" -> token.type = TokenType.CONTINUE;
                    case "lambda" -> token.type = TokenType.LAMBDA;
                    case "match" -> token.type = TokenType.MATCH;
                    case "with" -> token.type = TokenType.WITH;
                    case "case" -> token.type = TokenType.CASE;
                    case "for" -> token.type = TokenType.FOR;
                    case "in" -> token.type = TokenType.IN;
                    case "enum" -> token.type = TokenType.ENUM;
                    case "import" -> token.type = TokenType.IMPORT;
                    case "as" -> token.type = TokenType.AS;
                }
                tokens.add(token);
            }
            else
                addToken(TokenType.IDENTIFIER, word);
        }

        else
            Language.error(current + "Is Illegal char !!!", line);

        }

        // End of file token:
        addToken(TokenType.EOF);

        return tokens;

        
    }

    //Helper lexing methods:

    private void addToken(TokenType tokenType){
        Token token = new Token(tokenType, line);
        tokens.add(token);
    }

    private void addToken(TokenType tokenType, Object value){
        Token token = new Token(tokenType, value, line);
        tokens.add(token);
    }

    private void advance(){
        // Check if reached end of line:
        if (pos + 1 >= source.length())
            current = '\0';
        else {
            pos += 1;
            current = source.charAt(pos);
        }
    }

    private void advanceTwo(){
        // Check if reached end of line:
        if (pos + 2 >= source.length())
            current = '\0';
        else {
            pos += 2;
            current = source.charAt(pos);
        }
    }

    private boolean isNewlineChar(char c){
        return c == '\n';
    }

    private char peekNext(){
        if(pos + 1 >= source.length())
            return '\0';
        return source.charAt(pos + 1);
    }

    private boolean currentFollowedBy(char c){
        if(peekNext() == c)
            return true;
        return false;
        }

    //Method to check if word is keyword:
    private boolean is_keyword(String word){
        for (int i = 0; i < keywords.length; i++) 
            if(keywords[i] .equals(word)) return true;
        return false;
        
    }
}