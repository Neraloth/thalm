package common;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import common.World.Couple;

import objects.Guild.GuildMember;
import objects.*;
import objects.Fight.*;

public class Formulas {


	public static int getRandomValue(int i1,int i2)
	{
		Random rand = new Random();
		return (rand.nextInt((i2-i1)+1))+i1;
	}
	
	/*MARTHIEUBEAN*/
	public static int getRandomOrientation()
	{
		return (Formulas.getRandomValue(0, 3)*2)+1;
	}
	/*FIN*/
	
	public static int getRandomJet(String jet)//1d5+6
	{
		try
		{
			if(!jet.contains("d"))
				return Integer.parseInt(jet,16);
			
			int num = 0;
			int des = Integer.parseInt(jet.split("d")[0]);
			int faces = Integer.parseInt(jet.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(jet.split("d")[1].split("\\+")[1]);
			for(int a=0;a<des;a++)
			{
				num += getRandomValue(1,faces);
			}
			num += add;
			return num;
		}catch(NumberFormatException e){return -1;}
	}
	public static int getMiddleJet(String jet)//1d5+6
	{
		try
		{
			int num = 0;
			int des = Integer.parseInt(jet.split("d")[0]);
			int faces = Integer.parseInt(jet.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(jet.split("d")[1].split("\\+")[1]);
			num += ((1+faces)/2)*des;//on calcule moyenne
			num += add;
			return num;
		}catch(NumberFormatException e){return 0;}
	}
	public static int getTacleChance(Fighter tacleur, Fighter tacle)
	{
		int agiTR = tacleur.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
		int agiT = tacle.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
		int a = agiTR+25;
		int b = agiTR+agiT+50;
		int chance = (int)((long)(300*a/b)-100);
		if(chance <10)chance = 10;
		if(chance >90)chance = 90;
		return chance;
	}
	public static long getXpWinPvm3(Fighter perso, ArrayList<Fighter> winners,ArrayList<Fighter> loosers,long groupXP)
	{
		if(perso.getPersonnage()== null)return 0;
		if(winners.contains(perso))//Si winner
		{
			float sag = perso.getTotalStats().getEffect(Constants.STATS_ADD_SAGE);
			float coef = (sag + 100)/100;
			int taux = Ancestra.XP_PVM;
			long xpWin = 0;
			int lvlmax = 0;
			
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > lvlmax)
					lvlmax = entry.get_lvl();
			}
			int nbbonus = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > (lvlmax / 3))
					nbbonus += 1;				
			}
			
			int lvlLoosersmax = 0;
			for(Fighter entry : loosers)
			{
				if(entry.get_lvl() > lvlLoosersmax)
				lvlLoosersmax = entry.get_lvl();
			}
			
			int lvlLoosers = 0;
			for(Fighter entry : loosers)
				lvlLoosers += entry.get_lvl();
				
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			
			int lvl = perso.get_lvl();
			double bonusgroupe = ((double)lvl / (double)lvlmax);		
			
			double modif1 = 1;
			
			if (lvlLoosers + 5 > lvlWinners && lvlWinners > lvlLoosers - 10)
			{
				modif1 = 1;
				System.out.println(lvlLoosers+"+5 > "+lvlWinners+" > "+lvlLoosers+" +10");
				System.out.println("Modif 1 = "+modif1);
			}	
			if (lvlWinners < lvlLoosers - 10)
			{
				modif1 = (((double)lvlWinners + 10) / (double)lvlLoosers);
				System.out.println(lvlWinners+" < "+lvlLoosers+"-10");
				System.out.println("Modif 1 = "+modif1);
			}	
			if (lvlLoosers + 5 < lvlWinners)
			{
				modif1 = (double)lvlLoosers / (double)lvlWinners;
				System.out.println(lvlLoosers+"+5 < "+lvlWinners);
				System.out.println("Modif 1 = "+modif1);
			}
			/*
			if (lvlLoosers > lvlWinners + 10)
			{
				modif1 = ((double)lvlWinners + 10)/ (double)lvlLoosers;
				System.out.println("Modif 1 = "+modif1);
			}
			
			if (lvlWinners > lvlLoosers + 5)
			{
				modif1 = (double)lvlLoosers / (double)lvlWinners;
				System.out.println("Modif 1 = "+modif1);
			}*/
			
