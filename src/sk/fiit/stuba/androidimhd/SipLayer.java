package sk.fiit.stuba.androidimhd;

import gov.nist.stuba.core.NameValue;
import gov.nist.stuba.core.NameValueList;
import gov.nist.stuba.sip.address.ParameterNames;
import gov.nist.stuba.sip.address.SipUri;
import gov.nist.stuba.sip.clientauthutils.AccountManager;
import gov.nist.stuba.sip.clientauthutils.AuthenticationHelperImpl;
import gov.nist.stuba.sip.clientauthutils.MessageDigestAlgorithm;
import gov.nist.stuba.sip.clientauthutils.UserCredentials;
import gov.nist.stuba.sip.header.AuthenticationHeader;
import gov.nist.stuba.sip.header.AuthenticationInfo;
import gov.nist.stuba.sip.header.Authorization;
import gov.nist.stuba.sip.header.Credentials;
import gov.nist.stuba.sip.header.ParametersExt;
import gov.nist.stuba.sip.header.SIPHeaderNames;
import gov.nist.stuba.sip.header.ims.ParameterNamesIms;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.TooManyListenersException;

import org.apache.http.conn.util.InetAddressUtils;

import android.widget.Button;

import stuba.sip.ClientTransaction;
import stuba.sip.Dialog;
import stuba.sip.DialogTerminatedEvent;
import stuba.sip.IOExceptionEvent;
import stuba.sip.InvalidArgumentException;
import stuba.sip.ListeningPoint;
import stuba.sip.ObjectInUseException;
import stuba.sip.PeerUnavailableException;
import stuba.sip.RequestEvent;
import stuba.sip.ResponseEvent;
import stuba.sip.ServerTransaction;
import stuba.sip.SipException;
import stuba.sip.SipFactory;
import stuba.sip.SipListener;
import stuba.sip.SipProvider;
import stuba.sip.SipStack;
import stuba.sip.TimeoutEvent;
import stuba.sip.TransactionTerminatedEvent;
import stuba.sip.TransactionUnavailableException;
import stuba.sip.TransportNotSupportedException;
import stuba.sip.address.Address;
import stuba.sip.address.AddressFactory;
import stuba.sip.address.SipURI;
import stuba.sip.header.AllowEventsHeader;
import stuba.sip.header.AllowHeader;
import stuba.sip.header.CSeqHeader;
import stuba.sip.header.CallIdHeader;
import stuba.sip.header.ContactHeader;
import stuba.sip.header.ContentTypeHeader;
import stuba.sip.header.ExpiresHeader;
import stuba.sip.header.FromHeader;
import stuba.sip.header.HeaderFactory;
import stuba.sip.header.MaxForwardsHeader;
import stuba.sip.header.ToHeader;
import stuba.sip.header.UserAgentHeader;
import stuba.sip.header.ViaHeader;
import stuba.sip.header.WWWAuthenticateHeader;
import stuba.sip.message.MessageFactory;
import stuba.sip.message.Request;
import stuba.sip.message.Response;

public class SipLayer implements SipListener {

    private MessageProcessor messageProcessor;

    private String username;

    private SipStack sipStack;

    private SipFactory sipFactory;

    private AddressFactory addressFactory;

    private HeaderFactory headerFactory;

    private MessageFactory messageFactory;

    private SipProvider sipProvider;
    
    private Dialog registerDialog;
    
    private ClientTransaction trans;
    
    private int debugCnt = 0;
    
    private static final String domain = "open-ims.test"; //normalne by sa to malo ziskavat cez DHCP, popr. manualnu konfiguraciu
    
    private Button button;

