package com.hao;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OutMsgWorker extends Thread{
	
	private OutMsgHandler handler;
	private BlockingQueue<SocketChannel> channelQueue = new LinkedBlockingQueue<SocketChannel>();
	
	public OutMsgWorker(String name, OutMsgHandler handler)
	{
		super(name);
		this.handler = handler;
	}

	@Override
	public void run() {
		while(GameServer.instance.isRunning)
		{
			SocketChannel sc;
			try {
				if((sc = channelQueue.take()) != null)
				{
					this.handler.processWrite(sc);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void handleChannel(SocketChannel sc)
	{
		if(sc != null)
		{
			try {
				channelQueue.put(sc);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
