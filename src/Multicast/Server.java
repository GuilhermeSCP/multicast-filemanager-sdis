package Multicast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;

import UI.GUI;

public class Server {

	MulticastSocket ControlSocket;
	MulticastSocket BackupSocket;
	MulticastSocket RetrieveSocket;
	// //////////////////////////////
	Thread ControlThread;
	Thread BackupThread;
	Thread RetrieveThread;
	// //////////////////////////////
	int ControlPort;
	int BackupPort;
	int RetrievePort;
	// //////////////////////////////
	InetAddress ControlAddress;
	InetAddress BackupAddress;
	InetAddress RetrieveAddress;
	// //////////////////////////////
	static byte cr = 0xD;
	static byte lf = 0xA;
	static String crlf = "" + (char) cr + (char) lf;
	static String defaultVersion = "1.0";
	boolean proceed = true;

	public Server(MulticastSocket CS, MulticastSocket BS, MulticastSocket RS,
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

	public void start() {

		ControlThread = new Thread() {
			public void run() {
				try {
					ControlSocket.joinGroup(ControlAddress);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				while (proceed) {
					byte[] received = new byte[64100];
					DatagramPacket packet = new DatagramPacket(received,
							received.length);
					
					try {
						ControlSocket.receive(packet);
						InetAddress from = packet.getAddress();
						GUI.console.append("\nMensagem recebida de " + from + " : " + new String(received));
						System.out.println("\nMensagem recebida de " + from + " : " + new String(received));
						getPacketControl(packet);
					} catch (IOException e) {
						if (!proceed){
							GUI.console.append("\nControlSocket fechado");
							System.out.println("\nControlSocket fechado");
						}
						else
							e.printStackTrace();
					}
				}
			}
		};

		BackupThread = new Thread() {
			public void run() {
				try {
					BackupSocket.joinGroup(BackupAddress);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				while (proceed) {
					byte[] received = new byte[64100];
					DatagramPacket packet = new DatagramPacket(received,received.length);
					try {
						BackupSocket.receive(packet);
						InetAddress from = packet.getAddress();
						GUI.console.append("\nMensagem recebida de " + from + " : " + new String(received));
						System.out.println("\nMensagem recebida de " + from + " : " + new String(received));
						getPacketBackup(packet);
					} catch (IOException e) {
						if (!proceed){
							GUI.console.append("\nBackupSocket fechado");
							System.out.println("\nBackupSocket fechado");
						}
						else
							e.printStackTrace();
					}
				}
			}
		};

		RetrieveThread = new Thread() {
			public void run() {
				try {
					RetrieveSocket.joinGroup(RetrieveAddress);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				while (proceed) {
					byte[] received = new byte[64100];

					DatagramPacket packet = new DatagramPacket(received,received.length);
					try {
						RetrieveSocket.receive(packet);
						InetAddress from = packet.getAddress();
						GUI.console.append("\nMensagem recebida de " + from + " : " + new String(received));
						System.out.println("\nMensagem recebida de " + from + " : " + new String(received));
						getPacketRetrieve(packet);
					} catch (IOException e) {
						if (!proceed) {
							GUI.console.append("\nRetrieveSocket fechado");
							System.out.println("\nRetrieveSocket fechado");
						}
						else
							e.printStackTrace();
					}
				}
			}
		};

		ControlThread.start();
		BackupThread.start();
		RetrieveThread.start();
	}

	protected static void getPacketRetrieve(DatagramPacket packet) throws IOException {

		String msg = new String(packet.getData());
		String newMsg = msg.substring(0, packet.getLength());
		String[] split = newMsg.split(" ");
		if (split[0].equalsIgnoreCase("CHUNK")) {
			chunk(packet.getData(),newMsg);
		} else
			return;
	}

	private static void chunk(byte[] fullMessage, String msg) throws IOException {
		
		String[] split = msg.split(" ");
		int index = 0;
		int chunk = 0;
		//byte body[] = new byte[fullMessage.length - (index + 4)];
		if (split[2].matches("[a-z0-9]{64}"))
			chunk = Integer.parseInt(split[3].substring(0, split[3].indexOf(crlf+crlf)));
		if ((index = split[3].indexOf(crlf + crlf)) != -1) {
			//Integer.parseInt(split[4].substring(0, index));

			int indexBody = msg.indexOf(crlf + crlf);
			byte body[] = new byte[msg.length()-indexBody - 4];
			System.arraycopy( fullMessage, indexBody + 4, body, 0, msg.length()-indexBody - 4 );

			/*for (int i = (indexBody + 4), b = 0; i < fullMessage.length; i++, b++)
				body[b] = fullMessage[i];*/
			
			
			// /////////GUARDAR O CHUNK//////////////////////
			File f = new File("repositorio\\" + split[2] + ".part" + chunk);
			FileOutputStream out = new FileOutputStream(f);
			while (f.length() < body.length)
				out.write(body);
			out.close();
			// /////////////////////////////////////////////
			
			GUI.stored.put(split[2], chunk);
			FileOutputStream fileOut = new FileOutputStream("config");
			ObjectOutputStream oos = new ObjectOutputStream(fileOut);
			oos.writeObject(GUI.stored);
			oos.close();
			fileOut.close();
		}
		
	}

	protected void getPacketBackup(DatagramPacket packet) throws IOException {

		String msg = new String(packet.getData());
		String newMsg = msg.substring(0, packet.getLength());
		System.out.println("\nBytes received: "+newMsg.length());
		String[] split = newMsg.split(" ");
		if (split[0].equalsIgnoreCase("PUTCHUNK")) {
			// System.out.println("PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg><CRLF><CRLF><Body>");
			putchunk(packet.getData(), newMsg);
		} else
			return;

	}

	private void putchunk(byte[] fullMessage, String msg) throws IOException {
		
		
		String[] split = msg.split(" ");
		int index = 0;
		int chunk = 0;
		//byte body[] = new byte[fullMessage.length - (index + 4)];
		
		if (split[2].matches("[a-z0-9]{64}"))
			chunk = Integer.parseInt(split[3]);
		if ((index = split[4].indexOf(crlf + crlf)) != -1) {
			Integer.parseInt(split[4].substring(0, index));

			int indexBody = msg.indexOf(crlf + crlf);
			byte body[] = new byte[msg.length()-indexBody - 4];
			System.arraycopy( fullMessage, indexBody + 4, body, 0, msg.length()-indexBody - 4 );
			
		/*	for (int i = (indexBody + 4), b = 0; i < msg.length(); i++, b++)
				body[b] = fullMessage[i];*/

			// /////////GUARDAR O CHUNK//////////////////////
			File f = new File("repositorio\\" + split[2] + ".part" + chunk);
			FileOutputStream out = new FileOutputStream(f);
			while (f.length() < body.length)
				out.write(body);
			out.close();
			// /////////////////////////////////////////////

			String stored = "STORED " + defaultVersion + " " + split[2] + " "
					+ chunk + crlf + crlf;
			DatagramPacket packet = new DatagramPacket(stored.getBytes(),
					stored.getBytes().length, ControlAddress, ControlPort);
			waitTime();
			ControlSocket.send(packet);
			GUI.console.append("\nMensagem enviada: "+stored);
			System.out.println("\nMensagem enviada: "+stored);
		}
	}

	protected void getPacketControl(DatagramPacket packet) throws IOException {

		String msg = new String(packet.getData());
		String newMsg = msg.substring(0, packet.getLength());
		String[] split = newMsg.split(" ");
		if (split[0].equalsIgnoreCase("GETCHUNK")) {
			getchunk(split[2], split[3]);
		} else if (split[0].equalsIgnoreCase("STORED")) {
			stored(split[2], split[3]);
		} else if (split[0].equalsIgnoreCase("DELETE")) {
			delete(split[1]);
		} else if (split[0].equalsIgnoreCase("REMOVE")) {
			System.out.println("Função não implementada!");
		} else
			return;

	}

	private static void delete(String string) {

		int index;
		String fid = "";

		if ((index = string.indexOf(crlf + crlf)) != -1) {
			fid = string.substring(0, index);
		}

		final String fixe = new String(fid);

		File dir = new File("repositorio\\");
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {

				return name.matches(fixe+".part"+"[0-9]*");
			}
		});

		for (File todelete : files) {
			todelete.delete();
		}
		
		GUI.console.append("\nChunks eliminados.");
		System.out.println("\nChunks eliminados.");
	}

	private void getchunk(String fid, String msg) throws IOException {
		int index = 0;
		if (fid.matches("[a-z0-9]{64}"))
			if ((index = msg.indexOf(crlf + crlf)) != -1) {

				int chunkNo = Integer.parseInt(msg.substring(0, index));
				sendchunk(fid, chunkNo);
			}
	}

	private void sendchunk(final String fid, final int chunkNo)
			throws IOException {
		File dir = new File("repositorio\\");
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(fid + ".part" + chunkNo);
			}
		});

