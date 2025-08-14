import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class Stmt{
    abstract Object visit();
}

class ExpressionStmt extends Stmt {
    Expre expresion;
    ExpressionStmt(Expre expresion){
        this.expresion = expresion;
    }
    Void visit(){
        expresion.visit();
        return null;
    }

}

class Print extends Stmt {
    Expre expresion;
    Token printType;
    Print(Expre expresion, Token printType){
        this.expresion = expresion;
        this.printType = printType;
    }

    Void visit(){
        Object value = expresion.visit();
        if(printType.type.equals(TokenType.PRINT_LN))
            System.out.println(Language.stringify(value));
        else if(printType.type.equals(TokenType.PRINT))
            System.out.print(Language.stringify(value));
        return null;
    }

}

class VarDeclare extends Stmt {
    Token varName;
    Expre initializer;
    Language lang;

    VarDeclare(Token varName, Expre initializer, Language lang){
        this.varName = varName;
        this.initializer = initializer;
        this.lang = lang;
    }

    @Override
    Void visit() {
        Object value = null;
        if(initializer != null)
            value = initializer.visit();
        lang.environment.define(varName.value.toString(), value);
        return null;
    }

}

class Block extends Stmt {

    List<Stmt> statements;
    Environment environment;
    Language lang;

    Block(List<Stmt> statements, Language lang){
        this.statements = statements;
        this.lang = lang;
    }

    @Override
    Void visit() {

        Environment prev = lang.environment;
        lang.enviStack.add(prev);
        lang.environment = new Environment(prev);
        
        for (Stmt stmt : statements) 
            stmt.visit();
        
        while(lang.environment != prev)
            lang.environment = lang.enviStack.pop();
        
        
        return null;
    }
}

class If extends Stmt {

    LinkedHashMap<Expre, Stmt> branches = new LinkedHashMap<>();
    Stmt elseBranch;

    If(LinkedHashMap<Expre, Stmt> branches, Stmt elseBranch){
        this.branches = branches;
        this.elseBranch = elseBranch;
    }

    @Override
    Void visit() {

        for (Map.Entry<Expre, Stmt> entry : branches.entrySet()) {
            Expre condition = entry.getKey();
            Stmt thenBranch = entry.getValue();

            Object result = condition.visit();
            if( result instanceof Boolean == false)
                Language.error("if statement condition should result in a boolean value", condition.token.line);
            if(result.equals(true)){
                thenBranch.visit();
                return null;
            }
                
        }
        
        if (elseBranch != null)
            elseBranch.visit();
        
        return null;
        
        
    }
    
}

class BreakException extends RuntimeException {
    BreakException(Token keyword){
        super("at line " + keyword.line +": break statement must be inside a while loop block", null, false, false);
    }
}

class ContinueException extends RuntimeException {
    ContinueException(Token keyword){
        super("at line " + keyword.line + ": continue statement must be inside a while loop block", null, false, false);
    }
}

class While extends Stmt {
    Expre condition;
    Stmt body;
    Language lang;
    While(Expre condition, Stmt body, Language lang){
        this.condition = condition;
        this.body = body;
        this.lang = lang;
    }
    @Override
    Void visit() {
        Environment lasEnvi = lang.environment;
        // Try block for the break statement:
        try {
            while (condition.visit().equals(true)){
                // Try block for the continue statement:
                try {
                    body.visit();
                }
                catch(ContinueException e){
                    while (lang.environment != lasEnvi) 
                        lang.environment = lang.enviStack.pop();
                }
            }

                
        }
        catch(BreakException e){
            while (lang.environment != lasEnvi) 
                lang.environment = lang.enviStack.pop();
        }

    return null;
    }
}

class For extends Stmt {
    Token identfier;
    Expre iterable;
    Stmt body;
    Language lang;
    Token second_identfier;

    For(Token identfier, Expre iterable, Stmt body, Token second_identfier, Language lang){
        this.identfier = identfier;
        this.iterable = iterable;
        this.body = body;
        this.second_identfier = second_identfier;
        this.lang = lang;
    }

    @Override
    Void visit() {
        var iterable = this.iterable.visit();
        var list = new ArrayList<Object>();
        if(iterable instanceof String)
            for (char c : ((String) iterable).toCharArray())
                list.add(c + "");
        else if(iterable instanceof ListInstance)
            for (Object object : ((ListInstance) iterable).arrayList)
                list.add(object);
        else if (iterable instanceof SamirPairList)
            for (SamirPair pair : ((SamirPairList) iterable).list)
                list.add(pair);
        else
            Language.error("Can only iterate over strings, Lists, and PairLists", lang.line);
            
        Environment lasEnvi = lang.environment;
        Environment newEnvi = new Environment(lasEnvi);
        lang.enviStack.add(lasEnvi);
        lang.environment = newEnvi;

        if((iterable instanceof String || iterable instanceof ListInstance) && second_identfier != null)
            Language.error("Can only use 2 variables in a for loop to iterate over PairList", identfier.line);
        
        boolean unpacking = second_identfier != null;

        // Try block for the break statement:
        try {
            for(Object item : list){

                newEnvi.define((String) identfier.value, item);
                if(unpacking){
                    newEnvi.define((String) identfier.value, ((SamirPair) item).first);
                    newEnvi.define((String) second_identfier.value, ((SamirPair) item).second);
                }

                // Try block for the continue statement:
                try {
                    body.visit();
                }
                catch(ContinueException e){
                    while (lang.environment != lasEnvi) 
                        lang.environment = lang.enviStack.pop();
                    lang.enviStack.add(lasEnvi);
                    lang.environment = newEnvi;
                }
            } 
        }
        catch(BreakException e){
            while (lang.environment != lasEnvi) 
                lang.environment = lang.enviStack.pop();
        }

        while (lang.environment != lasEnvi) 
            lang.environment = lang.enviStack.pop();
        
        return null;
    }
}

