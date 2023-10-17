package eu.sealsproject.omt.client.interactive;

import eu.sealsproject.omt.client.HashAlignment;
import eu.sealsproject.omt.client.Relation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Simulates user interaction through the use of a reference alignment
 * with an optional error rate
 * @author Daniel Faria, Dominique Ritze
 */
public class Oracle
{	
	private static HashAlignment refAlign;
	private static double error = 0.0;
	private static boolean interactive = false;
	private static HashAlignment positive;
	private static HashAlignment negative;
	private static Vector<Long> timeIntervals;
	private static long previousTime;
	private static String outRawResultFolder;
	private static String testCaseId;
	private static File queryLog;
	private static File timeLog;
	private static File results;
	private static int[][] oracleClassMatrix;
	private static int count;
	private static int totalCount;
	private static int totalDistinctCount;
	
	/**
	 * Checks whether a given mapping is correct
	 * @param m: the Mapping to check
	 * @return whether the mapping is correct
	 */
	public static boolean check(Mapping m)
	{
		return check(m.getSourceURI(),m.getTargetURI(),m.getRelation());
	}
	
	/**
	 * Checks whether a given mapping is correct
	 * @param uri1: the URI of the source ontology entity
	 * @param uri2: the URI of the target ontology entity
	 * @param rel: the mapping relation in String form ("=", ">", or "<")
	 * @return whether the mapping is true
	 */
	public static boolean check(String uri1, String uri2, String rel)
	{
		Relation r = Relation.parse(rel);
		return check(uri1,uri2,r);
	}
	
	/**
	 * Checks whether a given mapping is correct
	 * @param uri1: the URI of the source ontology entity
	 * @param uri2: the URI of the target ontology entity
	 * @param rel: the mapping Relation
	 * @return
	 */
	public static boolean check(String uri1, String uri2, Relation rel)
	{
		//Check interactive
		if(!interactive)
			return false;
		//Record time since previous request
		long time = System.currentTimeMillis();
		if(previousTime != -1)
			timeIntervals.add(time - previousTime);
		previousTime = time;
		count++;
		totalCount++;
		//If the query was already done, return the result
		if(positive.contains(uri1, uri2, rel))
			return true;
		if(negative.contains(uri1, uri2, rel))
			return false;
		//Otherwise, if the mapping between uri1 and uri2 is 'unknown' in the
		//reference alignment return false by default, but do not store it or
		//count it as a query (it will also not count in the evaluation)
		if(refAlign.contains(uri1, uri2, Relation.UNKNOWN))
		{
			negative.add(uri1, uri2, rel);
			return false;
		}
		//Check if the query is present in the reference alignment
		boolean classification = refAlign.contains(uri1,uri2,rel);
		//Reverse the classification with probability given by the error
		if(Math.random() < error)
			classification = !classification;
		//Store the request
		if(classification)
			positive.add(uri1, uri2, rel);
		else
			negative.add(uri1, uri2, rel);
		return classification;
	}
	
	/**
	 * Checks which of a set of conflicting Mappings are correct
	 * @param maps: the set of Mappings to check
	 * @return the subset of mappings that are correct
	 */
	public static Set<Mapping> check(Set<Mapping> maps)
	{
		//Check each mapping in the given
		Set<Mapping> correct = new HashSet<Mapping>();
		for(Mapping m : maps)
			if(check(m))
				correct.add(m);
		//If a set of 2 or 3 conflicting mappings was given
		//reduce the count so that they are counted as a
		//group rather than as individual mappings
		if(maps.size() > 1 && maps.size() < 4)
		{
			int related = 0;
			Vector<Mapping> mapList = new Vector<Mapping>(maps);
			for(int i = 0; i < mapList.size()-1 && related < 2; i++)
			{
				for(int j = i+1; j < mapList.size(); j++)
				{
					if(mapList.get(i).getSourceURI().equals(mapList.get(j).getSourceURI()) ||
							mapList.get(i).getTargetURI().equals(mapList.get(j).getTargetURI()))
						related++;
				}
			}
			count -= related;
			totalCount -= related;
		}
		//Return the set of correct mappings
		return correct;
	}
	
	/**
	 * @return the confidence of the Oracle (1 - error rate)
	 */
	public double confidence()
	{
		return 1.0 - error;
	}

