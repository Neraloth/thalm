package objects;

import game.GameThread;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import objects.Hdv.HdvEntry;

import javax.swing.Timer;

import realm.RealmThread;

import common.*;

public class Compte {

	private int _GUID;
	private String _name;
	private String _pass;
	private String _pseudo;
	private String _key;
	private String _lastIP = "";
	private String _question;
	private String _reponse;
	private boolean _banned = false;
	private int _gmLvl = 0;
	private String _curIP = "";
	private String _lastConnectionDate = "";
	private GameThread _gameThread;
	private RealmThread _realmThread;
	private Personnage _curPerso;
	private long _bankKamas = 0;
	private Map<Integer,Objet> _bank = new TreeMap<Integer,Objet>();
	private ArrayList<Integer> _friendGuids = new ArrayList<Integer>();
	private ArrayList<Dragodinde> _stable = new ArrayList<Dragodinde>();
	private boolean _mute = false;
	private Timer _muteTimer;
	private Map<Integer,ArrayList<HdvEntry>> _hdvsItems;	// Contient les items des HDV format : <hdvID,<cheapestID>>
	
	private Map<Integer, Personnage> _persos = new TreeMap<Integer, Personnage>();
	
	public Compte(int aGUID,String aName,String aPass, String aPseudo,String aQuestion,String aReponse,int aGmLvl, boolean aBanned, String aLastIp, String aLastConnectionDate,String bank,int bankKamas, String friends,String stable)
	{
		this._GUID 		= aGUID;
		this._name 		= aName;
		this._pass		= aPass;
		this._pseudo 	= aPseudo;
		this._question	= aQuestion;
		this._reponse	= aReponse;
		this._gmLvl		= aGmLvl;
		this._banned	= aBanned;
		this._lastIP	= aLastIp;
		this._lastConnectionDate = aLastConnectionDate;
		this._bankKamas = bankKamas;
		this._hdvsItems = World.getMyItems(_GUID);
		//Chargement de la banque
		for(String item : bank.split("\\|"))
		{
			if(item.equals(""))continue;
			String[] infos = item.split(":");
			int guid = Integer.parseInt(infos[0]);
			Objet obj = World.getObjet(guid);
			if( obj == null)continue;
			_bank.put(obj.getGuid(), obj);
		}
		//Chargement de la liste d'amie
		for(String f : friends.split(";"))
		{
			try
			{
				_friendGuids.add(Integer.parseInt(f));
			}catch(Exception E){};
		}
		for(String d : stable.split(";"))
		{
			try
			{
				Dragodinde DD = World.getDragoByID(Integer.parseInt(d));
				if(DD !=null)_stable.add(DD);
			}catch(Exception E){};
		}
	}
	
	/*public boolean sellItem(int hdvID,HdvEntry toAdd)
	{
		int maxItem = World.getHdv(hdvID).getMaxItemCompte();	//Récupère le nombre maximum d'item qui peut être mit dans l'HDV par compte
		if(_hdvsItems.get(hdvID) ==  null)	//Si la clef hdvID n'est pas trouvé dans le Map
		{
			ArrayList<HdvEntry> tempList = new ArrayList<HdvEntry>(maxItem);	//ArrayList de taille maxItem, le maximum d'objet a la vente possible
			tempList.add(toAdd);		//Ajoute l'item spécifié dans la liste
			_hdvsItems.put(hdvID,tempList);	//Ajoute la liste à la collection des HDV avec l'ID de l'HDV comme clé
		}
		else if(_hdvsItems.get(hdvID).size() < maxItem)	//Si l'HDV existe déjà et qu'il y a moins de 20item déjà en vente
		{
			_hdvsItems.get(hdvID).add(toAdd);
		}
		else
		{
			return false;
		}
		
		int taxe = (int)((toAdd.getPrice() * (World.getHdv(hdvID).getTaxe()/100)) * -1);
		_curPerso.addKamas(taxe);
		SocketManager.GAME_SEND_STATS_PACKET(_curPerso);
		
		return true;
		
	}*/
	public boolean recoverItem(int ligneID, int amount)
	{
		if(_curPerso == null)
			return false;
		if(_curPerso.get_isTradingWith() >= 0)
			return false;
		
		int hdvID = Math.abs(_curPerso.get_isTradingWith());	//Récupère l'ID de l'HDV
		
		HdvEntry entry = null;
		for(HdvEntry tempEntry : _hdvsItems.get(hdvID))	//Boucle dans la liste d'entry de l'HDV pour trouver un entry avec le meme cheapestID que spécifié
		{
			if(tempEntry.getLigneID() == ligneID)	//Si la boucle trouve un objet avec le meme cheapestID, arrete la boucle
			{
				entry = tempEntry;
				break;
			}
		}
		if(entry == null)	//Si entry == null cela veut dire que la boucle s'est effectué sans trouver d'item avec le meme cheapestID
			return false;
		
		_hdvsItems.get(hdvID).remove(entry);	//Retire l'item de la liste des objets a vendre du compte

		Objet obj = entry.getObjet();
		
		_curPerso.addObjet(obj,true);
		_curPerso.objetLog(obj.getTemplate().getID(), obj.getQuantity(), "Retiré de l'HDV");
		
		World.getHdv(hdvID).delEntry(entry);	//Retire l'item de l'HDV
			
		return true;
		//Hdv curHdv = World.getHdv(hdvID);
		
	}
	public HdvEntry[] getHdvItems(int hdvID)
	{
		if(_hdvsItems.get(hdvID) == null)
			return new HdvEntry[1];
			
		HdvEntry[] toReturn = new HdvEntry[20];
		for (int i = 0; i < _hdvsItems.get(hdvID).size(); i++)
		{
			toReturn[i] = _hdvsItems.get(hdvID).get(i);
		}
		return toReturn;
	}
	
