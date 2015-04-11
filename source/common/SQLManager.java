package common;

import game.GameServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Calendar; //MARTHIEUBEAN

import com.mysql.jdbc.PreparedStatement;

import common.World.*;
import objects.*;
import objects.Hdv.*;
import objects.NPC_tmpl.*;
import objects.Objet.*;
import objects.Sort.*;
import objects.Carte.*;
import objects.Guild.GuildMember;
import realm.RealmServer;

public class SQLManager {
	
	private static Connection othCon;
	private static Connection statCon;
	
	private static Timer timerCommit;
	private static boolean needCommit;
	
	/*public synchronized static boolean execute(String query,String DBNAME) throws SQLException
	{
		if(!Ancestra.isInit) return false;
		
		Connection DB = DriverManager.getConnection("jdbc:mysql://"+Ancestra.DB_HOST+"/"+DBNAME,Ancestra.DB_USER,Ancestra.DB_PASS);
		Statement stat = DB.createStatement();
		stat.setQueryTimeout(0);
		stat.execute(query);
		stat.close();
		DB.close();
		return true;
	}*/

	public synchronized static ResultSet executeQuery(String query,String DBNAME) throws SQLException
	{
		if(!Ancestra.isInit)
			return null;
		
		Connection DB;
		if(DBNAME.equals(Ancestra.OTHER_DB_NAME))
			DB = othCon;
		else
			DB = statCon;
		
		Statement stat = DB.createStatement();
		ResultSet RS = stat.executeQuery(query);
		stat.setQueryTimeout(300);
		return RS;
	}

