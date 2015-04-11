package objects;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import objects.Personnage.Stats;
import common.Constants;
import common.Formulas;
import common.World;

public class Objet {

	public static class ObjTemplate
	{
		private int ID;
		private String StrTemplate;
		private String name;
		private	int type;
		private int level;
		private int pod;
		private int prix;
		private int panopID;
		private String conditions;
		private int PACost,POmin,POmax,TauxCC,TauxEC,BonusCC;
		private boolean isTwoHanded;
		private ArrayList<Action> onUseActions = new ArrayList<Action>();
		private long sold;
		private int avgPrice;
		
		public ObjTemplate(int id, String strTemplate, String name, int type,int level, int pod, int prix, int panopID, String conditions,String armesInfos)
		{
			this.ID = id;
			this.StrTemplate = strTemplate;
			this.name = name;
			this.type = type;
			this.level = level;
			this.pod = pod;
			this.prix = prix;
			this.panopID = panopID;
			this.conditions = conditions;
			this.PACost = -1;
			this.POmin = 1;
			this.POmax = 1;
			this.TauxCC = 100;
			this.TauxEC = 2;
			this.BonusCC = 0;
			
			try
			{
				String[] infos = armesInfos.split(";");
				PACost = Integer.parseInt(infos[0]);
				POmin = Integer.parseInt(infos[1]);
				POmax = Integer.parseInt(infos[2]);
				TauxCC = Integer.parseInt(infos[3]);
				TauxEC = Integer.parseInt(infos[4]);
				BonusCC = Integer.parseInt(infos[5]);
				isTwoHanded = infos[6].equals("1");
			}catch(Exception e){};
	
		}
		
		public void addAction(Action A)
		{
			onUseActions.add(A);
		}
		
		public boolean isTwoHanded()
		{
			return isTwoHanded;
		}
		
		public int getBonusCC()
		{
			return BonusCC;
		}
		
		public int getPOmin() {
			return POmin;
		}
		
		public int getPOmax() {
			return POmax;
		}

		public int getTauxCC() {
			return TauxCC;
		}

		public int getTauxEC() {
			return TauxEC;
		}

		public int getPACost()
		{
			return PACost;
		}
		public int getID() {
			return ID;
		}

