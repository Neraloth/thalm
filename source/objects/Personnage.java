package objects;

import game.GameServer;
import game.GameThread.GameAction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.Timer;

import objects.Carte.*;
import objects.Fight.*;
import objects.Guild.GuildMember;
import objects.Metier.*;
import objects.Sort.SortStats;
import objects.Objet.ObjTemplate; //MARTHIEUBEAN

import common.Ancestra;
import common.Constants;
import common.Formulas;
import common.Logger;
import common.SQLManager;
import common.SocketManager;
import common.World;
import common.World.Exchange;
import common.World.ItemSet;

public class Personnage {
	
	private int _GUID;
	private String _name;
	private int _sexe;
	private int _classe;
	private int _color1;
	private int _color2;
	private int _color3;
	private long _kamas;
	private int _spellPts;
	private int _capital;
	private int _energy;
	private int _lvl;
	private long _curExp;
	private int _size;
	private int _gfxID;
	private int _isMerchant = 0;
	private int _orientation = 1;
	private Compte _compte;
	private int _accID;
	private boolean _canAggro = true;
	private String _emotes = "7667711";
	
	//Variables d'ali
	private byte _align = 0;
	private int _deshonor = 0;
	private int _honor = 0;
	private boolean _showWings = false;
	private int _aLvl = 0;
	//Fin ali
	
	private GuildMember _guildMember;
	private boolean _showFriendConnection;
	private String _canaux;
	private Stats _baseStats;
	private Fight _fight;
	private boolean _away;
	private Carte _curCarte;
	private Case _curCell;
	private int _PDV;
	private boolean _isInBank;
	private int _PDVMAX;
	private boolean _sitted;
	private boolean _ready = false;
	private boolean _isOnline  = false;
	private Group _group;
	private int _isTradingWith = 0;
	private Exchange _curExchange;
	private int _isTalkingWith = 0;
	private int _inviting = 0;
	private int _duelID = -1;
	private Map<Integer,SortStats> _sorts = new TreeMap<Integer,SortStats>();
	private Map<Integer,Character> _sortsPlaces = new TreeMap<Integer,Character>();
	
	private Map<Integer,SpellEffect> _buffs = new TreeMap<Integer,SpellEffect>(); 
	private Map<Integer,Objet> _items = new TreeMap<Integer,Objet>();
	private Map<Integer,StatsMetier> _metiers = new TreeMap<Integer,StatsMetier>();
	private Timer _sitTimer;
	private String _savePos;
	private int _exPdv;
	private MountPark _inMountPark;//Enclos
	private int _emoteActive = 0;
	private JobAction _curJobAction;
	private Dragodinde _mount;
	private int _mountXpGive = 0;
	private boolean _onMount = false;
	//Zaap
	private boolean _isZaaping = false;
	private ArrayList<Integer> _zaaps = new ArrayList<Integer>();
	private boolean _isZaapiing = false;
	//Disponibilité
	public boolean isAbsent = false;
	public boolean isInvisible = false;
	//Logger
	private Logger loggeur;
	//Interface d'oublie de sort
	private boolean isForgetingSpell = false;
	
	public static class Group
	{
		private ArrayList<Personnage> _persos = new ArrayList<Personnage>();
		private Personnage _chief;
		
		public Group(Personnage p1,Personnage p2)
		{
			_chief = p1;
			_persos.add(p1);
			_persos.add(p2);
		}
		
		public boolean isChief(int guid)
		{
			return _chief.get_GUID() == guid;
		}
		
		public void addPerso(Personnage p)
		{
			_persos.add(p);
		}
		
		public int getPersosNumber()
		{
			return _persos.size();
		}
		
		public int getGroupLevel()
		{
			int lvls = 0;
			for(Personnage p : _persos)
			{
				lvls += p.get_lvl();
			}
			return lvls;
		}
		
		public ArrayList<Personnage> getPersos()
		{
			return _persos;
		}

		public Personnage getChief()
		{
			return _chief;
		}

		public void leave(Personnage p)
		{
			if(!_persos.contains(p))return;
			p.setGroup(null);
			_persos.remove(p);
			if(_persos.size() == 1)
			{
				_persos.get(0).setGroup(null);
				if(_persos.get(0).get_compte() == null)return;
				SocketManager.GAME_SEND_PV_PACKET(_persos.get(0).get_compte().getGameThread().get_out(),"");
			}
			else
				SocketManager.GAME_SEND_PM_DEL_PACKET_TO_GROUP(this,p.get_GUID());
		}
	}
	
	public static class Stats
	{
		private Map<Integer,Integer> Effects = new TreeMap<Integer,Integer>();
		
		public Stats(boolean addBases,Personnage perso)
		{
			Effects = new TreeMap<Integer,Integer>();
			if(!addBases)return;
			Effects.put(Constants.STATS_ADD_PA,  perso.get_lvl()<100?6:7);
			Effects.put(Constants.STATS_ADD_PM, 3);
			Effects.put(Constants.STATS_ADD_PROS, perso.get_classe()==Constants.CLASS_ENUTROF?120:100);
			Effects.put(Constants.STATS_ADD_PODS, 1000);
			Effects.put(Constants.STATS_CREATURE, 1);
			Effects.put(Constants.STATS_ADD_INIT, 1);
		}
		public Stats(Map<Integer, Integer> stats, boolean addBases,Personnage perso)
		{
			Effects = stats;
			if(!addBases)return;
			Effects.put(Constants.STATS_ADD_PA, perso.get_lvl()<100?6:7);
			Effects.put(Constants.STATS_ADD_PM, 3);
			Effects.put(Constants.STATS_ADD_PROS, perso.get_classe()==Constants.CLASS_ENUTROF?120:100);
			Effects.put(Constants.STATS_ADD_PODS, 1000);
			Effects.put(Constants.STATS_CREATURE, 1);
			Effects.put(Constants.STATS_ADD_INIT, 1);
		}
		
		public Stats(Map<Integer, Integer> stats)
		{
			Effects = stats;
		}
		
		public Stats()
		{
			Effects = new TreeMap<Integer,Integer>();
		}
		
		public int addOneStat(int id, int val)
		{
			if(Effects.get(id) == null || Effects.get(id) == 0)
				Effects.put(id,val);
			else
			{
				int newVal = (Effects.get(id)+val);
				Effects.put(id, newVal);
			}
			return Effects.get(id);
		}
		
		/*---------------------LIGNE PAR MARTHIEUBEAN-------------------------*/
		//Utiliser pour remettre tout les stats de base à "0". Utiliser pour le restat sur le site entre autre
		public void resetStat()
		{
			setStat(Constants.STATS_ADD_INTE,0);
			setStat(Constants.STATS_ADD_FORC,0);
			setStat(Constants.STATS_ADD_AGIL,0);
			setStat(Constants.STATS_ADD_CHAN,0);
			setStat(Constants.STATS_ADD_SAGE,0);
			setStat(Constants.STATS_ADD_VITA,0);
		}
		public void setStat(int stat, int _nbr)
		{
			if(_nbr <= -1)return;
			if(Effects.get(stat) != null && Effects.get(stat) != _nbr)
				Effects.put(stat,_nbr);
		}
		/*-----------------------FIN---------------------------------------*/
		
		public boolean isSameStats(Stats other)
		{
			for(Entry<Integer,Integer> entry : Effects.entrySet())
			{
				//Si la stat n'existe pas dans l'autre map
				if(other.getMap().get(entry.getKey()) == null)return false;
				//Si la stat existe mais n'a pas la même valeur
				if(other.getMap().get(entry.getKey()) != entry.getValue())return false;	
			}
			for(Entry<Integer,Integer> entry : other.getMap().entrySet())
			{
				//Si la stat n'existe pas dans l'autre map
				if(Effects.get(entry.getKey()) == null)return false;
				//Si la stat existe mais n'a pas la même valeur
				if(Effects.get(entry.getKey()) != entry.getValue())return false;	
			}
			return true;
		}
		
		public int getEffect(int id)
		{
			int val;
			if(Effects.get(id) == null)
				 val=0;
			else
				val = Effects.get(id);
			
			switch(id)//Bonus/Malus TODO
			{
				case Constants.STATS_ADD_AFLEE:
					if(Effects.get(Constants.STATS_REM_AFLEE)!= null)
						val -= (int)(getEffect(Constants.STATS_REM_AFLEE));
					if(Effects.get(Constants.STATS_ADD_SAGE) != null)
						val += (int)(getEffect(Constants.STATS_ADD_SAGE)/4);
				break;
				case Constants.STATS_ADD_MFLEE:
					if(Effects.get(Constants.STATS_REM_MFLEE)!= null)
						val -= (int)(getEffect(Constants.STATS_REM_MFLEE));
					if(Effects.get(Constants.STATS_ADD_SAGE) != null)
						val += (int)(getEffect(Constants.STATS_ADD_SAGE)/4);
				break;
				case Constants.STATS_ADD_INIT:
					if(Effects.get(Constants.STATS_REM_INIT)!= null)
						val -= Effects.get(Constants.STATS_REM_INIT);
				break;
				case Constants.STATS_ADD_AGIL:
					if(Effects.get(Constants.STATS_REM_AGIL)!= null)
						val -= Effects.get(Constants.STATS_REM_AGIL);
				break;
				case Constants.STATS_ADD_FORC:
					if(Effects.get(Constants.STATS_REM_FORC)!= null)
						val -= Effects.get(Constants.STATS_REM_FORC);
				break;
				case Constants.STATS_ADD_CHAN:
					if(Effects.get(Constants.STATS_REM_CHAN)!= null)
						val -= Effects.get(Constants.STATS_REM_CHAN);
				break;
				case Constants.STATS_ADD_INTE:
					if(Effects.get(Constants.STATS_REM_INTE)!= null)
					val -= Effects.get(Constants.STATS_REM_INTE);
				break;
				case Constants.STATS_ADD_PA:
					if(Effects.get(Constants.STATS_ADD_PA2)!= null)
						val += Effects.get(Constants.STATS_ADD_PA2);
					if(Effects.get(Constants.STATS_REM_PA)!= null)
						val -= Effects.get(Constants.STATS_REM_PA);
					if(Effects.get(Constants.STATS_REM_PA2)!= null)//Non esquivable
						val -= Effects.get(Constants.STATS_REM_PA2);
				break;
				case Constants.STATS_ADD_PM:
					if(Effects.get(Constants.STATS_ADD_PM2)!= null)
						val += Effects.get(Constants.STATS_ADD_PM2);
					if(Effects.get(Constants.STATS_REM_PM)!= null)
						val -= Effects.get(Constants.STATS_REM_PM);
					if(Effects.get(Constants.STATS_REM_PM2)!= null)//Non esquivable
						val -= Effects.get(Constants.STATS_REM_PM2);
				break;
				case Constants.STATS_ADD_PO:
					if(Effects.get(Constants.STATS_REM_PO)!= null)
						val -= Effects.get(Constants.STATS_REM_PO);
				break;
				case Constants.STATS_ADD_VITA:
					if(Effects.get(Constants.STATS_REM_VITA)!= null)
						val -= Effects.get(Constants.STATS_REM_VITA);
				break;
				case Constants.STATS_ADD_DOMA:
					if(Effects.get(Constants.STATS_REM_DOMA)!= null)
						val -= Effects.get(Constants.STATS_REM_DOMA);
				break;
				case Constants.STATS_ADD_PODS:
					if(Effects.get(Constants.STATS_REM_PODS)!= null)
						val -= Effects.get(Constants.STATS_REM_PODS);
				break;
				case Constants.STATS_ADD_PROS:
					if(Effects.get(Constants.STATS_REM_PROS)!= null)
						val -= Effects.get(Constants.STATS_REM_PROS);
				break;
				case Constants.STATS_ADD_R_TER:
					if(Effects.get(Constants.STATS_REM_R_TER)!= null)
						val -= Effects.get(Constants.STATS_REM_R_TER);
				break;
				case Constants.STATS_ADD_R_EAU:
					if(Effects.get(Constants.STATS_REM_R_EAU)!= null)
						val -= Effects.get(Constants.STATS_REM_R_EAU);
				break;
				case Constants.STATS_ADD_R_AIR:
					if(Effects.get(Constants.STATS_REM_R_AIR)!= null)
						val -= Effects.get(Constants.STATS_REM_R_AIR);
				break;
				case Constants.STATS_ADD_R_FEU:
					if(Effects.get(Constants.STATS_REM_R_FEU)!= null)
						val -= Effects.get(Constants.STATS_REM_R_FEU);
				break;
				case Constants.STATS_ADD_R_NEU:
					if(Effects.get(Constants.STATS_REM_R_NEU)!= null)
						val -= Effects.get(Constants.STATS_REM_R_NEU);
				break;
				case Constants.STATS_ADD_RP_TER:
					if(Effects.get(Constants.STATS_REM_RP_TER)!= null)
						val -= Effects.get(Constants.STATS_REM_RP_TER);
				break;
				case Constants.STATS_ADD_RP_EAU:
					if(Effects.get(Constants.STATS_REM_RP_EAU)!= null)
						val -= Effects.get(Constants.STATS_REM_RP_EAU);
				break;
				case Constants.STATS_ADD_RP_AIR:
					if(Effects.get(Constants.STATS_REM_RP_AIR)!= null)
						val -= Effects.get(Constants.STATS_REM_RP_AIR);
				break;
				case Constants.STATS_ADD_RP_FEU:
					if(Effects.get(Constants.STATS_REM_RP_FEU)!= null)
						val -= Effects.get(Constants.STATS_REM_RP_FEU);
				break;
				case Constants.STATS_ADD_RP_NEU:
					if(Effects.get(Constants.STATS_REM_RP_NEU)!= null)
						val -= Effects.get(Constants.STATS_REM_RP_NEU);
				break;
			}
			return val;
		}
		
