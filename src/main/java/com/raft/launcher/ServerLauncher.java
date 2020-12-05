package com.raft.launcher;

import com.raft.Server;

public class ServerLauncher {
	
	public static void main(String[] args) throws Exception{
		String root = "";
		boolean monitorMode = false;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-path": {
					root = args[i+1];
				}
				case "-monitor":{
					monitorMode = true;
				}
			}
		}
		if(root.isBlank())
			throw new IllegalArgumentException("Server root path could not be found");
		else {
			System.out.println("Starting server "+root+" "+monitorMode);
			new Server(root,monitorMode);
		}
	}
}
