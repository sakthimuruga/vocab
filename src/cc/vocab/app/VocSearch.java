package cc.vocab.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.linkedservices.ServiceDescriptionLab;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;

@Path("/search")
public class VocSearch {
	
	@GET
	@Produces("text/html")
	public Response searchHTML(@QueryParam("query") String query, @Context ServletContext cont, @Context UriInfo info) throws IOException{
		TreeMap<Integer, String> map = new TreeMap<Integer, String>(Collections.reverseOrder()); 
		map.putAll(doEnrichedS(query, cont));
		
		if(map.size()<1){
			return this.notFound(query, cont, info);
		}
		
	    Iterator<Integer> itr = map.keySet().iterator();
		
		String rep = "<h3>Search Results</h3>" +
				"maybe these URIs represent what you are looking for: <br />" +
				"<table border=\"0\">";
		rep += "<tr><th>URI</th><th>Occured Overall</th><th>&nbspType</th></tr>";
		for(int i = 1; (i<=100 && itr.hasNext()); i++){
			int q = itr.next();
			rep += "<tr><td>"+VocUtils.makeLink_sm(map.get(q).split("\t")[0], info)+"</td><td>"+q+"</td><td>&nbsp"+map.get(q).split("\t")[1]+"</td></tr>";
		}
		rep += "</table>";
		
		String resp = VocUtils.readFile("/files/resp.html", cont).replace("REPLACE_QUERY", "").replace("REPLACE_OCCUR", "").replace("REPLACE_POSITION", "");
		resp= resp.replace("REPLACE_KIND", rep);
		return Response.ok(resp).build();
	}
	
	@GET
	@Produces("application/rdf+xml")
	public Response searchXML(@QueryParam("query") String query, @Context ServletContext cont, @Context UriInfo info) throws IOException{
		Model model = this.getRDF(query, cont, info);
		OutputStream out = new ByteArrayOutputStream();
		model.write(out, null);
		return Response.ok(out.toString()).build();
	}
	@GET
	@Produces("text/N3")
	public Response searchN3(@QueryParam("query") String query, @Context ServletContext cont, @Context UriInfo info) throws IOException{
		Model model = this.getRDF(query, cont, info);
		OutputStream out = new ByteArrayOutputStream();
		model.write(out, "N3");
		return Response.ok(out.toString()).build();
	}
	@POST
	@Produces("application/rdf+xml")
	public Response searchXML_p(String input, @Context ServletContext cont, @Context UriInfo info) throws IOException{
		Model model = this.getRDFbyPost(input, cont, info);
		OutputStream out = new ByteArrayOutputStream();
		model.write(out, null);
		return Response.ok(out.toString()).build();
	}
	@POST
	@Produces("text/N3")
	public Response searchN3_p(String input, @Context ServletContext cont, @Context UriInfo info) throws IOException{
		Model model = this.getRDFbyPost(input, cont, info);
		OutputStream out = new ByteArrayOutputStream();
		model.write(out, "N3");
		return Response.ok(out.toString()).build();
	}
	
