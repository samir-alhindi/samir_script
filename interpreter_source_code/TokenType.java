public enum TokenType{

    // Primary literals:
    Double, STRING, TRUE, FALSE, NIL,

    // Numeric opperators:
    PLUS, MINUS, MULTIPLY, DIVIDE, MOD,

    // Compound assignment operators (+=, -=...):
    PLUS_EQUAL, MINUS_EQUAL, MULTIPLY_EQUAL, DIVIDE_EQUAL, MOD_EQUAL,

    // Boolean opperators:
    AND, OR, NOT, 

    // Relational opperators:
    DOUBLE_EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN,
    GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, 

    // Grouping:
    L_PAR, R_PAR, L_CUR, R_CUR, L_BRACKET, R_BRACKET,
    
    // keywords:
    VAR, IF, ELSE, ELIF, FUNC, WHILE, PRINT, PRINT_LN, THEN, DO,
    RETURN, CLASS, BREAK, CONTINUE, LAMBDA, MATCH, WITH, CASE,
    FOR, IN, ENUM, IMPORT, AS, 

    // Other:
    IDENTIFIER, EQUALS, EOS, EOF, COMMA, DOT, ARROW, COLON,
    }