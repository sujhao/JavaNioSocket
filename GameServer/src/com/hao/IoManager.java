package com.hao;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import com.alibaba.fastjson.JSONObject;
import com.hao.config.Setting;


public class IoManager {
	
	public static IoManager instance = new IoManager();
	private ServerSocketChannel serverSocketChannel;
	
	/**
	 * 为什么要有2个selector，因为可以实现在不同的线程用不同的selector做不同的事情
	 */
	private Selector acceptSelector;	
	private Selector ioSelector;
	
	public IoManager()
	{
		try {
			// 获得一个ServerSocket通道 
			serverSocketChannel = ServerSocketChannel.open();
			// 设置通道为非阻塞  
			serverSocketChannel.configureBlocking(false);
			// 将该通道对应的ServerSocket绑定到port端口  
			serverSocketChannel.socket().bind(new InetSocketAddress(Setting.port));
			// 获得一个通道管理器  
			acceptSelector = Selector.open();
		    //将通道管理器和该通道绑定，并为该通道注册SelectionKey.OP_ACCEPT事件,注册该事件后，  
			//当该事件到达时，selector.select()会返回，如果该事件没到达selector.select()会一直阻塞
			serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
			ioSelector = Selector.open();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addClient()
	{
		try {
			//当注册的事件到达时，方法返回；否则,该方法会一直阻塞
			acceptSelector.selectNow();
			// 获得selector中选中的项的迭代器，选中的项为注册的事件 
			Iterator<SelectionKey> ite = acceptSelector.selectedKeys().iterator();
			while(ite.hasNext())
			{
				SelectionKey key = (SelectionKey)ite.next();
				ite.remove(); // 删除已选的key,以防重复处理  
				// 客户端请求连接事件  
				if(key.isAcceptable())
				{
					ServerSocketChannel serverChanel = (ServerSocketChannel)key.channel();
					// 获得和客户端连接的通道  
					SocketChannel socketChannel = serverChanel.accept();
				    // 设置成非阻塞  
					socketChannel.configureBlocking(false);
					//在和客户端连接成功之后，为了可以接收到客户端的信息，需要给通道设置读的权限
					System.out.println("addClient:"+socketChannel.socket().getInetAddress().getHostAddress());
//					CrossDomainPolicy.instance.send(socketChannel);//发送组策略文件给前端
					socketChannel.register(ioSelector, SelectionKey.OP_READ, new SocketAttachment());
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void handleIO()
	{
		try {
			ioSelector.selectNow();
			Iterator<SelectionKey> ite = ioSelector.selectedKeys().iterator();
			while(ite.hasNext())
			{
				SelectionKey key = (SelectionKey)ite.next();
				ite.remove(); // 删除已选的key,以防重复处理  
				// 客户端请求连接事件  
				if(key.isReadable())
				{
//					System.out.println("isReadable:");
					try
					{
						handleRead(key);
					}catch(Exception e){
						
					}					
				}
//				else if(key.isWritable())
//				{
//					System.out.println("写信息");
//					handleWrite(key);
//				}
				else 
				{
					System.out.println("handleIO信息");
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private void handleWrite()
//	{
//		
//	}

	/**
	 * 处理读取客户端发来的信息 的事件 
	 * @param key
	 */
	private void handleRead(SelectionKey key) throws Exception
	{
		SocketAttachment at  = (SocketAttachment)key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();   
		if(!readBytesFromChannel(channel, at))
		{
			return;//从客户端读不到东西了证明客户端端口连接了
		}
		if(at.readMsgBuffer.remaining() < 4)		//如果长度不够一个字节，则将位置设为极限，将极限设为容量继续等待数据
		{
			at.readMsgBuffer.position(at.readMsgBuffer.limit());
			at.readMsgBuffer.limit(at.readMsgBuffer.capacity());
			return;
		}
		at.readMsgBuffer.flip();
		int size = at.readMsgBuffer.getInt();
		System.out.println("handleRead:"+size);
		boolean hasSizeByte = true;
		while(at.readMsgBuffer.remaining() >= size)
		{
			hasSizeByte = false;
			analysisOneMessage(size, at.readMsgBuffer, channel);
			if(at.readMsgBuffer.remaining() < 4)
			{
				at.readMsgBuffer.compact();
				break;
			}
			else
			{
				size = at.readMsgBuffer.getInt();
				hasSizeByte = true;
			}
		}
		if(hasSizeByte)	//执行到这里说明有size，但没有足够size的后续bytes
		{
			at.readMsgBuffer.position(at.readMsgBuffer.position()-4);	//把位置设会没读取长度前的位置并把读了的东西删掉
			at.readMsgBuffer.compact();
		}
	}
	
	private boolean readBytesFromChannel(SocketChannel channel, SocketAttachment at)
	{
		boolean isConnected = true;
		try {
			int count = channel.read(at.readMsgBuffer);
			if(count == -1)
			{
				isConnected = false;
			}
		} catch (IOException e) {
//			e.printStackTrace();
		}catch(Exception ex){
			
		}
		return isConnected;
	}
	
	private void analysisOneMessage(int size, ByteBuffer buffer, SocketChannel channel)
	{
		byte[] message = new byte[size];
		System.out.println("analysisOneMessageSize:"+size);
		buffer.get(message);
		
		String messageString = "";
		try {
			messageString = new String(message, "UTF-8"); // Byte转化成String
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println("analysisJson====:"+messageString);
		JSONObject jsonOb = JSONObject.parseObject(messageString);
		jsonOb.put("test", "sujiehao");
		jsonOb.put("res", 1);
		GameServer.instance.serverWriter.sendResponse(jsonOb, channel);
		
	}
	
	public LinkedList<ByteBuffer> getChannelOutMsgList(SocketChannel sc)
	{
		SocketAttachment att = getChannelSocketAttachment(sc);
		return att != null ? att.outMsgList:null;
	}
	
	public SocketAttachment getChannelSocketAttachment(SocketChannel sc)
	{
		SelectionKey sk  = sc.keyFor(ioSelector);
		return sk.attachment() != null ? (SocketAttachment)sk.attachment():null;
	}
}