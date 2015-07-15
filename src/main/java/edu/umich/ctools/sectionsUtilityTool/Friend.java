package edu.umich.ctools.sectionsUtilityTool;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.*;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;

import javax.net.ssl.*;
import javax.net.SocketFactory;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;

import edu.umich.its.lti.utils.PropertiesUtilities;

//TODO - Email Templates

public class Friend 
{

	private static Log M_log = LogFactory.getLog(Friend.class);

	//Currently it uses xmlrpc for communication
	public static XmlRpcClient Xclient;

	protected static String friendUrl = null;
	protected static String contactEmail = null;
	protected static String emailMessage = null;
	protected static String referrerUrl = null;
	protected static String ksFileName = null;
	protected static String ksPwd = null;
	protected static String friendEmailFile = null;
	protected static String requesterEmailFile = null;

	public final String KEYSTORETYPE_PKCS12 = "pkcs12";

	public final String TRUSTSTORETYPE_JKS = "jks";

	private static boolean sslInitialized = false;
	
	protected static final String FRIEND_PROPERTY_FILE_PATH_SECURE = "sectionsToolFriendPropsPathSecure";
	
	protected static Properties appExtSecurePropertiesFile=null;
	

	public void init()
	{
		M_log.info(this +".init()");
		
		String propertiesFilePathSecure = System.getProperty(FRIEND_PROPERTY_FILE_PATH_SECURE);
		M_log.info("props: " + propertiesFilePathSecure);
		if (!propertiesFilePathSecure.isEmpty()) {
			appExtSecurePropertiesFile=PropertiesUtilities.getPropertiesObjectFromURL(propertiesFilePathSecure);
			if(appExtSecurePropertiesFile!=null) {
				//PropertiesFile information
				friendUrl = appExtSecurePropertiesFile.getProperty("ctools.friend.url");
				contactEmail = appExtSecurePropertiesFile.getProperty("ctools.friend.contactemail");
				referrerUrl = appExtSecurePropertiesFile.getProperty("ctools.friend.referrer");
				ksFileName = appExtSecurePropertiesFile.getProperty("ctools.friend.ksfilename");
				ksPwd = appExtSecurePropertiesFile.getProperty("ctools.friend.kspassword");
				friendEmailFile = appExtSecurePropertiesFile.getProperty("ctools.friend.friendemail");
				requesterEmailFile = appExtSecurePropertiesFile.getProperty("ctools.friend.requesteremail");
				
				M_log.debug("friendUrl: " + friendUrl);
				M_log.debug("contactEmail: " + contactEmail);
				M_log.debug("referrerUrl: " + referrerUrl);
				M_log.debug("ksFileName: " + ksFileName);
				M_log.debug("ksPwd: " + ksPwd);
			}else {
				M_log.error("Failed to load secure application properties from sectionsToolPropsSecure.properties for SectionsTool");
			}
			
		}else {
			M_log.error("File path for (sectionsToolPropsPathSecure.properties) is not provided");
		}

		Properties systemProps = System.getProperties();
		String keyStoreType = (String) systemProps.get("javax.net.ssl.keyStoreType");
		String trustStoreType = (String) systemProps.get("javax.net.ssl.trustStoreType");
		if (keyStoreType != null && !KEYSTORETYPE_PKCS12.equals(keyStoreType)) // existing keyStoreType 
		{
			M_log.error(this + " init: existing settings of SSL keyStoreType mismatch: " + keyStoreType );
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
				M_log.error(this + " init the ctools.friend.keystorefile path is not defined. ");
			}
			else
			{
				systemProps.put("javax.net.ssl.keyStore",ksFileName);
			}

			if (ksPwd.isEmpty())
			{
				// log error for missing keystore password
				M_log.error(this + " init the ctools.friend.keystorepassword is not defined. ");
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
	public static int checkAccountExist(String email) {
		int rv = 0;	// default to be "no friend account"
		try {
			Xclient = new XmlRpcClient();
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(friendUrl));
			Xclient.setTransportFactory(new XmlRpcSun15HttpTransportFactory(Xclient));
			Xclient.setConfig(config);
			
			Object[] params = new Object[]{new String[] {email}};
			Object[] results = (Object[]) Xclient.execute("doAccountsExist", params);
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
			e.printStackTrace();
		}
		return rv;
	}

	public static String readFile(String path, Charset encoding) throws IOException{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
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
	public static int doSendInvite(String accountEmail, 
			String currentUserEmail, 
			String instructorName) {
		int rv = 0; // default to 
		
		try {
			emailMessage = Friend.readFile(friendEmailFile, StandardCharsets.UTF_8);
			emailMessage = emailMessage.replace("<instructor>", instructorName);
			emailMessage = emailMessage.replace("<contactEmail>", contactEmail);
			
			Xclient = new XmlRpcClient();
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(friendUrl));
			Xclient.setTransportFactory(new XmlRpcSun15HttpTransportFactory(Xclient));
			Xclient.setConfig(config);
			
			Object[] params = new Object[]{contactEmail, referrerUrl, emailMessage, new String[]{accountEmail}, currentUserEmail};
			Object[] results = (Object[]) Xclient.execute("sendInvites", params);
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

		return rv;
	}    

	public static void notifyCurrentUser(String instructorName, String instructorEmail, String inviteEmail){

		String to = instructorEmail;
		String from = contactEmail;
		String subjectLine = "Friend Accounts Integration Email";
		//This will need to be changed to properties
		String host = "deluxe.dsc.umich.edu";

		System.out.println("Setting up props");
		Properties properties = System.getProperties();
		properties.put("mail.smtp.auth", "false");
		properties.put("mail.smtp.starttls.enable", "true"); //Put below to false, if no https is needed
		properties.put("mail.smtp.host", host);
		properties.put("mail.debug", "true");

		System.out.println("Initiating Session");
		Session session = Session.getInstance(properties);

		try{
			
			emailMessage = Friend.readFile(requesterEmailFile, StandardCharsets.UTF_8);
			emailMessage = emailMessage.replace("<instructor>", instructorName);
			emailMessage = emailMessage.replace("<friend>", inviteEmail);
			
			System.out.println("Setting up message");
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(subjectLine);
			
			message.setText(emailMessage);

			System.out.println("Sending message");
			Transport.send(message);

			System.out.println("Message sent to " + instructorName);

		}catch (Exception e){
			System.out.println("quasiNotifyCurrentUser exception:");
			e.printStackTrace();
		}
	}
	
}
