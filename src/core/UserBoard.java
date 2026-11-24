package core;

import java.util.ArrayList;

public class UserBoard {
    private static final UserBoard INSTANCE = new UserBoard();

    private final ArrayList<User> users;

    private UserBoard() {
        this.users = new ArrayList<User>();
    }

    public static UserBoard getInstance() {
        return INSTANCE;
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
