import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
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
        this.options=new ArrayList<>();
        options.add("A");
        options.add("B");
        options.add("C");
    }

    @Override
    public void run() {
        try {
            this.listenForParticipant();
        }catch (Exception exp){}
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
        sendOutcome();

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

    private void sendOutcome(){
        //wait until
        for(Integer id:participants.keySet()){
            String outcome=getOutcome();
            final String msg=formString(options,"OUTCOME");//this line is not done
            participants.values().stream().forEach(x->send(x,msg));
        }
    }

    private String getOutcome(){
        return "dummy";
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
