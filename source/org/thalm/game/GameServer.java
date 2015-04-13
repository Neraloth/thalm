package org.thalm.game;

import java.io.IOException;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.thalm.common.*;
import org.thalm.game.character.Account;
import org.thalm.network.Packets;

public class GameServer implements Runnable{

	private ServerSocket _SS;
	private Thread _t;
	private ArrayList<GameClient> _clients = new ArrayList<GameClient>();
	private ArrayList<Account> _waitings = new ArrayList<Account>();
	private Timer _saveTimer;
	private long _startTime;
	private int _maxPlayer = 0;
	
	public GameServer(String Ip)
	{
		try {
			_saveTimer = new Timer();
			_saveTimer.schedule(new TimerTask()
			{
				public void run()
				{
					if(!Main.isSaving)
					{
						Thread t = new Thread(new SaveThread());
						t.start();
					}
				}
			}, Main.CONFIG_SAVE_TIME,Main.CONFIG_SAVE_TIME);

			_SS = new ServerSocket(Main.CONFIG_GAME_PORT);
			if(Main.CONFIG_USE_IP)
				Main.GAMESERVER_IP = Cipher.CryptIP(Ip)+Cipher.CryptPort(Main.CONFIG_GAME_PORT);
			_startTime = System.currentTimeMillis();
			_t = new Thread(this);
			_t.start();
		} catch (IOException e) {
			addToLog("IOException: "+e.getMessage());
			e.printStackTrace();
			Main.closeServers();
		}
	}
	
	public static class SaveThread implements Runnable
	{
		public void run()
		{
			Packets.GAME_SEND_MESSAGE_TO_ALL("Une sauvegarde a demarree!", Main.CONFIG_MOTD_COLOR);
			World.saveAll(null);
			Packets.GAME_SEND_MESSAGE_TO_ALL("Sauvegarde effectuee!", Main.CONFIG_MOTD_COLOR);
		}
	}
	
	public ArrayList<GameClient> getClients() {
		return _clients;
	}

	public long getStartTime()
	{
		return _startTime;
	}
	
	public int getMaxPlayer()
	{
		return _maxPlayer;
	}
	
	public int getPlayerNumber()
	{
		return _clients.size();
	}
	public void run()
	{	
		while(Main.isRunning)//bloque sur _SS.accept()
		{
			try
			{
				_clients.add(new GameClient(_SS.accept()));
				if(_clients.size() > _maxPlayer)_maxPlayer = _clients.size();
			}catch(IOException e)
			{
				addToLog("IOException: "+e.getMessage());
				try
				{
					if(!_SS.isClosed())_SS.close();
					Main.closeServers();
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
		//Copie
		ArrayList<GameClient> c = new ArrayList<GameClient>();
		c.addAll(_clients);
		for(GameClient GT : c)
		{
			try
			{
				GT.closeSocket();
			}catch(Exception e){};	
		}
	}
	
	public synchronized static void addToLog(String str)
	{
		System.out.println(str);
		if(Main.canLog)
		{
			try {
				String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
				Main.Log_Game.write(date+": "+str);
				Main.Log_Game.newLine();
				Main.Log_Game.flush();
			} catch (IOException e) {e.printStackTrace();}//ne devrait pas avoir lieu
		}
	}
	
	public synchronized static void addToSockLog(String str)
	{
		if(Main.CONFIG_DEBUG)System.out.println(str);
		if(Main.canLog)
		{
			try {
				String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
				Main.Log_GameSock.write(date+": "+str);
				Main.Log_GameSock.newLine();
				Main.Log_GameSock.flush();
			} catch (IOException e) {}//ne devrait pas avoir lieu
		}
	}

	public void delClient(GameClient gameThread)
	{
		_clients.remove(gameThread);
		if(_clients.size() > _maxPlayer)_maxPlayer = _clients.size();
	}

	public synchronized Account getWaitingCompte(int guid)
	{
		for (int i = 0; i < _waitings.size(); i++)
		{
			if(_waitings.get(i).get_GUID() == guid)
				return _waitings.get(i);
		}
		return null;
	}
	
	public synchronized void delWaitingCompte(Account _compte)
	{
		_waitings.remove(_compte);
	}
	
	public synchronized void addWaitingCompte(Account _compte)
	{
		_waitings.add(_compte);
	}
	public static String getServerTime()
	{
		Date actDate = new Date();
		return "BT"+(actDate.getTime()+3600000);
	}
	public static String getServerDate()
	{
		Date actDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");
		String jour = Integer.parseInt(dateFormat.format(actDate))+"";
		while(jour.length() <2)
		{
			jour = "0"+jour;
		}
		dateFormat = new SimpleDateFormat("MM");
		String mois = (Integer.parseInt(dateFormat.format(actDate))-1)+"";
		while(mois.length() <2)
		{
			mois = "0"+mois;
		}
		dateFormat = new SimpleDateFormat("yyyy");
		String annee = (Integer.parseInt(dateFormat.format(actDate))-1370)+"";
		return "BD"+annee+"|"+mois+"|"+jour;
	}

	public Thread getThread()
	{
		return _t;
	}
}
