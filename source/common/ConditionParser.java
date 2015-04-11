package common;
import java.util.ArrayList;

import com.singularsys.jep.Jep;
import com.singularsys.jep.JepException;

import objects.*;

public class ConditionParser
{
	public static boolean validConditions(Personnage perso,String req)
	{
		if(req == null || req.equals(""))return true;
		if(req.contains("BI"))return false;
		if(perso.get_compte().get_gmLvl() >= 5)return true;
		
		Jep jep = new Jep();
		if(req.contains("PO"))
			req = havePO(req, perso);
		
		req = req.replace("&", "&&").replace("=", "==").replace("|", "||").replace("!", "!=");
		try
		{
				//Stats stuff compris
				jep.addVariable("CI", perso.getTotalStats().getEffect(Constants.STATS_ADD_INTE));
			 	jep.addVariable("CV", perso.getTotalStats().getEffect(Constants.STATS_ADD_VITA));
			 	jep.addVariable("CA", perso.getTotalStats().getEffect(Constants.STATS_ADD_AGIL));
			 	jep.addVariable("CW", perso.getTotalStats().getEffect(Constants.STATS_ADD_SAGE));
			 	jep.addVariable("CC", perso.getTotalStats().getEffect(Constants.STATS_ADD_CHAN));
			 	jep.addVariable("CS", perso.getTotalStats().getEffect(Constants.STATS_ADD_FORC));
			 	//Stats de bases
			 	jep.addVariable("Ci", perso.get_baseStats().getEffect(Constants.STATS_ADD_INTE));
			 	jep.addVariable("Cs", perso.get_baseStats().getEffect(Constants.STATS_ADD_FORC));
			 	jep.addVariable("Cv", perso.get_baseStats().getEffect(Constants.STATS_ADD_VITA));
			 	jep.addVariable("Ca", perso.get_baseStats().getEffect(Constants.STATS_ADD_AGIL));
			 	jep.addVariable("Cw", perso.get_baseStats().getEffect(Constants.STATS_ADD_SAGE));
			 	jep.addVariable("Cc", perso.get_baseStats().getEffect(Constants.STATS_ADD_CHAN));
			 	//Autre
			 	jep.addVariable("Ps", perso.get_align());
			 	jep.addVariable("Pa", perso.getALvl());
			 	jep.addVariable("PP", perso.getGrade());
			 	jep.addVariable("PL", perso.get_lvl());
			 	jep.addVariable("PK", perso.get_kamas());
			 	jep.addVariable("PG", perso.get_classe());
			 	jep.addVariable("PS", perso.get_sexe());
			 	jep.addVariable("PZ", true);//Abonnement
			 	jep.addVariable("PX",perso.get_compte().get_gmLvl());
			 	//Gérer PO PJ PN
			 	
			 	/*AJOUTÉ PAR MARTHIEUBEAN (HOMEMADE)*/
			 	jep.addVariable("M_PID",perso.get_GUID());
			 	/*FIN*/
			 	
			 	jep.parse(req);
			 	Object result = jep.evaluate();
			 	boolean ok = false;
			 	if(result != null)ok = Boolean.valueOf(result.toString());
			 	return ok;
		} catch (JepException e)
		{
			System.out.println("An error occurred: " + e.getMessage());
		}
		return true;
	}
	
	public static String havePO(String cond,Personnage perso)
	{
		String[] cut = cond.replaceAll("[ ()]", "").split("[|&]");

		ArrayList<Integer> value = new ArrayList<Integer>(cut.length);
		
		for(String cur : cut)
		{
			if(!cur.contains("PO"))
				continue;

			if(perso.hasItemGuid(Integer.parseInt(cur.split("[=]")[1])))
				value.add(Integer.parseInt(cur.split("[=]")[1]));
			else
				value.add(-1);
		}
		
		for(int curValue : value)
		{
			cond = cond.replaceFirst("PO", curValue+"");
		}

		return cond;
	}
}
