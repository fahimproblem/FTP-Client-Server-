import java.io.*;  
import java.net.*;  
import java.util.Scanner;

public class ftpclient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to FTP Server.");

            while (true) {
                System.out.println("Enter command (upload/download/list/exit):");
                String command = scanner.nextLine();
                out.writeUTF(command);

                switch (command.toLowerCase()) {
                    case "upload":
                        uploadFile(out, scanner);
                        break;
                    case "download":
                        downloadFile(in, out, scanner);
                        break;
                    case "list":
                        listFiles(in);
                        break;
                    case "exit":
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid command.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void uploadFile(DataOutputStream out, Scanner scanner) throws IOException {
        System.out.println("Enter the file path to upload:");
        String filePath = scanner.nextLine();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("File not found.");
            return;
        }

        out.writeUTF(file.getName());
        out.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("File uploaded: " + file.getName());
    }

    private static void downloadFile(DataInputStream in, DataOutputStream out, Scanner scanner) throws IOException {
        System.out.println("Enter the file name to download:");
        String fileName = scanner.nextLine();
        out.writeUTF(fileName);

        String response = in.readUTF();
        if (response.equals("File not found")) {
            System.out.println("File not found on server.");
            return;
        }

        long fileSize = in.readLong();
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
        System.out.println("File downloaded: " + fileName);
    }

    private static void listFiles(DataInputStream in) throws IOException {
        int fileCount = in.readInt();
        System.out.println("Files on server:");
        for (int i = 0; i < fileCount; i++) {
            System.out.println(in.readUTF());
        }
    }
}
