package gov.anl.main;

import gov.anl.metadata.NodeType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class BatchDarshanGraph {

	private String path;
	
	private BatchInserter bi;
	private BatchInserterIndexProvider indexProvider;
	
	private TreeMap<BigInteger, Long> UserIndex;
	private TreeMap<BigInteger, Long> JobIndex;
	private TreeMap<BigInteger, Long> FileIndex;
	
	Label UserLabel;
	Label JobLabel;
	Label FileLabel;
	Label ProcLabel;
	
	private BatchInserterIndex user;
	private BatchInserterIndex job;
	private BatchInserterIndex proc;
	private BatchInserterIndex file;
	
	private long parseUint64(String v){
		long t;
		try{
			t = Long.parseLong(v);
		} catch (NumberFormatException e){
			//this means v is larger than Long.MAX_VALUE but less than 2*Long.MAX_VALUE
			BigInteger bt = new BigInteger(v);
			BigInteger largest = BigInteger.valueOf(Long.MAX_VALUE);
			t = 0 - bt.subtract(largest).longValue();
		}
		return t;
	}
	
	public BatchDarshanGraph(String darshan_log, String db_file){
		this.path = darshan_log;
		bi = BatchInserters.inserter(db_file);
		
		this.UserIndex = new TreeMap<BigInteger, Long>();
		this.JobIndex = new TreeMap<BigInteger, Long>();
		this.FileIndex = new TreeMap<BigInteger, Long>();
		
		this.UserLabel = DynamicLabel.label("user");
		this.JobLabel = DynamicLabel.label("job");
		this.ProcLabel = DynamicLabel.label("proc");
		this.FileLabel = DynamicLabel.label("file");
		
		this.indexProvider =  new LuceneBatchInserterIndexProvider(this.bi);
		this.user =  indexProvider.nodeIndex( "users", MapUtil.stringMap( "type", "exact" ) );
		this.job =  indexProvider.nodeIndex( "jobs", MapUtil.stringMap( "type", "exact" ) );
		this.proc =  indexProvider.nodeIndex( "procs", MapUtil.stringMap( "type", "exact" ) );
		this.file =  indexProvider.nodeIndex( "files", MapUtil.stringMap( "type", "exact" ) );
		
		
		user.setCacheCapacity( NodeType.UserId, 10000);
		job.setCacheCapacity( NodeType.JobId, 50000);
		proc.setCacheCapacity( NodeType.ProcId, 100000);
		file.setCacheCapacity( NodeType.FileId, 1000000);
	}
	
	/**
	 * @TODO Missing nprocs attribute on each job 
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void loadLog() throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line;
		long l = 0;
		
		while ((line = br.readLine()) != null) {
			System.out.println("Process line " + (l));
			String[] content = line.split(" ");
			
			String user_id = content[0];
			String job_id = content[1];
			String obj_id = content[2];
			long start_time = Long.parseLong(content[3]);
			long end_time = Long.parseLong(content[4]);
			int wrops = Integer.parseInt(content[5]);
			
			Long user;
			BigInteger big_user_id = new BigInteger(user_id);
			if (!this.UserIndex.containsKey(big_user_id)){
				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put(NodeType.UserId, user_id);
				user = bi.createNode(properties, this.UserLabel);
				this.UserIndex.put(big_user_id, user);
				this.user.add(user, properties);
			} else {
				user = this.UserIndex.get(big_user_id).longValue();
			}
			
			Long job;
			BigInteger big_job_id = new BigInteger(job_id);
			if (! this.JobIndex.containsKey(big_job_id)){
				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put(NodeType.JobId, job_id);
				job = bi.createNode(properties, this.JobLabel);
				this.JobIndex.put(big_job_id, job);
				this.job.add(job, properties);
			} else {
				job = this.JobIndex.get(big_job_id).longValue();
			}
			
			Long exe;
			BigInteger big_exe_id = new BigInteger(obj_id);
			if (! this.FileIndex.containsKey(big_exe_id)){
				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put(NodeType.FileId, obj_id);
				exe = bi.createNode(properties, this.FileLabel);
				this.FileIndex.put(big_exe_id, exe);
				this.file.add(exe, properties);
			} else {
				exe = this.FileIndex.get(big_exe_id).longValue();
			}
			
			RelationshipType rj = DynamicRelationshipType.withName("RunJob");
			RelationshipType rjb = DynamicRelationshipType.withName("JobRunBy");
			RelationshipType jre = DynamicRelationshipType.withName("ExeFile");
			RelationshipType erbj = DynamicRelationshipType.withName("FileExedBy");
			
			Map<String, Object> pt = new HashMap<String, Object>();
			pt.put(NodeType.StartTime, String.valueOf(start_time));
			pt.put(NodeType.EndTime, String.valueOf(end_time));
			
			bi.createRelationship(user, job, rj, pt);
			bi.createRelationship(job, user, rjb, pt);
			bi.createRelationship(job, exe, jre, pt);
			bi.createRelationship(exe, job, erbj, pt);
			
			
			for (int i = 0; i < wrops; i++){
				try{
					String proc_id = content[6+i*4];
					String file_id = content[7+i*4];
					long reads = Long.parseLong(content[8+i*4]);
					long writes = Long.parseLong(content[9+i*4]);
				
					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put(NodeType.ProcId, job_id+":"+proc_id);
					long proc = bi.createNode(properties, this.ProcLabel);
					this.proc.add(proc, properties);
					
					Long file;
					BigInteger big_file_id = new BigInteger(file_id);
					if (! this.FileIndex.containsKey(big_file_id)){
						Map<String, Object> p2 = new HashMap<String, Object>();
						p2.put(NodeType.FileId, file_id);
						file = bi.createNode(p2, this.FileLabel);
						this.FileIndex.put(big_file_id, file);
						this.file.add(file, p2);
					} else {
						file = this.FileIndex.get(big_file_id).longValue();
					}
					
					RelationshipType contain = DynamicRelationshipType.withName("Contain");
					RelationshipType isa = DynamicRelationshipType.withName("IsA");
					RelationshipType rd = DynamicRelationshipType.withName("ReadFile");
					RelationshipType rdby = DynamicRelationshipType.withName("FileReadBy");
					RelationshipType wt = DynamicRelationshipType.withName("WriteFile");
					RelationshipType wty = DynamicRelationshipType.withName("FileWrittenBy");
					
					Map<String, Object> p1 = new HashMap<String, Object>();
					p1.put(NodeType.Size, String.valueOf(reads));
					
					Map<String, Object> p2 = new HashMap<String, Object>();
					p1.put(NodeType.Size, String.valueOf(writes));
					
					bi.createRelationship(job, proc, contain, null);
					bi.createRelationship(proc, job, isa, null);
					bi.createRelationship(proc, file, rd, p1);
					bi.createRelationship(file, proc, rdby, p1);
					bi.createRelationship(proc, file, wt, p2);
					bi.createRelationship(file, proc, wty, p2);
					
					
				} catch (ArrayIndexOutOfBoundsException e){
					System.out.println("Array Exception at Index: " + l);
				}
			}
			l++;
		}
		this.user.flush();
		this.job.flush();
		this.proc.flush();
		this.file.flush();
		bi.shutdown();
		
		br.close();
		
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		BatchDarshanGraph dg = new BatchDarshanGraph("/Users/daidong/Documents/gitrepos/graph/darshan-graph/log-2013-1-9.txt", 
				"/Users/daidong/Documents/workspace/DarshanGraphProcessing/neo4jtest");
		
		dg.loadLog();
	}

}