			double modif2 = 0;
			if ((lvlLoosersmax * 2.5) > lvlWinners)
			{
				modif2 = 1;
			}
			else
			{
				modif2 = Math.floor((2.5 * (int)lvlLoosersmax)) / (int)lvlWinners;
			}
			
			double bonus = 1;
			if(nbbonus == 2)
				bonus = 1.1;
			if(nbbonus == 3)
				bonus = 1.5;
			if(nbbonus == 4)
				bonus = 2.3;
			if(nbbonus == 5)
				bonus = 3.1;
			if(nbbonus == 6)
				bonus = 3.6;
			if(nbbonus == 7)
				bonus = 4.2;
			if(nbbonus >= 8)
				bonus = 4.7;
				
			
			xpWin = (long) (groupXP * coef * bonusgroupe * modif1 * modif2 * taux * bonus);
			
			/*/ DEBUG XP
			System.out.println("=========");
			System.out.println("groupXP: "+groupXP);
			System.out.println("coef: "+coef);
			System.out.println("bonusgroupe: "+bonusgroupe);
			System.out.println("modif 1: "+modif1);
			System.out.println("modif 2: "+modif2);
			System.out.println("taux: "+taux);
			System.out.println("bonus: "+bonus);
			System.out.println("xpWin: "+xpWin);
			System.out.println("=========");
			//*/
			return xpWin;
			
		}
		return 0;
	}
	

	public static int calculFinalHeal(Personnage caster,int jet)
	{
		int statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
		int soins = caster.getTotalStats().getEffect(Constants.STATS_ADD_SOIN);
		if(statC<0)statC=0;
		return (int)(jet * (100 + statC) / 100 + soins);
	}
	
	public static int calculFinalDommage(Fight fight,Fighter caster,Fighter target,int statID,int jet,boolean isHeal)
	{
		float num = 0;
		float statC = 0, domC = 0, perdomC = 0, resfT = 0, respT = 0;
		domC = caster.getTotalStats().getEffect(Constants.STATS_ADD_DOMA);
		perdomC = caster.getTotalStats().getEffect(Constants.STATS_ADD_PERDOM);
		int multiplier = caster.getTotalStats().getEffect(Constants.STATS_MULTIPLY_DOMMAGE);
		switch(statID)
		{
			case Constants.ELEMENT_NULL://Fixe
				statC = 0;
				resfT = 0;
				respT = 0;
				respT = 0;
			break;
			case Constants.ELEMENT_NEUTRE://neutre
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_NEU);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_NEU);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_NEU);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_NEU);
				}
				//on ajoute les dom Physique
				domC += caster.getTotalStats().getEffect(142);
				//Ajout de la resist Physique
				resfT = target.getTotalStats().getEffect(184);
			break;
			case Constants.ELEMENT_TERRE://force
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_TER);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_TER);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_TER);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_TER);
				}
				//on ajout les dom Physique
				domC += caster.getTotalStats().getEffect(142);
				//Ajout de la resist Physique
				resfT = target.getTotalStats().getEffect(184);
			break;
			case Constants.ELEMENT_EAU://chance
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_EAU);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_EAU);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_EAU);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_EAU);
				}
				//Ajout de la resist Magique
				resfT = target.getTotalStats().getEffect(183);
			break;
			case Constants.ELEMENT_FEU://intell
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_FEU);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_FEU);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_FEU);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_FEU);
				}
				//Ajout de la resist Magique
				resfT = target.getTotalStats().getEffect(183);
			break;
			case Constants.ELEMENT_AIR://agilité
				statC = caster.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				resfT = target.getTotalStats().getEffect(Constants.STATS_ADD_R_AIR);
				respT = target.getTotalStats().getEffect(Constants.STATS_ADD_RP_AIR);
				if(caster.getPersonnage() != null)//Si c'est un joueur
				{
					respT += target.getTotalStats().getEffect(Constants.STATS_ADD_RP_PVP_AIR);
					resfT += target.getTotalStats().getEffect(Constants.STATS_ADD_R_PVP_AIR);
				}
				//Ajout de la resist Magique
				resfT = target.getTotalStats().getEffect(183);
			break;
		}
		//On bride la resistance a 50% si c'est un joueur 
		if(target.getMob() == null && respT >50)respT = 50;
		
		if(statC<0)statC=0;
		/* DEBUG
		System.out.println("Jet: "+jet+" Stats: "+statC+" perdomC: "+perdomC+" multiplier: "+multiplier);
		System.out.println("(100 + statC + perdomC)= "+(100 + statC + perdomC));
		System.out.println("(jet * (100 + statC + perdomC + (multiplier*100) ) / 100)= "+(jet * ((100 + statC + perdomC) / 100 )));
		System.out.println("res Fix. T "+ resfT);
		System.out.println("res %age T "+respT);
		if(target.getMob() != null)
		{
			System.out.println("resmonstre: "+target.getMob().getStats().getEffect(Constants.STATS_ADD_RP_FEU));
			System.out.println("TotalStat: "+target.getTotalStats().getEffect(Constants.STATS_ADD_RP_FEU));
			System.out.println("FightStat: "+target.getTotalStatsLessBuff().getEffect(Constants.STATS_ADD_RP_FEU));
			
		}
		//*/
		num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;//dégats bruts
		//Renvoie
		int renvoie = target.getTotalStatsLessBuff().getEffect(Constants.STATS_RETDOM);
		if(renvoie >0 && !isHeal)
		{
			if(renvoie > num)renvoie = (int)num;
			num -= renvoie;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.getGUID()+","+renvoie);
			if(renvoie>caster.getPDV())renvoie = caster.getPDV();
			if(num<1)num =0;
			caster.removePDV(renvoie);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+",-"+renvoie);
		}
		if(!isHeal)num -= resfT;//resis fixe
		int armor= getArmorResist(target,statID);
		if(!isHeal)num -= armor;
		if(!isHeal)if(armor > 0)SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+armor);
		int reduc =	(int)((num/(float)100)*respT);//Reduc %resis
		if(!isHeal)num -= reduc;
		//dégats finaux
		if(num < 1)num=0;
		return (int)num;
	}
	public static int calculZaapCost(Carte map1,Carte map2)
	{
		return (int) (10*(Math.abs(map2.getX()-map1.getX())+Math.abs(map2.getY()-map1.getY())-1));
	}
	private static int getArmorResist(Fighter target, int statID)
	{
		int armor = 0;
		for(SpellEffect SE : target.getBuffsByEffectID(265))
		{
			Fighter fighter;
			
			switch(SE.getSpell())
			{
				case 1://Armure incandescente
					//Si pas element feu, on ignore l'armure
					if(statID != Constants.ELEMENT_FEU)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				case 6://Armure Terrestre
					//Si pas element terre/neutre, on ignore l'armure
					if(statID != Constants.ELEMENT_TERRE && statID != Constants.ELEMENT_NEUTRE)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				case 14://Armure Venteuse
					//Si pas element air, on ignore l'armure
					if(statID != Constants.ELEMENT_AIR)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				case 18://Armure aqueuse
					//Si pas element eau, on ignore l'armure
					if(statID != Constants.ELEMENT_EAU)continue;
					//Les stats du féca sont prises en compte
					fighter = SE.getCaster();
				break;
				
				default://Dans les autres cas on prend les stats de la cible et on ignore l'element de l'attaque
					fighter = target;
				break;
			}
			int intell = fighter.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
			int carac = 0;
			switch(statID)
			{
				case Constants.ELEMENT_AIR:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				break;
				case Constants.ELEMENT_FEU:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
				break;
				case Constants.ELEMENT_EAU:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
				break;
				case Constants.ELEMENT_NEUTRE:
				case Constants.ELEMENT_TERRE:
					carac = fighter.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				break;
			}
			int value = SE.getValue();
			int a = value * (100 + (int)(intell/2) + (int)(carac/2))/100;
			armor += a;
		}
		for(SpellEffect SE : target.getBuffsByEffectID(105))
		{
			int intell = target.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
			int carac = 0;
			switch(statID)
			{
				case Constants.ELEMENT_AIR:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_AGIL);
				break;
				case Constants.ELEMENT_FEU:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_INTE);
				break;
				case Constants.ELEMENT_EAU:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_CHAN);
				break;
				case Constants.ELEMENT_NEUTRE:
				case Constants.ELEMENT_TERRE:
					carac = target.getTotalStats().getEffect(Constants.STATS_ADD_FORC);
				break;
			}
			int value = SE.getValue();
			int a = value * (100 + (int)(intell/2) + (int)(carac/2))/100;
			armor += a;
		}
		return armor;
	}

	public static int getPointsLost(char c, int value, Fighter caster,Fighter target)
	{
		int esquiveC = c=='a'?caster.getTotalStats().getEffect(Constants.STATS_ADD_AFLEE):caster.getTotalStats().getEffect(Constants.STATS_ADD_MFLEE);
		int esquiveT = c=='a'?target.getTotalStats().getEffect(Constants.STATS_ADD_AFLEE):target.getTotalStats().getEffect(Constants.STATS_ADD_MFLEE);
		int ptsMax = c=='a'?caster.getTotalStatsLessBuff().getEffect(Constants.STATS_ADD_PA):caster.getTotalStatsLessBuff().getEffect(Constants.STATS_ADD_PM);
		int retrait = 0;
		
		for(int i = 0; i < value;i++)
		{
			int ptsAct = c=='a'?target.getTotalStats().getEffect(Constants.STATS_ADD_PA):target.getTotalStats().getEffect(Constants.STATS_ADD_PM);
			if(esquiveT == 0)esquiveT=1;
			if(esquiveC == 0)esquiveC=1;
			double a = esquiveC/esquiveT;
			if(ptsAct == 0) ptsAct = 1;
			if(ptsMax == 0) ptsMax = 1;
			double b = ptsAct/ptsMax;
			
			int chance = (int) (a * b * 50);
			if(chance <0)chance = 0;
			if(chance >100)chance = 100;
			/* DEBUG
			System.out.println("Chance d'esquiver le "+(i+1)+" eme PA : "+chance);
			//*/
			
			int jet = getRandomValue(0, 99);
			if(jet<chance)
				retrait++;
		}
		return retrait;
	}

	public static long getXpWinPvm2(Fighter perso, ArrayList<Fighter> winners,ArrayList<Fighter> loosers,long groupXP)
	{
		if(perso.getPersonnage()== null)return 0;
		if(winners.contains(perso))//Si winner
		{
			float sag = perso.getTotalStats().getEffect(Constants.STATS_ADD_SAGE);
			float coef = (sag + 100)/100;
			int taux = Ancestra.XP_PVM;
			long xpWin = 0;
			int lvlmax = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > lvlmax)
					lvlmax = entry.get_lvl();
			}
			int nbbonus = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > (lvlmax / 3))
					nbbonus += 1;				
			}
			
			double bonus = 1;
			if(nbbonus == 2)
				bonus = 1.1;
			if(nbbonus == 3)
				bonus = 1.5;
			if(nbbonus == 4)
				bonus = 2.3;
			if(nbbonus == 5)
				bonus = 3.1;
			if(nbbonus == 6)
				bonus = 3.6;
			if(nbbonus == 7)
				bonus = 4.2;
			if(nbbonus >= 8)
				bonus = 4.7;
			
			int lvlLoosers = 0;
			for(Fighter entry : loosers)
				lvlLoosers += entry.get_lvl();
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			double rapport = 1+((double)lvlLoosers/(double)lvlWinners);
			if (rapport <= 1.3)
				rapport = 1.3;
			/*
			if (rapport > 5)
				rapport = 5;
			//*/
			int lvl = perso.get_lvl();
			double rapport2 = 1 + ((double)lvl / (double)lvlWinners);

			xpWin = (long) (groupXP * rapport * bonus * taux *coef * rapport2);
			
			/*/ DEBUG XP
			System.out.println("=========");
			System.out.println("groupXP: "+groupXP);
			System.out.println("rapport1: "+rapport);
			System.out.println("bonus: "+bonus);
			System.out.println("taux: "+taux);
			System.out.println("coef: "+coef);
			System.out.println("rapport2: "+rapport2);
			System.out.println("xpWin: "+xpWin);
			System.out.println("=========");
			//*/
			return xpWin;	
		}
		return 0;
	}
	
	public static long getXpWinPvm(Fighter perso, ArrayList<Fighter> team,ArrayList<Fighter> loose, long groupXP)
	{
		int lvlwin = 0;
		for(Fighter entry : team)lvlwin += entry.get_lvl();
		int lvllos = 0;
		for(Fighter entry : loose)lvllos += entry.get_lvl();
		float bonusSage = (perso.getTotalStats().getEffect(Constants.STATS_ADD_SAGE)+100)/100;
		/* Formule 1
		float taux = perso.get_lvl()/lvlwin;
		long xp = (long)(groupXP * taux * bonusSage * perso.get_lvl());
		//*/
		//* Formule 2
		long sXp = groupXP*lvllos;
		long gXp = 2 * groupXP * perso.get_lvl();
        long xp = (long)((sXp + gXp)*bonusSage);
		//*/
		return xp*Ancestra.XP_PVM;
	}
	public static long getXpWinPvP(Fighter perso, ArrayList<Fighter> winners, ArrayList<Fighter> looser)
	{
		if(perso.getPersonnage()== null)return 0;
		if(winners.contains(perso.getGUID()))//Si winner
		{
			int lvlLoosers = 0;
			for(Fighter entry : looser)
				lvlLoosers += entry.get_lvl();
		
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			int taux = Ancestra.XP_PVP;
			float rapport = (float)lvlLoosers/(float)lvlWinners;
			long xpWin = (long)(
						(
							rapport
						*	getXpNeededAtLevel(perso.getPersonnage().get_lvl())
						/	100
						)
						*	taux
					);
			//DEBUG
			System.out.println("Taux: "+taux);
			System.out.println("Rapport: "+rapport);
			System.out.println("XpNeeded: "+getXpNeededAtLevel(perso.getPersonnage().get_lvl()));
			System.out.println("xpWin: "+xpWin);
			//*/
			return xpWin;
		}
		return 0;
	}
	
	private static long getXpNeededAtLevel(int lvl)
	{
		long xp = (World.getPersoXpMax(lvl) - World.getPersoXpMin(lvl));
		System.out.println("Xp Max => "+World.getPersoXpMax(lvl));
		System.out.println("Xp Min => "+World.getPersoXpMin(lvl));
		
		return xp;
	}

	public static long getGuildXpWin(Fighter perso, AtomicReference<Long> xpWin)
	{
		if(perso.getPersonnage()== null)return 0;
		if(perso.getPersonnage().getGuildMember() == null)return 0;
		
		GuildMember gm = perso.getPersonnage().getGuildMember();
		
		double xp = (double)xpWin.get(), Lvl = perso.get_lvl(),LvlGuild = perso.getPersonnage().get_guild().get_lvl(),pXpGive = (double)gm.getPXpGive()/100;
		
		double maxP = xp * pXpGive * 0.10;	//Le maximum donné à la guilde est 10% du montant prélevé sur l'xp du combat
		double diff = Math.abs(Lvl - LvlGuild);	//Calcul l'écart entre le niveau du personnage et le niveau de la guilde
		double toGuild;
		if(diff >= 70)
		{
			toGuild = maxP * 0.10;	//Si l'écart entre les deux level est de 70 ou plus, l'experience donnée a la guilde est de 10% la valeur maximum de don
		}
		else if(diff >= 31 && diff <= 69)
		{
			toGuild = maxP - ((maxP * 0.10) * (Math.floor((diff+30)/10)));
		}
		else if(diff >= 10 && diff <= 30)
		{
			toGuild = maxP - ((maxP * 0.20) * (Math.floor(diff/10))) ;
		}
		else	//Si la différence est [0,9]
		{
			toGuild = maxP;
		}
		xpWin.set((long)(xp - xp*pXpGive));
		return (long) Math.round(toGuild);
	}
	
	public static long getMountXpWin(Fighter perso, AtomicReference<Long> xpWin)
	{
		if(perso.getPersonnage()== null)return 0;
		if(perso.getPersonnage().getMount() == null)return 0;
		
		int diff = Math.abs(perso.get_lvl() - perso.getPersonnage().getMount().get_level());
		
		double coeff = 0;
		double xp = (double) xpWin.get();
		double pToMount = (double)perso.getPersonnage().getMountXpGive() / 100 + 0.2;
		
		if(diff >= 0 && diff <= 9)
			coeff = 0.1;
		else if(diff >= 10 && diff <= 19)
			coeff = 0.08;
		else if(diff >= 20 && diff <= 29)
			coeff = 0.06;
		else if(diff >= 30 && diff <= 39)
			coeff = 0.04;
		else if(diff >= 40 && diff <= 49)
			coeff = 0.03;
		else if(diff >= 50 && diff <= 59)
			coeff = 0.02;
		else if(diff >= 60 && diff <= 69)
			coeff = 0.015;
		else
			coeff = 0.01;
		
		if(pToMount > 0.2)
			xpWin.set((long)(xp - (xp*(pToMount-0.2))));
		
		return (long)Math.round(xp * pToMount * coeff);
	}

	public static int getKamasWin(Fighter i, ArrayList<Fighter> winners, int maxk, int mink)
	{
		maxk++;
		int rkamas = (int)(Math.random() * (maxk-mink)) + mink;
		return rkamas*Ancestra.KAMAS;
	}
	
	public static int getZaapCost(Carte map, Carte map2)
	{
		int cost = 0;
		
		return cost;
	}
	
	public static int calculElementChangeChance(int lvlM,int lvlA,int lvlP)
	{
		int K = 350;
		if(lvlP == 1)K = 100;
		else if (lvlP == 25)K = 175;
		else if (lvlP == 50)K = 350;
		return (int)((lvlM*100)/(K + lvlA));
	}
	
	/*public static Objet fmObjet(int lvlM, Objet toFm,Objet rune)
	{
		int runeType = rune.getStats().getMap().keySet().toArray(new Integer[1])[0].intValue();	//Récupère la stats de la rune
		int runeSize = rune.getStats().getMap().get(runeType);	//Récupère la puissance d'ajout de la rune
		int maxW8 = 0,minW8 = 0;
		
		Map<Integer,Integer> maxStat = toFm.getMinMaxStats(true);	//Récuperation des stats max possible
		Map<Integer,Integer> minStat = toFm.getMinMaxStats(false);	//Récuperation des stats min possible
		
		for(int a : maxStat.keySet())
		{
			maxW8 += toFm.getStats().getEffect(runeType)
		}
		
		return toFm;
	}*/

	public static int calculHonorWin(ArrayList<Fighter> winners,ArrayList<Fighter> loosers,Fighter F)
	{
		float totalGradeWin = 0;
		for(Fighter f : winners)
		{
			if(f.getPersonnage() == null )continue;
			totalGradeWin += f.getPersonnage().getGrade();
		}
		float totalGradeLoose = 0;
		for(Fighter f : loosers)
		{
			if(f.getPersonnage() == null)continue;
			totalGradeLoose += f.getPersonnage().getGrade();
		}
		int base = (int)(100 * (float)(totalGradeLoose/totalGradeWin))/winners.size();
		if(loosers.contains(F))base = -base;
		return base * Ancestra.HONOR;
	}

	public static int calculDeshonorWin(ArrayList<Fighter> winners,ArrayList<Fighter> loosers,Fighter F)
	{
		ArrayList<Fighter> ennemy = new ArrayList<Fighter>();
		if(winners.contains(F))
			ennemy.addAll(loosers);
		else
			ennemy.addAll(winners);
		
		if(F.getPersonnage() == null)return 0;//Pas normal ca XD
		if(F.getPersonnage().get_align() == Constants.ALIGNEMENT_NEUTRE || F.getPersonnage().get_align() == Constants.ALIGNEMENT_MERCENAIRE)return 0;
		
		for(Fighter f : ennemy)
		{
			if(f.getPersonnage() == null)continue;
			if(f.getPersonnage().get_align() == Constants.ALIGNEMENT_NEUTRE)return 1;
		}
		return 0;
	}
	
	public static Couple<Integer, Integer> decompPierreAme(Objet toDecomp)
	{
		Couple<Integer, Integer> toReturn;
		String[] stats = toDecomp.parseStatsString().split("#");
		int lvlMax = Integer.parseInt(stats[3],16);
		int chance = Integer.parseInt(stats[1],16);
		toReturn = new Couple<Integer,Integer>(chance,lvlMax);
		
		return toReturn;
	}
	
	public static int totalCaptChance(int pierreChance, Personnage p)
	{
		int sortChance = 0;

		switch(p.getSortStatBySortIfHas(413).getLevel())
		{
			case 1:
				sortChance = 1;
				break;
			case 2:
				sortChance = 3;
				break;
			case 3:
				sortChance = 6;
				break;
			case 4:
				sortChance = 10;
				break;
			case 5:
				sortChance = 15;
				break;
			case 6:
				sortChance = 25;
				break;
		}
		
		return sortChance + pierreChance;
	}
	
	public static String parseReponse(String reponse)
	{
		String toReturn = "";
		
		String[] cut = reponse.split("[%]");
		
		if(cut.length == 1)return reponse;
		
		toReturn += cut[0];
		
		char charact;
		for (int i = 1; i < cut.length; i++)
		{
			charact = (char) Integer.parseInt(cut[i].substring(0, 2),16);
			toReturn += charact+cut[i].substring(2);
		}
		
		return toReturn;
	}
	
	public static int spellCost(int nb)
	{
		int total = 0;
		for (int i = 1; i < nb ; i++)
		{
			total += i;
		}
		
		return total;
	}
}
