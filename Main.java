public class Main {
    static int cordPort=1000;
    public static void main(String[] args) {
	// write your code here

        try {
            Coordinator cord = new Coordinator(cordPort, 3);
            runRunnable(cord);

            Participant one=new Participant(1001,cordPort,0);
            Participant two=new Participant(1002,cordPort,1);
            Participant three=new Participant(1003,cordPort,0);
            runRunnable(one);
            runRunnable(two);
            runRunnable(three);
        }catch (Exception ex){

        }

    }
    public static void runRunnable(Runnable r){
      new Thread(r).start();
    }
}
