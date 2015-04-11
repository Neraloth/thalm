package objects;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import objects.Sort.SortStats;

import org.joda.time.LocalDate;
import org.joda.time.Days;
import common.Ancestra;
import common.SQLManager;
import common.World;
import common.Constants;

public class Guild {
	private int _id;
	private String _name = "";
	private String _emblem = "";
	private Map<Integer,GuildMember> _members = new TreeMap<Integer,GuildMember>();
	private int _lvl;
	private long _xp;
	
	//Boost
	private Map<Integer, Integer> sorts = new TreeMap<Integer, Integer>();	//<ID, Level>
	private Map<Integer, Integer> stats = new TreeMap<Integer, Integer>(); //<Effet, Quantité>
	
	public static class GuildMember
	{
		private int _guid;
		private Guild _guild;
		private String _name;
		private int _level;
		private int _gfx;
		private byte _align;
		private int _rank = 0;
		private byte _pXpGive = 0;
		private long _xpGave = 0;
		private int _rights = 0;
		private String _lastCo;
		
		//Droit
		private Map<Integer,Boolean> haveRight = new TreeMap<Integer,Boolean>();

		public GuildMember(int gu,Guild g,String name,int lvl,int gfx,int r,long x,byte pXp,int ri,byte a,String lastCo)
		{
			_guid = gu;
			_guild = g;
			_name = name;
			_level = lvl;
			_gfx = gfx;
			_rank = r;
			_xpGave = x;
			_pXpGive = pXp;
			_rights = ri;
			_align = a;
			_lastCo = lastCo;
			parseIntToRight(_rights);
		}
		
		public int getAlign()
		{
			return _align;
		}
		
		public int getGfx()
		{
			return _gfx;
		}
		
		public int getLvl()
		{
			return _level;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getGuid()
		{
			return _guid;
		}
		public int getRank()
		{
			return _rank;
		}
		
		public Guild getGuild()
		{
			return _guild;
		}

		public String parseRights()
		{
			return Integer.toString(_rights,36);
		}

		public int getRights()
		{
			return _rights;
		}

		public long getXpGave() {
			return _xpGave;
		}

		public int getPXpGive()
		{
			return _pXpGive;
		}
		
		public String getLastCo()
		{
			return _lastCo;
		}
		
		public int getHoursFromLastCo()
		{
			String[] strDate = _lastCo.toString().split("~");
			
			LocalDate lastCo = new LocalDate(Integer.parseInt(strDate[0]),Integer.parseInt(strDate[1]),Integer.parseInt(strDate[2]));
			LocalDate now = new LocalDate();
			
			return Days.daysBetween(lastCo,now).getDays()*24;
		}

		public Personnage getPerso()
		{
			return World.getPersonnage(_guid);
		}

		public boolean canDo(int rightValue)
		{
			if(this._rights == 1)
				return true;
			
			return haveRight.get(rightValue);
		}

		public void setRank(int i)
		{
			_rank = i;
		}
		
		public void setAllRights(int rank,byte xp,int right)
		{
			if(rank == -1)
				rank = this._rank;
			
			if(xp < 0)
				xp = this._pXpGive;
			if(xp > 90)
				xp = 90;
			
			if(right == -1)
				right = this._rights;
			
			this._rank = rank;
			this._pXpGive = xp;
			
			if(right != this._rights && right != 1)	//Vérifie si les droits sont pareille ou si des droits de meneur; pour ne pas faire la conversion pour rien
				parseIntToRight(right);
			this._rights = right;
			
			SQLManager.UPDATE_GUILDMEMBER(this);
		}

		
		public void setLevel(int lvl)
		{
			this._level = lvl;
		}
		
		public void giveXpToGuild(long xp)
		{
			this._xpGave+=xp;
			this._guild.addXp(xp);
		}
		
		public void initRight()
		{
			haveRight.put(Constants.G_BOOST,false);
			haveRight.put(Constants.G_RIGHT,false);
			haveRight.put(Constants.G_INVITE,false);
			haveRight.put(Constants.G_BAN,false);
			haveRight.put(Constants.G_ALLXP,false);
			haveRight.put(Constants.G_HISXP,false);
			haveRight.put(Constants.G_RANK,false);
			haveRight.put(Constants.G_POSPERCO,false);
			haveRight.put(Constants.G_COLLPERCO,false);
			haveRight.put(Constants.G_USEENCLOS,false);
			haveRight.put(Constants.G_AMENCLOS,false);
			haveRight.put(Constants.G_OTHDINDE,false);
		}
		
		public void parseIntToRight(int total)
		{
			if(haveRight.size() == 0)
			{
				initRight();
			}
			if(total == 1)
				return;
			
			if(haveRight.size() > 0)	//Si les droits contiennent quelque chose -> Vidage (Même si le TreeMap supprimerais les entrées doublon lors de l'ajout)
				haveRight.clear();
				
			initRight();	//Remplissage des droits
			
			Integer[] mapKey = haveRight.keySet().toArray(new Integer[haveRight.size()]);	//Récupère les clef de map dans un tableau d'Integer
			
			while(total > 0)
			{
				for (int i = haveRight.size()-1; i < haveRight.size(); i--)
				{
					if(mapKey[i].intValue() <= total)
					{
						total ^= mapKey[i].intValue();
						haveRight.put(mapKey[i],true);
						break;
					}
				}
			}
		}
		
		public void setLastCo(String lastCo)
		{
			_lastCo = lastCo;
		}
	}
	public static class Percepteur {
		private int ID;
		private String nom;
		private Guild guild;
		private ArrayList<Objet> objets;
		private long kamas;
		private long xp;
		private int capital;	//Point à investir dans les stats/sorts
		
		
		public Percepteur(int iD, String nom, Guild guild, ArrayList<Objet> objets,
				long kamas, long xp, Map<Integer, SortStats> sorts,
				Map<Integer, Integer> stats, int capital) {
			super();
			ID = iD;
			this.nom = nom;
			this.guild = guild;
			this.objets = objets;
			this.kamas = kamas;
			this.xp = xp;

			this.capital = capital;
		}
		public long getKamas() {
			return kamas;
		}
		public void setKamas(long kamas) {
			this.kamas = kamas;
		}
		public long getXp() {
			return xp;
		}
		public void setXp(long xp) {
			this.xp = xp;
		}
		public int getCapital() {
			return capital;
		}
		public void setCapital(int capital) {
			this.capital = capital;
		}
		public int getID() {
			return ID;
		}
		public String getNom() {
			return nom;
		}
		public Guild getGuild() {
			return guild;
		}
		public ArrayList<Objet> getObjets() {
			return objets;
		}
	}
	

