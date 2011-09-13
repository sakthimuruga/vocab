package cc.vocab.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.ws.rs.core.UriInfo;

public class VocUtils {
	//read file as String
	protected static String readFile(String in, ServletContext cont) {
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
	
	protected static String makeLink (String query, UriInfo info) throws UnsupportedEncodingException{
		String  link = "<a href=\""+info.getBaseUri()+"lookup?query="+URLEncoder.encode(query,"UTF-8")+"\">"+query+"</a>" +
				"<a class=\"namespace-link\" href=\""+query+"\" rel=\"nofollow\"> <img src=\""+info.getBaseUri()+"img/link.png\" title=\"go to the vocabulary\" /></a> ";
		return link;
	}
	protected static String makeLink_sm (String query, UriInfo info) throws UnsupportedEncodingException{
		String  link = "<a href=\""+info.getBaseUri()+"lookup?query="+URLEncoder.encode(query,"UTF-8")+"\">"+query+"</a>" +
				"<a class=\"namespace-link\" href=\""+query+"\" rel=\"nofollow\"> <img src=\""+info.getBaseUri()+"img/link_sm.png\" title=\"go to the vocabulary\" /></a> ";
		return link;
	}
}
