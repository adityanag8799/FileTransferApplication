import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.net.*;

/**
 * The FileUploadEvent class represents an event related to file uploads. It
 * includes information about the uploader, file, and bytes uploaded.
 */
class FileUploadEvent {
    private String uploaderId;
    private File file;
    private long numberOfBytesUploaded;

    public FileUploadEvent() {
        this.uploaderId = null;
        this.file = null;
        this.numberOfBytesUploaded = 0;
    }

    /**
     * Sets the uploader's ID for the event.
     *
     * @param uploaderId The unique uploader ID.
     */
    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    /**
     * Gets the uploader's ID for the event.
     *
     * @return The uploader's ID.
     */
    public String getUploaderId() {
        return this.uploaderId;
    }

    /**
     * Sets the file associated with the event.
     *
     * @param file The uploaded file.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Gets the file associated with the event.
     *
     * @return The uploaded file.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Sets the number of bytes uploaded for the event.
     *
     * @param numberOfBytesUploaded The number of bytes uploaded.
     */
    public void setNumberOfBytesUploaded(long numberOfBytesUploaded) {
        this.numberOfBytesUploaded = numberOfBytesUploaded;
    }

    /**
     * Gets the number of bytes uploaded for the event.
     *
     * @return The number of bytes uploaded.
     */
    public long getNumberOfBytesUploaded() {
        return this.numberOfBytesUploaded;
    }
}

/**
 * The FileUploadListener interface defines methods to handle file upload
 * status changes.
 */
interface FileUploadListener {
    /**
     * Handles a file upload status change event.
     *
     * @param fileUploadEvent The file upload event.
     */
    public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}

/**
 * The FileModel class extends AbstractTableModel and represents the model for
 * displaying selected files in a table.
 */
class FileModel extends AbstractTableModel {
    private ArrayList<File> files;

    FileModel() {
        this.files = new ArrayList<>();
    }

    public ArrayList<File> getFiles() {
        return this.files;
    }

    public int getRowCount() {
        return this.files.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public String getColumnName(int c) {
        if (c == 0)
            return "S.No";
        else
            return "File";
    }

    public Class getColumnClass(int c) {
        if (c == 0)
            return Integer.class;
        else
            return String.class;
    }

    public boolean isCellEditable() {
        return false;
    }

    public Object getValueAt(int r, int c) {
        if (c == 0)
            return (r + 1);
        return this.files.get(r).getAbsolutePath();
    }

    public void add(File file) {
        this.files.add(file);
        fireTableDataChanged();
    }
}

/**
 * The FTClientFrame class serves as the client-side framework, comprising two
 * distinct sections: file selection and file uploading, facilitating a
 * streamlined user experience for managing and sending files.
 */
class FTClientFrame extends JFrame {
    private String host;
    private int portNumber;
    private FileSelectionPanel fileSelectionPanel;
    private FileUploadViewPanel fileUploadViewPanel;
    private Container container;

    FTClientFrame(String host, int portNumber) {
        this.portNumber = portNumber;
        this.host = host;
        fileSelectionPanel = new FileSelectionPanel();
        fileUploadViewPanel = new FileUploadViewPanel();
        container = getContentPane();
        container.setLayout(new GridLayout(1, 2));
        container.add(fileSelectionPanel);
        container.add(fileUploadViewPanel);
        setSize(900, 600);
        setLocation(10, 20);
        setVisible(true);
    }

    /**
     * The FileSelectionPanel class represents the panel for selecting files.
     */
    class FileSelectionPanel extends JPanel implements ActionListener {
        private JLabel titleLabel;
        private FileModel model;
        private JTable table;
        private JScrollPane jsp;
        private JButton addFileButton;

        FileSelectionPanel() {
            setLayout(new BorderLayout());
            titleLabel = new JLabel("Selected Files");
            model = new FileModel();
            table = new JTable(model);
            jsp = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            addFileButton = new JButton("Add File");
            addFileButton.addActionListener(this);
            add(titleLabel, BorderLayout.NORTH);
            add(jsp, BorderLayout.CENTER);
            add(addFileButton, BorderLayout.SOUTH);
        }

        public ArrayList<File> getFiles() {
            return model.getFiles();
        }

        public void actionPerformed(ActionEvent ev) {
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File("."));
            int selectedOption = jfc.showOpenDialog(this);
            if (selectedOption == jfc.APPROVE_OPTION) {
                File selectedFile = jfc.getSelectedFile();
                model.add(selectedFile);
            }
        }
    }

    /**
     * The FileUploadViewPanel class represents the panel for viewing and
     * controlling file uploads.
     */
    class FileUploadViewPanel extends JPanel implements ActionListener, FileUploadListener {
        private JButton uploadFilesButton;
        private JPanel progressPanelsContainer;
        private JScrollPane jsp;
        private ArrayList<ProgressPanel> progressPanels;
        ArrayList<File> files;
        ArrayList<FileUploadThread> fileUploaders;

        FileUploadViewPanel() {
            uploadFilesButton = new JButton("Upload File");
            setLayout(new BorderLayout());
            add(uploadFilesButton, BorderLayout.NORTH);
            uploadFilesButton.addActionListener(this);
        }

