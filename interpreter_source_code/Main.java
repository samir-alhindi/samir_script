
public class Main {
    public static void main(String[] args) {
        if(args.length == 1){
            String samir_script_filepath = args[0];
            Language lang = new Language(samir_script_filepath);
            lang.run();
        }
        // For debuging:
        else{
            Language lang = new Language("samir_script_programs\\class_members.smr");
            lang.run();
        }
        
    }
}

