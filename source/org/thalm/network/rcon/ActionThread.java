package org.thalm.network.rcon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.thalm.common.Main;
import org.thalm.common.Variable;
import org.thalm.database.Database;
import org.thalm.game.World;
import org.thalm.game.character.Player;
import org.thalm.game.character.item.Item;
import org.thalm.game.character.item.Item.*;
import org.thalm.network.Packets;

public class ActionThread implements Runnable {
	
	private BufferedReader bufferedReader;
	private Thread thread;
	private Socket socket;
	private Player player;
	
	private int _numAction, _nbAction, _playerId, _itemId;
	private String couleur = "DF0101";	//D�finit la couleur du message envoyer au client lors de l'ajout
	
	public ActionThread(Socket sock)
	{
		try
		{
			socket = sock;
			bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
		catch(IOException e)
		{
			try {
				if(!socket.isClosed())socket.close();
			} catch (IOException e1) {}
		}
	}
	
	public  void run()
	{
		try
    	{
			String packet = "";
			
			char charCur[] = new char[1];
	        
	    	while(bufferedReader.read(charCur, 0, 1)!=-1 && Main.isRunning)
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
	    		bufferedReader.close();
	    		if(!socket.isClosed())socket.close();
	    		thread.interrupt();
	    	}catch(IOException e1){};
    	}
    	finally
    	{
    		try
    		{
	    		bufferedReader.close();
	    		if(!socket.isClosed())socket.close();
	    		thread.interrupt();
	    	}catch(IOException e1){};
    	}
	}
	
