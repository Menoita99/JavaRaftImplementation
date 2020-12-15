package com.client;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;

import com.raft.models.Address;
import com.raft.models.ServerResponse;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.Getter;

@Getter
public class ClientController extends Application implements Initializable{

	//FXML attributes
    @FXML private Text ipClient;
    @FXML private Text ipLeader;
    @FXML private Text portLeader;
    @FXML private TextArea textArea;
    @FXML private TextField textField;

    private Client client;
    private static ClientController instance;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		instance = this;
		client = new Client();
		ipClient.setText(Address.getLocalIp());
		ipLeader.setText(client.getLeaderAddress().get().getIpAddress());
		portLeader.setText(client.getLeaderAddress().get().getPort()+"");
		client.getLeaderAddress().addListener((observable, oldValue, newValue)->{
			ipLeader.setText(newValue.getIpAddress());
			portLeader.setText(newValue.getPort()+"");
		});
		textArea.setEditable(false);
	}
 
	
	@Override
	public void start(Stage window) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("client.fxml"));
		window.setScene(new Scene(loader.load()));
		window.show();
	}

	
	
    @FXML 
    public void send(ActionEvent event) {
    	try {
	    	String command = textField.getText();
	    	textArea.appendText("> "+command+"\n");
			ServerResponse reponse = client.request(command);
	    	textField.clear();
	    	if(reponse.getResponse() == null) textArea.appendText("\n");
	    	else textArea.appendText(reponse.getResponse().toString()+"\n");
    	}catch (Exception e) {
    		showErrorDialog(e);
		}
    }
    
    
    
	/**
	 * Display's an error dialog with exception given
	 * @param exception Exception
	 */
	public void showErrorDialog(Exception exception) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(exception.getClass().toString());
		alert.setHeaderText(null);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("Error message : "+exception.getMessage());
		pw.println();
		exception.printStackTrace(pw);
		String exceptionText = sw.toString();

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(textArea, 0, 1);

		alert.getDialogPane().setExpandableContent(expContent);
		alert.showAndWait();
	}
	
	/**
	 * Display's an error dialog with the title and message given
	 * @param title dialog title
	 * @param Message dialog message
	 */
	public void showErrorDialog(String title,String Message) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(Message);
		alert.showAndWait();
	}





	/**
	 * Display's an information dialog with the title and message given
	 * @param title dialog title
	 * @param Message dialog message
	 */
	public void showInfoDialog(String title,String Message) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(Message);
		alert.showAndWait();
	}
	
	
	public static void main(String[] args) {
		launch(args);
	}


	/**
	 * @return the instance
	 */
	public static ClientController getInstance() {
		return instance;
	}
}
