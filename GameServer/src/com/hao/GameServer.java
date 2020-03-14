package com.hao;

public class GameServer extends Thread {
	
	public static GameServer instance = new GameServer();
	public volatile boolean isRunning = false;
	private ServerReader serverReader;
	public ServerWriter serverWriter;
	
	public static void main(String[] args)
	{
		instance.start();
	}
	
	@Override
	public void run()
	{
		isRunning = true;
		serverReader = new ServerReader();
		serverReader.start();
		serverWriter = new ServerWriter();
		System.out.println("GameServer start success=====");
		while(isRunning)
		{
			try {
				IoManager.instance.addClient();
				Thread.sleep(10L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopServer()
	{
		
	}
	
}