    /** Here we initialize the SIP stack. */
    public SipLayer(String username, String ip, int port, Button button)
	    throws PeerUnavailableException, TransportNotSupportedException,
	    InvalidArgumentException, ObjectInUseException,
	    TooManyListenersException {
	setUsername(username);
	sipFactory = SipFactory.getInstance();
	sipFactory.setPathName("gov.nist");
	Properties properties = new Properties();
	
	properties.setProperty("stuba.sip.STACK_NAME", "AndroidIMHD");
	properties.setProperty("stuba.sip.OUTBOUND_PROXY", "pcscf.open-ims.test:4060/UDP");
	
	this.button = button;
	
	String name = properties.getProperty("stuba.sip.STACK_NAME");
	
	if(name == null)
		System.out.println("NULL_MDEBUG: " + properties.getProperty("stuba.sip.STACK_NAME"));
	else
		System.out.println("NONULL_MDEBUG: " + properties.getProperty("stuba.sip.STACK_NAME"));
	
	System.out.println("_MDEBUG: set IP=" + ip);
	
	properties.setProperty("stuba.sip.IP_ADDRESS", ip);

	//DEBUGGING: Information will go to files 
	//textclient.log and textclientdebug.log
	properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
	properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
		"textclient.txt");
	properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
		"textclientdebug.log");

	sipStack = sipFactory.createSipStack(properties);
	headerFactory = sipFactory.createHeaderFactory();
	addressFactory = sipFactory.createAddressFactory();
	messageFactory = sipFactory.createMessageFactory();

	ListeningPoint tcp = sipStack.createListeningPoint(port, "tcp");
	ListeningPoint udp = sipStack.createListeningPoint(port, "udp");

	sipProvider = sipStack.createSipProvider(tcp);
	sipProvider.addSipListener(this);
	sipProvider = sipStack.createSipProvider(udp);
	sipProvider.addSipListener(this);
	
	Credentials credentials = new Credentials();
	NameValueList nmList = new NameValueList();
	//nmList.put(name, nameValue)
	//credentials.setCredentials(c);
	
    }

    /**
     * This method initiates registration process with OpenIMS core
     * @throws ParseException 
     * @throws InvalidArgumentException 
     * @throws SipException 
     */
    public void initiateRegister() throws ParseException, InvalidArgumentException, SipException {
    	
    	SipURI from = addressFactory.createSipURI(getUsername(), getHostName()
    			+ ":" + getPort());
    		Address fromNameAddress = addressFactory.createAddress(from);
    		fromNameAddress.setDisplayName(getUsername());
    		FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
    			"AndroidIMHD1.0");

    		String to = "sip:" + getUsername() + "@" + getHostName() + ":" + getPort(); //specificky pripad pre REGISTER to=from
    		
    		String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
    		String address = to.substring(to.indexOf("@") + 1);

    		SipURI toAddress = addressFactory.createSipURI(username, address);
    		Address toNameAddress = addressFactory.createAddress(toAddress);
    		toNameAddress.setDisplayName(username);
    		ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

    		SipUri requestURI = new SipUri(); //specificky pripad pre REGISTER
    		requestURI.setHost(domain);
    		
    		try {
				System.out.println("_MDEBUG: test DNS resolve:" + InetAddress.getByName("open-ims.test").getHostAddress());
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				System.out.println("_MDEBUG: failed to DNS resolve - fix DNS issue first!");
				e1.printStackTrace();
			}    		
    		
    		ArrayList viaHeaders = new ArrayList();//getHost
    		ViaHeader viaHeader = headerFactory.createViaHeader(getHostName(),
    			getPort(), "udp", "branch1");
    		viaHeaders.add(viaHeader);
    		
    		CallIdHeader callIdHeader = sipProvider.getNewCallId();

    		CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
    			Request.REGISTER);

    		MaxForwardsHeader maxForwards = headerFactory
    			.createMaxForwardsHeader(70);
    		
    		Request request = messageFactory.createRequest(requestURI,
    			Request.REGISTER, callIdHeader, cSeqHeader, fromHeader,
    			toHeader, viaHeaders, maxForwards);

    		SipURI contactURI = addressFactory.createSipURI(getUsername(),
    			getHost());
    		contactURI.setPort(getPort());
    		Address contactAddress = addressFactory.createAddress(contactURI);
    		contactAddress.setDisplayName(getUsername());
    		ContactHeader contactHeader = headerFactory
    			.createContactHeader(contactAddress);
    		request.addHeader(contactHeader);
    	
    		Authorization auth = new Authorization();
    		auth.setScheme("Digest");
    		auth.setUsername(username + "@" + domain);
    		auth.setRealm(domain);
    		auth.setNonce("");
    		
    		SipUri authURI = new SipUri();
    		authURI.setHost(domain);
    		auth.setURI(authURI);
    		
    		auth.setResponse("");
    		request.addHeader(auth);
    		
    		ExpiresHeader exipreHeader = headerFactory.createExpiresHeader(3600);
    		request.addHeader(exipreHeader);
    		
    		List list = new ArrayList();
    		list.add("AndroidIMHDv1.0");
    		
    		UserAgentHeader uaHeader = headerFactory.createUserAgentHeader(list);
    		request.addHeader(uaHeader);
    		
			sipProvider.sendRequest(request);
    }
       
    public void finishRegister(Response response) throws SipException, InvalidArgumentException, ParseException {
    	
    	System.out.println("_MDEBUG: Processing 2nd part of REGISTRATION.");
    	
    	ListIterator itr = response.getHeaderNames();
    	
    	if(!itr.hasNext())
    		System.out.println("_MDEBUG: Response is w/o headers.");
    	while(itr.hasNext()) {
    		Object item = itr.next();
    		System.out.println("_MDEBUG: Response iterator: " + (String)item);
    	}
    	
    	SipUri requestURI = new SipUri(); //specificky pripad pre REGISTER
		requestURI.setHost(domain);
    	
    	CallIdHeader callIdHeader = (CallIdHeader) response.getHeader(SIPHeaderNames.CALL_ID);
    	
    	System.out.println("Processed Header: Call-Id");
		
    	CSeqHeader cSeqHeader = (CSeqHeader) response.getHeader(SIPHeaderNames.CSEQ);
    	cSeqHeader.setSequenceNumber(2);
    	System.out.println("Processed Header: CSEQ");
    	
    	FromHeader fromHeader = (FromHeader) response.getHeader(SIPHeaderNames.FROM);
    	ToHeader toHeader = (ToHeader) response.getHeader(SIPHeaderNames.TO);
    	System.out.println("Processed Header: FROM & TO");
    	
    	ArrayList viaHeaders = new ArrayList();//getHost
		ViaHeader viaHeader = (ViaHeader) response.getHeader(SIPHeaderNames.VIA);
		viaHeader.removeParameter("received");
		viaHeader.removeParameter("rport");
		viaHeaders.add(viaHeader);
		System.out.println("Processed Header: VIA header=" + viaHeader.toString());
		
		MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
		System.out.println("Processed Header: MaxForwards");
    	
    	Request request = messageFactory.createRequest(requestURI, Request.REGISTER, callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);
    	System.out.println("Request created via messageFactory");
    	
    	SipURI contactURI = addressFactory.createSipURI(getUsername(),
    			getHost());
    		contactURI.setPort(getPort());
    		Address contactAddress = addressFactory.createAddress(contactURI);
    		contactAddress.setDisplayName(getUsername());
    		ContactHeader contactHeader = headerFactory
    			.createContactHeader(contactAddress);
    		request.addHeader(contactHeader);
    	System.out.println("Processed Header: ContactURI");
    		
    	AllowHeader allowHeader = (AllowHeader) response.getHeader(SIPHeaderNames.ALLOW);
    	response.addHeader(allowHeader);
    	System.out.println("Processed Header: AllowHeader");
    	
    	WWWAuthenticateHeader wwwAuthHeader = (WWWAuthenticateHeader) response.getHeader(SIPHeaderNames.WWW_AUTHENTICATE);
    	System.out.println("Processing Header: wwwAuthHeader");
    	
    	System.out.println("_MDEBUG: wwwAuth Scheme=" + wwwAuthHeader.getScheme());
		System.out.println("_MDEBUG: wwwAuth Nonce=" + wwwAuthHeader.getNonce());
		System.out.println("_MDEBUG: wwwAuth: URI=" + wwwAuthHeader.getURI());
		System.out.println("_MDEBUG: wwwAuth: Algorithm=" + wwwAuthHeader.getAlgorithm());
    	
		
    	Authorization authHeader = new Authorization();
    	authHeader.setScheme("Digest");
		authHeader.setUsername(username + "@" + domain);
		authHeader.setRealm(domain);
		authHeader.setNonce(wwwAuthHeader.getNonce());
		
		SipUri authURI = new SipUri();
		authURI.setHost(domain);
		authHeader.setURI(authURI);
		
		String algorithm = wwwAuthHeader.getAlgorithm();
		authHeader.setAlgorithm(algorithm);
		
		System.out.println("Authorization w/o MD5");		
		
		authHeader.setCNonce(Integer.toHexString((new Random().nextInt())));
		authHeader.setNonceCount(1);
		
		String qop = wwwAuthHeader.getQop();
		authHeader.setQop("auth-int");//iba auth-int
		
		try {
			String MD5p1, MD5p2, MD5p3, strNonce = "";
			strNonce = wwwAuthHeader.getNonce();
			
			String hbody = MD5HashingExample.computeDigest("");
			System.out.println("hbody=" + MD5HashingExample.computeDigest(""));
			MD5p1 = MD5HashingExample.computeDigest(getUsername()+"@"+domain+":"+domain+":"+"bob");
			System.out.println("MD5p1");
			MD5p2 = MD5HashingExample.computeDigest("REGISTER:sip:open-ims.test:"+hbody);
			System.out.println("MD5p2");
			System.out.println("uri="+authHeader.getURI().toString()+ " nonce=" + strNonce + " qop=" + "auth-int" + " nc=" + authHeader.getParameter(AuthenticationHeader.NC) + " cnonce=" + authHeader.getCNonce());
			MD5p3 = MD5HashingExample.computeDigest(MD5p1+":"+strNonce+":"+authHeader.getParameter(AuthenticationHeader.NC)+":"+authHeader.getCNonce()+":"+"auth-int"+":"+MD5p2);
			System.out.println("MD5p3=" + MD5p3);
			
			authHeader.setResponse(MD5p3);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.addHeader(authHeader);
		
		System.out.println("Processed Header: wwwAuth & Authorization");

		ExpiresHeader exipreHeader = headerFactory.createExpiresHeader(3600);//nezabudnut spravit TimerTask na refreshovat nie registracie po 3600sek
    		request.addHeader(exipreHeader);
    		
    	List list = new ArrayList();
    	list.add("AndroidIMHDv1.0");
    		
    	UserAgentHeader uaHeader = headerFactory.createUserAgentHeader(list);
    	request.addHeader(uaHeader);
    	
    	System.out.println("Processed Header: Expire & UA");
    	
    	debugCnt++;
    		
    	if (debugCnt > 1)
    		return;
    	
		sipProvider.sendRequest(request);
		System.out.println("_MDEBUG: Finishing REGISTRATION: Final REGISTER message sent!");
    }
    
    /**
     * This method uses the SIP stack to send a message. 
     */
    public void sendMessage(String to, String message) throws ParseException,
	    InvalidArgumentException, SipException {

	SipURI from = addressFactory.createSipURI(getUsername(), getHost()
		+ ":" + getPort());
	Address fromNameAddress = addressFactory.createAddress(from);
	fromNameAddress.setDisplayName(getUsername());
	FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
		"textclientv1.0");

	String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
	String address = to.substring(to.indexOf("@") + 1);

	SipURI toAddress = addressFactory.createSipURI(username, address);
	Address toNameAddress = addressFactory.createAddress(toAddress);
	toNameAddress.setDisplayName(username);
	ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

	SipURI requestURI = addressFactory.createSipURI(username, address);
	requestURI.setTransportParam("udp");

	ArrayList viaHeaders = new ArrayList();
	ViaHeader viaHeader = headerFactory.createViaHeader(getHost(),
		getPort(), "udp", "branch1");
	viaHeaders.add(viaHeader);

	CallIdHeader callIdHeader = sipProvider.getNewCallId();

	CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
		Request.MESSAGE);

	MaxForwardsHeader maxForwards = headerFactory
		.createMaxForwardsHeader(70);
	
	Request request = messageFactory.createRequest(requestURI,
		Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
		toHeader, viaHeaders, maxForwards);

	SipURI contactURI = addressFactory.createSipURI(getUsername(),
		getHost());
	contactURI.setPort(getPort());
	Address contactAddress = addressFactory.createAddress(contactURI);
	contactAddress.setDisplayName(getUsername());
	ContactHeader contactHeader = headerFactory
		.createContactHeader(contactAddress);
	request.addHeader(contactHeader);

	ContentTypeHeader contentTypeHeader = headerFactory
		.createContentTypeHeader("text", "plain");
	request.setContent(message, contentTypeHeader);

	sipProvider.sendRequest(request);
	
	System.out.println("Sprava sa poslala!");
    }

    /** This method is called by the SIP stack when a response arrives. */
    public void processResponse(ResponseEvent evt) {
	Response response = evt.getResponse();
	int status = response.getStatusCode();

	System.out.println("_MDEBUG: Received response with status: " + status);
	
	if ((status >= 200) && (status < 300)) { //Success!
	    messageProcessor.processInfo("--Sent");
	    return;
	}

	if (status == 401) {
		try {
			finishRegister(response);
			return;
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	System.out.println("_MDEBUG: Passed through response w/o action - undetected response!");
	messageProcessor.processError("Previous message not sent: " + status);
    }

    /** 
     * This method is called by the SIP stack when a new request arrives. 
     */
    public void processRequest(RequestEvent evt) {
	Request req = evt.getRequest();
	String method = req.getMethod();
	
	System.out.println("_MDEBUG: Received REQUEST with method: " + method);
	
	if (!method.equals("MESSAGE")) { //bad request type.
	    messageProcessor.processError("Bad request type: " + method);
	    return;
	}

	FromHeader from = (FromHeader) req.getHeader("From");
	messageProcessor.processMessage(from.getAddress().toString(),
		new String(req.getRawContent()));
	
	System.out.println("_MDEBUG: - processRequest:" + from.getAddress().toString() + " " + new String(req.getRawContent()));
	
	Response response = null;
	try { //Reply with OK
	    response = messageFactory.createResponse(200, req);
	    ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
	    toHeader.setTag("888"); //This is mandatory as per the spec.
	    ServerTransaction st = sipProvider.getNewServerTransaction(req);
	    st.sendResponse(response);
	} catch (Throwable e) {
	    e.printStackTrace();
	    messageProcessor.processError("Can't send OK reply.");
	}
    }

    /** 
     * This method is called by the SIP stack when there's no answer 
     * to a message. Note that this is treated differently from an error
     * message. 
     */
    public void processTimeout(TimeoutEvent evt) {
	messageProcessor
		.processError("Previous message not sent: " + "timeout");
    }

    /** 
     * This method is called by the SIP stack when there's an asynchronous
     * message transmission error.  
     */
    public void processIOException(IOExceptionEvent evt) {
	messageProcessor.processError("Previous message not sent: "
		+ "I/O Exception");
    }

    /** 
     * This method is called by the SIP stack when a dialog (session) ends. 
     */
    public void processDialogTerminated(DialogTerminatedEvent evt) {
    }

    /** 
     * This method is called by the SIP stack when a transaction ends. 
     */
    public void processTransactionTerminated(TransactionTerminatedEvent evt) {
    }

    
    /**
     * getter/setter methods
     */
    public String getHost() {
    	int port = sipProvider.getListeningPoint().getPort();
    	String host = sipStack.getIPAddress();
    	return host;
    }

    public String getHostName() {
    	return domain;
    }
    
    public int getPort() {
    	int port = sipProvider.getListeningPoint().getPort();
    	return port;
    }

    public String getUsername() {
    	return username;
    }

    public void setUsername(String newUsername) {
    	username = newUsername;
    }

    public MessageProcessor getMessageProcessor() {
    	return messageProcessor;
    }

    public void setMessageProcessor(MessageProcessor newMessageProcessor) {
    	messageProcessor = newMessageProcessor;
    }

}
