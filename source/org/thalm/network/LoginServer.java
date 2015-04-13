package org.thalm.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Calendar;



import org.thalm.common.Main;

public class LoginServer implements Runnable{

	private ServerSocket _SS;
	private Thread _t;

	public LoginServer()
	{
		try {
			_SS = new ServerSocket(Main.CONFIG_REALM_PORT);
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		} catch (IOException e) {
			addToLog("IOException: "+e.getMessage());
			e.printStackTrace();
			Main.closeServers();
		}
		
	}

	public void run()
	{	
		while(Main.isRunning)//bloque sur _SS.accept()
		{
			try
			{
				new LoginClient(_SS.accept());
			}catch(IOException e)
			{
				addToLog("IOException: "+e.getMessage());
				try
				{
					addToLog("Fermeture du serveur de connexion");	
					if(!_SS.isClosed())_SS.close();
				}
				catch(IOException e1){}
			}
		}
	}
	
	public void kickAll()
	{
		try {
			_SS.close();
		} catch (IOException e) {}
	}
	public synchronized static void addToLog(String str)
	{
		System.out.println(str);
		if(Main.canLog)
		{
			try {
				String date = Calendar.HOUR_OF_DAY+":"+Calendar.MINUTE+":"+Calendar.SECOND;
				Main.Log_Realm.write(date+": "+str);
				Main.Log_Realm.newLine();
				Main.Log_Realm.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}
	
	public synchronized static void addToSockLog(String str)
	{
		if(Main.CONFIG_DEBUG)System.out.println(str);
		if(Main.canLog)
		{
			try {
				String date = Calendar.HOUR_OF_DAY+":"+Calendar.MINUTE+":"+Calendar.SECOND;
				Main.Log_RealmSock.write(date+": "+str);
				Main.Log_RealmSock.newLine();
				Main.Log_RealmSock.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}

	public Thread getThread()
	{
		return _t;
	}
}
