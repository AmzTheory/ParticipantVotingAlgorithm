import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Coordinator implements Runnable {
    static int port;
    ServerSocket serverSocket;
    int numberOfParticipants;
    HashMap<Integer,SpecialSocket> participants;
    ArrayList<String> options;

    public Coordinator(int port,int numberOfParticipants) throws IOException {
        serverSocket=new ServerSocket(port);
        this.numberOfParticipants=numberOfParticipants;
        this.participants=new HashMap<>(numberOfParticipants);
        this.options=new ArrayList<  >();
        options.add("A");
        options.add("B");
        options.add("C");
    }

    @Override
    public void run() {
        try {
            this.listenForParticipant();
        }catch (Exception exp){
            exp.printStackTrace();
        }
    }

    public void listenForParticipant() throws IOException{
        int i=0;
        SpecialSocket soc;
        while(i<numberOfParticipants){
            soc=new SpecialSocket(serverSocket.accept());
            String msg=soc.getString();
            int id=extractPort(msg);
            addToSockets(id,soc);
            i++;
        }
        sendDetails();
        sendVoteoptions();
        String outcome=getOutcome();
        System.out.println("Coordinator print the results :"+outcome);

    }
    private void addToSockets(Integer id,SpecialSocket soc){
        this.participants.put(id,soc);
    }
    private int extractPort(String msg){
        String word[]=msg.split(" ");
        //check its Joint
        //check the second word can be converted to digit
        return  Integer.parseInt(word[1]);
    }
    private void sendDetails()throws  IOException{
        HashMap<Integer,SpecialSocket> temp;
        String msg;
        for(Integer id:participants.keySet()){
            temp= (HashMap<Integer, SpecialSocket>) participants.clone();
            temp.remove(id);
            msg=formString(new ArrayList<>(temp.keySet()),"DETAILS ");
            send(this.participants.get(id),msg);
        }
    }
    private void sendVoteoptions()throws  IOException{
        String msg=formString(options,"VOTE_OPTIONS ");
        participants.values().stream().forEach(x->send(x,msg));
    }

    private String getOutcome()throws IOException{
            String msg;
            for (Map.Entry<Integer, SpecialSocket> en : this.participants.entrySet()) {
                try {
                en.getValue().getSocket().setSoTimeout(1000);
                msg=en.getValue().getString();
                System.out.println("Coordinator RECEIVE:From " + en.getKey() + ": " + msg);
                en.getValue().close();//close the connection between this particular participant
                participants.remove(en.getKey());   //not needed any mo
                updateParticipant(msg);
                getOutComeFromRest();
                return msg;
                }catch (IOException exe){

                }
            }

           return getOutcome();//use recursion
        //return outcome;
    }
    private void updateParticipant(String msg){
        String split[]=msg.split(" ");
        ArrayList<Integer> participantVotes=stringsToInt(2,split);
        for(Integer key:this.participants.keySet()){
            if(!participantVotes.contains(key))//in case the participant haven't vote
                this.participants.get(key).close();
                //this.participants.remove(key);//remove cause the implies the participant has failed during the vote
        }
    }
    private void getOutComeFromRest() throws IOException{
        String msg;
        for (Map.Entry<Integer, SpecialSocket> en : this.participants.entrySet()) {
                try {
                    en.getValue().getSocket().setSoTimeout(1000);
                    msg = en.getValue().getString();
                    en.getValue().close();//close the connection
                    System.out.println("Coordinator RECEIVE:From " + en.getKey() + ": " + msg);
                }catch (IOException exe){

                }
        }
    }
    private ArrayList<Integer> stringsToInt(int startIndex,String split[]){
        ArrayList<Integer> list=new ArrayList<>();
        for (int i=startIndex;i<split.length;i++){
            list.add(Integer.parseInt(split[i]));
        }
        return list;
    }
    private String formString(ArrayList<? extends Object> ids,String title){
        String ret=title;
        for(int i=0;i<ids.size()-1;i++){
            ret+=ids.get(i)+",";
        }
        ret+=ids.get(ids.size()-1);
        return ret;
    }
    private String send(SpecialSocket soc,String msg) {
        try {
            soc.writeString(msg);
        }catch (IOException exe){

        }
        return "";
    }

}

