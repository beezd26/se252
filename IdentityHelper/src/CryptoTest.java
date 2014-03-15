import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;

import jscheme.JScheme;


public class CryptoTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		// TODO Auto-generated method stub
		String hash;
		/*byte[] signature; 
		try
		{
			//pass file to create checksum
			hash = CryptoHelper.CreateChecksum("src/task2.scm");
			
			//sign checksum pass hash and path to private key
			signature = CryptoHelper.SignChecksum(hash, "src/project.pem");
			
			//verify a signed checksum pass hash, byte[] or signed checksum, public key
			boolean isVerified = CryptoHelper.CheckChecksum(hash, signature, "src/project_public.pem");
			System.out.println(isVerified);
		}catch(Exception ex){
			System.out.println(ex.getMessage());
		}*/
		
		try {
			CryptoHelper.CreateSignedObject("object.ser", "src/task2.scm", "a text string", "src/project.pem");
			
			//read the signed object
			FileInputStream fileIn = new FileInputStream("object.ser");
	        ObjectInputStream in = new ObjectInputStream(fileIn);
	        SignedObject sigObject = (SignedObject) in.readObject();
	        in.close();
	        fileIn.close();
	        
	        //verify the signature of the object
			boolean verified = CryptoHelper.VerifySignedObject("src/project_public.pem", sigObject);
			System.out.println(verified);
			
			//if verified signature then deserialize the object
			if(verified)
			{
				CodeObject theObject = CryptoHelper.DeserializeObject(sigObject);
				final JScheme js = new JScheme ();
				
				//load scheme code
				ByteArrayInputStream bArray = new ByteArrayInputStream(theObject.scheme);				
				BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(bArray));			
				Object result = js.load (reader);
				//run scheme method
				js.call ("displaytext", theObject.initialData);
			}else
			{
				System.out.println("Signature cannot be verified");
			}
			
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

}
