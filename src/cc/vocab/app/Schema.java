package cc.vocab.app;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@Path("schema")
public class Schema {
	
	@Path("/{x}")
	@GET
	@Produces("application/rdf+xml")
	public Response schemaXML(@Context UriInfo info){
		OutputStream out = new ByteArrayOutputStream();
		getS(info).write(out, null);
		return Response.ok(out.toString()).build();
	}
	@Path("/{x}")
	@GET
	@Produces("text/N3")
	public Response schemaN3(@Context UriInfo info){
		OutputStream out = new ByteArrayOutputStream();
		getS(info).write(out, "N3");
		return Response.ok(out.toString()).build();
	}
	
	private Model getS(UriInfo info){
		
		String model = "" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
				"@prefix voc: <"+info.getBaseUri()+"schema/> . " +
				"voc:classUsedInDocuments a rdf:Property ;" +
				"rdfs:domain rdfs:Class ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the number of documents a class was used in the BTC dataset.\" . " +
				"voc:posInClassDocumentRanking a rdf:Property ;" +
				"rdfs:domain rdfs:Class ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the position of a class in the document ranking of classes. The ranking is ordered by the number of documents a class was used in the BTC dataset.\" . " +
				"voc:posInClassRanking a rdf:Property ;" +
				"rdfs:domain rdfs:Class ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the position of a class in the overall ranking of classes. The ranking is ordered by the overall occurence of classes in the BTC dataset.\" . " +
				"voc:usedAsClass a rdf:Property ;" +
				"rdfs:domain rdfs:Class ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the number of overall occurences of a class in the BTC dataset.\" . " +
				"voc:propertyUsedInDocuemnts a rdf:Property ;" +
				"rdfs:domain rdf:Property ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the number of documents a property was used in the BTC dataset.\" . " +
				"voc:posInPrDocumentRanking a rdf:Property ;" +
				"rdfs:domain rdf:Property ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the position of a property in the document ranking of properties. The ranking is ordered by the number of documents a property was used in the BTC dataset.\" . " +
				"voc:posInPropRanking a rdf:Property ;" +
				"rdfs:domain rdf:Property ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the position of a property in the overall ranking of properties. The ranking is ordered by the overall occurence of properties in the BTC dataset.\" . " +
				"voc:usedAsProperty a rdf:Property ;" +
				"rdfs:domain rdf:Property ;" +
				"rdfs:range rdfs:Literal ;" +
				"rdfs:comment \"Shows the number of overall occurences of a property in the BTC dataset.\" . " +
				"" ;
		
		Model sc = ModelFactory.createDefaultModel();
		sc.read(new StringReader(model), null, "N3");
		
		return sc;
		
	}
}
