public class Token {
    TokenType type;
    Object value;
    int line;
    Token(TokenType type, Object value, int line){
        this.type = type;
        this.value = value;
        this.line = line;
    }
    //Constructer for things without values:
    Token(TokenType type, int line){
        this.type = type;
        this.line = line;
    }

    @Override
    public String toString() {
        return  "[" + type + " : " + value + "]";
    }
}