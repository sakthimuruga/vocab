package cc.vocab.app;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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


@Path("/")
public class Voc {
	
	@Path("search")
	@GET
	@Produces("text/html")
	public Response searchHTML(@QueryParam("query") String query, @Context ServletContext cont) throws IOException{
		//resolve prefixes
		if(!query.startsWith("http://")){
			Map<String, String> pref = (Map<String, String>) cont.getAttribute(Listener.prefixes);
			String full = pref.get(query.split(":")[0]);
			if(full==null || full.isEmpty() || full.equals("???")){
				return notFound(query, cont);
			}
			query = full+query.split(":")[1];
		}
		
		int cl = 0;
		int pro = 0;
		
		//lookup classes
		String temp = lookupC(query, cont);

		String cPosO = "";
		String cNO = "";
		String cPosD = "";
		String cND ="";
		if(temp!=null ){
			cPosO = temp.split("\t")[0];
			cNO = temp.split("\t")[1];
			cPosD = temp.split("\t")[2];
			cND = temp.split("\t")[3];

			cl = Integer.valueOf(cNO);
			
		}
		
		//lookup properties
		temp = lookupP(query, cont);

		String pPosO = "";
		String pNO = "";
		String pPosD = "";
		String pND = "";
		
		if(temp!=null){
			pPosO = temp.split("\t")[0];
			pNO = temp.split("\t")[1];
			pPosD = temp.split("\t")[2];
			pND = temp.split("\t")[3];
			
			pro = Integer.valueOf(pNO);
		}
		
		//construct output if possible
		if(pro > 0 || cl > 0){
			String ret = readFile("/files/resp.html", cont).replace("REPLACE_QUERY", query);
			ret = (cl > pro) ? ret	.replace("REPLACE_KIND", "Class")
									.replace("REPLACE_OCCUR", "Occured overall "+beauStr(cNO)+" times <BR> and in "+beauStr(cND)+" datasets.")
									.replace("REPLACE_POSITION", "Is in Position "+beauStr(cPosO)+ " in the overall ranking <BR> " +
											"and in Position "+beauStr(cPosD)+" of the dataset ranking.") 
							 : ret	.replace("REPLACE_KIND", "Property")
							 		.replace("REPLACE_OCCUR", "Occured overall "+beauStr(pNO)+" times <BR> and in "+beauStr(pND)+" datasets.")
							 		.replace("REPLACE_POSITION", "Is in Position "+beauStr(pPosO)+ " in the overall ranking <BR> " +
							 				"and in Position "+beauStr(pPosD)+" of the dataset ranking.")  ;
			return Response.ok(ret).build();
		}else{
			return notFound(query, cont);
		}
	}
	
	
	@Path("search")
	@GET
	@Produces("application/rdf+xml")
	public Response searchXML(@QueryParam("query") String query,  @Context ServletContext cont, @Context UriInfo info ) throws IOException{
		//resolve prefixes
		if(!query.startsWith("http://")){
			Map<String, String> pref = (Map<String, String>) cont.getAttribute(Listener.prefixes);
			String full = pref.get(query.split(":")[0]);
			if(full==null || full.isEmpty() || full.equals("???")){
				return Response.noContent().build();
			}
			query = full+query.split(":")[1];
		}
		
		Model out = ModelFactory.createDefaultModel();
		out.add(getOutputFromValue(query, "http://www.w3.org/2000/01/rdf-schema#Class", cont, info));
		out.add(getOutputFromValue(query, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", cont, info));
		OutputStream outstr = new ByteArrayOutputStream();
		out.write(outstr, null);
		return Response.ok(outstr.toString()).build();
	}
	
	@Path("search")
	@GET
	@Produces("text/N3")
	public Response searchN3(@QueryParam("query") String query,  @Context ServletContext cont, @Context UriInfo info ) throws IOException{
		//resolve prefixes
		if(!query.startsWith("http://")){
			Map<String, String> pref = (Map<String, String>) cont.getAttribute(Listener.prefixes);
			String full = pref.get(query.split(":")[0]);
			if(full==null || full.isEmpty() || full.equals("???")){
				return Response.noContent().build();
			}
			query = full+query.split(":")[1];
		}
		
		Model out = ModelFactory.createDefaultModel();
		out.add(getOutputFromValue(query, "http://www.w3.org/2000/01/rdf-schema#Class", cont, info));
		out.add(getOutputFromValue(query, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", cont, info));
		OutputStream outstr = new ByteArrayOutputStream();
		out.write(outstr, "N3");
		return Response.ok(outstr.toString()).build();
	}
	
	@Path("search")
	@POST
	@Produces("application/rdf+XML")
	public Response searchXML_p(@FormParam("input") String input,  @Context ServletContext cont, @Context UriInfo info ){
		Model out = getOutput(input, cont, info);
		OutputStream outstr = new ByteArrayOutputStream();
		out.write(outstr, null);
		return Response.ok(outstr.toString()).build();
	}
	@Path("search")
	@POST
	@Produces("text/N3")
	public Response searchN3_p(@FormParam("input") String input,  @Context ServletContext cont, @Context UriInfo info ){
		Model out = getOutput(input, cont, info);
		OutputStream outstr = new ByteArrayOutputStream();
		out.write(outstr, "N3");
		return Response.ok(outstr.toString()).build();
	}
	@Path("search")
	@POST
	@Produces("text/html")
	public Response searchHTML_p(@FormParam("input") String input,  @Context ServletContext cont, @Context UriInfo info ){
		Model out = getOutput(input, cont, info);
		OutputStream outstr = new ByteArrayOutputStream();
		out.write(outstr, "N3");
		return Response.ok("Output:<br><TEXTAREA COLS=\"80\" ROWS=\"35\">"
				+outstr.toString()
				+"</TEXTAREA>").build();
	}
	
	@Path("search/description")
	@GET
	@Produces("text/html")
	public Response descHTML(@Context ServletContext cont, @Context UriInfo info ){
		String ret = readFile("/files/description.html", cont)
		.replace("PLACE_NS" , 
				ServiceDescriptionLab.getNS(readFile("/files/description.rdf", cont)
						.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/")) )
		.replace("PLACE_INPUT" , 
				ServiceDescriptionLab.getInput(readFile("/files/description.rdf", cont)
						.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/")) )
		.replace("PLACE_OUTPUT" , 
				ServiceDescriptionLab.getOutput(readFile("/files/description.rdf", cont)
						.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/")) )				
		;
		return Response.ok(ret).build();
	}
	@Path("search/description")
	@GET
	@Produces("application/rdf+xml")
	public Response descXML(@Context ServletContext cont, @Context UriInfo info ){
		return Response.ok( ServiceDescriptionLab.getDescAsXML(readFile("/files/description.rdf", cont)
								.replace("REPLACE_ME", info.getBaseUri().toString()+"schema/" )	)  
						).build();
	}
	@Path("search/description")
	@GET
	@Produces("text/N3")
	public Response descN3(@Context ServletContext cont, @Context UriInfo info ){
		return Response.ok( ServiceDescriptionLab.getDescAsN3(readFile("/files/description.rdf", cont)
								.replace("REPLACE_ME", info.getBaseUri()+"schema/" )	)  
						).build();
	}
	
	@Path("tco")
	@GET
	@Produces("text/html")
	public Response tcoHTML(@Context ServletContext cont, @Context UriInfo info ){
		Map<Integer, String> map =getTopCo(cont);
		String rep = "<table border=\"0\">";
		rep += "<tr><td>No.</td><td></td><td>Occured Overall</td></tr>";
		for(int i = 1; i<=100; i++){
			String tmp = map.get(i);
			rep += "<tr><td>"+i+"</td><td><a href=\""+tmp.split("\t")[1]+"\"	>"+tmp.split("\t")[1]+"</a></td><td>"+tmp.split("\t")[0]+"</td></tr>";
		}
		rep += "</table>";
		
		String resp = readFile("/files/resp.html", cont).replace("REPLACE_QUERY", "").replace("REPLACE_OCCUR", "").replace("REPLACE_POSITION", "");
		resp= resp.replace("REPLACE_KIND", rep);
		return Response.ok( resp).build();
	}
	
	@Path("tcd")
	@GET
	@Produces("text/html")
	public Response tcdHTML(@Context ServletContext cont, @Context UriInfo info ){
		Map<Integer, String> map =getTopCd(cont);
		String rep = "<table border=\"0\">";
		rep += "<tr><td>No.</td><td></td><td>Occured in Datasets</td></tr>";
		for(int i = 1; i<=100; i++){
			String tmp = map.get(i);
			rep += "<tr><td>"+i+"</td><td><a href=\""+tmp.split("\t")[1]+"\">"+tmp.split("\t")[1]+"</a></td><td>"+tmp.split("\t")[0]+"</td></tr>";
		}
		rep += "</table>";
		
		String resp = readFile("/files/resp.html", cont).replace("REPLACE_QUERY", "").replace("REPLACE_OCCUR", "").replace("REPLACE_POSITION", "");
		resp= resp.replace("REPLACE_KIND", rep);
		return Response.ok( resp).build();
	}
	
	@Path("tpo")
	@GET
	@Produces("text/html")
	public Response tpoHTML(@Context ServletContext cont, @Context UriInfo info ){
		Map<Integer, String> map =getTopPo(cont);
		String rep = "<table border=\"0\">";
		rep += "<tr><td>No.</td><td></td><td>Occured Overall</td></tr>";
		for(int i = 1; i<=100; i++){
			String tmp = map.get(i);
			rep += "<tr><td>"+i+"</td><td><a href=\""+tmp.split("\t")[1]+"\">"+tmp.split("\t")[1]+"</a></td><td>"+tmp.split("\t")[0]+"</td></tr>";
		}
		rep += "</table>";
		
		String resp = readFile("/files/resp.html", cont).replace("REPLACE_QUERY", "").replace("REPLACE_OCCUR", "").replace("REPLACE_POSITION", "");
		resp= resp.replace("REPLACE_KIND", rep);
		return Response.ok( resp).build();
	}
	
	@Path("tpd")
	@GET
	@Produces("text/html")
	public Response tpdHTML(@Context ServletContext cont, @Context UriInfo info ){
		Map<Integer, String> map =getTopPd(cont);
		String rep = "<table border=\"0\">";
		rep += "<tr><td>No.</td><td></td><td>Occured in Datasets</td></tr>";
		for(int i = 1; i<=100; i++){
			String tmp = map.get(i);
			rep += "<tr><td>"+i+"</td><td><a href=\""+tmp.split("\t")[1]+"\">"+tmp.split("\t")[1]+"</a></td><td>"+tmp.split("\t")[0]+"</td></tr>";
		}
		rep += "</table>";
		
		String resp = readFile("/files/resp.html", cont).replace("REPLACE_QUERY", "").replace("REPLACE_OCCUR", "").replace("REPLACE_POSITION", "");
		resp= resp.replace("REPLACE_KIND", rep);
		return Response.ok( resp).build();
	}
	
	/*
	 * HELPER
	 */	
	private Model getOutput(String input, ServletContext cont, UriInfo info){
		Model out = ModelFactory.createDefaultModel();
		String queryS = "" +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
				"SELECT ?uri ?type " +
				"WHERE { " +
				"		{ ?uri a ?type . FILTER regex( str(?type),  \"http://www.w3.org/1999/02/22-rdf-syntax-ns#Property\") } " +
				"		UNION " +
				"		{ ?uri a ?type . FILTER regex( str(?type),  \"http://www.w3.org/2000/01/rdf-schema#Class\") } " +
				"	   }" ;
		Model in = ModelFactory.createDefaultModel();
		in.read(new StringReader(input), null, null);
		Query query = QueryFactory.create(queryS);
		QueryExecution queryE = QueryExecutionFactory.create(query, in);
		ResultSet set = queryE.execSelect();
		while(set.hasNext()){
			QuerySolution qs = set.next();
			RDFNode uri = qs.get("?uri");
			RDFNode type = qs.get("?type");
		
			out.add(getOutputFromValue(uri.toString(), type.toString(), cont, info));
		}
		return out;
	}
	
	//Construct RDF
	private Model getOutputFromValue(String uri, String type, ServletContext cont, UriInfo info){
		Model out = ModelFactory.createDefaultModel();
		String model = "@prefix voc: <"+info.getBaseUri()+"schema/> . ";		
		
		String temp;

		if(type.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")){
			
			temp = lookupP(uri, cont);
			
			if(temp == null){
				model += "" +
						"<"+uri+"> voc:usedAsProperty \"0\" . " +
						"<"+uri+"> voc:propertyUsedInDatasets \"0\" . "; 
			}else{
				String pPosO = temp.split("\t")[0];
				String pNO = temp.split("\t")[1];
				String pPosD = temp.split("\t")[2];
				String pND = temp.split("\t")[3];
				model += "" +
					"<"+uri+"> voc:usedAsProperty \""+pNO+"\" . " +
					"<"+uri+"> voc:posInPropRanking \""+pPosO+"\" . " +
					"<"+uri+"> voc:posInPropDatasetRanking \""+pPosD+"\" . " +
					"<"+uri+"> voc:propertyUsedInDatasets \""+pND+"\" . "; 
			}
		}
		if(type.equals("http://www.w3.org/2000/01/rdf-schema#Class")){
			temp = lookupC( uri, cont);

			if(temp == null){
				model += "" +
						"<"+uri+"> voc:usedAsClass \"0\" . " +
						"<"+uri+"> voc:classUsedInDatasets \"0\" . "; 
			}else{
				String cPosO = temp.split("\t")[0];
				String cNO = temp.split("\t")[1];
				String cPosD = temp.split("\t")[2];
				String cND = temp.split("\t")[3];
				model += "" +
					"<"+uri+"> voc:usedAsClass \""+cNO+"\" . " +
					"<"+uri+"> voc:posInClassRanking \""+cPosO+"\" . " +
					"<"+uri+"> voc:posInClassDatasetRanking \""+cPosD+"\" . " +
					"<"+uri+"> voc:classUsedInDatasets \""+cND+"\" . "; 
			}
		}
		
		out.read(new StringReader(model), null, "N3");
		return out;
	}
	
	//lookup uri in class data files
	private String lookupC(String uri, ServletContext cont){
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
		Map<String, String> cd  = (Map<String, String>) cont.getAttribute(Listener.mapP);
		String temp = null;
		temp = cd.get(uri);
		if(temp==null || temp.isEmpty()){
			return null;
		}
		return temp;
	}
	
	
	//create not found page
	private Response notFound(String f, ServletContext cont){
		String ret = readFile("/files/resp.html", cont).replace("REPLACE_QUERY", f)
			.replace("REPLACE_KIND", "")
			.replace("REPLACE_OCCUR", "could not be found")
			.replace("REPLACE_POSITION", "");
		return Response.ok(ret).build();
	}
	
	
	//get top 100
	private static Map<Integer, String> getTopCo(ServletContext cont) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		
		Set<Entry<String, String>> temp = ((Map<String, String>) cont.getAttribute(Listener.mapC)).entrySet();
		for(Entry<String, String> e : temp){
			int pos = Integer.valueOf(e.getValue().split("\t")[0]);
			if(pos<=100){
				map.put(pos, e.getValue().split("\t")[1]+"\t"+e.getKey());
			}
		}
		return map;
	}
	private static Map<Integer, String> getTopCd(ServletContext cont) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		
		Set<Entry<String, String>> temp = ((Map<String, String>) cont.getAttribute(Listener.mapC)).entrySet();
		for(Entry<String, String> e : temp){
			int pos = Integer.valueOf(e.getValue().split("\t")[2]);
			if(pos<=100){
				map.put(pos, e.getValue().split("\t")[3]+"\t"+e.getKey());
			}
		}
		return map;
	}
	private static Map<Integer, String> getTopPo(ServletContext cont) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		
		Set<Entry<String, String>> temp = ((Map<String, String>) cont.getAttribute(Listener.mapP)).entrySet();
		for(Entry<String, String> e : temp){
			int pos = Integer.valueOf(e.getValue().split("\t")[0]);
			if(pos<=100){
				map.put(pos, e.getValue().split("\t")[1]+"\t"+e.getKey());
			}
		}
		return map;
	}
	private static Map<Integer, String> getTopPd(ServletContext cont) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		
		Set<Entry<String, String>> temp = ((Map<String, String>) cont.getAttribute(Listener.mapP)).entrySet();
		for(Entry<String, String> e : temp){
			int pos = Integer.valueOf(e.getValue().split("\t")[2]);
			if(pos<=100){
				map.put(pos, e.getValue().split("\t")[3]+"\t"+e.getKey());
			}
		}
		return map;
	}
		
	
	
	//read file as String
	private static String readFile(String in, ServletContext cont) {
		File file = new File(cont.getRealPath("/WEB-INF"+in));
		FileReader reader;
		try {
			reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
	
		    StringBuffer sb = new StringBuffer();
		    String eachLine = br.readLine();
	
		    while (eachLine != null) {
		      sb.append(eachLine);
		      sb.append("\n");
		      eachLine = br.readLine();
		    }
		    return sb.toString();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private String beauStr(String str){
		String out = "";
		int q = 1;
		for(int i = str.length(); i>0; i--){
			
			if(q % 3 == 0){
				out = " "+str.charAt((i-1))+out;
			}else{
				out = str.charAt((i-1))+out;
			}
			q +=1;
		}
		return out;
	}
}