	//Ends a matching suite from an OAEI track
	//Saves the logs and statistics, then reset the task variables 
	//WARNING: Internal Client use only
	public static void endTask()
	{
		//Ensure that this method was called from the Client by checking the stack trace
		if(!Thread.currentThread().getStackTrace()[2].toString().startsWith("eu.sealsproject.omt.client.Client"))
		{
			System.out.println("WARNING: Illegal access to Oracle class!");
			System.exit(-1);
		}
		if(outRawResultFolder != null && testCaseId != null)
		{
			//Save the query log and compile the classification matrix
			BufferedWriter writer;
			int[][] classMatrix = new int[2][2];
			try
			{
				writer = new BufferedWriter(new FileWriter(queryLog, true));
				writer.append(testCaseId + "\n");
				for(String source : positive.getSources())
				{
					for(String target : positive.getTargets(source))
					{
						for(Relation r : positive.getRelations(source,target))
						{
							//True positives
							if(refAlign.contains(source, target, r) || refAlign.contains(source, target, Relation.UNKNOWN))
							{
								writer.append(source + " " + r.toString() + " " + target + "\tTP\n");
								classMatrix[0][0]++;
								oracleClassMatrix[0][0]++;
							}
							//False positives
							else
							{
								writer.append(source + " " + r.toString() + " " + target + "\tFP\n");
								classMatrix[0][1]++;
								oracleClassMatrix[0][1]++;
							}
						}
					}
				}
				for(String source : negative.getSources())
				{
					for(String target : negative.getTargets(source))
					{
						for(Relation r : negative.getRelations(source,target))
						{
							//False negatives
							if(refAlign.contains(source, target, r))
							{
								writer.append(source + " " + r.toString() + " " + target + "\tFN\n");
								classMatrix[1][1]++;
								oracleClassMatrix[1][1]++;
							}
							//True negatives
							else
							{
								writer.append(source + " " + r.toString() + " " + target + "\tTN\n");
								classMatrix[1][0]++;
								oracleClassMatrix[1][0]++;
							}
						}
					}
				}
				writer.flush();
				writer.close();
			}
			catch(IOException e)
			{
				System.err.println("Error writing query log file: " + e.getMessage());
				e.printStackTrace();
			}
			//Save the time interval log
			try
			{
				writer = new BufferedWriter(new FileWriter(timeLog, true));
				writer.append(testCaseId + "\n");
				for(Long interval : timeIntervals)
					writer.append(interval + "\n");
				writer.flush();
				writer.close();
			}
			catch(IOException e)
			{
				System.err.println("Error writing time interval log file: " + e.getMessage());
				e.printStackTrace();
			}
			//Save the statistics
			try
			{
				writer = new BufferedWriter(new FileWriter(results, true));
				int distinct = positive.size()+negative.size();
				totalDistinctCount += distinct;
				double precision = Math.min(Math.round(classMatrix[0][0] * 1000.0 / positive.size())/1000.0, 1.0);
				double negPrecision = Math.min(Math.round(classMatrix[1][0] * 1000.0 / negative.size())/1000.0, 1.0);
				writer.append(testCaseId + "\t" + count + "\t" + distinct + "\t" + classMatrix[0][0] + "\t" +
						classMatrix[1][0] + "\t" + classMatrix[0][1] + "\t" + classMatrix[1][1] +
						"\t" + precision + "\t" + negPrecision + "\n");
				writer.flush();
				writer.close();
			}
			catch(IOException e)
			{
				System.err.println("Error writing results file: " + e.getMessage());
				e.printStackTrace();
			}	

		}
		refAlign = null;
		testCaseId = null;
		timeIntervals = null;
		interactive = false;
	}
	