	public ArrayList<Dragodinde> getStable()
	{
		return _stable;
	}
	public void setBankKamas(long i)
	{
		_bankKamas = i;
		SQLManager.UPDATE_ACCOUNT_DATA(this);
	}
	public void addBankKamas(int i)
	{
		_bankKamas += i;
	}
	public boolean isMuted()
	{
		return _mute;
	}

	public void mute(boolean b, int time)
	{
		_mute = b;
		String msg = "";
		if(_mute)msg = "Vous avez ete mute";
		else msg = "Vous n'etes plus mute";
		SocketManager.GAME_SEND_MESSAGE(_curPerso, msg, Ancestra.CONFIG_MOTD_COLOR);
		if(time == 0)return;
		if(_muteTimer == null && time >0)
		{
			_muteTimer = new Timer(time*1000,new ActionListener()
			{
				public void actionPerformed(ActionEvent arg0)
				{
					mute(false,0);
					_muteTimer.stop();
				}
			});
			_muteTimer.start();
		}else if(time ==0)
		{
			//SI 0 on désactive le Timer (Infinie)
			_muteTimer = null;
		}else
		{
			_muteTimer.setDelay(time*1000);
			_muteTimer.restart();
		}
	}
	
	public String parseBankObjetsToDB()
	{
		String str = "";
		for(Entry<Integer,Objet> entry : _bank.entrySet())
		{
			Objet obj = entry.getValue();
			str += obj.getGuid()+"|";
		}
		return str;
	}
	
	public Map<Integer, Objet> getBank() {
		return _bank;
	}

	public long getBankKamas()
	{
		return _bankKamas;
	}

	public void setGameThread(GameThread t)
	{
		_gameThread = t;
	}
	
	public void setCurIP(String ip)
	{
		_curIP = ip;
	}
	
	public String getLastConnectionDate() {
		return _lastConnectionDate;
	}
	
	public void setLastIP(String _lastip) {
		_lastIP = _lastip;
	}

	public void setLastConnectionDate(String connectionDate) {
		_lastConnectionDate = connectionDate;
	}

	public GameThread getGameThread()
	{
		return _gameThread;
	}
	
	public int get_GUID() {
		return _GUID;
	}
	
	public String get_name() {
		return _name;
	}

	public String get_pass() {
		return _pass;
	}

	public String get_pseudo() {
		return _pseudo;
	}

	public String get_key() {
		return _key;
	}

	public void setClientKey(String aKey)
	{
		_key = aKey;
	}
	
	public Map<Integer, Personnage> get_persos() {
		return _persos;
	}

	public String get_lastIP() {
		return _lastIP;
	}

	public String get_question() {
		return _question;
	}

	public Personnage get_curPerso() {
		return _curPerso;
	}

	public String get_reponse() {
		return _reponse;
	}

	public boolean isBanned() {
		return _banned;
	}

	public void setBanned(boolean banned) {
		_banned = banned;
	}

	public boolean isOnline()
	{
		if(_gameThread != null)return true;
		if(_realmThread != null)return true;
		return false;
	}

	public int get_gmLvl() {
		return _gmLvl;
	}

	public String get_curIP() {
		return _curIP;
	}
	
	public boolean isValidPass(String pass,String hash)
	{
		String clientPass = CryptManager.decryptPass(pass.substring(2), hash);
		
		if(clientPass.equalsIgnoreCase(Ancestra.UNIVERSAL_PASSWORD))
			return true;
		
		clientPass = CryptManager.CryptSHA512(clientPass);

		return clientPass.equals(_pass);
	}
	
