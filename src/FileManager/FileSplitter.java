package FileManager;

import java.security.MessageDigest;
import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileSplitter {

	int chunk_size = 64000;
	File f;
	String filename;
	String filepath;
	Vector<Chunk> chunks;
	int parts;
	String encryptedname;
	String encryptedFilepath;

	public FileSplitter(File f) {
		if (f == null)
			throw new IllegalArgumentException("File must be not null!");
		this.f = f;
		System.out.println("File Length (KB): " + f.length() / 1024.0);
		//////////////SPLIT//////////////////////////
		filename = f.getName();
		filepath = f.getPath();
		encryptedname = encryptFilename(filename);
		//encryptedFilepath = filepath.substring(0, filepath.lastIndexOf("\\"))+ "\\" + encryptedname;
		/////////////////////////////////////////////
		parts = ((int) (f.length() / chunk_size));
		if (f.length() % chunk_size > 0)
			parts++;
	}

	public boolean split() {
		if (chunk_size <= 0)
			return false;

		try {

			long flength = 0;

			chunks = new Vector<Chunk>(parts);

			FileInputStream fis = new FileInputStream(f);
			FileOutputStream fos = null;

			// Guarda as chunks no computador
			for (int i = 0; i < parts; i++) {
				File data = new File("repositorio\\" + encryptedname + ".part" + i);//retirar parts
				chunks.add(i, new Chunk(encryptedname, i, data));

				fos = new FileOutputStream(chunks.elementAt(i).getChunkData());

				int read = 0;
				int total = 0;
				byte[] buff = new byte[1024];
				int origbuff = buff.length;
				while (total < chunk_size) {
					read = fis.read(buff);
					if (read != -1) {
						//buff = invertBuffer(buff, 0, read);
						total += read;
						flength += read;
						fos.write(buff, 0, read);
					}
					if (i == chunks.capacity() - 1 && read < origbuff) {
						break;
					}
				}

				fos.flush();
				fos.close();
				fos = null;
			}

			fis.close();
			f = chunks.elementAt(0).getChunkData();

			System.out.println("Length Readed (KB): " + flength / 1024.0);
			return true;
		} catch (Exception ex) {
			System.out.println(ex);
			System.out.println(ex.getLocalizedMessage());
			System.out.println(ex.getStackTrace()[0].getLineNumber());
			ex.printStackTrace();
			return false;
		}
	}

	public boolean unsplit(String original) {
		try {
			boolean exists = true;
			File temp = null;
			// File(f.getPath().substring(0,f.getPath().lastIndexOf(".part")));
			//File dest = new File(filepath.substring(0,filepath.lastIndexOf("\\")) + "\\" + original);
			File dest = new File("recovered\\" + original);

			FileInputStream fis = null;
			FileOutputStream fos = new FileOutputStream(dest);
			int part = 0;
			long flength = 0;
			String name = null;

			while (exists) {
				name = f.getPath();
				name = name.substring(0, name.lastIndexOf("t") + 1) + part;
				temp = new File(name);

				exists = temp.exists();
				if (!exists)
					break;

				fis = new FileInputStream(temp);
				byte[] buff = new byte[1024];

				int read = 0;
				while ((read = fis.read(buff)) > 0) {
					//buff = invertBuffer(buff, 0, read);
					fos.write(buff, 0, read);
					if (read > 0)
						flength += read;
				}

				fis.close();
				fis = null;
				part++;
			}

			fos.flush();
			fos.close();
			f = dest;
			System.out.println("Length Writed: " + flength / 1024.0);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static byte[] invertBuffer(byte[] buff, int offset, int length) {
		if (buff == null || buff.length == 0)
			return null;
		if (offset < 0 || length < 0)
			return null;

		byte[] inverted = new byte[length];
		int ind = length - 1;
		for (int i = offset; i < length; i++) {
			inverted[ind] = buff[i];
			ind--;
		}
		return inverted;
	}

	public static String encryptFilename(String filename) {

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(filename.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public int getFileParts() {

		return parts;
	}

	public String getEncryptedName() {

		return encryptedname;
	}

	public String getEncryptedFilePath() {

		return encryptedFilepath;
	}

	public Vector<Chunk> getChunks() {
		return chunks;
	}

}
