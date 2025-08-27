
public class Main {
    public static void main(String[] args) {
        if(args.length == 1){
            String samir_script_filepath = args[0];
            Runtime lang = new Runtime(samir_script_filepath);
            lang.run();
        }
        // For debuging:
        else{
            Runtime lang = new Runtime("samir_script_programs\\subscript_assign.smr");
            lang.run();
        }
        
    }
}

