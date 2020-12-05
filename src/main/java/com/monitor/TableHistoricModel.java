package com.monitor;

import java.time.LocalDateTime;
import java.util.List;

import com.raft.models.Address;
import com.raft.state.Mode;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableHistoricModel {

	private SimpleObjectProperty<LocalDateTime> time = new SimpleObjectProperty<>();
	private SimpleObjectProperty<Mode> mode = new SimpleObjectProperty<>();
	private SimpleObjectProperty<Address> server = new SimpleObjectProperty<>();
	

	/**
	 * @param time
	 * @param mode
	 * @param server
	 */
	public TableHistoricModel(Mode mode, Address server) {
		this.time.set(LocalDateTime.now());
		this.mode.set(mode);
		this.server.set(server);
	}


	public LocalDateTime getTime() {
		return time.get();
	}


	public Mode getMode() {
		return mode.get();
	}


	public Address getServer() {
		return server.get();
	}


	public void setTime(LocalDateTime time) {
		this.time.set(time);
	}


	public void setMode(Mode mode) {
		this.mode.set(mode);
	}


	public void setServer(Address server) {
		this.server.set(server);
	}

	public SimpleObjectProperty<LocalDateTime> timeProperty() {
		return time;
	}

	public SimpleObjectProperty<Mode> modeProperty() {
		return mode;
	}

	public SimpleObjectProperty<Address> serverProperty() {
		return server;
	}
	
	public static List<TableColumn<TableHistoricModel, ?>> getColumns() {		
		TableColumn<TableHistoricModel, Object> timeCol = new TableColumn<>("Time");
		timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
		TableColumn<TableHistoricModel, Object> modeCol = new TableColumn<>("Mode");
		modeCol.setCellValueFactory(new PropertyValueFactory<>("mode"));
		TableColumn<TableHistoricModel, Object> serverCol = new TableColumn<>("Server");
		serverCol.setCellValueFactory(new PropertyValueFactory<>("server"));
		return List.of(timeCol,modeCol,serverCol);
	}

}
