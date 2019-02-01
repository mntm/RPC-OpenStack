package ca.polymtl.inf8480.tp1.shared;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

public class Email implements Serializable {
    private int id;
    private String from;
    private String subject;
    private String date;
    private byte[] content;
    private String contentPath;
    private boolean read = false;

    public Email(int id, String from, String subject, String date) {
        this.id = id;
        this.from = from;
        this.subject = subject;
        this.date = date;
    }

    public static Email fromString(String definition) {
        // parse the definition string

        String[] fields = definition.split(",");
        Email ret = new Email(
                Integer.parseInt(fields[0].split("=")[1]),
                fields[1].split("=")[1],
                fields[2].split("=")[1],
                fields[3].split("=")[1]
        );
        ret.setContentPath(fields[4].split("=")[1]);
        ret.setRead(Boolean.getBoolean(fields[5].split("=")[1]));

        try {
            File f = new File(ret.getContentPath());
            byte[] bytes = Files.readAllBytes(f.toPath());
            ret.setContent(bytes);
        } catch (IOException e) {
            System.err.println("Could not open file: " + ret.getContentPath());
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    public int getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public void saveContent() {

    }

    @Override
    public String toString() {
        return "id=" + id +
                ", from='" + from + '\'' +
                ", subject='" + subject + '\'' +
                ", date='" + date + '\'' +
                ", contentPath='" + contentPath + '\'' +
                ", read=" + read
                ;
    }
}
