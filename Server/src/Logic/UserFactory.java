package Logic;

import java.net.DatagramSocket;
import java.util.Vector;

public class UserFactory {

    static Vector<User> users=new Vector<>();

    public static User createUser(String username, DatagramSocket ds, int chatPort_out){
        User user=new User(username,ds,chatPort_out);
        users.add(user);
        return user;
    }

}
