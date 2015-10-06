package edu.umich.ctools.sectionsUtilityTool;

import java.util.Iterator;
import java.util.Properties;
import java.util.HashMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;

public class Friend 
{
	private static Log M_log = LogFactory.getLog(Friend.class);

	private static boolean sslInitialized = false;

	protected static String friendUrl = null;
	protected static String contactEmail = null;
	protected static String emailMessage = null;
	protected static String referrerUrl = null;
	protected static String ksFileName = null;
	protected static String ksPwd = null;
	protected static String friendEmailFile = null;
	protected static String requesterEmailFile = null;
	protected static String mailHost = null;
	protected static String subjectLine = null;

	protected static final String KEYSTORETYPE_PKCS12 = "pkcs12";
	protected static final String TRUSTSTORETYPE_JKS = "jks";

	protected static final String DO_ACCOUNT_EXIST_WS = "doAccountsExist";
	protected static final String SEND_INVITES_WS = "sendInvites";

	protected static final String INSTRUCTOR_NAME_TAG = "<instructor>";
	protected static final String CONTACT_EMAIL_TAG = "<contactEmail>";

	protected static final String FRIEND_PROPERTY_FILE_PATH = "sectionsToolFriendPropsPath";
	protected static final String FRIEND_PROPERTY_FILE_PATH_SECURE = "sectionsToolFriendPropsPathSecure";

	protected static final String FRIEND_URL = "umich.friend.url";
	protected static final String FRIEND_CONTACT_EMAIL = "umich.friend.contactemail"; 
	protected static final String FRIEND_REFERRER = "umich.friend.referrer";
	protected static final String FRIEND_FRIEND_EMAIL = "umich.friend.friendemail";
	protected static final String FRIEND_REQUESTER_EMAIL = "umich.friend.requesteremail";
	protected static final String FRIEND_MAIL_HOST = "umich.friend.mailhost";
	protected static final String FRIEND_SUBJECT_LINE = "umich.friend.subjectline";
	protected static final String FRIEND_KS_FILENAME = "umich.friend.ksfilename";
	protected static final String FRIEND_KS_PASSWORD = "umich.friend.kspassword";

	protected static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
	protected static final String MAIL_SMTP_STARTTLS = "mail.smtp.starttls.enable";
	protected static final String MAIL_SMTP_HOST = "mail.smtp.host";
	protected static final String MAIL_DEBUG = "mail.debug";

	private final static String CCM_PROPERTY_FILE_PATH = "ccmPropsPath";
	private final static String CCM_SECURE_PROPERTY_FILE_PATH = "ccmPropsPathSecure";	

	protected static Properties appExtSecureProperties=null;
	protected static Properties appExtProperties=null;	

	private XmlRpcClient Xclient;

