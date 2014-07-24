package gov.anl.main;

import gov.anl.dargraph.store.GraphProvInterface;
import gov.anl.dargraph.store.neo4j.GraphProvImpl;
import gov.anl.dargraph.store.neo4j.GraphProvImplInMemory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DarshanGraph {

	private String path;
	private GraphProvInterface gs;
	
	public DarshanGraph(String darshan_log, String db_file){
		this.path = darshan_log;
		//gs = new GraphProvImpl(db_file);
		gs = new GraphProvImplInMemory(db_file);
	}
	public void loadLog() throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line;
		long l = 0;
		
		while ((line = br.readLine()) != null) {
			
			System.out.println("Process line " + (l));
			
			String[] content = line.split(" ");
			
			/*
			long user_id = Long.parseLong(content[0]);
			long job_id = Long.parseLong(content[1]);
			long obj_id = Long.parseLong(content[2]);
			*/
			
			String user_id = content[0];
			String job_id = content[1];
			String obj_id = content[2];
			long start_time = Long.parseLong(content[3]);
			long end_time = Long.parseLong(content[4]);
			int wrops = Integer.parseInt(content[5]);
			
			gs.userStartJob(user_id, job_id, start_time, end_time);
			gs.jobFromObj(job_id, obj_id, start_time, end_time);
			
			for (int i = 0; i < wrops; i++){
				try{
					String proc_id = content[6+i*4];
					String file_id = content[7+i*4];
					long reads = Long.parseLong(content[8+i*4]);
					long writes = Long.parseLong(content[9+i*4]);
				
					gs.jobHasProcs(job_id, job_id+":"+proc_id);
					gs.procReadsFile(job_id+":"+proc_id, file_id, reads);
					gs.procWritesFile(job_id+":"+proc_id, file_id, writes);
				} catch (ArrayIndexOutOfBoundsException e){
					System.out.println("Array Exception at Index: " + l);
				}
			}
			l++;
		}
		gs.shutdown();
		br.close();
		
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		DarshanGraph dg = new DarshanGraph("/Users/daidong/Documents/workspace/DarshanGraphProcessing/log-2013-1-9.txt", 
				"/Users/daidong/Documents/workspace/DarshanGraphProcessing/neo4jtest");
		
		dg.loadLog();
	}

}
