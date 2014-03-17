import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemReader;


//generate keypair with openssl http://stackoverflow.com/questions/5244129/openssl-use-rsa-private-key-to-generate-public-key
//create pair :openssl rsa -in project.pem -out public.pem -outform PEM
//extract public key: openssl rsa -in mykey.pem -pubout > mykey.pub

public class CryptoHelper {
	/**
	 * Creates a signed object that contains a scheme program and initial data
	 * @param outputPath this is the filepath to write the signed object to
	 * @param schemePath the location of the scheme file to include in the signed object
	 * @param initialData any data that needs to be passed to the scheme program
	 * @param privateKeyPath the location of the private key
	 */
	public static void CreateSignedObject(String outputPath, String schemePath, String initialData, String privateKeyPath) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException
	{
		//create serializable CodeObject
		CodeObject theObject = new CodeObject();
		theObject.initialData = initialData;
		
		//store scheme file in byte array
		File file = new File(schemePath);		 
	    FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        
        for (int readNum; (readNum = fis.read(buf)) != -1;) {
            bos.write(buf, 0, readNum); //no doubt here is 0
            System.out.println("read " + readNum + " bytes,");
        }
           
		theObject.scheme = bos.toByteArray();
		
		//read private key from file using bouncy castle pemreader
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PemReader reader = new PemReader( new FileReader( privateKeyPath ) );
        final byte[] prKey = reader.readPemObject(  ).getContent(  );
        final PKCS8EncodedKeySpec pkcs8Spec = new PKCS8EncodedKeySpec(prKey);
        PrivateKey privateKey = keyFactory.generatePrivate( pkcs8Spec );
        
        //create signed object
        final Signature sig = Signature.getInstance( "SHA1withRSA");
        SignedObject sigObject =  new SignedObject(theObject, privateKey, sig);
        
        //write signed object to a file
        FileOutputStream fileOut = new FileOutputStream(outputPath);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(sigObject);
        out.close();
        fileOut.close();
        
	}
	
	/**
	 * Verifies the signature of a signed object 
	 * @param publicKeyPath the location of the public key
	 * @param sigObject the signed object to be verified with the supplied public key
	 */
	public static boolean VerifySignedObject(String publicKeyPath, SignedObject sigObject) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException 
	{
		//read public key from file using bouncy castle pemreader
		FileReader fr = new FileReader(publicKeyPath);     
    	final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PemReader reader = new PemReader( new FileReader( publicKeyPath ) );
        final byte[] pKey = reader.readPemObject(  ).getContent(  );
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(pKey);
        PublicKey publicKey = keyFactory.generatePublic( spec );
        
        //verify the signed object
		final Signature sig = Signature.getInstance( "SHA1withRSA");              
		return sigObject.verify(publicKey, sig);
	}
	
	/**
	 * Deserializes the object contained in the signed object
	 * @param sigObject the signed object
	 */
	public static CodeObject DeserializeObject(SignedObject sigObject) throws IOException, ClassNotFoundException
	{
		CodeObject theObject = (CodeObject) sigObject.getObject();
		return theObject;
	}
	
	//previous method before deciding to use SignedObject
	//create file checksum
	//code from http://www.sha1-online.com/sha1-java/
	public static String CreateChecksum(String filePath) throws NoSuchAlgorithmException, IOException
	{
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        FileInputStream fis = new FileInputStream(filePath);
  
        byte[] data = new byte[1024];
        int read = 0;
        while ((read = fis.read(data)) != -1) {
            sha1.update(data, 0, read);
        };
        
        fis.close();
        
        byte[] hashBytes = sha1.digest();
  
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hashBytes.length; i++) {
          sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        String fileHash = sb.toString();
         
		return fileHash;
	}
	
	
	//previous method before deciding to use SignedObject
	//sign checksum with private key
	//http://stackoverflow.com/questions/11691462/verifying-a-signature-with-a-public-key
	public static byte[] SignChecksum (String checksum, String privateKeyPath) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException, NoSuchProviderException
	{
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PemReader reader = new PemReader( new FileReader( privateKeyPath ) );
        final byte[] prKey = reader.readPemObject(  ).getContent(  );
        final PKCS8EncodedKeySpec pkcs8Spec = new PKCS8EncodedKeySpec(prKey);
        PrivateKey privateKey = keyFactory.generatePrivate( pkcs8Spec );
        
        final Signature sig = Signature.getInstance( "SHA1withRSA");
        sig.initSign(privateKey);
        sig.update(checksum.getBytes("UTF-8"));
        byte[] signedData = sig.sign();

        return signedData;
	}
		
	//previous method before deciding to use SignedObject
	//decrypt checksum with public key
	//http://stackoverflow.com/questions/11691462/verifying-a-signature-with-a-public-key
	public static boolean CheckChecksum (String message, byte[] sign, String publicKeyPath) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchProviderException 
	{
		System.out.println(message);
		System.out.println(sign.toString());
		FileReader fr = new FileReader(publicKeyPath);
        try
        {
        	final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PemReader reader = new PemReader( new FileReader( publicKeyPath ) );
            final byte[] pKey = reader.readPemObject(  ).getContent(  );
            final X509EncodedKeySpec spec = new X509EncodedKeySpec(pKey);
            PublicKey publicKey = keyFactory.generatePublic( spec );
            
    		final Signature sig = Signature.getInstance( "SHA1withRSA");
            sig.initVerify( publicKey );
            sig.update( message.getBytes( "UTF-8"));
            return sig.verify(sign);   
             
        }
        catch(Exception ex){
        	return false;
		}
        
	}
	
}
