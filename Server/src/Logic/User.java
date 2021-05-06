package Logic;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ThreadPoolExecutor;

public class User {
    String username;
    DatagramSocket datagramSocket;
    DatagramSocket datagramSocket2;
    DatagramSocket videoSockWriter;
    int chatPortRead;
    int videoControlport=3333;
    int videoPortWrite;
    ThreadPoolExecutor tpe;
    HashMap<String,Integer> videoPortReaders=new HashMap<>();
    static int VIDEOPORT_READ=1000;
    static int VIDEOPORT_WRITE=2000;

    static class VideoWriter implements Runnable{
        int port;
        DatagramSocket datagramSocket;
        User user;
        public VideoWriter(int port,User user){
            this.port=port;
            this.user=user;
        }

        public void run(){
            byte b[]=new byte[1000];
            String filename;
            int filesize=0;
            int sendsize=0;
            String ack="ack";
            System.out.println("sendImage1");
            try {
                datagramSocket = new DatagramSocket(this.port);
                DatagramPacket datagramPacket;
                byte[] file_b;
                while (true) {
                    System.out.println("sendImage2 port:"+port);
                    datagramSocket.receive((datagramPacket=new DatagramPacket(b,b.length)));
                    JSONParser jp=new JSONParser();
                    JSONObject jsonObject=(JSONObject) jp.parse(new String(b,0,datagramPacket.getLength()));
                    filename=(String)jsonObject.get("filename");
                    filesize=Integer.parseInt(String.valueOf(jsonObject.get("filesize")));
                    file_b=new byte[filesize];
                    System.out.println("sendImage3");
                    datagramSocket.send((datagramPacket=new DatagramPacket(ack.getBytes(),ack.getBytes().length,InetAddress.getByName("127.0.0.1"),datagramPacket.getPort())));

                    System.out.println("sendImage4");

                    while(sendsize<filesize){
                        datagramSocket.receive((datagramPacket=new DatagramPacket(b,b.length)));

                        for(int i=0;i<datagramPacket.getLength();i++){
                            file_b[sendsize+i]=b[i];
                        }

                        sendsize+=datagramPacket.getLength();
                        System.out.println(sendsize);

                    }


                    File file=new File("img/"+filename);
                    FileOutputStream fo=new FileOutputStream(file);
                    fo.write(file_b);
                    fo.close();

                    user.sendImage(filename);

                    datagramSocket.send((datagramPacket=new DatagramPacket(ack.getBytes(),ack.getBytes().length,InetAddress.getByName("127.0.0.1"),datagramPacket.getPort())));

                    System.out.println("sendImage5");
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public User(String username, DatagramSocket ds, int chatPortRead, ThreadPoolExecutor tpe){
        this.username=username;
        this.datagramSocket=ds;
        this.chatPortRead=chatPortRead;
        this.tpe=tpe;
        try {
            this.datagramSocket2 = new DatagramSocket(videoControlport);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setVideoWriter() throws Exception{
        DatagramPacket datagramPacket;
        String username="";
        JSONObject jo;
        byte b[];
        jo=new JSONObject();
        jo.put("type",4);
        jo.put("username",username);
        jo.put("port",VIDEOPORT_WRITE);
        b=jo.toString().getBytes();
        datagramSocket.send((datagramPacket=new DatagramPacket(b,b.length,InetAddress.getByName("127.0.0.1"),5000)));
        VideoWriter videoWriter=new VideoWriter(VIDEOPORT_WRITE,this);
        tpe.execute(videoWriter);
        VIDEOPORT_WRITE++;
    }

    public void setVideoReader(Vector<User> users) throws Exception{
        DatagramPacket datagramPacket;
        JSONObject jo;
        byte[] b;
        for(User user:users){
            if(!videoPortReaders.containsKey(user.username)){
                videoPortReaders.put(user.username,VIDEOPORT_READ);
                jo=new JSONObject();
                jo.put("type",3);
                jo.put("username",user.username);
                jo.put("port",VIDEOPORT_READ);
                b=jo.toString().getBytes(StandardCharsets.UTF_8);
                datagramSocket.send((datagramPacket=new DatagramPacket(b,b.length,InetAddress.getByName("127.0.0.1"),5000)));
                VIDEOPORT_READ++;
            }
        }
    }

    public void sendMessage(User u,String msg) throws Exception{
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("type",2);
        jsonObject.put("msg",msg);
        byte msg_b[]=jsonObject.toString().getBytes(StandardCharsets.UTF_8);
        DatagramPacket datagramPacket=new DatagramPacket(msg_b,msg_b.length, InetAddress.getByName("127.0.0.1"),chatPortRead);
        datagramSocket.send(datagramPacket);
    }



    public void sendImage(String filename) throws Exception{
        int port;
        byte packet[];
        JSONObject jsonObject;
        String msg;
        //주석**********************************
        System.out.println("sendImage");
        int filesize=0;
        String ack;
        DatagramPacket datagramPacket;
        Set<String> Keys= videoPortReaders.keySet();

        File file=new File("img/"+filename);
        FileInputStream fis=new FileInputStream(file);
        byte file_b[]=fis.readAllBytes();
        filesize=file_b.length;

        for(String key:Keys){
            int sendsize=0;
            port=Integer.parseInt(String.valueOf(videoPortReaders.get(key)));
            jsonObject = new JSONObject();
            jsonObject.put("username", username);
            jsonObject.put("filename", filename);
            jsonObject.put("filesize", filesize);
            packet = jsonObject.toString().getBytes();
            datagramPacket = new DatagramPacket(packet, packet.length, InetAddress.getByName("127.0.0.1"), port);
            datagramSocket2.send(datagramPacket);

            packet=new byte[1000];
            datagramSocket2.receive((datagramPacket=new DatagramPacket(packet,packet.length)));

            ack=new String(packet,0, datagramPacket.getLength());
            System.out.println("sendImage2");
            if(ack.equals("ack")){
                System.out.println("sendImage4");
            }

            while(sendsize<filesize){
                if(filesize-sendsize<300){
                    datagramSocket2.send((datagramPacket=new DatagramPacket(file_b,sendsize,filesize-sendsize,InetAddress.getByName("127.0.0.1"),port)));
                    sendsize=filesize;
                }
                else{
                    datagramSocket2.send((datagramPacket=new DatagramPacket(file_b,sendsize,300,InetAddress.getByName("127.0.0.1"),port)));
                    sendsize+=300;
                }
                System.out.println("sendImage5");
            }
            System.out.println("sendImage3");
            packet=new byte[1000];
            datagramSocket2.receive((datagramPacket=new DatagramPacket(packet,packet.length)));
            ack=new String(packet,0, datagramPacket.getLength());

            if(ack.equals("ack")){

            }

        }
        fis.close();
    }




}
