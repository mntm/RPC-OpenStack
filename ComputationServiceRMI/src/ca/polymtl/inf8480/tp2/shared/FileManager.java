package ca.polymtl.inf8480.tp2.shared;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Class used to manipulate files.
 */
public class FileManager {
    // Attribute
    private String dirName;

    public FileManager(final String dirName) {
        this.dirName = dirName + File.separator;
        Path dirPath = Paths.get(this.dirName);

        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
        } catch (final Exception e) {
            System.err.println("Could not create directory: " + this.dirName);
        }
    }

    /**
     * Calculate the MD5 checksum of a file.
     *
     * @param fileName the name of the file.
     * @return the string representation of the MD5.
     */
    public String md5Checksum(final String fileName) {
        if (!this.fileExists(fileName)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(Paths.get(this.dirName + fileName));
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[4096];
                while (dis.read(buffer) > -1) {
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString().toLowerCase();
        } catch (final NoSuchFileException e) {
            System.err.println("Could not find file: " + fileName);
            e.printStackTrace();
            return null;
        } catch (final NoSuchAlgorithmException | IOException e) {
            System.err.println("Could not calculated the MD5 or file: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates an empty file.
     *
     * @param fileName the name of the file.
     */
    public boolean createEmptyFile(final String fileName) {
        final Path path = Paths.get(this.dirName + fileName);

        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (FileAlreadyExistsException e) {
            System.err.println("File already exists: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("Error create file : " + fileName);
            return false;
        }
        return true;
    }

    /**
     * Get the content of a file.
     *
     * @param fileName the name of the file.
     * @return the content of the file.
     */
    public String readFile(final String fileName) {
        if (!this.fileExists(fileName)) {
            return "";
        }
        File f = new File(this.dirName + fileName);
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not open file: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Assure que les que les operations se fassent dans le bon repertoire
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public BufferedWriter newBufferedWriter(String filename, OpenOption... options) throws IOException {
        return Files.newBufferedWriter(Paths.get(this.dirName + filename), options);
    }

    public BufferedReader newBufferedReader(String filename) throws IOException {
        return Files.newBufferedReader(Paths.get(this.dirName + filename));
    }

    /**
     * Returns true if a file exists and is readable.
     */
    public boolean isReadable(String filename) {
        return Files.isReadable(Paths.get(this.dirName + filename));
    }


    /**
     * Returns true if a file exists.
     */
    private boolean fileExists(final String fileName) {
        File f = new File(this.dirName + fileName);
        return f.exists();
    }

    public boolean deleteFile(String filename) throws IOException {
        return Files.deleteIfExists(Paths.get(this.dirName + filename));
    }

    public int lineNumber(String filename) throws IOException {
        return (int) Files.lines(Paths.get(this.dirName + filename)).count();
    }

    public void writeSerializeableObject(String filename, Object o, OpenOption... options) throws IOException {
        boolean append = ((options != null) && (Arrays.asList(options).contains(StandardOpenOption.APPEND)));
        boolean exists = this.fileExists(filename);
        try (OutputStream fos = Files.newOutputStream(Paths.get(this.dirName + filename), options);
             ObjectOutputStream os = (exists && append) ?
                     new AppendingObjectOutputStream(fos) : new ObjectOutputStream(fos)) {
            os.writeObject(o);
        }
    }

    public Collection<Object> readSerializeableObjects(String filename) throws IOException {
        Collection<Object> ret = new ArrayList<>();
        try (InputStream is = Files.newInputStream(Paths.get(this.dirName + filename));
             ObjectInputStream ois = new ObjectInputStream(is)) {

            Object o;
            while ((o = ois.readObject()) != null) {
                ret.add(o);
            }

        } catch (EOFException e) {
            //do nothing, just leave the loop
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private class AppendingObjectOutputStream extends ObjectOutputStream {
        /*
         * https://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream/1195078#1195078
         */
        AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            reset();
        }

    }
}