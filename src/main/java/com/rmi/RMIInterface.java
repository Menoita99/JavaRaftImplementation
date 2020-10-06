package com.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.raft.models.ServerResponse;

public interface RMIInterface extends Remote {

    public ServerResponse request(String string) throws RemoteException;
    
    
}
