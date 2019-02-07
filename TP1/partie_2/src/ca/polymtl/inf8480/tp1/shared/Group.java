package ca.polymtl.inf8480.tp1.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private List<String> members = new ArrayList<>();
    private String name;

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addMember(String m) {
        if (m.trim().equals("")) return;
        this.members.add(m);
    }

    public static Group fromString(String definition) {
        String[] split = definition.split(":");
        Group g = new Group(split[0]);

        for (String user : split[1].split(",")) {
            g.addMember(user);
        }
        return g;
    }

    public List<String> getMembers() {
        return this.members;
    }

    @Override
    public String toString() {
        // <nom_du_groupe>:user1,user2,user3,...

        StringBuilder sb = new StringBuilder();
        sb.append(this.name).append(":");

        for (String m :
                this.members) {
            sb.append(m).append(",");
        }

        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }
}
