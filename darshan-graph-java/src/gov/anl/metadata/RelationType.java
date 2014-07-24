package gov.anl.metadata;

import org.neo4j.graphdb.RelationshipType;

public enum RelationType implements RelationshipType{

	IsA,
	Contain, 
	
	RunJob,
	JobRunBy,
	
	ExeFile,
	FileExedBy,
	
	HasProcs,
	OneProcOf,
	
	ReadFile,
	FileReadBy,
	
	WriteFile,
	FileWrittenBy;
	
	
}
