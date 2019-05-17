public class Main {
    static int cordPort=1000;
    public static void main(String[] args) {
	// write your code here

        try {
            Coordinator cord = new Coordinator(cordPort, 6);
            runRunnable(cord);

            Participant one=new Participant(1001,cordPort,0);
            Participant two=new Participant(1002,cordPort,0);
            Participant three=new Participant(1003,cordPort,0);
            Participant four=new Participant(1004,cordPort,0);
            Participant five=new Participant(1005,cordPort,1);
            Participant six=new Participant(1006,cordPort,1);
            runRunnable(one);
            runRunnable(two);
            runRunnable(three);
            runRunnable(four);
            runRunnable(five);
            runRunnable(six);
        }catch (Exception ex){

        }

    }
    public static void runRunnable(Runnable r){
      new Thread(r).start();
    }
}
