package FileManager;

import java.io.File;

public class Chunk {

	int chunk_id;
	String file_id;
	float chunk_size;
	File chunk_data;
	
	
	Chunk(String file, int id, File data) {
		file_id=file;
		chunk_id=id;
		chunk_size=64000;
		chunk_data=data;		
	}
	
	public int getChunkId() {
		
		return chunk_id;
	}
	
	public String getChunkFileId() {
		
		return file_id;
	}
	
	public File getChunkData() {
		
		return chunk_data;
	}
}
