package com.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.Random;

import com.raft.Server;
import com.raft.state.Mode;

public class TestRaft {

	public static void main(String[] args) throws IOException, AlreadyBoundException {
		Server s = new Server();
		s.setMode(Mode.LEADER);
		s.getLeaderState().reset(s);
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));
		
		//more 2 follow servers
		for (int i = 1; i <= 2; i++) {
			p.setProperty("port", "100"+i);
			new Server(p);
		}

		//3 clients
		for (int i = 0; i < 3; i++) {
			new Thread(()-> {
				try {
					int random = new Random().nextInt(99999);
					s.execute("var"+random+" = 0", "127.0.0."+random+":"+System.currentTimeMillis());
					for (int j = 0; j < 10; j++) 
						s.execute("var"+random+"+=1", "127.0.0."+random+":"+System.currentTimeMillis());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}).start();		
		}		
	}
}