	public Friend() throws MalformedURLException {
		super();
		M_log.debug("Friend constructor Called");
		appExtProperties = Utils.loadProperties(CCM_PROPERTY_FILE_PATH);
		appExtSecureProperties = Utils.loadProperties(CCM_SECURE_PROPERTY_FILE_PATH);	

		setProperties();

		Xclient = new XmlRpcClient();
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(friendUrl));
		Xclient.setTransportFactory(new XmlRpcSun15HttpTransportFactory(Xclient));
		Xclient.setConfig(config);
	}

	public void setProperties(){
		if(appExtSecureProperties!=null) {
			//PropertiesFile information
			friendUrl = appExtProperties.getProperty(FRIEND_URL);
			contactEmail = appExtProperties.getProperty(FRIEND_CONTACT_EMAIL);
			referrerUrl = appExtProperties.getProperty(FRIEND_REFERRER);
			friendEmailFile = appExtProperties.getProperty(FRIEND_FRIEND_EMAIL);
			requesterEmailFile = appExtProperties.getProperty(FRIEND_REQUESTER_EMAIL);
			mailHost = appExtProperties.getProperty(FRIEND_MAIL_HOST);
			subjectLine = appExtProperties.getProperty(FRIEND_SUBJECT_LINE);
			ksFileName = appExtSecureProperties.getProperty(FRIEND_KS_FILENAME);
			ksPwd = appExtSecureProperties.getProperty(FRIEND_KS_PASSWORD);

			M_log.debug("ksFileName: " + ksFileName);
			M_log.debug("ksPwd: " + ksPwd);
			M_log.debug("friendUrl: " + friendUrl);
			M_log.debug("contactEmail: " + contactEmail);
			M_log.debug("referrerUrl: " + referrerUrl);
		}else {
			M_log.error("Failed to load Friend application properties from sectionsToolFriend.properties for SectionsTool");
		}

		//Setting up properties for keyStore
		Properties systemProps = System.getProperties();
		String keyStoreType = (String) systemProps.get("javax.net.ssl.keyStoreType");
		String trustStoreType = (String) systemProps.get("javax.net.ssl.trustStoreType");
		if (keyStoreType != null && !KEYSTORETYPE_PKCS12.equals(keyStoreType)) // existing keyStoreType 
		{
			M_log.error(this + " setProperties: existing settings of SSL keyStoreType mismatch: " + keyStoreType );
			sslInitialized = false;
		}
		else if (trustStoreType != null  && !TRUSTSTORETYPE_JKS.equals(trustStoreType)) // existing trustStoreType
		{
			M_log.error(this + " init: existing settings of SSL trustStoretype mismatch: " + trustStoreType );
			sslInitialized = false;
		}
		else
		{	
			// key store
			systemProps.put("javax.net.ssl.keyStoreType", KEYSTORETYPE_PKCS12);
			systemProps.put("javax.net.ssl.trustStoreType", TRUSTSTORETYPE_JKS);

			if (ksFileName.isEmpty())
			{
				// log error for missing keystore file path
				M_log.error(this + " init the umich.friend.keystorefile path is not defined. ");
			}
			else
			{
				systemProps.put("javax.net.ssl.keyStore",ksFileName);
			}

			if (ksPwd.isEmpty())
			{
				// log error for missing keystore password
				M_log.error(this + " init the umich.friend.keystorepassword is not defined. ");
			}
			else
			{
				systemProps.put("javax.net.ssl.keyStorePassword",ksPwd);
			}

			// set system properties
			System.setProperties(systemProps);

			sslInitialized = true;
		}		
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info(this +".destroy()");

		if (sslInitialized)
		{
			// remove ssl system settings
			// configure JSSE system properties: 
			// http://fusesource.com/docs/broker/5.3/security/SSL-SysProps.html
			Properties systemProps = System.getProperties();
			//systemProps.remove("javax.net.debug");
			//important: http://stackoverflow.com/questions/6680416/apache-cxf-exception-in-ssl-communication-sockettimeout
			//java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
			systemProps.remove("javax.net.ssl.keyStoreType");
			systemProps.remove("javax.net.ssl.trustStoreType");
			systemProps.remove("javax.net.ssl.keyStore");
			systemProps.remove("javax.net.ssl.keyStorePassword");
			System.setProperties(systemProps);

			sslInitialized = false;
		}
	}	

	/*
	 * Check if an account exists
	 * Return a single integer with the following (It all succeeds of fails)
	 * @param email friend account Email
	 * @return
	 * -1 = bad e-mail address
	 * 0 = no friend account
	 * 1 = friend account exists 
	 *
	 */
	public CheckAccountExistsResponse checkAccountExist(String email) {
		M_log.debug("Friend checkAccountExists() called");
		CheckAccountExistsResponse response = CheckAccountExistsResponse.FRIEND_ACCOUNT_DOES_NOT_EXIST;
		int rv = 0;	// default to be "no friend account"
		try {

			Object[] params = new Object[]{new String[] {email}};
			Object[] results = (Object[]) Xclient.execute(DO_ACCOUNT_EXIST_WS, params);
			// though the friend XML-RPC service supports batch call mode, 
			// due to the ctools event model ( one "user added event" per user), 
			// we will pass only one email address to the doAccountsExit call for now
			// hence we should only get one call result returned
			if (results != null && results.length == 1)
			{
				rv = ((Integer) results[0]).intValue();
			}
		}
		catch (Exception e) {
			M_log.warn("Friend checkAccountExist(): email address " + email + " " + e.getMessage());
		}
		if(rv == -1){
			response = CheckAccountExistsResponse.INVALID_EMAIL;
		}
		if(rv == 0){
			response = CheckAccountExistsResponse.FRIEND_ACCOUNT_DOES_NOT_EXIST;
		}
		if(rv == 1){
			response = CheckAccountExistsResponse.FRIEND_ACCOUNT_ALREADY_EXISTS;
		}
		return response;
	}

	public static String readFile(String path, Charset encoding) throws IOException{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static String replacePlaceHolders(String message, HashMap<String,String> map){

		Iterator<String> keySetIterator = map.keySet().iterator();

		while(keySetIterator.hasNext()){ 
			String key = keySetIterator.next(); 
			message = message.replace(key, map.get(key)); 
		}

		return message;
	}


	/*
	 * Actually sends out an invite
	 *
	 * @param accountEmail friend account to be invited
	 * @param currentUser the person doing the inviting
	 * 
	 * @param associated site id
	 * @return
	 * -1 = bad e-mail address
	 * 0 = runtime problem
	 * 1 = successfully sent invitation 
	 */

	//accountEmail == userToBeInvited
	//currentUserEmail == instructorEmail
	public CreateAccountResponse doSendInvite(String accountEmail, 
			String currentUserEmail, 
			String instructorName) {
		M_log.debug("Friend doSendInvite() called");
		CreateAccountResponse response = CreateAccountResponse.RUNTIME_PROBLEM;
		int rv = 0; // default to be "runtime error"

		try {

			HashMap<String, String> map = new HashMap<String,String>();

			map.put(INSTRUCTOR_NAME_TAG, instructorName);
			map.put(CONTACT_EMAIL_TAG, contactEmail);

			emailMessage = Friend.readFile(friendEmailFile, StandardCharsets.UTF_8);
			emailMessage = replacePlaceHolders(emailMessage, map);

			Object[] params = new Object[]{contactEmail, referrerUrl, emailMessage, new String[]{accountEmail}, currentUserEmail};
			Object[] results = (Object[]) Xclient.execute(SEND_INVITES_WS, params);
			// though the friend XML-RPC service supports batch call mode, 
			// due to the ctools event model ( one "user added event" per user), 
			// we will pass only one email address to the "sendInvites" call for now
			// hence we should only get one call result returned
			if (results != null && results.length == 1)
			{
				rv = ((Integer) results[0]).intValue();
			}
		}
		catch (Exception e) {
			M_log.warn("Friend doSendInvite(): email address=" + accountEmail + " " + e.getMessage());
		}
		if(rv == -1){
			response = CreateAccountResponse.INVALID_EMAIL;
		}
		if(rv == 0){
			response = CreateAccountResponse.RUNTIME_PROBLEM;
		}
		if(rv == 1){
			response = CreateAccountResponse.INVITATION_SUCCESSFULLY_SENT;
		}
		return response;
	}    

	public static void notifyCurrentUser(String instructorName, String instructorEmail, String inviteEmail){

		M_log.debug("Friend notifyCurrentUser() called");
		String to = instructorEmail;
		String from = contactEmail;
		String host = mailHost;

		M_log.info("Setting up mailProps");

		Properties properties = System.getProperties();
		properties.put(MAIL_SMTP_AUTH, "false");
		properties.put(MAIL_SMTP_STARTTLS, "true"); //Put to false, if no https is needed
		properties.put(MAIL_SMTP_HOST, host);
		properties.put(MAIL_DEBUG, "true");

		M_log.debug("Initiating Session for sendMail");
		Session session = Session.getInstance(properties);

		try{

			HashMap<String, String> map = new HashMap<String,String>();

			map.put("<instructor>", instructorName);
			map.put("<friend>", inviteEmail);

			emailMessage = Friend.readFile(requesterEmailFile, StandardCharsets.UTF_8);
			emailMessage = replacePlaceHolders(emailMessage, map);

			M_log.debug("Setting up message for sendMail");
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(subjectLine);

			message.setText(emailMessage);

			M_log.info("Sending message");
			Transport.send(message);

			M_log.info("Message sent to " + instructorName);

		}catch (Exception e){
			M_log.error("notifyCurrentUser exception: " + e.getMessage());
		}
	}

}
