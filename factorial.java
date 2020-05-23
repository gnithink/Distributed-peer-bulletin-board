import java.util.Scanner;

class factorial {
    public static void main(String[] args) {
    int num;
    int factorial = 1 ;
    Scanner scan = null;
    do{
        System.out.println("Enter a positive integer: " );
       try{
            scan = new Scanner(System.in);
            num = scan.nextInt();
        }
        finally{
            if(scan != null)
                scan.close();
        }
    }
    while (num<0);

    
    for (int i = 1; i<= num; i++)
        factorial = factorial *i;
    
    System.out.println(num + "! = " + factorial);
    }
}