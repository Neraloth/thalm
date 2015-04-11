package objects;

import game.GameServer;
import game.GameThread.GameAction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.Timer;

import objects.Carte.Case;
import objects.Carte.InteractiveObject;
import objects.Objet.ObjTemplate;
import common.*;

public class Metier {

	public static class StatsMetier
	{
		private int _id;
		private Metier _template;
		private int _lvl;
		private long _xp;
		private ArrayList<JobAction> _posActions = new ArrayList<JobAction>();
		private boolean _isCheap = false;
		private boolean _freeOnFails = false;
		private boolean _noRessource = false;
		private JobAction _curAction;
		
		public StatsMetier(int id,Metier tp,int lvl,long xp)
		{
			_id = id;
			_template = tp;
			_lvl = lvl;
			_xp = xp;
			_posActions = Constants.getPosActionsToJob(tp.getId(),lvl);
		}

		public int get_lvl() {
			return _lvl;
		}
		public boolean isCheap() {
			return _isCheap;
		}

		public void setIsCheap(boolean isCheap) {
			_isCheap = isCheap;
		}

		public boolean isFreeOnFails() {
			return _freeOnFails;
		}

		public void setFreeOnFails(boolean freeOnFails) {
			_freeOnFails = freeOnFails;
		}

		public boolean isNoRessource() {
			return _noRessource;
		}

		public void setNoRessource(boolean noRessource) {
			_noRessource = noRessource;
		}

		public void levelUp(Personnage P,boolean send)
		{
			_lvl++;
			_posActions = Constants.getPosActionsToJob(_template.getId(),_lvl);
			
			if(send)
			{
				//on créer la listes des statsMetier a envoyer (Seulement celle ci)
				ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
				list.add(this);
				SocketManager.GAME_SEND_JS_PACKET(P, list);
				SocketManager.GAME_SEND_STATS_PACKET(P);
				SocketManager.GAME_SEND_Ow_PACKET(P);
				SocketManager.GAME_SEND_JN_PACKET(P,_template.getId(),_lvl);
				SocketManager.GAME_SEND_JO_PACKET(P, list);
			}
		}
		public String parseJS()
		{
			String str = "|"+_template.getId()+";";
			boolean first = true;
			for(JobAction JA : _posActions)
			{
				if(!first)str += ",";
				else first = false;
				str += JA.getSkillID()+"~"+JA.getMin()+"~";
				if(JA.isCraft())str += "0~0~"+JA.getChance();
				else str += JA.getMax()+"~0~"+JA.getTime();
			}
			return str;
		}
		public long getXp()
		{
			return _xp;
		}
		
		public void startAction(int id,Personnage P,InteractiveObject IO,GameAction GA,Case cell)
		{
			for(JobAction JA : _posActions)
			{
				if(JA.getSkillID() == id)
				{
					_curAction = JA;
					JA.startAction(P,IO,GA,cell);
					return;
				}
			}
		}
		
		public void endAction(int id,Personnage P,InteractiveObject IO,GameAction GA,Case cell)
		{
			if(_curAction == null)return;
			_curAction.endAction(P,IO,GA,cell);
			addXp(P,_curAction.getXpWin()*Ancestra.XP_METIER);
			//Packet JX
			//on créer la listes des statsMetier a envoyer (Seulement celle ci)
			ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
			list.add(this);
			SocketManager.GAME_SEND_JX_PACKET(P, list);
		}
		
		public void addXp(Personnage P,long xp)
		{
			if(_lvl >99)return;
			int exLvl = _lvl;
			_xp += xp;
			
			//Si l'xp dépasse le pallier du niveau suivant
			while(_xp >= World.getExpLevel(_lvl+1).metier && _lvl <100)
				levelUp(P,false);
			
			//s'il y a eu Up
			if(_lvl > exLvl && P.isOnline())
			{
				//on créer la listes des statsMetier a envoyer (Seulement celle ci)
				ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
				list.add(this);
				//on envoie le packet
				SocketManager.GAME_SEND_JS_PACKET(P, list);
				SocketManager.GAME_SEND_JN_PACKET(P,_template.getId(),_lvl);
				SocketManager.GAME_SEND_STATS_PACKET(P);
				SocketManager.GAME_SEND_Ow_PACKET(P);
				SocketManager.GAME_SEND_JO_PACKET(P, list);
			}
		}
		
		public String getXpString(String s)
		{
			String str = World.getExpLevel(_lvl).metier+s;
			str += _xp+s;
			str += World.getExpLevel((_lvl<100?_lvl+1:_lvl)).metier;
			return str;
		}
		public Metier getTemplate() {
			
			return _template;
		}

