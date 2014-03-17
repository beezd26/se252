import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Vector;

import jscheme.JScheme;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class Main {
	
	private static String sourceDirectory = "/home/david/blairdesktop";///home/david/prog1
	private static String sourceProg2Directory = "/home/alice/prog2";///home/david/prog2
	private static String localDirectory = "C:\\Users\\michalk\\Documents\\progsToRun";
	private static String localOutDirectory = "C:\\Users\\michalk\\Documents\\progsToSend";
	private static String machineName = "blairdesktop";

	public static void main(String[] args) throws IOException, InterruptedException {
		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		while(true){
		//Let user make selection
			
			System.out.println("Press 1 to send a program and 2 to check for programs");	
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			int selection = 0;
			
			try {
				String r = br.readLine();
				selection = Integer.parseInt(r);
			} catch (Exception ex) {
				System.out.println("Please enter a correct selection");
			}
			
			if(selection == 1)
			{
				System.out.println("Enter path to program file to send");
				String fileName = null;
				String receivingMachineName = null;
				String initialData = null;
				try {
					fileName = br.readLine();
					System.out.println("Enter name of machine to send to");
					receivingMachineName = br.readLine();
					System.out.println("Enter initial date");
					initialData = br.readLine();
					sendFiles(fileName, receivingMachineName, initialData);
					
				} catch (Exception ex) {
					System.out.println("Please enter correct data");
				}			
				
			}
			else if(selection == 2)
			{
				System.out.println("Checking for programs...");
			}
			else
			{
				System.out.println("Please enter a correct selection");
			}
			
			//pick up file from server
			/*while(!pickupFile()){
				Thread.sleep(10000);//sleep for 10 seconds if file not found
			}
			runSchemeProgram();
			while(!sendFiles()){
				Thread.sleep(10000);//sleep for 10 seconds if couldn't connect
			}*/
		}
	}
	
	public static boolean pickupFile(){
		boolean copiedFile = false;
		PublicKeyTarget target = new PublicKeyTarget();//target server
		try {
			final JSch jsch = new JSch ();
			JSch.setConfig ("StrictHostKeyChecking", "no");
			System.out.println ("Creating session");
			final Session session = target.getSession (jsch);//getting connection to target server
			session.connect ();//opening connection
			
			System.out.println ("Creating SFTP channel");
			//opening sftp channel to targer server
			final ChannelSftp channel = (ChannelSftp) session.openChannel ("sftp");
			channel.connect ();
			
			Vector files = channel.ls(sourceDirectory);
			ArrayList<String> filesFound = new ArrayList<String>();
			for (Object f : files) {
				if (f instanceof com.jcraft.jsch.ChannelSftp.LsEntry) {
					String filename = ((com.jcraft.jsch.ChannelSftp.LsEntry) f).getFilename();
					if(!filename.equals(".") && !filename.equals("..")){
						filesFound.add(filename);
					}
				}
				System.out.println(f.toString());
			}
			
			//creating a input stream to write the file
			//ByteArrayInputStream bais = new ByteArrayInputStream (fileContent.getBytes ("us-ascii"));
			int mode = ChannelSftp.OVERWRITE;
			
			if(filesFound.size() > 0){
				//get files
				for(int i1 = 0; i1 < filesFound.size(); i1++){
					channel.get(sourceDirectory + "/" + filesFound.get(i1), localDirectory);
					channel.rm(sourceDirectory + "/" + filesFound.get(i1));//remove copied files
					copiedFile = true;
				}
			}
			
			//channel.put (bais, fileName, mode);//writing the file on alice's directory
			
			//cleanup and close
			channel.disconnect ();
			session.disconnect ();
		} catch (Exception e) {
			System.err.println (e.toString ());
			return false;
		}
		return copiedFile;
	}
	

	public static void runSchemeProgram() throws IOException{
		File folder = new File(localDirectory);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("File " + listOfFiles[i].getName());
				BufferedReader reader = new BufferedReader(new FileReader(localDirectory + "\\" + listOfFiles[i].getName()));
				
				StringBuilder sb = new StringBuilder(); 
				String nextLine = reader.readLine();
				while(nextLine != null){
					sb.append(nextLine);
					nextLine = reader.readLine();
				}
				reader.close();
				
				//run jscheme app
				JScheme scheme = new  JScheme();
				scheme.load(sb.toString());
				
				BufferedWriter writer = new BufferedWriter(new FileWriter(localOutDirectory + "\\" + listOfFiles[i].getName()));
				scheme.call("main", writer);

				File f = new File(localDirectory + "\\" + listOfFiles[i].getName());
				f.delete();
			} 
	    }
	}
	

	public static boolean sendFiles(String fileName, String receivingMachineName, String initialData) throws IOException{
		//create code object and sign file
		String[] parts = fileName.split("/");
		String program = parts[parts.length - 1];
		String[] programParts = program.split("\\.");
		String serializedObjectName = programParts[0] + "-" + machineName + ".ser";
		
		try {
			CryptoHelper.CreateSignedObject(serializedObjectName, fileName, initialData, "src/project.pem");
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		
		PublicKeyTarget target = new PublicKeyTarget();
		final JSch jsch = new JSch ();
		JSch.setConfig ("StrictHostKeyChecking", "no");
		System.out.println ("Creating session");
		Session session;
		try {
			session = target.getSession (jsch);
			//getting connection to target server
			session.connect ();//opening connection
			
			System.out.println ("Creating SFTP channel");
			//opening sftp channel to targer server
			final ChannelSftp channel = (ChannelSftp) session.openChannel ("sftp");
			channel.connect ();		
			channel.put(serializedObjectName, channel.pwd () + "/" + receivingMachineName);
			System.out.println("File successfully uploaded");
			//cleanup and close
			channel.disconnect ();
			session.disconnect ();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;//couldn't connect
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

}

class PublicKeyTarget  {
	String hostname = "71.194.192.195";//host server//71.194.192.195
	String username = "sshgroupuser";
	int port = 22;//port to connect on
	String privateKeyFileName = "src/ssh_rsa";
			
	Session getSession (JSch jsch) throws JSchException {//function for getting connection to the remote server
		System.out.format ("connecting to %s@%s:%d%n", username, hostname, port);
	    jsch.addIdentity (privateKeyFileName); 
	    Session session = jsch.getSession (username, hostname, port);
	    return session;
	}
}
