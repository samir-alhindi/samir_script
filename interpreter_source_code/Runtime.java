import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class Runtime {

    Environment globals = new Environment();
    Environment environment = globals;
    Stack<Environment> enviStack = new Stack<>();

    int line = 1;
    String cur_file_name;

    String samir_script_filepath;
    Scanner scanner;

    Runtime(String samir_script_filepath){
        this.samir_script_filepath = samir_script_filepath;
        scanner = new Scanner(System.in);
    }

    void run(){
        NativeFunctions natives = new NativeFunctions(globals, this);
        natives.init();
        String source = Util.read_source(samir_script_filepath, this);
        List<Stmt> program = Util.lex_then_parse(source, this, Paths.get(samir_script_filepath).getFileName().toString());
        for (Stmt stmt : program) 
            stmt.visit();
    }

      static Object error(String log, int line, String file){
        System.out.printf("Error in file: %s at line %d: %s", file, line + 1, log);
        System.exit(1);
        return null;
    }


}