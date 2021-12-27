package com.geekbrains.DB.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    public ListView<String> listView;
    public TextField textField;
    public ListView<String> listViewServerFiles;
    public TextField textFieldServerFiles;
    private DataInputStream is;
    private DataOutputStream os;
    private File currentDir;
    private byte[] buf;
    private static final int SIZE = 256;

    @FXML
    public void sendMessage(ActionEvent actionEvent) throws IOException {
        String filename = textField.getText();
        File currentFile = currentDir.toPath().resolve(filename).toFile();
        os.writeUTF("#SEND#FILE#");
        os.writeUTF(filename);
        os.writeLong(currentFile.length());
        try (FileInputStream is = new FileInputStream(currentFile)) {
            while (true) {
                int read = is.read(buf);
                if (read == -1) {
                    break;
                }
                os.write(buf, 0, read);
            }
        }
        os.flush();
        textField.clear();
    }

    @FXML
    public void sendFile() {
        String fileName = textFieldServerFiles.getText();
        try {
            os.writeUTF("#DOWNLOAD#FILE");
            os.writeUTF(fileName);
            os.flush();
            textFieldServerFiles.clear();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void read() {
        try {
            while (true) {
                String message = is.readUTF();
                Platform.runLater(() -> {
                    if (message.equals("#SEND#FILE#")) {
                        try {
                            String fileName = is.readUTF();
                            long size = is.readLong();
                            Path currentPath = currentDir.toPath().resolve(fileName);
                            try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {
                                for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {
                                    int read = is.read(buf);
                                    fos.write(buf, 0, read);
                                }
                            }
                            textFieldServerFiles.setText("Uploaded: " + fileName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (message.equals("File successfully uploaded")) {
                        textField.setText("File successfully uploaded");
                    } else if (listViewServerFiles.getItems().contains("Server directory is empty")) {
                        listViewServerFiles.getItems().clear();
                        listViewServerFiles.getItems().add(message);
                    } else {
                        listViewServerFiles.getItems().add(message);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillCurrentDirFiles() {
        listView.getItems().clear();
        listView.getItems().add("..");
        listView.getItems().addAll(currentDir.list());
    }

    private void initClickListener() {
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = listView.getSelectionModel().getSelectedItem();
                System.out.println("chosen to send " + fileName);
                Path path = currentDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    currentDir = path.toFile();
                    fillCurrentDirFiles();
                    textField.clear();
                } else {
                    textField.setText(fileName);
                }
            }
        });
    }

    private void fileDownloadClickListener() {
        listViewServerFiles.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = listViewServerFiles.getSelectionModel().getSelectedItem();
                System.out.println("chosen to download " + fileName);
                textFieldServerFiles.setText(fileName);
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            buf = new byte[256];
            currentDir = new File(System.getProperty("user.home")); /*домашний каталог*/
            fillCurrentDirFiles();
            initClickListener();
            fileDownloadClickListener();
            Socket socket = new Socket("localHost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