		public String getStrTemplate() {
			return StrTemplate;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public int getLevel() {
			return level;
		}

		public int getPod() {
			return pod;
		}

		public int getPrix() {
			return prix;
		}

		public int getPanopID() {
			return panopID;
		}

		public String getConditions() {
			return conditions;
		}
		
		public Objet createNewItem(int qua,boolean useMax)
		{
			Objet item = new Objet(World.getNewItemGuid(), ID, qua, Constants.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(StrTemplate,useMax),getEffectTemplate(StrTemplate));
			return item;
		}

		private Stats generateNewStatsFromTemplate(String statsTemplate,boolean useMax)
		{
			Stats itemStats = new Stats(false, null);
			//Si stats Vides
			if(statsTemplate.equals("") || statsTemplate == null) return itemStats;
			
			String[] splitted = statsTemplate.split(",");
			for(String s : splitted)
			{	
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				boolean follow = true;
				
				for(int a : Constants.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
					if(a == statID)
						follow = false;
				if(!follow)continue;//Si c'était un effet Actif d'arme
				
				String jet = "";
				int value  = 1;
				try
				{
					jet = stats[4];
					value = Formulas.getRandomJet(jet);
					if(useMax)
					{
						try
						{
							//on prend le jet max
							int min = Integer.parseInt(stats[1],16);
							int max = Integer.parseInt(stats[2],16);
							value = min;
							if(max != 0)value = max;
						}catch(Exception e){value = Formulas.getRandomJet(jet);};			
					}
				}catch(Exception e){};
				itemStats.addOneStat(statID, value);
			}
			return itemStats;
		}
		
		private ArrayList<SpellEffect> getEffectTemplate(String statsTemplate)
		{
			ArrayList<SpellEffect> Effets = new ArrayList<SpellEffect>();
			if(statsTemplate.equals("") || statsTemplate == null) return Effets;
			
			String[] splitted = statsTemplate.split(",");
			for(String s : splitted)
			{	
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				for(int a : Constants.ARMES_EFFECT_IDS)
				{
					if(a == statID)
					{
						int id = statID;
						String min = stats[1];
						String max = stats[2];
						String jet = stats[4];
						String args = min+";"+max+";-1;-1;0;"+jet;
						Effets.add(new SpellEffect(id, args,0,-1));
					}
				}
			}
			return Effets;
		}
		
		public String parseItemTemplateStats()
		{
			String str = "";
			str += this.ID+";";
			str += StrTemplate;
			return str;
		}

		public void applyAction(Personnage perso,int objID)
		{
			for(Action a : onUseActions)a.apply(perso,objID);
		}
		
		public int getAvgPrice()
		{
			return avgPrice;
		}
		public long getSold()
		{
			return this.sold;
		}
		
		public synchronized void newSold(int amount, int price)
		{
			long oldSold = sold;
			sold += amount;
			avgPrice = (int)((avgPrice * oldSold + price) / sold);
		}
	}

	protected ObjTemplate template;
	protected int quantity = 1;
	protected int position = Constants.ITEM_POS_NO_EQUIPED;
	protected int guid;
	private Personnage.Stats Stats = new Stats();
	private ArrayList<SpellEffect> Effects = new ArrayList<SpellEffect>();
	private Map<Integer,String> txtStats = new TreeMap<Integer,String>();
	
	public Objet (int Guid, int template,int qua, int pos, String strStats)
	{
		this.guid = Guid;
		this.template = World.getObjTemplate(template);
		this.quantity = qua;
		this.position = pos;
		
		Stats = new Stats();
		parseStringToStats(strStats);
	}
	public Objet()
	{
		
	}
	
	public void parseStringToStats(String strStats)
	{
		String[] split = strStats.split(",");
		for(String s : split)
		{	
			try
			{
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				
				//Stats spécials
				if(statID == 997 || statID == 996)
				{
					txtStats.put(statID, stats[4]);
					continue;
				}
				//Si stats avec Texte (Signature, apartenance, etc)
				if((!stats[3].equals("") && !stats[3].equals("0")))
				{
					txtStats.put(statID, stats[3]);
					continue;
				}
				
				String jet = stats[4];
				boolean follow = true;
				for(int a : Constants.ARMES_EFFECT_IDS)
				{
					if(a == statID)
					{
						int id = statID;
						String min = stats[1];
						String max = stats[2];
						String args = min+";"+max+";-1;-1;0;"+jet;
						Effects.add(new SpellEffect(id, args,0,-1));
						follow = false;
					}
				}
				if(!follow)continue;//Si c'était un effet Actif d'arme ou une signature
				int value = Integer.parseInt(stats[1],16);
				Stats.addOneStat(statID, value);
			}catch(Exception e){continue;};
		}
	}

	public void addTxtStat(int i,String s)
	{
		txtStats.put(i, s);
	}
	
	public Objet(int Guid, int template, int qua, int pos,	Stats stats,ArrayList<SpellEffect> effects)
	{
		this.guid = Guid;
		this.template = World.getObjTemplate(template);
		this.quantity = qua;
		this.position = pos;
		this.Stats = stats;
		this.Effects = effects;
	}
	
	public Personnage.Stats getStats() {
		return Stats;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public ObjTemplate getTemplate() {
		return template;
	}

	public int getGuid() {
		return guid;
	}
	
	public String parseItem()
	{	
		String posi = position==Constants.ITEM_POS_NO_EQUIPED?"":Integer.toHexString(position);
		return Integer.toHexString(guid)+"~"+Integer.toHexString(template.getID())+"~"+Integer.toHexString(quantity)+"~"+posi+"~"+parseStatsString()+";";
	}

	public String parseStatsString()
	{
		if(getTemplate().getType() == 83)	//Si c'est une pierre d'âme vide
			return getTemplate().getStrTemplate();
		
		String stats = "";
		boolean isFirst = true;
		for(SpellEffect SE : Effects)
		{
			if(!isFirst)
				stats+=",";
			
			String[] infos = SE.getArgs().split(";");
			try
			{
				stats += Integer.toHexString(SE.getEffectID())+"#"+infos[0]+"#"+infos[1]+"#0#"+infos[5];
			}catch(Exception e)
			{
				e.printStackTrace();
				continue;
			};
			
			isFirst = false;
		}
		
		for(Entry<Integer,Integer> entry : Stats.getMap().entrySet())
		{
			if(!isFirst)stats+=",";
			String jet = "0d0+"+entry.getValue();
			stats += Integer.toHexString(entry.getKey())+"#"+Integer.toHexString(entry.getValue())+"#0#0#"+jet;
			isFirst = false;
		}
		
		for(Entry<Integer,String> entry : txtStats.entrySet())
		{
			if(!isFirst)stats+=",";
			if(entry.getKey() == Constants.CAPTURE_MONSTRE)
				stats +=  Integer.toHexString(entry.getKey())+"#0#0#"+entry.getValue();
			else
				stats +=  Integer.toHexString(entry.getKey())+"#0#0#0#"+entry.getValue();
			isFirst = false;
		}
		return stats;
	}

	public String parseToSave()
	{
		return parseStatsString();
	}
	
	public ArrayList<SpellEffect> getEffects()
	{
		return Effects;
	}

	public ArrayList<SpellEffect> getCritEffects()
	{
		ArrayList<SpellEffect> effets = new ArrayList<SpellEffect>();
		for(SpellEffect SE : Effects)
		{
			try
			{
				boolean boost = true;
				for(int i : Constants.NO_BOOST_CC_IDS)if(i == SE.getEffectID())boost = false;
				String[] infos = SE.getArgs().split(";");
				if(!boost)
				{
					effets.add(SE);
					continue;
				}
				int min = Integer.parseInt(infos[0],16)+ (boost?template.getBonusCC():0);
				int max = Integer.parseInt(infos[1],16)+ (boost?template.getBonusCC():0);
				String jet = "1d"+(max-min+1)+"+"+(min-1);
				//exCode: String newArgs = Integer.toHexString(min)+";"+Integer.toHexString(max)+";-1;-1;0;"+jet;
				//osef du minMax, vu qu'on se sert du jet pour calculer les dégats
				String newArgs = "0;0;0;-1;0;"+jet;
				effets.add(new SpellEffect(SE.getEffectID(),newArgs,0,-1));
			}catch(Exception e){continue;};
		}
		return effets;
	}

	public static Objet getCloneObjet(Objet obj,int qua)
	{
		Objet ob = new Objet(World.getNewItemGuid(), obj.getTemplate().getID(), qua,Constants.ITEM_POS_NO_EQUIPED, obj.getStats(), obj.getEffects());
		return ob;
	}

	public void clearStats()
	{
		//On vide l'item de tous ces effets
		Stats = new Stats();
		Effects.clear();
		txtStats.clear();
	}
	
	/*public Map<Integer,Integer> getMinMaxStats(boolean max)
	{
		Stats itemStats = new Stats(false, null);
		String statsTemplate = this.template.getStrTemplate();
		//Si stats Vides
		if(statsTemplate.equals("") || statsTemplate == null) return null;
		
		String[] splitted = statsTemplate.split(",");
		for(String s : splitted)
		{	
			String[] stats = s.split("#");
			int statID = Integer.parseInt(stats[0],16);
			boolean follow = true;
			
			for(int a : Constants.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
				if(a == statID)
					follow = false;
			if(!follow)continue;//Si c'était un effet Actif d'arme
			
			String jet = "";
			int value  = 1;
			try
			{
				value = Integer.parseInt(stats[(max?2:1)],16);	//1min, 2max
			}catch(Exception e){System.out.println(e.getMessage()); e.printStackTrace();};
			itemStats.addOneStat(statID, value);
		}
		return itemStats.getMap();
	}*/
	
	
}
