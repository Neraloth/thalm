package org.thalm.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.thalm.common.Main;
import org.thalm.common.Variable;
import org.thalm.database.Database;
import org.thalm.game.World;
import org.thalm.game.character.Account;

public class LoginClient implements Runnable{
	private BufferedReader _in;
	private Thread _t;
	private PrintWriter _out;
	private Socket _s;
	private String _hashKey;
	private int _packetNum = 0;
	private String _accountName;
	private String _hashPass;
	private Account _compte;
	
	public LoginClient(Socket sock)
	{
		try
		{
			_s = sock;
			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));
			_out = new PrintWriter(_s.getOutputStream());
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}
		catch(IOException e)
		{
			try {
				if(!_s.isClosed())_s.close();
			} catch (IOException e1) {}
		}
		finally
		{
			if(_compte != null)
			{
				_compte.setRealmThread(null);
				_compte.setGameThread(null);
			}
		}
	}

	public  void run()
	{
		try
    	{
			String packet = "";
			char charCur[] = new char[1];
			if(Main.CONFIG_POLICY)
				Packets.REALM_SEND_POLICY_FILE(_out);
	        
			_hashKey = Packets.REALM_SEND_HC_PACKET(_out);
	        
	    	while(_in.read(charCur, 0, 1)!=-1 && Main.isRunning)
	    	{
	    		if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
		    	{
	    			packet += charCur[0];
		    	}else if(packet != "")
		    	{
		    		LoginServer.addToSockLog("Realm: Recv << "+packet);
		    		_packetNum++;
		    		parsePacket(packet);
		    		packet = "";
		    	}
	    	}
    	}catch(IOException e)
    	{
    		try
    		{
	    		_in.close();
	    		_out.close();
	    		if(_compte != null)
	    		{
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setRealmThread(null);
	    		}
	    		if(!_s.isClosed())_s.close();
	    		_t.interrupt();
	    	}catch(IOException e1){};
    	}
    	finally
    	{
    		try
    		{
	    		_in.close();
	    		_out.close();
	    		if(_compte != null)
	    		{
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setRealmThread(null);
	    		}
	    		if(!_s.isClosed())_s.close();
	    		_t.interrupt();
	    	}catch(IOException e1){};
    	}
	}
	private void parsePacket(String packet)
	{
		switch(_packetNum)
		{
			case 1://Version
				if(!packet.equalsIgnoreCase(Variable.CLIENT_VERSION) && !Variable.IGNORE_VERSION)
				{
					Packets.REALM_SEND_REQUIRED_VERSION(_out);
					try {
						this._s.close();
					} catch (IOException e) {}
				}
				break;
			case 2://Account Name
				_accountName = packet.toLowerCase();
				break;
			case 3://HashPass
				if(!packet.substring(0, 2).equalsIgnoreCase("#1"))
				{
					try {
						this._s.close();
					} catch (IOException e) {}
				}
				_hashPass = packet;
				
				if(Account.COMPTE_LOGIN(_accountName,_hashPass,_hashKey))
				{
					_compte = World.getCompteByName(_accountName);
					if(_compte.isOnline() && _compte.getGameThread()!=null)
					{
						_compte.getGameThread().closeSocket();
					}
					if(_compte.isBanned())
					{
						Packets.REALM_SEND_BANNED(_out);
						try {
							_s.close();
						} catch (IOException e) {}
						return;
					}
					if(Main.CONFIG_PLAYER_LIMIT != -1 && Main.CONFIG_PLAYER_LIMIT <= Main.gameServer.getPlayerNumber())
					{
						//Seulement si joueur
						if(_compte.get_gmLvl() == 0)
						{
							Packets.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
							try {
								_s.close();
							} catch (IOException e) {}
							return;
						}
					}
					String ip = _s.getInetAddress().getHostAddress();
					//Verification Multi compte
					if(World.ipIsUsed(ip) >= Main.CONFIG_MAX_MULTI)
					{
						Packets.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
						try {
							_s.close();
						} catch (IOException e) {}
						return;
					}
					_compte.setRealmThread(this);
					_compte.setCurIP(ip);
					Packets.REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS(_out, _compte.get_pseudo(),(_compte.get_gmLvl()>0?(1):(0)), _compte.get_question() ); 
				}else//Si le compte n'a pas été reconnu
				{
					Database.LOAD_ACCOUNT_BY_USER(_accountName);
					if(Account.COMPTE_LOGIN(_accountName,_hashPass,_hashKey))
					{
						_compte = World.getCompteByName(_accountName);
						if(Main.CONFIG_PLAYER_LIMIT != 0 && Main.CONFIG_PLAYER_LIMIT <= Main.gameServer.getPlayerNumber())
						{
							//Seulement si joueur
							if(_compte.get_gmLvl() == 0)
							{
								Packets.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
								try {
									_s.close();
								} catch (IOException e) {}
								return;
							}
						}
						if(_compte.isOnline())
						{
							Packets.REALM_SEND_ALREADY_CONNECTED(_out);
							try {
								this._s.close();
							} catch (IOException e) {}
							return;
						}
						
						if(_compte.isBanned())
						{
							Packets.REALM_SEND_BANNED(_out);
							try {
								this._s.close();
							} catch (IOException e) {}
							return;
						}
						String ip = _s.getInetAddress().getHostAddress();
						//Verification Multi compte
						if(World.ipIsUsed(ip) >= Main.CONFIG_MAX_MULTI)
						{
							Packets.REALM_SEND_TOO_MANY_PLAYER_ERROR(_out);
							try {
								_s.close();
							} catch (IOException e) {}
							return;
						}
						_compte.setCurIP(ip);
						_compte.setRealmThread(this);
						Packets.REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS(_out, _compte.get_pseudo(),(_compte.get_gmLvl()>0?(1):(0)), _compte.get_question() ); 
					}else//Si le compte n'a pas été reconnu
					{
						Packets.REALM_SEND_LOGIN_ERROR(_out);
						try {
							this._s.close();
						} catch (IOException e) {}
					}
				}
				break;
			default:
				if(packet.substring(0,2).equals("Af"))
				{
					int queueID = 1;
					int position = 1;
					_packetNum--;
					Packets.MULTI_SEND_Af_PACKET(_out,position,1,1,0,queueID);
				}else
				if(packet.substring(0,2).equals("Ax"))
				{
					if(_compte == null)return;
					Database.LOAD_PERSO_BY_ACCOUNT(_compte.get_GUID());
					Packets.REALM_SEND_PERSO_LIST(_out, _compte.GET_PERSO_NUMBER());
				}else
				if(packet.equals("AX1"))
				{
					Main.gameServer.addWaitingCompte(_compte);
					String ip = _compte.get_curIP();
					Packets.REALM_SEND_GAME_SERVER_IP(_out, _compte.get_GUID(),ip.equals("127.0.0.1"));
				}
				break;
		}
	}
}
