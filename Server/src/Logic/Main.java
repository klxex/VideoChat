package Logic;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String args[]) throws Exception{

        byte b[];
        DatagramSocket datagramSocket=new DatagramSocket(7777);
        ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(4,8,60, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        DatagramPacket dp;
        while(true){
            b=new byte[9000];
            datagramSocket.receive((dp=new DatagramPacket(b,b.length)));
            JSONParser jp=new JSONParser();
            System.out.println(new String(b,0,dp.getLength()));
            JSONObject jsonObject=(JSONObject) jp.parse(new String(b,0,dp.getLength()));
            User user=null;
            String username;
            String msg;
            String filename;
            int type=Integer.parseInt(String.valueOf(jsonObject.get("type")));

            switch(type){
                //user 생성
                case 1:
                    System.out.println("ok1");
                    username=(String)jsonObject.get("username");
                    user=UserFactory.createUser(username,datagramSocket,dp.getPort(),threadPoolExecutor);
                    user.setVideoWriter();
                    for(User u:UserFactory.users){
                        u.setVideoReader(UserFactory.users);
                    }
                    break;

                //chat 보내기
                case 2:
                    System.out.println("ok2");
                    username=(String)jsonObject.get("username");
                    msg=(String)jsonObject.get("msg");

                    for(User u:UserFactory.users){
                        if(u.username.equals(username)){
                            user=u;
                        }
                    }

                    for(User u:UserFactory.users) {
                        user.sendMessage(u,msg);
                    }
                    break;


            }

        }
    }
}
