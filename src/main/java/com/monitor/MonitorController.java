package com.monitor;

import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;

import com.raft.models.Address;
import com.raft.state.Mode;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MonitorController extends Application implements Initializable {

	@FXML private Text leaderLabel;
	@FXML private Text speedLabel;
	@FXML private LineChart<String, Integer> speedChart;
	@FXML private TableView<TableHistoricModel> tableRight;
	@FXML private TableView<TableServerModel> tableBottom;

	private static final int MAX_ELEMS = 40;


	private XYChart.Series<String, Integer> data = new XYChart.Series<String, Integer>();
	private static MonitorController intance;


	public static MonitorController getInstance() {
		return intance;
	}


	@Override
	public void start(Stage window) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("monitor.fxml"));
		window.setScene(new Scene(loader.load()));
		window.show();
	}



	@Override
	public void initialize(URL location, ResourceBundle resources) {
		data.setName("Throughput");
		speedChart.setAnimated(false);
		speedChart.setCreateSymbols(false);
		speedChart.getData().add(data);
		tableBottom.getColumns().addAll(TableServerModel.getColumns());
		tableBottom.setItems(FXCollections.observableArrayList());
		tableRight.getColumns().addAll(TableHistoricModel.getColumns());
		tableRight.setItems(FXCollections.observableArrayList());
		intance = this;
		MonitorServer.getInstance();
	}




	public void updateChart(int evals) {
		Platform.runLater(()->{
			LocalTime now = LocalTime.now();
			speedLabel.setText("Speed: "+evals+" op/s");
			Data<String, Integer> entry = new XYChart.Data<String, Integer>(now.getMinute()+":"+(now.getSecond()<10?"0"+now.getSecond():now.getSecond()), evals);
			ObservableList<Data<String, Integer>> list = data.getData();
			if(list.size()>MAX_ELEMS)
				list.remove(0);
			list.add(entry);
		});
	}






	public void updateTableBottomStatus(MonitorRequest request,Address[] clusterArray) {
		Platform.runLater(()->{

			ObservableList<TableServerModel> items = tableBottom.getItems();
			Address serverIp = request.getSender();

			TableServerModel model = items.stream().filter(m -> m.getAddress().equals(serverIp)).findFirst().orElse(null);
			if(model == null) {
				model = new TableServerModel(serverIp, request.getMode(), request.getCurrentTerm(), request.getLastAplied().getIndex(), true);
				items.add(model);
			}else {
				model.setMode(request.getMode());
				model.setIndex(request.getLastAplied().getIndex());
				model.setTerm(request.getCurrentTerm());
				model.setActive(true);
			}

			if(request.getMode().equals(Mode.LEADER)) {
				for (int i = 0; i < clusterArray.length; i++) {
					Address server = clusterArray[i];
					if(!server .equals(serverIp)) {
						model = items.stream().filter(m -> m.getAddress().equals(server)).findFirst().orElse(null);
						if(model != null) 
							model.setActive(request.getActiveServers()[i]);
					}
				}
			}
		});
	}




	public void addHistoricEntry(MonitorRequest request) {
		Platform.runLater(()->{
			ObservableList<TableHistoricModel> items = tableRight.getItems();
			if(items.size() > MAX_ELEMS)
				items.remove(0);
			items.add(new TableHistoricModel(request.getMode(), request.getSender()));
		});
	}
	
	
	public void setLeaderLabelText(String string) {
		Platform.runLater(()->leaderLabel.setText(string));
	}


	public static void main(String[] args) {
		launch(args);
	}
}
