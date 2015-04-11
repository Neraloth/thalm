package objects;

import java.io.PrintWriter;

import objects.NPC_tmpl.NPC_question;
import objects.Objet.ObjTemplate;

import common.ConditionParser;
import common.Constants;
import common.Formulas;
import common.SocketManager;
import common.World;

import game.GameServer;

public class Action {

	private int ID;
	private String args;
	private String cond;
	
	public Action(int id, String args, String cond)
	{
		this.ID = id;
		this.args = args;
		this.cond = cond;
	}


	public void apply(Personnage perso, int objetID)
	{
		if(perso == null)return;
		if(!cond.equalsIgnoreCase("") && !cond.equalsIgnoreCase("-1")&& !ConditionParser.validConditions(perso,cond))
		{
			SocketManager.GAME_SEND_Im_PACKET(perso, "119");
			return;
		}
		PrintWriter out = perso.get_compte().getGameThread().get_out();	
		switch(ID)
		{
			case -2://créer guilde
				if(perso.is_away())return;
				if(perso.get_guild() != null || perso.getGuildMember() != null)
				{
					SocketManager.GAME_SEND_gC_PACKET(perso, "Ea");
					return;
				}
				SocketManager.GAME_SEND_gn_PACKET(perso);
			break;
			case -1://Ouvrir banque
				int cost = perso.getBankCost();
				if(cost > 0)
				{
					long nKamas = perso.get_kamas() - cost;
					if(nKamas <0)//Si le joueur n'a pas assez de kamas pour ouvrir la banque
					{
						
						return;
					}
					perso.set_kamas(nKamas);
					perso.kamasLog(-cost+"", "Ouverture de la banque");
					
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SocketManager.GAME_SEND_Im_PACKET(perso, "020;"+cost);
				}
				SocketManager.GAME_SEND_ECK_PACKET(perso.get_compte().getGameThread().get_out(), 5, "");
				SocketManager.GAME_SEND_EL_BANK_PACKET(perso);
				perso.set_away(true);
				perso.setInBank(true);
			break;
			
			case 0://Téléportation
				try
				{
					int newMapID = Integer.parseInt(args.split(",",2)[0]);
					int newCellID = Integer.parseInt(args.split(",",2)[1]);
					perso.teleport(newMapID,newCellID);
				}catch(Exception e ){return;};
			break;
			
			case 1://Discours NPC
				out = perso.get_compte().getGameThread().get_out();
				if(args.equalsIgnoreCase("DV"))
				{
					SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
					perso.set_isTalkingWith(0);
				}else
				{
					int qID = -1;
					try
					{
						qID = Integer.parseInt(args);
					}catch(NumberFormatException e){};
					
					NPC_question  quest = World.getNPCQuestion(qID);
					if(quest == null)
					{
						SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
						perso.set_isTalkingWith(0);
						return;
					}
					SocketManager.GAME_SEND_QUESTION_PACKET(out, quest.parseToDQPacket(perso));
				}
			break;
			
			case 4://Kamas
				try
				{
					int count = Integer.parseInt(args);
					long curKamas = perso.get_kamas();
					long newKamas = curKamas + count;
					if(newKamas <0) newKamas = 0;
					perso.set_kamas(newKamas);
					
					//Si en ligne (normalement oui)
					if(perso.isOnline())
						SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 5://objet
				try
				{
					int tID = Integer.parseInt(args.split(",")[0]);
					int count = Integer.parseInt(args.split(",")[1]);
					boolean send = true;
					if(args.split(",").length >2)send = args.split(",")[2].equals("1");
					
					//Si on ajoute
					if(count > 0)
					{
						ObjTemplate T = World.getObjTemplate(tID);
						if(T == null)return;
						Objet O = T.createNewItem(count, false);
						//Si retourne true, on l'ajoute au monde
						if(perso.addObjet(O, true))
							World.addObjet(O, true);
						perso.objetLog(tID, O.getQuantity(), "En utilisant un objet");
					}else
					{
						perso.removeByTemplateID(tID,-count);
					}
					//Si en ligne (normalement oui)
					if(perso.isOnline())//on envoie le packet qui indique l'ajout//retrait d'un item
					{
						SocketManager.GAME_SEND_Ow_PACKET(perso);
						if(send)
						{
							if(count >= 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "021;"+count+"~"+tID);
							}
							else if(count < 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "022;"+-count+"~"+tID);
							}
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 6://Apprendre un métier
				try
				{
					int mID = Integer.parseInt(args);
					if(World.getMetier(mID) == null)return;
					perso.learnJob(World.getMetier(mID));
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 7://retour au point de sauvegarde
				perso.warpToSavePos();
			break;
			case 8://Ajouter une Stat
				try
				{
					int statID = Integer.parseInt(args.split(",",2)[0]);
					int number = Integer.parseInt(args.split(",",2)[1]);
					perso.get_baseStats().addOneStat(statID, number);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					int messID = 0;
					switch(statID)
					{
						case Constants.STATS_ADD_INTE: messID = 14;break;
					}
					if(messID>0)
						SocketManager.GAME_SEND_Im_PACKET(perso, "0"+messID+";"+number);
				}catch(Exception e ){return;};
			break;
			case 9://Apprendre un sort
				try
				{
					int sID = Integer.parseInt(args);
					if(World.getSort(sID) == null)return;
					perso.learnSpell(sID,1, true,true);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 10://Pain/potion/viande/poisson
				try
				{
					int min = Integer.parseInt(args.split(",",2)[0]);
					int max = Integer.parseInt(args.split(",",2)[1]);
					if(max == 0) max = min;
					int val = Formulas.getRandomValue(min, max);
					if(perso.get_PDV() + val > perso.get_PDVMAX())val = perso.get_PDVMAX()-perso.get_PDV();
					perso.set_PDV(perso.get_PDV()+val);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 11://Definir l'alignement
				try
				{
					byte newAlign = Byte.parseByte(args.split(",",2)[0]);
					boolean replace = Integer.parseInt(args.split(",",2)[1]) == 1;
					//Si le perso n'est pas neutre, et qu'on doit pas remplacer, on passe
					if(perso.get_align() != Constants.ALIGNEMENT_NEUTRE && !replace)return;
					perso.modifAlignement(newAlign);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 12://Spawn Groupe args : boolean delObj?,boolean enArène?
				try
				{
					boolean delObj = args.split(",")[0].equals("true");
					boolean inArena = args.split(",")[1].equals("true");
					
					if(inArena && !World.isArenaMap(perso.get_curCarte().get_id()))return;	//Si la map du personnage n'est pas classé comme étant dans l'arène
					
					PierreAme pierrePleine = (PierreAme)World.getObjet(objetID);
					
					String groupData = pierrePleine.parseGroupData();
					String condition = "M_PID = "+perso.get_GUID();	//Condition pour que le groupe ne soit lançable que par le personnage qui à utiliser l'objet
					perso.get_curCarte().spawnGroup(true, false, perso.get_curCell().getID(), groupData,condition);
					
					if(delObj)
					{
						perso.removeItem(objetID, 1, true, true);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 13://Ouvrir l'interface d'oublie de sort
				perso.setisForgetingSpell(true);
				SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', perso);
			break;
			/*------------------LIGNE PAR MARTHIEUBEAN-----------------------*/
			//Ouvrir l'enclose pour accéder aux montures
			case 90:
				perso.openPublicMountPark();
				break;
			/*----------------------------FIN---------------------------------*/
			
			/* TODO: autres actions */
			
			default:
				GameServer.addToLog("Action ID="+ID+" non implantée");
			break;
		}
	}


	public int getID()
	{
		return ID;
	}
}