	private boolean parsePacket(String packet)
	{
		String[] result = packet.split(":");	//S�pare le packet en utilisant ":" comme d�limiteur
		String sortie = "+";
		ObjTemplate t;
		Item obj;
		
		Main.addToShopLog("Packet re�u : " + packet);
		
		if(result[0].equals("ZA"))	//ZA une action (ajout xp,kamas,lvl,...)
		{
			
			
			for (int iTokn = 1; iTokn < result.length; iTokn++) //Pour boucler dans le tableau de mot que l'on viens de cr�er en s�parant le packet (ZA:Action:Nombre:PlayerID)
			{
				switch (iTokn)
				{
					case 1:	//Si on est rendu au mot #1, le mot #0 �tant ZA
						_numAction = Integer.parseInt(result[iTokn]);
						break;
					case 2:
						_nbAction = Integer.parseInt(result[iTokn]);	//Multiplicateur de l'action (XP * _nbAction)
						break;
					case 3:
						_playerId = Integer.parseInt(result[iTokn]);	//L'ID du personnage � modifier
						player = World.getPersonnage(_playerId);	//R�cup�re le personnage � partir de son PlayerID
						if(player == null)
						{
							Database.LOAD_PERSO(_playerId);
							player = World.getPersonnage(_playerId);
						}
						break;
				}
			}
			
			switch (_numAction)	//D�termine quoi faire selon la valeur de _numAction
				{
					case 1:	//Monter d'un level
						if(player.get_lvl() >= Main.MAX_LEVEL) return false;
						player.levelUp(true,true,true);
						sortie+="1 Niveau";
						Database.SAVE_PERSONNAGE(player,false);		//Enregistrement du personnage dans la base de donn�es pour �viter d'avoir des informations non coh�rente entre le jeux et le site
						Main.addToShopLog("Ajout d'un lvl � : " + player.get_name());
						
						break;
					case 2:	//Ajouter X point d'experience
						player.addXp(_nbAction,true);
						sortie+=_nbAction+" Xp a votre personnage";
						Database.SAVE_PERSONNAGE(player,false);		//Enregistrement du personnage dans la base de donn�es pour �viter d'avoir des informations non coh�rente entre le jeux et le site
						Main.addToShopLog("Ajout de " + _nbAction + "xp � " + player.get_name());
						
						break;
					case 3:	//Ajouter X kamas
						player.addKamas(_nbAction);
						player.kamasLog(_nbAction+"", "Acheter sur la boutique (lvl"+player.get_lvl()+")");
						
						sortie+=_nbAction+" Kamas � votre personnage";
						Main.addToShopLog("Ajout de " + _nbAction + " kamas � " + player.get_name());
						
						break;
					case 4:	//Ajouter X point de capital
						player.addCapital(_nbAction);
						sortie+=_nbAction+" Point de capital � votre personnage";
						Main.addToShopLog("Ajout de " + _nbAction + " capital � " + player.get_name());
						
						break;
					case 5:	//Ajouter X point de sort
						player.addSpellPoint(_nbAction);
						sortie+=_nbAction+" Point de sort � votre personnage";
						Main.addToShopLog("Ajout de " + _nbAction + " spellPoint � " + player.get_name());
						
						break;
					case 6: //Apprendre un sort
						player.learnSpell(_nbAction,1,false,true);
						sortie = "Un nouveau sort viens d'�tre ajout� � votre personnage";
						Main.addToShopLog("Ajout du sort " + _nbAction + " � " + player.get_name());
						
						break;
					case 7: //Ajout de PA
						player.get_baseStats().addOneStat(Variable.STATS_ADD_PA,_nbAction);	//Ajout du PA au stats, c'est temporaire en attendant le reload des persos qui chargeras celui de la DB
						sortie += _nbAction+" PA � votre personnage";
						Main.addToShopLog("Ajout d'un PA � " + player.get_name());
						
						break;
					case 8: //Ajout de PM
						player.get_baseStats().addOneStat(Variable.STATS_ADD_PM,_nbAction);//Ajout du PM au stats, c'est temporaire en attendant le reload des persos qui chargeras celui de la DB
						sortie += _nbAction+" PM � votre personnage";
						Main.addToShopLog("Ajout d'un PM � " + player.get_name());
						
						break;
					case 22:	//Remettre les stats � z�ro
					player.resetStats();
					player.setCapital((player.get_lvl()-1) * 5);
					sortie = "Tout vos point de capital investis vous ont �t� retourn�s";
					Main.addToShopLog("Remise � z�ro des stats de " + player.get_name());
					
					break;
				}	//Fin du swtich

		}else
		if(result[0].equals("ZO"))	//Sinon si le packet est un packet ZO objet
		{
					
			for (int iTokn = 1; iTokn < result.length; iTokn++) //Pour boucler dans le tableau de mot que l'on viens de cr�er en s�parant le packet (ZO:Max:Nombre:ItemID:PlayerID)
			{
				switch (iTokn)
				{
					case 1:	//Si on est rendu au mot #1, le mot #0 �tant ZO
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
						player = World.getPersonnage(_playerId);
						if(player == null)
						{
							Database.LOAD_PERSO(_playerId);
							player = World.getPersonnage(_playerId);
						}
						break;
					
				}
			} //Fin du for
			
			switch (_numAction)
			{
				case 20:	//Ajouter un item avec des jets al�atoire
				
					t = World.getObjTemplate(_itemId);
					
					obj = t.createNewItem(_nbAction,false); //Si mis � "true" l'objet � des jets max. Sinon ce sont des jets al�atoire
					if(player.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
						World.addObjet(obj,true);
					player.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Achet� sur la boutique");
					
					ActionServer.addToSockLog("Objet "+_itemId+"ajoute a "+player.get_name()+" avec des stats aleatoire");
					sortie = "Un objet viens d'�tre ajout� � votre personnage, allez voir votre inventaire!";
					Main.addToShopLog("Ajout d'un objet stats al�atoire � " + player.get_name());
					
					break;
				case 21:	//Ajouter un item avec des jets MAX
				
					t = World.getObjTemplate(_itemId);
					
					obj = t.createNewItem(_nbAction,true); //Si mis � "true" l'objet � des jets max. Sinon ce sont des jets al�atoire
					if(player.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
						World.addObjet(obj,true);
					player.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Achet� sur la boutique");
					
					ActionServer.addToSockLog("Objet "+_itemId+"ajout� � "+player.get_name()+" avec des stats MAX");
					sortie = "Un objet avec des stats maximum viens d'�tre ajout� � votre personnage, allez voir votre inventaire!";
					Main.addToShopLog("Ajout d'un objet stats max � " + player.get_name());
					
					break;
			}//Fin du switch

		}//Fin equals."ZO"
		
		if(player.isOnline())
		{
			Packets.GAME_SEND_MESSAGE(player,sortie,couleur);	//Envoie du message		(mit ici pour qu'il soit executer peu importe le packet re�u)
			Packets.GAME_SEND_STATS_PACKET(player);	//Mise � jour des stats du client
		}
		else
		{
			Database.SAVE_PERSONNAGE(player, true);
			World.unloadPerso(_playerId);
		}
		return true; 
	}//Fin parsePacket
}
