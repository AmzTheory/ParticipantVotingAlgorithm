

import java.io.*;
import java.net.Socket;

public class SpecialSocket extends Socket {
    private static String IP="127.0.0.1";
    private static int port=1666;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int clientPort;
    boolean recieved;


    public static SpecialSocket createDummy() throws IOException{
        SpecialSocket s=new SpecialSocket(true);
        s.setSocket(null);
        s.in=null;
        s.out=null;
        return s;
    }

    public SpecialSocket(boolean f){
        //don't init any of the memebers
    }
    public SpecialSocket() throws IOException{
        this.setSocket(new Socket(IP,port));
        initStreams();


    }
    //this used for Server Socket
    public SpecialSocket(Socket socket) throws IOException{
        this.setSocket(socket);
        initStreams();
    }
    //this used for Server Socket
    public SpecialSocket(Socket socket,int clientPort) throws IOException{
        this.setSocket(socket);
        initStreams();
        this.clientPort=clientPort;
    }
    public SpecialSocket(int port) throws IOException{
        this.setSocket(new Socket(IP,port));
        initStreams();
    }


    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    private void  initStreams() throws IOException{
        this.in=new DataInputStream(this.getSocket().getInputStream());
        this.out=new DataOutputStream(this.getSocket().getOutputStream());
    }

    public Socket getSocket() {
        return socket;
    }

    public String getString()throws IOException{
        return in.readUTF();
    }
    public void writeString(String message) throws IOException{
        out.writeUTF(message);
    }


    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public void close(){
        try{
            socket.close();
            in.close();
            out.close();
            in=null;
            out=null;
        }catch (Exception ex){

        }
    }


}
