public class Token {
    TokenType type;
    Object value;
    int line;
    String file_name;
    Token(TokenType type, Object value, int line, String file_name){
        this.type = type;
        this.value = value;
        this.line = line;
        this.file_name = file_name;
    }
    //Constructer for things without values:
    Token(TokenType type, int line, String file_name){
        this.type = type;
        this.line = line;
        this.file_name = file_name;
    }

    @Override
    public String toString() {
        return  "[" + type + " : " + value + "]";
    }
}