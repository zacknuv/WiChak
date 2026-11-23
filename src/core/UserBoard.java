import java.util.ArrayList;

public class UserBoard {
    private ArrayList<User> users;

    public UserBoard() {
        this.users = new ArrayList<User>();
    }

    public void addUser(User user) {
        if (user == null) {
            return;
        }
        users.add(user);
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public User[] toArray() {
        return users.toArray(new User[0]);
    }

    public int getUserCount() {
        return users.size();
    }
}
