package com.hao;

public class ServerReader implements Runnable{
	
	private Thread thread;
	
	public ServerReader()
	{
		this.thread = new Thread(this);
	}
	
	public void start()
	{
		this.thread.start();
	}
	
	@Override
	public void run() {
	
		while(GameServer.instance.isRunning)
		{
			try {
				IoManager.instance.handleIO();
				Thread.sleep(5L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
