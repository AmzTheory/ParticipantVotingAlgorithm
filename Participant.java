import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class Participant implements Runnable {
    ServerSocket participantServer;
    int serverPort;
    int id;
    int timeout=5000;
    String optionChosen;
    SpecialSocket socketServer;
//    HashMap<Integer,SpecialSocket> participants;
    ArrayList<Integer> participants;
    HashMap<Integer,String> participantVotes;
    ArrayList<String> options;
    public Participant(int id,int serverPort) throws IOException {
        this.serverPort=serverPort;
        this.id=id;
        participantServer = new ServerSocket(this.id);
        socketServer =new SpecialSocket(serverPort);
        participants=new ArrayList<>();
        participantVotes=new HashMap<>();
        options=new ArrayList<>();
    }

    public void join()throws IOException{
        String msg="JOIN "+id;
        socketServer.writeString(msg);

        String msgReq= socketServer.getString();
        connectToParticipant(msgReq);

        String msgVotes= socketServer.getString();
        storeVotes(msgVotes);

        listForVotes();//getVotes from other participants


    }
    private void establishConnectionWithAll() throws IOException {
        SpecialSocket soc;
        for (Integer key:this.participants) {
            soc=new SpecialSocket(key);
            soc.writeString(Integer.toString(id));//inform the server socketServer about ur id
        }
    }
    private void connectToParticipant(String msg) throws IOException{
        String words[]=msg.split(" ");
        //check the title (it should be DETAILS)
        String[] listOfParticpant=words[1].split(",");
        int temp;
        SpecialSocket tempSoc;
        for(String part:listOfParticpant){
            temp=Integer.parseInt(part);
            if(temp!=id) {//check that its not itself
                participants.add(temp);
            }
        }
    }
    private void storeVotes(String msg){
        String words[]=msg.split(" ");
        //check the title (it should be VOTE_OPTIONS)
        String[] listOfOpts=words[1].split(",");
        SpecialSocket tempSoc;
        for(String opt:listOfOpts){
            this.options.add(opt);
        }
    }
    public void listenToParticpants() throws IOException{
        ArrayList<SpecialSocket> spSocket=new ArrayList<>();
        boolean done=false;
        //separate threading running that listen for other participant messages
        Thread listen=new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    final ServerSocket socket = new ServerSocket(id);
                    SpecialSocket sp;
                    int i = 0;
                    int port=0;

                    //establish connection (without timeout)
                    while (i < getParticipantsSize()) {
                        sp=new SpecialSocket(socket.accept());
                        port=Integer.parseInt(sp.getString());
                        sp.setClientPort(port);
                        System.out.println(id+ " establish connection with "+port);
                        i++;
                    }
                    //round 1 (with timeout)
                    LinkedList<SpecialSocket> specialSockets=new LinkedList<>();
                    //separate thread to listen for participant
                    Thread listenVotes =new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int j = 0;
                                SpecialSocket sp;
                                while (j < getParticipantsSize()) {
                                    specialSockets.offer(new SpecialSocket(socket.accept()));
                                    j++;
                                }
                            } catch (IOException ex) {
                            }

                        }
                    });
                    //participant need to send its vote
                    listenVotes.start();
                    sendVote();
                    try {
                        Thread.sleep(timeout);
                    }catch (InterruptedException exp){}

                    listenVotes.interrupt();
                    String msg;
                    for(SpecialSocket inst:specialSockets){
                        msg=inst.getString();
                        System.out.println(id+" RECEIVE: "+msg);
                        storeVoteOption(inst.getString());
                    }
                    //round 2


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        listen.start();
        //handle errors
    }

    //concerning round 1
    private void listForVotes() throws IOException {
        //round 1 (with timeout)
        LinkedList<SpecialSocket> specialSockets=new LinkedList<>();
        //separate thread to listen for participant
        Thread listenVotes =new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int j = 0;
                    SpecialSocket sp;
                    while (j < getParticipantsSize()) {
                        specialSockets.offer(new SpecialSocket(participantServer.accept()));
                        j++;
                    }
                } catch (IOException ex) {
                }

            }
        });
        //participant need to send its vote
        listenVotes.start();
        try {
            sendVote();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(timeout);
        }catch (InterruptedException exp){}

        listenVotes.interrupt();
        String msg;
        for(SpecialSocket inst:specialSockets){
            msg=inst.getString();
            System.out.println(id+" RECEIVE: "+msg);
            storeVoteOption(msg);
        }
    }
//    private void listForResults() throws IOException {
//        //round 1 (with timeout)
//        LinkedList<SpecialSocket> specialSockets=new LinkedList<>();
//        //separate thread to listen for participant
//        Thread listenVotes =new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    int j = 0;
//                    SpecialSocket sp;
//                    while (j < getParticipantsSize()) {
//                        specialSockets.offer(new SpecialSocket(participantServer.accept()));
//                        j++;
//                    }
//                } catch (IOException ex) {
//                }
//
//            }
//        });
//        //participant need to send its vote
//        listenVotes.start();
//        try {
//            sendVote();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            Thread.sleep(timeout);
//        }catch (InterruptedException exp){}
//
//        listenVotes.interrupt();
//        String msg;
//        for(SpecialSocket inst:specialSockets){
//            msg=inst.getString();
//            System.out.println(id+" RECEIVE: "+msg);
//            storeVoteOption(msg);
//        }
//    }

    //concerning round 2

    //round #1
    public void storeVoteOption(String msgVote){
        String words[]=msgVote.split(" ");

        //check word[0] is VOTE
        int id=Integer.parseInt(words[1]);
        String optionChosen=words[2];

        this.participantVotes.put(id,optionChosen);
    }
    private Participant getCopy(){
        return this;
    }


    //round #1
    public void sendVote()throws IOException{
        int index=new Random().nextInt(3);
         optionChosen=this.options.get(index);//vote is randomly chosen
//         optionChosen=this.options.get(0);
        SpecialSocket temp;
        String msg;
        //send the vote to all of the participants
        for (Integer idp: this.participants) {
            msg="VOTE "+this.id+" "+optionChosen;
            new SpecialSocket(idp).writeString(msg);
            System.out.println("SEND: "+msg+"  from -> "+this.id+"  to -> "+idp);
        }
    }

    //round #2(will be called by an inner thread)
    public void sendVotesToThers() throws IOException{
        String msg=formatSendVotesMessage();
        //send the vote results to all participants
        SpecialSocket soc;
        for (Integer key:this.participants) {
            soc=new SpecialSocket(key);
            soc.writeString(msg);
            System.out.println("SEND: "+msg+"  from -> "+this.id+"  to -> "+soc.getSocket().getPort());
        }
    }

    //round #2
    //will be in the form
    //VOTE PORT:OPTION PORT:OPTION PORT:OPTION
    private String formatSendVotesMessage(){
        String msg="VOTE";
        String current;
        for (Integer key:this.participantVotes.keySet()) {
            current=this.participantVotes.get(key);
            msg+=" "+key+":"+current;
        }
        //include its current option (not sure if this is needed)
        msg+=" "+id+":"+optionChosen;
        return msg;
    }

    //receive of round #2
    private void getVotesFromParticipant() throws IOException{

        for (SpecialSocket soc:this.participants.values()) {
            soc.getString();
        }

    }


    //the bottom function are synchronized

    //remove it in case of failure
    public synchronized void removeParticipant(int port){
        this.participants.remove(port);
    }
    public synchronized int getParticipantsSize(){
        int size = this.participants.size();
        return size;
    }

    @Override
    public void run() {
        try {
            this.join();
        }catch (IOException exp){
            System.out.println("Something went wrong");
        }
    }

}

