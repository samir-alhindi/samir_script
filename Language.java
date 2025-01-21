import java.util.ArrayList;

class Language {

    //// running ////
    static double run (String input){

        ///Tokenization///
        
        ArrayList<Token> tokens = new ArrayList<>();
        
        for (int i = 0; i < input.length(); i++) {
        
        //Check if char is white space:
        if(input.charAt(i) == ' ') continue;
        
        //Check if char is digit:
        else if(Character.isDigit(input.charAt(i))){

            //Creating Double token:
            String num_str = "";
            boolean found_dot = false;
            
            while (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.') {
                if(Character.isDigit(input.charAt(i))){
                    num_str += input.charAt(i);
                    i++;
                    if(i >= input.length()) break;
                }
                else if(input.charAt(i) == '.' && !found_dot){
                    found_dot = true;
                    num_str += input.charAt(i);
                    i++;
                    if(i >= input.length()) break;
                }
                //Error cases:
                else if(input.charAt(i) == '.' && found_dot){
                    error("Number can't have more than 1 dot !!!");
                    return 0.0;
                }
            }

            
            Token token = new Token(TokenType.Double, Double.parseDouble(num_str));
            tokens.add(token);
            
        }
        
        //Check if char is Boolean opperator:
        else if(input.charAt(i) == '+'){
            Token token = new Token(TokenType.PLUS, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '-'){
            Token token = new Token(TokenType.MINUS, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '*'){
            Token token = new Token(TokenType.MULTIPLY, 0);
            tokens.add(token);
        }
        else if(input.charAt(i) == '/'){
            Token token = new Token(TokenType.DIVIDE, 0);
            tokens.add(token);
        }

        else {
            error(input.charAt(i) + " is Illegal char !");
            return 0.0;
        }

        }

        System.out.println(tokens); // debugging.
        return 0.0;
    }

    static void error(String log){
        System.out.println("Error: " + log);
    }
}


class Token {
    TokenType type;
    double value;
    Token(TokenType type, double value){
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return  "[" + type + " : " + value + "]";
    }
}

enum TokenType{Double, PLUS, MINUS, MULTIPLY, DIVIDE}