package objects;

import game.*;
import game.GameThread.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.Timer;
import objects.Fight.*;
import objects.Guild.Percepteur;
import objects.Monstre.*;
import objects.NPC_tmpl.*;
import java.util.TreeMap;
import java.util.Map.Entry;
import common.*;
import common.World.*;

public class Carte {
	private int _id;
	private String _date;
	private int _w;
	private int _h;
	private String _key;
	private String _placesStr;
	private Map<Integer,Case> 		_cases 			= new TreeMap<Integer,Case>();
	private Map<Integer,Fight> 		_fights 		= new TreeMap<Integer,Fight>();
	private ArrayList<MobGrade> 	_mobPossibles 	= new ArrayList<MobGrade>();
	private Map<Integer,MobGroup> 	_mobGroups 		= new TreeMap<Integer,MobGroup>();
	private Map<Integer,MobGroup> 	_fixMobGroups 		= new TreeMap<Integer,MobGroup>();
	private Map<Integer,NPC>		_npcs	 		= new TreeMap<Integer, NPC>();
	private int _nextObjectID = -1;
	private int _X = 0;
	private int _Y = 0;
	private SubArea _subArea;
	private MountPark _mountPark;
	private int _maxGroup = 3;
	private Map<Integer,ArrayList<Action>> _endFightAction = new TreeMap<Integer,ArrayList<Action>>();
	
	/*MARTHIEUBEAN*/
	private int _maxSize;	//Indique nombre maximal de monstre possible dans un groupe
	private Percepteur perco;
	/*FIN*/
	
	public static class MountPark
	{
		private int _owner;
		private InteractiveObject _door;
		private int _size;
		private ArrayList<Case> _cases = new ArrayList<Case>();
		private Guild _guild;
		private Carte _map;
		private int _price;
		
		public MountPark(int owner, Carte map, int size,String data, int guild,int price)
		{
			_owner = owner;
			_door = map.getMountParkDoor();
			_size = size;
			_guild = null;//TODO:World.getGuild(guild);
			_map = map;
			_price = price;
			if(_map != null)_map.setMountPark(this);
		}

		public int get_owner() {
			return _owner;
		}

		public InteractiveObject get_door() {
			return _door;
		}

		public int get_size() {
			return _size;
		}

		public Guild get_guild() {
			return _guild;
		}

		public Carte get_map() {
			return _map;
		}

		public int get_price() {
			return _price;
		}

		public int getObjectNumb()
		{
			int n = 0;
			for(Case C : _cases)if(C.getObject() != null)n++;
			return n;
		}

		public String parseData()
		{
			String str ="";
			return str;
		}
		
	}
	
	public static class InteractiveObject
	{
		private int _id;
		private int _state;
		private Carte _map;
		private Case _cell;
		private boolean _interactive = true;
		private Timer _respawnTimer;
		private IOTemplate _template;
		
