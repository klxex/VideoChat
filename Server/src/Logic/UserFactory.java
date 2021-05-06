package Logic;

import java.net.DatagramSocket;
import java.util.Vector;
import java.util.concurrent.ThreadPoolExecutor;

public class UserFactory {

    static Vector<User> users=new Vector<>();

    public static User createUser(String username, DatagramSocket ds, int chatPort_out, ThreadPoolExecutor tpe){
        User user=new User(username,ds,chatPort_out,tpe);
        users.add(user);
        return user;
    }

}
