package cc.vocab.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Listener implements ServletContextListener {
	
	static String mapC = "mapCAttribute";
	static String mapP = "mapPAttribute";
	static String prefixes = "prefAttribute";
	static String searchMap = "searchMapAttribute";
	public static SimpleDateFormat RFC822 = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext ctx = event.getServletContext();
		Map<String, String> mapCval =  getMap( "C", ctx);
		Map<String, String> mapPval =  getMap( "P", ctx);
		
		ctx.setAttribute(mapC, mapCval);
		ctx.setAttribute(mapP, mapPval);
		ctx.setAttribute(prefixes, loadPrf(ctx));
		
		//ctx.setAttribute(searchMap, getSMap(ctx));
	
		
		//System.out.println(mapCoM.get("http://xmlns.com/foaf/0.1/Person"));
	}
	
	
	//read search Maps
	private static Map<String, String> getSMap(ServletContext cont){
		String in = "/files/map";
		Map<String, String> map = new HashMap<String, String>();
		
		int i = 0;
		File fileO = new File(cont.getRealPath("/WEB-INF"+in+"_"+i));
		
		do{
			FileReader reader;
				try {
					reader = new FileReader(fileO);
					BufferedReader br = new BufferedReader(reader);
			
				    String eachLine = br.readLine();
			
				    while (eachLine != null) {
				    	
				    	String[] a = eachLine.split("\t");
				    		
				    	map.put(a[0], a[1]);
				    	eachLine = br.readLine();
				    }
				    i+=1;
				    fileO = new File(cont.getRealPath("/WEB-INF"+in+"_"+i));
				    
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}while(fileO.exists());
		
		return map;
	}

	
	//read RDF analysis data files
	private static Map<String, String> getMap(String dec, ServletContext cont) {
		
		String inO = "";
		String inD = "";
		
		if(dec.equals("C")){
			inO = "/files/co";
			inD = "/files/cd";
		}
		if(dec.equals("P")){
			inO = "/files/po";
			inD = "/files/pd";
		}
		
		Map<String, String> map = new HashMap<String, String>();
		
		int i = 0;
		File fileO = new File(cont.getRealPath("/WEB-INF"+inO+"_"+i));
		
		
		//read in data from overall measurements
		do{
		FileReader reader;
			try {
				reader = new FileReader(fileO);
				BufferedReader br = new BufferedReader(reader);
		
			    String eachLine = br.readLine();
		
			    while (eachLine != null) {
			    	
			    	String[] a = eachLine.split("\t");
			    		
			    	map.put(a[2], a[0]+"\t"+a[1]);
			    	eachLine = br.readLine();
			    }
			    i+=1;
			    fileO = new File(cont.getRealPath("/WEB-INF"+inO+"_"+i));
			    
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}while(fileO.exists());
		
		int q = 0;
		File fileD = new File(cont.getRealPath("/WEB-INF"+inD+"_"+q));
		//read in data from dataset relative measurements
		do{
			FileReader reader;
				try {
				    reader = new FileReader(fileD);
				    BufferedReader br = new BufferedReader(reader);
			
				    String eachLine = br.readLine();
			
				    while (eachLine != null) {
				    	
				    	String[] a = eachLine.split("\t");
				    	
				    	String temp = map.get(a[2]);
				    	temp += "\t"+a[0]+"\t"+a[1];
				    	map.put(a[2], temp);
				    	eachLine = br.readLine();
				    }
				    q+=1;
				    fileD = new File(cont.getRealPath("/WEB-INF"+inD+"_"+q));
			    
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while(fileD.exists());
		return map;
	}
	
	
	//load prefixes
	private static Map<String, String> loadPrf(ServletContext ctx){
		List<String> allpopulartxt = new ArrayList<String>();
		try {
			
			URL u = new URL("http://prefix.cc/popular/all.file.txt");
			HttpURLConnection conn = (HttpURLConnection)u.openConnection();
			conn.setConnectTimeout(2000);
			conn.setReadTimeout(2000);
			int status = conn.getResponseCode();
			if (status != 200) {
				conn.disconnect();
				throw new IOException("not 200");
			} else {
				InputStream is = conn.getInputStream();

				BufferedReader br = new BufferedReader(new InputStreamReader(is));

				String line = null;
				while ((line = br.readLine()) != null) {
					allpopulartxt.add(line);
				}
				
				is.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			allpopulartxt.clear();

			InputStream in = null;
			
			try {
				in = new FileInputStream(ctx.getRealPath("/WEB-INF/files/prefixes"));
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				String line = null;
				while ((line = br.readLine()) != null) {
					allpopulartxt.add(line);
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		
		Map<String, String> prefixes = new HashMap<String, String>();
		for (String line : allpopulartxt) {
			StringTokenizer tok = new StringTokenizer(line, "\t");

			String pre = null, ns = null;

			if (tok.hasMoreTokens()) {
				pre = tok.nextToken().trim();
				if (tok.hasMoreTokens()) {
					ns = tok.nextToken().trim();
				}
			}

			if (pre != null && ns != null) {
				if (!prefixes.containsKey(ns)) {
					prefixes.put(pre, ns);
				}
			}
		}
		
		return prefixes;
	}

}
