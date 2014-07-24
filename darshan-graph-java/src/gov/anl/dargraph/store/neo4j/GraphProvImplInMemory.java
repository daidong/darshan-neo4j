package gov.anl.dargraph.store.neo4j;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import scala.math.BigInt;
import gov.anl.dargraph.store.GraphProvInterface;
import gov.anl.metadata.NodeType;
import gov.anl.metadata.RelationType;

public class GraphProvImplInMemory implements GraphProvInterface {

	private String path;
	private GraphDatabaseService graphDb;
	private HashMap<String, Node> UserIndex;
	private HashMap<String, Node> JobIndex;
	private HashMap<String, Node> FileIndex;
	
	private RelationshipIndex UserStartJobIndex;
	
	public GraphProvImplInMemory(String dbfile){
		this.path = dbfile;
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(this.path);
		registerShutdownHook(graphDb);
		createIndex();
	}
	
	public void createIndex(){
		this.UserIndex = new HashMap<String, Node>();
		this.JobIndex = new HashMap<String, Node>();
		this.FileIndex = new HashMap<String, Node>();
	}

	private void registerShutdownHook(final GraphDatabaseService graphDb){
		Runtime.getRuntime().addShutdownHook( new Thread(){
			@Override
			public void run(){
				graphDb.shutdown();
			}
		});
	}
	
	@Override
	public void userStartJob(String user_id, String job_id, long start_time,
			long end_time) {
		try (Transaction tx = graphDb.beginTx()){
			Node user = this.UserIndex.get(user_id);
			if (user == null){
				user = graphDb.createNode();
				user.setProperty(NodeType.UserId, user_id);
				this.UserIndex.put(user_id, user);
				
			}
			Node job = this.JobIndex.get(job_id);
			if (job == null){
				job = graphDb.createNode();
				job.setProperty(NodeType.JobId, job_id);
				this.JobIndex.put(job_id, job);
			}
			
			
			Relationship relationship = user.createRelationshipTo(job, RelationType.RunJob);
			relationship.setProperty(NodeType.StartTime, String.valueOf(start_time));
			relationship.setProperty(NodeType.EndTime, String.valueOf(end_time));
			
			Relationship relationship2 = job.createRelationshipTo(user, RelationType.JobRunBy);
			relationship2.setProperty(NodeType.StartTime, String.valueOf(start_time));
			relationship2.setProperty(NodeType.EndTime, String.valueOf(end_time));
			
			tx.success();
		}
	}

	@Override
	public void jobFromObj(String job_id, String obj_id, long start_time,
			long end_time) {
		
		try (Transaction tx = graphDb.beginTx()){
			Node job = this.JobIndex.get(job_id);
			if (job == null){
				job = graphDb.createNode();
				job.setProperty(NodeType.JobId, job_id);
				this.JobIndex.put(job_id, job);
			}
			
			Node obj = this.FileIndex.get(obj_id);
			if (obj == null){
				obj = graphDb.createNode();
				obj.setProperty(NodeType.FileId, obj_id);
				this.FileIndex.put(obj_id, obj);
				
			}
			
			Relationship relationship = job.createRelationshipTo(obj, RelationType.ExeFile);
			relationship.setProperty(NodeType.StartTime, String.valueOf(start_time));
			relationship.setProperty(NodeType.EndTime, String.valueOf(end_time));
			//UserStartJobIndex.add(relationship, "startJob", );
			
			Relationship relationship2 = obj.createRelationshipTo(job, RelationType.FileExedBy);
			relationship2.setProperty(NodeType.StartTime, String.valueOf(start_time));
			relationship2.setProperty(NodeType.EndTime, String.valueOf(end_time));
			
			tx.success();
		}
	
	}

	@Override
	public void jobHasProcs(String job_id, String proc_id) {
		
		try (Transaction tx = graphDb.beginTx()){
			Node job = this.JobIndex.get(job_id);
			if (job == null){
				job = graphDb.createNode();
				job.setProperty(NodeType.JobId, job_id);
				this.JobIndex.put(job_id, job);
			}
			
			Node proc = graphDb.createNode();
			proc.setProperty(NodeType.ProcId, proc_id);
			
			Relationship relationship = job.createRelationshipTo(proc, RelationType.Contain);
			Relationship relationship2 = proc.createRelationshipTo(job, RelationType.IsA);
			
			tx.success();
		}
	}

	@Override
	public void procReadsFile(String proc_id, String file_id, long reads) {
		try (Transaction tx = graphDb.beginTx()){
			
			//Node proc = this.ProcIndex.get(NodeType.ProcId, proc_id).getSingle();
			//if (proc == null){
			Node proc = graphDb.createNode();
			proc.setProperty(NodeType.ProcId, proc_id);
			//this.ProcIndex.add(proc, NodeType.ProcId, proc_id);
			//}
			
			Node file = this.FileIndex.get(file_id);
			if (file == null){
				file = graphDb.createNode();
				file.setProperty(NodeType.FileId, file_id);
				this.FileIndex.put(file_id, file);
			}
			
			Relationship relationship = proc.createRelationshipTo(file, RelationType.ReadFile);
			relationship.setProperty(NodeType.Size, String.valueOf(reads));
			
			Relationship relationship2 = file.createRelationshipTo(proc, RelationType.FileReadBy);
			relationship.setProperty(NodeType.Size, String.valueOf(reads));
			
			tx.success();
		}
	}
	@Override
	public void shutdown() {
		graphDb.shutdown();
	}

	@Override
	public void procWritesFile(String proc_id, String file_id, long writes) {
		try (Transaction tx = graphDb.beginTx()){
			
			/*
			Node proc = this.ProcIndex.get(NodeType.ProcId, proc_id).getSingle();
			if (proc == null){
				proc = graphDb.createNode();
				proc.setProperty(NodeType.ProcId, proc_id);
				this.ProcIndex.add(proc, NodeType.ProcId, proc_id);
				
			}
			*/
			Node proc = graphDb.createNode();
			proc.setProperty(NodeType.ProcId, proc_id);
			
			Node file = this.FileIndex.get(file_id);
			if (file == null){
				file = graphDb.createNode();
				file.setProperty(NodeType.FileId, file_id);
				this.JobIndex.put(file_id, file);
			}
			
			Relationship relationship = proc.createRelationshipTo(file, RelationType.WriteFile);
			relationship.setProperty(NodeType.Size, String.valueOf(writes));
			
			Relationship relationship2 = file.createRelationshipTo(proc, RelationType.FileWrittenBy);
			relationship.setProperty(NodeType.Size, String.valueOf(writes));
			
			tx.success();
		}
	}

}
