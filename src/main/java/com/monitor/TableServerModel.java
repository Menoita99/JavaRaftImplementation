package com.monitor;

import java.util.List;

import com.raft.models.Address;
import com.raft.state.Mode;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableServerModel {

	private SimpleObjectProperty<Address> address = new SimpleObjectProperty<>();
	private SimpleObjectProperty<Mode> mode = new SimpleObjectProperty<>();
	private SimpleObjectProperty<Long> term = new SimpleObjectProperty<>();
	private SimpleObjectProperty<Long> index = new SimpleObjectProperty<>();
	private SimpleObjectProperty<Boolean> active = new SimpleObjectProperty<>();
	
	
	
	public TableServerModel(Address address, Mode mode,long l, long m, boolean active) {
		this.address.set(address);
		this.mode.set(mode);;
		this.term.set(l);
		this.index.set(m);
		this.active.set(active);
	}



	public void setAddress(Address address) {
		this.address.set(address);
	}



	public void setMode(Mode mode) {
		this.mode.set(mode);
	}



	public void setTerm(Long term) {
		this.term.set(term);
	}



	public void setIndex(Long index) {
		this.index.set(index);
	}



	public void setActive(Boolean active) {
		this.active.set(active);
	}



	public Address getAddress() {
		return address.get();
	}



	public Mode getMode() {
		return mode.get();
	}



	public Long getTerm() {
		return term.get();
	}



	public Long getIndex() {
		return index.get();
	}



	public Boolean getActive() {
		return active.get();
	}



	public SimpleObjectProperty<Address> addressProperty() {
		return address;
	}



	public SimpleObjectProperty<Mode> modeProperty() {
		return mode;
	}



	public SimpleObjectProperty<Long> termProperty() {
		return term;
	}



	public SimpleObjectProperty<Long> indexProperty() {
		return index;
	}



	public SimpleObjectProperty<Boolean> activeProperty() {
		return active;
	}



	public static List<TableColumn<TableServerModel, ?>> getColumns() {		
		TableColumn<TableServerModel, Object> addressCol = new TableColumn<>("Address");
		addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
		TableColumn<TableServerModel, Object> modeCol = new TableColumn<>("Mode");
		modeCol.setCellValueFactory(new PropertyValueFactory<>("mode"));
		TableColumn<TableServerModel, Object> termCol = new TableColumn<>("Term");
		termCol.setCellValueFactory(new PropertyValueFactory<>("term"));
		TableColumn<TableServerModel, Object> indexCol = new TableColumn<>("Index");
		indexCol.setCellValueFactory(new PropertyValueFactory<>("index"));
		TableColumn<TableServerModel, Object> activeCol = new TableColumn<>("Active");
		activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
		return List.of(addressCol,modeCol,termCol,indexCol,activeCol);
	}
}