		public static Stats cumulStat(Stats s1,Stats s2)
		{
			TreeMap<Integer,Integer> effets = new TreeMap<Integer,Integer>();
			for(int a = 0; a <= Constants.MAX_EFFECTS_ID; a++)
			{
				if((s1.Effects.get(a) == null  || s1.Effects.get(a) == 0) && (s2.Effects.get(a) == null || s2.Effects.get(a) == 0))
					continue;
				int som = 0;
				if(s1.Effects.get(a) != null)
					som += s1.Effects.get(a);
				
				if(s2.Effects.get(a) != null)
					som += s2.Effects.get(a);
				
				effets.put(a, som);
			}
			return new Stats(effets,false,null);
		}
		
		public Map<Integer, Integer> getMap()
		{
			return Effects;
		}
		public String parseToItemSetStats()
		{
			String str= "";
			for(Entry<Integer,Integer> entry : Effects.entrySet())
			{
				if(str.length() >0)str += ",";
				str += Integer.toHexString(entry.getKey())+"#"+Integer.toHexString(entry.getValue())+"#0#0";
			}
			return str;
		}
	}
	
	public Personnage(int _guid, String _name, int _sexe, int _classe,
			int _color1, int _color2, int _color3,long _kamas, int pts, int _capital, int _energy, int _lvl, long exp,
			int _size, int _gfxid, byte alignement, int _compte, Map<Integer,Integer> stats,
			int seeFriend,String canaux, int map, int cell,String stuff,int pdvPer,String spells, String savePos,String jobs,
			int mountXp,int mount,int honor,int deshonor,int alvl,String z)
	{
		this._GUID = _guid;
		this._name = _name;
		this._sexe = _sexe;
		this._classe = _classe;
		this._color1 = _color1;
		this._color2 = _color2;
		this._color3 = _color3;
		this._kamas = _kamas;
		this._spellPts = pts;
		this._capital = _capital;
		this._align = alignement;
		this._honor = honor;
		this._deshonor = deshonor;
		this._aLvl = alvl;
		this._energy = _energy;
		this._lvl = _lvl;
		this._curExp = exp;
		if(mount != -1)this._mount = World.getDragoByID(mount);
		this._size = _size;
		this._gfxID = _gfxid;
		this._mountXpGive = mountXp;
		this._baseStats = new Stats(stats,true,this);
		this._accID = _compte;
		this._compte = World.getCompte(_compte);
		this._showFriendConnection = seeFriend==1;
		this._canaux = canaux;
		this._curCarte = World.getCarte(map);
		this._savePos = savePos;
		if(_curCarte == null && World.getCarte(Ancestra.CONFIG_START_MAP) != null)
		{
			this._curCarte = World.getCarte(Ancestra.CONFIG_START_MAP);
			this._curCell = _curCarte.getCase(Ancestra.CONFIG_START_CELL);
		}else if (_curCarte == null && World.getCarte(Ancestra.CONFIG_START_MAP) == null)
		{
			GameServer.addToLog("Personnage mal positione, et position de départ non valide. Fermeture du serveur.");
			Ancestra.closeServers();
		}
		else if(_curCarte != null)
		{
			this._curCell = _curCarte.getCase(cell);
			if(_curCell == null)
			{
				this._curCarte = World.getCarte(Ancestra.CONFIG_START_MAP);
				this._curCell = _curCarte.getCase(Ancestra.CONFIG_START_CELL);
			}
		}
		for(String str : z.split(","))
		{
			try
			{
				_zaaps.add(Integer.parseInt(str));
			}catch(Exception e){};
		}
		if(_curCarte == null || _curCell == null)
		{
			GameServer.addToLog("Map ou case de départ du personnage "+_name+" invalide");
			GameServer.addToLog("Map ou case par défaut invalide");
			GameServer.addToLog("Le serveur ne peut se lancer");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
			Ancestra.closeServers();
		}

		if(!stuff.equals(""))
		{
			if(stuff.charAt(stuff.length()-1) == '|')
				stuff = stuff.substring(0,stuff.length()-1);
			SQLManager.LOAD_ITEMS(stuff.replace("|",","));
		}
		for(String item : stuff.split("\\|"))
		{
			if(item.equals(""))continue;
			String[] infos = item.split(":");
			int guid = Integer.parseInt(infos[0]);
			Objet obj = World.getObjet(guid);
			if( obj == null)continue;
			_items.put(obj.getGuid(), obj);
		}
		
		this._PDVMAX = (_lvl-1)*5+Constants.getBasePdv(_classe)+getTotalStats().getEffect(Constants.STATS_ADD_VITA);
		this._PDV = (_PDVMAX*pdvPer)/100;
		parseSpells(spells);

		_sitTimer = new Timer(2000,new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				regenLife();
			}
		});
		_exPdv = _PDV;
		
		
		//Chargement des métiers
		if(!jobs.equals(""))
		{
			for(String aJobData : jobs.split(";"))
			{
				String[] infos = aJobData.split(",");
				try
				{
					int jobID = Integer.parseInt(infos[0]);
					long xp = Long.parseLong(infos[1]);
					Metier m = World.getMetier(jobID);
					StatsMetier SM = _metiers.get(learnJob(m));
					SM.addXp(this, xp);
				}catch(Exception e){e.getStackTrace();}
			}
		}
	}

	public void regenLife()
	{
		//Joueur pas en jeu
		if(_curCarte == null)return;
		//Pas de regen en combat
		if(_fight != null)return;
		//Déjà Full PDV
		if(_PDV == _PDVMAX)return;
		_PDV++;
	}
	
	public static Personnage CREATE_PERSONNAGE(String name, int sexe, int classe, int color1, int color2, int color3,Compte compte)
	{
		String z = "";
		if(Ancestra.CONFIG_ZAAP_ANK)
		{
			for(int[] i : Constants.AMAKNA_ZAAPS)
			{
				if(z.length() != 0)z+=",";
				z += i[0];
			}
		}
		if(Ancestra.CONFIG_ZAAP_INC)
		{
			for(int[] i : Constants.INCARNAM_ZAAPS)
			{
				if(z.length() != 0)z+=",";
				z += i[0];
			}
		}
		Personnage perso = new Personnage(
				SQLManager.getNextPersonnageGuid(),
				name,
				sexe,
				classe,
				color1,
				color2,
				color3,
				Ancestra.CONFIG_START_KAMAS,
				((Ancestra.CONFIG_START_LEVEL-1)*1),
				((Ancestra.CONFIG_START_LEVEL-1)*5),
				10000,
				Ancestra.CONFIG_START_LEVEL,
				World.getPersoXpMin(Ancestra.CONFIG_START_LEVEL),
				100,
				Integer.parseInt(classe+""+sexe),
				(byte)0,
				compte.get_GUID(),
				new TreeMap<Integer,Integer>(),
				1,
				"*#%!pi$:?",
				Constants.getStartMap(classe),
				Constants.getStartCell(classe),
				"",
				100,
				"",
				"10298,314",
				"",
				0,
				-1,
				0,
				0,
				0,
				z
				);
		perso._sorts = Constants.getStartSorts(classe);
		for(int a = 1; a <= perso.get_lvl();a++)
		{
			Constants.onLevelUpSpells(perso, a);
		}
		perso._sortsPlaces = Constants.getStartSortsPlaces(classe);
		
		/*---------------LIGNE PAR MARTHIEUBEAN-------------------*/
		//Ajoute un objet à la création du personnage
		if(Ancestra.CONFIG_START_ITEM != 0)
		{
			ObjTemplate t = World.getObjTemplate(Ancestra.CONFIG_START_ITEM);
							
			Objet obj = t.createNewItem(1,false); //Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
			if(perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
				World.addObjet(obj,true);
		}
		/*----------------------FIN-------------------------------*/
		
		if(!SQLManager.ADD_PERSO_IN_BDD(perso))
			return null;
		
		World.addPersonnage(perso);
	
		return perso;
	}

	public void set_Online(boolean d)
	{
		_isOnline = d;
	}
	
	public boolean isOnline()
	{
		return _isOnline;
	}
	
	public void setGroup(Group g)
	{
		_group = g;
	}

	public Group getGroup()
	{
		return _group;
	}
	
	public String parseSpellToDB()
	{
		String sorts = "";
		if(_sorts.size() == 0)return "";
		for(int key : _sorts.keySet())
		{
			//3;1;a,4;3;b
			SortStats SS = _sorts.get(key);
			sorts += SS.getSpellID()+";"+SS.getLevel()+";";
			if(_sortsPlaces.get(key)!=null)
				sorts += _sortsPlaces.get(key);
			else
				sorts += "_";
			sorts += ",";
		}
		sorts = sorts.substring(0, sorts.length()-1);
		return sorts;
	}
	
	private void parseSpells(String str)
	{
		String[] spells = str.split(",");
		for(String e : spells)
		{
			try
			{
				int id = Integer.parseInt(e.split(";")[0]);
				int lvl = Integer.parseInt(e.split(";")[1]);
				char place = e.split(";")[2].charAt(0);
				learnSpell(id,lvl,false,false);
				_sortsPlaces.put(id, place);
			}catch(NumberFormatException e1){continue;};
		}
	}
	
	public String get_savePos() {
		return _savePos;
	}

	public void set_savePos(String savePos) {
		_savePos = savePos;
	}

	public int get_isTradingWith() {
		return _isTradingWith;
	}

	public void set_isTradingWith(int tradingWith) {
		_isTradingWith = tradingWith;
	}

	public int get_isTalkingWith() {
		return _isTalkingWith;
	}

	public void set_isTalkingWith(int talkingWith) {
		_isTalkingWith = talkingWith;
	}

	public long get_kamas() {
		return _kamas;
	}

	public Map<Integer, SpellEffect> get_buff() {
		return _buffs;
	}

	public void set_kamas(long newKamas) {
		this._kamas = newKamas;
	}

	public Compte get_compte() {
		return _compte;
	}

	public int get_spellPts() {
		return _spellPts;
	}

	public void set_spellPts(int pts) {
		_spellPts = pts;
	}

	public Guild get_guild()
	{
		if(_guildMember == null)return null;
		return _guildMember.getGuild();
	}

	public void setGuildMember(GuildMember _guild) {
		this._guildMember = _guild;
	}
	
	public boolean is_ready() {
		return _ready;
	}

	public void set_ready(boolean _ready) {
		this._ready = _ready;
	}

	public int get_duelID() {
		return _duelID;
	}

	public Fight get_fight() {
		return _fight;
	}

	public void set_duelID(int _duelid) {
		_duelID = _duelid;
	}

	public int get_energy() {
		return _energy;
	}

	public boolean is_showFriendConnection() {
		return _showFriendConnection;
	}

	public String get_canaux() {
		return _canaux;
	}

	public void set_energy(int _energy) {
		this._energy = _energy;
	}

	public int get_lvl() {
		return _lvl;
	}

	public void set_lvl(int _lvl) {
		this._lvl = _lvl;
	}

	public long get_curExp() {
		return _curExp;
	}

	public Carte.Case get_curCell() {
		return _curCell;
	}

	public void set_curCell(Carte.Case cell) {
		_curCell = cell;
	}

	public void set_curExp(int exp) {
		_curExp = exp;
	}

	public int get_size() {
		return _size;
	}

	public void set_size(int _size) {
		this._size = _size;
	}

	public void set_fight(Fight _fight) {
		this._fight = _fight;
	}

	public int get_gfxID() {
		return _gfxID;
	}

	public void set_gfxID(int _gfxid) {
		_gfxID = _gfxid;
	}

	public int get_GUID() {
		return _GUID;
	}

	public Carte get_curCarte() {
		return _curCarte;
	}

	public String get_name() {
		return _name;
	}

	public boolean is_away() {
		return _away;
	}

	public void set_away(boolean _away) {
		this._away = _away;
	}

	public boolean isSitted() {
		return _sitted;
	}

	public int get_sexe() {
		return _sexe;
	}

	public int get_classe() {
		return _classe;
	}

	public int get_color1() {
		return _color1;
	}

	public int get_color2() {
		return _color2;
	}

	public Stats get_baseStats() {
		return _baseStats;
	}

	public int get_color3() {
		return _color3;
	}

	public int get_capital() {
		return _capital;
	}
	
	public boolean learnSpell(int spellID,int level,boolean save,boolean send)
	{
		if(World.getSort(spellID).getStatsByLevel(level)==null)
		{
			GameServer.addToLog("[ERROR]Sort "+spellID+" lvl "+level+" non trouvé.");
			return false;
		}
		_sorts.put(spellID, World.getSort(spellID).getStatsByLevel(level));
		if(send)
		{
			SocketManager.GAME_SEND_SPELL_LIST(this);
			SocketManager.GAME_SEND_Im_PACKET(this, "03;"+spellID);
		}
		if(save)SQLManager.SAVE_PERSONNAGE(this,false);
		return true;
	}
	
	public boolean boostSpell(int spellID)
	{
		if(getSortStatBySortIfHas(spellID)== null)
		{
			GameServer.addToLog(_name+" n'a pas le sort "+spellID);
			return false;
		}
		int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
		if(AncLevel == 6)return false;
		if(_spellPts>=AncLevel && World.getSort(spellID).getStatsByLevel(AncLevel+1).getReqLevel() <= _lvl)
		{
			if(learnSpell(spellID,AncLevel+1,true,false))
			{
				_spellPts -= AncLevel;
				SQLManager.SAVE_PERSONNAGE(this,false);
				return true;
			}else
			{
				GameServer.addToLog(_name+" : Echec LearnSpell "+spellID);
				return false;
			}
		}
		else//Pas le niveau ou pas les Points
		{
			if(_spellPts<AncLevel)
				GameServer.addToLog(_name+" n'a pas les points requis pour booster le sort "+spellID+" "+_spellPts+"/"+AncLevel);
			if(World.getSort(spellID).getStatsByLevel(AncLevel+1).getReqLevel() > _lvl)
				GameServer.addToLog(_name+" n'a pas le niveau pour booster le sort "+spellID+" "+_lvl+"/"+World.getSort(spellID).getStatsByLevel(AncLevel+1).getReqLevel());
			return false;
		}
	}
	
	public boolean forgetSpell(int spellID)
	{
		if(getSortStatBySortIfHas(spellID)== null)
		{
			GameServer.addToLog(_name+" n'a pas le sort "+spellID);
			return false;
		}
		int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
		if(AncLevel <= 1)return false;
		
		if(learnSpell(spellID,1,true,false))
		{
			_spellPts += Formulas.spellCost(AncLevel);
			
			SQLManager.SAVE_PERSONNAGE(this,false);
			return true;
		}else
		{
			GameServer.addToLog(_name+" : Echec LearnSpell "+spellID);
			return false;
		}
		
	}
	
	public String parseSpellList()
	{
		String packet = "SL";
		for (Iterator<SortStats> i = _sorts.values().iterator() ; i.hasNext();)
		{
		    SortStats SS = i.next();
			packet += SS.getSpellID()+"~"+SS.getLevel()+"~"+_sortsPlaces.get(SS.getSpellID())+";";
		}
		return packet;
	}

	public void set_SpellPlace(int SpellID, char Place)
		
		{
			replace_SpellInBook(Place);
			_sortsPlaces.remove(SpellID);	
			_sortsPlaces.put(SpellID, Place);
			SQLManager.SAVE_PERSONNAGE(this,false);//On sauvegarde les changements
		}

	private void replace_SpellInBook(char Place)
	{
		for(int key : _sorts.keySet())
		{
			if(_sortsPlaces.get(key)!=null)
			{
				if (_sortsPlaces.get(key).equals(Place))
					{
					_sortsPlaces.remove(key);
					}
			}
		}
	}
	
	public SortStats getSortStatBySortIfHas(int spellID)
	{
		return _sorts.get(spellID);
	}
	
	public String parseALK()
	{
		String perso = "";
		perso += this._GUID+";";
		perso += this._name+";";
		perso += this._lvl+";";
		perso += this._gfxID+";";
		perso += (this._color1!= -1?Integer.toHexString(this._color1):"-1")+";";
		perso += (this._color2!= -1?Integer.toHexString(this._color2):"-1")+";";
		perso += (this._color3!= -1?Integer.toHexString(this._color3):"-1")+";";
		perso += getGMStuffString()+ ";";
		perso += this._isMerchant+";";//merchant
		perso += "1;";
		perso += ";";//DeathCount	this.deathCount;
		perso += ";";//LevelMax
		return "|"+perso;
	}

	/*public void remove()
	{
		SQLManager.DELETE_PERSO_IN_BDD(this);
	}*/

	public void OnJoinGame()
	{
		if(_compte.getGameThread() == null)return;
		PrintWriter out = _compte.getGameThread().get_out();
		_compte.setCurPerso(this);
		_isOnline = true;
		
		if(_mount != null)SocketManager.GAME_SEND_Re_PACKET(this,"+",_mount);
		SocketManager.GAME_SEND_Rx_PACKET(this);
		
		SocketManager.GAME_SEND_ASK(out, this);
		//Envoie des bonus pano si besoin
		for(int a = 1;a<World.getItemSetNumber();a++)
		{
			int num =getNumbEquipedItemOfPanoplie(a);
			if(num == 0)continue;
			SocketManager.GAME_SEND_OS_PACKET(this, a);
		}
		
		//envoie des données de métier
		if(_metiers.size() >0)
		{
			ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
			list.addAll(_metiers.values());
			//packet JS
			SocketManager.GAME_SEND_JS_PACKET(this, list);
			//packet JX
			SocketManager.GAME_SEND_JX_PACKET(this, list);
			//Packet JO (Job Option)
			SocketManager.GAME_SEND_JO_PACKET(this, list);
			Objet obj = getObjetByPos(Constants.ITEM_POS_ARME);
			if(obj != null)
			{
				for(StatsMetier sm : list)
					if(sm.getTemplate().isValidTool(obj.getTemplate().getID()))
						SocketManager.GAME_SEND_OT_PACKET(_compte.getGameThread().get_out(),sm.getTemplate().getId());
			}
		}
		//Fin métier
		SocketManager.GAME_SEND_ALIGNEMENT(out, _align);
		SocketManager.GAME_SEND_ADD_CANAL(out,_canaux+"^"+(_compte.get_gmLvl()>0?"@¤":""));
		if(_guildMember != null)SocketManager.GAME_SEND_gS_PACKET(this,_guildMember);
		SocketManager.GAME_SEND_ZONE_ALLIGN_STATUT(out);
		SocketManager.GAME_SEND_SPELL_LIST(this);
		SocketManager.GAME_SEND_EMOTE_LIST(this,_emotes,"0");
		SocketManager.GAME_SEND_RESTRICTIONS(out);
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SocketManager.GAME_SEND_SEE_FRIEND_CONNEXION(out,_showFriendConnection);
		
		//Messages de bienvenue
		SocketManager.GAME_SEND_Im_PACKET(this, "189");
		if(!_compte.getLastConnectionDate().equals("") && !_compte.get_lastIP().equals(""))
			SocketManager.GAME_SEND_Im_PACKET(this, "0152;"+_compte.getLastConnectionDate()+"~"+_compte.get_lastIP());
		SocketManager.GAME_SEND_Im_PACKET(this, "0153;"+_compte.get_curIP());
		//Fin messages
		//Actualisation de l'ip
		_compte.setLastIP(_compte.get_curIP());
		
		//Mise a jour du lastConnectionDate
		Date actDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");
		String jour = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("MM");
		String mois = dateFormat.format(actDate);	
		dateFormat = new SimpleDateFormat("yyyy");
		String annee = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("HH");
		String heure = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("mm");
		String min = dateFormat.format(actDate);
		_compte.setLastConnectionDate(annee+"~"+mois+"~"+jour+"~"+heure+"~"+min);
		if(_guildMember != null)
			_guildMember.setLastCo(annee+"~"+mois+"~"+jour+"~"+heure+"~"+min);
		
		//Actualisation dans la DB
		SQLManager.UPDATE_LASTCONNECTION_INFO(_compte);
		
		if(!Ancestra.CONFIG_MOTD.equals(""))//Si le motd est notifié
		{
			String color = Ancestra.CONFIG_MOTD_COLOR;
			if(color.equals(""))color = "000000";//Noir
			
			SocketManager.GAME_SEND_MESSAGE(this, Ancestra.CONFIG_MOTD, color);
		}
		//on démarre le Timer pour la Regen de Pdv
		_sitTimer.start();
		//on le demarre coté client
		SocketManager.GAME_SEND_ILS_PACKET(this, 2000);
		
		initLogger();
	}
	
	public void sendGameCreate()
	{
		PrintWriter out = _compte.getGameThread().get_out();
		SocketManager.GAME_SEND_GAME_CREATE(out,_name);
		SocketManager.GAME_SEND_STATS_PACKET(this);
		SocketManager.GAME_SEND_MAPDATA(out,_curCarte.get_id(),_curCarte.get_date(),_curCarte.get_key());
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT(out,this.get_curCarte());
		_curCarte.addPlayer(this);
		
	}
	
	public String parseToOa()
	{
		String packetOa = "Oa";
		
		packetOa += _GUID + "|";
		packetOa += getGMStuffString();
			
		return packetOa;
	}
	
	public String parseToGM()
	{
		String str = "";
		
		/*String name = "";
		if(showRank && _compte.get_gmLvl() > 0)
			name += "[" + Constants.getRankName(_compte.get_gmLvl()) + "]";
		name += _name;*/
		
		if(_fight == null)// Hors combat
		{
			str+= _curCell.getID()+";";
			str+= _orientation+";";
			str+= "0"+";";//FIXME:Bonus Value ... ?
			str+= _GUID+";";
			str+= _name+";";
			str+= _classe+";";
			str+= _gfxID+"^"+_size+";";//gfxID^size
			str+= _sexe+";";
			str+= _align+",";//1,0,0,4055064
			str+= "0,";
			str+= (_showWings?getGrade():"0")+",";
			str+= _GUID+";";
			str+= (_color1==-1?"-1":Integer.toHexString(_color1))+";";
			str+= (_color2==-1?"-1":Integer.toHexString(_color2))+";";
			str+= (_color3==-1?"-1":Integer.toHexString(_color3))+";";
			str+= getGMStuffString()+";";
			str+= (_lvl>99?(_lvl>199?(2):(1)):(0))+";";//Aura
			str+= ";";//Emote
			str+= ";";//Emote timer
			if(this._guildMember!=null && this._guildMember.getGuild().getMembers().size()>9)
			{
				str += this._guildMember.getGuild().get_name()+";"+this._guildMember.getGuild().get_emblem()+";";
			}
			else str+= ";;";
			str+= /* Restriction */ "0;";//TODO: Restriction
			str+= (_onMount&&_mount!=null?_mount.get_color():"")+";";
			str+= ";";
		}
		return str;
	}
	
	public String getGMStuffString()
	{
		String str = "";
		if(getObjetByPos(Constants.ITEM_POS_ARME) != null)
		 	str+= Integer.toHexString(getObjetByPos(Constants.ITEM_POS_ARME).getTemplate().getID());	
		str+= ",";
		if(getObjetByPos(Constants.ITEM_POS_COIFFE) != null)
		 	str+= Integer.toHexString(getObjetByPos(Constants.ITEM_POS_COIFFE).getTemplate().getID());	
		str+= ",";
		if(getObjetByPos(Constants.ITEM_POS_CAPE) != null)
		 	str+= Integer.toHexString(getObjetByPos(Constants.ITEM_POS_CAPE).getTemplate().getID());	
		str+= ",";
		if(getObjetByPos(Constants.ITEM_POS_FAMILIER) != null)
		 	str+= Integer.toHexString(getObjetByPos(Constants.ITEM_POS_FAMILIER).getTemplate().getID());	
		str+= ",";
		if(getObjetByPos(Constants.ITEM_POS_BOUCLIER) != null)
		 	str+= Integer.toHexString(getObjetByPos(Constants.ITEM_POS_BOUCLIER).getTemplate().getID());	
		return str;
	}

	public String getAsPacket()
	{
		refreshStats();
		String ASData = "As";
		ASData += xpString(",")+"|";
		ASData += _kamas+"|"+_capital+"|"+_spellPts+"|";
		ASData += _align+"~"+_align+","+_aLvl+","+getGrade()+","+_honor+","+_deshonor+","+(_showWings?"1":"0")+"|";
		int pdv = get_PDV();
		int pdvMax = get_PDVMAX();
		if(_fight != null)
		{
			Fighter f = _fight.getFighterByPerso(this);
			if(f!= null)
			{
				pdv = f.getPDV();
				pdvMax = f.getPDVMAX();
			}
		}
		ASData += pdv+","+pdvMax+"|";
		ASData += _energy+",10000|";
		
		ASData += getInitiative()+"|";
		ASData += getTotalStats().getEffect(Constants.STATS_ADD_PROS)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_PA)+","+getStuffStats().getEffect(Constants.STATS_ADD_PA)+","+getDonsStats().getEffect(Constants.STATS_ADD_PA)+","+getBuffsStats().getEffect(Constants.STATS_ADD_PA)+","+getTotalStats().getEffect(Constants.STATS_ADD_PA)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_PM)+","+getStuffStats().getEffect(Constants.STATS_ADD_PM)+","+getDonsStats().getEffect(Constants.STATS_ADD_PM)+","+getBuffsStats().getEffect(Constants.STATS_ADD_PM)+","+getTotalStats().getEffect(Constants.STATS_ADD_PM)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_FORC)+","+ getStuffStats().getEffect(Constants.STATS_ADD_FORC)+","+getDonsStats().getEffect(Constants.STATS_ADD_FORC)+","+getBuffsStats().getEffect(Constants.STATS_ADD_FORC)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_VITA)+","+ getStuffStats().getEffect(Constants.STATS_ADD_VITA)+","+getDonsStats().getEffect(Constants.STATS_ADD_VITA)+","+getBuffsStats().getEffect(Constants.STATS_ADD_VITA)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_SAGE)+","+ getStuffStats().getEffect(Constants.STATS_ADD_SAGE)+","+getDonsStats().getEffect(Constants.STATS_ADD_SAGE)+","+getBuffsStats().getEffect(Constants.STATS_ADD_SAGE)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_CHAN)+","+ getStuffStats().getEffect(Constants.STATS_ADD_CHAN)+","+getDonsStats().getEffect(Constants.STATS_ADD_CHAN)+","+getBuffsStats().getEffect(Constants.STATS_ADD_CHAN)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_AGIL)+","+ getStuffStats().getEffect(Constants.STATS_ADD_AGIL)+","+getDonsStats().getEffect(Constants.STATS_ADD_AGIL)+","+getBuffsStats().getEffect(Constants.STATS_ADD_AGIL)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_INTE)+","+ getStuffStats().getEffect(Constants.STATS_ADD_INTE)+","+getDonsStats().getEffect(Constants.STATS_ADD_INTE)+","+getBuffsStats().getEffect(Constants.STATS_ADD_INTE)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_PO)+","+ getStuffStats().getEffect(Constants.STATS_ADD_PO)+","+getDonsStats().getEffect(Constants.STATS_ADD_PO)+","+getBuffsStats().getEffect(Constants.STATS_ADD_PO)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_CREATURE)+","+ getStuffStats().getEffect(Constants.STATS_CREATURE)+","+getDonsStats().getEffect(Constants.STATS_CREATURE)+","+getBuffsStats().getEffect(Constants.STATS_CREATURE)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_DOMA)+","+ getStuffStats().getEffect(Constants.STATS_ADD_DOMA)+","+getDonsStats().getEffect(Constants.STATS_ADD_DOMA)+","+getBuffsStats().getEffect(Constants.STATS_ADD_DOMA)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_PDOM)+","+ getStuffStats().getEffect(Constants.STATS_ADD_PDOM)+","+getDonsStats().getEffect(Constants.STATS_ADD_PDOM)+","+getBuffsStats().getEffect(Constants.STATS_ADD_PDOM)+"|";
		ASData += 0+","+0+","+0+","+0+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_PERDOM)+","+ getStuffStats().getEffect(Constants.STATS_ADD_PERDOM)+","+getDonsStats().getEffect(Constants.STATS_ADD_PERDOM)+","+getBuffsStats().getEffect(Constants.STATS_ADD_PERDOM)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_SOIN)+","+ getStuffStats().getEffect(Constants.STATS_ADD_SOIN)+","+getDonsStats().getEffect(Constants.STATS_ADD_SOIN)+","+getBuffsStats().getEffect(Constants.STATS_ADD_SOIN)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_TRAPDOM)+","+ getStuffStats().getEffect(Constants.STATS_TRAPDOM)+","+getDonsStats().getEffect(Constants.STATS_TRAPDOM)+","+getBuffsStats().getEffect(Constants.STATS_TRAPDOM)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_TRAPPER)+","+ getStuffStats().getEffect(Constants.STATS_TRAPPER)+","+getDonsStats().getEffect(Constants.STATS_TRAPPER)+","+getBuffsStats().getEffect(Constants.STATS_TRAPPER)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_RETDOM)+","+ getStuffStats().getEffect(Constants.STATS_RETDOM)+","+getDonsStats().getEffect(Constants.STATS_RETDOM)+","+getBuffsStats().getEffect(Constants.STATS_RETDOM)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_CC)+","+ getStuffStats().getEffect(Constants.STATS_ADD_CC)+","+getDonsStats().getEffect(Constants.STATS_ADD_CC)+","+getBuffsStats().getEffect(Constants.STATS_ADD_CC)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_EC)+","+ getStuffStats().getEffect(Constants.STATS_ADD_EC)+","+getDonsStats().getEffect(Constants.STATS_ADD_EC)+","+getBuffsStats().getEffect(Constants.STATS_ADD_EC)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_AFLEE)+","+ getStuffStats().getEffect(Constants.STATS_ADD_AFLEE)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_AFLEE)+","+getBuffsStats().getEffect(Constants.STATS_ADD_AFLEE)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_MFLEE)+","+ getStuffStats().getEffect(Constants.STATS_ADD_MFLEE)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_MFLEE)+","+getBuffsStats().getEffect(Constants.STATS_ADD_MFLEE)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_NEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_NEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_NEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_NEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_NEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_NEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_NEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_NEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_PVP_NEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_PVP_NEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_NEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_NEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_PVP_NEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_PVP_NEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_NEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_NEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_TER)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_TER)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_TER)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_TER)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_TER)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_TER)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_TER)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_TER)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_PVP_TER)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_PVP_TER)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_TER)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_TER)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_PVP_TER)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_PVP_TER)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_TER)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_TER)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_EAU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_EAU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_EAU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_EAU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_EAU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_EAU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_EAU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_EAU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_PVP_EAU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_PVP_EAU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_EAU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_EAU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_PVP_EAU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_PVP_EAU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_EAU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_EAU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_AIR)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_AIR)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_AIR)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_AIR)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_AIR)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_AIR)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_AIR)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_AIR)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_PVP_AIR)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_PVP_AIR)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_AIR)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_AIR)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_PVP_AIR)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_PVP_AIR)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_AIR)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_AIR)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_FEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_FEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_FEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_FEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_FEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_FEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_FEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_FEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_R_PVP_FEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_R_PVP_FEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_FEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_R_PVP_FEU)+"|";
		ASData += _baseStats.getEffect(Constants.STATS_ADD_RP_PVP_FEU)+","+ getStuffStats().getEffect(Constants.STATS_ADD_RP_PVP_FEU)+","+0+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_FEU)+","+getBuffsStats().getEffect(Constants.STATS_ADD_RP_PVP_FEU)+"|";
		
		return ASData;
	}
	
	public int getGrade()
	{
		if(_align == Constants.ALIGNEMENT_NEUTRE)return 0;
		if(_honor >= 17500)return 10;
		for(int n = 1; n <=10; n++)
		{
			if(_honor < World.getExpLevel(n).pvp)return n-1;
		}
		return 0;
	}
	
	public String xpString(String c)
	{
		return _curExp+c+World.getPersoXpMin(_lvl)+c+World.getPersoXpMax(_lvl);
	}
	
	public int emoteActive() {
		return _emoteActive;
	}

	public void setEmoteActive(int emoteActive) {
		this._emoteActive = emoteActive;
	}

	private Stats getStuffStats()
	{
		Stats stats = new Stats(false,null);
		ArrayList<Integer> itemSetApplied = new ArrayList<Integer>();
		
		for(Entry<Integer,Objet> entry : _items.entrySet())
		{
			if(entry.getValue().getPosition() != Constants.ITEM_POS_NO_EQUIPED)
			{
				stats = Stats.cumulStat(stats,entry.getValue().getStats());
				int panID = entry.getValue().getTemplate().getPanopID();
				//Si panoplie, et si l'effet de pano n'a pas encore été ajouté
				if(panID>0 && !itemSetApplied.contains(panID))
				{
					itemSetApplied.add(panID);
					ItemSet IS = World.getItemSet(panID);
					//Si la pano existe
					if(IS != null)
					{
						//on ajoute le bonus de pano en fonction du nombre d'item
						stats = Stats.cumulStat(stats,IS.getBonusStatByItemNumb(this.getNumbEquipedItemOfPanoplie(panID)));
					}
				}
			}
		}
		if(_onMount && _mount != null)
		{
			stats = Stats.cumulStat(stats, _mount.get_stats());
		}
		return stats;
	}

	private Stats getBuffsStats()
	{
		Stats stats = new Stats(false,null);
		for(Map.Entry<Integer, SpellEffect> entry : _buffs.entrySet())
		{
			stats.addOneStat(entry.getValue().getEffectID(), entry.getValue().getValue());
		}
		return stats;
	}

	public int get_orientation() {
		return _orientation;
	}

	public void set_orientation(int _orientation) {
		this._orientation = _orientation;
	}

	public int getInitiative()
	{
		int fact = 4;
		int pvmax = _PDVMAX - Constants.getBasePdv(_classe);
		int pv = _PDV - Constants.getBasePdv(_classe);
		if(_classe == Constants.CLASS_SACRIEUR)fact = 8;
		double coef = pvmax/fact;
		
		coef += getStuffStats().getEffect(Constants.STATS_ADD_INIT);
		
		coef += getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
		coef += getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
		coef += getTotalStats().getEffect(Constants.STATS_ADD_INTE);
		coef += getTotalStats().getEffect(Constants.STATS_ADD_FORC);
		
		int init = 1;
		if(pvmax != 0)
		 init = (int)(coef*((double)pv/(double)pvmax));
		if(init <0)
			init = 0;
		return init;
	}

	public Stats getTotalStats()
	{
		Stats total = new Stats(false,null);
		total = Stats.cumulStat(total,_baseStats);
		total = Stats.cumulStat(total,getStuffStats());
		total = Stats.cumulStat(total,getDonsStats());
		if(_fight == null)
			total = Stats.cumulStat(total,getBuffsStats());
		
		return total;
	}

	private Stats getDonsStats()
	{
		/* TODO*/
		Stats stats = new Stats(false,null);
		return stats;
	}

	public int getPodUsed()
	{
		int pod = 0;
		for(Entry<Integer,Objet> entry : _items.entrySet())
		{
			pod += entry.getValue().getTemplate().getPod() * entry.getValue().getQuantity();
		}
		return pod;
	}

	public int getMaxPod() {
		int pods = getTotalStats().getEffect(Constants.STATS_ADD_PODS);
		pods += getTotalStats().getEffect(Constants.STATS_ADD_FORC)*5;
		for(StatsMetier SM : _metiers.values())
		{
			pods += SM.get_lvl()*5;
			if(SM.get_lvl() == 100) pods += 1000;
		}
		return pods;
	}

	public int get_PDV() {
		return _PDV;
	}

	public void set_PDV(int _pdv) {
		_PDV = _pdv;
		if(_group != null)
		{
			SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_group,this);
		}
	}

	public int get_PDVMAX() {
		return _PDVMAX;
	}

	public void set_PDVMAX(int _pdvmax) {
		_PDVMAX = _pdvmax;
		if(_group != null)
		{
			SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_group,this);
		}
	}

	public void setSitted(boolean b)
	{
		_sitted = b;
		int diff = _PDV - _exPdv;
		int time = (b?1000:2000);
		
		_exPdv = _PDV;
		if(_isOnline)
		{//On envoie le message "Vous avez recuperer X pdv"
		SocketManager.GAME_SEND_ILF_PACKET(this, diff);
		//On envoie la modif du Timer de regenPdv coté client
		SocketManager.GAME_SEND_ILS_PACKET(this, time);
		}
		//on modifie le delay coté Serveur du timer de regenPDV
		_sitTimer.setDelay(time);
		//Si on se leve, on desactive l'émote
		if((_emoteActive == 1 || _emoteActive == 19) && b == false)_emoteActive = 0;
	}

	public byte get_align()
	{
		return _align;
	}
	
	public int get_pdvper() {
		int pdvper = 100;
		pdvper = (100*_PDV)/_PDVMAX;
		return pdvper;
	}

	public void emoticone(String str) 
	{
		try
		{
			int id = Integer.parseInt(str);
			Carte map = _curCarte;
			if(_fight == null)
				SocketManager.GAME_SEND_EMOTICONE_TO_MAP(map,_GUID,id);
			else
				SocketManager.GAME_SEND_EMOTICONE_TO_FIGHT(_fight,7,_GUID,id);
		}catch(NumberFormatException e){return;};
	}

	public void refreshMapAfterFight()
	{
		_curCarte.addPlayer(this);
		if(_compte.getGameThread().get_out() != null)
		{
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SocketManager.GAME_SEND_ILS_PACKET(this, 1000);
		}
		_fight = null;
		_away = false;
	}

	public void boostStat(int stat)
	{
		GameServer.addToLog("Perso "+_name+": tentative de boost stat "+stat);
		int value = 0;
		switch(stat)
		{
			case 10://Force
				value = _baseStats.getEffect(Constants.STATS_ADD_FORC);
			break;
			case 13://Chance
				value = _baseStats.getEffect(Constants.STATS_ADD_CHAN);
			break;
			case 14://Agilité
				value = _baseStats.getEffect(Constants.STATS_ADD_AGIL);
			break;
			case 15://Intelligence
				value = _baseStats.getEffect(Constants.STATS_ADD_INTE);
			break;
		}
		int cout = Constants.getReqPtsToBoostStatsByClass(_classe, stat, value);
		if(cout <= _capital)
		{
			switch(stat)
			{
				case 11://Vita
					if(_classe != Constants.CLASS_SACRIEUR)
						_baseStats.addOneStat(Constants.STATS_ADD_VITA, 1);
					else
						_baseStats.addOneStat(Constants.STATS_ADD_VITA, 2);
					_PDV++;
				break;
				case 12://Sage
					_baseStats.addOneStat(Constants.STATS_ADD_SAGE, 1);
				break;
				case 10://Force
					_baseStats.addOneStat(Constants.STATS_ADD_FORC, 1);
				break;
				case 13://Chance
					_baseStats.addOneStat(Constants.STATS_ADD_CHAN, 1);
				break;
				case 14://Agilité
					_baseStats.addOneStat(Constants.STATS_ADD_AGIL, 1);
				break;
				case 15://Intelligence
					_baseStats.addOneStat(Constants.STATS_ADD_INTE, 1);
				break;
				default:
					return;
			}
			_capital -= cout;
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SQLManager.SAVE_PERSONNAGE(this,false);
		}
	}

	public boolean isMuted()
	{
		return _compte.isMuted();
	}
	public void set_curCarte(Carte carte)
	{
		_curCarte = carte;
	}

	public String parseObjetsToDB()
	{
		String str = "";
		for(Entry<Integer,Objet> entry : _items.entrySet())
		{
			Objet obj = entry.getValue();
			str += obj.getGuid()+"|";
		}
		return str;
	}
	
	public boolean addObjet(Objet newObj,boolean stackIfSimilar)	//Return false quand il Stack
	{
		for(Entry<Integer,Objet> entry : _items.entrySet())
		{
			Objet obj = entry.getValue();
			if(obj.getTemplate().getID() == newObj.getTemplate().getID()
				&& obj.getStats().isSameStats(newObj.getStats())
				&& stackIfSimilar
				&& newObj.getTemplate().getType() != 85
				&& obj.getPosition() == Constants.ITEM_POS_NO_EQUIPED)//Si meme Template et Memes Stats et Objet non équipé
			{
				obj.setQuantity(obj.getQuantity()+newObj.getQuantity());//On ajoute QUA item a la quantité de l'objet existant
				if(_isOnline)SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this,obj);
				return false;
			}
		}
		_items.put(newObj.getGuid(), newObj);
		SocketManager.GAME_SEND_OAKO_PACKET(this,newObj);
		return true;
	}
	public Map<Integer,Objet> getItems()
	{
		return _items;
	}
	public String parseItemToASK()
	{
		String str = "";
		for(Objet  obj : _items.values())str += obj.parseItem();
		return str;
	}

	public String getBankItemsIDSplitByChar(String splitter)
	{
		String str = "";
		for(int entry : _compte.getBank().keySet())str += entry+splitter;
		return str;
	}
	
	public String getItemsIDSplitByChar(String splitter)
	{
		String str = "";
		for(int entry : _items.keySet())
		{
			if(str.length() != 0) str += splitter;
			str += entry;
		}
		return str;
	}

	public boolean hasItemGuid(int guid)
	{
		return _items.get(guid) != null?_items.get(guid).getQuantity()>0:false;
	}
	
	public void sellItem(int guid,int qua)
	{
		if(qua <= 0)
			return;
		if(_items.get(guid).getQuantity() < qua)//Si il a moins d'item que ce qu'on veut Del
			qua = _items.get(guid).getQuantity();
		
		int prix = qua * (_items.get(guid).getTemplate().getPrix()/10);//Calcul du prix de vente (prix d'achat/10)
		int newQua =  _items.get(guid).getQuantity() - qua;
		
		int templateID = _items.get(guid).getTemplate().getID();	//Utilisé pour le log
		
		if(newQua <= 0)//Ne devrait pas etre <0, S'il n'y a plus d'item apres la vente 
		{
			_items.remove(guid);
			//objetLog(templateID, -qua, "Vente à un PNJ");
			
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this,guid);
		}else//S'il reste des items apres la vente
		{
			_items.get(guid).setQuantity(newQua);
			//objetLog(templateID, -qua, "Vente à un PNJ");
			
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, _items.get(guid));
		}
		_kamas = _kamas + prix;
		kamasLog(prix+"", "Vente de l'item '" + World.getObjTemplate(templateID).getName() + "' à un PNJ");	//Enregistrement de la transaction
		
		SocketManager.GAME_SEND_STATS_PACKET(this);
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SocketManager.GAME_SEND_ESK_PACKEt(this);
	}

	public void removeItem(int guid)
	{
		_items.remove(guid);
	}
	public void removeItem(int guid, int nombre,boolean send,boolean deleteFromWorld)
	{
		Objet obj = _items.get(guid);
		
		if(nombre > obj.getQuantity())
			nombre = obj.getQuantity();
		
		if(obj.getQuantity() >= nombre)
		{
			int newQua = obj.getQuantity() - nombre;
			if(newQua >0)
			{
				obj.setQuantity(newQua);
				if(send && _isOnline)
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
			}else
			{
				//on supprime de l'inventaire et du Monde
				_items.remove(obj.getGuid());
				if(deleteFromWorld)
					World.removeItem(obj.getGuid());
				//on envoie le packet si connecté
				if(send && _isOnline)
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.getGuid());
			}
		}
	}
	public void deleteItem(int guid)
	{
		_items.remove(guid);
		World.removeItem(guid);
	}
	
	public Objet getObjetByPos(int pos)
	{
		if(pos == Constants.ITEM_POS_NO_EQUIPED)return null;
		
		for(Entry<Integer,Objet> entry : _items.entrySet())
		{
			Objet obj = entry.getValue();
			if(obj.getPosition() == pos)
				return obj;
		}
		return null;
	}

	public void refreshStats()
	{
		double actPdvPer = (100*(double)_PDV)/(double)_PDVMAX;
		_PDVMAX = (_lvl-1)*5+Constants.getBasePdv(_classe)+getTotalStats().getEffect(Constants.STATS_ADD_VITA);
		_PDV = (int) Math.round(_PDVMAX*actPdvPer/100);
	}

	public void levelUp(boolean send,boolean addXp)
	{
		/*-------------------LIGNE PAR MARTHIEUBEAN-------------------------------*/
		if(Ancestra.CONFIG_POINT_PER_LEVEL > 0 && _lvl % Ancestra.CONFIG_LEVEL_FOR_POINT == 0)	
		{
			SQLManager.addPoint(Ancestra.CONFIG_POINT_PER_LEVEL,_compte);
			SocketManager.GAME_SEND_MESSAGE(this,"Vous gagner 1 point pour la boutique du site internet","DF0101");
		}
		/*------------------------------FIN-----------------------------------------*/
		
		if(_lvl == Ancestra.MAX_LEVEL)return;
		_lvl++;
		_capital+=5;
		_spellPts++;
		_PDVMAX += 5;
		_PDV = _PDVMAX;
		if(_lvl == 100)
			_baseStats.addOneStat(Constants.STATS_ADD_PA, 1);
		Constants.onLevelUpSpells(this,_lvl);
		if(addXp)_curExp = World.getExpLevel(_lvl).perso;
		if(send && _isOnline)
		{
			SocketManager.GAME_SEND_NEW_LVL_PACKET(_compte.getGameThread().get_out(),_lvl);
			SocketManager.GAME_SEND_STATS_PACKET(this);
		}
		
		if(this._guildMember != null)
			this._guildMember.setLevel(_lvl);
	}
	
	/*-------------------------------Surcharge pour le site internet MARTHIEUBEAN-----------------*/
	public void levelUp(boolean send,boolean addXp, boolean fromWeb)
	{
		
		_lvl++;
		_capital+=5;
		_spellPts++;
		_PDVMAX += 5;
		_PDV = _PDVMAX;
		
		if(!(fromWeb))
		{
			SQLManager.addPoint(Ancestra.CONFIG_POINT_PER_LEVEL,_compte);		//LIGNE PAR MARTHIEUBEAN
			SocketManager.GAME_SEND_MESSAGE(this,"Vous gagner 1 point pour la boutique du site internet","DF0101");		//LIGNE PAR MARTHIEUBEAN
		}
		
		if(_lvl == 100)
			_baseStats.addOneStat(Constants.STATS_ADD_PA, 1);
		Constants.onLevelUpSpells(this,_lvl);
		if(addXp)_curExp = World.getExpLevel(_lvl).perso;
		if(send && _isOnline)
		{
			SocketManager.GAME_SEND_NEW_LVL_PACKET(_compte.getGameThread().get_out(),_lvl);
			SocketManager.GAME_SEND_STATS_PACKET(this);
		}
		if(this._guildMember != null)
			this._guildMember.setLevel(_lvl);
	}
	
	public void addXp(long winxp,boolean fromWeb)
	{
		_curExp += winxp;
		int exLevel = _lvl;
		while(_curExp >= World.getPersoXpMax(_lvl) && _lvl<Ancestra.MAX_LEVEL)
			levelUp(false,false,true);
		if(_isOnline)
		{
			if(exLevel < _lvl)SocketManager.GAME_SEND_NEW_LVL_PACKET(_compte.getGameThread().get_out(),_lvl);
			SocketManager.GAME_SEND_STATS_PACKET(this);
		}
	}
	/*-----------------------------------FIN------------------------------------------------------*/
	
	public void addXp(long winxp)
	{
		_curExp += winxp;
		int exLevel = _lvl;
		while(_curExp >= World.getPersoXpMax(_lvl) && _lvl<Ancestra.MAX_LEVEL)
			levelUp(false,false);
		if(_isOnline)
		{
			if(exLevel < _lvl)SocketManager.GAME_SEND_NEW_LVL_PACKET(_compte.getGameThread().get_out(),_lvl);
			SocketManager.GAME_SEND_STATS_PACKET(this);
		}
	}
	
	public void addKamas (long k)
	{
		_kamas += k;
	}

	public Objet getSimilarItem(Objet exObj)
	{
		for(Entry<Integer,Objet> entry : _items.entrySet())
		{
			Objet obj = entry.getValue();
			if(obj.getTemplate().getID() == exObj.getTemplate().getID()
				&& obj.getStats().isSameStats(exObj.getStats())
				&& obj.getGuid() != exObj.getGuid()
				&& obj.getPosition() == Constants.ITEM_POS_NO_EQUIPED)
			return obj;
		}
		return null;
	}

	public void setCurExchange(Exchange echg)
	{
		_curExchange = echg;
	}
	
	public Exchange get_curExchange()
	{
		return _curExchange;
	}

	public int learnJob(Metier m)
	{
		for(Entry<Integer,StatsMetier> entry : _metiers.entrySet())
		{
			if(entry.getValue().getTemplate().getId() == m.getId())//Si le joueur a déjà le métier
				return -1;
		}
		int Msize = _metiers.size();
		if(Msize == 6)//Si le joueur a déjà 6 métiers
			return -1;
		int pos = 0;
		if(Constants.isMageJob(m.getId()))
		{
			if(_metiers.get(5) == null) pos = 5;
			if(_metiers.get(4) == null) pos = 4;
			if(_metiers.get(3) == null) pos = 3;
		}else
		{
			if(_metiers.get(2) == null) pos = 2;
			if(_metiers.get(1) == null) pos = 1;
			if(_metiers.get(0) == null) pos = 0;
		}
		
		StatsMetier sm = new StatsMetier(pos,m,1,0);
		_metiers.put(pos, sm);//On apprend le métier lvl 1 avec 0 xp
		if(_isOnline)
		{
			//on créer la listes des statsMetier a envoyer (Seulement celle ci)
			ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
			list.add(sm);
			
			SocketManager.GAME_SEND_Im_PACKET(this, "02;"+m.getId());
			//packet JS
			SocketManager.GAME_SEND_JS_PACKET(this, list);
			//packet JX
			SocketManager.GAME_SEND_JX_PACKET(this, list);
			//Packet JO (Job Option)
			SocketManager.GAME_SEND_JO_PACKET(this,list);
			
			Objet obj = getObjetByPos(Constants.ITEM_POS_ARME);
			if(obj != null)
				if(sm.getTemplate().isValidTool(obj.getTemplate().getID()))
					SocketManager.GAME_SEND_OT_PACKET(_compte.getGameThread().get_out(),m.getId());
		}
		return pos;
	}

	public boolean hasEquiped(int id)
	{
		for(Entry<Integer,Objet> entry : _items.entrySet())
			if(entry.getValue().getTemplate().getID() == id && entry.getValue().getPosition() != Constants.ITEM_POS_NO_EQUIPED)
				return true;
		return false;
	}

	public void setInvitation(int target)
	{
		_inviting = target;
	}
	
	public int getInvitation()
	{
		return _inviting;
	}
	
	public String parseToPM()
	{
		String str = _GUID+";";
		str += _name+";";
		str += _gfxID+";";
		str += _color1+";";
		str += _color2+";";
		str += _color3+";";
		str += getGMStuffString()+";";
		str += _PDV+","+_PDVMAX+";";
		str += _lvl+";";
		str += getInitiative()+";";
		str += getTotalStats().getEffect(Constants.STATS_ADD_PROS)+";";
		str += "1";//Side = ?
		return str;
	}
	
	public int getNumbEquipedItemOfPanoplie(int panID)
	{
		int nb = 0;
		for(Entry<Integer, Objet> i : _items.entrySet())
		{
			//On ignore les objets non équipés
			if(i.getValue().getPosition() == Constants.ITEM_POS_NO_EQUIPED)continue;
			//On prend que les items de la pano demandée, puis on augmente le nombre si besoin
			if(i.getValue().getTemplate().getPanopID() == panID)nb++;
		}
		return nb;
	}

	public void startActionOnCell(GameAction GA)
	{
		int cellID = -1;
		int action = -1;
		try
		{
			cellID = Integer.parseInt(GA._args.split(";")[0]);
			action = Integer.parseInt(GA._args.split(";")[1]);
		}catch(Exception e){};
		if(cellID == -1 || action == -1)return;
		//Si case invalide
		if(!_curCarte.getCase(cellID).canDoAction(action))return;
		_curCarte.getCase(cellID).startAction(this,GA);
	}

	public void finishActionOnCell(GameAction GA)
	{
		int cellID = -1;
		try
		{
			cellID = Integer.parseInt(GA._args.split(";")[0]);
		}catch(Exception e){};
		if(cellID == -1)return;
		_curCarte.getCase(cellID).finishAction(this,GA);
	}
	
	public void teleport(int newMapID, int newCellID)
	{
		PrintWriter PW = _compte.getGameThread().get_out();
		if(World.getCarte(newMapID) == null)return;
		if(World.getCarte(newMapID).getCase(newCellID) == null)return;
		if(PW != null)
		{
			SocketManager.GAME_SEND_GA2_PACKET(PW,_GUID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_curCarte, _GUID);
		}
		_curCell.removePlayer(_GUID);
		_curCarte = World.getCarte(newMapID);
		_curCell = _curCarte.getCase(newCellID);
		
		if(PW != null)
		{
		SocketManager.GAME_SEND_MAPDATA(
				PW,
				newMapID,
				_curCarte.get_date(),
				_curCarte.get_key());
		_curCarte.addPlayer(this);
		}
	}
	public int getBankCost()
	{
		return _compte.getBank().size();
	}
	
	public String getStringVar(String str)
	{
		//TODO completer
		if(str.equals("name"))return _name;
		if(str.equals("bankCost"))
		{
			return getBankCost()+"";
		}
		return "";
	}

	public void setBankKamas(long i)
	{
		_compte.setBankKamas(i);
		SQLManager.UPDATE_ACCOUNT_DATA(_compte);
	}
	
	public long getBankKamas()
	{
		return _compte.getBankKamas();
	}

	public void setInBank(boolean b)
	{
		_isInBank = b;
	}
	public boolean isInBank()
	{
		return _isInBank;
	}

	public String parseBankPacket()
	{
		String packet = "";
		for(Entry<Integer, Objet> entry : _compte.getBank().entrySet())
			packet += "O"+entry.getValue().parseItem()+";";
		if(getBankKamas() != 0)
			packet += "G"+getBankKamas();
		return packet;
	}

	public void addCapital(int pts)
	{
		_capital += pts;
	}

	public void addSpellPoint(int pts)
	{
		_spellPts += pts;
	}

	public void addInBank(int guid, int qua)
	{
		Objet PersoObj = World.getObjet(guid);
		//Si le joueur n'a pas l'item dans son sac ...
		if(_items.get(guid) == null)
		{
			GameServer.addToLog("Le joueur "+_name+" a tenter d'ajouter un objet en banque qu'il n'avait pas.");
			return;
		}
		//Si c'est un item équipé ...
		if(PersoObj.getPosition() != Constants.ITEM_POS_NO_EQUIPED)return;
		
		Objet BankObj = getSimilarBankItem(PersoObj);
		int newQua = PersoObj.getQuantity() - qua;
		if(BankObj == null)//S'il n'y pas d'item du meme Template
		{
			//S'il ne reste pas d'item dans le sac
			if(newQua <= 0)
			{
				//On enleve l'objet du sac du joueur
				removeItem(PersoObj.getGuid());
				//On met l'objet du sac dans la banque, avec la meme quantité
				_compte.getBank().put(PersoObj.getGuid(), PersoObj);
				String str = "O+"+PersoObj.getGuid()+"|"+PersoObj.getQuantity()+"|"+PersoObj.getTemplate().getID()+"|"+PersoObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid);
				
			}
			else//S'il reste des objets au joueur
			{
				//on modifie la quantité d'item du sac
				PersoObj.setQuantity(newQua);
				//On ajoute l'objet a la banque et au monde
				BankObj = Objet.getCloneObjet(PersoObj, qua);
				World.addObjet(BankObj, true);
				_compte.getBank().put(BankObj.getGuid(), BankObj);
				
				//Envoie des packets
				String str = "O+"+BankObj.getGuid()+"|"+BankObj.getQuantity()+"|"+BankObj.getTemplate().getID()+"|"+BankObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);
				
			}
		}else // S'il y avait un item du meme template
		{
			//S'il ne reste pas d'item dans le sac
			if(newQua <= 0)
			{
				//On enleve l'objet du sac du joueur
				removeItem(PersoObj.getGuid());
				//On enleve l'objet du monde
				World.removeItem(PersoObj.getGuid());
				//On ajoute la quantité a l'objet en banque
				BankObj.setQuantity(BankObj.getQuantity() + PersoObj.getQuantity());
				//on envoie l'ajout a la banque de l'objet
				String str = "O+"+BankObj.getGuid()+"|"+BankObj.getQuantity()+"|"+BankObj.getTemplate().getID()+"|"+BankObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				//on envoie la supression de l'objet du sac au joueur
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid);
				
			}else //S'il restait des objets
			{
				//on modifie la quantité d'item du sac
				PersoObj.setQuantity(newQua);
				BankObj.setQuantity(BankObj.getQuantity() + qua);
				String str = "O+"+BankObj.getGuid()+"|"+BankObj.getQuantity()+"|"+BankObj.getTemplate().getID()+"|"+BankObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);
				
			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SQLManager.UPDATE_ACCOUNT_DATA(_compte);
	}

	private Objet getSimilarBankItem(Objet obj)
	{
		for(Objet value : _compte.getBank().values())
		{
			if(value.getTemplate().getType() == 85)
				continue;
			if(value.getTemplate().getID() == obj.getTemplate().getID() && value.getStats().isSameStats(obj.getStats()))
				return value;
		}
		return null;
	}

	public void removeFromBank(int guid, int qua)
	{
		Objet BankObj = World.getObjet(guid);
		//Si le joueur n'a pas l'item dans sa banque ...
		if(_compte.getBank().get(guid) == null)
		{
			GameServer.addToLog("Le joueur "+_name+" a tenter de retirer un objet en banque qu'il n'avait pas.");
			return;
		}
		
		Objet PersoObj = getSimilarItem(BankObj);
		
		int newQua = BankObj.getQuantity() - qua;
		
		if(PersoObj == null)//Si le joueur n'avait aucun item similaire
		{
			//S'il ne reste rien en banque
			if(newQua <= 0)
			{
				//On retire l'item de la banque
				_compte.getBank().remove(guid);
				//On l'ajoute au joueur
				_items.put(guid, BankObj);
				objetLog(BankObj.getTemplate().getID(), BankObj.getQuantity(), "Retrait de la banque");
				
				//On envoie les packets
				SocketManager.GAME_SEND_OAKO_PACKET(this,BankObj);
				String str = "O-"+guid;
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				
			}else //S'il reste des objets en banque
			{
				//On crée une copy de l'item en banque
				PersoObj = Objet.getCloneObjet(BankObj, qua);
				//On l'ajoute au monde
				World.addObjet(PersoObj, true);
				//On retire X objet de la banque
				BankObj.setQuantity(newQua);
				//On l'ajoute au joueur
				_items.put(PersoObj.getGuid(), PersoObj);
				objetLog(PersoObj.getTemplate().getID(), PersoObj.getQuantity(), "Retrait de la banque");
				
				//On envoie les packets
				SocketManager.GAME_SEND_OAKO_PACKET(this,PersoObj);
				String str = "O+"+BankObj.getGuid()+"|"+BankObj.getQuantity()+"|"+BankObj.getTemplate().getID()+"|"+BankObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				
			}
		}
		else
		{
			//S'il ne reste rien en banque
			if(newQua <= 0)
			{
				//On retire l'item de la banque
				_compte.getBank().remove(BankObj.getGuid());
				World.removeItem(BankObj.getGuid());
				//On Modifie la quantité de l'item du sac du joueur
				PersoObj.setQuantity(PersoObj.getQuantity() + BankObj.getQuantity());
				
				//On envoie les packets
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);
				String str = "O-"+guid;
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				
			}
			else//S'il reste des objets en banque
			{
				//On retire X objet de la banque
				BankObj.setQuantity(newQua);
				//On ajoute X objets au joueurs
				PersoObj.setQuantity(PersoObj.getQuantity() + qua);
				
				//On envoie les packets
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this,PersoObj);
				String str = "O+"+BankObj.getGuid()+"|"+BankObj.getQuantity()+"|"+BankObj.getTemplate().getID()+"|"+BankObj.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(this, str);
				
			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SQLManager.UPDATE_ACCOUNT_DATA(_compte);
	}

	public void openMountPark()
	{
		_inMountPark = _curCarte.getMountPark();
		_away = true;
		String str = parseDragoList();
		SocketManager.GAME_SEND_ECK_PACKET(this, 16, str);
	}
	
	/*--------------------LIGNE PAR MARTHIEUBEAN------------------------*/
	//Presque même methode que openMountPark, à la différence qu'elle ouvre toujours l'enclose publique de la map 8747
	public void openPublicMountPark()
	{
		_inMountPark = World.getCarte(8747).getMountPark();
		_away = true;
		String str = parseDragoList();
		SocketManager.GAME_SEND_ECK_PACKET(this, 16, str);
	}
	/*--------------------------FIN--------------------------------------*/
	
	private String parseDragoList()
	{
		if(_compte.getStable().size() == 0)return "~";
		String packet = "";
		for(Dragodinde DD : _compte.getStable())
		{
			if(packet.length() >0)packet+= ";";
			packet += DD.parse();
		}
		return packet;
	}

	public void leftMountPark()
	{
		if(_inMountPark == null)return;
		_inMountPark = null;
	}

	public MountPark getInMountPark()
	{
		return _inMountPark;
	}

	public void fullPDV()
	{
		_PDV = _PDVMAX;
	}

	public void warpToSavePos()
	{
		try
		{
			String[] infos = _savePos.split(",");
			teleport(Integer.parseInt(infos[0]), Integer.parseInt(infos[1]));
		}catch(Exception e){};
	}
	
	public void removeByTemplateID(int tID, int count)
	{
		//Copie de la liste pour eviter les modif concurrentes
		ArrayList<Objet> list = new ArrayList<Objet>();
		list.addAll(_items.values());
		
		ArrayList<Objet> remove = new ArrayList<Objet>();
		int tempCount = count;
		
		//on verifie pour chaque objet
		for(Objet obj : list)
		{
			//Si mauvais TemplateID, on passe
			if(obj.getTemplate().getID() != tID)continue;
			
			if(obj.getQuantity() >= count)
			{
				int newQua = obj.getQuantity() - count;
				if(newQua >0)
				{
					obj.setQuantity(newQua);
					if(_isOnline)
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
				}else
				{
					//on supprime de l'inventaire et du Monde
					_items.remove(obj.getGuid());
					World.removeItem(obj.getGuid());
					//on envoie le packet si connecté
					if(_isOnline)
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.getGuid());
				}
				return;
			}
			else//Si pas assez d'objet
			{
				if(obj.getQuantity() >= tempCount)
				{
					int newQua = obj.getQuantity() - tempCount;
					if(newQua > 0)
					{
						obj.setQuantity(newQua);
						if(_isOnline)
							SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
					}
					else remove.add(obj);
					
					for(Objet o : remove)
					{
						//on supprime de l'inventaire et du Monde
						_items.remove(o.getGuid());
						World.removeItem(o.getGuid());
						//on envoie le packet si connecté
						if(_isOnline)
							SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, o.getGuid());
					}
				}else
				{
					// on réduit le compteur
					tempCount -= obj.getQuantity();
					remove.add(obj);
				}
			}
		}
	}

	public Map<Integer,StatsMetier> getMetiers()
	{
		return _metiers;
	}

	public void doJobAction(int actionID, InteractiveObject object, GameAction GA,Case cell)
	{
		StatsMetier SM = getMetierBySkill(actionID);
		if(SM == null)return;
		SM.startAction(actionID,this, object,GA,cell);
	}
	public void finishJobAction(int actionID, InteractiveObject object, GameAction GA,Case cell)
	{
		StatsMetier SM = getMetierBySkill(actionID);
		if(SM == null)return;
		SM.endAction(actionID,this, object,GA,cell);
	}

	public String parseJobData()
	{
		String str = "";
		for(StatsMetier SM : _metiers.values())
		{
			if(str.length() >0)str+=";";
			str += SM.getTemplate().getId()+","+SM.getXp();
		}
		return str;
	}
	public boolean canAggro() {
		return _canAggro;
	}

	public void set_canAggro(boolean canAggro) {
		_canAggro = canAggro;
	}

	public void setCurJobAction(JobAction JA)
	{
		_curJobAction = JA;
	}
	public JobAction getCurJobAction()
	{
		return _curJobAction;
	}

	public StatsMetier getMetierBySkill(int skID)
	{
		for(StatsMetier SM : _metiers.values())
			if(SM.isValidMapAction(skID))return SM;
		return null;
	}

	public String parseToFriendList(int guid)
	{
		String str = ";";
		str += "?;";//FIXME
		str += _name+";";
		if(_compte.isFriendWith(guid))
		{
			str += _lvl+";";
			str += _align+";";//Pas sur :p
		}else
		{
			str += "?;";
			str += "-1;";
		}
		str += _classe+";";
		str += _sexe+";";
		str += _gfxID;
		return str;
	}

	public StatsMetier getMetierByID(int job)
	{
		for(StatsMetier SM : _metiers.values())if(SM.getTemplate().getId() == job)return SM;
		return null;
	}

	public boolean isOnMount()
	{
		return _onMount;
	}
	public void toogleOnMount()
	{
		_onMount = !_onMount;
		Objet obj = getObjetByPos(Constants.ITEM_POS_FAMILIER);
		if(_onMount && obj != null)
		{
			obj.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this, obj);
		}
		//on envoie les packets
		SocketManager.GAME_SEND_Re_PACKET(this, "+", _mount);
		SocketManager.GAME_SEND_ALTER_GM_PACKET(_curCarte,this);
		SocketManager.GAME_SEND_Rr_PACKET(this,_onMount?"+":"-");
		SocketManager.GAME_SEND_STATS_PACKET(this);
	}
	public int getMountXpGive()
	{
		return _mountXpGive;
	}

	public Dragodinde getMount()
	{
		return _mount;
	}

	public void setMount(Dragodinde DD)
	{
		_mount = DD;
	}

	public void setMountGiveXp(int parseInt)
	{
		_mountXpGive = parseInt;
	}

	public void resetVars()
	{
		_isTradingWith = 0;
		_isTalkingWith = 0;
		_away = false;
		_emoteActive = 0;
		_fight = null;
		_duelID = 0;
		_ready = false;
		_curExchange = null;
		_group = null;
		_isInBank = false;
		_inviting = -1;
		_sitted = false;
		_curJobAction = null;
		_isZaaping = false;
		_inMountPark = null;
		_onMount = false;
	}
	public void addChanel(String chan)
	{
		if(_canaux.indexOf(chan) >=0)return;
		_canaux += chan;
		SocketManager.GAME_SEND_cC_PACKET(this, '+', chan);
	}
	
	public void removeChanel(String chan)
	{
		_canaux = _canaux.replace(chan, "");
		SocketManager.GAME_SEND_cC_PACKET(this, '-', chan);
	}
	
	/*---------------------LIGNE PAR MARTHIEUBEAN-----------------*/
	//Appel la fonction de l'obet stats afin de mettre tout les stats à Zéro
	public void resetStats()
	{
		this._baseStats.resetStat();
	}
	
	//Fait seulement l'intermédiaire entre le monde extérieur et la fonction dans l'objets stats.
	public void setStat(int stat,int _nbr)
	{
		this._baseStats.setStat(stat,_nbr);
	}
	
	//Fonction pour définir de façon absolue le nombre de point de capital. Utilisé pour le restats des capitaux entre autres
	public void setCapital(int pts)
	{
		_capital = pts;
	}
	/*----------------------FIN------------------------------------*/

	public void modifAlignement(byte a)
	{
		//Reset Variables
		_honor = 0;
		_deshonor = 0;
		_align = a;
		_aLvl = 1;
		//envoies des packets
		//Im022;10~42 ?
		SocketManager.GAME_SEND_ZC_PACKET(this, a);
		SocketManager.GAME_SEND_STATS_PACKET(this);
		//Im045;50 ?
	}

	public void setDeshonor(int _deshonor)
	{
		this._deshonor = _deshonor;
	}

	public int getDeshonor()
	{
		return _deshonor;
	}

	public boolean isShowingWings()
	{
		return _showWings;
	}

	public void setShowWings(boolean showWings) {
		_showWings = showWings;
	}

	public int get_honor()
	{
		return _honor;
	}

	public void set_honor(int honor)
	{
		_honor = honor;
	}
	public void setALvl(int a)
	{
		_aLvl = a;
	}
	public int getALvl()
	{
		return _aLvl;
	}

	public void toogleWings(char c)
	{
		if(_align == Constants.ALIGNEMENT_NEUTRE)return;
		int hloose = _honor*5/100;//FIXME: perte de X% honneur
		switch(c)
		{
		case '*':
			SocketManager.GAME_SEND_GIP_PACKET(this,hloose);
		return;
		case '+':
			_showWings = true;
			SocketManager.GAME_SEND_STATS_PACKET(this);
		break;
		case '-':
			_showWings = false;
			_honor -= hloose;
			SocketManager.GAME_SEND_STATS_PACKET(this);
		break;
		}
		//SocketManager.GAME_SEND_ALTER_GM_PACKET(_curCarte, this);
	}

	public void addHonor(int winH)
	{
		int g = getGrade();
		_honor += winH;
		//Changement de grade
		if(getGrade() != g)
		{
			//TODO: Message IG
		}
	}

	public GuildMember getGuildMember()
	{
		return _guildMember;
	}

	public int getAccID()
	{
		return _accID;
	}

	public void setAccount(Compte c)
	{
		_compte = c;
	}
	
	public String parseZaapList()//Pour le packet WC
	{
		String map = _curCarte.get_id()+"";
		try
		{
			map = _savePos.split(",")[0];
		}catch(Exception e){};
		String str = map+"";
		int SubAreaID = _curCarte.getSubArea().get_area().get_superArea().get_id();
		for(int i : _zaaps)
		{
			if(World.getCarte(i) == null)continue;
			if(World.getCarte(i).getSubArea().get_area().get_superArea().get_id() != SubAreaID)continue;
			int cost = Formulas.calculZaapCost(_curCarte, World.getCarte(i));
			if(i == _curCarte.get_id()) cost = 0;
			str += "|"+i+";"+cost;
		}
		return str;
	}
	public String parseZaapiList(int city)//Pour packet Wc
	{
		String map = get_curCarte().get_id()+"";

		String str = map+"";

		for(int[] i : (city == Constants.AREA_BONTA?Constants.BONTA_ZAAPI:Constants.BRAKMAR_ZAAPI))
		{
			if(World.getCarte(i[0]) == null)continue;
			if(i[0] == Integer.parseInt(map))continue;
			str += "|"+i[0]+";"+Ancestra.CONFIG_ZAAPI_COST;
		}
		return str;
	}
	
	public boolean hasZaap(int mapID)
	{
		for(int i : _zaaps)if( i == mapID)return true;
		return false;
	}

	public void openZaapiMenu()
	{
		_isZaapiing = true;
		SocketManager.GAME_SEND_Wc_PACKET(this);
	}
	public void useZaapi(short id)
	{
		if(!_isZaapiing)return;//S'il n'a pas ouvert l'interface Zaap(hack?)
		int cost = Ancestra.CONFIG_ZAAPI_COST;
		if(_kamas < cost)return;//S'il n'a pas les kamas (verif coté client)
		int mapID = id;
		int SubAreaID = _curCarte.getSubArea().get_area().get_superArea().get_id();
		int cellID = World.getZaapiCellIdByMapId(id);
		if(World.getCarte(mapID) == null)
		{
			GameServer.addToLog("La map "+id+" n'est pas implantee, Zaapi refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if(World.getCarte(mapID).getCase(cellID) == null)
		{
			GameServer.addToLog("La cellule associee au zaapi "+id+" n'est pas implantee, Zaapi refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if(!World.getCarte(mapID).getCase(cellID).isWalkable(true))
		{
			GameServer.addToLog("La cellule associee au zaapi "+id+" n'est pas 'walkable', Zaapi refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if(World.getCarte(mapID).getSubArea().get_area().get_superArea().get_id() != SubAreaID)
		{
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		_kamas -= cost;
		kamasLog(-cost+"", "Utilisation du Zaapi");
		
		SocketManager.GAME_SEND_Wv_PACKET(this);//On ferme l'interface Zaap
		SocketManager.GAME_SEND_STATS_PACKET(this);//On envoie la perte de kamas
		teleport(mapID,cellID);
		_isZaapiing = false;
	}
	public void openZaapMenu()
	{
		_isZaaping = true;
		if(!hasZaap(_curCarte.get_id()))//Si le joueur ne connaissait pas ce zaap
		{
			_zaaps.add(_curCarte.get_id());
			SocketManager.GAME_SEND_Im_PACKET(this, "024");
		}
		SocketManager.GAME_SEND_WC_PACKET(this);
	}
	public void useZaap(short id)
	{
		if(!_isZaaping)return;//S'il n'a pas ouvert l'interface Zaap(hack?)
		if(!hasZaap(id))return;//S'il n'a pas le zaap demandé(ne devrais pas arriver)
		int cost = Formulas.calculZaapCost(_curCarte, World.getCarte(id));
		if(_kamas < cost)return;//S'il n'a pas les kamas (verif coté client)
		int mapID = id;
		int SubAreaID = _curCarte.getSubArea().get_area().get_superArea().get_id();
		int cellID = World.getZaapCellIdByMapId(id);
		if(World.getCarte(mapID) == null)
		{
			GameServer.addToLog("La map "+id+" n'est pas implantee, Zaap refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if(World.getCarte(mapID).getCase(cellID) == null)
		{
			GameServer.addToLog("La cellule associee au zaap "+id+" n'est pas implantee, Zaap refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if(!World.getCarte(mapID).getCase(cellID).isWalkable(true))
		{
			GameServer.addToLog("La cellule associee au zaap "+id+" n'est pas 'walkable', Zaap refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if(World.getCarte(mapID).getSubArea().get_area().get_superArea().get_id() != SubAreaID)
		{
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		_kamas -= cost;
		kamasLog(-cost+"", "Utilisation du Zaap");
		
		teleport(mapID,cellID);
		SocketManager.GAME_SEND_STATS_PACKET(this);//On envoie la perte de kamas
		SocketManager.GAME_SEND_WV_PACKET(this);//On ferme l'interface Zaap
		_isZaaping = false;
	}
	public String parseZaaps()
	{
		String str = "";
		boolean first = true;
		for(int i : _zaaps)
		{
			if(!first) str += ",";
			first = false;
			str += i+"";
		}
		return str;
	}
	public void stopZaaping()
	{
		if(!_isZaaping)return;
		_isZaaping = false;
		SocketManager.GAME_SEND_WV_PACKET(this);
	}
	public void stopZaapiing()
	{
		if(!_isZaapiing)return;
		_isZaapiing = false;
		SocketManager.GAME_SEND_Wv_PACKET(this);
	}
	
	public boolean hasItemTemplate(int i, int q)
	{
		for(Objet obj : _items.values())
		{
			if(obj.getPosition() != Constants.ITEM_POS_NO_EQUIPED)continue;
			if(obj.getTemplate().getID() != i)continue;
			if(obj.getQuantity() >= q)return true;
		}
		return false;
	}
	
	public boolean isDispo(Personnage sender)
	{
		if(isAbsent)
			return false;
		
		if(isInvisible)
		{
			return _compte.isFriendWith(sender.get_compte().get_GUID());
		}
		
		return true;
	}
	
	/*public void setShowRank(boolean show)
	{
		showRank = show;
		/*if(show)
		{
			String rank;
			if(_compte.get_gmLvl() > 0)
				rank = "[" + Constants.getRankName(_compte.get_gmLvl()) + "]";
			else
				rank = "";
			
			_name = rank + _name;
		}
		else
		{
			if(!_name.contains("]"))
				return;
			_name = _name.substring(_name.indexOf("]")+1);
		}
		
	}*/
	
	public void initLogger()
	{
		//Instanciation du logger
		loggeur = new Logger("Perso_logs/"+_name+".txt", Ancestra.LOGGER_BUFFER_SIZE);
		
		int mois = Calendar.getInstance().get(Calendar.MONTH);
		int jour = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + 1;	//+1, janvier = 0
		int annee = Calendar.getInstance().get(Calendar.YEAR);
		
		loggeur.newLine();loggeur.newLine();	//Passe deux ligne
		loggeur.addToLog("Connexion le " + annee + "-" + jour + "-" + mois);
	}
	public synchronized void closeLogger()
	{
		if(loggeur != null)
			loggeur.close();
	}
	public synchronized void kamasLog(String qte,String raison)
	{
		if(loggeur == null)return;
		
		String toLog = "Kamas:" + qte + "k Raison: "+raison;	//-500k Raison: Achat de l'item '7754' à un PNJ
		loggeur.addToLog(toLog);
	}
	public synchronized void objetLog(int obj, int qte, String raison)
	{
		if(loggeur == null)return;
		
		String nomObj = World.getObjTemplate(obj).getName();
		String toLog = "Objet: " + qte + "x " + nomObj + " Raison: " + raison; //-5x 7754 Raison: Vente à un PNJ
		loggeur.addToLog(toLog);
	}
	public synchronized void commitLogger()
	{
		if(loggeur != null)		
			loggeur.write();
	}
	public synchronized void sockLog(String packet)
	{
		if(loggeur == null)return;
		
		loggeur.addToLog(packet);
	}

	public void setisForgetingSpell(boolean isForgetingSpell) {
		this.isForgetingSpell = isForgetingSpell;
	}

	public boolean isForgetingSpell() {
		return isForgetingSpell;
	}
}