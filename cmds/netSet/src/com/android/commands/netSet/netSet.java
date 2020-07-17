package com.android.commands.netSet;

import android.util.Log;
import android.util.Slog;
import java.io.IOException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.net.EthernetManager;
import android.net.IEthernetManager;

import android.os.RemoteException;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.StaticIpConfiguration;
import android.net.NetworkUtils;
import android.net.LinkAddress;
import android.net.LinkProperties;

import java.util.regex.Pattern;
import java.lang.Integer;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import android.os.SystemProperties;


public class netSet {
	private static final String TAG = "netSet";
    private static final String ETHERNET_SERVICE = "ethernet";
	private final static String nullIpInfo = "0.0.0.0";
	
	IEthernetManager mService;

	private  static String mEthHwAddress = null;
	private  static String mEthIpAddress = null;
	private  static String mEthNetmask = null;
	private  static String mEthGateway = null;
	private  static String mEthdns1 = null;
	private  static String mEthdns2 = null;

	private String[] mArgs;
    private int mNextArg;

	public netSet() {
	}
	
	/**
     * Get Ethernet configuration.
     * @return the Ethernet Configuration, contained in {@link IpConfiguration}.
     */
    public IpConfiguration getConfiguration() {
        try {
            return mService.getConfiguration();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

	 /**
     * Set Ethernet configuration.
     */
    public void setConfiguration(IpConfiguration config) {
        try {
            mService.setConfiguration(config);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

	//将子网掩码转换成ip子网掩码形式，比如输入32输出为255.255.255.255  
    public  String interMask2String(int prefixLength) {
        String netMask = null;
		int inetMask = prefixLength;
		
		int part = inetMask / 8;
		int remainder = inetMask % 8;
		int sum = 0;
		
		for (int i = 8; i > 8 - remainder; i--) {
			sum = sum + (int) Math.pow(2, i - 1);
		}
		
		if (part == 0) {
			netMask = sum + ".0.0.0";
		} else if (part == 1) {
			netMask = "255." + sum + ".0.0";
		} else if (part == 2) {
			netMask = "255.255." + sum + ".0";
		} else if (part == 3) {
			netMask = "255.255.255." + sum;
		} else if (part == 4) {
			netMask = "255.255.255.255";
		}

		return netMask;
	}

	public void getEthInfoFromStaticIp() {
		StaticIpConfiguration staticIpConfiguration=getConfiguration().getStaticIpConfiguration();

		if(staticIpConfiguration == null) {
			myLog("staticIpConfiguration==null");
			return;
		}
		
		LinkAddress ipAddress = staticIpConfiguration.ipAddress;
		InetAddress gateway   = staticIpConfiguration.gateway;
		ArrayList<InetAddress> dnsServers=staticIpConfiguration.dnsServers;

		if( ipAddress !=null) {
			mEthIpAddress=ipAddress.getAddress().getHostAddress();
			mEthNetmask=interMask2String(ipAddress.getPrefixLength());
		}
		
		if(gateway !=null) {
			mEthGateway=gateway.getHostAddress();
		}
		
		mEthdns1=dnsServers.get(0).getHostAddress();

		if(dnsServers.size() > 1) { /* 只保留两个*/
			mEthdns2=dnsServers.get(1).getHostAddress();
		}

		displayInfo("get mode: static", "static");
	}

	public String getIpAddress() {
        try {
            return mService.getIpAddress();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getNetmask() {
        try {
            return mService.getNetmask();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getGateway() {
        try {
            return mService.getGateway();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getDns() {
        try {
            return mService.getDns();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
	
	public void getEthInfoFromDhcp(){	
	String tempIpInfo;
	String iface = "eth0";
		
	tempIpInfo = getIpAddress();
	if ((tempIpInfo != null) && (!tempIpInfo.equals("")) ){ 
		mEthIpAddress = tempIpInfo;
	} else {  
		mEthIpAddress = nullIpInfo;
	}
				
	tempIpInfo = getNetmask();
	if ((tempIpInfo != null) && (!tempIpInfo.equals("")) ){
        mEthNetmask = tempIpInfo;
	} else {           		
		mEthNetmask = nullIpInfo;
	}
					
	tempIpInfo = getGateway();
	if ((tempIpInfo != null) && (!tempIpInfo.equals(""))){
    	mEthGateway = tempIpInfo;
	} else {
		mEthGateway = nullIpInfo;
	}

	tempIpInfo = getDns();
	if ((tempIpInfo != null) && (!tempIpInfo.equals(""))){
        String data[] = tempIpInfo.split(",");
   		mEthdns1 = data[0];
	
        if (data.length <= 1){
            mEthdns2 = nullIpInfo;
        }else{
            mEthdns2 = data[1];
        }
	} else {
		mEthdns1 = nullIpInfo;      		
	}

	displayInfo("get mode: dhcp", "dhcp");
}

	public void getEthInfo(){

        IpAssignment mode = getConfiguration().getIpAssignment();        
	   
         if ( mode== IpAssignment.DHCP) {
			//System.out.println("getEthInfoFromDhcp");
            getEthInfoFromDhcp();
	    } else if(mode == IpAssignment.STATIC) {
		    //System.out.println("getEthInfoFromStaticIp");
            getEthInfoFromStaticIp();
	    } else{
		    myLog("getEthInfo: unknow IpAssignment");
        }
    }

	private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException|ClassCastException e) {
            return null;
        }
    }

	/*
     * convert subMask string to prefix length
     */
    private int maskStr2InetMask(String maskStr) {
    	StringBuffer sb ;
    	String str;
    	int inetmask = 0; 
    	int count = 0;
    	/*
    	 * check the subMask format
    	 */
      	Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
    	if (pattern.matcher(maskStr).matches() == false) {
    		myLog("subMask is error");
    		return 0;
    	}
    	
    	String[] ipSegment = maskStr.split("\\.");
    	for(int n =0; n<ipSegment.length;n++) {
    		sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(ipSegment[n])));
    		str = sb.reverse().toString();
    		count=0;
    		for(int i=0; i<str.length();i++) {
    			i=str.indexOf("1",i);
    			if(i==-1)  
    				break;
    			count++;
    		}
    		inetmask+=count;
    	}
    	return inetmask;
    }
	
	private boolean setStaticIpConfiguration() {
		if (mArgs.length < 6) {
            showUsage();
            return false;
        }

		mEthIpAddress = nextArg();
		mEthNetmask   = nextArg();
		mEthGateway   = nextArg();
		mEthdns1      = nextArg();
		mEthdns2      = nextArg();
		displayInfo("Set mode: static", "static");
		
        Inet4Address inetAddr = getIPv4Address(this.mEthIpAddress);
        int prefixLength = maskStr2InetMask(this.mEthNetmask); 
        InetAddress gatewayAddr =getIPv4Address(this.mEthGateway); 
        InetAddress dnsAddr = getIPv4Address(this.mEthdns1);
		 
        if (null==inetAddr || null==gatewayAddr || null==dnsAddr 
			|| inetAddr.getAddress().toString().isEmpty() || prefixLength ==0 
			|| gatewayAddr.toString().isEmpty() || dnsAddr.toString().isEmpty()) {
              myLog("ip,mask or dnsAddr is wrong");
			  return false;
		}
		  
		StaticIpConfiguration staticIpConfiguration =new StaticIpConfiguration();
        staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, prefixLength);
        staticIpConfiguration.gateway=gatewayAddr;
        staticIpConfiguration.dnsServers.add(dnsAddr);
		
		setConfiguration(new IpConfiguration(IpAssignment.STATIC, ProxySettings.NONE, staticIpConfiguration, null));
        return true;
    }

	public void setEthInfo() {
		String op = nextArg();
		if (op == null) {
		    showUsage();
		    return;
		}

		if ("dhcp".equals(op)) {
			setConfiguration(new IpConfiguration(IpAssignment.DHCP, ProxySettings.NONE,null,null));
    	    myLog("switch to dhcp");
            return;
        }

		if ("static".equals(op)) {
			setStaticIpConfiguration();
    	    myLog("switch to static");
            return;
        }

		showUsage();
	}

    public void run(String[] args) {
        if (args.length < 1) {
            showUsage();
            return;
        }

		IBinder b = ServiceManager.getService(ETHERNET_SERVICE);
        mService = IEthernetManager.Stub.asInterface(b);
        if (mService == null) {
			myLog("get ethernet manager failed");
            return;
        }
		
		mArgs = args;
        String op = args[0];
        mNextArg = 1;

		if ("get".equals(op)) {
            getEthInfo();
            return;
        }

		if ("set".equals(op)) {
            setEthInfo();
            return;
        }

		showUsage();
	}
	
	public static void main(String[] args) {
			myLog("BEGIN TO SET OR GET NET INFO!");
            new netSet().run(args);
	}

	private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
		
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }

	public static void myLog(String msg) {
		System.out.println(msg);
		Log.i(TAG, msg);
	}

	private void displayInfo(String msg, String mode) {
		myLog(msg);
		myLog("mode: " + mode);
		myLog("mEthIpAddress: " + mEthIpAddress);
		myLog("mEthNetmask: " 	+ mEthNetmask);
		myLog("mEthGateway: "   + mEthGateway);
		myLog("mEthdns1: "      + mEthdns1);

		SystemProperties.set("fjrh.eth0.mode", mode);
		SystemProperties.set("fjrh.eth0.ip", mEthIpAddress);
		SystemProperties.set("fjrh.eth0.mask", mEthNetmask);
		SystemProperties.set("fjrh.eth0.gateway", mEthGateway);
		SystemProperties.set("fjrh.eth0.dns1", mEthdns1);

	}
	
    public void showUsage() {
		System.err.println("usage: netSet [get|set] <[dhcp|static] ip netmask gateway dns1 dns2>");
	}
}

