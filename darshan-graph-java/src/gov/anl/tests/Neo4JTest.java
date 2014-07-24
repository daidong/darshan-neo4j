package gov.anl.tests;

import java.util.ArrayList;

import gov.anl.metadata.RelationType;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

public class Neo4JTest {
	
	private String path = "/tmp/neo4jtest";
	private GraphDatabaseService graphDb;
	
	public Neo4JTest(){
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path);
		registerShutdownHook(graphDb);
		
	}
	
	private void registerShutdownHook(final GraphDatabaseService graphDb){
		Runtime.getRuntime().addShutdownHook( new Thread(){
			@Override
			public void run(){
				graphDb.shutdown();
			}
		});
	}
	
	public void addLabelAt(){
		IndexDefinition indexDefinition;
		try ( Transaction tx = graphDb.beginTx() )
		{
		    Schema schema = graphDb.schema();
		    indexDefinition = schema.indexFor( DynamicLabel.label( "User" ) )
		            .on( "username" )
		            .create();
		    // ask for synchronzation before index finish
		    //schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );
		    tx.success();
		}
		
	}
	
	public void dropLabelAt(){
		try ( Transaction tx = graphDb.beginTx() )
		{
		    Label label = DynamicLabel.label( "User" );
		    for ( IndexDefinition indexDefinition : graphDb.schema()
		            .getIndexes( label ) )
		    {
		        // There is only one index
		        indexDefinition.drop();
		    }

		    tx.success();
		}
	}
	
	public void addLabelNodes(){
		try ( Transaction tx = graphDb.beginTx() )
		{
		    Label label = DynamicLabel.label( "User" );

		    // Create some users
		    for ( int id = 0; id < 100; id++ )
		    {
		        Node userNode = graphDb.createNode( label );
		        userNode.setProperty( "username", "user" + id + "@neo4j.org" );
		    }
		    System.out.println( "Users created" );
		    tx.success();
		}
	}
	
	public void locateLabelNode(){
		Label label = DynamicLabel.label( "User" );
		int idToFind = 45;
		String nameToFind = "user" + idToFind + "@neo4j.org";
		try ( Transaction tx = graphDb.beginTx() )
		{
		    try ( ResourceIterator<Node> users =
		            graphDb.findNodesByLabelAndProperty( label, "username", nameToFind ).iterator() )
		    {
		        ArrayList<Node> userNodes = new ArrayList<>();
		        while ( users.hasNext() )
		        {
		        	userNodes.add( users.next() );
		        }

		        for ( Node node : userNodes )
		        {
		            System.out.println( "The username of user " + idToFind + " is " + node.getProperty( "username" ) );
		        }
		    }
		    tx.success();
		}
	}
	
	public void locateIndexNode(){
		try ( Transaction tx = graphDb.beginTx() )
		{
			Index<Node> nodeIndex = graphDb.index().forNodes("node");
			/*
			for ( int id = 0; id < 100; id++ )
            {
				Node node = graphDb.createNode();
				String username = "user" + id + "@neo4j.org";
		        node.setProperty( "username", username );
		        nodeIndex.add( node, "username", username );
            }
			*/
			int idToFind = 450;
			String userName = "user" + idToFind + "@neo4j.org";
			Node foundUser = nodeIndex.get( "username", userName ).getSingle();
			if (foundUser != null)
				System.out.println( "The username of user " + idToFind + " is " + foundUser.getProperty( "username" ) );
			tx.success();
		}
	}
	
	public void basic(){
		
		Node firstNode;
		Node secondNode;
		Relationship relationship;
		
		try (Transaction tx = graphDb.beginTx()){
			firstNode = graphDb.createNode();
			firstNode.setProperty("message", "hello, ");
			secondNode = graphDb.createNode();
			secondNode.setProperty("message", "world!");
			
			relationship = firstNode.createRelationshipTo(secondNode, RelationType.IsA);
			relationship.setProperty("message", "good ");
			

			System.out.print( firstNode.getProperty( "message" ) );
			System.out.print( relationship.getProperty( "message" ) );
			System.out.print( secondNode.getProperty( "message" ) );
			
			firstNode.getSingleRelationship( RelationType.IsA, Direction.OUTGOING ).delete();
			firstNode.delete();
			secondNode.delete();
			
			tx.success();
		}
		
		
		
		graphDb.shutdown();
	}
	
	
	public static void main(String[] args) {
		Neo4JTest neo = new Neo4JTest();
		//neo.basic();
		//neo.askDataBaseIndexAt();
		//neo.addIndexNodes();
		//neo.locateIndexNode();
		neo.locateIndexNode();
	}

}
