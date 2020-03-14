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
	 * ΪʲôҪ��2��selector����Ϊ����ʵ���ڲ�ͬ���߳��ò�ͬ��selector����ͬ������
	 */
	private Selector acceptSelector;	
	private Selector ioSelector;
	
	public IoManager()
	{
		try {
			// ���һ��ServerSocketͨ�� 
			serverSocketChannel = ServerSocketChannel.open();
			// ����ͨ��Ϊ������  
			serverSocketChannel.configureBlocking(false);
			// ����ͨ����Ӧ��ServerSocket�󶨵�port�˿�  
			serverSocketChannel.socket().bind(new InetSocketAddress(Setting.port));
			// ���һ��ͨ��������  
			acceptSelector = Selector.open();
		    //��ͨ���������͸�ͨ���󶨣���Ϊ��ͨ��ע��SelectionKey.OP_ACCEPT�¼�,ע����¼���  
			//�����¼�����ʱ��selector.select()�᷵�أ�������¼�û����selector.select()��һֱ����
			serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
			ioSelector = Selector.open();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addClient()
	{
		try {
			//��ע����¼�����ʱ���������أ�����,�÷�����һֱ����
			acceptSelector.selectNow();
			// ���selector��ѡ�е���ĵ�������ѡ�е���Ϊע����¼� 
			Iterator<SelectionKey> ite = acceptSelector.selectedKeys().iterator();
			while(ite.hasNext())
			{
				SelectionKey key = (SelectionKey)ite.next();
				ite.remove(); // ɾ����ѡ��key,�Է��ظ�����  
				// �ͻ������������¼�  
				if(key.isAcceptable())
				{
					ServerSocketChannel serverChanel = (ServerSocketChannel)key.channel();
					// ��úͿͻ������ӵ�ͨ��  
					SocketChannel socketChannel = serverChanel.accept();
				    // ���óɷ�����  
					socketChannel.configureBlocking(false);
					//�ںͿͻ������ӳɹ�֮��Ϊ�˿��Խ��յ��ͻ��˵���Ϣ����Ҫ��ͨ�����ö���Ȩ��
					System.out.println("addClient:"+socketChannel.socket().getInetAddress().getHostAddress());
//					CrossDomainPolicy.instance.send(socketChannel);//����������ļ���ǰ��
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
				ite.remove(); // ɾ����ѡ��key,�Է��ظ�����  
				// �ͻ������������¼�  
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
//					System.out.println("д��Ϣ");
//					handleWrite(key);
//				}
				else 
				{
					System.out.println("handleIO��Ϣ");
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
	 * �����ȡ�ͻ��˷�������Ϣ ���¼� 
	 * @param key
	 */
	private void handleRead(SelectionKey key) throws Exception
	{
		SocketAttachment at  = (SocketAttachment)key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();   
		if(!readBytesFromChannel(channel, at))
		{
			return;//�ӿͻ��˶�����������֤���ͻ��˶˿�������
		}
		if(at.readMsgBuffer.remaining() < 4)		//������Ȳ���һ���ֽڣ���λ����Ϊ���ޣ���������Ϊ���������ȴ�����
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
		if(hasSizeByte)	//ִ�е�����˵����size����û���㹻size�ĺ���bytes
		{
			at.readMsgBuffer.position(at.readMsgBuffer.position()-4);	//��λ�����û��ȡ����ǰ��λ�ò��Ѷ��˵Ķ���ɾ��
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
			messageString = new String(message, "UTF-8"); // Byteת����String
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