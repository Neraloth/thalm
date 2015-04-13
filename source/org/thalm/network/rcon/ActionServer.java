package org.thalm.network.rcon;



import java.io.IOException;
import java.net.ServerSocket;

import org.thalm.common.Main;

public class ActionServer implements Runnable {

	private ServerSocket serverSocket;
	private Thread thread;

	public ActionServer() {
		try {
			serverSocket = new ServerSocket(Main.CONFIG_ACTION_PORT);
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		} catch (IOException e) {
			addToLog("IOException: "+e.getMessage());
			e.printStackTrace();
			Main.closeServers();
		}
	}

	public void run() {	
		while(Main.isRunning) {
			try {
				new ActionThread(serverSocket.accept());
			}catch(IOException e) {
				try {
					addToLog("Fermeture du serveur d'action");	
					if(!serverSocket.isClosed())serverSocket.close();
				}
				catch(IOException e1){}
			}
		}
	}
	
	public void kickAll() {
		try {
			serverSocket.close();
		} catch (IOException e) {}
	}
	
	public synchronized static void addToLog(String str) {
		Main.addToShopLog(str);
	}
	
	public synchronized static void addToSockLog(String str)
	{
		if(Main.CONFIG_DEBUG)
		{
			System.out.println (str);
		}
		
	}

	public Thread getThread() {
		return thread;
	}
}