	@Path("/description")
	@GET
	@Produces("text/html")
	public Response descHTML(@Context ServletContext cont, @Context UriInfo info){
		String ret = VocUtils.readFile("/files/description.html", cont)
		.replace("PLACE_NS" , 
				ServiceDescriptionLab.getNS(VocUtils.readFile("/files/descriptionS.rdf", cont)
						.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/")) )
		.replace("PLACE_INPUT" , 
				ServiceDescriptionLab.getInput(VocUtils.readFile("/files/descriptionS.rdf", cont)
						.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/")) )
		.replace("PLACE_OUTPUT" , 
				ServiceDescriptionLab.getOutput(VocUtils.readFile("/files/descriptionS.rdf", cont)
						.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/")) )				
		;
		return Response.ok(ret).build();
	}
	@Path("/description")
	@GET
	@Produces("application/rdf+xml")
	public Response descXML(@Context ServletContext cont, @Context UriInfo info ){
		return Response.ok( ServiceDescriptionLab.getDescAsXML(VocUtils.readFile("/files/descriptionS.rdf", cont)
								.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/" )	)  
						).build();
	}
	@Path("/description")
	@GET
	@Produces("text/N3")
	public Response descN3(@Context ServletContext cont, @Context UriInfo info ){
		return Response.ok( ServiceDescriptionLab.getDescAsN3(VocUtils.readFile("/files/descriptionS.rdf", cont)
								.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/" )	)  
						).build();
	}
	
	/*
	 * HELPER
	 */
	
	//search by lowering input
	private Model getRDFbyPost(String input, ServletContext cont, UriInfo info){
		Model in = ModelFactory.createDefaultModel();
		in.read(new StringReader(input), null, null);
		String queryS = "" +
				"PREFIX so:<http://purl.org/linkedservices/ontology/SearchOntology#> " +
				"SELECT ?qStr " +
				"WHERE { " +
				"	?x a so:Query ; " +
				"	so:queryString ?qStr . " +
				" }";
		Query query = QueryFactory.create(queryS);
		QueryExecution queryEx = QueryExecutionFactory.create(query, in);
		ResultSet rSet = queryEx.execSelect();
		Model toReturn = ModelFactory.createDefaultModel();
		while(rSet.hasNext()){
			QuerySolution sol = rSet.next();
			RDFNode qStr = sol.get("?qStr");
			toReturn.add(getRDF(qStr.toString(), cont, info));
		}
		return toReturn;
	}
	
	//get search result as RDF
	private Model getRDF(String query, ServletContext cont, UriInfo info){
		Model out = ModelFactory.createDefaultModel();
		Map<Integer, String> map = doEnrichedS(query, cont);
		String model = "" +
				"@prefix voc: <"+info.getBaseUri()+"schema/> . " +
				"@prefix so: <http://purl.org/linkedservices/ontology/SearchOntology#> . " +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
				"<"+info.getBaseUri()+"search?query="+query+"#query> a so:Query ; " +
				"	so:queryString \""+query+"\" ";

		String detail ="";
		for(Entry<Integer, String> e: map.entrySet()){
			model += "" +
					"; so:result <"+e.getValue().split("\t")[0]+"> ";
			detail += "" +
					"<"+e.getValue().split("\t")[0]+"> a "+( (e.getValue().split("\t")[1].equals("Class")) ? "rdfs:Class" : "rdf:Property" ) +" ; " +
					( (e.getValue().split("\t")[1].equals("Class")) ? "voc:usedAsClass" : "voc:usedAsProperty" ) +" "+e.getKey()+" . ";
		}
		model += ". ";
		model += detail;
		
		out.read(new StringReader(model), null, "N3");
		return out;
	}
	
	//search enriched with overall appearence
	private Map<Integer, String> doEnrichedS(String query, ServletContext cont){
		Set<String> results = doSearch(query, cont);
		Map<Integer, String> map = new HashMap<Integer, String>();
		if(results == null){
			return map;
		}
		for(String s : results){
			String c = lookupC(s, cont);
			String p = lookupP(s, cont);
			if(c!=null && p!=null){
				int ci = Integer.valueOf(c.split("\t")[1]);
				int pi = Integer.valueOf(p.split("\t")[1]);
				if(ci<pi){
					map.put(pi, s+"\tProperty");
				}else{
					map.put(ci, s+"\tClass");
				}
			}else if(c==null && p!=null){
				int pi = Integer.valueOf(p.split("\t")[1]);
				map.put(pi, s+"\tProperty");
			}else if(c!=null && p==null){
				int ci = Integer.valueOf(c.split("\t")[1]);
				map.put(ci, s+"\tClass");
			}
		}
		return map;
	}
	
	//execute search
	private Set<String> doSearch(String query, ServletContext cont){
		query = query.toLowerCase();
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) cont.getAttribute(Listener.searchMap);
		@SuppressWarnings("unchecked")
		Map<String, String> ref = (Map<String, String>) cont.getAttribute(Listener.refMap);
		StringTokenizer tok = new StringTokenizer(query, " ");
		String uris = null;
		while(uris == null && tok.hasMoreTokens()){
			String first = tok.nextToken();
			uris = map.get(first);
		}
		if(uris == null){
			return null;
		}

		StringTokenizer init = new StringTokenizer(uris, " ");
		Set<String> s1 = new HashSet<String>();
		while(init.hasMoreTokens()){
			s1.add(ref.get(Integer.valueOf(init.nextToken())));
		}
		Set<String> intersect = new TreeSet<String>(s1);
		while(tok.hasMoreTokens()){
			Set<String> so = new HashSet<String>();
			String other = tok.nextToken();
			if(map.get(other)==null){
				intersect.retainAll(so);
				continue;
			}
			StringTokenizer run = new StringTokenizer(map.get(other), " ");
			while(run.hasMoreTokens()){
				so.add(ref.get(Integer.valueOf(run.nextToken())));
			}
			intersect.retainAll(so);
		}
		return intersect;
	}
	
	//lookup uri in class data files
	private String lookupC(String uri, ServletContext cont){
		@SuppressWarnings("unchecked")
		Map<String, String> cd  = (Map<String, String>) cont.getAttribute(Listener.mapC);
		String temp = null;
		temp = cd.get(uri);
		if(temp==null || temp.isEmpty()){
			return null;
		}
		return temp;
	}
	
	//lookup uri in property data files
	private String lookupP(String uri, ServletContext cont){
		@SuppressWarnings("unchecked")
		Map<String, String> cd  = (Map<String, String>) cont.getAttribute(Listener.mapP);
		String temp = null;
		temp = cd.get(uri);
		if(temp==null || temp.isEmpty()){
			return null;
		}
		return temp;
	}
	
	//create not found page
	private Response notFound(String f, ServletContext cont, UriInfo info) throws UnsupportedEncodingException{
		String ret = VocUtils.readFile("/files/resp.html", cont).replace("REPLACE_QUERY", f)
			.replace("REPLACE_KIND", "")
			.replace("REPLACE_OCCUR", "could not be found")
			.replace("REPLACE_POSITION", "");
		return Response.ok(ret).build();
	}

}
