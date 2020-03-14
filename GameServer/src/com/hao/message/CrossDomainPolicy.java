package com.hao.message;

import java.nio.channels.SocketChannel;

import com.hao.GameServer;
import com.hao.config.Setting;

public class CrossDomainPolicy {
	public static final CrossDomainPolicy instance = new CrossDomainPolicy();
	private String crossDomainContent;
	
	private CrossDomainPolicy()
	{
		StringBuilder sb = new StringBuilder("<cross-domain-policy>");
		sb.append("<allow-access-from domain=\"*\" to-ports=\">");
		sb.append(Setting.port).append("\"/></cross-domain-policy>");
		sb.append("</cross-domain-policy>");
		crossDomainContent = sb.toString();
	}
	
	public void send(SocketChannel sc)
	{
		System.out.println("CrossDomainPolicy:\n"+crossDomainContent);
//		GameServer.instance.serverWriter.sendResponse(crossDomainContent, sc);
	}
}
