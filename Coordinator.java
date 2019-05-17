import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Coordinator implements Runnable {
    int port;
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
    public Coordinator(String[] args) throws IOException {
        this.port=Integer.parseInt(args[0]);
        serverSocket=new ServerSocket(port);
        this.numberOfParticipants=Integer.parseInt(args[1]);
        this.participants=new HashMap<>(numberOfParticipants);

        this.options=new ArrayList<  >();
        for(int i=2;i<args.length;i++)
            options.add(args[i]);//add the options

        //ready
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
        setUpVote();
        System.exit(0);
    }
    private void setUpVote()throws IOException{
        sendDetails();
        sendVoteoptions();
        getState();
        String out=getOutCome();
        if(out.equals("")){
            String op=options.get(options.size()-1);
            options.remove(options.size()-1);//remove last one
            System.out.println("Draw : "+op);
            setUpVote();
        }else
            System.out.println("Coordinator print the results :"+out);

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
    private void getState(){
        String msg;
        ArrayList<Integer> listremove=new ArrayList<>();
        for (Map.Entry<Integer, SpecialSocket> en : this.participants.entrySet()) {
            try {
                msg=en.getValue().getString();
                if(msg.equals("FAIL")){//crashe somewhere which implies don't expect OUTCOME message
                    en.getValue().close();//close the connection between this particular participant
                   // participants.remove(en.getKey());   //not needed any mo
                    listremove.add(en.getKey());
                }//otherwise DONE

            }catch (IOException exe){};
            }
        listremove.forEach(x->participants.remove(x));//removed carshed keys
    }
//    private String getOutcome()throws IOException{
//            String msg;
//            for (Map.Entry<Integer, SpecialSocket> en : this.participants.entrySet()) {
//                try {
//                en.getValue().getSocket().setSoTimeout(1000);
//                msg=en.getValue().getString();
//                System.out.println("Coordinator RECEIVE:From " + en.getKey() + ": " + msg);
//                en.getValue().close();//close the connection between this particular participant
//                participants.remove(en.getKey());   //not needed any mo
//                updateParticipant(msg);
//                getOutComeFromRest();
//                return msg;
//                }catch (IOException exe){
//
//                }
//            }
//
//           return getOutcome();//use recursion
//        //return outcome;
//    }
    private void updateParticipant(String msg){
        String split[]=msg.split(" ");
        ArrayList<Integer> participantVotes=stringsToInt(2,split);
        for(Integer key:this.participants.keySet()){
            if(!participantVotes.contains(key))//in case the participant haven't vote
                this.participants.get(key).close();
                //this.participants.remove(key);//remove cause the implies the participant has failed during the vote
        }
    }
    private String getOutCome() throws IOException{
        String msg="";
        for (Map.Entry<Integer, SpecialSocket> en : this.participants.entrySet()) {
                try {
                    msg = en.getValue().getString();
                    if (msg.equals(" "))
                    en.getValue().close();//close the connection
                    System.out.println("Coordinator RECEIVE:From " + en.getKey() + ": " + msg);
                }catch (IOException exe){

                }
        }
        //checking
        return msg;
    }
    private ArrayList<Integer> stringsToInt(int startIndex,String split[]){
        ArrayList<Integer> list=new ArrayList<>();
        for (int i=startIndex;i<split.length;i++){
            list.add(Integer.parseInt(split[i]));
        }
        return list;
    }
    private String formString(ArrayList<? extends Object> ids,String title){
        if(ids.size()==0)
            return "";
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
    public static void main(String[] args) {
        try {
            Coordinator cord = new Coordinator(args);
            cord.run();
        }catch (IOException exe){};
    }
}

