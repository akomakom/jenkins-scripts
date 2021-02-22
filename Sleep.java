// for jstack debugging
public class Sleep {
    public static void main(String[] args) throws InterruptedException {
        int sleep = 60;
        if (args.length > 0) {
            sleep = Integer.valueOf(args[0]);
        }
        System.out.println("Sleeping for " + sleep);
        Thread.sleep(sleep * 1000);
    }
}
