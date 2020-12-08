package com.raft.launcher;

import com.client.Client;

public class ClientLauncher {

	public static void main(String[] args) {
		Client c = new Client();
		c.startInfiniteRequests();
	}
}
