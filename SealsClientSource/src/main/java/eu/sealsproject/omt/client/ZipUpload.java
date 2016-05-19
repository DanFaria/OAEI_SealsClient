package eu.sealsproject.omt.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import eu.sealsproject.platform.repos.common.AlreadyExistsException;
import eu.sealsproject.platform.repos.common.NotExistsException;
import eu.sealsproject.platform.repos.common.RepositoryException;
import eu.sealsproject.platform.repos.common.ViolatedConstraintException;

public class ZipUpload {
	
	   RRSClient client; 
	
  	   public ZipUpload() {
  		      this.client = new RRSClient();  
	   }
	   
  	   /*
  	    * Type = rr or ir
  	    */
	   public boolean zip(String path, String zip) {
		   
		      FileOutputStream dest = null;
		      BufferedInputStream origin = null;
		      final int BUFFER = 2048;
		      
		      //System.out.println(path);
		      //System.out.println(zip);
		     

			  try {
				  
				  dest = new FileOutputStream(zip);
				  ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
				  byte data[] = new byte[BUFFER];
				  
	              File f = new File(path);
	      	              
			      String files[] = f.list();
			      for (int i=0; i<files.length; i++) {
			          
			           if (files[i].endsWith(".rdf")) {
			        	      //System.out.println("Adding: "+ path + File.separator + files[i]);
			            	  FileInputStream fi = new FileInputStream(path + File.separator + files[i]);
			            	  origin = new BufferedInputStream(fi, BUFFER);
			            	  ZipEntry entry = new ZipEntry(files[i]);
			            	  out.putNextEntry(entry);
			            	  
			            	  int count;
			                  while((count = origin.read(data, 0, 
			                    BUFFER)) != -1) {
			                     out.write(data, 0, count);
			                  }
			                  origin.close();
					    }
			      }
			      out.close();
			      return true;
			      
			  } catch (FileNotFoundException e) {
				        System.err.println("File not found " + e.getMessage());
			  } catch (IOException e) {
			    	   System.err.println("Error adding file " + e.getMessage());
			  }
   	          return false;
	   }
	   	  
	   
	   public boolean upload(String type, String pathSuiteZIP, String repMetadata) {
		   
		      try {		    		  
		    		  if (type.equalsIgnoreCase("rr")) { 
		    		      client.addRawResult(repMetadata, new File(pathSuiteZIP));
		    		  } 
		    		  
		    		  if (type.equalsIgnoreCase("ir")) { 
		    		      client.addInterpretation(repMetadata, new File(pathSuiteZIP));
		    		  } 
		    		  
		    		  return true;
		    	  } catch (RepositoryException e) {
					   	  System.err.println(e.getMessage());
		    	  } catch (ViolatedConstraintException e) {
		    		  System.err.println(e.getMessage());
		    	  } catch (NotExistsException e) {
		    		  System.err.println(e.getMessage());
		    	  } catch (AlreadyExistsException e) {
		    		  System.err.println(e.getMessage());
		    	  }
		      return false;
	   }
}
