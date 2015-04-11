package action;



import java.io.IOException;
import java.net.ServerSocket;
import common.Ancestra;

public class ActionServer implements Runnable{

	private ServerSocket _SS;
	private Thread _t;

	public ActionServer()
	{
		try {
			_SS = new ServerSocket(Ancestra.CONFIG_ACTION_PORT);
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		} catch (IOException e) {
			addToLog("IOException: "+e.getMessage());
			e.printStackTrace();
			Ancestra.closeServers();
		}
		
	}

	public void run()
	{	
		while(Ancestra.isRunning)//bloque sur _SS.accept()
		{
			try
			{
				new ActionThread(_SS.accept());
			}catch(IOException e)
			{
				try
				{
					addToLog("Fermeture du serveur d'action");	
					if(!_SS.isClosed())_SS.close();
				}
				catch(IOException e1){}
			}
		}
	}
	
	public void kickAll()
	{
		try {
			_SS.close();
		} catch (IOException e) {}
	}
	public synchronized static void addToLog(String str)
	{
		Ancestra.addToShopLog(str);
	}
	
	public synchronized static void addToSockLog(String str)
	{
		if(Ancestra.CONFIG_DEBUG)
		{
			System.out.println (str);
		}
		
	}

	public Thread getThread()
	{
		return _t;
	}
}
