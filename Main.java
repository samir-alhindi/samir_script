import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while(true){
            System.out.print("> ");
            String input = scanner.nextLine();
            Double result = Language.run(input);
            if(result != null)
                System.out.println(result);
        }
    }
}