	public Guild(Personnage owner,String name,String emblem)
	{
		_id = World.getNextHighestGuildID();
		_name = name;
		_emblem = emblem;
		_lvl = 1;
		_xp= 0;
		//decompileSpell(Ancestra.BASE_GUILD_SPELL);
	}
	public Guild(int id,String name, String emblem,int lvl,long xp,
			Map<Integer, Integer> sorts, Map<Integer, Integer> stats)
	{
		_id = id;
		_name = name;
		_emblem = emblem;
		_xp = xp;
		_lvl = lvl;
		this.sorts = sorts;
		this.stats = stats;
	}

	public GuildMember addMember(int guid,String name,int lvl,int gfx,int r,byte pXp,long x,int ri,byte a,String lastCo)
	{
		GuildMember GM = new GuildMember(guid,this,name,lvl,gfx,r,x,pXp,ri,a,lastCo);
		_members.put(guid,GM);
		return GM;
	}
	public GuildMember addNewMember(Personnage p)
	{
		GuildMember GM = new GuildMember(p.get_GUID(),this,p.get_name(),p.get_lvl(),p.get_gfxID(),0,0,(byte) 0,0,p.get_align(),p.get_compte().getLastConnectionDate());
		_members.put(p.get_GUID(),GM);
		return GM;
	}

	public int get_id()
	{
		return _id;
	}

	public Map<Integer, Integer> getSorts() {
		return sorts;
	}
	public Map<Integer, Integer> getStats() {
		return stats;
	}
	public void addStat(int stat, int qte)
	{
		int old = stats.get(stat);
		
		stats.put(stat, old + qte);
	}
	public void boostSort(int ID)
	{
		int old = sorts.get(ID);
		
		sorts.put(ID, old + 1);
	}
	
	public String get_name() {
		return _name;
	}
	public String get_emblem()
	{
		return _emblem;
	}
	public long get_xp()
	{
		return _xp;
	}
	public int get_lvl()
	{
		return _lvl;
	}
	public int getSize()
	{
		return _members.size();
	}
	public String parseMembersToGM()
	{
		String str = "";
		for(GuildMember GM : _members.values())
		{
			String online = "0";
			if(GM.getPerso() != null)if(GM.getPerso().isOnline())online = "1";
			if(str.length() != 0)str += "|";
			str += GM.getGuid()+";";
			str += GM.getName()+";";
			str += GM.getLvl()+";";
			str += GM.getGfx()+";";
			str += GM.getRank()+";";
			str += GM.getXpGave()+";";
			str += GM.getPXpGive()+";";
			str += GM.getRights()+";";
			str += online+";";
			str += GM.getAlign()+";";
			str += GM.getHoursFromLastCo();
		}
		return str;
	}
	public ArrayList<Personnage> getMembers()
	{
		ArrayList<Personnage> a = new ArrayList<Personnage>();
		for(GuildMember GM : _members.values())a.add(GM.getPerso());
		return a;
	}
	public GuildMember getMember(int guid)
	{
		return _members.get(guid);
	}
	public void removeMember(int guid)
	{
		/*if(_members.get(guid).getRank() == 1 && _members.size() > 1)	//Si c'est le meneur et qu'il y a d'autre personne dans la guilde
		{
			GuildMember newMeneur = null;
			for(GuildMember curGm : _members.values())
			{
				if(curGm.getGuid() == guid)continue;
				
				if(newMeneur == null)
				{
					newMeneur = curGm;
					continue;
				}
				if(curGm.getRank() == 2)	//Si bras droit
				{
					newMeneur = curGm;
					break;
				}
				if(curGm.getXpGave() > newMeneur.getXpGave())
					newMeneur = curGm;
			}
			if(newMeneur != null)
				newMeneur.setRank(1);
		}*/
		_members.remove(guid);
		SQLManager.DEL_GUILDMEMBER(guid);
	}
	
	public void addXp(long xp)
	{
		this._xp+=xp;
		
		while(_xp >= World.getGuildXpMax(_lvl) && _lvl<Ancestra.MAX_LEVEL)
			levelUp();
	}
	
	public void levelUp()
	{
		this._lvl++;
	}
	
	public void decompileSpell(String spellStr) //ID;lvl|ID;lvl|...
	{
		int id;
		int lvl;
		
		for(String split : spellStr.split("\\|"))
		{
			id = Integer.parseInt(split.split(";")[0]);
			lvl = Integer.parseInt(split.split(";")[1]);
			
			sorts.put(id, lvl);
		}
	}
	public String compileSpell()
	{
		String toReturn = "";
		boolean isFirst = true;
		
		for(Entry<Integer, Integer> curSpell : sorts.entrySet())
		{
			if(!isFirst)
				toReturn += "|";
			
			toReturn += curSpell.getKey() + ";" + curSpell.getValue();
			
			isFirst = false;
		}
		
		return toReturn;
	}
}
