package UI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import Multicast.Client;
import Multicast.Server;

@SuppressWarnings("serial")
public class GUI extends JFrame {

	static Client client;
	static Server server;
	private JPanel panel;
	public static JTextArea console;
	public static Hashtable<String,Integer> stored = new Hashtable<>();
	public static Vector<String> backup = new Vector<String>();

	public static void main(String args[]) throws Exception {

		int ControlPort = 8765;
		int BackupPort = 8766;
		int RetrievePort = 8767;
		////////////////////////////////
		MulticastSocket ControlSocket=new MulticastSocket(ControlPort);
		ControlSocket.setLoopbackMode(true);
		MulticastSocket BackupSocket=new MulticastSocket(BackupPort);
		BackupSocket.setLoopbackMode(true);
		MulticastSocket RetrieveSocket=new MulticastSocket(RetrievePort);
		RetrieveSocket.setLoopbackMode(true);
		////////////////////////////////
		InetAddress ControlAddress=InetAddress.getByName("239.0.0.1");
		InetAddress BackupAddress=InetAddress.getByName("239.0.0.2");
		InetAddress RetrieveAddress=InetAddress.getByName("239.0.0.3");
		client = new Client(ControlSocket,BackupSocket,RetrieveSocket,ControlAddress,BackupAddress,RetrieveAddress,ControlPort,BackupPort,RetrievePort);
		server = new Server(ControlSocket,BackupSocket,RetrieveSocket,ControlAddress,BackupAddress,RetrieveAddress,ControlPort,BackupPort,RetrievePort);	
		server.start();
		
		File config = new File("config");
		if(config.exists()){
//		FileInputStream fileIn = new FileInputStream("config");
//		ObjectInputStream in = new ObjectInputStream(fileIn);
//		stored = (Hashtable<String,Integer>) in.readObject();
//		in.close();
//		fileIn.close();
		}
		File bak = new File("backup");
		if (bak.exists()) {
			FileInputStream fis = new FileInputStream("backup");
			ObjectInputStream ois = new ObjectInputStream(fis);
			backup = (Vector<String>) ois.readObject();
			ois.close();
			fis.close();
			for (int k = 0; k < backup.size(); k++) {
				File f = new File("repositorio\\"+backup.elementAt(k)+".part0");
				if (!(f.exists()))
					f.createNewFile();
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				GUI gui = new GUI();
				gui.setVisible(true);
			}
		});
	}

	public GUI() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		initGUI();

	}

	public final void initGUI() {
		this.setSize(100, 100);
		console = new JTextArea();

		JPanel top_panel = new JPanel();
		getContentPane().add(top_panel, BorderLayout.NORTH);
		top_panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		final JButton btnIniciar = new JButton("Iniciar");
		final JButton btnParar = new JButton("Parar");

		
		btnIniciar.setEnabled(false);
		btnIniciar.setHorizontalAlignment(SwingConstants.RIGHT);
		top_panel.add(btnIniciar);
		btnIniciar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				server.start();
				console.append("\nO servidor iniciou.");
				System.out.println("\nO servidor iniciou.");
				btnParar.setEnabled(true);
				btnIniciar.setEnabled(false);
			}
		});
		

		
		btnParar.setEnabled(true);
		btnParar.setHorizontalAlignment(SwingConstants.RIGHT);
		top_panel.add(btnParar);
		btnParar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				server.stopServer();
				console.append("\nO Servidor terminou.");
				System.out.println("\nO Servidor terminou.");
				btnIniciar.setEnabled(true);
				btnParar.setEnabled(false);
			}
		});
		// ////////////////////////////////////////////////////

		panel = new JPanel();
//		panel.setAlignmentX(LEFT_ALIGNMENT);
//		panel.setAlignmentY(TOP_ALIGNMENT);
		console = new JTextArea();
		console.setEditable(false);
//		console.setAlignmentX(LEFT_ALIGNMENT);
//		console.setAlignmentY(TOP_ALIGNMENT);
		console.setBorder(BorderFactory.createEmptyBorder(200, 200, 200, 200));

		JScrollPane pane = new JScrollPane();
		pane.setViewportView(console);
		pane.setAlignmentX(LEFT_ALIGNMENT);
		pane.setAlignmentY(TOP_ALIGNMENT);

		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(pane);
		getContentPane().add(panel);

		JPanel bottom_panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) bottom_panel.getLayout();
		flowLayout.setVgap(35);
		flowLayout.setHgap(10);
		getContentPane().add(bottom_panel, BorderLayout.SOUTH);

		JButton split = new JButton("Guardar Ficheiro");
		bottom_panel.add(split);
		split.setHorizontalAlignment(SwingConstants.RIGHT);

		split.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				JFileChooser fileopen = new JFileChooser();
				FileFilter filter = new FileNameExtensionFilter("c files", "c");
				fileopen.addChoosableFileFilter(filter);

				int ret = fileopen.showDialog(panel, "Ficheiro a guardar");

				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = fileopen.getSelectedFile();
					try {
						client.sendFile(file,1);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		});

		JButton unsplit = new JButton("Recuperar Ficheiro");
		bottom_panel.add(unsplit);

		unsplit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				JFileChooser fileopen = new JFileChooser();
				File repos = new File("repositorio");
				fileopen.setCurrentDirectory(repos);
				FileFilter filter = new FileNameExtensionFilter("c files", "c");
				fileopen.addChoosableFileFilter(filter);

				int ret = fileopen.showDialog(panel, "Seleccionar a primeira parte");

				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = fileopen.getSelectedFile();
					try {
						client.getFile(file);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		});

		JButton deletefile = new JButton("Apagar Ficheiro");
		bottom_panel.add(deletefile);
		deletefile.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				JFileChooser fileopen = new JFileChooser();
				File repos = new File("repositorio");
				fileopen.setCurrentDirectory(repos);
				FileFilter filter = new FileNameExtensionFilter("c files", "c");
				fileopen.addChoosableFileFilter(filter);

				int ret = fileopen.showDialog(panel, "Apagar ficheiro");

				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = fileopen.getSelectedFile();
					try {
						client.delete(file.getName());
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
					String state = "\nEliminação pedida ao grupo Multicast.";
					console.append(state);
					System.out.println(state);
				}

			}
		});
		

		setTitle("Multicast Backuper");
		setSize(600, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

	}

	public String readFile(File file) {

		StringBuffer fileBuffer = null;
		String fileString = null;
		String line = null;

		try {
			FileReader in = new FileReader(file);
			BufferedReader brd = new BufferedReader(in);
			fileBuffer = new StringBuffer();

			while ((line = brd.readLine()) != null) {
				fileBuffer.append(line).append(
						System.getProperty("line.separator"));
			}

			in.close();
			fileString = fileBuffer.toString();
		} catch (IOException e) {
			return null;
		}
		return fileString;
	}
	
	public static Hashtable<String,Integer> getStored(){
		return stored;
	}

}