	public synchronized static PreparedStatement newTransact(String baseQuery,Connection dbCon) throws SQLException
	{
		PreparedStatement toReturn = (PreparedStatement) dbCon.prepareStatement(baseQuery);
		
		needCommit = true;
		return toReturn;
	}
	public synchronized static void commitTransacts()
	{
		try
		{
			if(othCon.isClosed() || statCon.isClosed())
			{
				closeCons();
				setUpConnexion();
			}
			
			statCon.commit();
			othCon.commit();
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR:"+e.getMessage());
			e.printStackTrace();
			//rollBack(con); //Pas de rollBack, la BD sauvegarde ce qu'elle peut
		}
	}
	public synchronized static void rollBack(Connection con)
	{
		try
		{
			con.rollback();
		}
		catch(SQLException e)
		{
			System.out.println("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public synchronized static void closeCons()
	{
		try
		{
			commitTransacts();
			
			othCon.close();
			statCon.close();
		}catch (Exception e)
		{
			System.out.println("Erreur à la fermeture des connexions SQL:"+e.getMessage());
			e.printStackTrace();
		}
	}
	public static final boolean setUpConnexion()
	{
		try
		{
			othCon = DriverManager.getConnection("jdbc:mysql://"+Ancestra.DB_HOST+"/"+Ancestra.OTHER_DB_NAME,Ancestra.DB_USER,Ancestra.DB_PASS);
			othCon.setAutoCommit(false);
			
			statCon = DriverManager.getConnection("jdbc:mysql://"+Ancestra.DB_HOST+"/"+Ancestra.STATIC_DB_NAME,Ancestra.DB_USER,Ancestra.DB_PASS);
			statCon.setAutoCommit(false);
			
			if(!statCon.isValid(1000) || !othCon.isValid(1000))
			{
				GameServer.addToLog("SQLError : Connexion à la BD invalide!");
				return false;
			}
			
			needCommit = false;
			TIMER(true);
			
			return true;
		}catch(SQLException e)
		{
			System.out.println("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public static void UPDATE_ACCOUNT_DATA(Compte acc)
	{
		try
		{
			String baseQuery = "UPDATE accounts SET " +
								"`bankKamas` = ?,"+
								"`bank` = ?,"+
								"`level` = ?,"+
								"`stable` = ?,"+
								"`banned` = ?,"+
								"`friends` = ?"+
								" WHERE `guid` = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setLong(1, acc.getBankKamas());
			p.setString(2, acc.parseBankObjetsToDB());
			p.setInt(3, acc.get_gmLvl());
			p.setString(4, acc.parseStableIDs());
			p.setInt(5, (acc.isBanned()?1:0));
			p.setString(6, acc.parseFriendListToDB());
			p.setInt(7, acc.get_GUID());
			
			p.executeUpdate();
			closePreparedStatement(p);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void UPDATE_ACCOUNT_BANKKAMAS(int compteID,int toAdd)
	{
		try
		{
			String baseQuery = "UPDATE `accounts` SET" +
					" bankKamas = bankKamas + ?" +
					" WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setInt(1, toAdd);
			p.setInt(2, compteID);
			
			p.executeUpdate();
			closePreparedStatement(p);
			
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_CRAFTS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from crafts;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				ArrayList<Couple<Integer,Integer>> m = new ArrayList<Couple<Integer,Integer>>();
				
				boolean cont = true;
				for(String str : RS.getString("craft").split(";"))
				{
					try
					{
							int tID = Integer.parseInt(str.split("\\*")[0]);
							int qua =  Integer.parseInt(str.split("\\*")[1]);
							m.add(new Couple<Integer,Integer>(tID,qua));
					}catch(Exception e){e.printStackTrace();cont = false;};
				}
				//s'il y a eu une erreur de parsing, on ignore cette recette
				if(!cont)continue;
				
				World.addCraft
				(
					RS.getInt("id"),
					m
				);
			}
			closeResultSet(RS);;
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_GUILDS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from guilds;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				World.addGuild
				(
				new Guild(
						RS.getInt("id"),
						RS.getString("name"),
						RS.getString("emblem"),
						RS.getInt("lvl"),
						RS.getLong("xp"),
						new TreeMap<Integer, Integer>(),
						new TreeMap<Integer, Integer>()
				),false
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_GUILD_MEMBERS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from guild_members;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				//Personnage P = World.getPersonnage(RS.getInt("guid"));
				Guild G = World.getGuild(RS.getInt("guild"));
				if(G == null)continue;
				/*GuildMember GM = */G.addMember(RS.getInt("guid"), RS.getString("name"), RS.getInt("level"), RS.getInt("gfxid"), RS.getInt("rank"), RS.getByte("pxp"), RS.getLong("xpdone"), RS.getInt("rights"), RS.getByte("align"),RS.getDate("lastConnection").toString().replaceAll("-","~"));
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_MOUNTS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from mounts_data;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				World.addDragodinde
				(
					new Dragodinde
					(
						RS.getInt("id"),
						RS.getInt("color"),
						RS.getInt("sexe"),
						RS.getInt("amour"),
						RS.getInt("endurance"),
						RS.getInt("level"),
						RS.getLong("xp"),
						RS.getString("name"),
						RS.getInt("fatigue"),
						RS.getInt("energie"),
						RS.getInt("reproductions"),
						RS.getInt("maturite"),
						RS.getInt("serenite"),
						RS.getString("items"),
						RS.getString("ancetres")
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_DROPS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from drops;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Monstre MT = World.getMonstre(RS.getInt("mob"));
				MT.addDrop(new Drop(
						RS.getInt("item"),
						RS.getInt("seuil"),
						RS.getFloat("taux"),
						RS.getInt("max")
				));
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_ITEMSETS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from itemsets;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addItemSet(
							new ItemSet
							(
								RS.getInt("id"),
								RS.getString("items"),
								RS.getString("bonus")
							)
						);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_IOTEMPLATE()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from interactive_objects_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addIOTemplate(
							new IOTemplate
							(
								RS.getInt("id"),
								RS.getInt("respawn"),
								RS.getInt("duration"),
								RS.getInt("unknow"),
								RS.getInt("walkable")==1
							)
						);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_MOUNTPARKS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from mountpark_data;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getInt("mapid"));
				if(map == null)continue;
				new MountPark(
						RS.getInt("owner"),
						map,
						RS.getInt("size"),
						RS.getString("data"),
						RS.getInt("guild"),
						RS.getInt("price")
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_JOBS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from jobs_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addJob(
						new Metier(
							RS.getInt("id"),
							RS.getString("tools"),
							RS.getString("crafts")
							)
						);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void LOAD_AREA()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from area_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Area A = new Area
					(
						RS.getInt("id"),
						RS.getInt("superarea"),
						RS.getString("name")
					);
				World.addArea(A);
				//on ajoute la zone au continent
				A.get_superArea().addArea(A);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void LOAD_SUBAREA()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from subarea_data;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				SubArea SA = new SubArea
					(
						RS.getInt("id"),
						RS.getInt("area"),
						RS.getInt("alignement"),
						RS.getString("name")
					);
				World.addSubArea(SA);
				//on ajoute la sous zone a la zone
				if(SA.get_area() != null)
					SA.get_area().addSubArea(SA);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static int LOAD_NPCS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from npcs;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getInt("mapid"));
				if(map == null)continue;
				map.addNpc(RS.getInt("npcid"), RS.getInt("cellid"), RS.getInt("orientation"));
				nbr ++;
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}
	
	public static void LOAD_COMPTES()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts;",Ancestra.OTHER_DB_NAME);
			String baseQuery = "UPDATE accounts " +
								"SET `reload_needed` = 0 " +
								"WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			while(RS.next())
			{
				World.addAccount(new Compte(
				RS.getInt("guid"),
				RS.getString("account"),
				RS.getString("pass"),
				RS.getString("pseudo"),
				RS.getString("question"),
				RS.getString("reponse"),
				RS.getInt("level"),
				(RS.getInt("banned") == 1),
				RS.getString("lastIP"),
				RS.getString("lastConnectionDate"),
				RS.getString("bank"),
				RS.getInt("bankKamas"),
				RS.getString("friends"),
				RS.getString("stable")
				));
				
				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}
			closePreparedStatement(p);
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static int getNextPersonnageGuid()
	{
		try
		{
			ResultSet RS = executeQuery("SELECT guid FROM personnages ORDER BY guid DESC LIMIT 1;",Ancestra.OTHER_DB_NAME);
			RS.first();
			int guid = RS.getInt("guid");
			guid++;
			closeResultSet(RS);
			return guid;
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
		return 0;
	}
	public static void LOAD_PERSO_BY_ACCOUNT(int accID)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages WHERE account = '"+accID+"';", Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				stats.put(Constants.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constants.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constants.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constants.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constants.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constants.STATS_ADD_AGIL, RS.getInt("agilite"));
				
				Personnage perso = new Personnage(
						RS.getInt("guid"),
						RS.getString("name"),
						RS.getInt("sexe"),
						RS.getInt("class"),
						RS.getInt("color1"),
						RS.getInt("color2"),
						RS.getInt("color3"),
						RS.getLong("kamas"),
						RS.getInt("spellboost"),
						RS.getInt("capital"),
						RS.getInt("energy"),
						RS.getInt("level"),
						RS.getLong("xp"),
						RS.getInt("size"),
						RS.getInt("gfx"),
						RS.getByte("alignement"),
						RS.getInt("account"),
						stats,
						RS.getInt("seeFriend"),
						RS.getString("canaux"),
						RS.getInt("map"),
						RS.getInt("cell"),
						RS.getString("objets"),
						RS.getInt("pdvper"),
						RS.getString("spells"),
						RS.getString("savepos"),
						RS.getString("jobs"),
						RS.getInt("mountxpgive"),
						RS.getInt("mount"),
						RS.getInt("honor"),
						RS.getInt("deshonor"),
						RS.getInt("alvl"),
						RS.getString("zaaps")
						);
				perso.get_baseStats().addOneStat(Constants.STATS_ADD_PA,RS.getInt("pa"));	//MARTHIEUBEAN
				perso.get_baseStats().addOneStat(Constants.STATS_ADD_PM,RS.getInt("pm"));	//MARTHIEUBEAN
				World.addPersonnage(perso);
				/*Afféctation d'une guilde au membre s'il y a lieu*/
				int guildId = isPersoInGuild(RS.getInt("guid"));
				if(guildId >= 0)
				{
					perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
				}
				/*FIN*/
				if(World.getCompte(accID) != null)
					World.getCompte(accID).addPerso(perso);
			}
			
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
	}
	public static void LOAD_PERSO(int persoID)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages WHERE guid = '"+persoID+"';", Ancestra.OTHER_DB_NAME);
			int accID;
			while(RS.next())
			{
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				stats.put(Constants.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constants.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constants.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constants.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constants.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constants.STATS_ADD_AGIL, RS.getInt("agilite"));
				
				accID = RS.getInt("account");
				
				Personnage perso = new Personnage(
						RS.getInt("guid"),
						RS.getString("name"),
						RS.getInt("sexe"),
						RS.getInt("class"),
						RS.getInt("color1"),
						RS.getInt("color2"),
						RS.getInt("color3"),
						RS.getLong("kamas"),
						RS.getInt("spellboost"),
						RS.getInt("capital"),
						RS.getInt("energy"),
						RS.getInt("level"),
						RS.getLong("xp"),
						RS.getInt("size"),
						RS.getInt("gfx"),
						RS.getByte("alignement"),
						accID,
						stats,
						RS.getInt("seeFriend"),
						RS.getString("canaux"),
						RS.getInt("map"),
						RS.getInt("cell"),
						RS.getString("objets"),
						RS.getInt("pdvper"),
						RS.getString("spells"),
						RS.getString("savepos"),
						RS.getString("jobs"),
						RS.getInt("mountxpgive"),
						RS.getInt("mount"),
						RS.getInt("honor"),
						RS.getInt("deshonor"),
						RS.getInt("alvl"),
						RS.getString("zaaps")
						);
				perso.get_baseStats().addOneStat(Constants.STATS_ADD_PA,RS.getInt("pa"));	//MARTHIEUBEAN
				perso.get_baseStats().addOneStat(Constants.STATS_ADD_PM,RS.getInt("pm"));	//MARTHIEUBEAN
				World.addPersonnage(perso);
				/*Afféctation d'une guilde au membre s'il y a lieu*/
				int guildId = isPersoInGuild(RS.getInt("guid"));
				if(guildId >= 0)
				{
					perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
				}
				/*FIN*/
				if(World.getCompte(accID) != null)
					World.getCompte(accID).addPerso(perso);
			}
			
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
	}

	public static boolean DELETE_PERSO_IN_BDD(Personnage perso)
	{
		int guid = perso.get_GUID();
		String baseQuery = "DELETE FROM personnages WHERE guid = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, guid);
			
			p.execute();
			
			if(!perso.getItemsIDSplitByChar(",").equals(""))
			{
				baseQuery = "DELETE FROM items WHERE guid IN (?);";
				p = newTransact(baseQuery, othCon);
				p.setString(1, perso.getItemsIDSplitByChar(","));
				
				p.execute();
			}
			if(perso.getMount() != null)
			{
				baseQuery = "DELETE FROM mounts_data WHERE id = ?";
				p = newTransact(baseQuery, othCon);
				p.setInt(1, perso.getMount().get_id());
				
				p.execute();
				World.delDragoByID(perso.getMount().get_id());
			}
			if(perso.getGuildMember() != null)
			{
				perso.get_guild().removeMember(guid);
				baseQuery = "DELETE FROM guild_members WHERE guid = ?";
				p = newTransact(baseQuery, othCon);
				p.setInt(1, guid);
				
				p.execute();
			}
			
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
			GameServer.addToLog("Game: Supression du personnage echouee");
			return false;
		}
	}
	
	public static boolean ADD_PERSO_IN_BDD(Personnage perso)
	{
		String baseQuery = "INSERT INTO personnages( `guid` , `name` , `sexe` , `class` , `color1` , `color2` , `color3` , `kamas` , `spellboost` , `capital` , `energy` , `level` , `xp` , `size` , `gfx` , `account`,`cell`,`map`,`spells`,`objets`)" +
				" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'');";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setInt(1,perso.get_GUID());
			p.setString(2, perso.get_name());
			p.setInt(3,perso.get_sexe());
			p.setInt(4,perso.get_classe());
			p.setInt(5,perso.get_color1());
			p.setInt(6,perso.get_color2());
			p.setInt(7,perso.get_color3());
			p.setLong(8,perso.get_kamas());
			p.setInt(9,perso.get_spellPts());
			p.setInt(10,perso.get_capital());
			p.setInt(11,perso.get_energy());
			p.setInt(12,perso.get_lvl());
			p.setLong(13,perso.get_curExp());
			p.setInt(14,perso.get_size());
			p.setInt(15,perso.get_gfxID());
			p.setInt(16,perso.getAccID());
			p.setInt(17,perso.get_curCell().getID());
			p.setInt(18,perso.get_curCarte().get_id());
			p.setString(19, perso.parseSpellToDB());
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
			GameServer.addToLog("Game: Creation du personnage echouee");
			return false;
		}
	}

	public static void LOAD_EXP()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from experience;",Ancestra.STATIC_DB_NAME);
			while(RS.next())World.addExpLevel(RS.getInt("lvl"),new World.ExpLevel(RS.getLong("perso"),RS.getInt("metier"),RS.getInt("dinde"),RS.getInt("pvp")));
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	
	
	public static int LOAD_TRIGGERS()
	{
		try
		{
			int nbr = 0;
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM `scripted_cells`",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				if(World.getCarte(RS.getInt("MapID")) == null) continue;
				if(World.getCarte(RS.getInt("MapID")).getCase(RS.getInt("CellID")) == null) continue;
				
				switch(RS.getInt("EventID"))
				{
					case 1://Stop sur la case(triggers)
						World.getCarte(RS.getInt("MapID")).getCase(RS.getInt("CellID")).addOnCellStopAction(RS.getInt("ActionID"), RS.getString("ActionsArgs"), RS.getString("Conditions"));	
					break;
						
					default:
						GameServer.addToLog("Action Event "+RS.getInt("EventID")+" non implanté");
					break;
				}
				nbr++;
			}
			closeResultSet(RS);
			return nbr;
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
		return 0;
	}

	public static void LOAD_MAPS()
	{
		try
		{
			ResultSet RS;
			if(!Ancestra.CONFIG_DEBUG)
			{
				RS = SQLManager.executeQuery("SELECT  * from maps LIMIT "+Constants.DEBUG_MAP_LIMIT+";",Ancestra.STATIC_DB_NAME);
			}
			else
			{
				/*DEBUG*/
				String divers = "250000,675,7411,10109";
				String houseLac = "9015,10853,10854,10855,10858,10862,10865,10869,10875,10881,10883,10885,10890,10894,10900,10901";
				String hdv = "4216,4271,8759,4287,2221,4232,4178,4183,8760,4098,4179,6159,4299,4247,4262,8757,4174,4172,8478";
				RS = SQLManager.executeQuery("SELECT  * from maps WHERE id IN("+divers+",10291,8747,4216,10129,10130,10131,10132,10133,10134,"+hdv+","+houseLac+");",Ancestra.STATIC_DB_NAME);
				/**/
				//RS = executeQuery("SELECT m.* FROM maps AS m,maps_stat AS ms WHERE ms.demandes > 3 AND m.id = ms.map", Ancestra.STATIC_DB_NAME);
			}
			
			while(RS.next())
			{
					World.addCarte(
							new Carte(
							RS.getInt("id"),
							RS.getString("date"),
							RS.getInt("width"),
							RS.getInt("heigth"),
							RS.getString("key"),
							RS.getString("places"),
							RS.getString("mapData"),
							RS.getString("monsters"),
							RS.getString("mappos"),
							RS.getInt("numgroup"),
							RS.getInt("groupmaxsize")
							));
			}
			SQLManager.closeResultSet(RS);
			RS = SQLManager.executeQuery("SELECT  * from mobgroups_fix;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
					Carte c = World.getCarte(RS.getInt("mapid"));
					if(c == null)continue;
					if(c.getCase(RS.getInt("cellid")) == null)continue;
					c.addStaticGroup(RS.getInt("cellid"), RS.getString("groupData"));
			}
			SQLManager.closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}

	public static void SAVE_PERSONNAGE(Personnage _perso, boolean saveItem)
	{
		String baseQuery = "UPDATE `personnages` SET "+
						"`seeFriend`= ?,"+
						"`canaux`= ?,"+
						"`pdvper`= ?,"+
						"`map`= ?,"+
						"`cell`= ?,"+
						"`vitalite`= ?,"+
						"`force`= ?,"+
						"`sagesse`= ?,"+
						"`intelligence`= ?,"+
						"`chance`= ?,"+
						"`agilite`= ?,"+
						"`alignement`= ?,"+
						"`honor`= ?,"+
						"`deshonor`= ?,"+
						"`alvl`= ?,"+
						"`gfx`= ?,"+
						"`xp`= ?,"+
						"`level`= ?,"+
						"`energy`= ?,"+
						"`capital`= ?,"+
						"`spellboost`= ?,"+
						"`kamas`= ?,"+
						"`size` = ?," +
						"`spells` = ?," +
						"`objets` = ?,"+
						"`savepos` = ?,"+
						"`jobs` = ?,"+
						"`mountxpgive` = ?,"+
						"`zaaps` = ?,"+
						"`mount` = ?"+		
						" WHERE `personnages`.`guid` = ? LIMIT 1 ;";
		
		PreparedStatement p = null;
		
		try
		{
			p = newTransact(baseQuery, othCon);
			
			p.setInt(1,(_perso.is_showFriendConnection()?1:0));
			p.setString(2,_perso.get_canaux());
			p.setInt(3,_perso.get_pdvper());
			p.setInt(4,_perso.get_curCarte().get_id());
			p.setInt(5,_perso.get_curCell().getID());
			p.setInt(6,_perso.get_baseStats().getEffect(Constants.STATS_ADD_VITA));
			p.setInt(7,_perso.get_baseStats().getEffect(Constants.STATS_ADD_FORC));
			p.setInt(8,_perso.get_baseStats().getEffect(Constants.STATS_ADD_SAGE));
			p.setInt(9,_perso.get_baseStats().getEffect(Constants.STATS_ADD_INTE));
			p.setInt(10,_perso.get_baseStats().getEffect(Constants.STATS_ADD_CHAN));
			p.setInt(11,_perso.get_baseStats().getEffect(Constants.STATS_ADD_AGIL));
			p.setInt(12,_perso.get_align());
			p.setInt(13,_perso.get_honor());
			p.setInt(14,_perso.getDeshonor());
			p.setInt(15,_perso.getALvl());
			p.setInt(16,_perso.get_gfxID());
			p.setLong(17,_perso.get_curExp());
			p.setInt(18,_perso.get_lvl());
			p.setInt(19,_perso.get_energy());
			p.setInt(20,_perso.get_capital());
			p.setInt(21,_perso.get_spellPts());
			p.setLong(22,_perso.get_kamas());
			p.setInt(23,_perso.get_size());
			p.setString(24,_perso.parseSpellToDB());
			p.setString(25,_perso.parseObjetsToDB());
			p.setString(26,_perso.get_savePos());
			p.setString(27,_perso.parseJobData());
			p.setInt(28,_perso.getMountXpGive());
			p.setString(29,_perso.parseZaaps());
			p.setInt(30, (_perso.getMount()!=null?_perso.getMount().get_id():-1));
			p.setInt(31,_perso.get_GUID());
			
			p.executeUpdate();
			
			if(_perso.getGuildMember() != null)
				UPDATE_GUILDMEMBER(_perso.getGuildMember());
			if(_perso.getMount() != null)
				UPDATE_MOUNT_INFOS(_perso.getMount());
			GameServer.addToLog("Personnage "+_perso.get_name()+" sauvegardé");
		}catch(Exception e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.out.println("Requete: "+baseQuery);
			System.out.println("Le personnage n'a pas été sauvegardé");
			System.exit(1);
		};
		if(saveItem)
		{
			baseQuery = "UPDATE `items` SET qua = ?, pos= ?, stats = ?"+
			" WHERE guid = ?;";
			try {
				p = newTransact(baseQuery, othCon);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			for(String idStr : _perso.getItemsIDSplitByChar(":").split(":"))
			{
				try
				{
					int guid = Integer.parseInt(idStr);
					Objet obj = World.getObjet(guid);
					if(obj == null)continue;
					
					p.setInt(1, obj.getQuantity());
					p.setInt(2, obj.getPosition());
					p.setString(3, obj.parseToSave());
					p.setInt(4, Integer.parseInt(idStr));
					
					p.execute();
				}catch(Exception e){continue;};
				
			}
			
			if(_perso.get_compte() == null)
				return;
			for(String idStr : _perso.getBankItemsIDSplitByChar(":").split(":"))
			{
				try
				{
					int guid = Integer.parseInt(idStr);
					Objet obj = World.getObjet(guid);
					if(obj == null)continue;
					
					p.setInt(1, obj.getQuantity());
					p.setInt(2, obj.getPosition());
					p.setString(3, obj.parseToSave());
					p.setInt(4, Integer.parseInt(idStr));
					
					p.execute();
				}catch(Exception e){continue;};
				
			}
		}
		
		closePreparedStatement(p);
	}

	public static void LOAD_SORTS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT  * from sorts;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("id");
				Sort sort = new Sort(id,RS.getInt("sprite"),RS.getString("spriteInfos"),RS.getString("effectTarget"));
				SortStats l1 = parseSortStats(id,1,RS.getString("lvl1"));
				SortStats l2 = parseSortStats(id,2,RS.getString("lvl2"));
				SortStats l3 = parseSortStats(id,3,RS.getString("lvl3"));
				SortStats l4 = parseSortStats(id,4,RS.getString("lvl4"));
				SortStats l5 = null;
				if(!RS.getString("lvl5").equalsIgnoreCase("-1"))
					l5 = parseSortStats(id,5,RS.getString("lvl5"));
				SortStats l6 = null;
				if(!RS.getString("lvl6").equalsIgnoreCase("-1"))
						l6 = parseSortStats(id,6,RS.getString("lvl6"));
				sort.addSortStats(1,l1);
				sort.addSortStats(2,l2);
				sort.addSortStats(3,l3);
				sort.addSortStats(4,l4);
				sort.addSortStats(5,l5);
				sort.addSortStats(6,l6);
				World.addSort(sort);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}

	public static void LOAD_OBJ_TEMPLATE()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT  * from item_template;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
					World.addObjTemplate
					(
						new ObjTemplate
						(
							RS.getInt("id"),
							RS.getString("statsTemplate"),
							RS.getString("name"),
							RS.getInt("type"),
							RS.getInt("level"),
							RS.getInt("pod"),
							RS.getInt("prix"),
							RS.getInt("panoplie"),
							RS.getString("condition"),
							RS.getString("armesInfos")
						)
					);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	
	private static SortStats parseSortStats(int id,int lvl,String str)
	{
		try
		{
			SortStats stats = null;
			String[] stat = str.split(",");
			String effets = stat[0];
			String CCeffets = stat[1];
			int PACOST = 6;
			try
			{
				PACOST = Integer.parseInt(stat[2].trim());
			}catch(NumberFormatException e){};
			
			int POm = Integer.parseInt(stat[3].trim());
			int POM = Integer.parseInt(stat[4].trim());
			int TCC = Integer.parseInt(stat[5].trim());
			int TEC = Integer.parseInt(stat[6].trim());
			boolean line = stat[7].trim().equalsIgnoreCase("true");
			boolean LDV = stat[8].trim().equalsIgnoreCase("true");
			boolean emptyCell = stat[9].trim().equalsIgnoreCase("true");
			boolean MODPO = stat[10].trim().equalsIgnoreCase("true");
			//int unk = Integer.parseInt(stat[11]);//All 0
			int MaxByTurn = Integer.parseInt(stat[12].trim());
			int MaxByTarget = Integer.parseInt(stat[13].trim());
			int CoolDown = Integer.parseInt(stat[14].trim());
			String type = stat[15].trim();
			int level = Integer.parseInt(stat[stat.length-2].trim());
			boolean endTurn = stat[19].trim().equalsIgnoreCase("true");
			stats = new SortStats(id,lvl,PACOST,POm, POM, TCC, TEC, line, LDV, emptyCell, MODPO, MaxByTurn, MaxByTarget, CoolDown, level, endTurn, effets, CCeffets,type);
			return stats;
		}catch(Exception e)
		{
			e.printStackTrace();
			int nbr = 0;
			System.out.println("[DEBUG]Sort "+id+" lvl "+lvl);
			for(String z:str.split(","))
			{
				System.out.println("[DEBUG]"+nbr+" "+z);
				nbr++;
			}
			System.exit(1);
			return null;
		}
	}

	public static void LOAD_MOB_TEMPLATE() {
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM monsters;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("id");
				int gfxID = RS.getInt("gfxID");
				int align = RS.getInt("align");
				String colors = RS.getString("colors");
				String grades = RS.getString("grades");
				String spells = RS.getString("spells");
				String stats = RS.getString("stats");
				String pdvs = RS.getString("pdvs");
				String pts = RS.getString("points");
				String inits = RS.getString("inits");
				int mK = RS.getInt("minKamas");
				int MK = RS.getInt("maxKamas");
				int IAType = RS.getInt("AI_Type");
				String xp = RS.getString("exps");
				boolean capturable = RS.getString("capturable").equalsIgnoreCase("true");
				//String drop = RS.getString("drop");
				World.addMobTemplate
				(
					id,
					new Monstre
					(
						id,
						gfxID,
						align,
						colors,
						grades,
						spells,
						stats,
						pdvs,
						pts,
						inits,
						mK,
						MK,
						xp,
						IAType,
						capturable
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}

	public static void LOAD_NPC_TEMPLATE()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_template;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("id");
				int bonusValue = RS.getInt("bonusValue");
				int gfxID = RS.getInt("gfxID");
				int scaleX = RS.getInt("scaleX");
				int scaleY = RS.getInt("scaleY");
				int sex = RS.getInt("sex");
				int color1 = RS.getInt("color1");
				int color2 = RS.getInt("color2");
				int color3 = RS.getInt("color3");
				String access = RS.getString("accessories");
				int extraClip = RS.getInt("extraClip");
				int customArtWork = RS.getInt("customArtWork");
				int initQId = RS.getInt("initQuestion");
				String ventes = RS.getString("ventes");
				World.addNpcTemplate
				(
					new NPC_tmpl
					(
						id,
						bonusValue,
						gfxID,
						scaleX,
						scaleY,
						sex,
						color1,
						color2,
						color3,
						access,
						extraClip,
						customArtWork,
						initQId,
						ventes
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}

	public static void SAVE_NEW_ITEM(Objet item)
	{
		try {
		String baseQuery = "REPLACE INTO `items` VALUES(?,?,?,?,?);";
		
		PreparedStatement p = newTransact(baseQuery, othCon);
		
		p.setInt(1,item.getGuid());
		p.setInt(2,item.getTemplate().getID());
		p.setInt(3,item.getQuantity());
		p.setInt(4,item.getPosition());
		p.setString(5,item.parseToSave());
		
		p.execute();
		closePreparedStatement(p);
		} catch (SQLException e) {e.printStackTrace();}
	}
	
	public static boolean SAVE_NEW_FIXGROUP(int mapID,int cellID,String groupData)
	{
		try {
		String baseQuery = "REPLACE INTO `mobgroups_fix` VALUES(?,?,?)";
		PreparedStatement p = newTransact(baseQuery, statCon);
		
		p.setInt(1, mapID);
		p.setInt(2, cellID);
		p.setString(3, groupData);
		
		p.execute();
		closePreparedStatement(p);
		
		return true;
		} catch (SQLException e) {e.printStackTrace();}
		return false;
	}
	public static void LOAD_NPC_QUESTIONS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_questions;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				World.addNPCQuestion
				(
					new NPC_question
					(
						RS.getInt("ID"),
						RS.getString("responses"),
						RS.getString("params"),
						RS.getString("cond"),
						RS.getInt("ifFalse")
					)
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}

	public static void LOAD_NPC_ANSWERS()
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_reponses_actions;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("ID");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if(World.getNPCreponse(id) == null)
					World.addNPCreponse(new NPC_reponse(id));
				World.getNPCreponse(id).addAction(new Action(type,args,""));
				
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
	}
	
	public static int LOAD_ENDFIGHT_ACTIONS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM endfight_action;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				Carte map = World.getCarte(RS.getInt("map"));
				if(map == null)continue;
				map.addEndFightAction(RS.getInt("fighttype"),
						new Action(RS.getInt("action"),RS.getString("args"),RS.getString("cond")));
				nbr++;
			}
			closeResultSet(RS);
			return nbr;
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
		return nbr;
	}
	
	public static int LOAD_ITEM_ACTIONS()
	{
		int nbr = 0;
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM use_item_actions;",Ancestra.STATIC_DB_NAME);
			while(RS.next())
			{
				int id = RS.getInt("template");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if(World.getObjTemplate(id) == null)continue;
				World.getObjTemplate(id).addAction(new Action(type,args,""));
				nbr++;
			}
			closeResultSet(RS);
			return nbr;
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		}
		return nbr;
	}
	
	public static void LOAD_ITEMS(String ids)
	{
		String req = "SELECT * FROM items WHERE guid IN ("+ids+");";
		try
		{
			ResultSet RS = SQLManager.executeQuery(req,Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				int guid 	= RS.getInt("guid");
				int tempID 	= RS.getInt("template");
				int qua 	= RS.getInt("qua");
				int pos		= RS.getInt("pos");
				String stats= RS.getString("stats");
				World.addObjet
				(
					World.newObjet
					(
						guid,
						tempID,
						qua,
						pos,
						stats
					),
					false
				);
			}
			closeResultSet(RS);
		}catch(SQLException e)
		{
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.out.println("Requete: \n"+req);
			System.exit(1);
		}
	}
	
	public static void LOAD_HDVS()
	{
		try
		{
			ResultSet RS = executeQuery("SELECT * FROM `hdvs` ORDER BY id ASC",Ancestra.OTHER_DB_NAME);
			
			while(RS.next())
			{
				World.addHdv(new Hdv(
								RS.getInt("map"),
								RS.getFloat("sellTaxe"),
								RS.getShort("sellTime"),
								RS.getShort("accountItem"),
								RS.getShort("lvlMax"),
								RS.getString("categories")));
				
			}
			
			RS = executeQuery("SELECT id MAX FROM `hdvs`",Ancestra.OTHER_DB_NAME);
			RS.first();
			World.setNextHdvID(RS.getInt("MAX"));
			
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void LOAD_HDVS_ITEMS()
	{
		try
		{
			long time1 = System.currentTimeMillis();	//TIME
			ResultSet RS = executeQuery("SELECT i.*"+
					" FROM `items` AS i,`hdvs_items` AS h"+
					" WHERE i.guid = h.itemID",Ancestra.OTHER_DB_NAME);
			
			//Load items
			while(RS.next())
			{
				int guid 	= RS.getInt("guid");
				int tempID 	= RS.getInt("template");
				int qua 	= RS.getInt("qua");
				int pos		= RS.getInt("pos");
				String stats= RS.getString("stats");
				World.addObjet
				(
					World.newObjet
					(
						guid,
						tempID,
						qua,
						pos,
						stats
					),
					false
				);
			}
			
			//Load HDV entry
			RS = executeQuery("SELECT * FROM `hdvs_items`",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				Hdv tempHdv = World.getHdv(RS.getInt("map"));
				if(tempHdv == null)continue;
				
				
				tempHdv.addEntry(new Hdv.HdvEntry(
										RS.getInt("price"),
										RS.getByte("count"),
										RS.getInt("ownerGuid"),
										World.getObjet(RS.getInt("itemID"))));
			}
			System.out.println (System.currentTimeMillis() - time1 + "ms pour loader les HDVS items");	//TIME
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void DELETE_ITEM(int guid)
	{
		String baseQuery = "DELETE FROM items WHERE guid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, guid);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}

	public static void CREATE_MOUNT(Dragodinde DD)
	{
		String baseQuery = "REPLACE INTO `mounts_data`(`id`,`color`,`sexe`,`name`,`xp`,`level`," +
				"`endurance`,`amour`,`maturite`,`serenite`,`reproductions`,`fatigue`,`items`," +
				"`ancetres`,`energie`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1,DD.get_id());
			p.setInt(2,DD.get_color());
			p.setInt(3,DD.get_sexe());
			p.setString(4,DD.get_nom());
			p.setLong(5,DD.get_exp());
			p.setInt(6,DD.get_level());
			p.setInt(7,DD.get_endurance());
			p.setInt(8,DD.get_amour());
			p.setInt(9,DD.get_maturite());
			p.setInt(10,DD.get_serenite());
			p.setInt(11,DD.get_reprod());
			p.setInt(12,DD.get_fatigue());
			p.setString(13,DD.getItemsId());
			p.setString(14,DD.get_ancetres());
			p.setInt(15,DD.get_energie());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void SAVE_ITEM(Objet item)
	{
		String baseQuery = "REPLACE INTO `items` VALUES (?,?,?,?,?);";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, item.getGuid());
			p.setInt(2, item.getTemplate().getID());
			p.setInt(3, item.getQuantity());
			p.setInt(4, item.getPosition());
			p.setString(5,item.parseToSave());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}	
	}

	public static void LOAD_ACCOUNT_BY_GUID(int user)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `guid` = '"+user+"';",Ancestra.OTHER_DB_NAME);
			
			String baseQuery = "UPDATE accounts " +
								"SET `reload_needed` = 0 " +
								"WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			while(RS.next())
			{
				//Si le compte est déjà connecté, on zap
				if(World.getCompte(RS.getInt("guid")) != null)if(World.getCompte(RS.getInt("guid")).isOnline())continue;
				
				Compte C = new Compte(
						RS.getInt("guid"),
						RS.getString("account"),
						RS.getString("pass"),
						RS.getString("pseudo"),
						RS.getString("question"),
						RS.getString("reponse"),
						RS.getInt("level"),
						(RS.getInt("banned") == 1),
						RS.getString("lastIP"),
						RS.getString("lastConnectionDate"),
						RS.getString("bank"),
						RS.getInt("bankKamas"),
						RS.getString("friends"),
						RS.getString("stable")
						);
				World.addAccount(C);
				World.ReassignAccountToChar(C);
				
				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}
			
			closePreparedStatement(p);
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	public static int LOAD_ACCOUNT_BY_PERSO(String persoName)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT account from personnages WHERE `name` LIKE '"+persoName+"';",Ancestra.OTHER_DB_NAME);
			
			int accID = -1;
			boolean found = RS.first();
			
			if(found)
				accID = RS.getInt("account");
			
			if(accID != -1)
				LOAD_ACCOUNT_BY_GUID(accID);
			
			closeResultSet(RS);
			return accID;
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		return -1;
	}
	private static void closeResultSet(ResultSet RS)
	{
		try {
			RS.getStatement().close();
			RS.close();
		} catch (SQLException e) {e.printStackTrace();}

		
	}
	private static void closePreparedStatement(PreparedStatement p)
	{
		try {
			p.clearParameters();
			p.close();
		} catch (SQLException e) {e.printStackTrace();}
	}

	public static void LOAD_ACCOUNT_BY_USER(String user)
	{
		try
		{
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `account` LIKE '"+user+"';",Ancestra.OTHER_DB_NAME);
			
			String baseQuery = "UPDATE accounts " +
								"SET `reload_needed` = 0 " +
								"WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			while(RS.next())
			{
				//Si le compte est déjà connecté, on zap
				if(World.getCompte(RS.getInt("guid")) != null)
					if(World.getCompte(RS.getInt("guid")).isOnline())
						continue;
				
				Compte C = new Compte(
						RS.getInt("guid"),
						RS.getString("account"),
						RS.getString("pass"),
						RS.getString("pseudo"),
						RS.getString("question"),
						RS.getString("reponse"),
						RS.getInt("level"),
						(RS.getInt("banned") == 1),
						RS.getString("lastIP"),
						RS.getString("lastConnectionDate"),
						RS.getString("bank"),
						RS.getInt("bankKamas"),
						RS.getString("friends"),
						RS.getString("stable")
						);
				World.addAccount(C);
				World.ReassignAccountToChar(C);
				
				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}
			
			closePreparedStatement(p);
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}

	public static void UPDATE_LASTCONNECTION_INFO(Compte compte)
	{
		String baseQuery = "UPDATE accounts SET " +
		"`lastIP` = ?," +
		"`lastConnectionDate` = ?" +
		" WHERE `guid` = ?;";
		
		try
		{
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setString(1, compte.get_curIP());
			p.setString(2, compte.getLastConnectionDate());
			p.setInt(3, compte.get_GUID());
			
			p.executeUpdate();
			closePreparedStatement(p);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			RealmServer.addToLog("Query: "+baseQuery);
			e.printStackTrace();
		}
	}
	
	public static void UPDATE_MOUNT_INFOS(Dragodinde DD)
	{
		String baseQuery = "UPDATE mounts_data SET " +
		"`name` = ?," +
		"`xp` = ?," +
		"`level` = ?," +
		"`endurance` = ?," +
		"`amour` = ?," +
		"`maturite` = ?," +
		"`serenite` = ?," +
		"`reproductions` = ?," +
		"`fatigue` = ?," +
		"`energie` = ?," +
		"`ancetres` = ?," +
		"`items` = ?" +
		" WHERE `id` = ?;";
		
		try
		{
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setString(1,DD.get_nom());
			p.setLong(2,DD.get_exp());
			p.setInt(3,DD.get_level());
			p.setInt(4,DD.get_endurance());
			p.setInt(5,DD.get_amour());
			p.setInt(6,DD.get_maturite());
			p.setInt(7,DD.get_serenite());
			p.setInt(8,DD.get_reprod());
			p.setInt(9,DD.get_fatigue());
			p.setInt(10,DD.get_energie());
			p.setString(11,DD.get_ancetres());
			p.setString(12,DD.getItemsId());
			p.setInt(13,DD.get_id());
			
			p.execute();
			closePreparedStatement(p);

		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Query: "+baseQuery);
			e.printStackTrace();
		}
	}

	public static void SAVE_MOUNTPARK(MountPark MP)
	{
		String baseQuery = "REPLACE INTO `mountpark_data`( `mapid` , `size` , `owner` , `guild` , `price` , `data` )" +
				" VALUES (?,?,?,?,?,?);";
				
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1,MP.get_map().get_id());
			p.setInt(2,MP.get_size());
			p.setInt(3,MP.get_owner());
			p.setInt(4,(MP.get_guild()==null?-1:MP.get_guild().get_id()));
			p.setInt(5,MP.get_price());
			p.setString(6,MP.parseData());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}

	public static boolean SAVE_TRIGGER(int mapID1, int cellID1, int action, int event,String args, String cond)
	{
		String baseQuery = "REPLACE INTO `scripted_cells`" +
				" VALUES (?,?,?,?,?,?);";
		
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1,mapID1);
			p.setInt(2,cellID1);
			p.setInt(3,action);
			p.setInt(4,event);
			p.setString(5,args);
			p.setString(6,cond);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	
	public static boolean REMOVE_TRIGGER(int mapID, int cellID)
	{
		String baseQuery = "DELETE FROM `scripted_cells` WHERE "+
							"`MapID` = ? AND "+
							"`CellID` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, mapID);
			p.setInt(2, cellID);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	
	public static boolean SAVE_MAP_DATA(Carte map)
	{
		String baseQuery = "UPDATE `maps` SET "+
		"`places` = ?, "+
		"`numgroup` = ? "+
		"WHERE id = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setString(1,map.get_placesStr());
			p.setInt(2, map.getMaxGroupNumb());
			p.setInt(3, map.get_id());
			
			p.executeUpdate();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean DELETE_NPC_ON_MAP(int m,int c)
	{
		String baseQuery = "DELETE FROM npcs WHERE mapid = ? AND cellid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, m);
			p.setInt(2, c);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	public static boolean ADD_NPC_ON_MAP(int m,int id,int c,int o)
	{
		String baseQuery = "INSERT INTO `npcs`" +
				" VALUES (?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, m);
			p.setInt(2, id);
			p.setInt(3, c);
			p.setInt(4, o);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}

	public static boolean ADD_ENDFIGHTACTION(int mapID, int type, int Aid,String args,String cond)
	{
		if(!DEL_ENDFIGHTACTION(mapID,type,Aid))return false;
		String baseQuery = "INSERT INTO `endfight_action` " +
				"VALUES (?,?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, mapID);
			p.setInt(2, type);
			p.setInt(3, Aid);
			p.setString(4,args);
			p.setString(5, cond);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}

	public static boolean DEL_ENDFIGHTACTION(int mapID, int type, int aid)
	{
		String baseQuery = "DELETE FROM `endfight_action` " +
				"WHERE map = ? AND " +
				"fighttype = ? AND " +
				"action = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, mapID);
			p.setInt(2, type);
			p.setInt(3, aid);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
			return false;
		}
	}

	public static void SAVE_NEWGUILD(Guild g)
	{
		String baseQuery = "INSERT INTO `guilds` " +
				"VALUES (?,?,?,1,0);";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, g.get_id());
			p.setString(2, g.get_name());
			p.setString(3, g.get_emblem());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void DEL_GUILD(int id)
	{
		String baseQuery = "DELETE FROM `guilds` " +
				"WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, id);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void DEL_GUILDMEMBER(int id)
	{
		String baseQuery = "DELETE FROM `guild_members` " +
				"WHERE `guid` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, id);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void UPDATE_GUILD(Guild g)
	{
		String baseQuery = "UPDATE `guilds` SET "+
		"`lvl` = ?,"+
		"`xp` = ?" +
		" WHERE id = ?;";
		
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1, g.get_lvl());
			p.setLong(2, g.get_xp());
			p.setInt(3, g.get_id());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static void UPDATE_GUILDMEMBER(GuildMember gm)
	{
		String baseQuery = "REPLACE INTO `guild_members` " +
						"VALUES(?,?,?,?,?,?,?,?,?,?,?);";
						
		try {
			PreparedStatement p = newTransact(baseQuery, othCon);
			p.setInt(1,gm.getGuid());
			p.setInt(2,gm.getGuild().get_id());
			p.setString(3,gm.getName());
			p.setInt(4,gm.getLvl());
			p.setInt(5,gm.getGfx());
			p.setInt(6,gm.getRank());
			p.setLong(7,gm.getXpGave());
			p.setInt(8,gm.getPXpGive());
			p.setInt(9,gm.getRights());
			p.setInt(10,gm.getAlign());
			p.setString(11,gm.getLastCo());
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
	}
	public static int isPersoInGuild(int guid)
	{
		int guildId = -1;
		
		try
		{
			ResultSet GuildQuery = SQLManager.executeQuery("SELECT guild FROM `guild_members` WHERE guid="+guid+";", Ancestra.OTHER_DB_NAME);
			
			boolean found = GuildQuery.first();
			
			if(found)
				guildId = GuildQuery.getInt("guild");
			
			closeResultSet(GuildQuery);
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		
		return guildId;
	}
	public static int[] isPersoInGuild(String name)
	{
		int guildId = -1;
		int guid = -1;
		try
		{
			ResultSet GuildQuery = SQLManager.executeQuery("SELECT guild,guid FROM `guild_members` WHERE name='"+name+"';", Ancestra.OTHER_DB_NAME);
			boolean found = GuildQuery.first();
			
			if(found)
			{
				guildId = GuildQuery.getInt("guild");
				guid = GuildQuery.getInt("guid");
			}
			
			closeResultSet(GuildQuery);
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		int[] toReturn = {guid,guildId};
		return toReturn;
	}
	
	public static boolean ADD_REPONSEACTION(int repID, int type, String args)
	{
		String baseQuery = "DELETE FROM `npc_reponses_actions` " +
						"WHERE `ID` = ? AND " +
						"`type` = ?;";
		PreparedStatement p; 
		try {
			p = newTransact(baseQuery, statCon);
			p.setInt(1, repID);
			p.setInt(2, type);
			
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		baseQuery = "INSERT INTO `npc_reponses_actions` " +
				"VALUES (?,?,?);";
		try {
			p = newTransact(baseQuery, statCon);
			p.setInt(1, repID);
			p.setInt(2, type);
			p.setString(3, args);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}

	public static boolean UPDATE_INITQUESTION(int id, int q)
	{
		String baseQuery = "UPDATE `npc_template` SET " +
							"`initQuestion` = ? " +
							"WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setInt(1, q);
			p.setInt(2, id);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}

	public static boolean UPDATE_NPCREPONSES(int id, String reps)
	{
		String baseQuery = "UPDATE `npc_questions` SET " +
							"`responses` = ? " +
							"WHERE `ID` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, statCon);
			p.setString(1, reps);
			p.setInt(2, id);
			
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
			GameServer.addToLog("Game: Query: "+baseQuery);
		}
		return false;
	}
	
	/*-------------LIGNE PAR MARTHIEUBEAN------------------*/
	//Ajoute des points (pour le site internet) au compte
	public static void addPoint(int _nombre,Compte _compte)
	{
		String baseQuery = "UPDATE accounts" +
				" SET point = point + ?" +
				" WHERE guid = ?";
		try
		{
			PreparedStatement p = newTransact(baseQuery, othCon);
			
			p.setInt(1, _nombre);
			p.setInt(2, _compte.get_GUID());
			
			p.executeUpdate();
			closePreparedStatement(p);
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}
	/*----------------------FIN----------------------------*/
	
	/*MARTHIEUBEAN*/
	
	public static void SAVE_HDVS_ITEMS(ArrayList<HdvEntry> liste)
	{
		long time1 = System.currentTimeMillis();	//TIME
		PreparedStatement queries = null;
		try
		{
			String emptyQuery = "TRUNCATE TABLE `hdvs_items`";
			PreparedStatement emptyTable = newTransact(emptyQuery, othCon);
			emptyTable.execute();
			
			String baseQuery = "INSERT INTO `hdvs_items` "+
								"(`map`,`ownerGuid`,`price`,`count`,`itemID`) "+
								"VALUES(?,?,?,?,?);";
			queries = newTransact(baseQuery, othCon);
			
			for(HdvEntry curEntry : liste)
			{
				if(curEntry.getOwner() == -1)continue;
				queries.setInt(1, curEntry.getHdvID());
				queries.setInt(2, curEntry.getOwner());
				queries.setInt(3, curEntry.getPrice());
				queries.setInt(4, curEntry.getAmount(false));
				queries.setInt(5, curEntry.getObjet().getGuid());
				
				queries.execute();
			}
			
			closePreparedStatement(queries);
			SAVE_HDV_AVGPRICE();
			System.out.println("Sauvegarde HDV en "+(System.currentTimeMillis()-time1)+"ms");	//TIME
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR:"+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void SAVE_HDV_AVGPRICE()
	{
		String baseQuery = "UPDATE `item_template`"+
							" SET sold = ?,avgPrice = ?"+
							" WHERE id = ?;";
		PreparedStatement queries = null;
		
		Map<Integer, ObjTemplate> templates = World.getObjTemplates();
		try
		{
			queries = newTransact(baseQuery, statCon);
			
			for(ObjTemplate curTemp : templates.values())
			{
				if(curTemp.getSold() == 0)
					continue;
				
				queries.setLong(1, curTemp.getSold());
				queries.setInt(2, curTemp.getAvgPrice());
				queries.setInt(3, curTemp.getID());
				queries.executeUpdate();
			}
			closePreparedStatement(queries);
		}catch(SQLException e)
		{
			GameServer.addToLog("SQL ERROR:"+e.getMessage());
			e.printStackTrace();
		}
	}
	public static void TIMER(boolean start)
	{
		if(start)
		{
			timerCommit = new Timer();
			timerCommit.schedule(new TimerTask() {
				
				public void run() {
					if(!needCommit)return;
					
					commitTransacts();
					needCommit = false;
					
				}
			}, Ancestra.CONFIG_DB_COMMIT, Ancestra.CONFIG_DB_COMMIT);
		}
		else
			timerCommit.cancel();
	}
	
	public static int getNextObjetID()
	{
		try
		{
			ResultSet RS = executeQuery("SELECT MAX(guid) AS max FROM items;",Ancestra.OTHER_DB_NAME);
			
			int guid = 0;
			boolean found = RS.first();
			
			if(found)
				guid = RS.getInt("max");
			
			closeResultSet(RS);
			return guid;
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
		return 0;
	}
	
	public static boolean needReloadAccount(String login)
	{
		boolean reload_needed = false;
		try
		{
			String query = "SELECT reload_needed " +
							"FROM accounts " +
							"WHERE account LIKE '" + login + "'" +
							";";
			ResultSet RS = executeQuery(query,Ancestra.OTHER_DB_NAME);
			
			boolean found = RS.first();
			
			if(found)
			{
				if(RS.getInt("reload_needed") == 1)
					reload_needed = true;
			}
			
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
		return reload_needed;
	}
	public static boolean persoExist(String name)
	{
		boolean exist = false;
		try
		{
			String query = "SELECT COUNT(*) AS exist " +
					"FROM personnages " +
					"WHERE name LIKE '" + name + "';";
			
			ResultSet RS = executeQuery(query,Ancestra.OTHER_DB_NAME);
			
			boolean found = RS.first();
			
			if(found)
			{
				if(RS.getInt("exist") != 0)
					exist = true;
			}
			
			closeResultSet(RS);
		}catch(SQLException e)
		{
			RealmServer.addToLog("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		return exist;
	}
}