package com.larphoid.lanchatexample;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Client {
	InetAddress ip;
	long lastRead;
	List<Long> timestamps = new ArrayList<Long>();
	List<String> messages = new ArrayList<String>();
	List<Boolean> isFromSender = new ArrayList<Boolean>();
}
