
public class CryptoTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		// TODO Auto-generated method stub
		String hash;
		byte[] signature; 
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
		}		
	}

}
