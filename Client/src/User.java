import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class User {
    String username;
    DatagramSocket datagramSocket;
    static HashMap<String,VideoReader> videoReaders=new HashMap<>();
    static ThreadPoolExecutor threadPoolExecutor;
    int videoWriter;
    int videoControlPort=5555;
    DatagramSocket videoControlSock;

    public User(){
        try {

            this.datagramSocket = new DatagramSocket(5000);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        this.threadPoolExecutor=new ThreadPoolExecutor(4,8,60,TimeUnit.SECONDS,new LinkedBlockingQueue<>());
    }

    public void start() throws Exception{
        videoControlSock=new DatagramSocket(videoControlPort);
        ChatReader chatReader=new ChatReader(datagramSocket,this);
        DatagramPacket datagramPacket;
        threadPoolExecutor.execute(chatReader);
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            System.out.println("1.user 생성 2.chat 보내기 3.이미지 보내기");
            int type = Integer.parseInt(br.readLine());
            String username = null;
            byte packet[];
            JSONObject jsonObject;
            String msg;
            int sendsize=0;
            int filesize=0;
            String ack;

            switch (type) {
                //user 생성
                case 1:
                    System.out.println("user 이름을 입력하세요");
                    username = br.readLine();
                    jsonObject = new JSONObject();
                    jsonObject.put("username", username);
                    jsonObject.put("type", type);
                    packet = jsonObject.toString().getBytes();
                    System.out.println("***********1");
                    datagramPacket = new DatagramPacket(packet, packet.length, InetAddress.getByName("127.0.0.1"), 7777);
                    System.out.println("***********2");
                    datagramSocket.send(datagramPacket);
                    System.out.println("***********3");
                    break;

                case 2:
                    System.out.println("user 이름을 입력하세요");
                    username = br.readLine();
                    System.out.println("메세지를 입력하세요");
                    msg = br.readLine();
                    jsonObject = new JSONObject();
                    jsonObject.put("username", username);
                    jsonObject.put("type", type);
                    jsonObject.put("msg", msg);

                    packet = jsonObject.toString().getBytes();
                    datagramPacket = new DatagramPacket(packet, packet.length, InetAddress.getByName("127.0.0.1"), 7777);
                    datagramSocket.send(datagramPacket);
                    break;

                //video send
                case 3:

                    System.out.println("file 이름을 입력하세요");
                    String filename=br.readLine();
                    File file=new File("image_in/"+filename);
                    FileInputStream fis=new FileInputStream(file);
                    byte file_b[]=fis.readAllBytes();
                    filesize=file_b.length;
                    jsonObject = new JSONObject();
                    jsonObject.put("username", username);
                    jsonObject.put("filename", filename);
                    jsonObject.put("filesize", filesize);
                    packet = jsonObject.toString().getBytes();
                    datagramPacket = new DatagramPacket(packet, packet.length, InetAddress.getByName("127.0.0.1"), videoWriter);
                    videoControlSock.send(datagramPacket);
                    packet=new byte[1000];
                    System.out.println("case3_1"+videoWriter);
                    videoControlSock.receive((datagramPacket=new DatagramPacket(packet,packet.length)));
                    ack=new String(packet,0, datagramPacket.getLength());

                    if(ack.equals("ack")){
                        System.out.println("ack 확인");
                    }
                    System.out.println("case3_2"+filesize);

                    while(sendsize<filesize){
                        if(filesize-sendsize<300){
                            videoControlSock.send((datagramPacket=new DatagramPacket(file_b,sendsize,filesize-sendsize,InetAddress.getByName("127.0.0.1"),videoWriter)));
                            sendsize=filesize;
                        }
                        else{
                            videoControlSock.send((datagramPacket=new DatagramPacket(file_b,sendsize,300,InetAddress.getByName("127.0.0.1"),videoWriter)));
                            sendsize+=300;
                        }
                        System.out.println(sendsize);
                    }

                    System.out.println("case3_3");
                    packet=new byte[1000];
                    videoControlSock.receive((datagramPacket=new DatagramPacket(packet,packet.length)));
                    ack=new String(packet,0, datagramPacket.getLength());
                    System.out.println("case3_4");
                    if(ack.equals("ack")){

                    }
                    break;




            }
            System.out.println("***********");
        }
    }


    static class VideoReader implements Runnable{

            int port;
            DatagramSocket datagramSocket;

            public VideoReader(int port){
                this.port = port ;
            }

            public void run(){
                byte b[]=new byte[1000];
                String filename;
                int filesize=0;
                int sendsize=0;
                String ack="ack";
                System.out.println("VideoReader 1");
                try {
                    datagramSocket = new DatagramSocket(this.port);
                    DatagramPacket datagramPacket;
                    byte[] file_b;
                    while (true) {
                        datagramSocket.receive((datagramPacket=new DatagramPacket(b,b.length)));

                        JSONParser jp=new JSONParser();
                        JSONObject jsonObject=(JSONObject) jp.parse(new String(b,0,datagramPacket.getLength()));
                        filename=(String)jsonObject.get("filename");
                        filesize=Integer.parseInt(String.valueOf(jsonObject.get("filesize")));
                        file_b=new byte[filesize];

                        datagramSocket.send((datagramPacket=new DatagramPacket(ack.getBytes(),ack.getBytes().length,InetAddress.getByName("127.0.0.1"),datagramPacket.getPort())));
                        System.out.println("VideoReader 2");
                        while(sendsize<filesize){
                            datagramSocket.receive((datagramPacket=new DatagramPacket(b,b.length)));

                            for(int i=0;i<datagramPacket.getLength();i++){
                                file_b[sendsize+i]=b[i];
                            }
                            System.out.println("VideoReader 2");
                            sendsize+=datagramPacket.getLength();

                        }
                        System.out.println("VideoReader 3+"+filename);
                        File file=new File("image_out/"+filename);
                        FileOutputStream fo=new FileOutputStream(file);
                        fo.write(file_b);
                        fo.close();
                        System.out.println("VideoReader 4");
                        datagramSocket.send((datagramPacket=new DatagramPacket(ack.getBytes(),ack.getBytes().length,InetAddress.getByName("127.0.0.1"),datagramPacket.getPort())));


                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }

    }


    static class ChatReader implements Runnable{
        DatagramSocket datagramSockets;
        String username;
        VideoReader vr;
        User user;
        int port;
        ChatReader(DatagramSocket datagramSocket,User user){
            this.datagramSockets=datagramSocket;
            this.user=user;
        }
        public void run(){
            byte b[]=new byte[1000];
            DatagramPacket datagramPacket;
            while(true){
                try {
                    datagramSockets.receive((datagramPacket=new DatagramPacket(b,b.length)));
                    JSONParser jp=new JSONParser();
                    System.out.println(new String(b,0,datagramPacket.getLength()));
                    JSONObject jsonObject=(JSONObject) jp.parse(new String(b,0,datagramPacket.getLength()));
                    int type=Integer.parseInt(String.valueOf(jsonObject.get("type")));

                    switch(type){

                        case 2:
                            System.out.println((String)jsonObject.get("msg"));
                            break;

                        case 3://create videoReader
                            username=(String)jsonObject.get("username");
                            port=Integer.parseInt(String.valueOf(jsonObject.get("port")));
                            videoReaders.put(username,(vr=new VideoReader(port)));
                            threadPoolExecutor.execute(vr);
                            System.out.println("create videoReader");
                            break;

                        case 4://VideoWriter
                            username=(String)jsonObject.get("username");
                            port=Integer.parseInt(String.valueOf(jsonObject.get("port")));
                            user.videoWriter=port;
                            System.out.println("videoWriter"+port);
                            System.out.println("create videoWriter");
                            break;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
