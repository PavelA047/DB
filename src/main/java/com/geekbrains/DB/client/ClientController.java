package com.geekbrains.DB.client;

import com.geekbrains.DB.utils.SenderUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    public ListView<String> clientView;
    public TextField textField;
    public ListView<String> serverView;
    public Label clientLabel;
    public Label serverLabel;
    private DataInputStream is;
    private DataOutputStream os;
    private File currentDir;
    private byte[] buf;


    private void read() {
        try {
            while (true) {
                String command = is.readUTF();
                if (command.equals("#LIST")) {
                    Platform.runLater(() -> serverView.getItems().clear());
                    int count = is.readInt();
                    for (int i = 0; i < count; i++) {
                        String fileName = is.readUTF();
                        Platform.runLater(() -> serverView.getItems().add(fileName));
                    }
                }
                if (command.equals("#SEND#FILE#")) {
                    SenderUtils.getFileFromInputStream(is, currentDir);
                    Platform.runLater(this::fillCurrentDirFiles);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillCurrentDirFiles() {
        clientView.getItems().clear();
        clientView.getItems().add("..");
        clientView.getItems().addAll(currentDir.list());
        clientLabel.setText(getClientFilesDetails());
    }

    private String getClientFilesDetails() {
        File[] files = currentDir.listFiles();
        long size = 0;
        String label;
        if (files != null) {
            label = files.length + " files in current dir.\n";
            for (File file : files) {
                size += files.length;
            }
            label += "Summary size: " + size + " bytes.";
        } else {
            label = "Current dir. is empty";
        }
        return label;
    }

    private void initClickListener() {
        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = clientView.getSelectionModel().getSelectedItem();
                System.out.println("chosen " + fileName);
                Path path = currentDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    currentDir = path.toFile();
                    fillCurrentDirFiles();
                }
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

    public void upload(ActionEvent actionEvent) throws IOException {
        String filename = clientView.getSelectionModel().getSelectedItem();
        File currentFile = currentDir.toPath().resolve(filename).toFile();
        SenderUtils.loadFileToOutputStream(os, currentFile);
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        os.writeUTF("#GET#FILE#");
        os.writeUTF(fileName);
        os.flush();
    }
}
