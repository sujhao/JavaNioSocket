package com.hao;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.hao.config.Setting;

public class SocketAttachment {
	public ByteBuffer readMsgBuffer;
	public LinkedList<ByteBuffer> outMsgList = new LinkedList<ByteBuffer>();
	public SocketAttachment()
	{
		readMsgBuffer = ByteBuffer.allocate(Setting.bufferSize);
	}
}
