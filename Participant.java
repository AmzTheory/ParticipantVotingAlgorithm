

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

public class Participant implements Runnable {
    ServerSocket participantServer;
    int serverPort;
    int id;
    int timeout=5000;
    int failcond;

    String optionChosen;
    SpecialSocket socketServer;
    ArrayList<Integer> participants;
    HashMap<Integer,String> participantVotes;
    HashMap<String,Integer> options;
    public Participant(int id,int serverPort,int failcond) throws IOException {
        this.serverPort=serverPort;
        this.id=id;
        this.failcond=failcond;//this need to be modified
        participantServer = new ServerSocket(this.id);
        socketServer =new SpecialSocket(serverPort);
        participants=new ArrayList<>();
        participantVotes=new HashMap<>();
        options=new HashMap<>();
    }

    public void join()throws IOException{
        try {

            String msg = "JOIN " + id;
            socketServer.writeString(msg);

            String msgReq = socketServer.getString();
            connectToParticipant(msgReq);

            String msgVotes = socketServer.getString();
            storeVotes(msgVotes);
            listForVotes();//getVotes from other participants
            listForResults();//getResult from other particpant

            if(failcond==2)
                throw new InterruptedException();
            //compare Results
            String outComeMessage = getOutcomeMessage();
            socketServer.writeString(outComeMessage);
            // System.out.println("SEND from "+id+":"+ outComeMessage);
            //inform the coordinator with OUTCOME
        }catch (InterruptedException ex){
        };

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
            this.options.put(opt,0);
        }
    }

    //concerning round 1
    private void listForVotes() throws IOException,InterruptedException {
        //round 1 (with timeout)
        LinkedList<SpecialSocket> specialSockets=new LinkedList<>();
        //separate thread to listen for participant
        Thread listenVotes =new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int j = 0;
                    SpecialSocket sp;
                    String msg;
                    while (j<getParticipantsSize()) {
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
        if(failcond==1)
            throw new InterruptedException();

        try {
            listenVotes.join(timeout);
        }catch (InterruptedException exp){}

        participantServer.close();
        String msg;
        for(SpecialSocket inst:specialSockets){
            msg=inst.getString();
            System.out.println(id+" RECEIVE: "+msg);
            storeVoteOption(msg);
        }
    }
    private void listForResults() throws IOException {

        participantServer=new ServerSocket(id);
        //separate thread to listen for participant
        Thread listenVotes =new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int j = 0;
                    SpecialSocket sp;
                    while (true) {

                        sp=new SpecialSocket(participantServer.accept());
                        final String msg=sp.getString();//deal with string one by one
                        System.out.println("RECEIVE:"+sp.getSocket().getLocalPort()+" "+msg);
                        new Thread(new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    ArrayList<Integer> news = verifyVote(msg);
                                    if(news==null){
                                        //do nothing(incorrect message (discard)
                                    }else if (news.size() != 0)//in case there aren't any discovered new votes
                                        sendAll(formatSendVotesMessage(news));
                                }catch (IOException exe){

                                }
                            }
                        }).start();

                        j++;
                    }
                } catch (IOException ex) {

                }

            }
        });
        //participant need to send its vote
        listenVotes.start();
        try {
            sendVotesToOthers();
        } catch (IOException e) {}

        try {
            listenVotes.join(timeout);
            participantServer.close();
        }catch (InterruptedException exp){}
        //At this stage we've passed the timeout


    }

    //concerning round 2

    //round #1
    public void storeVoteOption(String msgVote){
        String words[]=msgVote.split(" ");

        //check word[0] is VOTE
        if(!words[0].toLowerCase().equals("vote"))
            return; //discard message (message written in the wrong format)
        int id=Integer.parseInt(words[1]);
        String optionVote=words[2];
        Integer current=options.get(optionVote);
        this.options.put(optionVote,current+1);//increment by one
        this.participantVotes.put(id,optionVote);
    }
    private ArrayList<Integer> verifyVote(String msg){
        if(!msg.contains(":"))
            return null;// just to avoid getting message from previous round(after timeout)
        String records[]=msg.split(" ");
        String split[];
        int key,current;
        String optionVote;
        ArrayList<Integer> newOnes=new ArrayList<>();
        for(int i=1;i<records.length;i++){
            split=records[i].split(":");
            key=Integer.parseInt(split[0]);
            optionVote=split[1];
            if(!this.participantVotes.containsKey(key)) {
                current=options.get(optionVote);
                this.options.put(optionVote,current+1);//increment by one
                this.participantVotes.put(key,optionVote);
                newOnes.add(key);
            }
        }
        return newOnes;//in case this empty list,that implies there hasn't any new ones
    }


    //round #1
    public void sendVote()throws IOException{
        int index=new Random().nextInt(3);
        optionChosen=(String)this.options.keySet().toArray()[index];//vote is randomly chosen
        int current=options.get(optionChosen);
        this.options.put(optionChosen,current+1);//increment by one
        this.participantVotes.put(this.id,optionChosen);
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
    public void sendVotesToOthers() throws IOException{
        ArrayList<Integer> list=new ArrayList<>(this.participantVotes.keySet());
        list.remove(new Integer(this.id));//remove yourself cause this is not new
        String msg=formatSendVotesMessage(list);
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
    private String formatSendVotesMessage(ArrayList<Integer> list){
        String msg="VOTE";
        String current;
        for (Integer key:list) {
            current = this.participantVotes.get(key);
            msg += " " + key + ":" + current;
        }
        return msg;
    }

    private void sendAll(String msg)throws IOException{
        for (Integer id:this.participants) {
            new SpecialSocket(id).writeString(msg);
            System.out.println("Send "+msg+ " from "+this.id);
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
    public synchronized String participantsToString(){
        String msg="";
        for (Integer id:participantVotes.keySet()){
            msg+=id.toString()+" ";
        }
        return msg.trim();
    }
    private String getOutcomeMessage(){
        String msg="OUTCOME";
        String optionWon="";
        int high=0;
        int hits=1;
        //improve it using priority queue
        for(Map.Entry<String, Integer> en:this.options.entrySet()){
            if(en.getValue()>high){
                high=en.getValue();
                hits=1;
                optionWon=en.getKey();
            }else if(en.getValue()==high){
                hits+=1;
            }
        }
        if(hits>1)//in case of draw(undecided)
            return msg+" null "+participantsToString();

        return msg+" "+optionWon+" "+participantsToString();
    }


    @Override
    public void run() {
        try {
            this.join();
        }catch (IOException exp){
           exp.printStackTrace();
        }
    }

}

