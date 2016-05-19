package eu.sealsproject.omt.client;

import java.io.*;

import eu.sealsproject.platform.repos.common.AlreadyExistsException;
import eu.sealsproject.platform.repos.common.NotExistsException;
import eu.sealsproject.platform.repos.common.RepositoryException;
import eu.sealsproject.platform.repos.common.ViolatedConstraintException;
import eu.sealsproject.platform.repos.rrs.client.RrsClient;

/** 
 *  @author Cassia Trojahn
 * 
 * */
public class RRSClient {
	
	public static final String RESULTS_REPOSITORY_URL = "http://seals-test.sti2.at/rrs-web/";
	private RrsClient client = new RrsClient(RESULTS_REPOSITORY_URL);
	
	public RRSClient() {
		
	}
	
	/**
	 * 
	 * @param metadata
	 * @param zip
	 * @return
	 * @throws RepositoryException
	 * @throws ViolatedConstraintException
	 * @throws NotExistsException
	 * @throws AlreadyExistsException
	 */
	public boolean addRawResult(String metadata, File zip) throws RepositoryException,
	                                                 ViolatedConstraintException, 
	                                                 NotExistsException,
	                                                 AlreadyExistsException {
          return client.addRawResult(new StringReader(metadata), zip);
    }
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public String retrieveRawResultMetadata(String id) {
           return client.retrieveRawResultMetadata(id);
    }

	/**
	 * TODO
	 * @param id
	 */
    public void retrieveRawResultData(String id) {
           InputStream input = client.retrieveRawResultData(id);
           if (input != null) {
        	   try {
        		   input.close();
        	   } catch (IOException e) {
		           // TODO Auto-generated catch block
	 	           e.printStackTrace();
        	   }
           }
    }	
    
    /**
     * 
     * 
     */
    public boolean removeRawResult(String id) {
           return client.removeRawResult(id);
    }

    /**
     * 
     * @param metadata
     * @param zip
     * @return
     * @throws RepositoryException
     * @throws ViolatedConstraintException
     * @throws NotExistsException
     * @throws AlreadyExistsException
     */
    public boolean addInterpretation(String metadata, File zip) throws RepositoryException,
	                                                                ViolatedConstraintException, 
	                                                                NotExistsException,
	                                                                AlreadyExistsException
    {
           return client.addInterpretation(new StringReader(metadata),zip);
    }
    
    /**
     * 
     * @param id
     * @return
     */
    public String retrieveInterpretationMetadata(String id) {
          return client.retrieveInterpretationMetadata(id);
    }

    /**
     * TODO
     * @param id
     */
    public void retrieveInterpretationData(String id) {
    		InputStream input = client.retrieveRawResultData(id);

    		if (input != null) {
    			try {
    				input.close();
    			} catch (IOException e) {
    				 	 e.printStackTrace();
    			}
    		}
    }
    
     /**
      * 
      */
     public boolean removeInterpretation(String id) {
    	 return client.removeInterpretation(id);
     }

}
