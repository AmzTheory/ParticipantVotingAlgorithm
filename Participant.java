import com.sun.security.ntlm.Server;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Participant implements Runnable {
    int serverPort;
    int id;
    int timeout=5000;
    String optionChosen;
    SpecialSocket socket;
    HashMap<Integer,SpecialSocket> participants;
    HashMap<Integer,SpecialSocket> serverSockets;//used to keep track of all server to be able to comunicate  back with the participant with the same socket
    HashMap<Integer,String> participantVotes;
    ArrayList<String> options;
    public Participant(int id,int serverPort) throws IOException {
        this.serverPort=serverPort;
        this.id=id;
        socket=new SpecialSocket(serverPort);
        participants=new HashMap<>();
        participantVotes=new HashMap<>();
        options=new ArrayList<>();
    }

    public void join()throws IOException{
        String msg="JOIN "+id;
        socket.writeString(msg);

        String msgReq=socket.getString();
        connectToParticipant(msgReq);

        listenToParticpants();//communicate with other participants
        establishConnectionWithAll();

        String msgVotes=socket.getString();
        storeVotes(msgVotes);

        sendVote();//send the participant votes to all of the other participants
    }
    private void establishConnectionWithAll() throws IOException {
        for (Integer key:this.participants.keySet()) {
            participants.put(key,new SpecialSocket(key));
            participants.get(key).writeString(Integer.toString(id));//inform the server socket about ur id
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
                participants.put(temp, null);
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
                        System.out.println("RECEIVE:");
                        storeVoteOption(ins.get);
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
        for (Integer idp: this.participants.keySet()) {
            temp= this.participants.get(idp);
            msg="VOTE "+this.id+" "+optionChosen;
            temp.writeString(msg);
            System.out.println("SEND: "+msg+"  from -> "+this.id+"  to -> "+idp);
        }
    }

    //round #2(will be called by an inner thread)
    public void sendVotesToThers() throws IOException{
        String msg=formatSendVotesMessage();
        //send the vote results to all participants
        for (SpecialSocket soc:this.participants.values()) {
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
        //wait until you've received all of the votes(this could be done outside)

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

