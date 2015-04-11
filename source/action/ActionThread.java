package action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import objects.Personnage;

import common.Ancestra;
import common.SocketManager;
import common.SQLManager;
import common.Constants;
import common.World;
import objects.*;
import objects.Objet.*;

public class ActionThread implements Runnable{
	private BufferedReader _in;
	private Thread _t;
	private Socket _s;
	private Personnage _player;
	
	private int _numAction, _nbAction, _playerId, _itemId;
	private String couleur = "DF0101";	//Définit la couleur du message envoyer au client lors de l'ajout
	
	public ActionThread(Socket sock)
	{
		try
		{
			_s = sock;
			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));
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
	}
	
	public  void run()
	{
		try
    	{
			String packet = "";
			
			char charCur[] = new char[1];
	        
	    	while(_in.read(charCur, 0, 1)!=-1 && Ancestra.isRunning)
	    	{
	    		if (charCur[0] != ';' && charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
		    	{
	    			packet += charCur[0];
		    	}else if(packet != "")
		    	{
		    		ActionServer.addToSockLog("Action: Recv << "+packet);
		    		parsePacket(packet);
		    		packet = "";
		    	}
	    	}
    	}catch(IOException e)
    	{
    		try
    		{
	    		_in.close();
	    		if(!_s.isClosed())_s.close();
	    		_t.interrupt();
	    	}catch(IOException e1){};
    	}
    	finally
    	{
    		try
    		{
	    		_in.close();
	    		if(!_s.isClosed())_s.close();
	    		_t.interrupt();
	    	}catch(IOException e1){};
    	}
	}
	
	private boolean parsePacket(String packet)
	{
		String[] result = packet.split(":");	//Sépare le packet en utilisant ":" comme délimiteur
		String sortie = "+";
		ObjTemplate t;
		Objet obj;
		
		Ancestra.addToShopLog("Packet reçu : " + packet);
		
		if(result[0].equals("ZA"))	//ZA une action (ajout xp,kamas,lvl,...)
		{
			
			
			for (int iTokn = 1; iTokn < result.length; iTokn++) //Pour boucler dans le tableau de mot que l'on viens de créer en séparant le packet (ZA:Action:Nombre:PlayerID)
			{
				switch (iTokn)
				{
					case 1:	//Si on est rendu au mot #1, le mot #0 étant ZA
						_numAction = Integer.parseInt(result[iTokn]);
						break;
					case 2:
						_nbAction = Integer.parseInt(result[iTokn]);	//Multiplicateur de l'action (XP * _nbAction)
						break;
					case 3:
						_playerId = Integer.parseInt(result[iTokn]);	//L'ID du personnage à modifier
						_player = World.getPersonnage(_playerId);	//Récupère le personnage à partir de son PlayerID
						if(_player == null)
						{
							SQLManager.LOAD_PERSO(_playerId);
							_player = World.getPersonnage(_playerId);
						}
						break;
				}
			}
			
			switch (_numAction)	//Détermine quoi faire selon la valeur de _numAction
				{
					case 1:	//Monter d'un level
						if(_player.get_lvl() >= Ancestra.MAX_LEVEL) return false;
						_player.levelUp(true,true,true);
						sortie+="1 Niveau";
						SQLManager.SAVE_PERSONNAGE(_player,false);		//Enregistrement du personnage dans la base de données pour éviter d'avoir des informations non cohérente entre le jeux et le site
						Ancestra.addToShopLog("Ajout d'un lvl à : " + _player.get_name());
						
						break;
					case 2:	//Ajouter X point d'experience
						_player.addXp(_nbAction,true);
						sortie+=_nbAction+" Xp a votre personnage";
						SQLManager.SAVE_PERSONNAGE(_player,false);		//Enregistrement du personnage dans la base de données pour éviter d'avoir des informations non cohérente entre le jeux et le site
						Ancestra.addToShopLog("Ajout de " + _nbAction + "xp à " + _player.get_name());
						
						break;
					case 3:	//Ajouter X kamas
						_player.addKamas(_nbAction);
						_player.kamasLog(_nbAction+"", "Acheter sur la boutique (lvl"+_player.get_lvl()+")");
						
						sortie+=_nbAction+" Kamas à votre personnage";
						Ancestra.addToShopLog("Ajout de " + _nbAction + " kamas à " + _player.get_name());
						
						break;
					case 4:	//Ajouter X point de capital
						_player.addCapital(_nbAction);
						sortie+=_nbAction+" Point de capital à votre personnage";
						Ancestra.addToShopLog("Ajout de " + _nbAction + " capital à " + _player.get_name());
						
						break;
					case 5:	//Ajouter X point de sort
						_player.addSpellPoint(_nbAction);
						sortie+=_nbAction+" Point de sort à votre personnage";
						Ancestra.addToShopLog("Ajout de " + _nbAction + " spellPoint à " + _player.get_name());
						
						break;
					case 6: //Apprendre un sort
						_player.learnSpell(_nbAction,1,false,true);
						sortie = "Un nouveau sort viens d'être ajouté à votre personnage";
						Ancestra.addToShopLog("Ajout du sort " + _nbAction + " à " + _player.get_name());
						
						break;
					case 7: //Ajout de PA
						_player.get_baseStats().addOneStat(Constants.STATS_ADD_PA,_nbAction);	//Ajout du PA au stats, c'est temporaire en attendant le reload des persos qui chargeras celui de la DB
						sortie += _nbAction+" PA à votre personnage";
						Ancestra.addToShopLog("Ajout d'un PA à " + _player.get_name());
						
						break;
					case 8: //Ajout de PM
						_player.get_baseStats().addOneStat(Constants.STATS_ADD_PM,_nbAction);//Ajout du PM au stats, c'est temporaire en attendant le reload des persos qui chargeras celui de la DB
						sortie += _nbAction+" PM à votre personnage";
						Ancestra.addToShopLog("Ajout d'un PM à " + _player.get_name());
						
						break;
					case 22:	//Remettre les stats à zéro
					_player.resetStats();
					_player.setCapital((_player.get_lvl()-1) * 5);
					sortie = "Tout vos point de capital investis vous ont été retournés";
					Ancestra.addToShopLog("Remise à zéro des stats de " + _player.get_name());
					
					break;
				}	//Fin du swtich

		}else
		if(result[0].equals("ZO"))	//Sinon si le packet est un packet ZO objet
		{
					
			for (int iTokn = 1; iTokn < result.length; iTokn++) //Pour boucler dans le tableau de mot que l'on viens de créer en séparant le packet (ZO:Max:Nombre:ItemID:PlayerID)
			{
				switch (iTokn)
				{
					case 1:	//Si on est rendu au mot #1, le mot #0 étant ZO
						_numAction = Integer.parseInt(result[iTokn]);
						break;
					
					case 2:
						_nbAction = Integer.parseInt(result[iTokn]);
						break;
						
					case 3:
						_itemId = Integer.parseInt(result[iTokn]);
						break;
						
					case 4:
						_playerId = Integer.parseInt(result[iTokn]);
						_player = World.getPersonnage(_playerId);
						if(_player == null)
						{
							SQLManager.LOAD_PERSO(_playerId);
							_player = World.getPersonnage(_playerId);
						}
						break;
					
				}
			} //Fin du for
			
			switch (_numAction)
			{
				case 20:	//Ajouter un item avec des jets aléatoire
				
					t = World.getObjTemplate(_itemId);
					
					obj = t.createNewItem(_nbAction,false); //Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
					if(_player.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
						World.addObjet(obj,true);
					_player.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Acheté sur la boutique");
					
					ActionServer.addToSockLog("Objet "+_itemId+"ajoute a "+_player.get_name()+" avec des stats aleatoire");
					sortie = "Un objet viens d'être ajouté à votre personnage, allez voir votre inventaire!";
					Ancestra.addToShopLog("Ajout d'un objet stats aléatoire à " + _player.get_name());
					
					break;
				case 21:	//Ajouter un item avec des jets MAX
				
					t = World.getObjTemplate(_itemId);
					
					obj = t.createNewItem(_nbAction,true); //Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
					if(_player.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
						World.addObjet(obj,true);
					_player.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Acheté sur la boutique");
					
					ActionServer.addToSockLog("Objet "+_itemId+"ajouté à "+_player.get_name()+" avec des stats MAX");
					sortie = "Un objet avec des stats maximum viens d'être ajouté à votre personnage, allez voir votre inventaire!";
					Ancestra.addToShopLog("Ajout d'un objet stats max à " + _player.get_name());
					
					break;
			}//Fin du switch

		}//Fin equals."ZO"
		
		if(_player.isOnline())
		{
			SocketManager.GAME_SEND_MESSAGE(_player,sortie,couleur);	//Envoie du message		(mit ici pour qu'il soit executer peu importe le packet reçu)
			SocketManager.GAME_SEND_STATS_PACKET(_player);	//Mise à jour des stats du client
		}
		else
		{
			SQLManager.SAVE_PERSONNAGE(_player, true);
			World.unloadPerso(_playerId);
		}
		return true; 
	}//Fin parsePacket
}
