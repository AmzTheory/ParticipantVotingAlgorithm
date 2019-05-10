import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class ParticipantServerThread extends Thread {
    Participant part;
    LinkedList<SpecialSocket> sockets;
    public ParticipantServerThread(Participant part){
        super();
        this.part=part;
    }
    @Override
    public void run() {
        SpecialSocket soc=new SpecialSocket(true);
        try {
            //SpecialSocket spSoc;
            ServerSocket socket = new ServerSocket(part.id);
            int i=0;
            Socket client;
            int port;
            while(i<this.part.getParticipantsSize()){
                soc=new SpecialSocket(socket.accept());
                //port=Integer.parseInt(soc.getString());
                //soc.setClientPort(port);
                new ParticipantGetVotesThread(part,soc).start();//run get votes thread
                i++;
            }




        } catch (IOException e) {
            int portclient=soc.getClientPort();
            this.part.removeParticipant(portclient);
        }
    }
}
