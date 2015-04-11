package common;

import game.GameServer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import common.World.ItemSet;

import objects.Carte;
import objects.Dragodinde;
import objects.Guild;
import objects.Objet;
import objects.Personnage;
import objects.Fight;
import objects.Carte.Case;
import objects.Carte.InteractiveObject;
import objects.Carte.MountPark;
import objects.Fight.Fighter;
import objects.Guild.GuildMember;
import objects.Metier.StatsMetier;
import objects.Monstre.MobGroup;
import objects.NPC_tmpl.NPC;
import objects.Objet.ObjTemplate;
import objects.Personnage.Group;
import objects.Hdv;
import objects.Hdv.*;

import realm.RealmServer;

public class SocketManager {

	public static String REALM_SEND_HC_PACKET(PrintWriter out)
	{
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
        Random rand = new Random();
        String hashkey = "";
        for (int i=0; i<32; i++)
        {
               hashkey = hashkey+ alphabet.charAt(rand.nextInt(alphabet.length()));
        }
        String packet = "HC"+hashkey;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Realm: Send>>"+packet);
		return hashkey;
	}
	
	public static void REALM_SEND_REQUIRED_VERSION(PrintWriter out)
	{
		String packet = "AlEv" +Constants.CLIENT_VERSION;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);
	}
	
	public static void REALM_SEND_LOGIN_ERROR(PrintWriter out)
	{
		String packet = "AlEf";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);
	}
	
	public static void send(Personnage p, String packet)
	{
		if(p.get_compte() == null)return;
		if(p.get_compte().getGameThread() == null)return;
		PrintWriter out = p.get_compte().getGameThread().get_out();
		if(out != null && !packet.equals("") && !packet.equals(""+(char)0x00))
		{
			packet = CryptManager.toUtf(packet);	//MARTHIEUBEAN
			out.print((packet)+(char)0x00);
			
			out.flush();
		}
	}
	
	public static void send(PrintWriter out, String packet)
	{
		if(out != null && !packet.equals("") && !packet.equals(""+(char)0x00))
		{
			packet = CryptManager.toUtf(packet);	//MARTHIEUBEAN
			out.print(packet+(char)0x00);
			out.flush();
		}
	}

	public static void MULTI_SEND_Af_PACKET(PrintWriter out,int position, int totalAbo, int totalNonAbo, int subscribe,
			int queueID)
	{
		String packet = "Af"+position+"|"+totalAbo+"|"+totalNonAbo+"|"+subscribe+"|"+queueID;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Serv: Send>>"+packet);
	}

	public static void REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS(PrintWriter out,
			String pseudo, int level, String question)
	{
		String packet = "Ad"+pseudo+(char)0x00;
		packet += "Ac0"+(char)0x00;
		//AH[ID];[State];[Completion];[CanLog]
		packet += "AH1;1;110;1"+(char)0x00;
		packet += "AlK"+level+(char)0x00;
		packet += "AQ"+question.replace(" ", "+");
		
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);
	}

	public static void REALM_SEND_BANNED(PrintWriter out)
	{
		String packet = "AlEb";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);
	}

	public static void REALM_SEND_ALREADY_CONNECTED(PrintWriter out)
	{
		String packet = "AlEc";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);	
	}

	public static void REALM_SEND_POLICY_FILE(PrintWriter out)
	{
		String packet = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +   
    		"<cross-domain-policy>"+  
    	    "<allow-access-from domain=\"*\" to-ports=\"*\" secure=\"false\" />"+  
    	    "<site-control permitted-cross-domain-policies=\"master-only\" />"+  
    	    "</cross-domain-policy>";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void REALM_SEND_PERSO_LIST(PrintWriter out, int number)
	{
		String packet = "AxK31536000000";
		if(number>0)
			packet+= "|1," + number;
		
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);	
	}
	
	public static void REALM_SEND_GAME_SERVER_IP(PrintWriter out,int guid,boolean isHost)
	{
		String packet = "A";
		if(Ancestra.CONFIG_USE_IP)
		{
			String ip = Ancestra.CONFIG_IP_LOOPBACK && isHost?CryptManager.CryptIP("127.0.0.1")+CryptManager.CryptPort(Ancestra.CONFIG_GAME_PORT):Ancestra.GAMESERVER_IP;
			packet += "XK"+ip+guid;
		}else
		{
			String ip = Ancestra.CONFIG_IP_LOOPBACK && isHost?"127.0.0.1":Ancestra.IP;
			packet += "YK"+ip+":"+Ancestra.CONFIG_GAME_PORT+";"+guid;
		}
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			RealmServer.addToSockLog("Conn: Send>>"+packet);
	}
	
	public static void GAME_SEND_HELLOGAME_PACKET(PrintWriter out)
	{
		String packet = "HG";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_ATTRIBUTE_FAILED(PrintWriter out)
	{
		String packet = "ATE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_ATTRIBUTE_SUCCESS(PrintWriter out)
	{
		String packet = "ATK0";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_AV0(PrintWriter out)
	{
		String packet = "AV0";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_HIDE_GENERATE_NAME(PrintWriter out)
	{
		String packet = "APE2";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_PERSO_LIST(PrintWriter out,
			Map<Integer, Personnage> persos)
	{
		String packet = "ALK31536000000|"+persos.size();
		for(Entry<Integer,Personnage > entry : persos.entrySet())
		{
			packet += entry.getValue().parseALK();
			
		}
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
		
	}

	public static void GAME_SEND_NAME_ALREADY_EXIST(PrintWriter out)
	{
		String packet = "AAEa";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
		
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_CREATE_PERSO_FULL(PrintWriter out)
	{
		String packet = "AAEf";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_CREATE_OK(PrintWriter out)
	{
		String packet = "AAK";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_DELETE_PERSO_FAILED(PrintWriter out)
	{
		String packet = "ADE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_CREATE_FAILED(PrintWriter out)
	{
		String packet = "AAEF";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);

	}

	public static void GAME_SEND_PERSO_SELECTION_FAILED(PrintWriter out)
	{
		String packet = "ASE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_STATS_PACKET(Personnage perso)
	{
		String packet = perso.getAsPacket();
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_Rx_PACKET(Personnage out)
	{
		String packet = "Rx"+out.getMountXpGive();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_Rn_PACKET(Personnage out,String name)
	{
		String packet = "Rn"+name;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_Re_PACKET(Personnage out,String sign,Dragodinde DD)
	{
		String packet = "Re"+sign;
		if(sign.equals("+"))packet += DD.parse();
		
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_ASK(PrintWriter out,Personnage perso)
	{
		String packet = "ASK|"+
		perso.get_GUID()+"|"+
		perso.get_name()+"|"+
		perso.get_lvl()+"|"+
		perso.get_classe()+"|"+
		perso.get_sexe()+"|"+
		perso.get_gfxID()+"|"+
		(perso.get_color1()==-1?"-1":Integer.toHexString(perso.get_color1()))+"|"+
		(perso.get_color2()==-1?"-1":Integer.toHexString(perso.get_color2()))+"|"+
		(perso.get_color3()==-1?"-1":Integer.toHexString(perso.get_color3()))+"|"+
		perso.parseItemToASK();
		
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_ALIGNEMENT(PrintWriter out,int alliID)
	{
		String packet = "ZS"+alliID;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_ADD_CANAL(PrintWriter out, String chans)
	{
		String packet = "cC+"+chans;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_ZONE_ALLIGN_STATUT(PrintWriter out)
	{
		String packet = "al|"+World.getSousZoneStateString();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_SEESPELL_OPTION(PrintWriter out, boolean spells)
	{
		String packet = "SLo"+(spells?"+":"-");
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_RESTRICTIONS(PrintWriter out)
	{
		String packet =  "AR6bk";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_Ow_PACKET(Personnage perso)
	{
		String packet =  "Ow"+perso.getPodUsed()+"|"+perso.getMaxPod();
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_OT_PACKET(PrintWriter out, int id)
	{
		String packet =  "OT";
		if(id > 0) packet += id;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_SEE_FRIEND_CONNEXION(PrintWriter out,boolean see)
	{
		String packet = "FO"+(see?"+":"-");
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_GAME_CREATE(PrintWriter out, String _name)
	{
		String packet = "GCK|1|"+_name;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_SERVER_HOUR(PrintWriter out)
	{
		String packet = GameServer.getServerTime();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_SERVER_DATE(PrintWriter out)
	{
		String packet = GameServer.getServerDate();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_MAPDATA(PrintWriter out, int id, String date,String key)
	{
		String packet = "GDM|"+id+"|"+date+"|"+key;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_GDK_PACKET(PrintWriter out)
	{
		String packet = "GDK";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_MAP_MOBS_GMS_PACKETS(PrintWriter out, Carte carte)
	{
		String packet = carte.getMobGroupGMsPackets();
		if(packet.equals(""))return;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_OBJECTS_GDS_PACKETS(PrintWriter out, Carte carte)
	{
		String packet = carte.getObjectsGDsPackets();
		if(packet.equals(""))return;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_NPCS_GMS_PACKETS(PrintWriter out, Carte carte)
	{
		String packet = carte.getNpcsGMsPackets();
		if(packet.equals(""))return;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_GMS_PACKETS(PrintWriter out, Carte carte)
	{
		String packet = carte.getGMsPackets();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_ERASE_ON_MAP_TO_MAP(Carte map,int guid)
	{
		String packet = "GM|-"+guid;
		for(int z=0;z < map.getPersos().size();z++)
		{
			if(map.getPersos().get(z).get_compte().getGameThread() == null)continue;
			send(map.getPersos().get(z).get_compte().getGameThread().get_out(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map "+map.get_id()+": Send>>"+packet);
	}

	public static void GAME_SEND_ADD_PLAYER_TO_MAP(Carte map, Personnage perso)
	{
		String packet = "GM|+"+perso.parseToGM();
		for(Personnage P : map.getPersos())send(P,packet);	
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map "+map.get_id()+": Send>>"+packet);
	}

	public static void GAME_SEND_DUEL_Y_AWAY(PrintWriter out, int guid)
	{
		String packet = "GA;903;"+guid+";o";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_DUEL_E_AWAY(PrintWriter out, int guid)
	{
		String packet = "GA;903;"+guid+";z";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_MAP_NEW_DUEL_TO_MAP(Carte map,int guid, int guid2)
	{
		String packet = "GA;900;"+guid+";"+guid2;
		for(Personnage P : map.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map "+map.get_id()+": Send>>"+packet);
	}
	
	public static void GAME_SEND_CANCEL_DUEL_TO_MAP(Carte map, int guid,int guid2)
	{
		String packet = "GA;902;"+guid+";"+guid2;
		for(Personnage P : map.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_START_DUEL_TO_MAP(Carte map,int guid, int guid2)
	{
		String packet = "GA;901;"+guid+";"+guid2;
		for(Personnage P : map.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT(PrintWriter out,Carte map)
	{
		String packet = "fC"+map.getNbrFight();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight fight, int teams,int state, int cancelBtn, int duel, int spec, int time, int type)
	{
		String packet = "GJK"+state+"|"+cancelBtn+"|"+duel+"|"+spec+"|"+time+"|"+type;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight fight,int teams, String places, int team)
	{
		String packet = "GP"+places+"|"+team;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(Carte map)
	{
		String packet = "fC"+map.getNbrFight();
		for(int z=0;z < map.getPersos().size();z++)
		{
			send(map.getPersos().get(z),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(Carte map,int arg1, int guid1,int guid2,int cell1,String str1,int cell2,String str2)
	{
		String packet = "Gc+"+guid1+";"+arg1+"|"+guid1+";"+cell1+";"+str1+"|"+guid2+";"+cell2+ ";"+str2;
		for(int z=0;z < map.getPersos().size();z++)
		{
			send(map.getPersos().get(z),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(Carte map, int guid)
	{
		String packet = "Gc-"+guid;
		for(int z=0;z < map.getPersos().size();z++)
		{
			send(map.getPersos().get(z),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(Carte map,int teamID,Fighter perso)
	{
		String packet = "Gt"+teamID+"|+"+perso.getGUID()+";"+perso.getPacketsName()+";"+perso.get_lvl();
		for(int z=0;z < map.getPersos().size();z++)
		{
			send(map.getPersos().get(z),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	
	public static void GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(Carte map)
	{
		String packet = map.getMobGroupGMsPackets(); // Un par un comme sa lors du respawn :)
		for(Personnage z:map.getPersos())
			send(z,packet);
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_MOBS_GM_PACKET(Carte map, MobGroup current_Mobs)
	{
		String packet = "GM|";
		packet += current_Mobs.parseGM(); // Un par un comme sa lors du respawn :)
		for(Personnage z:map.getPersos())
			send(z,packet);
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_GMS_PACKETS(Carte map, Personnage _perso)
	{
		String packet = map.getGMsPackets();
		send(_perso, packet);
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_ON_EQUIP_ITEM(Carte map, Personnage _perso)
	{
		String packet = _perso.parseToOa();;
		
		for(Personnage z:map.getPersos())
			send(z,packet);
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(Fight fight, int teams, Carte map, int guid, int cell)
	{
		String packet = "GIC|"+guid+";"+cell+";1";
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
				send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(Carte map,char s,char option, int guid)
	{
		String packet = "Go"+s+option+guid;
		for(Personnage z:map.getPersos())
			send(z,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(Fight fight,int teams, int guid, boolean b)
	{
		String packet = "GR"+(b?"1":"0")+guid;
		if(fight.get_state() != 2)return;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			if(f.hasLeft())continue;
				send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}

	public static void GAME_SEND_GJK_PACKET(Personnage out,int state,int cancelBtn,int duel,int spec,int time,int unknown)
	{
		String packet = "GJK"+state+"|"+cancelBtn+"|"+duel+"|"+spec+"|"+time+"|"+unknown;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_PLACES_PACKET(PrintWriter out,String places, int team)
	{
		String packet = "GP"+places+"|"+team;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_Im_PACKET(Personnage out,String str)
	{
		String packet = "Im"+str;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_ILS_PACKET(Personnage out,int i)
	{
		String packet = "ILS"+i;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}public static void GAME_SEND_ILF_PACKET(Personnage P,int i)
	{
		String packet = "ILF"+i;
		send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_Im_PACKET_TO_MAP(Carte map, String id)
	{
		String packet = "Im"+id;
		for(Personnage z:map.getPersos())
			send(z,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	public static void GAME_SEND_eUK_PACKET_TO_MAP(Carte map, int guid, int emote)
	{
		String packet = "eUK"+guid+"|"+emote;
		for(Personnage z:map.getPersos())
			send(z,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	public static void GAME_SEND_Im_PACKET_TO_FIGHT(Fight fight,int teams, String id)
	{
		String packet = "Im"+id;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_MESSAGE(Personnage out,String mess, String color)
	{
		String packet = "cs<font color='#"+color+"'>"+mess+"</font>";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_MESSAGE_TO_MAP(Carte map,String mess, String color)
	{
		String packet = "cs<font color='#"+color+"'>"+mess+"</font>";
		for(Personnage perso : map.getPersos())
		{
			send(perso,packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_GA903_ERROR_PACKET(PrintWriter out, char c,int guid)
	{
		String packet = "GA;903;"+guid+";"+c;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_GIC_PACKETS_TO_FIGHT(Fight fight,int teams)
	{
		String packet = "GIC|";
		for(Fighter p : fight.getFighters(3))
		{
			if(p.get_fightCell() == null)continue;
			packet += p.getGUID()+";"+p.get_fightCell().getID()+";1|";
		}
		for(Fighter perso:fight.getFighters(teams))
		{
			if(perso.hasLeft())continue;
			if(perso.getPersonnage() == null || !perso.getPersonnage().isOnline())continue;
			send(perso.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	public static void GAME_SEND_GIC_PACKET_TO_FIGHT(Fight fight,int teams,Fighter f)
	{
		String packet = "GIC|"+f.getGUID()+";"+f.get_fightCell().getID()+";1|";

		for(Fighter perso:fight.getFighters(teams))
		{
			if(perso.hasLeft())continue;
			if(perso.getPersonnage() == null || !perso.getPersonnage().isOnline())continue;
			send(perso.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	public static void GAME_SEND_GS_PACKET_TO_FIGHT(Fight fight,int teams)
	{
		String packet = "GS";
		for(Fighter f:fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			f.initBuffStats();
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	public static void GAME_SEND_GS_PACKET(Personnage out)
	{
		String packet = "GS";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	public static void GAME_SEND_GTL_PACKET_TO_FIGHT(Fight fight, int teams)
	{
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),fight.getGTL());
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+fight.getGTL());
	}
	public static void GAME_SEND_GTL_PACKET(Personnage out,Fight fight)
	{
		String packet = fight.getGTL();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	public static void GAME_SEND_GTM_PACKET_TO_FIGHT(Fight fight, int teams)
	{
		String packet = "GTM";
		for(Fighter f : fight.getFighters(3))
		{
			packet += "|"+f.getGUID()+";";
			if(f.isDead())
			{
				packet += "1";
				continue;
			}else
			packet += "0;"
			+f.getPDV()+";"
			+f.getPA()+";"
			+f.getPM()+";"
			+(f.isHide()?"-1":f.get_fightCell().getID())+";"//On envoie pas la cell d'un invisible :p
			+";"//??
			+f.getPDVMAX();
		}
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}

	public static void GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(Fight fight,int teams, int guid, int time)
	{
		String packet = "GTS"+guid+"|"+time;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	public static void GAME_SEND_GAMETURNSTART_PACKET(Personnage P,int guid, int time)
	{
		String packet = "GTS"+guid+"|"+time;
		send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	public static void GAME_SEND_GV_PACKET(Personnage P)
	{
		String packet = "GV";
		send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	public static void GAME_SEND_PONG(PrintWriter out)
	{
		String packet = "pong";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_QPONG(PrintWriter out)
	{
		String packet = "qpong";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_GAS_PACKET_TO_FIGHT(Fight fight,int teams, int guid)
	{
		String packet = "GAS"+guid;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	
	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight,int teams, int actionID,String s1, String s2)
	{
		String packet = "GA;"+actionID+";"+s1;
		if(!s2.equals(""))
			packet+=";"+s2;
		
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight("+fight.getFighters(teams).size()+") : Send>>"+packet);
	}
	
	public static void GAME_SEND_GA_PACKET(PrintWriter out, String actionID,String s0,String s1, String s2)
	{
		String packet = "GA"+actionID+";"+s0;
		if(!s1.equals(""))
			packet += ";"+s1;
		if(!s2.equals(""))
			packet+=";"+s2;
		
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight,int teams,int gameActionID,String s1, String s2,String s3)
	{
		String packet = "GA"+gameActionID+";"+s1+";"+s2+";"+s3;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	
	public static void GAME_SEND_GAMEACTION_TO_FIGHT(Fight fight, int teams,String packet)
	{
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}

	public static void GAME_SEND_GAF_PACKET_TO_FIGHT(Fight fight, int teams, int i1,int guid)
	{
		String packet = "GAF"+i1+"|"+guid;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}

	public static void GAME_SEND_BN(Personnage out)
	{
		String packet = "BN";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_BN(PrintWriter out)
	{
		String packet = "BN";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(Fight fight,int teams, int guid)
	{
		String packet = "GTF"+guid;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}

	public static void GAME_SEND_GTR_PACKET_TO_FIGHT(Fight fight, int teams,int guid)
	{
		String packet = "GTR"+guid;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}

	public static void GAME_SEND_EMOTICONE_TO_MAP(Carte map,int guid, int id)
	{
		String packet = "cS"+guid+"|"+id;
		for(Personnage perso : map.getPersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_SPELL_UPGRADE_FAILED(PrintWriter _out)
	{
		String packet = "SUE";
		send(_out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_SPELL_UPGRADE_SUCCED(PrintWriter _out,int spellID,int level)
	{
		String packet = "SUK"+spellID+"~"+level;
		send(_out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_SPELL_LIST(Personnage perso)
	{
		String packet = perso.parseSpellList();
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(Fight fight, int teams,int guid)
	{
		String packet = "GA;103;"+guid+";"+guid;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft() || f.getPersonnage() == null)continue;
			if(f.getPersonnage().isOnline())
				send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(Fight fight, int teams, int win)
	{
		String packet = fight.GetGE(win);
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft() || f.getPersonnage() == null)continue;
			if(f.getPersonnage().isOnline())
				send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	
	public static void GAME_SEND_FIGHT_GE_PACKET(PrintWriter out,Fight fight, int win)
	{
		String packet = fight.GetGE(win);
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	
	public static void GAME_SEND_FIGHT_GIE_TO_FIGHT(Fight fight, int teams,int mType,int cible,int value,String mParam2,String mParam3,String mParam4, int turn,int spellID)
	{
		String packet = "GIE"+mType+";"+cible+";"+value+";"+mParam2+";"+mParam3+";"+mParam4+";"+turn+";"+spellID;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft() || f.getPersonnage() == null)continue;
			if(f.getPersonnage().isOnline())
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight : Send>>"+packet);
	}
	
	public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight fight, int teams,Carte map)
	{
		String packet = map.getFightersGMsPackets();
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}

	public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight fight,Carte map, Personnage _perso)
	{
		String packet = map.getFightersGMsPackets();
		send(_perso, packet);
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	
	public static void GAME_SEND_FIGHT_PLAYER_JOIN(Fight fight,int teams, Fighter _fighter) {
		String packet = _fighter.getGmPacket();
		
		for(Fighter f : fight.getFighters(teams))
		{
			if (f != _fighter)
			{
				if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
				if(f.getPersonnage() != null && f.getPersonnage().get_compte().getGameThread() != null)
					send(f.getPersonnage(),packet);
			}
		}
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	
	public static void GAME_SEND_cMK_PACKET(Personnage perso,String suffix,int guid,String name,String msg)
	{
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_FIGHT_LIST_PACKET(PrintWriter out,Carte map)
	{
		String packet = "fL";
		for(Entry<Integer,Fight> entry : map.get_fights().entrySet())
		{
			if(packet.length()>2)
			{
				packet += "|";
			}
			packet += entry.getValue().parseFightInfos();
		}
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_cMK_PACKET_TO_MAP(Carte map,String suffix,int guid,String name,String msg)
	{
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Personnage perso : map.getPersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	public static void GAME_SEND_cMK_PACKET_TO_GUILD(Guild g,String suffix,int guid,String name,String msg)
	{
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Personnage perso : g.getMembers())
		{
			if(perso == null || !perso.isOnline())continue;
			send(perso,packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Guild("+World.getOnlinePersos().size()+"): Send>>"+packet);
	}
	public static void GAME_SEND_cMK_PACKET_TO_ALL(String suffix,int guid,String name,String msg)
	{
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Personnage perso : World.getOnlinePersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: ALL("+World.getOnlinePersos().size()+"): Send>>"+packet);
	}
	public static void GAME_SEND_cMK_PACKET_TO_ADMIN(String suffix,int guid,String name,String msg)
	{
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Personnage perso : World.getOnlinePersos())if(perso.isOnline())if(perso.get_compte() != null)if(perso.get_compte().get_gmLvl()>0)
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: ALL("+World.getOnlinePersos().size()+"): Send>>"+packet);
	}
	public static void GAME_SEND_cMK_PACKET_TO_FIGHT(Fight fight,int teams,String suffix,int guid,String name,String msg)
	{
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	public static void GAME_SEND_cMK_PACKET_TO_ALIGN(byte align,int guid,String name,String msg)
	{
		String packet = "cMK!|"+guid+"|"+name+"|"+msg;	//'!' = Canal alignement
		for(Personnage perso : World.getOnlinePersos())
		{
			if(perso.get_align() != align)continue;
			send(perso,packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_GDZ_PACKET_TO_FIGHT(Fight fight,int teams,String suffix,int cell,int size,int unk)
	{
		String packet = "GDZ"+suffix+cell+";"+size+";"+unk;
		
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	
	public static void GAME_SEND_GDC_PACKET_TO_FIGHT(Fight fight,int teams,int cell)
	{
		String packet = "GDC"+cell;
		
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
			send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}
	
	public static void GAME_SEND_GA2_PACKET(PrintWriter out, int guid)
	{
		String packet = "GA;2;"+guid+";";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_CHAT_ERROR_PACKET(PrintWriter out,String name)
	{
		String packet = "cMEf"+name;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_eD_PACKET_TO_MAP(Carte map,int guid, int dir)
	{
		String packet = "eD"+guid+"|"+dir;
		for(Personnage perso : map.getPersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_ECK_PACKET(Personnage out, int type,String str)
	{
		String packet = "ECK"+type;
		if(!str.equals(""))packet += "|"+str;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);	
	}
	
	public static void GAME_SEND_ECK_PACKET(PrintWriter out, int type,String str)
	{
		String packet = "ECK"+type;
		if(!str.equals(""))packet += "|"+str;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);	
	}
	
	public static void GAME_SEND_ITEM_VENDOR_LIST_PACKET(PrintWriter out, NPC npc)
	{
		String packet = "EL"+npc.get_template().getItemVendorList();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);	
	}

	public static void GAME_SEND_EV_PACKET(PrintWriter out)
	{
		String packet = "EV";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);	
	}

	public static void GAME_SEND_DCK_PACKET(PrintWriter out, int id)
	{
		String packet = "DCK"+id;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);	
	}

	public static void GAME_SEND_QUESTION_PACKET(PrintWriter out,String str)
	{
		String packet = "DQ"+str;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_END_DIALOG_PACKET(PrintWriter out)
	{
		String packet = "DV";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_CONSOLE_MESSAGE_PACKET(PrintWriter out, String mess)
	{
		String packet = "BAT2"+mess;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_CONSOLE_MESSAGE_TO_ADMIN(String mess,int minGmLvl)
	{
		String packet = "BAT2"+mess;
		
		ArrayList<Personnage> perso = new ArrayList<Personnage>();
		perso.addAll(World.getOnlinePersos());
		
		for(Personnage p : perso)
		{
			if(!p.isOnline())continue;
			if(p.get_compte().get_gmLvl() < minGmLvl)continue;
			
			send(p,packet);
		}
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_BUY_ERROR_PACKET(PrintWriter out)
	{
		String packet = "EBE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_SELL_ERROR_PACKET(PrintWriter out)
	{
		String packet = "ESE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_BUY_OK_PACKET(PrintWriter out)
	{
		String packet = "EBK";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_OBJECT_QUANTITY_PACKET(Personnage out, Objet obj)
	{
		String packet = "OQ"+obj.getGuid()+"|"+obj.getQuantity();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_OAKO_PACKET(Personnage out, Objet obj)
	{
		String packet = "OAKO"+obj.parseItem();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_ESK_PACKEt(Personnage out)
	{
		String packet = "ESK";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_REMOVE_ITEM_PACKET(Personnage out, int guid)
	{
		String packet = "OR"+guid;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_DELETE_OBJECT_FAILED_PACKET(PrintWriter out)
	{
		String packet = "OdE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_OBJET_MOVE_PACKET(Personnage out,Objet obj)
	{
		String packet = "OM"+obj.getGuid()+"|";
		if(obj.getPosition() != Constants.ITEM_POS_NO_EQUIPED)
			packet += obj.getPosition();
		
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_EMOTICONE_TO_FIGHT(Fight fight, int teams, int guid, int id)
	{
		String packet = "cS"+guid+"|"+id;;
		for(Fighter f : fight.getFighters(teams))
		{
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().isOnline())continue;
				send(f.getPersonnage(),packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}

	public static void GAME_SEND_OAEL_PACKET(PrintWriter out)
	{
		String packet = "OAEL";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_NEW_LVL_PACKET(PrintWriter out, int lvl)
	{
		String packet = "AN"+lvl;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_MESSAGE_TO_ALL(String msg,String color)
	{
		String packet = "cs<font color='#"+color+"'>"+msg+"</font>";
		for(Personnage P : World.getOnlinePersos())
		{
			send(P,packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: ALL: Send>>"+packet);
	}

	public static void GAME_SEND_EXCHANGE_REQUEST_OK(PrintWriter out,	int guid, int guidT, int msgID)
	{
		String packet = "ERK"+guid+"|"+guidT+"|"+msgID;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_EXCHANGE_REQUEST_ERROR(PrintWriter out, char c)
	{
		String packet = "ERE"+c;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_EXCHANGE_CONFIRM_OK(PrintWriter out, int type)
	{
		String packet = "ECK"+type;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_EXCHANGE_MOVE_OK(Personnage out,char type,String signe,String s1)
	{
		String packet = "EMK"+type+signe;
		if(!s1.equals(""))
			packet += s1;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_EXCHANGE_OTHER_MOVE_OK(PrintWriter out,char type,String signe,String s1)
	{
		String packet = "EmK"+type+signe;
		if(!s1.equals(""))
			packet += s1;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_EXCHANGE_OK(PrintWriter out,boolean ok, int guid)
	{
		String packet = "EK"+(ok?"1":"0")+guid;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_EXCHANGE_VALID(PrintWriter out, char c)
	{
		String packet = "EV"+c;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_GROUP_INVITATION_ERROR(PrintWriter out, String s) {
		String packet = "PIE"+s;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_GROUP_INVITATION(PrintWriter out,String n1, String n2)
	{
		String packet = "PIK"+n1+"|"+n2;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_GROUP_CREATE(PrintWriter out, Group g)
	{
		String packet = "PCK"+g.getChief().get_name();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Groupe: Send>>"+packet);
	}

	public static void GAME_SEND_PL_PACKET(PrintWriter out, Group g)
	{
		String packet = "PL"+g.getChief().get_GUID();
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Groupe: Send>>"+packet);
	}
	
	public static void GAME_SEND_PR_PACKET(Personnage out)
	{
		String packet = "PR";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_PV_PACKET(PrintWriter out,String s)
	{
		String packet = "PV"+s;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_ALL_PM_ADD_PACKET(PrintWriter out,Group g)
	{
		String packet = "PM+";
		boolean first = true;
		for(Personnage p : g.getPersos())
		{
			if(!first) packet += "|";
			packet += p.parseToPM();
			first = false;
		}
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_PM_ADD_PACKET_TO_GROUP(Group g, Personnage p)
	{
		String packet = "PM+"+p.parseToPM();
		for(Personnage P : g.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Groupe: Send>>"+packet);
	}
	
	public static void GAME_SEND_PM_MOD_PACKET_TO_GROUP(Group g,Personnage p)
	{
		String packet = "PM~"+p.parseToPM();
		for(Personnage P : g.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Groupe: Send>>"+packet);
	}

	public static void GAME_SEND_PM_DEL_PACKET_TO_GROUP(Group g, int guid)
	{
		String packet = "PM-"+guid;
		for(Personnage P : g.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Groupe: Send>>"+packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_GROUP(Group g,String s, int guid, String name, String msg)
	{
		String packet = "cMK"+s+"|"+guid+"|"+name+"|"+msg+"|";
		for(Personnage P : g.getPersos())send(P,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Groupe: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_DETAILS(PrintWriter out, Fight fight)
	{
		if(fight == null)return;
		String packet = "fD"+fight.get_id()+"|";
		for(Fighter f : fight.getFighters(1))packet += f.getPacketsName()+"~"+f.get_lvl()+";";
		packet += "|";
		for(Fighter f : fight.getFighters(2))packet += f.getPacketsName()+"~"+f.get_lvl()+";";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_IQ_PACKET(Personnage perso, int guid,	int qua)
	{
		String packet = "IQ"+guid+"|"+qua;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_JN_PACKET(Personnage perso, int jobID,	int lvl)
	{
		String packet = "JN"+jobID+"|"+lvl;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_GDF_PACKET_TO_MAP(Carte map, Case cell)
	{
		int cellID = cell.getID();
		InteractiveObject object = cell.getObject();
		String packet = "GDF|"+cellID+";"+object.getState()+";"+(object.isInteractive()?"1":"0");
		for(Personnage perso : map.getPersos())send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	
	public static void GAME_SEND_GA_PACKET_TO_MAP(Carte map, String gameActionID, int actionID,String s1, String s2)
	{
		String packet = "GA"+gameActionID+";"+actionID+";"+s1;
		if(!s2.equals(""))packet += ";"+s2;
		
		for(Personnage perso : map.getPersos())send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_EL_BANK_PACKET(Personnage perso)
	{
		String packet = "EL"+perso.parseBankPacket();
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_JX_PACKET(Personnage perso,ArrayList<StatsMetier> SMs)
	{
		String packet = "JX";
		for(StatsMetier sm : SMs)
		{
			packet += "|"+sm.getTemplate().getId()+";"
					+ sm.get_lvl()+";"
					+ sm.getXpString(";")+";";
		}
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_JO_PACKET(Personnage perso,ArrayList<StatsMetier> SMs)
	{
		for(StatsMetier sm : SMs)
		{
			String packet = "JO"+sm.getID()+"|"+sm.getOptBinValue()+"|2";//FIXME 2=?
			send(perso,packet);
			if(Ancestra.CONFIG_DEBUG)
				GameServer.addToSockLog("Game: Send>>"+packet);
		}
	}
	public static void GAME_SEND_JS_PACKET(Personnage perso,ArrayList<StatsMetier> SMs)
	{
		String packet = "JS";
		for(StatsMetier sm : SMs)
		{
			packet += sm.parseJS();
		}
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_EsK_PACKET(Personnage perso, String str)
	{
		String packet = "EsK"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_FIGHT_SHOW_CASE(ArrayList<PrintWriter> PWs, int guid, int cellID)
	{
		String packet = "Gf"+guid+"|"+cellID;;
		for(PrintWriter PW : PWs)
		{
			send(PW,packet);
		}
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Fight: Send>>"+packet);
	}

	public static void GAME_SEND_Ea_PACKET(Personnage perso, String str)
	{
		String packet = "Ea"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_EA_PACKET(Personnage perso, String str)
	{
		String packet = "EA"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_Ec_PACKET(Personnage perso, String str)
	{
		String packet = "Ec"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_Em_PACKET(Personnage perso, String str)
	{
		String packet = "Em"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_IO_PACKET_TO_MAP(Carte map,int guid,String str)
	{
		String packet = "IO"+guid+"|"+str;
		for(Personnage perso : map.getPersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_FRIENDLIST_PACKET(Personnage perso)
	{
		String packet = "FL"+perso.get_compte().parseFriendList();
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_FA_PACKET(Personnage perso, String str)
	{
		String packet = "FA"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_FD_PACKET(Personnage perso, String str)
	{
		String packet = "FD"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_Rp_PACKET(Personnage perso, MountPark MP)
	{
		if(MP == null)return;
		String packet = "Rp"+MP.get_owner()+";"+MP.get_price()+";"+MP.get_size()+";"+MP.getObjectNumb()+";";
		Guild G = MP.get_guild();
		//Si une guilde est definie
		if(G != null)
		{
			packet += G.get_name()+";";
		}else packet += ";";
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_OS_PACKET(Personnage perso, int pano)
	{
		String packet = "OS";
		int num = perso.getNumbEquipedItemOfPanoplie(pano);
		if(num <= 0) packet += "-"+pano;
		else
		{
			packet += "+"+pano+"|";
			ItemSet IS = World.getItemSet(pano);
			if(IS != null)
			{
				String items = "";
				//Pour chaque objet de la pano
				for(ObjTemplate OT : IS.getItemTemplates())
				{
					//Si le joueur l'a équipé
					if(perso.hasEquiped(OT.getID()))
					{
						//On l'ajoute au packet
						if(items.length() >0)items+=";";
						items += OT.getID();
					}
				}
				packet += items+"|"+IS.getBonusStatByItemNumb(num).parseToItemSetStats();
			}
		}	
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_MOUNT_DESCRIPTION_PACKET(Personnage perso,Dragodinde DD)
	{
		String packet = "Rd"+DD.parse();
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_Rr_PACKET(Personnage perso, String str)
	{
		String packet = "Rr"+str;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_ALTER_GM_PACKET(Carte map,	Personnage perso)
	{
		String packet = "GM|~"+perso.parseToGM();
		for(Personnage z:map.getPersos())
			send(z,packet);
		
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_Ee_PACKET(Personnage perso, char c,String s)
	{
		String packet = "Ee"+c+s;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_cC_PACKET(Personnage perso, char c,String s)
	{
		String packet = "cC"+c+s;
		send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_ADD_NPC_TO_MAP(Carte map, NPC npc)
	{
		String packet = "GM|"+npc.parseGM();
		for(Personnage perso : map.getPersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}

	public static void GAME_SEND_GDO_PACKET_TO_MAP(Carte map, char c,int cell, int itm, int i)
	{
		String packet = "GDO"+c+cell+";"+itm+";"+i;
		for(Personnage perso : map.getPersos())
			send(perso,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Map: Send>>"+packet);
	}
	public static void GAME_SEND_GDO_PACKET(Personnage p, char c,int cell, int itm, int i)
	{
		String packet = "GDO"+c+cell+";"+itm+";"+i;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_ZC_PACKET(Personnage p,int a)
	{
		String packet = "ZC"+a;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_GIP_PACKET(Personnage p,int a)
	{
		String packet = "GIP"+a;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_gIB_PACKET(Personnage p)
	{
		String packet = "";
		Guild guild = p.get_guild();
		
		packet = "gIB1|2|3|4|5|6|7|8|9|10|451;5";
		
		send(p,packet);
	}
	public static void GAME_SEND_gn_PACKET(Personnage p)
	{
		String packet = "gn";
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gC_PACKET(Personnage p, String s)
	{
		String packet = "gC"+s;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gV_PACKET(Personnage p)
	{
		String packet = "gV";
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gIM_PACKET(Personnage p, Guild g, char c)
	{
		String packet = "gIM"+c;
		switch(c)
		{
			case '+':
				packet += g.parseMembersToGM();
			break;
		}
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gS_PACKET(Personnage p, GuildMember gm)
	{
		String packet = "gS"+gm.getGuild().get_name()+"|"+gm.getGuild().get_emblem().replace(',', '|')+"|"+gm.parseRights();
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gJ_PACKET(Personnage p, String str)
	{
		String packet = "gJ"+str;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gK_PACKET(Personnage p, String str)
	{
		String packet = "gK"+str;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_gIG_PACKET(Personnage p, Guild g)
	{
		long xpMin = World.getExpLevel(g.get_lvl()).guilde;
		long xpMax = World.getExpLevel(g.get_lvl()>=Ancestra.MAX_LEVEL?Ancestra.MAX_LEVEL:g.get_lvl()+1).guilde;
		String packet = "gIG"+(g.getSize()>9?1:0)+"|"+g.get_lvl()+"|"+xpMin+"|"+g.get_xp()+"|"+xpMax;
		send(p,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_M_PACKET(PrintWriter out, String id, String args)
	{
		String packet = "M"+id+"|"+args;
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_WC_PACKET(Personnage perso)
	{
		String packet = "WC"+perso.parseZaapList();
		send(perso.get_compte().getGameThread().get_out(),packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_Wc_PACKET(Personnage perso)
	{
		String packet = "Wc"+perso.parseZaapiList(perso.get_curCarte().getSubArea().get_area().get_id());
		send(perso.get_compte().getGameThread().get_out(),packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}

	public static void GAME_SEND_WV_PACKET(Personnage out)
	{
		String packet = "WV";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_Wv_PACKET(Personnage out)
	{
		String packet = "Wv";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	
	public static void GAME_SEND_WUE_PACKET(Personnage out)
	{
		String packet = "WUE";
		send(out,packet);
		if(Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}
	public static void GAME_SEND_EMOTE_LIST(Personnage perso,String s, String s1)
	{
		String packet = "eL"+s+"|"+s1;
		send(perso, packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	public static void GAME_SEND_NO_EMOTE(Personnage out)
	{
		String packet = "eUE";
		send(out, packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}

	public static void REALM_SEND_TOO_MANY_PLAYER_ERROR(PrintWriter out)
	{
		String packet = "AlEw";
		send(out, packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	/*---Packet utiliser par l'HDV---*/
	public static void GAME_SEND_EHL_PACKET(Personnage out, int categ, String templates)	//Packet de listage des templates dans une catégorie (En réponse au packet EHT)
	{
		String packet = "EHL"+categ+"|"+templates;
		
		send(out,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	public static void GAME_SEND_EHL_PACKET(Personnage out, String items)	//Packet de listage des objets en vente
	{
		String packet = "EHL"+items;
		
		send(out,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	public static void GAME_SEND_EHP_PACKET(Personnage out, int templateID)	//Packet d'envoie du prix moyen du template (En réponse a un packet EHP)
	{
		
		String packet = "EHP"+templateID+"|"+World.getObjTemplate(templateID).getAvgPrice();
		
		send(out,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	public static void GAME_SEND_EHl(Personnage out, Hdv seller,int templateID)
	{
		long time1 = System.currentTimeMillis();	//TIME
		String packet = "EHl" + seller.parseToEHl(templateID);
		System.out.println (System.currentTimeMillis() - time1 + "pour lister les prix d'un template");	//TIME
		send(out,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	public static void GAME_SEND_EHm_PACKET(Personnage out, String sign,String str)
	{
		String packet = "EHm"+sign + str;
		
		send(out,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	public static void GAME_SEND_EHM_PACKET(Personnage out, String sign,String str)
	{
		String packet = "EHM"+sign + str;
		
		send(out,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	
	public static void GAME_SEND_HDVITEM_SELLING(Personnage perso)
	{
		String packet = "EL";
		
		HdvEntry[] entries = perso.get_compte().getHdvItems(Math.abs(perso.get_isTradingWith()));	//Récupère un tableau de tout les items que le personnage à en vente dans l'HDV où il est
		
		boolean isFirst = true;
		for(HdvEntry curEntry : entries)
		{
			if(curEntry == null)
				break;
			if(!isFirst)
				packet += "|";
			packet += curEntry.parseToEL();
			
		isFirst = false;
		}
		
		send(perso,packet);
		if (Ancestra.CONFIG_DEBUG)
			GameServer.addToSockLog("Game: Send>>" + packet);
	}
	/*--------------------------------*/
	
	public static void GAME_SEND_FORGETSPELL_INTERFACE(char sign,Personnage perso)
	{
		String packet = "SF"+sign;
		send(perso, packet);
	}
}
