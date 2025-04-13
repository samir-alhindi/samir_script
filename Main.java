import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Language lang = new Language();

        //Testing
        lang.file("test.smr");

        if(args.length > 1){
            System.exit(64);
        }
        else if(args.length == 1){
            String file_name = args[0];
            lang.file(file_name);
        }
        else{
            while(true){
                System.out.print("> ");
                String input = scanner.nextLine();
                lang.repl(input, false);
            }
        }
    }
}