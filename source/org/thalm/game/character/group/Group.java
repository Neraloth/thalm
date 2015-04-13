package org.thalm.game.character.group;

import java.util.ArrayList;

import org.thalm.game.character.Player;
import org.thalm.network.Packets;

public class Group
{
	private ArrayList<Player> _persos = new ArrayList<Player>();
	private Player _chief;
	
	public Group(Player p1,Player p2)
	{
		_chief = p1;
		_persos.add(p1);
		_persos.add(p2);
	}
	
	public boolean isChief(int guid)
	{
		return _chief.get_GUID() == guid;
	}
	
	public void addPerso(Player p)
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
		for(Player p : _persos)
		{
			lvls += p.get_lvl();
		}
		return lvls;
	}
	
	public ArrayList<Player> getPersos()
	{
		return _persos;
	}

	public Player getChief()
	{
		return _chief;
	}

	public void leave(Player p)
	{
		if(!_persos.contains(p))return;
		p.setGroup(null);
		_persos.remove(p);
		if(_persos.size() == 1)
		{
			_persos.get(0).setGroup(null);
			if(_persos.get(0).get_compte() == null)return;
			Packets.GAME_SEND_PV_PACKET(_persos.get(0).get_compte().getGameThread().get_out(),"");
		}
		else
			Packets.GAME_SEND_PM_DEL_PACKET_TO_GROUP(this,p.get_GUID());
	}
}