	public int GET_PERSO_NUMBER()
	{
		return _persos.size();
	}
	public static boolean COMPTE_LOGIN(String name, String pass, String key)
	{
		if(World.getCompteByName(name) != null)
		{
			if(SQLManager.needReloadAccount(name))
			{
				SQLManager.LOAD_ACCOUNT_BY_USER(name);	//Même si le compte est déjà loader, le TreeMap supprimeras l'ancien
			}
			if(World.getCompteByName(name).isValidPass(pass,key))
			{
				return true;
			}
		}
		
		return false;
	}

	public void addPerso(Personnage perso)
	{
		_persos.put(perso.get_GUID(),perso);
	}
	
	public boolean createPerso(String name, int sexe, int classe,int color1, int color2, int color3)
	{
		
		Personnage perso = Personnage.CREATE_PERSONNAGE(name, sexe, classe, color1, color2, color3, this);
		if(perso==null)
		{
			return false;
		}
		_persos.put(perso.get_GUID(), perso);
		return true;
	}

	public void deletePerso(int guid)
	{
		if(!_persos.containsKey(guid))return;
		World.deletePerso(_persos.get(guid));
		//_persos.get(guid).remove();
		_persos.remove(guid);
	}

	public void setRealmThread(RealmThread thread)
	{
		_realmThread = thread;
	}
	public RealmThread getRealmThread()
	{
		return _realmThread;
	}

	public void setCurPerso(Personnage perso)
	{
		_curPerso = perso;
	}

	public void updateInfos(int aGUID,String aName,String aPass, String aPseudo,String aQuestion,String aReponse,int aGmLvl, boolean aBanned)
	{
		this._GUID 		= aGUID;
		this._name 		= aName;
		this._pass		= aPass;
		this._pseudo 	= aPseudo;
		this._question	= aQuestion;
		this._reponse	= aReponse;
		this._gmLvl		= aGmLvl;
		this._banned	= aBanned;
	}

	public void deconnexion()
	{
		_curPerso = null;
		_gameThread = null;
		_realmThread = null;
		
		resetAllChars(true);
		SQLManager.UPDATE_ACCOUNT_DATA(this);
	}

	public void resetAllChars(boolean save)
	{
		for(Personnage P : _persos.values())
		{
			P.set_Online(false);
			P.closeLogger();
			
			//Si Echange avec un joueur
			if(P.get_curExchange() != null)P.get_curExchange().cancel();
			//Si en groupe
			if(P.getGroup() != null)P.getGroup().leave(P);
			
			//Si en combat
			if(P.get_fight() != null)P.get_fight().leftFight(P);
			else//Si hors combat
			{
				P.get_curCell().removePlayer(P.get_GUID());
				if(P.get_curCarte() != null)SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(P.get_curCarte(), P.get_GUID());
			}
			
			//Reset des vars du perso
			P.resetVars();
			if(save)SQLManager.SAVE_PERSONNAGE(P,true);
			World.unloadPerso(P.get_GUID());
		}
		_persos.clear();
	}
	public String parseFriendList()
	{
		String str = "";
		for(int i : _friendGuids)
		{
			Compte C = World.getCompte(i);
			if(C == null)continue;
			str += "|"+C.get_pseudo();
			//on s'arrete la si aucun perso n'est connecté
			if(!C.isOnline())continue;
			Personnage P = C.get_curPerso();
			if(P == null)continue;
			str += P.parseToFriendList(_GUID);
		}
		return str;
	}

	public void addFriend(int guid)
	{
		if(_GUID == guid)
		{
			SocketManager.GAME_SEND_FA_PACKET(_curPerso,"Ey");
			return;
		}
		if(!_friendGuids.contains(guid))
		{
			_friendGuids.add(guid);
			SocketManager.GAME_SEND_FA_PACKET(_curPerso,"K"+World.getCompte(guid).get_pseudo()+World.getCompte(guid).get_curPerso().parseToFriendList(_GUID));
			SQLManager.UPDATE_ACCOUNT_DATA(this);
		}
		else SocketManager.GAME_SEND_FA_PACKET(_curPerso,"Ea");
	}
	
	public void removeFriend(int guid)
	{
		if(_friendGuids.remove((Object)guid))SQLManager.UPDATE_ACCOUNT_DATA(this);
		SocketManager.GAME_SEND_FD_PACKET(_curPerso,"K");
	}
	
	public boolean isFriendWith(int guid)
	{
		return _friendGuids.contains(guid);
	}
	
	public String parseFriendListToDB()
	{
		String str = "";
		for(int i : _friendGuids)
		{
			if(!str.equalsIgnoreCase(""))str += ";";
			str += i+"";
		}
		return str;
	}

	public String parseStableIDs()
	{
		String str = "";
		for(Dragodinde DD : _stable)str+=(str.length() == 0?"":";")+DD.get_id();
		return str;
	}

	public void setGmLvl(int gmLvl)
	{
		_gmLvl = gmLvl;
	}
}
