package org.thalm.game.character.mount;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.thalm.common.Variable;
import org.thalm.database.Database;
import org.thalm.game.World;
import org.thalm.game.character.Player.Stats;
import org.thalm.game.character.item.Item;

public class Mount {

	private int _id;
	private int _color;
	private int _sexe;
	private int _amour;
	private int _endurance;
	private int _level;
	private long _exp;
	private String _nom;
	private int _fatigue;
	private int _energie;
	private int _reprod;
	private int _maturite;
	private int _serenite;
	private Stats _stats = new Stats();
	private String _ancetres = ",,,,,,,,,,,,,";
	private ArrayList<Item> _items = new ArrayList<Item>();
	//TODO: CapacitÚs
	
	public Mount(int color)
	{
		_id = World.getNextIdForMount();
		_color = color;
		_level = 1;
		_exp = 0;
		_nom = "SansNom";
		_fatigue = 0;
		_energie = getMaxEnergie();
		_reprod = 0;
		_maturite = getMaxMatu();
		_serenite = 0;
		_stats = Variable.getMountStats(_color,_level);
		_ancetres = ",,,,,,,,,,,,,";
		
		World.addDragodinde(this);
		Database.CREATE_MOUNT(this);
	}
	
	public Mount(int id, int color, int sexe, int amour, int endurance,
			int level, long exp, String nom, int fatigue,
			int energie, int reprod, int maturite, int serenite,String items,String anc)
	{
		_id = id;
		_color = color;
		_sexe = sexe;
		_amour = amour;
		_endurance = endurance;
		_level = level;
		_exp = exp;
		_nom = nom;
		_fatigue = fatigue;
		_energie = energie;
		_reprod = reprod;
		_maturite = maturite;
		_serenite = serenite;
		_ancetres = anc;
		_stats = Variable.getMountStats(_color,_level);
		for(String str : items.split(";"))
		{
			try
			{
				Item obj = World.getObjet(Integer.parseInt(str));
				if(obj != null)_items.add(obj);
			}catch(Exception e){continue;}
		}
	}

	public int get_id() {
		return _id;
	}

	public int get_color() {
		return _color;
	}

	public int get_sexe() {
		return _sexe;
	}

	public int get_amour() {
		return _amour;
	}

	public String get_ancetres() {
		return _ancetres;
	}

	public int get_endurance() {
		return _endurance;
	}
	public int get_level() {
		return _level;
	}

	public long get_exp() {
		return _exp;
	}

	public String get_nom() {
		return _nom;
	}

	public int get_fatigue() {
		return _fatigue;
	}

	public int get_energie() {
		return _energie;
	}

	public int get_reprod() {
		return _reprod;
	}

	public int get_maturite() {
		return _maturite;
	}

	public int get_serenite() {
		return _serenite;
	}

	public Stats get_stats() {
		return _stats;
	}

	public ArrayList<Item> get_items() {
		return _items;
	}
	
	public String parse()
	{
		String str = _id+":";
		str += _color+":";
		str += _ancetres+":";
		str += ","+":";//FIXME capacitÚs
		str += _nom+":";
		str += _sexe+":";
		str += parseXpString()+":";
		str += _level+":";
		str += "1"+":";//FIXME
		str += getTotalPod()+":";
		str += "0"+":";//FIXME podActuel?
		str += _endurance+",10000:";
		str += _maturite+","+getMaxMatu()+":";
		str += _energie+","+getMaxEnergie()+":";
		str += _serenite+",-10000,10000:";
		str += _amour+",10000:";
		str += "-1"+":";//FIXME
		str += "0"+":";//FIXME
		str += parseStats()+":";
		str += _fatigue+",240:";
		str += _reprod+",20:";
		return str;
	}

	private String parseStats()
	{
		String stats = "";
		for(Entry<Integer,Integer> entry : _stats.getMap().entrySet())
		{
			if(entry.getValue() <= 0)continue;
			if(stats.length() >0)stats += ",";
			stats += Integer.toHexString(entry.getKey())+"#"+Integer.toHexString(entry.getValue())+"#0#0";
		}
		return stats;
	}

	private int getMaxEnergie()
	{
		int energie = 1000;
		return energie;
	}

	private int getMaxMatu()
	{
		int matu = 1000;
		return matu;
	}

	private int getTotalPod()
	{
		int pod = 1000;
		
		return pod;
	}

	private String parseXpString()
	{
		return _exp+","+World.getExpLevel(_level).dinde+","+World.getExpLevel(_level+1).dinde;
	}

	public boolean isMountable()
	{
		if(_energie <10
		|| _maturite < getMaxMatu()
		|| _fatigue == 240)return false;
		return true;
	}

	public String getItemsId()
	{
		String str = "";
		for(Item obj : _items)str += (str.length()>0?";":"")+obj.getGuid();
		return str;
	}

	public void setName(String packet)
	{
		_nom = packet;
		Database.UPDATE_MOUNT_INFOS(this);
	}
	
	public void addXp(long amount)
	{
		_exp += amount;

		while(_exp >= World.getExpLevel(_level+1).dinde && _level<100)
			levelUp();
		
	}
	
	public void levelUp()
	{
		_level++;
		_stats = Variable.getMountStats(_color,_level);
	}
}
