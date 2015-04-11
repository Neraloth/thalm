package common;

import game.GameThread;
import game.GameServer.SaveThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.TreeMap;
import java.util.Map.Entry;

import common.World.ItemSet;

import objects.Action;
import objects.Carte;
import objects.Compte;
import objects.NPC_tmpl;
import objects.Objet;
import objects.Personnage;
import objects.PierreAme;
import objects.Carte.MountPark;
import objects.Hdv.HdvEntry;
import objects.Metier.StatsMetier;
import objects.Monstre.MobGroup;
import objects.NPC_tmpl.NPC;
import objects.NPC_tmpl.NPC_question;
import objects.NPC_tmpl.NPC_reponse;
import objects.Objet.ObjTemplate;

public class Commands {
	Compte _compte;
	Personnage _perso;
	PrintWriter _out;
	
	public Commands(Personnage perso)
	{
		this._compte = perso.get_compte();
		this._perso = perso;
		this._out = _compte.getGameThread().get_out();
	}
	
	public void consoleCommand(String packet)
	{
		if(_compte.get_gmLvl() == 0)
		{
			_compte.getGameThread().closeSocket();
			return;
		}
		
		String msg = packet.substring(2);
		String[] infos = msg.split(" ");
		if(infos.length == 0)return;
		String command = infos[0];
		
		if(Ancestra.canLog)
		{
			Ancestra.addToMjLog(_compte.get_curIP()+": "+_compte.get_name()+" "+_perso.get_name()+"=>"+msg);
		}
		if(command.equalsIgnoreCase("EXIT"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			System.exit(0);
		}else if(command.equalsIgnoreCase("RESETSAVE"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			try
			{
				World.resetSave();
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur! :"+e.getMessage());
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, e.getStackTrace()+"");
				return;
			}
			
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Variable de sauvegarde reseté!");
			return;
			
		}else if(command.equalsIgnoreCase("FORGETSPELL"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			
			Personnage perso;
			if(infos.length >= 2)
				perso = World.getPersoByName(infos[1]);
			else
				perso = _perso;
			
			if(perso == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage '" + infos[1] + "' introuvable!");
				return;
			}
			
			perso.setisForgetingSpell(true);
			SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', perso);
			
			if(perso.isForgetingSpell())
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Interface d'oubli de sort ouvert");
			}
			return;
			
		}else if(command.equalsIgnoreCase("FULLHDV"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			
			fullHdv(Integer.parseInt(infos[1]));
			return;
			
		}else if(command.equalsIgnoreCase("SETGUILDRANK"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage toChange = null;
			
			if(infos.length > 3)
			{
				toChange = World.getPersoByName(infos[3]);
			}
			else
			{
				toChange = _perso;
			}
			
			if(toChange == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage non trouvé dans la mémoire!");
				return;
			}
			else if(toChange.getGuildMember() == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le personnage n'a pas de guilde");
				return;
			}
			
			int rank;
			int right;
			try
			{
				rank = Integer.parseInt(infos[1]);
				right = Integer.parseInt(infos[2]);
			}catch(NumberFormatException e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeurs invalide!");
				return;
			}
			
			toChange.getGuildMember().setAllRights(rank, (byte) -1, right);
			SocketManager.GAME_SEND_gS_PACKET(toChange,toChange.getGuildMember());
			
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Rang correctement changé!");
			return;
			
		}else if(command.equalsIgnoreCase("SPAWNMOB"))//Format : SPAWNMOB id,lvlMin,lvlMax;id,lvlMin,lvlMax... CONDITIONS(Facultatif)
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			
			String groupData = infos[1];
			String cond = "";
			if(infos.length > 2)
				cond = infos[2];
			
			_perso.get_curCarte().spawnGroup(false, false, _perso.get_curCell().getID(), groupData, cond);
			return;
			
		}else if(command.equalsIgnoreCase("RELOADCONFIG"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			
			Ancestra.loadConfiguration();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Fichier de configuration rechargé!");
			return;
			
		}else if(command.equalsIgnoreCase("GENPIERRE"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			PierreAme pierre = new PierreAme(World.getNewItemGuid(), 1, 7010, -1, infos[1]);
			if(_perso.addObjet(pierre, false))
				World.addObjet(pierre, true);
			_perso.objetLog(pierre.getTemplate().getID(), 1, "Ajouté par un MJ");
			
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Pierre créé");
			return;
		}
		else
		if(command.equalsIgnoreCase("INFOS"))
		{
			long uptime = System.currentTimeMillis() - Ancestra.gameServer.getStartTime();
			int jour = (int) (uptime/(1000*3600*24));
			uptime %= (1000*3600*24);
			int hour = (int) (uptime/(1000*3600));
			uptime %= (1000*3600);
			int min = (int) (uptime/(1000*60));
			uptime %= (1000*60);
			int sec = (int) (uptime/(1000));
			
			String mess =	"===========\n"
				+       	"mAncestra v. "+Constants.SERVER_VERSION+" par "+Constants.SERVER_MAKER+"\n"
				+			"\n"
				+			"Uptime: "+jour+"j "+hour+"h "+min+"m "+sec+"s\n"
				+			"Joueurs en lignes: "+Ancestra.gameServer.getPlayerNumber()+"\n"
				+			"Record de connexion: "+Ancestra.gameServer.getMaxPlayer()+"\n"
				+			"===========";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("REFRESHMOBS"))
		{
			_perso.get_curCarte().refreshSpawns();
			String mess = "Mob Spawn refreshed!";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("SAVE") && !Ancestra.isSaving)
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Thread t = new Thread(new SaveThread());
			t.start();
			String mess = "Sauvegarde lancee!";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("MAPINFO"))
		{
			String mess = 	"==========\n"
						+	"Liste des Npcs de la carte:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			Carte map = _perso.get_curCarte();
			for(Entry<Integer,NPC> entry : map.get_npcs().entrySet())
			{
				mess = entry.getKey()+" "+entry.getValue().get_template().get_id()+" "+entry.getValue().get_cellID()+" "+entry.getValue().get_template().get_initQuestionID();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = "Liste des groupes de monstres:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			for(Entry<Integer,MobGroup> entry : map.getMobGroups().entrySet())
			{
				mess = entry.getKey()+" "+entry.getValue().getCellID()+" "+entry.getValue().getAlignement()+" "+entry.getValue().getSize();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = "==========";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("WHO"))
		{
			String mess = 	"==========\n"
				+			"Liste des joueurs en ligne:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			int diff = Ancestra.gameServer.getClients().size() -  30;
			for(byte b = 0; b < 30; b++)
			{
				if(b == Ancestra.gameServer.getClients().size())break;
				GameThread GT = Ancestra.gameServer.getClients().get(b);
				Personnage P = GT.getPerso();
				if(P == null)continue;
				mess = P.get_name()+"("+P.get_GUID()+") ";
				
				switch(P.get_classe())
				{
					case Constants.CLASS_FECA:
						mess += "Fec";
					break;
					case Constants.CLASS_OSAMODAS:
						mess += "Osa";
					break;
					case Constants.CLASS_ENUTROF:
						mess += "Enu";
					break;
					case Constants.CLASS_SRAM:
						mess += "Sra";
					break;
					case Constants.CLASS_XELOR:
						mess += "Xel";
					break;
					case Constants.CLASS_ECAFLIP:
						mess += "Eca";
					break;
					case Constants.CLASS_ENIRIPSA:
						mess += "Eni";
					break;
					case Constants.CLASS_IOP:
						mess += "Iop";
					break;
					case Constants.CLASS_CRA:
						mess += "Cra";
					break;
					case Constants.CLASS_SADIDA:
						mess += "Sad";
					break;
					case Constants.CLASS_SACRIEUR:
						mess += "Sac";
					break;
					case Constants.CLASS_PANDAWA:
						mess += "Pan";
					break;
					default:
						mess += "Unk";
				}
				mess += " ";
				mess += (P.get_sexe()==0?"M":"F")+" ";
				mess += P.get_lvl()+" ";
				mess += P.get_curCarte().get_id()+"("+P.get_curCarte().getX()+"/"+P.get_curCarte().getY()+") ";
				mess += P.get_fight()==null?"":"Combat ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			if(diff >0)
			{
				mess = 	"Et "+diff+" autres personnages";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = 	"==========\n";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("SHOWFIGHTPOS"))
		{
			String mess = "Liste des StartCell [teamID][cellID]:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			String places = _perso.get_curCarte().get_placesStr();
			if(places.indexOf('|') == -1 || places.length() <2)
			{
				mess = "Les places n'ont pas ete definies";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
				return;
			}
			String team0 = "",team1 = "";
			String[] p = places.split("\\|");
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			mess = "Team 0:\n";
			boolean isFirst = true;
			for(int a = 0;a <= team0.length()-2; a+=2)
			{
				if(!isFirst)
					mess += ", ";
				String code = team0.substring(a,a+2);
				mess += CryptManager.cellCode_To_ID(code);
				
				isFirst = false;
			}
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			mess = "Team 1:\n";
			isFirst = true;
			for(int a = 0;a <= team1.length()-2; a+=2)
			{
				if(!isFirst)
					mess += ", ";
				
				String code = team1.substring(a,a+2);
				mess += CryptManager.cellCode_To_ID(code);
				
				isFirst = false;
			}
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("DELFIGHTPOS"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int cell = -1;
			try
			{
				cell = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(cell < 0 || _perso.get_curCarte().getCase(cell) == null)
			{
				cell = _perso.get_curCell().getID();
			}
			String places = _perso.get_curCarte().get_placesStr();
			String[] p = places.split("\\|");
			String newPlaces = "";
			String team0 = "",team1 = "";
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			
			for(int a = 0;a<=team0.length()-2;a+=2)
			{
				String c = p[0].substring(a,a+2);
				if(cell == CryptManager.cellCode_To_ID(c))continue;
				newPlaces += c;
			}
			newPlaces += "|";
			for(int a = 0;a<=team1.length()-2;a+=2)
			{
				String c = p[1].substring(a,a+2);
				if(cell == CryptManager.cellCode_To_ID(c))continue;
				newPlaces += c;
			}
			_perso.get_curCarte().setPlaces(newPlaces);
			if(!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))return;
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Les places ont ete modifiees ("+newPlaces+")");
			return;
		}
		else
		if(command.equalsIgnoreCase("CREATEGUILD"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage perso = _perso;
			if(infos.length >1)
			{
				perso = World.getPersoByName(infos[1]);
			}
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			if(!perso.isOnline())
			{
				String mess = "Le personnage "+perso.get_name()+" n'etait pas connecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(perso.get_guild() != null || perso.getGuildMember() != null)
			{
				String mess = "Le personnage "+perso.get_name()+" a deja une guilde";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			SocketManager.GAME_SEND_gn_PACKET(perso);
			String mess = perso.get_name()+": Panneau de creation de guilde ouvert";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("TOOGLEAGGRO"))
		{
			Personnage perso = _perso;
			String name = infos[1];
			
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			perso.set_canAggro(!perso.canAggro());
			String mess = perso.get_name();
			if(perso.canAggro()) mess += " peut maintenant etre aggresser";
			else mess += " ne peut plus etre agresser";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}
		else
		if(infos.length <2 && !infos[0].equalsIgnoreCase("LISTFILE"))
		{
			String mess = "Commande non reconnue ou incomplete";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}
		//Commandes avec 1 argument
		infos = msg.split(" ");

		if(command.equalsIgnoreCase("ANNOUNCE"))
		{
			infos = msg.split(" ",2);
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(infos[1], Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		else
		if(command.equalsIgnoreCase("NAMEANNOUNCE"))
		{
			infos = msg.split(" ",2);
			String prefix = "["+_perso.get_name()+"]";
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(prefix+infos[1], Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		else
		if(command.equalsIgnoreCase("BAN"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage P = World.getPersoByName(infos[1]);
			Compte c;
			if(P == null)	//Si le personnage est introuvable dans la mémoire
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage non trouvé dans la mémoire\nRecherche dans la BD...");
				int accID = SQLManager.LOAD_ACCOUNT_BY_PERSO(infos[1]);
				
				c = World.getCompte(accID);
			}else
			{
				c = P.get_compte();
			}
			
			if(c == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage introuvable");
				return;
			}
			P.get_compte().setBanned(true);
			SQLManager.UPDATE_ACCOUNT_DATA(P.get_compte());
			if(c.getGameThread() != null)
				c.getGameThread().kick();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez banni "+P.get_name());
			return;
		}
		else
		if(command.equalsIgnoreCase("UNBAN"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Compte c = World.getCompteByName(infos[1]);
			if(c == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Compte du personnage non trouvé dans la mémoire\nRecherche dans la BD...");
				
				int accID = SQLManager.LOAD_ACCOUNT_BY_PERSO(infos[1]);
				c = World.getCompte(accID);
				
				if(c == null)
				{
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Compte du personnage non trouvé");
					return;
				}
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Compte trouvé!");
			}
			c.setBanned(false);
			SQLManager.UPDATE_ACCOUNT_DATA(c);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez debanni le compte '"+c.get_name()+"'");
			return;
		}
		else
		if(command.equalsIgnoreCase("ADDFIGHTPOS"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int team = -1;
			int cell = -1;
			try
			{
				team = Integer.parseInt(infos[1]);
				cell = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if( team < 0 || team>1)
			{
				String str = "Team ou cellID incorects";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(cell <0 || _perso.get_curCarte().getCase(cell) == null || !_perso.get_curCarte().getCase(cell).isWalkable(true))
			{
				cell = _perso.get_curCell().getID();
			}
			String places = _perso.get_curCarte().get_placesStr();
			String[] p = places.split("\\|");
			boolean already = false;
			String team0 = "",team1 = "";
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			
			//Si case déjà utilisée
			System.out.println("0 => "+team0+"\n1 =>"+team1+"\nCell: "+CryptManager.cellID_To_Code(cell));
			for(int a = 0; a <= team0.length()-2;a+=2)if(cell == CryptManager.cellCode_To_ID(team0.substring(a,a+2)))already = true;
			for(int a = 0; a <= team1.length()-2;a+=2)if(cell == CryptManager.cellCode_To_ID(team1.substring(a,a+2)))already = true;
			if(already)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"La case est deja dans la liste");
				return;
			}
			if(team == 0)team0 += CryptManager.cellID_To_Code(cell);
			else if(team == 1)team1 += CryptManager.cellID_To_Code(cell);
			
			String newPlaces = team0+"|"+team1;
			
			_perso.get_curCarte().setPlaces(newPlaces);
			if(!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))return;
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Les places ont ete modifiees ("+newPlaces+")");
			return;
		}
		else
		if(command.equalsIgnoreCase("SETMAXGROUP"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -1;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "Le nombre de groupe a ete fixe";
			_perso.get_curCarte().setMaxGroup(id);
			boolean ok = SQLManager.SAVE_MAP_DATA(_perso.get_curCarte());
			if(ok)mess += " et a ete sauvegarder a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ADDREPONSEACTION"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -30;
			int repID = 0;
			String args = infos[3];
			try
			{
				repID = Integer.parseInt(infos[1]);
				id = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_reponse rep = World.getNPCreponse(repID);
			if(id == -30 || rep == null)
			{
				String str = "Au moins une des valeur est invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			
			rep.addAction(new Action(id,args,""));
			boolean ok = SQLManager.ADD_REPONSEACTION(repID,id,args);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("SETINITQUESTION"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -30;
			int q = 0;
			try
			{
				q = Integer.parseInt(infos[2]);
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == -30)
			{
				String str = "NpcID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			NPC_tmpl npc = World.getNPCTemplate(id);
			
			npc.setInitQuestion(q);
			boolean ok = SQLManager.UPDATE_INITQUESTION(id,q);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ADDENDFIGHTACTION"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -30;
			int type = 0;
			String args = infos[3];
			String cond = infos[4];
			try
			{
				type = Integer.parseInt(infos[1]);
				id = Integer.parseInt(infos[2]);
				
			}catch(Exception e){};
			if(id == -30)
			{
				String str = "Au moins une des valeur est invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			_perso.get_curCarte().addEndFightAction(type, new Action(id,args,cond));
			boolean ok = SQLManager.ADD_ENDFIGHTACTION(_perso.get_curCarte().get_id(),type,id,args,cond);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}else
		if(command.equalsIgnoreCase("MUTE"))
		{
			if(_compte.get_gmLvl() < 1)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage perso = _perso;
			String name = infos[1];
			int time = 0;
			try
			{
				time = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			
			perso = World.getPersoByName(name);
			if(perso == null || time < 0)
			{
				String mess = "Le personnage n'existe pas ou la duree est invalide.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			String mess = "Vous avez mute "+perso.get_name()+" pour "+time+" secondes";
			if(perso.get_compte() == null)
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			perso.get_compte().mute(true,time);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}else
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1124;"+time);
			}
			return;
		}
		else
		if(command.equalsIgnoreCase("UNMUTE"))
		{
			Personnage perso = _perso;
			String name = infos[1];
			
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			perso.get_compte().mute(false,0);
			String mess = "Vous avez unmute "+perso.get_name();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}
		else
		if(command.equalsIgnoreCase("KICK"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage perso = _perso;
			String name = infos[1];
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(perso.isOnline())
			{
				perso.get_compte().getGameThread().kick();
				String mess = "Vous avez kick "+perso.get_name();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				
			}
			else
			{
				String mess = "Le personnage "+perso.get_name()+" n'est pas connecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
			return;
		}
		else
		if(command.equalsIgnoreCase("SPELLPOINT"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int pts = -1;
			try
			{
				pts = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(pts == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.addSpellPoint(pts);
			SocketManager.GAME_SEND_STATS_PACKET(target);
			String str = "Le nombre de point de sort a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEARNSPELL"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int spell = -1;
			try
			{
				spell = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(spell == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.learnSpell(spell, 1, true,true);
			
			String str = "Le sort a ete appris";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			
			return;
		}else
		if(command.equalsIgnoreCase("SETALIGN"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			byte align = -1;
			try
			{
				align = Byte.parseByte(infos[1]);
			}catch(Exception e){};
			if(align < Constants.ALIGNEMENT_NEUTRE || align >Constants.ALIGNEMENT_MERCENAIRE)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.modifAlignement(align);
			
			String str = "L'alignement du joueur a ete modifie";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETREPONSES"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			if(infos.length <3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Il manque un/des arguments");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			String reps = infos[2];
			NPC_question Q = World.getNPCQuestion(id);
			String str = "";
			if(id == 0 || Q == null)
			{
				str = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Q.setReponses(reps);
			boolean a= SQLManager.UPDATE_NPCREPONSES(id,reps);
			str = "Liste des reponses pour la question "+id+": "+Q.getReponses();
			if(a)str += "(sauvegarde dans la BDD)";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("SHOWREPONSES"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC_question Q = World.getNPCQuestion(id);
			String str = "";
			if(id == 0 || Q == null)
			{
				str = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			str = "Liste des reponses pour la question "+id+": "+Q.getReponses();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("HONOR"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int honor = 0;
			try
			{
				honor = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			String str = "Vous avez ajouter "+honor+" honneur a "+target.get_name();
			if(target.get_align() == Constants.ALIGNEMENT_NEUTRE)
			{
				str = "Le joueur est neutre ...";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			target.addHonor(honor);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			
		}else
		if(command.equalsIgnoreCase("ADDJOBXP"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int job = -1;
			int xp = -1;
			try
			{
				job = Integer.parseInt(infos[1]);
				xp = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(job == -1 || xp < 0)
			{
				String str = "Valeurs invalides";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 3)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			StatsMetier SM = target.getMetierByID(job);
			if(SM== null)
			{
				String str = "Le joueur ne connais pas le métier demandé";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
				
			SM.addXp(target, xp);
			
			String str = "Le metier a ete experimenter";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEARNJOB"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int job = -1;
			try
			{
				System.out.println(infos[1]);
				job = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(job == -1 || World.getMetier(job) == null)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.learnJob(World.getMetier(job));
			
			String str = "Le metier a ete appris";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("CAPITAL"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int pts = -1;
			try
			{
				pts = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(pts == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.addCapital(pts);
			SocketManager.GAME_SEND_STATS_PACKET(target);
			String str = "Le capital a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SPAWNFIX"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			String groupData = infos[1];

			_perso.get_curCarte().addStaticGroup(_perso.get_curCell().getID(), groupData);
			String str = "Le grouppe a ete fixe";
			//Sauvegarde DB de la modif
			if(SQLManager.SAVE_NEW_FIXGROUP(_perso.get_curCarte().get_id(),_perso.get_curCell().getID(), groupData))
				str += " et a ete sauvegarde dans la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}
		if(command.equalsIgnoreCase("SIZE"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int size = -1;
			try
			{
				size = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(size == -1)
			{
				String str = "Taille invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.set_size(size);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "La taille du joueur a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETADMIN"))
		{
			if(_compte.get_gmLvl() < 4)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int gmLvl = -100;
			try
			{
				gmLvl = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(gmLvl == -100)
			{
				String str = "Valeur incorrecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.get_compte().setGmLvl(gmLvl);
			SQLManager.UPDATE_ACCOUNT_DATA(target.get_compte());
			String str = "Le niveau GM du joueur a ete modifie";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("DEMORPH"))
		{
			Personnage target = _perso;
			if(infos.length > 1)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[1]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			int morphID = target.get_classe()*10 + target.get_sexe();
			target.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "Le joueur a ete transformé";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("MORPH"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int morphID = -1;
			try
			{
				morphID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(morphID == -1)
			{
				String str = "MorphID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "Le joueur a ete transformé";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("GONAME") || command.equalsIgnoreCase("JOIN"))
		{
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int mapID = P.get_curCarte().get_id();
			int cellID = P.get_curCell().getID();
			
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié 
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
				if(target.get_fight() != null)
				{
					String str = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("NAMEGO"))
		{
			Personnage target = World.getPersoByName(infos[1]);
			if(target == null)
			{
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(target.get_fight() != null)
			{
				String str = "La cible est en combat";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage P = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				P = World.getPersoByName(infos[2]);
				if(P == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			int mapID = P.get_curCarte().get_id();
			int cellID = P.get_curCell().getID();
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDNPC"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == 0 || World.getNPCTemplate(id) == null)
			{
				String str = "NpcID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			NPC npc = _perso.get_curCarte().addNpc(id, _perso.get_curCell().getID(), _perso.get_orientation());
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(), npc);
			String str = "Le PNJ a ete ajoute";
			if(_perso.get_orientation() == 0
					|| _perso.get_orientation() == 2
					|| _perso.get_orientation() == 4
					|| _perso.get_orientation() == 6)
						str += " mais est invisible (orientation diagonale invalide).";
			
			if(SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(), id, _perso.get_curCell().getID(), _perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("DELNPC"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC npc = _perso.get_curCarte().getNPC(id);
			if(id == 0 || npc == null)
			{
				String str = "Npc GUID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int exC = npc.get_cellID();
			//on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), id);
			_perso.get_curCarte().removeNpcOrMobGroup(id);
			
			String str = "Le PNJ a ete supprime";
			if(SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(),exC))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("MOVENPC"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC npc = _perso.get_curCarte().getNPC(id);
			if(id == 0 || npc == null)
			{
				String str = "Npc GUID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int exC = npc.get_cellID();
			//on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), id);
			//on change sa position/orientation
			npc.setCellID(_perso.get_curCell().getID());
			npc.setOrientation(_perso.get_orientation());
			//on envoie la modif
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(),npc);
			String str = "Le PNJ a ete deplace";
			if(_perso.get_orientation() == 0
			|| _perso.get_orientation() == 2
			|| _perso.get_orientation() == 4
			|| _perso.get_orientation() == 6)
				str += " mais est devenu invisible (orientation diagonale invalide).";
			if(SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(),exC)
			&& SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(),npc.get_template().get_id(),_perso.get_curCell().getID(),_perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("DELTRIGGER"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int cellID = -1;
			try
			{
				cellID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(cellID == -1 || _perso.get_curCarte().getCase(cellID) == null)
			{
				String str = "CellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			_perso.get_curCarte().getCase(cellID).clearOnCellAction();
			boolean success = SQLManager.REMOVE_TRIGGER(_perso.get_curCarte().get_id(),cellID);
			String str = "";
			if(success)	str = "Le trigger a ete retire";
			else 		str = "Le trigger n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDTRIGGER"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int actionID = -1;
			String args = "",cond = "";
			try
			{
				actionID = Integer.parseInt(infos[1]);
				args = infos[2];
				cond = infos[3];
			}catch(Exception e){};
			if(args.equals("") || actionID <= -3)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			_perso.get_curCell().addOnCellStopAction(actionID,args, cond);
			boolean success = SQLManager.SAVE_TRIGGER(_perso.get_curCarte().get_id(),_perso.get_curCell().getID(),actionID,1,args,cond);
			String str = "";
			if(success)	str = "Le trigger a ete ajoute";
			else 		str = "Le trigger n'a pas ete ajoute";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("TELEPORT"))
		{
			int mapID = -1;
			int cellID = -1;
			try
			{
				mapID = Integer.parseInt(infos[1]);
				cellID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(mapID == -1 || cellID == -1 || World.getCarte(mapID) == null)
			{
				String str = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(World.getCarte(mapID).getCase(cellID) == null)
			{
				String str = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 3)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if(target == null  || target.get_fight() != null)
				{
					String str = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("DELNPCITEM"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int npcGUID = 0;
			int itmID = -1;
			try
			{
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_tmpl npc =  _perso.get_curCarte().getNPC(npcGUID).get_template();
			if(npcGUID == 0 || itmID == -1 || npc == null)
			{
				String str = "NpcGUID ou itmID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			
			String str = "";
			if(npc.delItemVendor(itmID))str = "L'objet a ete retire";
			else str = "L'objet n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("ADDNPCITEM"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int npcGUID = 0;
			int itmID = -1;
			try
			{
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_tmpl npc =  _perso.get_curCarte().getNPC(npcGUID).get_template();
			ObjTemplate item =  World.getObjTemplate(itmID);
			if(npcGUID == 0 || itmID == -1 || npc == null || item == null)
			{
				String str = "NpcGUID ou itmID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			
			String str = "";
			if(npc.addItemVendor(item))str = "L'objet a ete rajoute";
			else str = "L'objet n'a pas ete rajoute";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("GOMAP"))
		{
			int mapX = 0;
			int mapY = 0;
			int cellID = 0;
			int contID = 0;//Par défaut Amakna
			try
			{
				mapX = Integer.parseInt(infos[1]);
				mapY = Integer.parseInt(infos[2]);
				cellID = Integer.parseInt(infos[3]);
				contID = Integer.parseInt(infos[4]);
			}catch(Exception e){};
			Carte map = World.getCarteByPosAndCont(mapX,mapY,contID);
			if(map == null)
			{
				String str = "Position ou continent invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(map.getCase(cellID) == null)
			{
				String str = "CellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 5)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[5]);
				if(target == null || target.get_fight() != null)
				{
					String str = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
				if(target.get_fight() != null)
				{
					String str = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(map.get_id(), cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("ADDMOUNTPARK"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int size = -1;
			int owner = -2;
			int price = -1;
			try
			{
				size = Integer.parseInt(infos[1]);
				owner = Integer.parseInt(infos[2]);
				price = Integer.parseInt(infos[3]);
				if(price > 20000000)price = 20000000;
				if(price <0)price = 0;
			}catch(Exception e){};
			if(size == -1 || owner == -2 || price == -1 || _perso.get_curCarte().getMountPark() != null)
			{
				String str = "Infos invalides ou map deja config.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			MountPark MP = new MountPark(owner, _perso.get_curCarte(), size, "", -1, price);
			_perso.get_curCarte().setMountPark(MP);
			SQLManager.SAVE_MOUNTPARK(MP);
			String str = "L'enclos a ete config. avec succes";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ITEM") || command.equalsIgnoreCase("!getitem"))
		{
			boolean isOffiCmd = command.equalsIgnoreCase("!getitem");
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int tID = 0;
			try
			{
				tID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(tID == 0)
			{
				String mess = "Le template "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			int qua = 1;
			if(infos.length == 3)//Si une quantité est spécifiée
			{
				try
				{
					qua = Integer.parseInt(infos[2]);
				}catch(Exception e){};
			}
			boolean useMax = false;
			if(infos.length == 4 && !isOffiCmd)//Si un jet est spécifiée
			{
				if(infos[3].equalsIgnoreCase("MAX"))useMax = true;
			}
			ObjTemplate t = World.getObjTemplate(tID);
			if(t == null)
			{
				String mess = "Le template "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(qua <1)qua =1;
			Objet obj = t.createNewItem(qua,useMax);
			if(_perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
				World.addObjet(obj,true);
			_perso.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Ajouté par un MJ");
			
			String str = "Creation de l'item "+tID+" reussie";
			if(useMax) str += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("ITEMSET"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int tID = 0;
			try
			{
				tID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			ItemSet IS = World.getItemSet(tID);
			if(tID == 0 || IS == null)
			{
				String mess = "La panoplie "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			boolean useMax = false;
			if(infos.length == 3)useMax = infos[2].equals("MAX");//Si un jet est spécifiée

			
			for(ObjTemplate t : IS.getItemTemplates())
			{
				Objet obj = t.createNewItem(1,useMax);
				if(_perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
					World.addObjet(obj,true);
				_perso.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Ajouté par un MJ");
			}
			String str = "Creation de la panoplie "+tID+" reussie";
			if(useMax) str += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("LEVEL"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
				if(count < 1)	count = 1;
				if(count > 200)	count = 200;
				Personnage perso = _perso;
				if(infos.length == 3)//Si le nom du perso est spécifier
				{
					String name = infos[2];
					perso = World.getPersoByName(name);
					if(perso == null)
						perso = _perso;
				}
				if(perso.get_lvl() < count)
				{
					while(perso.get_lvl() < count)
					{
						perso.levelUp(false,true);
					}
					if(perso.isOnline())
					{
						SocketManager.GAME_SEND_NEW_LVL_PACKET(perso.get_compte().getGameThread().get_out(),perso.get_lvl());
						SocketManager.GAME_SEND_STATS_PACKET(perso);
					}
				}
				String mess = "Vous avez fixer le niveau de "+perso.get_name()+" a "+count;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
		}
		else
		if(command.equalsIgnoreCase("PDVPER"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
				if(count < 0)	count = 0;
				if(count > 100)	count = 100;
				Personnage perso = _perso;
				if(infos.length == 3)//Si le nom du perso est spécifié
				{
					String name = infos[2];
					perso = World.getPersoByName(name);
					if(perso == null)
						perso = _perso;
				}
				int newPDV = perso.get_PDVMAX() * count / 100;
				perso.set_PDV(newPDV);
				if(perso.isOnline())
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				String mess = "Vous avez fixer le pourcentage de pdv de "+perso.get_name()+" a "+count;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
		}else
		if(command.equalsIgnoreCase("KAMAS"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			long count = 0;
			try
			{
				count = Long.parseLong(infos[1]);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
			if(count == 0)return;
			
			Personnage perso = _perso;
			if(infos.length == 3)//Si le nom du perso est spécifier
			{
				String name = infos[2];
				perso = World.getPersoByName(name);
				if(perso == null)
					perso = _perso;
			}
			long curKamas = perso.get_kamas();
			long newKamas = curKamas + count;
			if(newKamas <0) newKamas = 0;
			if(newKamas > Long.MAX_VALUE) newKamas = Long.MAX_VALUE;
			perso.set_kamas(newKamas);
			perso.kamasLog(count+"", "Commande MJ executé par '" + _perso.get_name() + "'");
			
			if(perso.isOnline())
				SocketManager.GAME_SEND_STATS_PACKET(perso);
			String mess = "Vous avez ";
			mess += (count<0?"retirer":"ajouter")+" ";
			mess += Math.abs(count)+" kamas a "+perso.get_name();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else if (command.equalsIgnoreCase("DOACTION"))
		{
			//DOACTION NAME TYPE ARGS COND
			if(infos.length < 4)
			{
				String mess = "Nombre d'argument de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			int type = -100;
			String args = "",cond = "";
			Personnage perso = _perso;
			try
			{
				perso = World.getPersoByName(infos[1]);
				if(perso == null)perso = _perso;
				type = Integer.parseInt(infos[2]);
				args = infos[3];
				if(infos.length >4)
				cond = infos[4];
			}catch(Exception e)
			{
				String mess = "Arguments de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			(new Action(type,args,cond)).apply(perso,-1);
			String mess = "Action effectuee !";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}
		/*MARTHIEUBEAN*/
		else if (command.equalsIgnoreCase("RUNFILE"))
		{
			if(infos.length < 2)	//Si le nombre de paramètre est < 2, donc qu'il n'y a que la commande d'écrit
			{
				String mess = "Nombre d'argument de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			String fileName = infos[1];	//Stockage du nom de fichier dans une variable
			BufferedReader fichier = null;
			
			try
			{
				
				fichier = new BufferedReader(new FileReader("RunScript/"+fileName+(!fileName.contains(".")?".run":"")));	//Ouverture d'un flux de lecture sur le fichier demandé
			}catch(FileNotFoundException e)	//Erreur survient lorsque le fichier est introuvable
			{
				String mess = "Le fichier \""+fileName+(!fileName.contains(".")?".run":"")+"\" n'existe pas!";	//Envoie d'un message expliquant l'erreur
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			String line = "";
			
			try
			{
				while ((line=fichier.readLine())!=null)
				{
					if(line.contains("#") || line.length() <= 0)	//Si la ligne est un commentaire ou si elle est vide
					{
						continue;
					}
					else if(line.charAt(0) == '>') //Si la ligne est une ligne d'affichage
					{
						SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,line.replaceFirst(">",""));	//Envoie du message sans le '>'
					}
					else	//Finalement, si le packet est une commande normal, ajout de deux caractère de bourrage et on execute la fonction d'execution de commande. Ce qui permet d'executer un fichier dans un fichier par exemple
					{
						line = line.replaceAll("%me%",_perso.get_name());	//Remplace tout les %me% par le nom du perso en cours
						
						for (int i = 1; i <= 9; i++)
						{
							try
							{
								line = line.replaceAll("param"+i,infos[i+1]);
							}catch(ArrayIndexOutOfBoundsException e) //Si le paramètre n'existe pas, la boucle doit s'arrêter
							{
								break;
							}
						}
						consoleCommand("XX"+line);
					}
				}
				fichier.close();
			}catch(Exception e)
			{
				String errMsg = "Fichier RUN illisible";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,errMsg);
				System.out.println (e.getMessage());
				return;
			}
		}else if (command.equalsIgnoreCase("LISTFILE"))
		{
			String[] listFichier = null;
			String sortie = "";
			File repertoire;
			try
			{
				repertoire = new File("RunScript");
				listFichier = repertoire.list();
				
				sortie += "==================\n";
				for (int i = 0; i < listFichier.length; i++)
				{
					if(listFichier[i].endsWith(".run"))
					{
						sortie += listFichier[i] + "\n";
					}
				}
				sortie += "==================";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,sortie);
			}catch(Exception e)
			{
				String errMsg = "Erreur lors du listage des fichiers run";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,errMsg);
				System.out.println (e.getMessage());
				return;
			}
		}else if (command.equalsIgnoreCase("HELPFILE"))
		{
			if(infos.length < 2)	//Si le nombre de paramètre est < 2, donc qu'il n'y a que la commande d'écrit
			{
				String mess = "Nombre d'argument de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			String fileName = infos[1];	//Stockage du nom de fichier dans une variable
			BufferedReader fichier = null;
			
			try
			{
				
				fichier = new BufferedReader(new FileReader("RunScript/"+fileName+".help"));	//Ouverture d'un flux de lecture sur le fichier demandé
			}catch(FileNotFoundException e)	//Erreur survient lorsque le fichier est introuvable
			{
				String mess = "Le fichier \""+fileName+".help\" n'existe pas!";	//Envoie d'un message expliquant l'erreur
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			String line = "";
			String sortie = "";
			try
			{
				sortie += "======================\n";
				while ((line=fichier.readLine())!=null)
				{
					sortie += line+"\n";
				}
				sortie += "======================";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,sortie);
				fichier.close();
			}catch(Exception e)
			{
				String errMsg = "Fichier d'aide illisible";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,errMsg);
				System.out.println (e.getMessage());
				return;
			}
		}else if(command.equalsIgnoreCase("SET"))	//SET INTELLIGENCE 500 *nomPerso*
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int count = -1;
			try
			{
				count = Integer.parseInt(infos[2]);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			}
			if(count < 0)count = 0;
			
			Personnage perso = _perso;
			if(infos.length == 4 && !infos[3].equalsIgnoreCase(_perso.get_name()))//Si le nom du perso est spécifier et que ce n'est pas son perso.
			{
				String name = infos[3];
				perso = World.getPersoByName(name);
				if(perso == null)
					perso = _perso;
			}
			
			String mess = "Vous avez définit ";
			String stats = infos[1];
			
			if(stats.equalsIgnoreCase("Intelligence"))
			{
				perso.setStat(Constants.STATS_ADD_INTE,count);
				mess+="l'intelligence";
			}else if(stats.equalsIgnoreCase("Force"))
			{
				perso.setStat(Constants.STATS_ADD_FORC,count);
				mess+="la force";
			}else if(stats.equalsIgnoreCase("Agilite"))
			{
				perso.setStat(Constants.STATS_ADD_AGIL,count);
				mess+="l'agilité";
			}else if(stats.equalsIgnoreCase("Chance"))
			{
				perso.setStat(Constants.STATS_ADD_CHAN,count);
				mess+="la chance";
			}else if(stats.equalsIgnoreCase("Sagesse"))
			{
				perso.setStat(Constants.STATS_ADD_SAGE,count);
				mess+="la sagesse";
			}else if(stats.equalsIgnoreCase("Vitalite"))
			{
				perso.setStat(Constants.STATS_ADD_VITA,count);
				mess+="la vitalité";
			}else
			{
				mess = "Stats \""+stats+"\" invalide!";
				count = -1;
				return;
			}
			if(perso.isOnline())
				SocketManager.GAME_SEND_STATS_PACKET(perso);
				
			if(count >= 0)
				mess+=" de " + perso.get_name() + " à " + count;
				
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			return;
		}
		/*FIN*/
		else
		{
			String mess = "Commande non reconnue";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}
	}
	
	/**
	 * 
	 * @param command La commande reçu par le GameThread /!\Sans le '.' au début/!\.
	 * @param perso Le personnage qui à entré la commande.
	 */
	public void dotCommand(String command)
	{
		//Retour au point de sauvegarde
		if(command.length() > 5 && command.substring(0, 5).equalsIgnoreCase("start"))
		{
			if(_perso.get_fight() != null)return;
			_perso.warpToSavePos();
			return;
		}
		
		//Zone shop
		else if(command.length() > 5 && command.substring(0, 4).equalsIgnoreCase("shop"))
		{
			if(_perso.get_fight() != null
			|| Ancestra.CONFIG_SHOP_MAPID == 0
			|| Ancestra.CONFIG_SHOP_CELLID == 0)return;
			
			_perso.teleport(Ancestra.CONFIG_SHOP_MAPID, Ancestra.CONFIG_SHOP_CELLID);
			return;
		}
	}
	
	private void fullHdv(int ofEachTemplate)
	{
		SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Démarrage du remplissage!");
		
		TreeMap<Integer,ObjTemplate> template =(TreeMap<Integer, ObjTemplate>) World.getObjTemplates();
		
		Objet objet = null;
		HdvEntry entry = null;
		byte amount = 0;
		int hdv = 0;
		
		int lastSend = 0;
		long time1 = System.currentTimeMillis(); //TIME
		for (ObjTemplate curTemp : template.values()) //Boucler dans les template
		{
			try
			{
				if(Ancestra.NOTINHDV.contains(curTemp.getID()))
					continue;
				for (int j = 0; j < ofEachTemplate; j++) //Ajouter plusieur fois le template
				{
					if(curTemp.getType() == 85)
						break;
					objet = curTemp.createNewItem(1, false);
					hdv = getHdv(objet.getTemplate().getType());
					
					if(hdv < 0)
						break;
						
					amount = (byte) Formulas.getRandomValue(1, 3);
					
					
					entry = new HdvEntry(calculPrice(objet,amount), amount, -1, objet);
					objet.setQuantity(entry.getAmount(true));
					
					
					World.getHdv(hdv).addEntry(entry);
					World.addObjet(objet, false);
				}
			}catch (Exception e)
			{
				continue;
			}
			
			if((System.currentTimeMillis() - time1)/1000 != lastSend
				&& (System.currentTimeMillis() - time1)/1000 % 3 == 0)
			{
				lastSend = (int) ((System.currentTimeMillis() - time1)/1000);
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,(System.currentTimeMillis() - time1)/1000 + "sec Template: "+curTemp.getID());
			}
		}
		SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Remplissage fini en "+(System.currentTimeMillis() - time1) + "ms");
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("HDV remplis!",Ancestra.CONFIG_MOTD_COLOR);
	}
	private int getHdv(int type)
	{
		//TODO ajouter les HDV Astrub et Brâkmar
		switch(type)
		{
			case 12:
			case 14: 
			case 26: 
			case 43: 
			case 44: 
			case 45: 
			case 66: 
			case 70: 
			case 71: 
			case 86:
				return 4271;
			case 1:
			case 9:
				return 4216;
			case 18: 
			case 72: 
			case 77: 
			case 90: 
			case 97: 
			case 113: 
			case 116:
				return 8759;
			case 63:
			case 64:
			case 69:
				return 4287;
			case 33:
			case 42:
				return 2221;
			case 84: 
			case 93: 
			case 112: 
			case 114:
				return 4232;
			case 38: 
			case 95: 
			case 96: 
			case 98: 
			case 108:
				return 4178;
			case 10:
			case 11:
				return 4183;
			case 13: 
			case 25: 
			case 73: 
			case 75: 
			case 76:
				return 8760;
			case 5: 
			case 6: 
			case 7: 
			case 8: 
			case 19: 
			case 20: 
			case 21: 
			case 22:
				return 4098;
			case 39: 
			case 40: 
			case 50: 
			case 51: 
			case 88:
				return 4179;
			case 87:
				return 6159;
			case 34:
			case 52:
			case 60:
				return 4299;
			case 41:
			case 49:
			case 62:
				return 4247;
			case 15: 
			case 35: 
			case 36: 
			case 46: 
			case 47: 
			case 48: 
			case 53: 
			case 54: 
			case 55: 
			case 56: 
			case 57: 
			case 58: 
			case 59: 
			case 65: 
			case 68: 
			case 103: 
			case 104: 
			case 105: 
			case 106: 
			case 107: 
			case 109: 
			case 110: 
			case 111:
				return 4262;
			case 78:
				return 8757;
			case 2:
			case 3:
			case 4:
				return 4174;
			case 16:
			case 17:
			case 81:
				return 4172;
			case 83:
				return 10129;
			case 82:
				return 8039;
			default:
				return -1;
		}
	}
	private int calculPrice(Objet obj, int logAmount)
	{
		int amount = (byte)(Math.pow(10,(double)logAmount) / 10);
		int stats = 0;
		
		for(int curStat : obj.getStats().getMap().values())
		{
			stats += curStat;
		}
		if(stats > 0)
			return (int) (((Math.cbrt(stats) * Math.pow(obj.getTemplate().getLevel(), 2)) * 10 + Formulas.getRandomValue(1, obj.getTemplate().getLevel()*100)) * amount);
		else
			return (int) ((Math.pow(obj.getTemplate().getLevel(),2) * 10 + Formulas.getRandomValue(1, obj.getTemplate().getLevel()*100))*amount);
	}
}
