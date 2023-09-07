import java.net.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

// RequestProcessor Class: Handles incoming client requests
class RequestProcessor extends Thread {
    private Socket socket;
    private String id;
    private FTServerFrame fsf;

    RequestProcessor(Socket socket, String id, FTServerFrame fsf) {
        this.id = id;
        this.fsf = fsf;
        this.socket = socket;
        start();
    }

    public void run() {
        try {
            // Notify server GUI about the client connection
            SwingUtilities.invokeLater(() -> fsf.updateLog("Client connected with id: " + id));

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            int bytesToReceive = 1024;
            byte header[] = new byte[bytesToReceive];
            int bytesReadCount = readHeaderData(is, header);

            int lengthOfFile = getLengthOfFile(header);
            String fileName = getFileName(header);

            // Notify server GUI about the incoming file
            SwingUtilities.invokeLater(() -> fsf.updateLog("Receiving File: " + fileName + " Length: " + lengthOfFile));
            File file = saveFile(fileName);
            sendAck(os);
            receiveAndSaveFileData(is, file, lengthOfFile);
            sendAck(os);
            socket.close();

            // Notify server GUI about file saved and client connection closure
            SwingUtilities.invokeLater(() -> {
                fsf.updateLog("File Saved to " + file.getAbsolutePath());
                fsf.updateLog("Connection with client " + id + " closed.");
            });
        } catch (Exception e) {
            // Handle exceptions by printing to console
            System.out.println(e);
        }
    }

    // Read header data from the client
    private int readHeaderData(InputStream is, byte[] header) throws IOException {
        int bytesReadCount;
        int i = 0;
        int j = 1;
        while (j < header.length) {
            bytesReadCount = is.read(header, i, header.length - i);
            if (bytesReadCount == -1) continue;
            i += bytesReadCount;
            j += bytesReadCount;
        }

        return i;
    }

    // Extract the length of the file from the header
    private int getLengthOfFile(byte[] header) {
        int lengthOfFile = 0;
        int i = 0;
        int j = 1;

        while (header[i] != ',') {
            lengthOfFile = lengthOfFile + (header[i] * j);
            j = j * 10;
            i++;
        }

        return lengthOfFile;
    }

    // Extract the file name from the header
    private String getFileName(byte[] header) {
        int i = header.length - 1;

        while (i >= 0 && header[i] == 0) {
            i--;
        }
        StringBuffer sb = new StringBuffer();
        for (int j = i - 1; j >= 0; j--) {
            sb.append((char) header[j]);
        }

        return sb.reverse().toString().trim();
    }

    // Save the received file
    private File saveFile(String fileName) {
        File file = new File("uploads" + File.separator + fileName);
        if (file.exists()) file.delete();

        return file;
    }

    // Send acknowledgment to the client
    private void sendAck(OutputStream os) throws IOException {
        byte ack[] = {1};
        os.write(ack);
        os.flush();
    }

    // Receive and save file data
    private void receiveAndSaveFileData(InputStream is, File file, int lengthOfFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        int chunkSize = 4096;
        byte bytes[] = new byte[chunkSize];
        long m = 0;

        while (m < lengthOfFile) {
            int bytesReadCount = is.read(bytes);
            if (bytesReadCount == -1) continue;
            fos.write(bytes, 0, bytesReadCount);
            fos.flush();
            m += bytesReadCount;
        }

        fos.close();
    }
}

// Rest of the code remains unchanged...



// FTServerF Class (Server Thread): Manages server setup and client connections
class FTServerF extends Thread {
    private ServerSocket serverSocket;
    private FTServerFrame fsf;

    // Constructor
    FTServerF(FTServerFrame fsf) {
        this.fsf = fsf;
    }

    // Main thread execution
    public void run() {
        try {
            serverSocket = new ServerSocket(5500);
            startListening();
        } catch (Exception e) {
            // Handle exceptions by printing to console
            System.out.println(e);
        }
    }

    // Shutdown the server
    public void shutdown() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            // Handle exceptions by printing to console
            System.out.println(e);
        }
    }

    // Start listening for client connections
    public void startListening() {
        try {
            RequestProcessor requestProcessor;
            Socket socket;
            while (true) {
                System.out.println("Server Started!");

                // Update the server log when the server is started and listening
                SwingUtilities.invokeLater(new Thread() {
                    public void run() {
                        fsf.updateLog("Server is started and listening to port no 5500");
                    }
                });

                socket = serverSocket.accept();
                requestProcessor = new RequestProcessor(socket, UUID.randomUUID().toString(), fsf);
            }
        } catch (Exception e) {
            // Handle exceptions by printing to console
            System.out.println("Server Stopped Listening.");
            System.out.println(e);
        }
    }
}

// FTServerFrame Class: Represents the server's graphical user interface
class FTServerFrame extends JFrame implements ActionListener {
    private FTServerF server;
    private Container container;
    private JButton button;
    private JTextArea jta;
    private JScrollPane jsp;
    private boolean serverState = false;

    // Constructor
    FTServerFrame() {
        container = getContentPane();
        container.setLayout(new BorderLayout());
        button = new JButton("start");
        jta = new JTextArea();
        jsp = new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        container.add(jsp, BorderLayout.CENTER);
        container.add(button, BorderLayout.SOUTH);
        server = new FTServerF(this);
        setLocation(100, 100);
        setSize(500, 500);
        setVisible(true);
        button.addActionListener(this);
    }

    // Update the server log
    public void updateLog(String message) {
        jta.append(message + "\n");
    }

    // ActionListener for the "Start" and "Stop" button
    public void actionPerformed(ActionEvent ev) {
        if (serverState == false) {
            server = new FTServerF(this);
            server.start();
            serverState = true;
            button.setText("Stop");
        } else {
            server.shutdown();
            serverState = false;
            button.setText("Start");
            jta.append("Server Stopped!\n");
        }
    }

    // Main method
    public static void main(String gg[]) {
        FTServerFrame ftserverFrame = new FTServerFrame();
    }
}
