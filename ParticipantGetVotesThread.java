import java.io.IOException;

public class ParticipantGetVotesThread extends Thread {
    Participant part;
    SpecialSocket soc;


    public ParticipantGetVotesThread(Participant part,SpecialSocket soc){
        this.part=part;
        this.soc=soc;
    }

    @Override
    public void run() {
        try {
            String msg;
            msg = soc.getString();
            System.out.println("RECEIVE: "+msg +"   in -> "+this.soc.getSocket().getLocalPort());
            part.storeVoteOption(msg);
            //here this particular should be ready for round#2
            part.sendVotesToThers();
        }catch (IOException ex){
        }
    }
}
