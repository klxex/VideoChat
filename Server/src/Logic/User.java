package Logic;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class User {
    String username;
    DatagramSocket ds;
    int chatPort_out;
    int videoPort_in;
    ThreadPoolExecutor tpe;
    HashMap<String,Integer> videoPorts_out;

    public User(String username, DatagramSocket ds, int chatPort_out, ThreadPoolExecutor tpe){
        this.username=username;
        this.ds=ds;
        this.chatPort_out=chatPort_out;
        this.tpe=tpe;
    }

    public void setVideoPort_in(int port){
        DatagramPacket datagramPacket=new DatagramPacket();

    }

    public void setVideoPort_out(String name,int videoPort){

    }

    public void sendMessage(){

    }

    public void sendImage(){

    }




}
