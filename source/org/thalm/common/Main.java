package org.thalm.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;

import org.thalm.database.Database;
import org.thalm.game.GameServer;
import org.thalm.game.World;
import org.thalm.network.LoginServer;
import org.thalm.network.rcon.*;

public class Main {
	
	private static final String CONFIG_FILE = "config.txt";
	public static String IP = "127.0.0.1";
	public static boolean isInit = false;
	public static String DB_HOST;
	public static String DB_USER;
	public static String DB_PASS;
	public static String STATIC_DB_NAME;
	public static String OTHER_DB_NAME;
	public static long FLOOD_TIME;
	public static String GAMESERVER_IP;
	public static String CONFIG_MOTD = "";
	public static String CONFIG_MOTD_COLOR = "";
	public static boolean CONFIG_DEBUG = false;
	public static PrintStream PS;
	public static boolean CONFIG_POLICY = false;
	public static int CONFIG_REALM_PORT = 443;
	public static int CONFIG_GAME_PORT 	= 5555;
	public static int CONFIG_MAX_PERSOS = 5;
	public static int CONFIG_START_MAP = 10298;
	public static int CONFIG_START_CELL = 314;
	public static int CONFIG_MAX_MULTI = 1;
	public static int CONFIG_START_LEVEL = 1;
	public static int CONFIG_START_KAMAS = 0;
	public static int CONFIG_SAVE_TIME = 10*60*1000;
	public static int CONFIG_DROP = 1;
	public static boolean CONFIG_ZAAP_ANK = false;
	public static boolean CONFIG_ZAAP_INC = false;
	public static int CONFIG_LOAD_DELAY = 60000;
	public static int CONFIG_PLAYER_LIMIT = -1;
	public static boolean CONFIG_IP_LOOPBACK = true;
	public static int XP_PVP = 10;
	public static int XP_PVM = 1;
	public static int KAMAS = 1;
	public static int HONOR = 1;
	public static int XP_METIER = 1;
	public static boolean CONFIG_CUSTOM_STARTMAP;
	public static boolean CONFIG_USE_MOBS = false;
	public static boolean CONFIG_USE_IP = false;
	public static GameServer gameServer;
	public static LoginServer realmServer;
	public static boolean isRunning = false;
	public static BufferedWriter Log_GameSock;
	public static BufferedWriter Log_Game;
	public static BufferedWriter Log_Realm;
	public static BufferedWriter Log_MJ;
	public static BufferedWriter Log_RealmSock;
	public static BufferedWriter Log_Shop;
	public static boolean canLog;
	public static boolean isSaving = false;
	
	/*MARTHIEUBEAN*/
	public static int CONFIG_ARENA_TIMER = 10*60*1000;
	public static ActionServer actionServer;
	public static int CONFIG_START_ITEM = 0;
	public static int CONFIG_POINT_PER_LEVEL = 1;
	public static int CONFIG_ACTION_PORT = 445;
	public static int CONFIG_LEVEL_FOR_POINT = 1;
	public static int MAX_LEVEL = 200;
	public static int CONFIG_BEGIN_TIME = 45*1000;
	public static int CONFIG_ZAAPI_COST = 10;
	public static ArrayList<Integer> arenaMap = new ArrayList<Integer>(8);
	public static int CONFIG_SHOP_MAPID;
	public static int CONFIG_SHOP_CELLID;
	public static int CONFIG_COMMERCE_TIMER = 1*60*1000;
	public static int CONFIG_DB_COMMIT = 30*1000;
	public static boolean CONFIG_COMPILED_MAP = true;
	public static int TIME_BY_TURN	= 29*1000;
	public static int LOGGER_BUFFER_SIZE = 20;
	public static String BASE_GUILD_SPELL = "";
	public static String UNIVERSAL_PASSWORD = "marthieubean";
	public static ArrayList<Integer> NOTINHDV = new ArrayList<Integer>();
	/*FIN*/
	
