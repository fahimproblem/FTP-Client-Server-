import java.io.*;  
import java.net.*;  
import java.util.concurrent.*;

public class ftpserver {
    private static final int PORT = 1234;
    private static final int MAX_CLIENTS = 2;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("FTP Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected to client: " + clientSocket.getInetAddress());
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String command;
            while (true) {
                command = in.readUTF();
                if (command.equalsIgnoreCase("upload")) {
                    receiveFile(in);
                } else if (command.equalsIgnoreCase("download")) {
                    sendFile(in, out);
                } else if (command.equalsIgnoreCase("list")) {
                    listFiles(out);
                } else if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Client disconnected: " + socket.getInetAddress());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(DataInputStream in) throws IOException {
        String fileName = in.readUTF();
        long fileSize = in.readLong();
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
        System.out.println("File uploaded: " + fileName);
    }

    private void sendFile(DataInputStream in, DataOutputStream out) throws IOException {
        String fileName = in.readUTF();
        File file = new File(fileName);

        if (!file.exists()) {
            out.writeUTF("File not found");
            return;
        }

        out.writeUTF("File found");
        out.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("File sent: " + fileName);
    }

    private void listFiles(DataOutputStream out) throws IOException {
        File directory = new File(".");
        File[] files = directory.listFiles();
        out.writeInt(files.length);
        for (File file : files) {
            out.writeUTF(file.getName());
        }
    }
}