		public int getOptBinValue()
		{
			int nbr = 0;
			nbr += (_isCheap?1:0);
			nbr += (_freeOnFails?2:0);
			nbr += (_noRessource?4:0);
			return nbr;
		}
		
		public boolean isValidMapAction(int id)
		{
			for(JobAction JA : _posActions)if(JA.getSkillID() == id) return true;
			return false;
		}
		
		public void setOptBinValue(int bin)
		{
			_isCheap = false;
			_freeOnFails = false;
			_noRessource = false;
			
			if(bin - 4 >=0)
			{
				bin -= 4;
				_isCheap = true;
			}
			if(bin - 2 >=0)
			{
				bin -=2;
				_freeOnFails = true;
			}
			if(bin - 1 >= 0)
			{
				bin -= 1;
				_noRessource = true;
			}
		}

		public int getID()
		{
			return _id;
		}
	}
	
	public static class JobAction
	{
		private int _skID;
		private int _min = 1;
		private int _max = 1;
		private boolean _isCraft;
		private int _chan = 100;
		private int _time = 0;
		private int _xpWin = 0;
		private long _startTime;
		private Map<Integer,Integer> _ingredients = new TreeMap<Integer,Integer>();
		private Map<Integer,Integer> _lastCraft = new TreeMap<Integer,Integer>();
		private Timer _craftTimer;
		private Personnage _P;
		