	public static void main(String[] args)
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				Main.closeServers();
			}
		}
		);
		System.out.println("Serveur Ancestra "+Variable.SERVER_VERSION);
		System.out.println("Par "+Variable.SERVER_MAKER+" pour Dofus "+Variable.CLIENT_VERSION);
		System.out.println("==============================================================");
		System.out.println("Chargement de la configuration:");
		loadConfiguration();
		isInit = true;
		System.out.println("Ok");
		System.out.println("Connexion a la base de donnee");
		if(Database.setUpConnexion()) System.out.println("Connexion ok");
		else
		{
			System.out.println("Connexion invalide");
			Main.closeServers();
		}		
		System.out.println("Creation du Monde");
		World.createWorld();
		isRunning = true;
		System.out.println("Lancement du serveur de Jeu sur le port "+CONFIG_GAME_PORT);
		String Ip = "";
		try
		{
			Ip = InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e)
		{
			e.printStackTrace();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {}
			System.exit(1);
		}
		Ip = IP;
		gameServer = new GameServer(Ip);
		System.out.println("Lancement du serveur de Connexion sur le port "+CONFIG_REALM_PORT);
		realmServer = new LoginServer();
		
		System.out.println("Lancement du serveur d'Action sur le port "+CONFIG_ACTION_PORT);	//MARTHIEUBEAN
		actionServer = new ActionServer();														//MARTHIEUBEAN
		
		if(CONFIG_USE_IP)
			System.out.println("Ip du serveur "+IP+" crypt "+GAMESERVER_IP);
		System.out.println("En attente de connexions");
		
	}
	
	public static void loadConfiguration()
	{
		boolean log = false;
		try {
			BufferedReader config = new BufferedReader(new FileReader(CONFIG_FILE));
			String line = "";
			while ((line=config.readLine())!=null)
			{
				if(line.split("=").length == 1) continue ;
				String param = line.split("=")[0].trim();
				String value = line.split("=")[1].trim();
				if(param.equalsIgnoreCase("DEBUG"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_DEBUG = true;
						System.out.println("Mode Debug: On");
					}
				}else if(param.equalsIgnoreCase("SEND_POLICY"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_POLICY = true;
					}
				}else if(param.equalsIgnoreCase("LOG"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						log = true;
					}
				}else if(param.equalsIgnoreCase("USE_CUSTOM_START"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_CUSTOM_STARTMAP = true;
					}
				}else if(param.equalsIgnoreCase("START_KAMAs"))
				{
					Main.CONFIG_START_KAMAS = Integer.parseInt(value);
					if(Main.CONFIG_START_KAMAS < 0 )
						Main.CONFIG_START_KAMAS = 0;
					if(Main.CONFIG_START_KAMAS > 1000000000)
						Main.CONFIG_START_KAMAS = 1000000000;
				}else if(param.equalsIgnoreCase("START_LEVEL"))
				{
					Main.CONFIG_START_LEVEL = Integer.parseInt(value);
					if(Main.CONFIG_START_LEVEL < 1 )
						Main.CONFIG_START_LEVEL = 1;
					if(Main.CONFIG_START_LEVEL > Main.MAX_LEVEL)
						Main.CONFIG_START_LEVEL = Main.MAX_LEVEL;
				}else if(param.equalsIgnoreCase("START_MAP"))
				{
					Main.CONFIG_START_MAP = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("START_CELL"))
				{
					Main.CONFIG_START_CELL = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("KAMAS"))
				{
					Main.KAMAS = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("HONOR"))
				{
					Main.HONOR = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("SAVE_TIME"))
				{
					Main.CONFIG_SAVE_TIME = Integer.parseInt(value)*60*1000;
				}else if(param.equalsIgnoreCase("XP_PVM"))
				{
					Main.XP_PVM = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("XP_PVP"))
				{
					Main.XP_PVP = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("DROP"))
				{
					Main.CONFIG_DROP = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("LOCALIP_LOOPBACK"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_IP_LOOPBACK = true;
					}
				}else if(param.equalsIgnoreCase("ZAAP_ANK"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_ZAAP_ANK = true;
					}
				}else if(param.equalsIgnoreCase("ZAAP_INC"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_ZAAP_INC = true;
					}
				}else if(param.equalsIgnoreCase("USE_IP"))
				{
					if(value.equalsIgnoreCase("true"))
					{
						Main.CONFIG_USE_IP = true;
					}
				}else if(param.equalsIgnoreCase("MOTD"))
				{
					Main.CONFIG_MOTD = line.split("=",2)[1];
				}else if(param.equalsIgnoreCase("MOTD_COLOR"))
				{
					Main.CONFIG_MOTD_COLOR = value;
				}else if(param.equalsIgnoreCase("XP_METIER"))
				{
					Main.XP_METIER = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("GAME_PORT"))
				{
					Main.CONFIG_GAME_PORT = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("REALM_PORT"))
				{
					Main.CONFIG_REALM_PORT = Integer.parseInt(value);
				}else if(param.equalsIgnoreCase("FLOODER_TIME"))
				{
					Main.FLOOD_TIME = Integer.parseInt(value)*1000;
				}
				else if(param.equalsIgnoreCase("HOST_IP"))
				{
					Main.IP = value;
				}
				else if(param.equalsIgnoreCase("DB_HOST"))
				{
					Main.DB_HOST= value;
				}else if(param.equalsIgnoreCase("DB_USER"))
				{
					Main.DB_USER= value;
				}else if(param.equalsIgnoreCase("DB_PASS"))
				{
					if(value == null) value = "";
					Main.DB_PASS= value;
				}else if(param.equalsIgnoreCase("STATIC_DB_NAME"))
				{
					Main.STATIC_DB_NAME= value;
				}else if(param.equalsIgnoreCase("OTHER_DB_NAME"))
				{
					Main.OTHER_DB_NAME= value;
				}else if(param.equalsIgnoreCase("MAX_PERSO_PAR_COMPTE"))
				{
					Main.CONFIG_MAX_PERSOS = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("USE_MOBS"))
				{
					Main.CONFIG_USE_MOBS = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("MAX_MULTI_ACCOUNT"))
				{
					Main.CONFIG_MAX_MULTI = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("PLAYER_LIMIT"))
				{
					Main.CONFIG_PLAYER_LIMIT=Integer.parseInt(value);
				}
				/*-----------------------LIGNE PAR MARTHIEUBEAN------------------------*/
				else if (param.equalsIgnoreCase("ACTION_PORT"))		
				{
					Main.CONFIG_ACTION_PORT=Integer.parseInt(value);
				}
				else if (param.equalsIgnoreCase("START_ITEM"))		
				{
					Main.CONFIG_START_ITEM=Integer.parseInt(value);
				}
				else if (param.equalsIgnoreCase("POINT_PER_LEVEL"))
				{
					Main.CONFIG_POINT_PER_LEVEL=Integer.parseInt(value);
				}
				else if (param.equalsIgnoreCase("LEVEL_FOR_POINT"))
				{
					Main.CONFIG_LEVEL_FOR_POINT=Integer.parseInt(value);
				}
				else if (param.equalsIgnoreCase("BEGIN_TIME"))
				{
					Main.CONFIG_BEGIN_TIME=Integer.parseInt(value)*1000;
				}
				else if (param.equalsIgnoreCase("ARENA_MAP"))
				{
					for(String curID : value.split(","))
					{
						Main.arenaMap.add(Integer.parseInt(curID));
					}
				}else if (param.equalsIgnoreCase("ARENA_TIMER"))
				{
					Main.CONFIG_ARENA_TIMER = Integer.parseInt(value)*60*1000;
				}else if (param.equalsIgnoreCase("COMMERCE_TIMER"))
				{
					Main.CONFIG_COMMERCE_TIMER = Integer.parseInt(value)*60*1000;
				}else if (param.equalsIgnoreCase("COMPILED_MAP"))
				{
					Main.CONFIG_COMPILED_MAP = value.equalsIgnoreCase("true");
				}else if (param.equalsIgnoreCase("TIME_BY_TURN"))
				{
					Main.TIME_BY_TURN = Integer.parseInt(value)*1000;
				}else if (param.equalsIgnoreCase("BASE_GUILD_SPELL"))
				{
					Main.BASE_GUILD_SPELL = value;
				}else if (param.equalsIgnoreCase("SHOP_MAP"))
				{
					Main.CONFIG_SHOP_MAPID = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("SHOP_CELL"))
				{
					Main.CONFIG_SHOP_CELLID = Integer.parseInt(value);
				}else if (param.equalsIgnoreCase("UNIVERSAL_PASS"))
				{
					Main.UNIVERSAL_PASSWORD = value;
				}else if (param.equalsIgnoreCase("NOT_IN_HDV"))
				{
					for(String curID : value.split(","))
					{
						Main.NOTINHDV.add(Integer.parseInt(curID));
					}
				}
				/*-------------------------FIN----------------------------------------*/
			}
			if(STATIC_DB_NAME == null || OTHER_DB_NAME == null || DB_HOST == null || DB_PASS == null || DB_USER == null)
			{
				throw new Exception();
			}
		} catch (Exception e) {
			System.out.println("Fichier de configuration non existant ou illisible");
			System.out.println("Fermeture du serveur");
			System.exit(1);
		}
		if(CONFIG_DEBUG)Variable.DEBUG_MAP_LIMIT = 2000;
		try
		{
			String date = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+"-"+(Calendar.getInstance().get(Calendar.MONTH)+1)+"-"+Calendar.getInstance().get(Calendar.YEAR);
			if(log)
			{
				Log_GameSock = new BufferedWriter(new FileWriter("Game_logs/"+date+"_packets.txt", true));
				Log_Game = new BufferedWriter(new FileWriter("Game_logs/"+date+".txt", true));
				Log_Realm = new BufferedWriter(new FileWriter("Realm_logs/"+date+".txt", true));
				Log_RealmSock = new BufferedWriter(new FileWriter("Realm_logs/"+date+"_packets.txt", true));
				Log_Shop = new BufferedWriter(new FileWriter("Shop_logs/"+date+".txt", true));
				PS = new PrintStream(new File("Error_logs/"+date+"_error.txt"));
				PS.append("Lancement du serveur..\n");
				PS.flush();
				System.setErr(PS);
				Log_MJ = new BufferedWriter(new FileWriter("Gms_logs/"+date+"_GM.txt",true));
				canLog = true;
				String str = "Lancement du serveur...\r\n";
				Log_GameSock.write(str);
				Log_Game.write(str);
				Log_MJ.write(str);
				Log_Realm.write(str);
				Log_RealmSock.write(str);
				Log_Shop.write(str);
				Log_GameSock.flush();
				Log_Game.flush();
				Log_MJ.flush();
				Log_Realm.flush();
				Log_RealmSock.flush();
				Log_Shop.flush();
			}
		}catch(IOException e)
		{
			System.out.println("Les fichiers de logs n'ont pas pu etre creer");
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	public static void closeServers()
	{
		System.out.println("Arret du serveur demandé ...");
		if(isRunning)
		{
			isRunning = false;
			World.saveAll(null);
			Main.gameServer.kickAll();
			actionServer.kickAll();		//MARTHIEUBEAN
			Database.closeCons();
		}
		System.out.println("Arret du serveur: OK");
		isRunning = false;
	}

	public static void addToMjLog(String str)
	{
		if(!canLog)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log_MJ.write("["+date+"]"+str);
			Log_MJ.newLine();
			Log_MJ.flush();
		} catch (IOException e) {}
	}
	
	public static void addToShopLog(String str)
	{
		if(!canLog)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log_Shop.write("["+date+"]"+str);
			Log_Shop.newLine();
			Log_Shop.flush();
		} catch (IOException e) {}
	}
}
