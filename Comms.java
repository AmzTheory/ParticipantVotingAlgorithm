;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Comms {
    static String IP="127.0.0.1";
    static int port=1666;
    public Comms(){
    }

    /**
     * This method will be called when the clien
     * @param msg
     * @return
     */
    public static SpecialSocket sendMessage(String msg){

           SpecialSocket socket = null;
           try {
               socket = new SpecialSocket();
            //   socket.writeMessage(msg);
           }catch (Exception exp) {

           }
       return socket;
    }

    public static String receiveMessage(SpecialSocket socket){
        try {
           // Message message=socket.getMessage();
            socket.close();
            return "";
        }catch (Exception exp){
            return "";
        }
    }
    //Server recieve Message
    public static void receiveMessage() {
        class ServerThread implements Runnable{
            private Socket socket;
            private ServerSocket serverSoc;
            private SpecialSocket soc;
            @Override
            public void run() {
                try{
                    serverSoc=new ServerSocket(port);
                    while(true) {
                        socket = serverSoc.accept();
                        try {
                            soc = new SpecialSocket(socket);
                        } catch (Exception exe) {}

                        }
                }catch (Exception ex){

                }
                new Thread(new ServerThread()).start();
            }
        }
        //while(true) {
            new Thread(new ServerThread()).start();
       // }
    }




}