		public InteractiveObject(Carte a_map,Case a_cell,int a_id)
		{
			_id = a_id;
			_map = a_map;
			_cell = a_cell;
			_state = Constants.IOBJECT_STATE_FULL;
			int respawnTime = 10000;
			_template = World.getIOTemplate(_id);
			if(_template != null)respawnTime = _template.getRespawnTime();
			//définition du timer
			_respawnTimer = new Timer(respawnTime,
					new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							_respawnTimer.stop();
							_state = Constants.IOBJECT_STATE_FULLING;
							_interactive = true;
							SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(_map, _cell);
							_state = Constants.IOBJECT_STATE_FULL;
						}
					}
			);
		}
		
		public int getID()
		{
			return _id;
		}
		
		public boolean isInteractive()
		{
			return _interactive;
		}
		
		public void setInteractive(boolean b)
		{
			_interactive = b;
		}
		
		public int getState()
		{
			return _state;
		}
		
		public void setState(int state)
		{
			_state = state;
		}

		public int getUseDuration()
		{
			int duration = 1500;
			if(_template != null)
			{
				duration = _template.getDuration();
			}
			return duration;
		}

		public void startTimer()
		{
			if(_respawnTimer == null)return;
			_state = Constants.IOBJECT_STATE_EMPTY2;
			_respawnTimer.restart();
		}

		public int getUnknowValue()
		{
			int unk = 4;
			if(_template != null)
			{
				unk = _template.getUnk();
			}
			return unk;
		}

		public boolean isWalkable()
		{
			if(_template == null)return false;
			return _template.isWalkable() && _state == Constants.IOBJECT_STATE_FULL;
		}
	}
	
	public static class Case
	{
		private int _id;
		private Map<Integer, Personnage>	_persos;//		= new TreeMap<Integer, Personnage>();
		private Map<Integer, Fighter> 		_fighters;//	= new TreeMap<Integer, Fighter>();
		private boolean _Walkable = true;
		private boolean _LoS = true;
		private int _map;
		//private ArrayList<Action> _onCellPass;
		//private ArrayList<Action> _onItemOnCell;
		private ArrayList<Action> _onCellStop;// = new ArrayList<Action>();
		private InteractiveObject _object;
		private Objet _droppedItem;
		
		public Case(Carte a_map,int id,boolean _walk,boolean LoS, int objID)
		{
			_map = a_map.get_id();
			_id = id;
			_Walkable = _walk;
			_LoS = LoS;
			if(objID == -1)return;
			_object = new InteractiveObject(a_map,this,objID);
		}
		
		public InteractiveObject getObject()
		{
			return _object;
		}
		public Objet getDroppedItem()
		{
			return _droppedItem;
		}
		public boolean canDoAction(int id)
		{
			switch(id)
			{
				//Moudre et egrenner - Paysan
				case 122:
				case 47:
					return _object.getID() == 7007;
				//Faucher Blé
				case 45:
					switch(_object.getID())
					{
						case 7511://Blé
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Faucher Orge
				case 53:
					switch(_object.getID())
					{
						case 7515://Orge
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				
				//Faucher Avoine
				case 57:
					switch(_object.getID())
					{
						case 7517://Avoine
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;	
				//Faucher Houblon
				case 46:
					switch(_object.getID())
					{
						case 7512://Houblon
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Faucher Lin
				case 50:
				case 68:
					switch(_object.getID())
					{
						case 7513://Lin
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Faucher Riz
				case 159:
					switch(_object.getID())
					{
						case 7550://Riz
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Faucher Seigle
				case 52:
					switch(_object.getID())
					{
						case 7516://Seigle
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Faucher Malt
				case 58:
					switch(_object.getID())
					{
						case 7518://Malt
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;			
				//Faucher Chanvre - Cueillir Chanvre
				case 69:
				case 54:
					switch(_object.getID())
					{
						case 7514://Chanvre
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Scier - Bucheron
				case 101:
					return _object.getID() == 7003;
				//Couper Frêne
				case 6:
					switch(_object.getID())
					{
						case 7500://Frêne
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Châtaignier
				case 39:
					switch(_object.getID())
					{
						case 7501://Châtaignier
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Noyer
				case 40:
					switch(_object.getID())
					{
						case 7502://Noyer
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Chêne
				case 10:
					switch(_object.getID())
					{
						case 7503://Chêne
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Oliviolet
				case 141:
					switch(_object.getID())
					{
						case 7542://Oliviolet
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Bombu
				case 139:
					switch(_object.getID())
					{
						case 7541://Bombu
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Erable
				case 37:
					switch(_object.getID())
					{
						case 7504://Erable
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Bambou
				case 154:
					switch(_object.getID())
					{
						case 7553://Bambou
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper If
				case 33:
					switch(_object.getID())
					{
						case 7505://If
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Merisier
				case 41:
					switch(_object.getID())
					{
						case 7506://Merisier
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Ebène
				case 34:
					switch(_object.getID())
					{
						case 7507://Ebène
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Kalyptus
				case 174:
					switch(_object.getID())
					{
						case 7557://Kalyptus
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Charme
				case 38:
					switch(_object.getID())
					{
						case 7508://Charme
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Orme
				case 35:
					switch(_object.getID())
					{
						case 7509://Orme
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Bambou Sombre
				case 155:
					switch(_object.getID())
					{
						case 7554://Bambou Sombre
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Couper Bambou Sacré
				case 158:
					switch(_object.getID())
					{
						case 7552://Bambou Sacré
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Puiser
				case 102:
					switch(_object.getID())
					{
						case 7519://Puits
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Polir
				case 48:
					return _object.getID() == 7510;
				//Moule/Fondre - Mineur
				case 32:
					return _object.getID() == 7002;
				//Miner Fer
				case 24:
					switch(_object.getID())
					{
						case 7520://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Cuivre
				case 25:
					switch(_object.getID())
					{
						case 7522://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Bronze
				case 26:
					switch(_object.getID())
					{
						case 7523://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Kobalte
				case 28:
					switch(_object.getID())
					{
						case 7525://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Manga
				case 56:
					switch(_object.getID())
					{
						case 7524://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Sili
				case 162:
					switch(_object.getID())
					{
						case 7556://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Etain
				case 55:
					switch(_object.getID())
					{
						case 7521://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Argent
				case 29:
					switch(_object.getID())
					{
						case 7526://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Bauxite
				case 31:
					switch(_object.getID())
					{
						case 7528://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Or
				case 30:
					switch(_object.getID())
					{
						case 7527://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Miner Dolomite
				case 161:
					switch(_object.getID())
					{
						case 7555://Miner
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Fabriquer potion - Alchimiste
				case 23:
					return _object.getID() == 7019;
				//Cueillir Trèfle
				case 71:
					switch(_object.getID())
					{
						case 7533://Trèfle
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Cueillir Menthe
				case 72:
					switch(_object.getID())
					{
						case 7534://Menthe
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Cueillir Orchidée
				case 73:
					switch(_object.getID())
					{
						case 7535:// Orchidée
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Cueillir Edelweiss
				case 74:
					switch(_object.getID())
					{
						case 7536://Edelweiss
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Cueillir Graine de Pandouille
				case 160:
					switch(_object.getID())
					{
						case 7551://Graine de Pandouille
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Vider - Pêcheur
				case 133:
					return _object.getID() == 7024;
				//Pêcher Petits poissons de mer
				case 128:
					switch(_object.getID())
					{
						case 7530://Petits poissons de mer
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Petits poissons de rivière
				case 124:
					switch(_object.getID())
					{
						case 7529://Petits poissons de rivière
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Pichon
				case 136:
					switch(_object.getID())
					{
						case 7544://Pichon
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Ombre Etrange
				case 140:
					switch(_object.getID())
					{
						case 7543://Ombre Etrange
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Poissons de rivière
				case 125:
					switch(_object.getID())
					{
						case 7532://Poissons de rivière
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Poissons de mer
				case 129:
					switch(_object.getID())
					{
						case 7531://Poissons de mer
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Gros poissons de rivière
				case 126:
					switch(_object.getID())
					{
						case 7537://Gros poissons de rivière
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Gros poissons de mers
				case 130:
					switch(_object.getID())
					{
						case 7538://Gros poissons de mers
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Poissons géants de rivière
				case 127:
					switch(_object.getID())
					{
						case 7539://Poissons géants de rivière
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Pêcher Poissons géants de mer
				case 131:
					switch(_object.getID())
					{
						case 7540://Poissons géants de mer
							return _object.getState() == Constants.IOBJECT_STATE_FULL;
					}
				return false;
				//Boulanger
				case 109://Pain
				case 27://Bonbon
					return _object.getID() == 7001;
				//Poissonier
				case 135://Faire un poisson (mangeable)
					return _object.getID() == 7022;
				//Chasseur
				case 134://
					return _object.getID() == 7023;
				//Boucher
				case 132://
					return _object.getID() == 7025;
				case 44://Sauvegarder
				case 114://Utiliser le Zaap
					switch(_object.getID())
					{
						//Zaaps
						case 7000:
						case 7026:
						case 7029:
						case 4287:
							return true;
					}
				return false;
				case 157://Utiliser le Zaapi
					switch(_object.getID())
					{
						//Zaaps
						case 7031:
						case 7030:
							return true;
					}
				return false;
				
				case 175://Accéder
				case 176://Acheter
				case 177://Vendre
				case 178://Modifier le prix de vente
					switch(_object.getID())
					{
						//Enclos
						case 6763:
						case 6766:
						case 6767:
						case 6772:
							return true;
					}
				return false;
				
				//Se rendre à incarnam
				case 183:
					switch(_object.getID())
					{
						case 1845:
						case 1853:
						case 1854:
						case 1855:
						case 1856:
						case 1857:
						case 1858:
						case 1859:
						case 1860:
						case 1861:
						case 1862:
						case 2319:
							return true;
					}
				return false;
				
				//Enclume magique
				case  1:
				case 113:
				case 115:
				case 116:
				case 117:
				case 118:
				case 119:
				case 120:
					return _object.getID() == 7020;

				//Enclume
				case 19:
				case 143:
				case 145:
				case 144:
				case 142:
				case 146:
				case 67:
				case 21:
				case 65:
				case 66:
				case 20:
				case 18:
					return _object.getID() == 7012;

				//Costume Mage
				case 167:
				case 165:
				case 166:
					return _object.getID() == 7036;

				//Coordo Mage
				case 164:
				case 163:
					return _object.getID() == 7037;

				//Joai Mage
				case 168:
				case 169:
					return _object.getID() == 7038;

				//Bricoleur
				case 171:
				case 182:
					return _object.getID() == 7039;

				//Forgeur Bouclier
				case 156:
					return _object.getID() == 7027;

				//Coordonier
				case 13:
				case 14:
					return _object.getID() == 7011;

				//Tailleur (Dos)
				case 123:
				case 64:
					return _object.getID() == 7015;


				//Sculteur
				case 17:
				case 16:
				case 147:
				case 148:
				case 149:
				case 15:
					return _object.getID() == 7013;

				//Tailleur (Haut)
				case 63:
					return (_object.getID() == 7014 || _object.getID() == 7016);
				//Atelier : Créer Amu // Anneau
				case 11:
				case 12:
					return (_object.getID() >= 7008 && _object.getID() <= 7010);

				//Action ID non trouvé
				default:
					GameServer.addToLog("MapActionID non existant dans Case.canDoAction: "+id);
					return false;
			}
		}
		
		public int getID()
		{
			return _id;
		}
		
		public void addOnCellStopAction(int id, String args, String cond)
		{
			if(_onCellStop == null)
				_onCellStop = new ArrayList<Action>();
			
			_onCellStop.add(new Action(id,args,cond));
		}
		
		public void applyOnCellStopActions(Personnage perso)
		{
			if(_onCellStop == null)
				return;
			
			for(Action act : _onCellStop)
			{
				act.apply(perso,-1);
			}
		}
		public void addPerso(Personnage perso)
		{
			if(_persos == null)
				_persos = new TreeMap<Integer, Personnage>();
			
			_persos.put(perso.get_GUID(),perso);
			
		}
		public void addFighter(Fighter fighter)
		{
			if(_fighters == null)
				_fighters = new TreeMap<Integer, Fighter>();
			
			_fighters.put(fighter.getGUID(),fighter);
		}
		public boolean isWalkable(boolean useObject)
		{
			if(_object != null && useObject)return _Walkable && _object.isWalkable();
			return _Walkable;
		}
		public boolean blockLoS()
		{
			if(_fighters == null)
				return _LoS;
			
			boolean fighter = true;
			for(Entry<Integer,Fighter> f : _fighters.entrySet())
			{
				if(!f.getValue().isHide())fighter = false;
			}
			return _LoS && fighter;
		}
		public boolean isLoS()
		{
			return _LoS;
		}
		public void removePlayer(int _guid)
		{
			if(_persos == null)
				return;
			
			_persos.remove(_guid);
			
			if(_persos.size() == 0)
				_persos = null;
		}
		public Map<Integer, Personnage> getPersos()
		{
			if(_persos == null)
				return new TreeMap<Integer, Personnage>();
			
			return _persos;
		}
		public Map<Integer, Fighter> getFighters()
		{
			if(_fighters == null)
				return new TreeMap<Integer, Fighter>();
			
			return _fighters;
		}
		public Fighter getFirstFighter()
		{
			if(_fighters == null)
				return null;
			
			for(Entry<Integer,Fighter> entry : _fighters.entrySet())
			{
				return entry.getValue();
			}
			return null;
		}

		public void startAction(Personnage perso, GameAction GA)
		{
			int actionID = -1;
			try
			{
				actionID = Integer.parseInt(GA._args.split(";")[1]);
			}catch(Exception e){e.printStackTrace();}
			if(actionID == -1)return;
			
			if(Constants.isJobAction(actionID))
			{
				perso.doJobAction(actionID,_object,GA,this);
				return;
			}
			switch(actionID)
			{
				case 44://Sauvegarder pos
					String str = _map+","+_id;
					perso.set_savePos(str);
					SocketManager.GAME_SEND_Im_PACKET(perso, "06");
				break;
			
				case 102://Puiser
					if(!_object.isInteractive())return;//Si l'objet est utilisé
					if(_object.getState() != Constants.IOBJECT_STATE_FULL)return;//Si le puits est vide
					_object.setState(Constants.IOBJECT_STATE_EMPTYING);
					_object.setInteractive(false);
					SocketManager.GAME_SEND_GA_PACKET_TO_MAP(perso.get_curCarte(),""+GA._id, 501, perso.get_GUID()+"", _id+","+_object.getUseDuration()+","+_object.getUnknowValue());
					SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(perso.get_curCarte(),this);
				break;
				case 114://Utiliser (zaap)
					perso.openZaapMenu();
					perso.get_compte().getGameThread().removeAction(GA);
				break;
				case 157://Utiliser Zaapi
					perso.openZaapiMenu();
					perso.get_compte().getGameThread().removeAction(GA);
				break;
				case 175://Acceder a un enclos
					if(_object.getState() != Constants.IOBJECT_STATE_EMPTY);
					//SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(perso.get_curCarte(),this);
					perso.openMountPark();
				break;
				
				case 183://Retourner sur Incarnam
					if(perso.get_lvl()>15)
					{
						SocketManager.GAME_SEND_Im_PACKET(perso, "1127");
						perso.get_compte().getGameThread().removeAction(GA);
						return;
					}
					int mapID  = Constants.getStartMap(perso.get_classe());
					int cellID = Constants.getStartCell(perso.get_classe());
					perso.teleport(mapID, cellID);
					perso.get_compte().getGameThread().removeAction(GA);
				break;
				
				default:
					GameServer.addToLog("Case.startAction non définie pour l'actionID = "+actionID);
				break;
			}
		}
		public void finishAction(Personnage perso, GameAction GA)
		{
			int actionID = -1;
			try
			{
				actionID = Integer.parseInt(GA._args.split(";")[1]);
			}catch(Exception e){}
			if(actionID == -1)return;
			
			if(Constants.isJobAction(actionID))
			{
				perso.finishJobAction(actionID,_object,GA,this);
				return;
			}
			switch(actionID)
			{
				case 44://Sauvegarder a un zaap
				break;
				
				case 102://Puiser
					_object.setState(Constants.IOBJECT_STATE_EMPTY);
					_object.setInteractive(false);
					_object.startTimer();
					SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(perso.get_curCarte(),this);
					int qua = Formulas.getRandomValue(1, 10);//On a entre 1 et 10 eaux
					Objet obj = World.getObjTemplate(311).createNewItem(qua, false);
					if(perso.addObjet(obj, true))
						World.addObjet(obj,true);
					SocketManager.GAME_SEND_IQ_PACKET(perso,perso.get_GUID(),qua);
				break;
				
				case 183:
				break;
				
				default:
					GameServer.addToLog("[FIXME]Case.finishAction non définie pour l'actionID = "+actionID);
				break;
			}
		}

		public void clearOnCellAction()
		{
			//_onCellStop.clear();
			_onCellStop = null;
		}

		public void addDroppedItem(Objet obj)
		{
			_droppedItem = obj;
		}

		public void clearDroppedItem()
		{
			_droppedItem = null;
		}
	}
	
	
	public Carte(int _id, String _date, int _w, int _h, String _key, String places,String dData,String monsters,String mapPos,int maxGroup,int maxSize)	//MARTHIEUBEAN
	{
		this._id = _id;
		this._date = _date;
		this._w = _w;
		this._h = _h;
		this._key = _key;
		this._placesStr = places;
		this._maxGroup = maxGroup;
		this._maxSize = maxSize;	//MARTHIEUBEAN
		String[] mapInfos = mapPos.split(",");
		try
		{
			this._X = Integer.parseInt(mapInfos[0]);
			this._Y = Integer.parseInt(mapInfos[1]);
			int subArea = Integer.parseInt(mapInfos[2]);
			_subArea = World.getSubArea(subArea);
			if(_subArea != null)_subArea.addMap(this);
		}catch(Exception e)
		{
			GameServer.addToLog("Erreur de chargement de la map "+_id+": Le champ MapPos est invalide");
			System.exit(0);
		}
		
		if(Ancestra.CONFIG_COMPILED_MAP)
		{
			_cases = CryptManager.DecompileMapData(this,dData);
		}
		else
		{
			String cellsData = dData;
			String[] cellsDataArray = cellsData.split("\\|");
			
			for(String o : cellsDataArray)
			{
				
				boolean Walkable = true;
				boolean LineOfSight = true;
				int Number = -1;
				int obj = -1;
				String[] cellInfos = o.split(",");
				try
				{
					Walkable = cellInfos[2].equals("1");
					LineOfSight = cellInfos[1].equals("1");
					Number = Integer.parseInt(cellInfos[0]);
					if(!cellInfos[3].trim().equals(""))
					{
						obj = Integer.parseInt(cellInfos[3]);
					}
				}catch(Exception d){};
				if(Number == -1)continue;
				
	            _cases.put(Number, new Case(this,Number,Walkable,LineOfSight,obj));	
			}
		}
		
		for(String mob : monsters.split("\\|"))
		{
			if(mob.equals(""))continue;
			int id = 0;
			int lvl = 0;
			
			try
			{
				id = Integer.parseInt(mob.split(",")[0]);
				lvl = Integer.parseInt(mob.split(",")[1]);
			}catch(NumberFormatException e){continue;};
			if(id == 0 || lvl == 0)continue;
			if(World.getMonstre(id) == null)continue;
			if(World.getMonstre(id).getGradeByLevel(lvl) == null)continue;
			_mobPossibles.add(World.getMonstre(id).getGradeByLevel(lvl));
		}
		if(_cases.size() == 0)return;
		
		for(int id : Constants.NO_MOBS_MAP)
		{
			if(id == _id)return;
		}
		
		if (Ancestra.CONFIG_USE_MOBS)
		{
			spawnGroup(Constants.ALIGNEMENT_NEUTRE,_maxGroup,false,-1);//Spawn des groupes d'alignement neutre 
			spawnGroup(Constants.ALIGNEMENT_BONTARIEN,1,false,-1);//Spawn du groupe de gardes bontarien s'il y a
			spawnGroup(Constants.ALIGNEMENT_BRAKMARIEN,1,false,-1);//Spawn du groupe de gardes brakmarien s'il y a
		}
	}

	public void applyEndFightAction(int type,Personnage perso)
	{
		System.out.println("Type: "+type+" Perso: "+perso.get_name());
		if(_endFightAction.get(type) == null)return;
		for(Action A : _endFightAction.get(type))A.apply(perso,-1);
	}
	public void addEndFightAction(int type,Action A)
	{
		if(_endFightAction.get(type) == null)_endFightAction.put(type, new ArrayList<Action>());
		//On retire l'action si elle existait déjà
		delEndFightAction(type,A.getID());
		_endFightAction.get(type).add(A);
	}
	public void delEndFightAction(int type,int aType)
	{
		if(_endFightAction.get(type) == null)return;
		ArrayList<Action> copy = new ArrayList<Action>();
		copy.addAll(_endFightAction.get(type));
		for(Action A : copy)if(A.getID() == aType)_endFightAction.get(type).remove(A);
	}
	public void setMountPark(MountPark mountPark)
	{
		_mountPark = mountPark;
	}
	public MountPark getMountPark()
	{
		return _mountPark;
	}
	public Carte(int id, String date, int w, int h, String key, String places)
	{
		_id = id;
		_date = date;
		_w = w;
		_h = h;
		_key = key;
		_placesStr = places;
		_cases = new TreeMap<Integer,Case>();
	}
	
	public SubArea getSubArea()
	{
		return _subArea;
	}
	
	public int getX() {
		return _X;
	}

	public int getY() {
		return _Y;
	}
	
	public Map<Integer, NPC> get_npcs() {
		return _npcs;
	}

	public NPC addNpc(int npcID,int cellID, int dir)
	{
		NPC_tmpl temp = World.getNPCTemplate(npcID);
		if(temp == null)return null;
		if(getCase(cellID) == null)return null;
		NPC npc = new NPC(temp,_nextObjectID,cellID,dir);
		_npcs.put(_nextObjectID, npc);
		_nextObjectID--;
		return npc;
	}
	
	public void spawnGroup(int align, int nbr,boolean log,int cellID)
	{
		if(nbr<1)return;
		if(_mobGroups.size() - _fixMobGroups.size() >= _maxGroup)return;
		for(int a = 1; a<=nbr;a++)
		{
			MobGroup group  = new MobGroup(_nextObjectID,align,_mobPossibles,this,cellID,this._maxSize);
			if(group.getMobs().size() == 0)continue;
			_mobGroups.put(_nextObjectID, group);
			if(log)
			{
				GameServer.addToLog("Groupe de monstres ajoutés sur la map: "+_id+" alignement: "+align+" ID: "+_nextObjectID);
				SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
			}
			_nextObjectID--;
		}
	}
	public void spawnGroup(boolean timer,boolean log,int cellID,String groupData,String condition)
	{
		MobGroup group = new MobGroup(_nextObjectID, cellID, groupData);
		if(group.getMobs().size() == 0)return;
		_mobGroups.put(_nextObjectID, group);
		group.setCondition(condition);
		group.setIsFix(false);
		
		if(log)
		{
			GameServer.addToLog("Groupe de monstres ajoutés sur la map: "+_id+" ID: "+_nextObjectID);
		}
		SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
		_nextObjectID--;
		
		if(timer)
			group.startCondTimer();
	}
	
	public void addStaticGroup(int cellID,String groupData)
	{
		MobGroup group = new MobGroup(_nextObjectID,cellID,groupData);
		if(group.getMobs().size() == 0)return;
		_mobGroups.put(_nextObjectID, group);
		_nextObjectID--;
		_fixMobGroups.put(-1000+_nextObjectID, group);
		SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
	}
	
	public void setPlaces(String place)
	{
		_placesStr = place;
	}
	public void removeFight(int id)
	{
		_fights.remove(id);
	}

	public NPC getNPC(int id)
	{
		return _npcs.get(id);
	}
	
	public Case getCase(int id)
	{
		return _cases.get(id);
	}
	
	public ArrayList<Personnage> getPersos()
	{
		ArrayList<Personnage> persos = new ArrayList<Personnage>();
		for(Case c : _cases.values())for(Personnage entry : c.getPersos().values())persos.add(entry);
		return persos;
	}
	public int get_id() {
		return _id;
	}

	public String get_date() {
		return _date;
	}

	public int get_w() {
		return _w;
	}

	public int get_h() {
		return _h;
	}

	public String get_key() {
		return _key;
	}

	public String get_placesStr() {
		return _placesStr;
	}

	public void addPlayer(Personnage perso)
	{
		SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this,perso);
		perso.get_curCell().addPerso(perso);
	}

	public String getGMsPackets()
	{
		String packets = "";

		for(Case cell : _cases.values())for(Personnage perso : cell.getPersos().values())packets += "GM|+"+perso.parseToGM()+'\u0000';
		return packets;
	}
	public String getFightersGMsPackets()
	{
		String packets = "";
		for(Entry<Integer,Case> cell : _cases.entrySet())
		{
			for(Entry<Integer,Fighter> f : cell.getValue().getFighters().entrySet())
			{
				packets += f.getValue().getGmPacket()+'\u0000';
			}
		}
		return packets;
	}
	public String getMobGroupGMsPackets()
	{
		if(_mobGroups.size() == 0)return "";
		String packets = "GM|";
		boolean isFirst = true;
		for(MobGroup entry : _mobGroups.values())
		{
			String GM = entry.parseGM();
			if(GM.equals(""))continue;
			
			if(!isFirst)
				packets += "|";
			
			packets += GM;
			isFirst = false;
		}
		return packets;
	}
	
	public String getNpcsGMsPackets()
	{
		if(_npcs.size() == 0)return "";
		String packets = "GM|";
		boolean isFirst = true;
		for(Entry<Integer,NPC> entry : _npcs.entrySet())
		{
			String GM = entry.getValue().parseGM();
			if(GM.equals(""))continue;
			
			if(!isFirst)
				packets += "|";
			
			packets += GM;
			isFirst = false;
		}
		return packets;
	}
	
	public String getObjectsGDsPackets()
	{
		String packets = "";
		boolean first = true;
		for(Entry<Integer,Case> entry : _cases.entrySet())
		{
			if(entry.getValue().getObject() != null)
			{
				if(!first)packets += (char)0x00;
				first = false;
				int cellID = entry.getValue().getID();
				InteractiveObject object = entry.getValue().getObject();
				packets += "GDF|"+cellID+";"+object.getState()+";"+(object.isInteractive()?"1":"0");
			}
		}
		return packets;
	}
	
	public int getNbrFight()
	{
		return _fights.size();
	}
	
	public Map<Integer, Fight> get_fights() {
		return _fights;
	}

	public Fight newFight(Personnage init1,Personnage init2,int type)
	{
		int id = 1;
		if(_fights.size() != 0)
			id = ((Integer)(_fights.keySet().toArray()[_fights.size()-1]))+1;
		
		Fight f = new Fight(type,id,this,init1,init2);
		_fights.put(id,f);
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
		return f;
	}
	
	public int getRandomFreeCellID()
	{
		ArrayList<Integer> freecell = new ArrayList<Integer>();
		for(Entry<Integer,Case> entry : _cases.entrySet())
		{
			//Si la case n'est pas marchable
			if(!entry.getValue().isWalkable(true))continue;
			//Si la case est prise par un groupe de monstre
			boolean ok = true;
			for(Entry<Integer,MobGroup> mgEntry : _mobGroups.entrySet())
			{
				if(mgEntry.getValue().getCellID() == entry.getValue().getID())
					ok = false;
			}
			if(!ok)continue;
			//Si la case est prise par un npc
			ok = true;
			for(Entry<Integer,NPC> npcEntry : _npcs.entrySet())
			{
				if(npcEntry.getValue().get_cellID() == entry.getValue().getID())
					ok = false;
			}
			if(!ok)continue;
			//Si la case est prise par un joueur
			if(entry.getValue().getPersos().size() != 0)continue;
			//Sinon
			freecell.add(entry.getValue().getID());
		}
		if(freecell.size() == 0)
		{
			GameServer.addToLog("Aucune cellulle libre n'a ete trouve sur la map "+_id+" : groupe non spawn");
			return -1;
		}
		int rand = Formulas.getRandomValue(0, freecell.size()-1);
		return freecell.get(rand);
		/*
		int max =  _cases.size()-_w;
		int rand = 0;
		int lim = 0;
		boolean isOccuped;
		
		do
		{
			isOccuped = false;
			rand = Formulas.getRandomValue(_w,max);
			if(lim >50)
				return 0;
			for(Entry<Integer,MobGroup> group : _mobGroups.entrySet())
			{
				if (group.getValue().getCellID() != 0)
				{
					if(group.getValue().getCellID() == _cases.get(_cases.keySet().toArray()[rand]).getID())
						isOccuped = true;
				}
			}
			for(Entry<Integer,NPC> npc : _npcs.entrySet())
			{
				if(npc.getValue().get_cellID() == _cases.get(_cases.keySet().toArray()[rand]).getID())
					isOccuped = true;
			}
			
			if (_cases.get(_cases.keySet().toArray()[rand]).isWalkable() && !isOccuped)
			{
				return _cases.get(_cases.keySet().toArray()[rand]).getID();
			}
			
			lim++;
		}while(!_cases.get(_cases.keySet().toArray()[rand]).isWalkable() && !isOccuped);
		
		return 0;
		//*/
	}
	
	public void refreshSpawns()
	{
		for(int id : _mobGroups.keySet())
		{
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this, id);
		}
		_mobGroups.clear();
		_mobGroups.putAll(_fixMobGroups);
		for(MobGroup mg : _fixMobGroups.values())SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, mg);

		spawnGroup(Constants.ALIGNEMENT_NEUTRE,_maxGroup,true,-1);//Spawn des groupes d'alignement neutre 
		spawnGroup(Constants.ALIGNEMENT_BONTARIEN,1,true,-1);//Spawn du groupe de gardes bontarien s'il y a
		spawnGroup(Constants.ALIGNEMENT_BRAKMARIEN,1,true,-1);//Spawn du groupe de gardes brakmarien s'il y a
	}
	
	public void onPlayerArriveOnCell(Personnage perso,int caseID)
	{
		if(_cases.get(caseID) == null)return;
		Objet obj = _cases.get(caseID).getDroppedItem();
		if(obj != null)
		{
			if(perso.addObjet(obj, true))
				World.addObjet(obj, true);
			perso.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Ramassé sur le sol");
			
			SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this,'-',caseID,0,0);
			SocketManager.GAME_SEND_Ow_PACKET(perso);
			_cases.get(caseID).clearDroppedItem();
		}
		_cases.get(caseID).applyOnCellStopActions(perso);
		
		if(_placesStr.equalsIgnoreCase("|")) return;
		//Si le joueur a changer de map ou ne peut etre aggro
		if(perso.get_curCarte().get_id() != _id || !perso.canAggro())return;
		
		for(MobGroup group : _mobGroups.values())
		{
			if(Pathfinding.getDistanceBetween(this,caseID,group.getCellID()) <= group.getAggroDistance())//S'il y aggro
			{
				if((group.getAlignement() == -1 
						|| ((perso.get_align() == 1 || perso.get_align() == 2) && (perso.get_align() != group.getAlignement())))
						&& ConditionParser.validConditions(perso, group.getCondition()))
				{
					
					GameServer.addToLog(perso.get_name()+" lance un combat contre le groupe "+group.getID()+" sur la map "+_id);
					startFigthVersusMonstres(perso,group);
					return;
				}
			}
		}
	}
	
	private void startFigthVersusMonstres(Personnage perso, MobGroup group)
	{
		int id = 1;
		if(_fights.size() != 0)
			id = ((Integer)(_fights.keySet().toArray()[_fights.size()-1]))+1;
		
		if(!group.isFix())_mobGroups.remove(group.getID());
		else SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(this);
		_fights.put(id, new Fight(id,this,perso,group));
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
	}

	public Carte getMapCopy()
	{
		Map<Integer,Case> cases = new TreeMap<Integer,Case>();
		
		Carte map = new Carte(_id,_date,_w,_h,_key,_placesStr);
		
		for(Entry<Integer,Case> entry : _cases.entrySet())
			cases.put(entry.getKey(),
					new Case(
							map,
							entry.getValue().getID(),
							entry.getValue().isWalkable(false),
							entry.getValue().isLoS(),
							(entry.getValue().getObject()==null?-1:entry.getValue().getObject().getID())
							)
						);
		map.setCases(cases);
		return map;
	}

	private void setCases(Map<Integer, Case> cases)
	{
		_cases = cases;
	}

	public InteractiveObject getMountParkDoor()
	{
		for(Case c : _cases.values())
		{
			if(c.getObject() == null)continue;
			//Si enclose
			if(c.getObject().getID() == 6763
			|| c.getObject().getID() == 6766
			|| c.getObject().getID() == 6767
			|| c.getObject().getID() == 6772)
				return c.getObject();

		}
		return null;
	}

	public Map<Integer, MobGroup> getMobGroups()
	{
		return _mobGroups;
	}

	public void removeNpcOrMobGroup(int id)
	{
		_npcs.remove(id);
		_mobGroups.remove(id);
	}

	public int getMaxGroupNumb()
	{
		return _maxGroup;
	}

	public void setMaxGroup(int id)
	{
		_maxGroup = id;
	}

	public Fight getFight(int id)
	{
		return _fights.get(id);
	}

	public void sendFloorItems(Personnage perso)
	{
		for(Case c : _cases.values())
		{
			if(c.getDroppedItem() != null)
			SocketManager.GAME_SEND_GDO_PACKET(perso,'+',c.getID(),c.getDroppedItem().getTemplate().getID(),0);
		}
	}

	public void setPercepteur(Percepteur perco) {
		this.perco = perco;
	}

	public Percepteur getPercepteur() {
		return perco;
	}
	
}
