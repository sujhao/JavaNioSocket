package com.hao;

import java.nio.channels.SocketChannel;

import com.hao.config.Setting;

public abstract class OutMsgHandler {
	
	private OutMsgWorker[] workers;
	
	public OutMsgHandler()
	{
		int threadCount = Setting.writeThreadNum;
		workers = new OutMsgWorker[threadCount];
		for(int i=0; i<threadCount; i++)
		{
			workers[i] = new OutMsgWorker("OutMsgHandler"+i, this);
			workers[i].start();
		}
	}
	
	public void handleChannel(SocketChannel sc)
	{
		int key = sc.hashCode()%Setting.writeThreadNum;
		OutMsgWorker worker = this.workers[key];
		worker.handleChannel(sc);
	}
	
	protected abstract void processWrite(SocketChannel sc);
}
