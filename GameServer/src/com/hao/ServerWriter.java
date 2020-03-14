package com.hao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;

import com.alibaba.fastjson.JSONObject;
import com.hao.config.Setting;

public class ServerWriter extends OutMsgHandler{

//	public void sendResponse(ActionScriptObject ao, List<SocketChannel> channels)
//	{
//		for(SocketChannel sc:channels)
//		{
//			sendResponse(ao, sc);
//		}
//	}
//	
//	public void sendResponse(ActionScriptObject ao, SocketChannel sc)
//	{
////		byte[] byteMsg = Amf3Helper.convertAsObject2ByteArray(ao.getWrapData());
//		byte[] byteMsg = null;
//		this.sendResponse(byteMsg, sc);
//	}
	
	public void sendResponse(JSONObject jsonOb, SocketChannel sc)
	{
		byte[] byteMsg = jsonOb.toJSONString().getBytes();
		this.sendResponse(byteMsg, sc);
	}
	
	public void sendResponse(String s, SocketChannel sc)
	{
		byte[] byteMsg = s.getBytes(Charset.forName(Setting.charsetName));
		this.sendResponse(byteMsg, sc);
	}
	
	public void sendResponse(byte[] byteMsg, SocketChannel sc)
	{
		ByteBuffer buffer = ByteBuffer.allocate(byteMsg.length+4);
		buffer.putInt(byteMsg.length);
		buffer.put(byteMsg);
		buffer.flip();
		System.out.println("sendResponseLen:"+byteMsg.length);
		LinkedList<ByteBuffer> outMsgList = IoManager.instance.getChannelOutMsgList(sc);
		if(outMsgList==null)return;
		synchronized (outMsgList) {
			outMsgList.addLast(buffer);
			if(outMsgList.size() == 1)
			{
				this.handleChannel(sc);
			}
		}
	}

	@Override
	protected void processWrite(SocketChannel sc) {
		LinkedList<ByteBuffer> outMsgList = IoManager.instance.getChannelOutMsgList(sc);
		if(outMsgList == null || outMsgList.size() == 0)return;
		synchronized (outMsgList) {
			ByteBuffer buffer = outMsgList.getFirst();
			boolean writeAllOk = writeToChannel(sc, buffer);
			System.out.println("writeAllOk:"+writeAllOk);
			if(writeAllOk)
			{
				outMsgList.removeFirst();
			}
			if(outMsgList.size() > 0)
			{
				handleChannel(sc);
			}
		}
	}
	
	private boolean writeToChannel(SocketChannel sc, ByteBuffer buffer)
	{
		boolean writeAllOk = false;
		try {
			sc.write(buffer);
			if(buffer.hasRemaining())
			{
				writeAllOk = false;
			}
			else
			{
				writeAllOk = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writeAllOk;
	}
	
}
