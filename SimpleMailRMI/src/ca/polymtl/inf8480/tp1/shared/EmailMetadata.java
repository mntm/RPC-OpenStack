package ca.polymtl.inf8480.tp1.shared;

import java.io.Serializable;

public class EmailMetadata implements Serializable {
    private String from;
    private String subject;
    private String date;
    private String contentPath = "";
    private boolean read = false;

    public EmailMetadata(String from, String subject, String date) {
        this.from = from;
        this.subject = subject;
        this.date = date;
    }

    public static EmailMetadata fromString(String definition) {
        // parse the definition string

        String[] fields = definition.split(",");
        EmailMetadata ret = new EmailMetadata(
                fields[0].split("=")[1],
                fields[1].split("=")[1],
                fields[2].split("=")[1]
        );
        ret.setContentPath(fields[3].split("=")[1]);
        ret.setRead(fields[4].split("=")[1].equals("-"));

        return ret;
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

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    @Override
    public String toString() {
        return "" +
                "from=" + from +
                ", subject=" + subject +
                ", date=" + date +
                ", contentPath=" + contentPath +
                ", read=" + ((read) ? "-" : "N")
                ;
    }
}
