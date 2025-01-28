import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while(true){
            System.out.print("> ");
            String input = scanner.nextLine();
            double result = Language.run(input);
            System.out.println(result);
        }
    }
}