		public JobAction(int sk,int min, int max,boolean craft, int arg,int xpWin)
		{
			_skID = sk;
			_min = min;
			_max = max;
			_isCraft = craft;
			if(craft)_chan = arg;
			else _time = arg;
			_xpWin = xpWin;
			
			_craftTimer = new Timer(100,new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					craft();
					_craftTimer.stop();
				}
			});
		}
		
		public void endAction(Personnage P, InteractiveObject IO, GameAction GA,Case cell)
		{
			if(!_isCraft)
			{
				//Si recue trop tot, on ignore
				if(_startTime - System.currentTimeMillis() > 500)return;
				IO.setState(Constants.IOBJECT_STATE_EMPTY);
				IO.startTimer();
				//Packet GDF (changement d'état de l'IO)
				SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(P.get_curCarte(), cell);
				
				boolean special = Formulas.getRandomValue(0, 99)==0;//Restriction de niveau ou pas ?
				
				//On ajoute X ressources
				int qua = (_max>_min?Formulas.getRandomValue(_min, _max):_min);
				int tID = Constants.getObjectByJobSkill(_skID,special);
								
				ObjTemplate T = World.getObjTemplate(tID);
				if(T == null)return;
				Objet O = T.createNewItem(qua, false);
				//Si retourne true, on l'ajoute au monde
				if(P.addObjet(O, true))
					World.addObjet(O, true);
				P.objetLog(O.getTemplate().getID(), O.getQuantity(), "Crafté");
				
				SocketManager.GAME_SEND_IQ_PACKET(P,P.get_GUID(),qua);
				SocketManager.GAME_SEND_Ow_PACKET(P);
			}
		}

		public void startAction(Personnage P, InteractiveObject IO, GameAction GA,Case cell)
		{
			_P = P;
			if(!_isCraft)
			{
				IO.setInteractive(false);
				IO.setState(Constants.IOBJECT_STATE_EMPTYING);
				SocketManager.GAME_SEND_GA_PACKET_TO_MAP(P.get_curCarte(),""+GA._id, 501, P.get_GUID()+"", cell.getID()+","+_time);
				SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(P.get_curCarte(),cell);
				_startTime = System.currentTimeMillis()+_time;//pour eviter le cheat
			}else
			{
				P.set_away(true);
				IO.setState(Constants.IOBJECT_STATE_EMPTYING);//FIXME trouver la bonne valeur
				P.setCurJobAction(this);
				SocketManager.GAME_SEND_ECK_PACKET(P, 3, _min+";"+_skID);//_min => Nbr de Case de l'interface
				SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(P.get_curCarte(), cell);
			}
		}

		public int getSkillID()
		{
			return _skID;
		}
		public int getMin()
		{
			return _min;
		}
		public int getXpWin()
		{
			return _xpWin;
		}
		public int getMax()
		{
			return _max;
		}
		public int getChance()
		{
			return _chan;
		}
		public int getTime()
		{
			return _time;
		}
		public boolean isCraft()
		{
			return _isCraft;
		}
		
		public void modifIngredient(Personnage P,int guid, int qua)
		{
			//on prend l'ancienne valeur
			int q = _ingredients.get(guid)==null?0:_ingredients.get(guid);
			//on enleve l'entrée dans la Map
			_ingredients.remove(guid);
			//on ajoute (ou retire, en fct du signe) X objet
			q += qua;
			if(q > 0)
			{
				_ingredients.put(guid,q);
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(P,'O', "+", guid+"|"+q);
			}else SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(P,'O', "-", guid+"");
		}

		public void craft()
		{
			if(!_isCraft)return;
			boolean signed = false;//TODO
			try
			{
				Thread.sleep(750);
			}catch(Exception e){};
			//Si Forgemagie
			if(_skID == 1
			|| _skID == 113
			|| _skID == 115
			|| _skID == 116
			|| _skID == 117
			|| _skID == 118
			|| _skID == 119
			|| _skID == 120
			|| (_skID >= 163 && _skID <= 169))
			{
				doFmCraft();
				return;
			}
			
			Map<Integer,Integer> items = new TreeMap<Integer,Integer>();
			//on retire les items mis en ingrédients
			for(Entry<Integer,Integer> e : _ingredients.entrySet())
			{
				//Si le joueur n'a pas l'objet
				if(!_P.hasItemGuid(e.getKey()))
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					GameServer.addToLog("/!\\ "+_P.get_name()+" essaye de crafter avec un objet qu'il n'a pas");
					return;
				}
				//Si l'objet n'existe pas
				Objet obj = World.getObjet(e.getKey());
				if(obj == null)
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					GameServer.addToLog("/!\\ "+_P.get_name()+" essaye de crafter avec un objet qui n'existe pas");
					return;
				}
				//Si la quantité est trop faible
				if(obj.getQuantity() < e.getValue())
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					GameServer.addToLog("/!\\ "+_P.get_name()+" essaye de crafter avec un objet dont la quantité est trop faible");
					return;
				}
				//On calcule la nouvelle quantité
				int newQua = obj.getQuantity() - e.getValue();
				
				if(newQua <0)return;//ne devrais pas arriver
				if(newQua == 0)
				{
					_P.removeItem(e.getKey());
					World.removeItem(e.getKey());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_P, e.getKey());
				}else
				{
					obj.setQuantity(newQua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, obj);
				}
				//on ajoute le couple tID/qua a la liste des ingrédients pour la recherche
				items.put(obj.getTemplate().getID(), e.getValue());
			}
			//On retire les items a ignorer pour la recette
			//Rune de signature
				if(items.containsKey(7508))signed = true;
				items.remove(7508);
			//Fin des items a retirer
			SocketManager.GAME_SEND_Ow_PACKET(_P);
			
			//On trouve le template corespondant si existant
			StatsMetier SM = _P.getMetierBySkill(_skID);
			int tID = World.getObjectByIngredientForJob(SM.getTemplate().getListBySkill(_skID),items);
			
			//Recette non existante ou pas adapté au métier
			if(tID == -1 || !SM.getTemplate().canCraft(_skID, tID))
			{
				SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-");
				_ingredients.clear();
				
				return;
			}
			
			int chan =  Constants.getChanceByNbrCaseByLvl(SM.get_lvl(),_ingredients.size());
			int jet = Formulas.getRandomValue(1, 100);
			boolean success = chan >= jet;
			
			if(!success)//Si echec
			{
				SocketManager.GAME_SEND_Ec_PACKET(_P,"EF");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-"+tID);
				SocketManager.GAME_SEND_Im_PACKET(_P, "0118");
			}else
			{
				Objet newObj = World.getObjTemplate(tID).createNewItem(1, false);
				//Si signé on ajoute la ligne de Stat "Fabriqué par:"
				if(signed)newObj.addTxtStat(988, _P.get_name());
				boolean add = true;
				int guid = newObj.getGuid();
				
				for(Entry<Integer,Objet> entry : _P.getItems().entrySet())
				{
					Objet obj = entry.getValue();
					if(obj.getTemplate().getID() == newObj.getTemplate().getID()
						&& obj.getStats().isSameStats(newObj.getStats())
						&& obj.getPosition() == Constants.ITEM_POS_NO_EQUIPED)//Si meme Template et Memes Stats et Objet non équipé
					{
						obj.setQuantity(obj.getQuantity()+newObj.getQuantity());//On ajoute QUA item a la quantité de l'objet existant
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P,obj);
						add = false;
						guid = obj.getGuid();
					}
				}
				if(add)
				{
					_P.getItems().put(newObj.getGuid(), newObj);
					SocketManager.GAME_SEND_OAKO_PACKET(_P,newObj);
					World.addObjet(newObj, true);
				}
				
				//on envoie les Packets
				SocketManager.GAME_SEND_Ow_PACKET(_P);
				SocketManager.GAME_SEND_Em_PACKET(_P,"KO+"+guid+"|1|"+tID+"|"+newObj.parseStatsString().replace(";","#"));
				SocketManager.GAME_SEND_Ec_PACKET(_P,"K;"+tID);
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"+"+tID);
			}
			
			
			//On donne l'xp
			int winXP =  Constants.calculXpWinCraft(SM.get_lvl(),_ingredients.size()) * Ancestra.XP_METIER;
			if(success)
			{
				SM.addXp(_P,winXP);
				ArrayList<StatsMetier> SMs = new ArrayList<StatsMetier>();
				SMs.add(SM);
				SocketManager.GAME_SEND_JX_PACKET(_P, SMs);
			}
			
			_lastCraft.clear();
			_lastCraft.putAll(_ingredients);
			_ingredients.clear();
			//*/
		}
		
		private void doFmCraft()
		{
			boolean signed = false;
			Objet obj = null,sign = null,mod = null;// sign = Rune de signature, mod: rune ou Potion, obj : objet modifé
			int isElementChanging = 0,stat = -1;
			for(int guid : _ingredients.keySet())
			{
				Objet ing = World.getObjet(guid);
				if(!_P.hasItemGuid(guid) || ing == null)
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-");
					_ingredients.clear();
					return;
				}
				int id =ing.getTemplate().getID();
				switch(id)
				{
				//Potions
				case 1333://Potion Etincelle
					stat = 99; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1335://Potion crachin
					stat = 96; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1337://Potion de courant d'air
					stat = 98; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1338://Potion de secousse
					stat = 97; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1340://Potion d'eboulement
					stat = 97; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1341://Potion Averse
					stat = 96; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1342://Potion de rafale
					stat = 98; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1343://Potion de Flambée
					stat = 99; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1345://Potion Incendie
					stat = 99; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1346://Potion Tsunami
					stat = 96; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1347://Potion Ouragan
					stat = 98; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				case 1348://Potion de seisme
					stat = 97; 
					isElementChanging = ing.getTemplate().getLevel();
					mod = ing;
				break;
				//Fin potions
				
				case 7508://Rune de signature
					signed = true;
					sign = ing;
				break;
				default://Si pas runes ou popo, et qu'il a un cout en PA, alors c'est une arme (une vérification du type serait préférable)
					if(ing.getTemplate().getPACost()>0)obj = ing;
				break;
				}
			}
			StatsMetier SM = _P.getMetierBySkill(_skID);
			
			if(SM == null || obj == null || mod == null)
			{
				SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-");
				_ingredients.clear();
				return;
			}
			int chan = 0;
			
			//* DEBUG
			System.out.println("ElmChg: "+isElementChanging);
			System.out.println("LevelM: "+SM.get_lvl());
			System.out.println("LevelA: "+obj.getTemplate().getLevel());
			///*/
			
			//Si changement d'élément
			if(isElementChanging > 0)chan = Formulas.calculElementChangeChance(SM.get_lvl(), obj.getTemplate().getLevel(), isElementChanging);
			//else TODO;
			
			//Min/max de 5% /95%
			if(chan > 100-(SM.get_lvl()/20))chan =100-(SM.get_lvl()/20);
			if(chan < (SM.get_lvl()/20))chan = (SM.get_lvl()/20);
			
			System.out.println("Chance: "+chan);
			
			int jet = Formulas.getRandomValue(1, 100);
			boolean success = chan >= jet;
			int tID = obj.getTemplate().getID();
			if(!success)//Si echec
			{
				//FIXME
				SocketManager.GAME_SEND_Em_PACKET(_P,"EO+"+_P.get_GUID()+"|1|"+tID+"|"+obj.parseStatsString().replace(";","#"));
				SocketManager.GAME_SEND_Ec_PACKET(_P,"EF");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-"+tID);
				SocketManager.GAME_SEND_Im_PACKET(_P, "0118");
			}else
			{
				int coef = 50;
				if(isElementChanging == 25)coef = 65;
				if(isElementChanging == 50)coef = 85;
				//Si signé on ajoute la ligne de Stat "Modifié par: "
				if(signed)obj.addTxtStat(985, _P.get_name());
				
				for(SpellEffect SE : obj.getEffects())
				{
					//Si pas un effet Dom Neutre, on continue
					if(SE.getEffectID() != 100)continue;
					String[] infos = SE.getArgs().split(";");
					try
					{
						//on calcule les nouvelles stats
						int min = Integer.parseInt(infos[0],16);
						int max = Integer.parseInt(infos[1],16);
						int newMin = (int)((min * coef) /100);
						int newMax = (int)((max * coef) /100);
						
						String newJet = "1d"+(newMax-newMin+1)+"+"+(newMin-1);
						String newArgs = Integer.toHexString(newMin)+";"+Integer.toHexString(newMax)+";-1;-1;0;"+newJet;
						
						SE.setArgs(newArgs);//on modifie les propriétés du SpellEffect
						SE.setEffectID(stat);//On change l'élement d'attaque
						
					}catch(Exception e){e.printStackTrace();};
				}
				//On envoie les packets
				SocketManager.GAME_SEND_Ow_PACKET(_P);
				SocketManager.GAME_SEND_Em_PACKET(_P,"KO+"+_P.get_GUID()+"|1|"+tID+"|"+obj.parseStatsString().replace(";","#"));
				SocketManager.GAME_SEND_Ec_PACKET(_P,"K;"+tID);
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"+"+tID);
			}
			//On consumme les runes
			//Rune de signature si diff de null
			if(sign != null)
			{
				int newQua = sign.getQuantity() -1;
				//S'il ne reste rien
				if(newQua <= 0)
				{
					_P.removeItem(sign.getGuid());
					World.removeItem(sign.getGuid());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_P, sign.getGuid());
				}else
				{
					sign.setQuantity(newQua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, sign);
				}
			}
			//Objet modificateur
			if(mod != null)
			{
				int newQua = mod.getQuantity() -1;
				//S'il ne reste rien
				if(newQua <= 0)
				{
					_P.removeItem(mod.getGuid());
					World.removeItem(mod.getGuid());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_P, mod.getGuid());
				}else
				{
					mod.setQuantity(newQua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, mod);
				}
			}
			//fin
			
			//On sauve le dernier craft
			_lastCraft.clear();
			_lastCraft.putAll(_ingredients);
			_ingredients.clear();
		}

		public void repeat(int time,Personnage P)
		{
			_craftTimer.stop();
			// /!\ Time = Nombre Réel -1
			_lastCraft.clear();
			_lastCraft.putAll(_ingredients);
			for(int a = time; a>=0;a--)
			{
				SocketManager.GAME_SEND_EA_PACKET(P,a+"");
				_ingredients.clear();
				_ingredients.putAll(_lastCraft);
				craft();
			}
			SocketManager.GAME_SEND_Ea_PACKET(P, "1");
		}

		public void startCraft(Personnage P)
		{
			//on retarde le lancement du craft en cas de packet EMR (craft auto)
			_craftTimer.start();
		}

		public void putLastCraftIngredients()
		{
			if(_P == null)return;
			if(_lastCraft == null)return;
			if(_ingredients.size() != 0)return;//OffiLike, mais possible de faire un truc plus propre en enlevant les objets présent et en rajoutant ceux de la recette
			_ingredients.clear();
			_ingredients.putAll(_lastCraft);
			for(Entry<Integer,Integer> e : _ingredients.entrySet())
			{
				if(World.getObjet(e.getKey()) == null)return;
				if(World.getObjet(e.getKey()).getQuantity() < e.getValue())return;
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(_P,'O', "+", e.getKey()+"|"+e.getValue());
			}
		}

		public void resetCraft()
		{
			_ingredients.clear();
			_lastCraft.clear();
		}
	}
	//Classe Metier
	private int _id;
	private ArrayList<Integer> _tools = new ArrayList<Integer>();
	private Map<Integer,ArrayList<Integer>> _crafts = new TreeMap<Integer,ArrayList<Integer>>();
	
	public Metier(int id,String tools,String crafts)
	{
		_id= id;
		if(!tools.equals(""))
		{
			for(String str : tools.split(","))
			{
				try
				{
					int tool = Integer.parseInt(str);
					_tools.add(tool);
				}catch(Exception e){continue;};
			}
		}
		
		if(!crafts.equals(""))
		{
			for(String str : crafts.split("\\|"))
			{
				try
				{
					int skID = Integer.parseInt(str.split(";")[0]);
					ArrayList<Integer> list = new ArrayList<Integer>();
					for(String str2 : str.split(";")[1].split(","))list.add(Integer.parseInt(str2));
					_crafts.put(skID, list);
				}catch(Exception e){continue;};
			}
		}
	}
	public ArrayList<Integer> getListBySkill(int skID)
	{
		return _crafts.get(skID);
	}
	public boolean canCraft(int skill,int template)
	{
		if(_crafts.get(skill) != null)for(int a : _crafts.get(skill))if(a == template)return true;
		return false;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public boolean isValidTool(int t)
	{
		for(int a : _tools)if(t == a)return true;
		return false;
	}
	
}