		if (files.length > 0) {
			FileInputStream file = new FileInputStream(files[0]);
			byte[] fileContent = new byte[64000];
			int chunkLength = file.read(fileContent, 0, 64000);
			file.close();
			String message = "CHUNK 1.0 " + fid + " " + chunkNo + crlf + crlf;

			byte[] _message = message.getBytes();
			byte[] fullMessage = new byte[_message.length + chunkLength];

			for (int i = 0; i < _message.length; i++) //passa para o fullMessage o cabeçalho
				fullMessage[i] = _message[i];

			if (chunkLength > 0) {
				byte[] chunkContent = new byte[chunkLength];

				for (int a = 0; a < chunkLength; a++)
					chunkContent[a] = fileContent[a];

				for (int j = _message.length, l = 0; l < chunkLength; j++, l++)
					fullMessage[j] = chunkContent[l];
			}

			DatagramPacket packet = new DatagramPacket(fullMessage,
					fullMessage.length, RetrieveAddress, RetrievePort);
			RetrieveSocket.send(packet);
			GUI.console.append("\nMensagem enviada: "+message);
			System.out.println("\nMensagem enviada: "+message);
		} else {
			GUI.console.append("\nO FileID não corresponde a nenhum chunk armazenado.");
			System.out.println("\nO FileID não corresponde a nenhum chunk armazenado.");
			}
	}

	private static void stored(String split, String split2) throws IOException {

		String fid = split;
		int index=0;
		String cid = "";
		if ((index = split2.indexOf(crlf + crlf)) != -1) {
			cid = split2.substring(0, index);
		}
		int chunkNo = Integer.parseInt(cid);

		GUI.stored.put(fid, chunkNo);
		FileOutputStream fileOut = new FileOutputStream("config");
		ObjectOutputStream oos = new ObjectOutputStream(fileOut);
		oos.writeObject(GUI.stored);
		oos.close();
		fileOut.close();

	}

	public void stopServer() {

		proceed = false;
		ControlSocket.close();
		BackupSocket.close();
		RetrieveSocket.close();
	}

	private static void waitTime() {
		Random randomGenerator = new Random();
		int time = randomGenerator.nextInt(400);
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