	//Ends a matching suite from an OAEI track
	//WARNING: Internal Client use only
	public static void endSuite()
	{
		//Ensure that this method was called from the Client by checking the stack trace
		if(!Thread.currentThread().getStackTrace()[2].toString().startsWith("eu.sealsproject.omt.client.Client"))
		{
			System.out.println("WARNING: Illegal access to Oracle class!");
			System.exit(-1);
		}
		if(outRawResultFolder != null)
		{
			try
			{
				BufferedWriter writer = new BufferedWriter(new FileWriter(results, true));
				double precision = Math.min(Math.round(oracleClassMatrix[0][0] * 1000.0 / positive.size())/1000.0, 1.0);
				double negPrecision = Math.min(Math.round(oracleClassMatrix[1][0] * 1000.0 / negative.size())/1000.0, 1.0);
				writer.append("Global\t" + totalCount + "\t" + totalDistinctCount + "\t" + oracleClassMatrix[0][0] + "\t" +
						oracleClassMatrix[1][0] + "\t" + oracleClassMatrix[0][1] + "\t" + oracleClassMatrix[1][1] +
						"\t" + precision + "\t" + negPrecision + "\n");
				writer.flush();
				writer.close();
			}
			catch(IOException e)
			{
				System.err.println("Error writing results file: " + e.getMessage());
				e.printStackTrace();
			}
			outRawResultFolder = null;
		}
		oracleClassMatrix = null;
	}
	
	/**
	 * @return the reference alignment according to the Oracle
	 * (i.e., with false positives and negatives introduced
	 * according to the error rate).
	 */
	public static HashAlignment getOracleReference()
	{
		//Initiate as a copy of the true reference
		HashAlignment oracleAlign = new HashAlignment(refAlign);
		//Add false positives
		for(String source : positive.getSources())
			for(String target : positive.getTargets(source))
				for(Relation r : positive.getRelations(source,target))
					if(!refAlign.contains(source, target, r))
						oracleAlign.add(source, target, r);
		//Remove false negatives
		for(String source : negative.getSources())
			for(String target : negative.getTargets(source))
				for(Relation r : negative.getRelations(source,target))
					if(refAlign.contains(source, target, r))
						oracleAlign.remove(source, target, r);
		return oracleAlign;
	}
	
	/**
	 * @return whether the track interactive
	 */
	public static boolean isInteractive()
	{
		return interactive;
	}
	
	//Starts a matching suite for an OAEI track
	//WARNING: Internal Client use only
	public static void startSuite(double e, String folder)
	{
		//Ensure that this method was called from the Client by checking the stack trace
		if(!Thread.currentThread().getStackTrace()[2].toString().startsWith("eu.sealsproject.omt.client.Client"))
		{
			System.out.println("WARNING: Illegal access to Oracle class!");
			System.exit(-1);
		}
		error = e;
		oracleClassMatrix = new int[2][2];
		outRawResultFolder = folder;
		totalCount = 0;
		totalDistinctCount = 0;
		if(outRawResultFolder != null)
		{
			File resultsFolder = new File(outRawResultFolder);
			resultsFolder.mkdir();
			queryLog = new File(resultsFolder, "interactive_query_log.txt");
			if(queryLog.exists())
				queryLog.delete();
			timeLog = new File(resultsFolder, "interactive_request_intervals.txt");
			if(timeLog.exists())
				timeLog.delete();
			results = new File(resultsFolder, "interactive_results.txt");
			if(results.exists())
				results.delete();
			try
			{
				BufferedWriter writer = new BufferedWriter(new FileWriter(results, true));
				writer.append("Test Case ID\tTotal Requests\tDistinct Requests\tTrue Positives\tTrue Negatives\tFalse Positives\tFalse Negatives\tPrecision\tNegative Precision\n");
				writer.flush();
				writer.close();
			}
			catch(IOException e1)
			{
				System.err.println("Error writing results file: " + e1.getMessage());
				e1.printStackTrace();
			}
		}
	}

	//Starts a matching task for an OAEI track
	//WARNING: Internal Client use only
	public static void startTask(HashAlignment referenceAlignment, String id)
	{
		//Ensure that this method was called from the Client by checking the stack trace
		if(!Thread.currentThread().getStackTrace()[2].toString().startsWith("eu.sealsproject.omt.client.Client"))
		{
			System.out.println("WARNING: Illegal access to Oracle class!");
			System.exit(-1);
		}
		count = 0;
		refAlign = referenceAlignment;
		testCaseId = id;
		interactive = true;
		previousTime = -1;
		timeIntervals = new Vector<Long>();
		positive = new HashAlignment();
		negative = new HashAlignment();
	}
}