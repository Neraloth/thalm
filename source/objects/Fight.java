package objects;

import game.GameServer;
import game.GameThread.GameAction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Timer;

import objects.Carte.Case;
import objects.Monstre.MobGrade;
import objects.Monstre.MobGroup;
import objects.Objet.ObjTemplate;
import objects.Personnage.Group;
import objects.Personnage.Stats;
import objects.Sort.SortStats;

import common.*;
import common.World.*;

public class Fight
{
	/*
	 * TODO:
	 * Effets de combat
	 */
	public static class Piege
	{
		private Fighter _caster;
		private Case _cell;
		private byte _size;
		private int _spell;
		private SortStats _trapSpell;
		private Fight _fight;
		private int _color;
		
		public Piege(Fight fight, Fighter caster, Case cell, byte size, SortStats trapSpell, int spell)
		{
			_fight = fight;
			_caster = caster;
			_cell =cell;
			_spell = spell;
			_size = size;
			_trapSpell = trapSpell;
			_color = Constants.getTrapsColor(spell);
		}

		public Case get_cell() {
			return _cell;
		}

		public byte get_size() {
			return _size;
		}

		public Fighter get_caster() {
			return _caster;
		}
	
		public void desapear()
		{
			int team = _caster.getTeam()+1;
			String str = "GDZ-"+_cell.getID()+";"+_size+";"+_color;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team, 999, _caster.getGUID()+"", str);
			str = "GDC"+_cell.getID();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team, 999, _caster.getGUID()+"", str);
		}
		
		public void onTraped(Fighter target)
		{
			if(target.isDead())return;
			_fight.get_traps().remove(this);
			//On efface le pieges
			desapear();
			//On déclenche ses effets
			String str = _spell+","+_cell.getID()+",0,1,1,"+_caster.getGUID();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 307, target.getGUID()+"", str);
			
			ArrayList<Case> cells = new ArrayList<Case>();
			cells.add(_cell);
			//on ajoute les cases
			for(int a = 0; a < _size;a++)
			{
				char[] dirs = {'b','d','f','h'};
				ArrayList<Case> cases2 = new ArrayList<Case>();//on évite les modifications concurrentes
				cases2.addAll(cells);
				for(Case aCell : cases2)
				{
					for(char d : dirs)
					{
						Case cell = _fight.get_map().getCase(Pathfinding.GetCaseIDFromDirrection(aCell.getID(), d, _fight.get_map(), true));
						if(cell == null)continue;
						if(!cells.contains(cell))
						{
							cells.add(cell);
						}
					}
				}
			}
			Fighter fakeCaster;
			if(_caster.getPersonnage() == null)
					fakeCaster = new Fighter(_fight,_caster.getMob());
			else 	fakeCaster = new Fighter(_fight,_caster.getPersonnage());

			fakeCaster.set_fightCell(_cell);
			_trapSpell.applySpellEffectToFight(_fight,fakeCaster,target.get_fightCell(),cells,false);
			_fight.verifIfTeamAllDead();
		}
		
		public int get_color()
		{
			return _color;
		}
	}
	
	public static class Fighter
	{
		private int _id = 0;
		private boolean _canPlay = false;
		private Fight _fight;
		private int _type = 0; // 1 Personnage, 2 : Mob
		private MobGrade _mob = null;
		private Personnage _perso = null;
		private int _team = -2;
		private Case _cell;
		private ArrayList<SpellEffect> _fightBuffs = new ArrayList<SpellEffect>();
		private Map<Integer,Integer> _chatiValue = new TreeMap<Integer,Integer>();
		private int _orientation; 
		private Fighter _invocator;
		private int _PDVMAX;
		private int _PDV;
		private boolean _isDead;
		private boolean _hasLeft;
		private int _gfxID;
		private Map<Integer,Integer> _state = new TreeMap<Integer,Integer>();
		private Fighter _isHolding;
		private Fighter _holdedBy;
		
		public Fighter(Fight f, MobGrade mob)
		{
			_fight = f;
			_type = 2;
			_mob = mob;
			_id = mob.getInFightID();
			_PDVMAX = mob.getPDVMAX();
			_PDV = mob.getPDV();
			_gfxID = getDefaultGfx();
		}
		
		public Fighter(Fight f, Personnage perso)
		{
			_fight = f;
			_type = 1;
			_perso = perso;
			_id = perso.get_GUID();
			_PDVMAX = perso.get_PDVMAX();
			_PDV = perso.get_PDV();
			_gfxID = getDefaultGfx();
		}
		
		public int getGUID()
		{
			return _id;
		}
		public Fighter get_isHolding() {
			return _isHolding;
		}

		public void set_isHolding(Fighter isHolding) {
			_isHolding = isHolding;
		}

		public Fighter get_holdedBy() {
			return _holdedBy;
		}

		public void set_holdedBy(Fighter holdedBy) {
			_holdedBy = holdedBy;
		}

		public int get_gfxID() {
			return _gfxID;
		}

		public void set_gfxID(int gfxID) {
			_gfxID = gfxID;
		}

		public ArrayList<SpellEffect> get_fightBuff()
		{
			return _fightBuffs;
		}
		public void set_fightCell(Case cell)
		{
			_cell = cell;
		}
		public boolean isHide()
		{
			return hasBuff(150);
		}
		public Case get_fightCell()
		{		
			return _cell;
		}
		public void setTeam(int i)
		{
			_team = i;
		}
		public boolean isDead() {
			return _isDead;
		}

		public void setDead(boolean isDead) {
			_isDead = isDead;
		}

		public boolean hasLeft() {
			return _hasLeft;
		}

		public void setLeft(boolean hasLeft) {
			_hasLeft = hasLeft;
		}

		public Personnage getPersonnage()
		{
			if(_type == 1)
				return _perso;
			return null;
		}
		public boolean testIfCC(int tauxCC)
		{
			if(tauxCC < 2)return false;
			int agi = getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
			if(agi <0)agi =0;
			tauxCC -= getTotalStats().getEffect(Constants.STATS_ADD_CC);
			tauxCC = (int)((tauxCC * 2.9901) / Math.log(agi +12));//Influence de l'agi
			if(tauxCC<2)tauxCC = 2;
			int jet = Formulas.getRandomValue(1, tauxCC);
			return (jet == tauxCC);
		}
		
		public Stats getTotalStats()
		{
			Stats stats = new Stats(new TreeMap<Integer,Integer>());
			if(_type == 1)
				stats = _perso.getTotalStats();
			if(_type == 2)
				stats =_mob.getStats();
			
			stats = Stats.cumulStat(stats,getFightBuffStats());
			return stats;
		}
		
		
		public void initBuffStats()
		{
			if(_type == 1)
			{
				for(Map.Entry<Integer, SpellEffect> entry : _perso.get_buff().entrySet())
				{
					_fightBuffs.add(entry.getValue());
				}
			}
		}
		
		private Stats getFightBuffStats()
		{
			Stats stats = new Stats();
			for(SpellEffect entry : _fightBuffs)
			{
				stats.addOneStat(entry.getEffectID(), entry.getValue());
			}
			return stats;
		}
		
		public String getGmPacket()
		{
			String str = "GM|+";
			str+= _cell.getID()+";";
			_orientation = 1;
			str+= _orientation+";";
			str+= "0;";
			str+= getGUID()+";";
			str+= getPacketsName()+";";
			switch(_type)
			{
				case 1://Perso
					str+= _perso.get_classe()+";";
					str+= _perso.get_gfxID()+"^"+_perso.get_size()+";";
					str+= _perso.get_sexe()+";";
					str+= _perso.get_lvl()+";";
					str+= _perso.get_align()+",";
					str+= "0,";//TODO
					str+= (_perso.isShowingWings()?_perso.getGrade():"0")+",";
					str+= _perso.get_GUID()+";";
					str+= (_perso.get_color1()==-1?"-1":Integer.toHexString(_perso.get_color1()))+";";
					str+= (_perso.get_color2()==-1?"-1":Integer.toHexString(_perso.get_color2()))+";";
					str+= (_perso.get_color3()==-1?"-1":Integer.toHexString(_perso.get_color3()))+";";
					str+= _perso.getGMStuffString()+";";
					str+= getPDV()+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_PA)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_PM)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_RP_NEU)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_RP_TER)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_RP_FEU)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_RP_EAU)+";";	
					str+= getTotalStats().getEffect(Constants.STATS_ADD_RP_AIR)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_AFLEE)+";";
					str+= getTotalStats().getEffect(Constants.STATS_ADD_MFLEE)+";";
					str+= _team+";";
					if(_perso.isOnMount() && _perso.getMount() != null)str+=_perso.getMount().get_color();
					str+= ";";
				break;
				case 2://Mob
					str+= "-2;";
					str+= _mob.getTemplate().getGfxID()+"^100;";
					str+= _mob.getGrade()+";";
					str+= _mob.getTemplate().getColors().replace(",", ";")+";";
					str+= "0,0,0,0;";
					str+= this.getPDVMAX()+";";
					str+= _mob.getPA()+";";
					str+= _mob.getPM()+";";
					str+= _team;
				break;
			}
			
			return str;
		}
		
		public void setState(int id, int t)
		{
			_state.remove(id);
			if(t != 0)
			_state.put(id, t);
		}
		
		public boolean isState(int id)
		{
			if(_state.get(id) == null)return false;
			return _state.get(id) != 0;
		}
		
		public void decrementStates()
		{
			//Copie pour évident les modif concurrentes
			ArrayList<Entry<Integer,Integer>> entries = new ArrayList<Entry<Integer, Integer>>();
			entries.addAll(_state.entrySet());
			for(Entry<Integer,Integer> e : entries)
			{
				//Si la valeur est négative, on y touche pas
				if(e.getKey() < 0)continue;
				
				_state.remove(e.getKey());
				int nVal = e.getValue()-1;
				//Si 0 on ne remet pas la valeur dans le tableau
				if(nVal == 0)//ne pas mettre plus petit, -1 = infinie
				{
					//on envoie au client la desactivation de l'état
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, getGUID()+"", getGUID()+","+e.getKey()+",0");
					continue;
				}
				//Sinon on remet avec la nouvelle valeur
				_state.put(e.getKey(), nVal);
			}
		}
		
		public int getPDV()
		{
			int pdv = _PDV + getBuffValue(Constants.STATS_ADD_VITA);;
			return pdv;
		}
		
		public void removePDV(int pdv)
		{
			_PDV -= pdv;
		}
		
		public void applyBeginningTurnBuff(Fight fight)
		{
			synchronized(_fightBuffs)
			{
				for(int effectID : Constants.BEGIN_TURN_BUFF)
				{
					//On évite les modifications concurrentes
					ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
					buffs.addAll(_fightBuffs);
					for(SpellEffect entry : buffs)
					{
						if(entry.getEffectID() == effectID)
						{
							GameServer.addToLog("Effet de debut de tour : "+ effectID);
							entry.applyBeginingBuff(fight, this);
						}
					}
				}
			}
		}

		public boolean hasBuff(int id)
		{
			for(SpellEffect entry : _fightBuffs)
			{
				if(entry.getEffectID() == id && entry.getDuration() >0)
				{
					return true;
				}
			}
			return false;
		}
		
		public int getBuffValue(int id)
		{
			int value = 0;
			for(SpellEffect entry : _fightBuffs)
			{
				if(entry.getEffectID() == id)
					value += entry.getValue();
			}
			return value;
		}
	
		public void refreshfightBuff()
		{
			//Copie pour contrer les modifications Concurentes
			ArrayList<SpellEffect> b = new ArrayList<SpellEffect>();
			for(SpellEffect entry : _fightBuffs)
			{
				if(entry.decrementDuration() != 0)//Si pas fin du buff
				{
					b.add(entry);
				}else
				{
					GameServer.addToLog("Suppression du buff "+entry.getEffectID()+" sur le joueur Fighter ID= "+getGUID());
					switch(entry.getEffectID())
					{
						case 150://Invisibilité
							SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, entry.getCaster().getGUID()+"",getGUID()+",0");
						break;
					}
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(b);
		}
		
		public void addBuff(int id,int val,int duration,int turns,boolean debuff,int spellID,String args,Fighter caster)
		{
			//Si c'est le jouer actif qui s'autoBuff, on ajoute 1 a la durée
			_fightBuffs.add(new SpellEffect(id,val,(_canPlay?duration+1:duration),turns,debuff,caster,args,spellID));
			GameServer.addToLog("Ajout du Buff "+id+" sur le personnage Fighter ID = "+this.getGUID());
			switch(id)
			{
				case 6://Renvoie de sort
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), -1, val+"", "10", "", duration, spellID);
				break;
				
				case 79://Chance éca
					val = Integer.parseInt(args.split(";")[0]);
					String valMax = args.split(";")[1];
					String chance = args.split(";")[2];
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax, chance, "", duration, spellID);
				break;
				
				default:
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration, spellID);
				break;
			}
		}
		
		public int getInitiative()
		{
			if(_type == 1)
				return _perso.getInitiative();
			if(_type == 2)
				return _mob.getInit();
			return 0;
		}
		public int getPDVMAX()
		{
			return _PDVMAX + getBuffValue(Constants.STATS_ADD_VITA);
		}
		
		public int get_lvl() {
			if(_type == 1)
				return _perso.get_lvl();
			if(_type == 2)
				return _mob.getLevel();
	
			return 0;
		}
		public String xpString(String str)
		{
			if(_perso != null)
			{
				int max = _perso.get_lvl()+1;
				if(max>Ancestra.MAX_LEVEL)max = Ancestra.MAX_LEVEL;
				return World.getExpLevel(_perso.get_lvl()).perso+str+_perso.get_curExp()+str+World.getExpLevel(max).perso;		
			}
			return "0"+str+"0"+str+"0";
		}
		public String getPacketsName()
		{
			if(_type == 1)
				return _perso.get_name();
			if(_type == 2)
				return _mob.getTemplate().getID()+"";
			
			return "";
		}
		public MobGrade getMob()
		{
			if(_type == 2)
				return _mob;
			
			return null;
		}
		public int getTeam()
		{
			return _team;
		}
		
		public boolean canPlay()
		{
			return _canPlay;
		}
		public void setCanPlay(boolean b)
		{
			_canPlay = b;
		}
		public ArrayList<SpellEffect> getBuffsByEffectID(int effectID)
		{
			ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
			for(SpellEffect buff : _fightBuffs)
			{
				if(buff.getEffectID() == effectID)
					buffs.add(buff);
			}
			return buffs;
		}
		public Stats getTotalStatsLessBuff()
		{
			Stats stats = new Stats(new TreeMap<Integer,Integer>());
			if(_type == 1)
				stats = _perso.getTotalStats();
			if(_type == 2)
				stats =_mob.getStats();
			
			return stats;
		}
		public int getPA()
		{
			if(_type == 1)
				return getTotalStats().getEffect(Constants.STATS_ADD_PA);
			if(_type == 2)
				return getTotalStats().getEffect(Constants.STATS_ADD_PA) + _mob.getPA();
			
			return 0;
		}
		public int getPM()
		{
			if(_type == 1)
				return getTotalStats().getEffect(Constants.STATS_ADD_PM);
			if(_type == 2)
				return getTotalStats().getEffect(Constants.STATS_ADD_PM) + _mob.getPM();
			return 0;
		}
		
		public void setInvocator(Fighter caster)
		{
			_invocator = caster;
		}
		
		public Fighter getInvocator()
		{
			return _invocator;
		}
		
		public boolean isInvocation()
		{
			return (_invocator!=null);
		}
		public void debuff()
		{
			ArrayList<SpellEffect> newBuffs = new ArrayList<SpellEffect>();
			//on vérifie chaque buff en cours, si pas débuffable, on l'ajout a la nouvelle liste
			for(SpellEffect SE : _fightBuffs)
			{
				if(!SE.isDebuffabe())newBuffs.add(SE);
				//On envoie les Packets si besoin
				switch(SE.getEffectID())
				{
					case Constants.STATS_ADD_PA:
					case Constants.STATS_ADD_PA2:
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 101,getGUID()+"",getGUID()+",-"+SE.getValue());
					break;
					
					case Constants.STATS_ADD_PM:
					case Constants.STATS_ADD_PM2:
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 127,getGUID()+"",getGUID()+",-"+SE.getValue());
					break;
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(newBuffs);
			if(_perso != null && !_hasLeft)
				SocketManager.GAME_SEND_STATS_PACKET(_perso);
		}

		public void fullPDV()
		{
			_PDV = _PDVMAX;
		}

		public void setIsDead(boolean b)
		{
			_isDead = b;
		}

		public void unHide()
		{
			//on retire le buff invi
			ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
			buffs.addAll(get_fightBuff());
			for(SpellEffect SE : buffs)
			{
				if(SE.getEffectID() == 150)
					get_fightBuff().remove(SE);
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150,getGUID()+"",getGUID()+",0");
			//On actualise la position
			SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(_fight, 7,this);
		}

		public int getPdvMaxOutFight()
		{
			if(_perso != null)return _perso.get_PDVMAX();
			if(_mob != null)return _mob.getPDVMAX();
			return 0;
		}

		public Map<Integer, Integer> get_chatiValue() {
			return _chatiValue;
		}

		public int getDefaultGfx()
		{
			if(_perso != null)return _perso.get_gfxID();
			if(_mob != null)return _mob.getTemplate().getGfxID();
			return 0;
		}

		public long getXpGive()
		{
			if(_mob != null)return _mob.getBaseXp();
			return 0;
		}

	}
	
	public static class Glyphe
	{
		private Fighter _caster;
		private Case _cell;
		private byte _size;
		private int _spell;
		private SortStats _trapSpell;
		private byte _duration;
		private Fight _fight;
		private int _color;
		
		public Glyphe(Fight fight, Fighter caster, Case cell, byte size, SortStats trapSpell, byte duration, int spell)
		{
			_fight = fight;
			_caster = caster;
			_cell =cell;
			_spell = spell;
			_size = size;
			_trapSpell = trapSpell;
			_duration = duration;
			_color = Constants.getGlyphColor(spell);
		}

		public Case get_cell() {
			return _cell;
		}

		public byte get_size() {
			return _size;
		}

		public Fighter get_caster() {
			return _caster;
		}
		
		public byte get_duration() {
			return _duration;
		}

		public int decrementDuration()
		{
			_duration--;
			return _duration;
		}
		
		public void onTraped(Fighter target)
		{
			String str = _spell+","+_cell.getID()+",0,1,1,"+_caster.getGUID();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 307, target.getGUID()+"", str);
			_trapSpell.applySpellEffectToFight(_fight,_caster,target.get_fightCell(),false);
			_fight.verifIfTeamAllDead();
		}

		public void desapear()
		{
			SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(_fight, 7, "-",_cell.getID(), _size, _color);
			SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(_fight, 7, _cell.getID());
		}
		
		public int get_color()
		{
			return _color;
		}
	}
	
	private int _id;
	private Map<Integer,Fighter> _team0 = new TreeMap<Integer,Fighter>();
	private Map<Integer,Fighter> _team1 = new TreeMap<Integer,Fighter>();
	private Map<Integer,Personnage> _spec  = new TreeMap<Integer,Personnage>();
	private Carte _map;
	private Fighter _init0;
	private Fighter _init1;
	private ArrayList<Case> _start0 = new ArrayList<Case>();
	private ArrayList<Case> _start1 = new ArrayList<Case>();
	private int _state =0;
	private int _type = -1;
	private boolean locked0 = false;
	private boolean onlyGroup0 = false;
	private boolean locked1 = false;
	private boolean onlyGroup1 = false;
	private boolean specOk = true;
	private boolean help1 = false;
	private boolean help2 = false;
	private int _st2;
	private int _st1;
	private int _curPlayer;
	private long _startTime = 0;
	private int _curFighterPA;
	private int _curFighterPM;
	private int _curFighterUsedPA;
	private int _curFighterUsedPM;
	private String _curAction = "";
	private List<Fighter> _ordreJeu = new ArrayList<Fighter>();
	private Timer _turnTimer;
	private List<Glyphe> _glyphs = new ArrayList<Glyphe>();
	private List<Piege> _traps = new ArrayList<Piege>();
	private MobGroup _mobGroup;
	
	private ArrayList<Fighter> _captureur = new ArrayList<Fighter>(8);	//Création d'une liste de longueur 8. Les combats contiennent un max de 8 Attaquant
	private boolean isCapturable = false;
	private int captWinner = 0;
	private PierreAme pierrePleine;
	
	public Fight(int type, int id,Carte map, Personnage init1, Personnage init2)
	{
		_type = type; //1: Défie (2: Pvm) 3:PVP
		_id = id;
		_map = map.getMapCopy();
		_init0 = new Fighter(this,init1);
		_init1 = new Fighter(this,init2);
		_team0.put(init1.get_GUID(), _init0);
		_team1.put(init2.get_GUID(), _init1);
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this,7,2,_type==Constants.FIGHT_TYPE_CHALLENGE?1:0,1,0,_type==Constants.FIGHT_TYPE_CHALLENGE?0:Ancestra.CONFIG_BEGIN_TIME,_type);
		//on desactive le timer de regen coté client
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
		SocketManager.GAME_SEND_ILF_PACKET(init2, 0);
		
		if(_type!=Constants.FIGHT_TYPE_CHALLENGE)
		{
			_turnTimer = new Timer(Ancestra.CONFIG_BEGIN_TIME,new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					startFight();
					_turnTimer.stop();
					return;
				}
			});
			_turnTimer.start();
		}
		Random teams = new Random();
		if(teams.nextBoolean())
		{
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),0);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,2,_map.get_placesStr(),1);
			_st1 = 0;
			_st2 = 1;
		}else
		{
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,2,_map.get_placesStr(),0);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.get_GUID()+"", init2.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.get_GUID()+"", init2.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		
		_init0.set_fightCell(getRandomCell(_start0));
		_init1.set_fightCell(getRandomCell(_start1));
		
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
		_init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());
		
		_init0.get_fightCell().addFighter(_init0);
		_init1.get_fightCell().addFighter(_init1);
		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);
		_init1.getPersonnage().set_fight(this);
		_init1.setTeam(1);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().get_curCarte(), _init1.getGUID());
		
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),0,_init0.getGUID(),_init1.getGUID(),_init0.getPersonnage().get_curCell().getID(),"0;-1", _init1.getPersonnage().get_curCell().getID(), "0;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID(), _init0);
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init1.getGUID(), _init1);
		
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this,7,_map);
		
		set_state(Constants.FIGHT_STATE_PLACE);
	}
	
	public Fight(int id,Carte map,Personnage init1, MobGroup group)
	{
		_mobGroup = group;
		_type = Constants.FIGHT_TYPE_PVM; //(1: Défie) 2: Pvm (3:PVP)
		_id = id;
		_map = map.getMapCopy();
		_init0 = new Fighter(this,init1);
		
		_team0.put(init1.get_GUID(), _init0);
		for(Entry<Integer, MobGrade> entry : group.getMobs().entrySet())
		{
			entry.getValue().setInFightID(entry.getKey());
			Fighter mob = new Fighter(this,entry.getValue());
			_team1.put(entry.getKey(), mob);
		}
		
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this,1,2,0,1,0,Ancestra.CONFIG_BEGIN_TIME,_type);
		
		//on desactive le timer de regen coté client
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
		
		_turnTimer = new Timer(Ancestra.CONFIG_BEGIN_TIME,new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				startFight();
				_turnTimer.stop();
				return;
			}
		});
		_turnTimer.start();
		Random teams = new Random();
		if(teams.nextBoolean())
		{
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),0);
			_st1 = 0;
			_st2 = 1;
		}else
		{
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),1);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTE+",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID()+"", init1.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
		
		List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer,Fighter>>();
		e.addAll(_team1.entrySet());
		for(Entry<Integer,Fighter> entry : e)
		{
			Fighter f = entry.getValue();
			Case cell = getRandomCell(_start1);
			if(cell == null)
			{
				_team1.remove(f.getGUID());
				continue;
			}
			
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID()+"", f.getGUID()+","+Constants.ETAT_PORTEUR+",0");
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			f.setTeam(1);
			f.fullPDV();
		}
		_init0.set_fightCell(getRandomCell(_start0));
		
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().get_GUID());
		
		_init0.get_fightCell().addFighter(_init0);
		
		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);
		
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), group.getID());
		
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),4,_init0.getGUID(),group.getID(),(_init0.getPersonnage().get_curCell().getID()+1),"0;-1",group.getCellID(),"1;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID(), _init0);
		
		for(Fighter f : _team1.values())
		{
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),group.getID(), f);
		}
		
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this,7,_map);
		
		set_state(2);
	}

	public Carte get_map() {
		return _map;
	}

	public List<Piege> get_traps() {
		return _traps;
	}

	public List<Glyphe> get_glyphs() {
		return _glyphs;
	}

	private Case getRandomCell(List<Case> cells)
	{
		Random rand = new Random();
		Case cell;
		if(cells.size() == 0)return null;
		int limit = 0;
		do
		{
			int id = rand.nextInt(cells.size()-1);
			cell = cells.get(id);
			limit++;
		}while((cell == null || cell.getFighters().size() != 0) && limit < 80);
		if(limit == 80)
		{
			GameServer.addToLog("Case non trouvé dans la liste");
			return null;
		}
		return cell;		
	}
	
	private ArrayList<Case> parsePlaces(int num)
	{
		return CryptManager.parseStartCell(_map, num);
	}
	
	public int get_id() {
		return _id;
	}

	public ArrayList<Fighter> getFighters(int teams)//teams entre 0 et 7, binaire([spec][t2][t1]);
	{
		ArrayList<Fighter> fighters = new ArrayList<Fighter>();
		
		if(teams - 4 >= 0)
		{
			for(Entry<Integer,Personnage> entry : _spec.entrySet())
			{
				fighters.add(new Fighter(this,entry.getValue()));
			}
			teams -= 4;
		}
		if(teams -2 >= 0)
		{
			for(Entry<Integer,Fighter> entry : _team1.entrySet())
			{
				fighters.add(entry.getValue());
			}
			teams -= 2;
		}
		if(teams -1 >=0)
		{	
			for(Entry<Integer,Fighter> entry : _team0.entrySet())
			{
				fighters.add(entry.getValue());
			}
		}
		return fighters;
	}
	
	public synchronized void changePlace(Personnage perso,int cell)
	{
		Fighter fighter = getFighterByPerso(perso);
		int team = getTeamID(perso.get_GUID()) -1;
		if(fighter == null)return;
		if(get_state() != 2 || isOccuped(cell) || perso.is_ready() || (team == 0 && !groupCellContains(_start0,cell)) || (team == 1 && !groupCellContains(_start1,cell)))return;

		fighter.get_fightCell().getFighters().clear();
		fighter.set_fightCell(_map.getCase(cell));
		
		_map.getCase(cell).addFighter(fighter);
		SocketManager.GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(this,3,_map,perso.get_GUID(),cell);
	}

	public boolean isOccuped(int cell)
	{
		/* ex Code
		for(Entry<Integer,Fighter> entry : _team0.entrySet())
		{
			if(entry.getValue().getPDV() <= 0)continue;
			if(entry.getValue().get_fightCell().getID() == cell)
				return true;
		}
		for(Entry<Integer,Fighter> entry : _team1.entrySet())
		{
			if(entry.getValue().getPDV() <= 0)continue;
			if(entry.getValue().get_fightCell().getID() == cell)
				return true;
		}
		//*/
		return _map.getCase(cell).getFighters().size() > 0;
	}

	private boolean groupCellContains(ArrayList<Case> cells, int cell)
	{
		for(int a = 0; a<cells.size();a++)
		{
			if(cells.get(a).getID() == cell)
				return true;
		}
		return false;
	}

	public void verifIfAllReady()
	{
		boolean val = true;
		for(int a=0;a<_team0.size();a++)
		{
			if(!_team0.get(_team0.keySet().toArray()[a]).getPersonnage().is_ready())
				val = false;
		}
		if(_type != Constants.FIGHT_TYPE_PVM)
		{
			for(int a=0;a<_team1.size();a++)
			{
				if(!_team1.get(_team1.keySet().toArray()[a]).getPersonnage().is_ready())
					val = false;
			}
		}
		if(val)
		{
			startFight();
		}
	}

	private void startFight()
	{
		if(_state >= Constants.FIGHT_STATE_ACTIVE)return;
		_state = Constants.FIGHT_STATE_ACTIVE;
		_startTime = System.currentTimeMillis();
		SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID());
		if(_type == Constants.FIGHT_TYPE_PVM)
		{
			int align = -1;
			if(_team1.size() >0)
			{
				 _team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
			}
			//Si groupe non fixe
			if(!_mobGroup.isFix())World.getCarte(_map.get_id()).spawnGroup(align, 1, true,_mobGroup.getCellID());//Respawn d'un groupe
		}
		SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(this, 7);
		SocketManager.GAME_SEND_GS_PACKET_TO_FIGHT(this, 7);
		InitOrdreJeu();
		_curPlayer = -1;
		SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this,7);
		SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
		if(_turnTimer  != null)_turnTimer.stop();
		_turnTimer = null;
		_turnTimer = new Timer(Ancestra.TIME_BY_TURN,new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					endTurn();
				}
			});
		GameServer.addToLog("Début du combat");
		for(Fighter F : getFighters(3))
		{
			Personnage perso = F.getPersonnage();
			if(perso == null)continue;
			if(perso.isOnMount())
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_CHEVAUCHANT+",1");
			
		}
		try
		{
			Thread.sleep(100);
		}catch(Exception e){};
		startTurn();
	}

	private void startTurn()
	{
		if(!verifyStillInFight())
			verifIfTeamAllDead();
		
		if(_state >= Constants.FIGHT_STATE_FINISHED)return;
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {e1.printStackTrace();}
		
		_curPlayer++;
		_curAction = "";
		if(_curPlayer >= _ordreJeu.size())_curPlayer = 0;
		
		_curFighterPA = _ordreJeu.get(_curPlayer).getPA();
		_curFighterPM = _ordreJeu.get(_curPlayer).getPM();
		_curFighterUsedPA = 0;
		_curFighterUsedPM = 0;
		
		if(!_ordreJeu.get(_curPlayer).isDead())
		{
			_ordreJeu.get(_curPlayer).applyBeginningTurnBuff(this);
			if(_state == Constants.FIGHT_STATE_FINISHED)return;
			if(_ordreJeu.get(_curPlayer).getPDV()<=0)onFighterDie(_ordreJeu.get(_curPlayer));
		}
		
		if(_ordreJeu.get(_curPlayer).isDead())//Si joueur mort
		{
			GameServer.addToLog("("+_curPlayer+") Fighter ID=  "+_ordreJeu.get(_curPlayer).getGUID()+" est mort");
			endTurn();
			return;
		}
		//reset des Max des Chatis
		_ordreJeu.get(_curPlayer).get_chatiValue().clear();
		//Gestion des glyphes
		ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
		glyphs.addAll(_glyphs);
		for(Glyphe g : glyphs)
		{
			if(_state >= Constants.FIGHT_STATE_FINISHED)return;
			//Si c'est ce joueur qui l'a lancé
			if(g.get_caster().getGUID() == _ordreJeu.get(_curPlayer).getGUID())
			{
				//on réduit la durée restante, et si 0, on supprime
				if(g.decrementDuration() == 0)
				{
					_glyphs.remove(g);
					g.desapear();
					continue;//Continue pour pas que le joueur active le glyphe s'il était dessus
				}
			}
			//Si dans le glyphe
			int dist = Pathfinding.getDistanceBetween(_map,_ordreJeu.get(_curPlayer).get_fightCell().getID() , g.get_cell().getID());
			if(dist <= g.get_size())
			{
				//Alors le joueur est dans le glyphe
				g.onTraped(_ordreJeu.get(_curPlayer));
			}
		}
		if(_ordreJeu == null)return;
		if(_ordreJeu.size() < _curPlayer)return;
		if(_ordreJeu.get(_curPlayer) == null)return;
		if(_ordreJeu.get(_curPlayer).isDead())//Si joueur mort
		{
			GameServer.addToLog("("+_curPlayer+") Fighter ID=  "+_ordreJeu.get(_curPlayer).getGUID()+" est mort");
			endTurn();
			return;
		}
		
		if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
			SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
		
		if(_ordreJeu.get(_curPlayer).hasBuff(Constants.EFFECT_PASS_TURN))//Si il doit passer son tour
		{
			GameServer.addToLog("("+_curPlayer+") Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID()+" passe son tour");
			endTurn();
			return;
		}
		GameServer.addToLog("("+_curPlayer+")Début du tour de Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID());
		_ordreJeu.get(_curPlayer).setCanPlay(true);
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(this,7,_ordreJeu.get(_curPlayer).getGUID(),Ancestra.TIME_BY_TURN);
		_turnTimer.restart();
		
		if(_ordreJeu.get(_curPlayer).getPersonnage() == null)//Si ce n'est pas un joueur
		{
			new IA.IAThread(_ordreJeu.get(_curPlayer),this);		
		}
	}

	public void endTurn()
	{
		if(_state >= Constants.FIGHT_STATE_FINISHED)return;
		if(_ordreJeu.get(_curPlayer) != null)
			SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(this,7,_ordreJeu.get(_curPlayer).getGUID());
		
		if(_curPlayer == -1)return;
		try
		{
			_turnTimer.stop();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {e1.printStackTrace();}
			if(_ordreJeu == null || _ordreJeu.get(_curPlayer) == null)return;
			if(!_ordreJeu.get(_curPlayer).hasLeft())
			{
				_ordreJeu.get(_curPlayer).setCanPlay(false);
				_curAction = "";
				if(!_ordreJeu.get(_curPlayer).isDead())
				{
					//Si empoisonné (Créer une fonction applyEndTurnbuff si d'autres effets existent)
					for(SpellEffect SE : _ordreJeu.get(_curPlayer).getBuffsByEffectID(131))
					{
						int pas = SE.getValue();
						int val = -1;
						try
						{
							val = Integer.parseInt(SE.getArgs().split(";")[1]);
						}catch(Exception e){};
						if(val == -1)continue;
						
						int nbr = (int) Math.floor((double)_curFighterUsedPA/(double)pas);
						int dgt = val * nbr;
	
						//Si poison paralysant
						if(SE.getSpell() == 200)
						{
							int inte = SE.getCaster().getTotalStats().getEffect(Constants.STATS_ADD_INTE);
							if(inte < 0)inte = 0;
							int pdom = SE.getCaster().getTotalStats().getEffect(Constants.STATS_ADD_PERDOM);
							if(pdom < 0)pdom = 0;
							//on applique le boost
							dgt = (int)(((100+inte+pdom)/100) * dgt);
						}
						if(dgt == 0)continue;
						
						if(dgt>_ordreJeu.get(_curPlayer).getPDV())dgt = _ordreJeu.get(_curPlayer).getPDV();//va mourrir
						_ordreJeu.get(_curPlayer).removePDV(dgt);
						dgt = -(dgt);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, SE.getCaster().getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+dgt);
						
					}
					if(_ordreJeu.get(_curPlayer).getPDV() <= 0)onFighterDie(_ordreJeu.get(_curPlayer));
				}
				//reset des valeurs
				_curFighterUsedPA = 0;
				_curFighterUsedPM = 0;
				_curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PA);
				_curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PM);
				_ordreJeu.get(_curPlayer).refreshfightBuff();
				
				if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
					if(_ordreJeu.get(_curPlayer).getPersonnage().isOnline())
						SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
			}
			SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
			SocketManager.GAME_SEND_GTR_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer==_ordreJeu.size()?0:_curPlayer).getGUID());
			GameServer.addToLog("("+_curPlayer+")Fin du tour de Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID());
			startTurn();
		}catch(NullPointerException e)
		{
			e.printStackTrace();
			endTurn();
		}
	}

	private void InitOrdreJeu()
	{
		int curMaxIni = 0;
		Fighter curMax = null;
		byte actTeam = 0;
		boolean team1_ready = false;
		boolean team2_ready = false;
		do
		{
			if(actTeam == 0 || actTeam == 1 || team2_ready)
			{
				team1_ready = true;
				for(Entry<Integer,Fighter> entry : _team0.entrySet())
				{
					if(_ordreJeu.contains(entry.getValue()))continue;
					team1_ready = false;
					if(entry.getValue().getInitiative() >= curMaxIni)
					{
						curMaxIni = entry.getValue().getInitiative();
						curMax = entry.getValue();
					}
				}
			}
			if(actTeam == 0 || actTeam == 2 || team1_ready)
			{
				team2_ready = true;
				for(Entry<Integer,Fighter> entry : _team1.entrySet())
				{
					if(_ordreJeu.contains(entry.getValue()))continue;
					team2_ready = false;
					if(entry.getValue().getInitiative() >= curMaxIni)
					{
						curMaxIni = entry.getValue().getInitiative();
						curMax = entry.getValue();
					}
				}
			}
			if(curMax == null)return;
			_ordreJeu.add(curMax);
			if(curMax.getTeam() == 0)
				actTeam = 1;
			else
				actTeam = 0;
			curMaxIni = 0;
			curMax = null;
		}while(_ordreJeu.size() != getFighters(3).size());
	}

	public void joinFight(Personnage perso, int guid)
	{	
		Fighter current_Join = null;
		if(_team0.containsKey(guid))
		{
			Case cell = getRandomCell(_start0);
			if(cell == null)return;
			
			if(onlyGroup0)
			{
				Group g = _init0.getPersonnage().getGroup();
				if(g != null)
				{
					if(!g.getPersos().contains(perso))
					{
						SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
						return;
					}
				}
			}
			if(_type == Constants.FIGHT_TYPE_AGRESSION)
			{
				if(perso.get_align() == Constants.ALIGNEMENT_NEUTRE)
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
					return;
				}
				if(_init0.getPersonnage().get_align() != perso.get_align())
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
					return;
				}
			}
			if(locked0)
			{
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
				return;
			};
			SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,0,_type);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(), _map.get_placesStr(), _st1);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
			
			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(0);
			_team0.put(perso.get_GUID(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			//Désactive le timer de regen
			SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		}else if(_team1.containsKey(guid))
		{
			Case cell = getRandomCell(_start1);
			if(cell == null)return;
			
			if(onlyGroup1)
			{
				Group g = _init1.getPersonnage().getGroup();
				if(g != null)
				{
					if(!g.getPersos().contains(perso))
					{
						SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
						return;
					}
				}
			}
			if(_type == Constants.FIGHT_TYPE_AGRESSION)
			{
				if(perso.get_align() == Constants.ALIGNEMENT_NEUTRE)
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
					return;
				}
				if(_init0.getPersonnage().get_align() != perso.get_align())
				{
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
					return;
				}
			}
			if(locked1)
			{
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(),'f',guid);
				return;
			};
			SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,0,0);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(), _map.get_placesStr(), _st2);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID()+"", perso.get_GUID()+","+Constants.ETAT_PORTEUR+",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(1);
			_team1.put(perso.get_GUID(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
		}
		perso.get_curCell().removePlayer(perso.get_GUID());
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(),(current_Join.getTeam()==0?_init0:_init1).getGUID(), current_Join);
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this,7,current_Join);
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this,_map,perso);
	}

	public void toggleLockTeam(int guid)
	{
		if(_init0.getGUID() == guid)
		{
			locked0 = !locked0;
			GameServer.addToLog(locked0?"L'équipe 1 devient bloquée":"L'équipe 1 n'est plus bloquée");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), locked0?'+':'-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,1,locked0?"095":"096");
		}else if(_init1.getGUID() == guid)
		{
			locked1 = !locked1;
			GameServer.addToLog(locked1?"L'équipe 2 devient bloquée":"L'équipe 2 n'est plus bloquée");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), locked1?'+':'-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,2,locked1?"095":"096");
		}
	}

	public void toggleOnlyGroup(int guid)
	{
		if(_init0.getGUID() == guid)
		{
			onlyGroup0 = !onlyGroup0;
			GameServer.addToLog(locked0?"L'équipe 1 n'accepte que les membres du groupe":"L'équipe 1 n'est plus bloquée");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), onlyGroup0?'+':'-', 'P', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,1,onlyGroup0?"093":"094");
		}else if(_init1.getGUID() == guid)
		{
			onlyGroup1 = !onlyGroup1;
			GameServer.addToLog(locked1?"L'équipe 2 n'accepte que les membres du groupe":"L'équipe 2 n'est plus bloquée");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), onlyGroup1?'+':'-', 'P', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,2,onlyGroup1?"095":"096");
		}
	}
	
	public void toggleLockSpec(int guid)
	{
		if(_init0.getGUID() == guid || _init1.getGUID() == guid)
		{
			specOk = !specOk;
			GameServer.addToLog(specOk?"Le combat accepte les spectateurs":"Le combat n'accepte plus les spectateurs");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), specOk?'+':'-', 'S', _init0.getGUID());
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), specOk?'+':'-', 'S', _init1.getGUID());
			SocketManager.GAME_SEND_Im_PACKET_TO_MAP(_map,specOk?"039":"040");
		}
	}

	public void toggleHelp(int guid)
	{
		if(_init0.getGUID() == guid)
		{
			help1 = !help1;
			GameServer.addToLog(help2?"L'équipe 1 demande de l'aide":"L'équipe 1s ne demande plus d'aide");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), locked0?'+':'-', 'H', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,1,help1?"0103":"0104");
		}else if(_init1.getGUID() == guid)
		{
			help2 = !help2;
			GameServer.addToLog(help2?"L'équipe 2 demande de l'aide":"L'équipe 2 ne demande plus d'aide");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), locked1?'+':'-', 'H', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,2,help2?"0103":"0104");
		}
	}
	
	public void set_state(int _state) {
		this._state = _state;
	}

	public int get_state() {
		return _state;
	}

	public List<Fighter> get_ordreJeu() {
		return _ordreJeu;
	}

	public boolean fighterDeplace(Fighter f, GameAction GA)
	{
		String path = GA._args;
		if(path.equals(""))
		{
			GameServer.addToLog("Echec du deplacement: chemin vide");
			return false;
		}
		if(_ordreJeu.size() <= _curPlayer)return false;
		if(_ordreJeu.get(_curPlayer) == null)return false;
		GameServer.addToLog("("+_curPlayer+")Tentative de déplacement de Fighter ID= "+f.getGUID()+" a partir de la case "+f.get_fightCell().getID());
		GameServer.addToLog("Path: "+path);
		if(!_curAction.equals("")|| _ordreJeu.get(_curPlayer).getGUID() != f.getGUID() || _state!= Constants.FIGHT_STATE_ACTIVE)
		{
			if(!_curAction.equals(""))
				GameServer.addToLog("Echec du deplacement: il y deja une action en cours");
			if(_ordreJeu.get(_curPlayer).getGUID() != f.getGUID())
				GameServer.addToLog("Echec du deplacement: ce n'est pas a ce joueur de jouer");
			if(_state != Constants.FIGHT_STATE_ACTIVE)
				GameServer.addToLog("Echec du deplacement: le combat n'est pas en cours");
			return false;
		}
		
		Fighter tacle = Pathfinding.getEnnemyFighterArround(f.get_fightCell().getID(), _map, this);
		if( tacle != null)//Tentative de Tacle
		{
			GameServer.addToLog("Le personnage est a coté d'un ennemi ("+tacle.getPacketsName()+","+tacle.get_fightCell().getID()+") => Tentative de tacle:");
			int chance = Formulas.getTacleChance(f, tacle);
			int rand = Formulas.getRandomValue(0, 99);
			if(rand > chance)
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7,GA._id, "104",_ordreJeu.get(_curPlayer).getGUID()+";", "");//Joueur taclé
				int pertePA = _curFighterPA*chance/100;
				
				if(pertePA  < 0)pertePA = -pertePA;
				if(_curFighterPM < 0)_curFighterPM = -_curFighterPM;
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7,GA._id,"129", f.getGUID()+"", f.getGUID()+",-"+_curFighterPM);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7,GA._id,"102", f.getGUID()+"", f.getGUID()+",-"+pertePA);
				
				_curFighterPM = 0;
				_curFighterPA -= pertePA;
				GameServer.addToLog("Echec du deplacement: fighter tacle");
				return false;
			}
		}
		
		//*
		AtomicReference<String> pathRef = new AtomicReference<String>(path);
		int nStep = Pathfinding.isValidPath(_map, f.get_fightCell().getID(), pathRef, this);
		String newPath = pathRef.get();
		if( nStep > _curFighterPM || nStep == -1000)
		{
			GameServer.addToLog("("+_curPlayer+") Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID()+" a demander un chemin inaccessible ou trop loin");
			return false;
		}
		
		_curFighterPM -= nStep;
		_curFighterUsedPM += nStep;
		
		int nextCellID = CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2));
		//les monstres n'ont pas de GAS//GAF
		if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this,7,_ordreJeu.get(_curPlayer).getGUID());
        //Si le joueur n'est pas invisible
        if(!_ordreJeu.get(_curPlayer).isHide())
	        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "1", _ordreJeu.get(_curPlayer).getGUID()+"", "a"+CryptManager.cellID_To_Code(f.get_fightCell().getID())+newPath);
        else//Si le joueur est planqué x)
        {
        	if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
        	{
        		//On envoie le path qu'au joueur qui se déplace
        		PrintWriter out = _ordreJeu.get(_curPlayer).getPersonnage().get_compte().getGameThread().get_out();
        		SocketManager.GAME_SEND_GA_PACKET(out,  GA._id+"", "1", _ordreJeu.get(_curPlayer).getGUID()+"", "a"+CryptManager.cellID_To_Code(f.get_fightCell().getID())+newPath);
        	}
        }
       
        //Si porté
        Fighter po = _ordreJeu.get(_curPlayer).get_holdedBy();
        if(po != null
        && _ordreJeu.get(_curPlayer).isState(Constants.ETAT_PORTE)
        && po.isState(Constants.ETAT_PORTEUR))
        {
        	System.out.println("Porteur: "+po.getPacketsName());
        	System.out.println("NextCellID "+nextCellID);
        	System.out.println("Cell du Porteur "+po.get_fightCell().getID());
        	
        	//si le joueur va bouger
       		if(nextCellID != po.get_fightCell().getID())
       		{
       			//on retire les états
       			po.setState(Constants.ETAT_PORTEUR, 0);
       			_ordreJeu.get(_curPlayer).setState(Constants.ETAT_PORTE,0);
       			//on retire dé lie les 2 fighters
       			po.set_isHolding(null);
       			_ordreJeu.get(_curPlayer).set_holdedBy(null);
       			//La nouvelle case sera définie plus tard dans le code
       			//On envoie les packets
       			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, po.getGUID()+"", po.getGUID()+","+Constants.ETAT_PORTEUR+",0");
    			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, _ordreJeu.get(_curPlayer).getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+Constants.ETAT_PORTE+",0");
       		}
      	}
        
		_ordreJeu.get(_curPlayer).get_fightCell().getFighters().clear();
        GameServer.addToLog("("+_curPlayer+") Fighter ID= "+f.getGUID()+" se déplace de la case "+_ordreJeu.get(_curPlayer).get_fightCell().getID()+" vers "+CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2)));
        _ordreJeu.get(_curPlayer).set_fightCell(_map.getCase(nextCellID));
        _ordreJeu.get(_curPlayer).get_fightCell().addFighter(_ordreJeu.get(_curPlayer));
        _curAction = "GA;129;"+_ordreJeu.get(_curPlayer).getGUID()+";"+_ordreJeu.get(_curPlayer).getGUID()+",-"+nStep;
        
        //Si porteur
        po = _ordreJeu.get(_curPlayer).get_isHolding();
        if(po != null
        && _ordreJeu.get(_curPlayer).isState(Constants.ETAT_PORTEUR)
        && po.isState(Constants.ETAT_PORTE))
        {
       		//on déplace le porté sur la case
        	po.set_fightCell(_ordreJeu.get(_curPlayer).get_fightCell());
        	GameServer.addToLog(po.getPacketsName()+" se deplace vers la case "+nextCellID);
      	}
        
        if(f.getPersonnage() == null)
        {
        	try {
    			Thread.sleep(900+100*nStep);//Estimation de la durée du déplacement
    		} catch (InterruptedException e) {};
        	SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this,7,_curAction);
    		_curAction = "";
    		ArrayList<Piege> P = new ArrayList<Piege>();
    		P.addAll(_traps);
    		for(Piege p : P)
    		{
    			Fighter F = _ordreJeu.get(_curPlayer);
    			int dist = Pathfinding.getDistanceBetween(_map,p.get_cell().getID(),F.get_fightCell().getID());
    			//on active le piege
    			if(dist <= p.get_size())p.onTraped(F);
    		}
    		return true;
        }
        //*/
        f.getPersonnage().get_compte().getGameThread().addAction(GA);
        return true;
    }

	public void onGK(Personnage perso)
	{
		if(_curAction.equals("")|| _ordreJeu.get(_curPlayer).getGUID() != perso.get_GUID() || _state!= Constants.FIGHT_STATE_ACTIVE)return;
		GameServer.addToLog("("+_curPlayer+")Fin du déplacement de Fighter ID= "+perso.get_GUID());
		SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this,7,_curAction);
		SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this,7,2,_ordreJeu.get(_curPlayer).getGUID());
		//copie
		ArrayList<Piege> P = (new ArrayList<Piege>());
		P.addAll(_traps);
		for(Piege p : P)
		{
			Fighter F = getFighterByPerso(perso);
			int dist = Pathfinding.getDistanceBetween(_map,p.get_cell().getID(),F.get_fightCell().getID());
			//on active le piege
			if(dist <= p.get_size())p.onTraped(F);
			if(_state == Constants.FIGHT_STATE_FINISHED)break;
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {};
		
		_curAction = "";
	}

	public void playerPass(Personnage _perso)
	{
		Fighter f = getFighterByPerso(_perso);
		if(f == null)return;
		if(!f.canPlay())return;
		endTurn();
	}

	public int tryCastSpell(Fighter fighter,SortStats Spell, int caseID)
	{
		if(!_curAction.equals(""))return 1;
		if(Spell == null)return 1;
		
		Case Cell = _map.getCase(caseID);
		_curAction = "casting";
		if(CanCastSpell(fighter,Spell,Cell))
		{
			if(fighter.getPersonnage() != null)
				SocketManager.GAME_SEND_STATS_PACKET(fighter.getPersonnage());
			
			GameServer.addToLog(fighter.getPacketsName()+" tentative de lancer le sort "+Spell.getSpellID()+" sur la case "+caseID);
			_curFighterPA -= Spell.getPACost();
			_curFighterUsedPA += Spell.getPACost();
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.getGUID());
			boolean isEc = Spell.getTauxEC() != 0 && Formulas.getRandomValue(1, Spell.getTauxEC()) == Spell.getTauxEC();
			if(isEc)
			{
				GameServer.addToLog(fighter.getPacketsName()+" Echec critique sur le sort "+Spell.getSpellID());
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.getGUID()+"", Spell.getSpellID()+"");
			}else
			{
				boolean isCC = fighter.testIfCC(Spell.getTauxCC());
				String sort = Spell.getSpellID()+","+caseID+","+Spell.getSpriteID()+","+Spell.getLevel()+","+Spell.getSpriteInfos();
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.getGUID()+"", sort);	
				if(isCC)
				{
					GameServer.addToLog(fighter.getPacketsName()+" Coup critique sur le sort "+Spell.getSpellID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.getGUID()+"", sort);
				}
				//Si le joueur est invi, on montre la case
				if(fighter.isHide())showCaseToAll(fighter.getGUID(), fighter.get_fightCell().getID());
				//on applique les effets de l'arme
				Spell.applySpellEffectToFight(this,fighter,Cell,isCC);
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102,fighter.getGUID()+"",fighter.getGUID()+",-"+Spell.getPACost());
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			//Refresh des Stats
			refreshCurPlayerInfos();
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {};
			if((isEc && Spell.isEcEndTurn()))
			{
				_curAction = "";
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {};
				endTurn();
				return 5;
			}
			verifIfTeamAllDead();
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {};
		_curAction = "";
		return 0;
	}

	public boolean CanCastSpell(Fighter fighter, SortStats spell, Case cell)
	{
		//Si le sort n'est pas existant
		if(spell == null)
		{
			GameServer.addToLog("Sort non existant");
			return false;
		}
		//Si ce n'est pas au joueur de jouer
		if(_ordreJeu.get(_curPlayer) == null)return false;
		if(_ordreJeu.get(_curPlayer).getGUID() != fighter.getGUID())
		{
			GameServer.addToLog("Ce n'est pas au joueur de jouer");
			return false;
		}
		//Si le joueur n'a pas assez de PA
		if(_curFighterPA < spell.getPACost())
		{
			GameServer.addToLog("Le joueur n'a pas assez de PA ("+_curFighterPA+"/"+spell.getPACost()+")");
			return false;
		}
		//Si la cellule visée n'existe pas
		if(cell == null)
		{
			GameServer.addToLog("La cellule visée n'existe pas");
			return false;
		}
		//Si la cellule visée n'est pas alignée avec le joueur alors que le sort le demande
		if(spell.isLineLaunch() && !Pathfinding.casesAreInSameLine(_map, fighter.get_fightCell().getID(), cell.getID(), 'z'))
		{
			GameServer.addToLog("Le sort demande un lancer en ligne, or la case n'est pas alignée avec le joueur");
			return false;
		}
		//Si le sort demande une ligne de vue et que la case demandée n'en fait pas partie
		if(spell.hasLDV() && !Pathfinding.checkLoS(_map,fighter.get_fightCell().getID(),cell.getID()))
		{
			GameServer.addToLog("Le sort demande une ligne de vue, mais la case visée n'est pas visible pour le joueur");
			return false;
		}
		int dist = Pathfinding.getDistanceBetween(_map, fighter.get_fightCell().getID(), cell.getID());
		int MaxPO = spell.getMaxPO();
		if(spell.isModifPO())
			MaxPO += fighter.getTotalStats().getEffect(Constants.STATS_ADD_PO);
		//Vérification Portée mini / maxi
		if(dist < spell.getMinPO() || dist > MaxPO)
		{
			GameServer.addToLog("La case est trop proche ou trop éloignée Min: "+spell.getMinPO()+" Max: "+spell.getMaxPO()+" Dist: "+dist);
			return false;
		}
		
		/* TODO: COOLDOWN */
		
		return true;
	}
	
	public String GetGE(int win)
    {
		long time = System.currentTimeMillis() - _startTime;

		int initGUID = _init0.getGUID();		
		int type = Constants.FIGHT_TYPE_CHALLENGE;// toujours 0
		if(_type == Constants.FIGHT_TYPE_AGRESSION)//Sauf si gain d'honneur
			type = _type;
		
        String Packet = "GE"+time+"|"+initGUID+"|"+type+"|";
        ArrayList<Fighter> TEAM1 = new ArrayList<Fighter>();
        ArrayList<Fighter> TEAM2 = new ArrayList<Fighter>();
        
        if(win == 1)
        {
        	TEAM1.addAll(_team0.values());
        	TEAM2.addAll(_team1.values());
        }
        else
        {
        	TEAM1.addAll(_team1.values());
        	TEAM2.addAll(_team0.values());
        }
        //Calculs des niveaux de groupes
        int TEAM1lvl = 0;
        int TEAM2lvl = 0;
        for(Fighter F : TEAM1)
        {
        	if(F.isInvocation())continue;
        	TEAM1lvl += F.get_lvl();
        }
        for(Fighter F : TEAM2)
        {
        	if(F.isInvocation())continue;
        	TEAM2lvl += F.get_lvl();
        }
        //fin
        /* DEBUG
        System.out.println("TEAM1: lvl="+TEAM1lvl);
        System.out.println("TEAM2: lvl="+TEAM2lvl);
        //*/
        //DROP SYSTEM
        	//Calcul de la PP de groupe
	        int groupPP = 0,minkamas = 0,maxkamas = 0;
	        for(Fighter F : TEAM1)
        	{
	        	if(!F.isInvocation() || (F.getMob() != null && F.getMob().getTemplate().getID() ==258))
	        		groupPP += F.getTotalStats().getEffect(Constants.STATS_ADD_PROS);
        	}
	        if(groupPP <0)groupPP =0;
        	//Calcul des drops possibles
	        ArrayList<Drop> possibleDrops = new ArrayList<Drop>();
	        
	        
	        for(Fighter F : TEAM2)
	        {
	        	if(F.isInvocation() || F.getMob() == null)continue;
	        	minkamas += F.getMob().getTemplate().getMinKamas();
	        	maxkamas += F.getMob().getTemplate().getMaxKamas();
	        	for(Drop D : F.getMob().getDrops())
	        	{
	        		if(D.getMinProsp() <= groupPP)
	        		{
	        			//On augmente le taux en fonction de la PP
	        			int taux = (int)((groupPP * D.get_taux()*Ancestra.CONFIG_DROP)/100);
	        			possibleDrops.add(new Drop(D.get_itemID(),0,taux,D.get_max()));
	        		}
	        	}
	        	
	        }
	        
	        
	        //On Réordonne la liste en fonction de la PP
	        ArrayList<Fighter> Temp = new ArrayList<Fighter>();
	        Fighter curMax = null;
	        while(Temp.size() < TEAM1.size())
	        {
	        	int curPP = -1;
		        for(Fighter F : TEAM1)
		        {
	        		//S'il a plus de PP et qu'il n'est pas listé
		        	if(F.getTotalStats().getEffect(Constants.STATS_ADD_PROS) > curPP && !Temp.contains(F))
		        	{
		        		curMax = F;
		        		curPP = F.getTotalStats().getEffect(Constants.STATS_ADD_PROS);
		        	}
		        }
	        	Temp.add(curMax);
	        }
	        //On enleve les invocs
	        TEAM1.clear();
	        TEAM1.addAll(Temp);
	        /* DEBUG
	        System.out.println("DROP: PP ="+groupPP);
	        System.out.println("DROP: nbr="+possibleDrops.size());
	        System.out.println("DROP: Kam="+totalkamas);
	        //*/
	    //FIN DROP SYSTEM
	    //XP SYSTEM
	        long totalXP = 0;
	        for(Fighter F : TEAM2)
	        {
	        	if(F.isInvocation() || F.getMob() == null)continue;
	        	totalXP += F.getMob().getBaseXp();
	        }
	        /* DEBUG
	        System.out.println("TEAM1: xpTotal="+totalXP);
	        //*/
	    //FIN XP SYSTEM
	    
	    //Capture d'âmes
	        boolean mobCapturable = true;
	        for(Fighter F : TEAM2)
	        {
	        	try
	        	{
	        		mobCapturable &= F.getMob().getTemplate().isCapturable();
	        	}catch (Exception e) {
					mobCapturable = false;
					break;
				}
	        }
	        isCapturable |= mobCapturable;

	        if(isCapturable)
	        {
		        boolean isFirst = true;
		        int maxLvl = 0;
		        String pierreStats = "";

		        
		        for(Fighter F : TEAM2)	//Création de la pierre et verifie si le groupe peut être capturé
		        {
		        	if(!isFirst)
		        		pierreStats += "|";
		        	
		        	pierreStats += F.getMob().getTemplate().getID() + "," + F.get_lvl();//Converti l'ID du monstre en Hex et l'ajoute au stats de la futur pierre d'âme
		        	
		        	isFirst = false;
		        	
		        	if(F.get_lvl() > maxLvl)	//Trouve le monstre au plus haut lvl du groupe (pour la puissance de la pierre)
		        		maxLvl = F.get_lvl();
		        }
		        pierrePleine = new PierreAme(World.getNewItemGuid(),1,7010,Constants.ITEM_POS_NO_EQUIPED,pierreStats);	//Crée la pierre d'âme
		        
		        for(Fighter F : TEAM1)	//Récupère les captureur
		        {
		        	if(!F.isInvocation() && F.isState(Constants.ETAT_CAPT_AME))
		        		_captureur.add(F);
		        }
		        
		        if(_captureur.size() > 0 && !World.isArenaMap(get_map().get_id()))	//S'il y a des captureurs
	    		{
	    			for (int i = 0; i < _captureur.size(); i++)
	    			{
	    				try
	    				{
			        		Fighter f = _captureur.get(Formulas.getRandomValue(0, _captureur.size()-1));	//Récupère un captureur au hasard dans la liste
			    			if(!(f.getPersonnage().getObjetByPos(Constants.ITEM_POS_ARME).getTemplate().getType() == Constants.ITEM_TYPE_PIERRE_AME	//Avoir une pierre d'ame vide d'équipé
			    				))
		    				{
			    				_captureur.remove(f);
		    					continue;
		    				}
			    			Couple<Integer,Integer> pierreJoueur = Formulas.decompPierreAme(f.getPersonnage().getObjetByPos(Constants.ITEM_POS_ARME));//Récupère les stats de la pierre équippé
			    			
			    			if(pierreJoueur.second < maxLvl)	//Si la pierre est trop faible
			    			{
			    				_captureur.remove(f);
		    					continue;
		    				}
			    			
			    			int captChance = Formulas.totalCaptChance(pierreJoueur.first, f.getPersonnage());
			    			
			    			if(Formulas.getRandomValue(1, 100) <= captChance)	//Si le joueur obtiens la capture
			    			{
			    				//Retire la pierre vide au personnage et lui envoie ce changement
			    				int pierreVide = f.getPersonnage().getObjetByPos(Constants.ITEM_POS_ARME).getGuid();
			    				f.getPersonnage().deleteItem(pierreVide);
			    				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPersonnage(), pierreVide);
			    				
			    				captWinner = f._id;
			    				break;
			    			}
		    			}
	    				catch(NullPointerException e)
	    				{
	    					continue;
	    				}
	    			}
	    		}
	        }
	        
	    //Fin Capture

	    for(Fighter i : TEAM1)
		{
        	if(type == Constants.FIGHT_TYPE_CHALLENGE)
        	{
        		if(i.isInvocation() && i.getMob() != null && i.getMob().getTemplate().getID() != 258)continue;
        		long winxp 	= Formulas.getXpWinPvm2(i,TEAM1,TEAM2,totalXP);
        		AtomicReference<Long> XP = new AtomicReference<Long>();
        		XP.set(winxp);
        		
        		long guildxp = Formulas.getGuildXpWin(i,XP);
        		long mountxp = 0;
        		if(i.getPersonnage() != null && i.getPersonnage().isOnMount())
        		{
        			mountxp = Formulas.getMountXpWin(i,XP);
        			i.getPersonnage().getMount().addXp(mountxp);
        			SocketManager.GAME_SEND_Re_PACKET(i.getPersonnage(),"+",i.getPersonnage().getMount());
        		}
        		int winKamas	= Formulas.getKamasWin(i,TEAM1,minkamas,maxkamas);
        		String drops = "";
        		//Drop system
        		ArrayList<Drop> temp = new ArrayList<Drop>();
        		temp.addAll(possibleDrops);
        		Map<Integer,Integer> itemWon = new TreeMap<Integer,Integer>();
        		
        		for(Drop D : temp)
        		{
        			int t = (int)(D.get_taux()*100);//Permet de gerer des taux>0.01
        			int jet = Formulas.getRandomValue(0, 100*100);
        			if(jet < t)
        			{
        				ObjTemplate OT = World.getObjTemplate(D.get_itemID());
        				if(OT == null)continue;
        				//on ajoute a la liste
        				itemWon.put(OT.getID(),(itemWon.get(OT.getID())==null?0:itemWon.get(OT.getID()))+1);
        				
        				D.setMax(D.get_max()-1);
        				if(D.get_max() == 0)possibleDrops.remove(D);
        			}
        		}
        		
        		if(i._id == captWinner)	//S'il à capturé le groupe
        		{
        			if(drops.length() >0)drops += ",";
        			drops += pierrePleine.getTemplate().getID()+"~"+1;
        			if(i.getPersonnage().addObjet(pierrePleine, false))
        				World.addObjet(pierrePleine, true);
        			i.getPersonnage().objetLog(pierrePleine.getTemplate().getID(), 1, "Capturé");
        		}
        		
        		for(Entry<Integer,Integer> entry : itemWon.entrySet())
        		{
        			ObjTemplate OT = World.getObjTemplate(entry.getKey());
        			if(OT == null)continue;
        			if(drops.length() >0)drops += ",";
        			drops += entry.getKey()+"~"+entry.getValue();
        			Objet obj = OT.createNewItem(entry.getValue(), false);
        			if(i.getPersonnage().addObjet(obj, true))
        				World.addObjet(obj, true);
        			i.getPersonnage().objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Droppé");
        		}
        		
        		//fin drop system
        		winxp = XP.get();
        		if(winxp != 0 && i.getPersonnage() != null)
        			i.getPersonnage().addXp(winxp);
        		if(winKamas != 0 && i.getPersonnage() != null)
        		{
        			i.getPersonnage().addKamas(winKamas);
        			i.getPersonnage().kamasLog(winKamas+"", "Gagnés dans un combat");
        		}
        		if(guildxp > 0 && i.getPersonnage().getGuildMember() != null)
        			i.getPersonnage().getGuildMember().giveXpToGuild(guildxp);
        			
        		
        		Packet += "2;" + i.getGUID() + ";" + i.getPacketsName() + ";" + i.get_lvl() + ";" + (i.isDead() ?  "1" : "0" )+";";
        		Packet += i.xpString(";")+";";
        		Packet += (winxp == 0?"":winxp)+";";
        		Packet += (guildxp == 0?"":guildxp)+";";
        		Packet += (mountxp == 0?"":mountxp)+";";
        		Packet += drops+";";//Drop
        		Packet += (winKamas == 0?"":winKamas)+"|";
        	}else
        	{
        		//Calcul honeur
        		int winH = Formulas.calculHonorWin(TEAM1,TEAM2,i);
        		int winD = Formulas.calculDeshonorWin(TEAM1,TEAM2,i);
        		
        		Personnage P = i.getPersonnage();
        		if(P.get_honor()+winH<0)winH = -P.get_honor();
        		P.addHonor(winH);
        		P.setDeshonor(P.getDeshonor()+winD);
        		Packet += "2;" + i.getGUID() + ";" + i.getPacketsName() + ";" + i.get_lvl() + ";" + (i.isDead() ?  "1" : "0" )+";";
        		Packet += (P.get_align()!=Constants.ALIGNEMENT_NEUTRE?World.getExpLevel(P.getGrade()).pvp:0)+";";
        		Packet += P.get_honor()+";";
        		int maxHonor = World.getExpLevel(P.getGrade()+1).pvp;
        		if(maxHonor == -1)maxHonor = World.getExpLevel(P.getGrade()).pvp;
        		Packet += (P.get_align()!=Constants.ALIGNEMENT_NEUTRE?maxHonor:0)+";";
        		Packet += winH+";";
        		Packet += P.getGrade()+";";
        		Packet += P.getDeshonor()+";";
        		Packet += winD;
        		Packet += ";;0;0;0;0;0|";
        	}
		}
		for(Fighter i : TEAM2)
		{
			if(i.isInvocation() && i.getMob().getTemplate().getID() != 285)continue;//On affiche pas les invocs
        	if(_type != Constants.FIGHT_TYPE_AGRESSION)
        		Packet += "0;" + i.getGUID() + ";" + i.getPacketsName() + ";" + i.get_lvl() + ";" + (i.getPDV()==0 ?  "1" : "0" )+";"+i.xpString(";")+";;;;|";
        	else
        	{
        		//Calcul honeur
        		int winH = Formulas.calculHonorWin(TEAM1,TEAM2,i);
        		int winD = Formulas.calculDeshonorWin(TEAM1,TEAM2,i);
        		
        		Personnage P = i.getPersonnage();
        		if(P.get_honor()+winH<0)winH = -P.get_honor();
        		P.addHonor(winH);
        		if(P.getDeshonor()+winD<0)winD = -P.getDeshonor();
        		if(P.getDeshonor()+winD>100)winD = 100-P.getDeshonor();
        		P.setDeshonor(P.getDeshonor()+winD);
        		Packet += "0;" + i.getGUID() + ";" + i.getPacketsName() + ";" + i.get_lvl() + ";" + (i.isDead() ?  "1" : "0" )+";";
        		Packet += (P.get_align()!=Constants.ALIGNEMENT_NEUTRE?World.getExpLevel(P.getGrade()).pvp:0)+";";
        		Packet += P.get_honor()+";";
        		int maxHonor = World.getExpLevel(P.getGrade()+1).pvp;
        		if(maxHonor == -1)maxHonor = World.getExpLevel(P.getGrade()).pvp;
        		Packet += (P.get_align()!=Constants.ALIGNEMENT_NEUTRE?maxHonor:0)+";";
        		Packet += winH+";";
        		Packet += P.getGrade()+";";
        		Packet += P.getDeshonor()+";";
        		Packet += winD;
        		Packet += ";;0;0;0;0;0|";
        	}
		}
        return Packet;
    }
    
	public void verifIfTeamAllDead()
	{
		if(_state >=Constants.FIGHT_STATE_FINISHED)return;
		boolean team0 = true;
		boolean team1 = true;
		for(Entry<Integer,Fighter> entry : _team0.entrySet())
		{
			if(entry.getValue().isInvocation())continue;
			if(!entry.getValue().isDead())
			{
				team0 = false;
				 break;
			}
		}
		for(Entry<Integer,Fighter> entry : _team1.entrySet())
		{
			if(entry.getValue().isInvocation())continue;
			if(!entry.getValue().isDead())
			{
				team1 = false;
				break;
			}
		}
		if(team0 || team1 || !verifyStillInFight())
		{
			_state = Constants.FIGHT_STATE_FINISHED;
			int winner = team0?2:1;
			GameServer.addToLog("L'equipe "+winner+" gagne !");
			_turnTimer.stop();
			//On despawn tous le monde
			_curPlayer = -1;
			for(Entry<Integer, Fighter> entry : _team0.entrySet())
			{
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
			}
			for(Entry<Integer, Fighter> entry : _team1.entrySet())
			{
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
			}
			this._init0.getPersonnage().get_curCarte().removeFight(this._id);
			SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(this,7,winner);
			
			for(Entry<Integer, Fighter> entry : _team0.entrySet())
			{
				Personnage perso = entry.getValue().getPersonnage();
				if(perso == null)continue;
				perso.set_duelID(-1);
				perso.set_ready(false);
				perso.set_fight(null);
				if(_type==Constants.FIGHT_TYPE_CHALLENGE)perso.fullPDV();
			}
			switch(_type)
			{
				
				case Constants.FIGHT_TYPE_CHALLENGE://Défie
				for(Entry<Integer, Fighter> entry : _team1.entrySet())
				{
					Personnage perso = entry.getValue().getPersonnage();
					if(perso == null)continue;
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.fullPDV();
					perso.set_fight(null);
				}
				break;
				case Constants.FIGHT_TYPE_AGRESSION://Aggro
					for(Entry<Integer, Fighter> entry : _team1.entrySet())
					{
						Personnage perso = entry.getValue().getPersonnage();
						if(perso == null)continue;
						perso.set_duelID(-1);
						perso.set_ready(false);
						perso.set_fight(null);
					}
				break;
				case Constants.FIGHT_TYPE_PVM://PvM
					if(_team1.get(-1) == null)return;				
				break;
			}
			//on vire les spec du combat
			for(Personnage perso: _spec.values())
			{
				//on remet le perso sur la map
				perso.get_curCarte().addPlayer(perso);
				//SocketManager.GAME_SEND_GV_PACKET(perso);	//Mauvaise ligne apparemment
				perso.refreshMapAfterFight();
				_spec.remove(perso.get_GUID());
			}
			
			World.getCarte(_map.get_id()).removeFight(_id);
			SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
			_map = null;
			_ordreJeu = null;
			ArrayList<Fighter> winTeam = new ArrayList<Fighter>();
			ArrayList<Fighter> looseTeam = new ArrayList<Fighter>();
			if(team0)
			{
				looseTeam.addAll(_team0.values());
				winTeam.addAll(_team1.values());
			}
			else
			{
				winTeam.addAll(_team0.values());
				looseTeam.addAll(_team1.values());
			}
			try
			{
				Thread.sleep(1600);
			}catch(Exception E){};
			//Pour les gagnants, on active les endFight actions
			for(Fighter F : winTeam)
			{
				if(F.hasLeft())continue;
				if(F.getPersonnage() == null)continue;
				if(F.isInvocation())continue;
				if(!F.getPersonnage().isOnline())continue;
				F.getPersonnage().set_PDV(F.getPDV());
				try
				{
					Thread.sleep(200);
				}catch(Exception E){};
				
				F.getPersonnage().get_curCarte().applyEndFightAction(_type, F.getPersonnage());
				F.getPersonnage().refreshMapAfterFight();
			}
			//Pour les perdant ont TP au point de sauvegarde
			for(Fighter F : looseTeam)
			{
				if(F.hasLeft())continue;
				if(F.getPersonnage() == null)continue;
				if(F.isInvocation())continue;
				if(!F.getPersonnage().isOnline())continue;
				try
				{
					Thread.sleep(200);
				}catch(Exception E){};
				if(_type == Constants.FIGHT_TYPE_CHALLENGE)
				{
					F.getPersonnage().refreshMapAfterFight();
					continue;
				}
				//TODO perte d'energie
				F.getPersonnage().warpToSavePos();
				F.getPersonnage().set_PDV(1);
			}
		}
	}

	public void onFighterDie(Fighter target) 
	{
		target.setIsDead(true);
		SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(this,7,target.getGUID());
		target.get_fightCell().getFighters().clear();
		if(target.getTeam() == 0)
		{
			TreeMap<Integer,Fighter> team = new TreeMap<Integer,Fighter>();
			team.putAll(_team0);
			for(Entry<Integer,Fighter> entry : team.entrySet())
			{
				if(entry.getValue().getInvocator() == null)continue;
				if(entry.getValue().isDead())continue;
				if(entry.getValue().getInvocator().getGUID() == target.getGUID())//si il a été invoqué par le joueur mort
				{
					onFighterDie(entry.getValue());
					int index = _ordreJeu.indexOf(entry.getValue());
					if(index != -1)_ordreJeu.remove(index);
					if(_team0.containsKey(entry.getValue().getGUID()))_team0.remove(entry.getValue());
					else if (_team1.containsKey(entry.getValue().getGUID()))_team1.remove(entry.getValue());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID()+"", getGTL());
				}
			}
		}else if(target.getTeam() == 1)
		{
			TreeMap<Integer,Fighter> team = new TreeMap<Integer,Fighter>();
			team.putAll(_team1);
			for(Entry<Integer,Fighter> entry : team.entrySet())
			{
				if(entry.getValue().getInvocator() == null)continue;
				if(entry.getValue().getPDV() == 0)continue;
				if(entry.getValue().getInvocator().getGUID() == target.getGUID())//si il a été invoqué par le joueur mort
				{
					onFighterDie(entry.getValue());
					
					int index = _ordreJeu.indexOf(entry.getValue());
					if(index != -1)_ordreJeu.remove(index);
					
					if(_team0.containsKey(entry.getValue().getGUID()))_team0.remove(entry.getValue());
					else if (_team1.containsKey(entry.getValue().getGUID()))_team1.remove(entry.getValue());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID()+"", getGTL());
				}
			}
		}
		if(target.getMob() != null)
		{
			//Si c'est une invocation, on la retire de la liste
			try
			{
				boolean isStatic = false;
				for(int id : Constants.STATIC_INVOCATIONS)if(id == target.getMob().getTemplate().getID())isStatic = true;
				if(target.isInvocation() && !isStatic)
				{
					int index = _ordreJeu.indexOf(target);
					//Si le joueur courant a un index plus élevé, on le diminue pour éviter le outOfBound
					if(_curPlayer > index) _curPlayer--;
					if(index != -1)_ordreJeu.remove(index);
					if(_team0.containsKey(target.getGUID()))_team0.remove(target.getGUID());
					else if (_team1.containsKey(target.getGUID()))_team1.remove(target.getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID()+"", getGTL());
				}
			}catch(Exception e){e.printStackTrace();};
		}
		//on supprime les glyphes du joueur
		ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
		glyphs.addAll(_glyphs);
		for(Glyphe g : glyphs)
		{
			//Si c'est ce joueur qui l'a lancé
			if(g.get_caster().getGUID() == target.getGUID())
			{
				SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(this, 7, "-", g.get_cell().getID(), g.get_size(), 4);
				SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(this, 7, g.get_cell().getID());
				_glyphs.remove(g);
			}
		}
		
		//on supprime les pieges du joueur
		ArrayList<Piege> Ps = new ArrayList<Piege>();
		Ps.addAll(_traps);
		for(Piege p : Ps)
		{
			if(p.get_caster().getGUID() == target.getGUID())
			{
				p.desapear();
				_traps.remove(p);
			}
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {};
	}

	public int getTeamID(int guid)
	{
		if(_team0.containsKey(guid))
			return 1;
		if(_team1.containsKey(guid))
			return 2;
		if(_spec.containsKey(guid))
			return 4;
		return -1;
	}

	public void tryCaC(Personnage perso, int cellID)
	{
		Fighter caster = getFighterByPerso(perso);
		
		if(caster == null)return;
		
		if(_ordreJeu.get(_curPlayer).getGUID() != caster.getGUID())//Si ce n'est pas a lui de jouer
			return;
		
		if(perso.getObjetByPos(Constants.ITEM_POS_ARME) == null)//S'il n'a pas de CaC
		{
			if(_curFighterPA < 4)//S'il n'a pas assez de PA
				return;
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, perso.get_GUID());
			//Si le joueur est invisible
			if(caster.isHide())caster.unHide();
			
			Fighter target = _map.getCase(cellID).getFirstFighter();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.get_GUID()+"", cellID+"");
			
			if(target != null)
			{
				int dmg = Formulas.getRandomJet("1d5+0");
				int finalDommage = Formulas.calculFinalDommage(this,caster, target,Constants.ELEMENT_NEUTRE, dmg,false);
				
				finalDommage = SpellEffect.applyOnHitBuffs(finalDommage,target,caster,this);//S'il y a des buffs spéciaux
				
				if(finalDommage>target.getPDV())finalDommage = target.getPDV();//Target va mourrir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, caster.getGUID()+"", target.getGUID()+","+finalDommage);
			}
			_curFighterPA-= 4;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102,perso.get_GUID()+"",perso.get_GUID()+",-4");
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());
			
			if(target.getPDV() <=0)
				onFighterDie(target);
			verifIfTeamAllDead();
		}else
		{
			Objet arme = perso.getObjetByPos(Constants.ITEM_POS_ARME);
			int PACost = arme.getTemplate().getPACost();
			
			if(_curFighterPA < PACost)//S'il n'a pas assez de PA
			{
				
				return;
			}
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, perso.get_GUID());
			boolean isEc = arme.getTemplate().getTauxEC() != 0 && Formulas.getRandomValue(1, arme.getTemplate().getTauxEC()) == arme.getTemplate().getTauxEC();
			if(isEc)
			{
				GameServer.addToLog(perso.get_name()+" Echec critique sur le CaC ");
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.get_GUID()+"", "");//Echec Critique Cac
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());//Fin de l'action
				endTurn();
			}else
			{
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.get_GUID()+"", cellID+"");
				boolean isCC = caster.testIfCC(arme.getTemplate().getTauxCC());
				if(isCC)
				{
					GameServer.addToLog(perso.get_name()+" Coup critique sur le CaC");
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, perso.get_GUID()+"", "0");
				}
				
				//Si le joueur est invisible
				if(caster.isHide())caster.unHide();
				
				ArrayList<SpellEffect> effets = arme.getEffects();
				if(isCC)
				{
					effets = arme.getCritEffects();
				}
				for(SpellEffect SE : effets)
				{
					if(_state != Constants.FIGHT_STATE_ACTIVE)break;
					ArrayList<Fighter> cibles = Pathfinding.getCiblesByZoneByWeapon(this,arme.getTemplate().getType(),_map.getCase(cellID),caster.get_fightCell().getID());
					SE.setTurn(0);
					SE.applyToFight(this, caster, cibles);
				}
				_curFighterPA-= PACost;
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102,perso.get_GUID()+"",perso.get_GUID()+",-"+PACost);
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());
				verifIfTeamAllDead();
			}
		}
	}
	
	public Fighter getFighterByPerso(Personnage perso)
	{
		Fighter fighter = null;
		if(_team0.get(perso.get_GUID()) != null)
			fighter = _team0.get(perso.get_GUID());
		if(_team1.get(perso.get_GUID()) != null)
			fighter = _team1.get(perso.get_GUID());
		return fighter;
	}

	public Fighter getCurFighter()
	{
		return _ordreJeu.get(_curPlayer);
	}

	public void refreshCurPlayerInfos()
	{
		_curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PA) - _curFighterUsedPA;
		_curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constants.STATS_ADD_PM) - _curFighterUsedPM;
	}

	public void leftFight(Personnage perso)
	{
		if(perso == null)return;
		Fighter F = this.getFighterByPerso(perso);
		GameServer.addToLog(perso.get_name()+" a quitter le combat");
		if(F != null)
		{
			if(_state >= Constants.FIGHT_STATE_ACTIVE)
			{
				onFighterDie(F);		
				verifIfTeamAllDead();
				F.setLeft(true);
				//si le combat n'est pas terminé
				if(_state == Constants.FIGHT_STATE_ACTIVE)
				{
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());
					if(perso.isOnline())
					{
						/*
						PrintWriter out = perso.get_compte().getGameThread().get_out();
						int win = -1;
						//*/
						SocketManager.GAME_SEND_GV_PACKET(perso);
						//SocketManager.GAME_SEND_FIGHT_GE_PACKET(out,this,win);
					}
					//si c'était a son tour de jouer
					if(_ordreJeu.get(_curPlayer) == null)return;
					if(_ordreJeu.get(_curPlayer).getGUID() == F.getGUID())
					{
						endTurn();
					}
				}
			}
			else
				F.setLeft(true);
		}else//Si perso en spec
		{
			SocketManager.GAME_SEND_GV_PACKET(perso);
			_spec.remove(perso.get_GUID());
		}
		perso.setSitted(false);
		perso.set_fight(null);
		perso.set_away(false);
	}
	
	public String getGTL()
	{
		String packet = "GTL";
		for(Fighter f: get_ordreJeu())
		{
			packet += "|"+f.getGUID();
		}
		return packet+(char)0x00;
	}

	public int getNextLowerFighterGuid()
	{
		int g = -1;
		for(Fighter f : getFighters(3))
		{
			if(f.getGUID() < g)
				g = f.getGUID();
		}
		g--;
		return g;
	}

	public void addFighterInTeam(Fighter f, int team)
	{
		if(team == 0)
			_team0.put(f.getGUID(), f);
		else if (team == 1)
			_team1.put(f.getGUID(), f);
	}

	public String parseFightInfos()
	{
		String infos = _id+";";
		long time = System.nanoTime()-_startTime;
		infos += (_startTime  == 0?"-1":time)+";";
		//Team1
		infos += "0,";//0 car toujours joueur :)
		infos += _init0.getPersonnage().get_align()+",";
		infos += _team0.size()+";";
		//Team2
		switch(_type)
		{
			case Constants.FIGHT_TYPE_CHALLENGE:
				infos += "0,";
				infos += _init1.getPersonnage().get_align()+",";
				infos += _team1.size()+";";
			break;
			
			case Constants.FIGHT_TYPE_AGRESSION:
				infos += "0,";
				infos += _init1.getPersonnage().get_align()+",";
				infos += _team1.size()+";";
			break;
			
			case Constants.FIGHT_TYPE_PVM:
				infos += "1,";//FIXME: valeur pour Monstres ? :o
				infos += _team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign()+",";
				infos += _team1.size()+";";
			break;
		}
		return infos;
	}

	public void showCaseToTeam(int guid, int cellID)
	{
		int teams = getTeamID(guid)-1;
		if(teams == 4)return;//Les spectateurs ne montrent pas
		ArrayList<PrintWriter> PWs = new ArrayList<PrintWriter>();
		if(teams == 0)
		{
			for(Entry<Integer,Fighter> e : _team0.entrySet())
			{
				if(e.getValue().getPersonnage() != null)
					PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
			}
		}
		else if(teams == 1)
		{
			for(Entry<Integer,Fighter> e : _team1.entrySet())
			{
				if(e.getValue().getPersonnage() != null)
					PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
			}
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}
	
	public void showCaseToAll(int guid, int cellID)
	{
		ArrayList<PrintWriter> PWs = new ArrayList<PrintWriter>();
		for(Entry<Integer,Fighter> e : _team0.entrySet())
		{
			if(e.getValue().getPersonnage() != null)
				PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
		}
		for(Entry<Integer,Fighter> e : _team1.entrySet())
		{
			if(e.getValue().getPersonnage() != null)
				PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
		}
		for(Entry<Integer,Personnage> e : _spec.entrySet())
		{
			PWs.add(e.getValue().get_compte().getGameThread().get_out());
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}

	public void joinAsSpect(Personnage p)
	{
		if(!specOk  || _state != Constants.FIGHT_STATE_ACTIVE)
		{
			SocketManager.GAME_SEND_Im_PACKET(p, "157");
			return;
		}
		p.get_curCell().removePlayer(p.get_GUID());
		SocketManager.GAME_SEND_GJK_PACKET(p, _state, 0, 0, 1, 0, _type);
		SocketManager.GAME_SEND_GS_PACKET(p);
		SocketManager.GAME_SEND_GTL_PACKET(p,this);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.get_curCarte(), p.get_GUID());
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this,_map,p);
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET(p,_ordreJeu.get(_curPlayer).getGUID(),Ancestra.TIME_BY_TURN);
		_spec.put(p.get_GUID(), p);
		p.set_fight(this);
		SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "036;"+p.get_name());
	}
	
	public boolean verifyStillInFight()	//Return true si au moins un joueur est encore dans le combat
	{
		boolean delFight = false;
		for(Fighter f : _team0.values())
		{
			if(!f.isInvocation() 
			&& !f.isDead()
			&& f.getPersonnage() == null
			&& f.getMob() != null)	//Si c'est un groupe de monstre... TODO : Verifier que ce n'est pas un percepteur
			{
				break;
			}
			if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null
					&& f.getPersonnage().get_fight().get_id() == this.get_id()) //Si il n'est plus dans ce combat
			{
				return true;
			}
		}
		for(Fighter f : _team1.values())
		{
			if(!f.isInvocation() 
			&& !f.isDead()
			&& f.getPersonnage() == null
			&& f.getMob() != null)	//Si c'est un groupe de monstre... TODO : Verifier que ce n'est pas un percepteur
			{
				break;
			}
			if(f.getPersonnage().get_fight() != null
					&& f.getPersonnage().get_fight().get_id() == this.get_id()) //Si il n'est plus dans ce combat
			{
				return true;
			}
		}
		
		return false;
	}
}
