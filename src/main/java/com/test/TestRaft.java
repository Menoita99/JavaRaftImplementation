package com.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.ArrayList;
import java.util.Properties;

import com.raft.Server;
import com.raft.state.Mode;

public class TestRaft {

	public static void main(String[] args) throws IOException, AlreadyBoundException, InterruptedException {
		TestRaft t = new TestRaft();
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));
		ArrayList<Server> servers = new ArrayList<>();

		//servers
		for (int i = 0; i < 3; i++) {
			p.setProperty("port", "100"+i);
			Server s = new Server(p);
			System.out.println("Server "+i+" up "+s.getSelfId());
			servers.add(s);
		}

		Thread.sleep(3000);

		Server lider = null;

		for (Server server : servers) 
			if(server.getMode()==Mode.LEADER)
				lider = server;

		//clients
		for (int i = 0; i < 3; i++) 
			t.new ClientTest(lider, i).start();
	}


	
	
	
	
	private class ClientTest extends Thread{
		private Server lider;
		private int i;

		public ClientTest(Server lider, int i) {
			this.lider = lider;
			this.i = i;
		}

		@Override
		public void run() {
			try {
				lider.execute((char)(97+i)+" = 0", "127.0.0."+i+":"+System.currentTimeMillis());
				for (int j = 0; j < 10; j++) 
					lider.execute((char)(97+i)+"+=1", "127.0.0."+i+":"+System.currentTimeMillis());
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}