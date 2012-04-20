package sk.fiit.stuba.androidimhd;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.TooManyListenersException;

import stuba.sip.InvalidArgumentException;
import stuba.sip.ObjectInUseException;
import stuba.sip.PeerUnavailableException;
import stuba.sip.SipException;
import stuba.sip.TransportNotSupportedException;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class AndoirdImhd extends Activity {
	
	SipLayer sipLayer;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Hello, Android");
        //setContentView(tv);
        //setContentView(R.layout.main);
        
        Button button = new Button(this);
        button.setText("Send SIP MESSAGE.");
        setContentView(button);
        
        InetAddress[] intet = null;
        String ip, ips = null;
		try {
			intet = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(intet != null)
		    for (InetAddress inetAddress : intet) {
				System.out.println("adresa: " + inetAddress.getHostAddress());
				ips += " " + inetAddress.getHostAddress() + " ";
			}
        
        try {
        	ip = InetAddress.getLocalHost().getHostAddress();
        	System.out.println("_MDEBUG: my IP is=" + ip);
			sipLayer = new SipLayer("bob", "10.0.2.15", 5070, button);
			button.setText(ips);
		} catch (PeerUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ObjectInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
        
        button.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				try {
					String ip = InetAddress.getLocalHost().getHostAddress();
					//sipLayer.sendMessage("sip:snoopy71@10.0.0.2:5061", "android");//10.0.2.2 je PC
					sipLayer.initiateRegister();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SipException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
    }
}