package Multicast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.util.Vector;

import UI.GUI;

import FileManager.FileSplitter;
import FileManager.Chunk;

public class Client {

	MulticastSocket ControlSocket;
	MulticastSocket BackupSocket;
	MulticastSocket RetrieveSocket;
	// //////////////////////////////
	Thread ControlThread;
	Thread BackupThread;
	Thread RetrieveThread;
	// /////////////////////////////
	int ControlPort;
	int BackupPort;
	int RetrievePort;
	// ////////////////////////////
	InetAddress ControlAddress;
	InetAddress BackupAddress;
	InetAddress RetrieveAddress;
	// ///////////////////////////
	byte cr = 0xD;
	byte lf = 0xA;
	String crlf = "" + (char) cr + (char) lf;
	boolean proceed = true;
	String defaultVersion = "1.0";

	public Client(MulticastSocket CS, MulticastSocket BS, MulticastSocket RS,
			InetAddress CA, InetAddress BA, InetAddress RA, int CP, int BP, int RP) {
		ControlSocket = CS;
		BackupSocket = BS;
		RetrieveSocket = RS;
		ControlAddress = CA;
		BackupAddress = BA;
		RetrieveAddress = RA;
		ControlPort = CP;
		BackupPort = BP;
		RetrievePort = RP;

	}

	public void sendFile(File f, int rd) throws IOException,
			InterruptedException {

		FileSplitter splitter = new FileSplitter(f);
		int fileparts = splitter.getFileParts();
		splitter.split();

		Vector<Chunk> chunks = splitter.getChunks();

		for (int i = 0; i < fileparts; i++) {
			int count = 0;
			int lombo=-1;
			do {
				
				putChunk(chunks.elementAt(i).getChunkFileId(), i, rd, chunks.elementAt(i).getChunkData());
				count++;
				Thread.sleep(400);
				if(GUI.getStored().containsKey(chunks.elementAt(i).getChunkFileId()))
					lombo = GUI.getStored().get(chunks.elementAt(i).getChunkFileId());
				
			} while (count < 5	&& lombo != i);
			if(count>=5)
				return;
		}
		GUI.backup.add(splitter.getEncryptedName());
		FileOutputStream fileOut = new FileOutputStream("backup");
		ObjectOutputStream oos = new ObjectOutputStream(fileOut);
		oos.writeObject(GUI.backup);
		oos.close();
		fileOut.close();
		
	}

	public void putChunk(String fid, int cid, int repdeg, File data)
			throws IOException {

		FileInputStream fis = new FileInputStream(data);
		byte[] buffer = new byte[(int) data.length()];
		fis.read(buffer);
		fis.close();
		String body = new String(buffer);
		String msg = "PUTCHUNK " + defaultVersion + " " + fid + " "
				+ String.valueOf(cid) + " " + repdeg + crlf + crlf
				+ body;
		DatagramPacket packet = new DatagramPacket(msg.getBytes(),
				msg.getBytes().length, BackupAddress, BackupPort);
		BackupSocket.send(packet);
		GUI.console.append("\nMensagem enviada: " + msg);
		System.out.println("\nMensagem enviada: " + msg);
	}

	public void getChunk(String fid, int cid) throws IOException {

		String msg = "GETCHUNK " + defaultVersion + " " + fid + " "
				+ String.valueOf(cid) + crlf + crlf;
		DatagramPacket packet = new DatagramPacket(msg.getBytes(),
				msg.getBytes().length, ControlAddress, ControlPort);
		ControlSocket.send(packet);
		GUI.console.append("\nMensagem enviada: "+msg);
		System.out.println("\nMensagem enviada: "+msg);
	}

	public void getFile(File f) throws IOException, InterruptedException {

		int tries = 0;

		String filename = f.getName().substring(0, f.getName().lastIndexOf(".part"));
		
		boolean last = false;
		int i = 0;
		int notrespond=0;

		while (!last) {// while() para fazer enquanto o chunk for == a 64000
			
			//getChunk(FileSplitter.encryptFilename(filename), i);
			getChunk(filename, i);
			Thread.sleep(400);
			
			if (GUI.stored.containsKey(filename)) {
				do {
					if (tries > 5)
						return;
					waitTime();
					//getChunk(FileSplitter.encryptFilename(filename), i);
					getChunk(filename, i);
					tries++;
				} while (!(GUI.stored.get(filename) == i));
				File check = new File("repositorio\\" + filename + ".part" + i);
				if (check.exists() && check.length() < 64000)
					last = true;
				i++;
				notrespond=0;
			}
			notrespond++;
			if(notrespond>5)
				return;
		}
		FileSplitter splitter = new FileSplitter(f);
		splitter.unsplit(filename);
		GUI.console.append("\nFicheiro recuperado.");
		System.out.println("\nFicheiro recuperado.");

	}

	public void delete(String filename) throws IOException, InterruptedException {

		String fid = new String(filename.substring(0, filename.indexOf(".part")));
		String msg = "DELETE " + fid + crlf + crlf;
		DatagramPacket packet = new DatagramPacket(msg.getBytes(),
				msg.getBytes().length, ControlAddress, ControlPort);
		ControlSocket.send(packet);
		GUI.console.append("\nMensagem enviada: "+msg);
		System.out.println("\nMensagem enviada: "+msg);

		for (int i = 0; i < 5; i++) {

			Thread.sleep(1000);
			ControlSocket.send(packet);
		}

	}

	private void waitTime() {
		Random randomGenerator = new Random();
		int time = randomGenerator.nextInt(400);
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}