        public void actionPerformed(ActionEvent ev) {
            files = fileSelectionPanel.getFiles();
            if (files.size() == 0) {
                JOptionPane.showMessageDialog(FTClientFrame.this, "No Files selected for upload");
                return;
            }
            progressPanelsContainer = new JPanel();
            progressPanelsContainer.setLayout(new GridLayout(files.size(), 1));
            ProgressPanel progressPanel;
            progressPanels = new ArrayList<>();
            fileUploaders = new ArrayList<>();
            FileUploadThread fut;
            String uploadersId;
            for (File file : files) {
                uploadersId = UUID.randomUUID().toString();
                progressPanel = new ProgressPanel(uploadersId, file);
                progressPanels.add(progressPanel);
                progressPanelsContainer.add(progressPanel);
                fut = new FileUploadThread(this, uploadersId, file, host, portNumber);
                fileUploaders.add(fut);
            }
            jsp = new JScrollPane(progressPanelsContainer, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            add(jsp, BorderLayout.CENTER);
            this.revalidate();
            this.repaint();
            for (FileUploadThread fileUploadThread : fileUploaders) {
                fileUploadThread.start();
            }
        }

        public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent) {
            String uploaderId = fileUploadEvent.getUploaderId();
            long numberOfBytesUploaded = fileUploadEvent.getNumberOfBytesUploaded();
            File file = fileUploadEvent.getFile();
            for (ProgressPanel progressPanel : progressPanels) {
                if (progressPanel.getId().equals(uploaderId)) {
                    progressPanel.updateProgressBar(numberOfBytesUploaded);
                    break;
                }
            }
        }

        /**
         * The ProgressPanel class represents a panel displaying the progress of a file
         * upload.
         */
        class ProgressPanel extends JPanel {
            private File file;
            private JLabel fileNameLabel;
            private JProgressBar progressBar;
            private long fileLength;
            private String id;

            public ProgressPanel(String id, File file) {
                this.id = id;
                this.file = file;
                this.fileLength = file.length();
                fileNameLabel = new JLabel("Uploading : " + file.getAbsolutePath());
                progressBar = new JProgressBar(1, 100);
                setLayout(new GridLayout(2, 1));
                add(fileNameLabel);
                add(progressBar);
            }

            public String getId() {
                return id;
            }

            public void updateProgressBar(long bytesUploaded) {
                int percentage;
                if (bytesUploaded == fileLength)
                    percentage = 100;
                else
                    percentage = (int) ((bytesUploaded * 100) / fileLength);
                progressBar.setValue(percentage);
                if (percentage == 100) {
                    fileNameLabel.setText("Uploaded : " + file.getAbsolutePath());
                }
            }
        } // ProgressPanel ends
    } // FileUploadViewPanel ends

    public static void main(String gg[]) {
        FTClientFrame fcf = new FTClientFrame("localhost", 5500);
    }
}

/**
 * The FileUploadThread class represents a thread for uploading a file to the
 * server.
 */
class FileUploadThread extends Thread {
    private String id;
    private FileUploadListener fileUploadListener;
    private File file;
    private String host;
    private int portNumber;

    FileUploadThread(FileUploadListener fileUploadListener, String id, File file, String host, int portNumber) {
        this.fileUploadListener = fileUploadListener;
        this.id = id;
        this.host = host;
        this.file = file;
        this.portNumber = portNumber;
    }

    public void run() {
        try {
            long lengthOfFile = file.length();
            String name = file.getName();
            byte header[] = new byte[1024];
            int i;
            long x, k;
            i = 0;
            k = lengthOfFile;
            while (k > 0) {
                header[i] = (byte) (k % 10);
                k = k / 10;
                i++;
            }
            header[i] = (byte) ',';
            i++;
            x = name.length();
            int r = 0;
            while (r < x) {
                header[i] = (byte) name.charAt(r);
                i++;
                r++;
            }
            while (i <= 1023) {
                header[i] = (byte) 32;
                i++;
            }
            Socket socket = new Socket(host, portNumber);
            OutputStream os = socket.getOutputStream();
            os.write(header, 0, 1024);
            os.flush();
            InputStream is = socket.getInputStream();
            byte ack[] = new byte[1];
            int bytesReadCount;
            while (true) {
                bytesReadCount = is.read(ack);
                if (bytesReadCount == -1)
                    continue;
                break;
            }
            FileInputStream fis = new FileInputStream(file);
            int chunkSize = 4096;
            byte bytes[] = new byte[chunkSize];
            int j = 0;
            while (j < lengthOfFile) {
                bytesReadCount = fis.read(bytes);
                os.write(bytes, 0, bytesReadCount);
                os.flush();
                j = j + bytesReadCount;
                long brc = j;
                SwingUtilities.invokeLater(() -> {
                    FileUploadEvent fue = new FileUploadEvent();
                    fue.setUploaderId(id);
                    fue.setFile(file);
                    fue.setNumberOfBytesUploaded(brc);
                    fileUploadListener.fileUploadStatusChanged(fue);
                });
            }
            fis.close();
            while (true) {
                bytesReadCount = is.read(ack);
                if (bytesReadCount == -1)
                    continue;
                break;
            }
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