class Match extends Stmt {

    Token keyword;
    Expre mainExpre;
    LinkedHashMap<Expre, Stmt> branches;
    Stmt elseBranch;

    Match(Token keyword, Expre mainExpre, LinkedHashMap<Expre, Stmt> branches, Stmt elseBranch){
        this.keyword = keyword;
        this.branches = branches;
        this.mainExpre = mainExpre;
        this.elseBranch = elseBranch;
    }

    @Override
    Void visit() {
        Object mainExpreVisitd = mainExpre.visit();
        if(mainExpreVisitd == null)
            Language.error("Can't match null", keyword.line);
        for (Map.Entry<Expre, Stmt> entry : branches.entrySet()) {
            Expre condition = entry.getKey();
            Stmt stmt = entry.getValue();

            Object result = condition.visit();
            if(mainExpreVisitd.equals(result)){
                stmt.visit();
                return null;
            }
                
        }
        
        if (elseBranch != null)
            elseBranch.visit();
        
        return null;
    }

    
}

class Function extends Stmt {
    Token name;
    List<Token> parameters;
    List<Stmt> body;
    Language lang;

    Function(Token name, List<Token> parameters, List<Stmt> body, Language lang){
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.lang = lang;
    }

    @Override
    Void visit() {
        SamirFunction function = new SamirFunction(this, lang.environment, lang);
        lang.environment.define(name.value.toString(), function);
        return null;
    }
}

class Return extends Stmt {
    Token keyword;
    Expre value;
    Return(Token keyword, Expre value){
        this.keyword = keyword;
        this.value = value;
    }
    @Override
    Void visit() throws ReturnException {
       Object value = null;
       if (this.value != null) value = this.value.visit();

       throw new ReturnException(value, keyword);
    }
}

class Continue extends Stmt {
    Token keyword;
    Continue(Token keyword){
        this.keyword = keyword;
    }

    @Override
    Object visit() {
        throw new ContinueException(keyword);
    }
}

class Break extends Stmt {
    Token keyword;
    Break(Token keyword){
        this.keyword = keyword;
    }

    @Override
    Object visit() {
        throw new BreakException(keyword);
    }
}

class ClassDeclre extends Stmt {
    List<Stmt> classBody;
    List<Token> parameters;
    Function to_string;
    Token name;
    Language lang;
    ClassDeclre(List<Stmt> classBody, Token name, List<Token> parameters, Function to_string, Language lang){
        this.classBody = classBody;
        this.name = name;
        this.parameters = parameters;
        this.to_string = to_string;
        this.lang = lang;
    }

    Void visit(){
        SamirClass samir_class = new SamirClass(this, lang.environment, lang);
        lang.environment.define(name.value.toString(), samir_class);
        return null;
    }
}

class EnumDecl extends Stmt {
    Token keyword;
    List<Token> identfiers;
    Language lang;
    EnumDecl(Token keyword, List<Token> identfiers, Language lang){
        this.keyword = keyword;
        this.identfiers = identfiers;
        this.lang = lang;
    }
    @Override
    Void visit() {
        for (int i = 0; i < identfiers.size(); i++)
            lang.environment.define(identfiers.get(i).value.toString(), Language.int_to_Double(i));
        return null;
    }
}

class Import extends Stmt {
    Token keyword;
    String string_path;
    Token identfier;
    Language lang;
    Import(Token keyword, String string_path, Token identfier, Language lang){
        this.keyword = keyword;
        this.string_path = string_path;
        this.identfier = identfier;
        this.lang = lang;
    }
    @Override
    Void visit() {

        if(Thread.currentThread().getStackTrace().length > 500)
            Language.error("Please remove any circular dependency in file imports", keyword.line);

        Path path = Paths.get(string_path);
        // Local file path:
        if( ! path.isAbsolute()){
            Path parent_dir = Paths.get(lang.samir_script_filepath).getParent();
            path = parent_dir.resolve(path);
        }

        Language new_lang = new Language(path.toString());
        new_lang.run();
        SamirInstance import_instance = new Importinstance(new_lang, identfier);
        lang.environment.define((String) identfier.value, import_instance);
        return null;
    }
}