package eu.sealsproject.omt.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;

import eu.sealsproject.domain.omt.metadata.rrs.Content;
import eu.sealsproject.omt.client.interactive.Oracle;
import eu.sealsproject.platform.repos.tdrs.client.TestCaseIterator;
import eu.sealsproject.platform.repos.tdrs.client.TestDataSuite;
import eu.sealsproject.platform.res.domain.omt.IOntologyMatchingToolBridge;
import eu.sealsproject.platform.res.tool.api.ToolBridgeException;
import eu.sealsproject.platform.res.tool.api.ToolException;
import eu.sealsproject.platform.res.tool.bundle.api.IToolPackage;
import eu.sealsproject.platform.res.tool.bundle.factory.PackageCreationException;
import eu.sealsproject.platform.res.tool.bundle.factory.ToolPackageFactory;
import eu.sealsproject.platform.res.tool.bundle.loaders.ToolBridgeLoadingException;
import eu.sealsproject.platform.res.tool.bundle.loaders.impl.ToolBridgeLoader;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

/**
 * Basic tool client to be used locally by tool developers and OAEI track organizers.
 * The current version is explained and documented in the tutorial v3 available at
 * http://oaei.ontologymatching.org/2011.5/seals-eval.html. There you will also find
 * the download link to the jar with all dependencies.
 * 
 * @author Daniel Faria, Christian Meilicke, Dominique Ritze, Cassia Trojahn
 */
public class Client
{
	//Client Version & Date
	private static final String VERSION = "7.0";
	private static final String DATE = "09-06-2016";
	//Production server
	private static final String DEFAULT_TDRS_URL = "http://repositories.seals-project.eu/tdrs/";
	//Links to the ontologies and reference alignment for the default test
	private static final String DEFAULT_PREDEFINED_SOURCE = "testdata/persistent/conference/conference-v1/suite/cmt-ekaw/component/source/";
	private static final String DEFAULT_PREDEFINED_TARGET = "testdata/persistent/conference/conference-v1/suite/cmt-ekaw/component/target/";
	private static final String DEFAULT_PREDEFINED_REFERENCE = "testdata/persistent/conference/conference-v1/suite/cmt-ekaw/component/reference/";
	//Client Modes
	public enum Mode
	{
		PREDEF, PREDEFI, PARAM, PARAMI, SUITE
	}
	//Parameters
	private static Mode mode = null;
	private static String outRawResultFolder = null;
	private static URL source = null;
	private static URL target = null;
	private static URL reference = null;
	private static URL input = null;
	private static URL alignment = null;
	private static String tdrsLocation = null;
	private static String testDataCollectionName = null;
	private static String testDataVersionNumber = null;
	private static String testCaseId = null;
	private static String resultsId = null;
	private static String toolName = null;
	private static boolean interactive = false;
	private static boolean automated = false;
	private static boolean skipTestsWithoutRefAlign = true;
	private static IOntologyMatchingToolBridge bridge;
	private static File packagePath;
	private static File outputFile = null;
	//Reference Alignments (normal and oracle)
	private static HashAlignment refAlign = null;
	private static HashAlignment oracleAlign = null;
	//Evaluation Parameters
	private static long runTime = 0;
	private static long totalRunTime = 0;
	private static int[] classification = null;
	private static int[] oracleClassification = null;

	public static void main(String[] args)
	{
		//Process the arguments:
		//Check if help was called
		if(args.length == 1 && (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("--help")))
			printHelpMessage();
		//Otherwise exit if the number of parameters is illegal
		else if(args.length < 2 || args.length == 3 || args.length > 13)
			printArgError();
		
		//The first argument needs to be the location of the tool
		String packageLocation = args[0];
		
		//The second argument needs to be the mode
		mode = null;
		if(args[1].equalsIgnoreCase("-t"))
			mode = Mode.PREDEF;
		else if(args[1].equalsIgnoreCase("-ti"))
			mode = Mode.PREDEFI;
		else if(args[1].equalsIgnoreCase("-o"))
			mode = Mode.PARAM;
		else if(args[1].equalsIgnoreCase("-oi"))
			mode = Mode.PARAMI;
		else if(args[1].equalsIgnoreCase("-x"))
			mode = Mode.SUITE;
		else
			printArgError();

		//Process the remaining arguments according to the mode
		switch(mode)
		{
			//PREDEF or PREDEFI mode - 2 parameters
			case PREDEF:
			case PREDEFI:
				if(args.length != 2)
					printArgError();
				//The matching task is predefined
				String src = DEFAULT_TDRS_URL + DEFAULT_PREDEFINED_SOURCE;
				String tgt = DEFAULT_TDRS_URL + DEFAULT_PREDEFINED_TARGET;
				String ref = DEFAULT_TDRS_URL + DEFAULT_PREDEFINED_REFERENCE;
				try
				{
					source = new URL(src);
					target = new URL(tgt);
					reference = new URL(ref);
					if(mode.equals(Mode.PREDEFI))
						input = reference;
				}
				catch(MalformedURLException e)
				{
					e.printStackTrace();
				}
				break;
			//PARAM mode - 4+ parameters
			case PARAM:
				if(args.length < 4)
					printArgError();
				try
				{
					//First the source and target ontology URLs
					source = (new URI(args[2])).toURL();
					target = (new URI(args[3])).toURL();
					//Then optional parameters
					if(args.length > 4)
					{
						//First check if an (optional) alignment was passed
						int i = 4;
						if(!args[i].startsWith("-"))
						{
							reference = (new URI(args[4])).toURL();
							i++;
						}
						while(i < args.length)
						{
							if(args[i].equalsIgnoreCase("-i") && i < args.length-1)
							{
								double error = Double.parseDouble(args[++i]);
								Oracle.startSuite(error,null);
								interactive = true;
							}
							else if(args[i].equalsIgnoreCase("-f") && i < args.length-1)
								outputFile = new File(args[++i]);
							else if(args[i].equalsIgnoreCase("-z"))
								automated = true;
							else
								printArgError();
							i++;
						}
					}
				}
				catch(MalformedURLException e)
				{
					System.out.println(">>> Argument is not a URL!");
					e.printStackTrace();
				}
				catch(URISyntaxException e)
				{
					System.out.println(">>> Argument is not a URL!");
					e.printStackTrace();
				}
				break;
				//PARAMI mode - 5+ parameters
				case PARAMI:
					if(args.length < 5)
						printArgError();
					try
					{
						//First the source and target ontology URLs
						source = (new URI(args[2])).toURL();
						target = (new URI(args[3])).toURL();
						input = (new URI(args[4])).toURL();
						for(int i = 5; i < args.length; i++)
						{
							if(args[i].equalsIgnoreCase("-f") && i < args.length-1)
								outputFile = new File(args[++i]);
							else if(args[i].equalsIgnoreCase("-z"))
								automated = true;
							else
								printArgError();
						}
					}
					catch(MalformedURLException e)
					{
						System.out.println(">>> Argument is not a URL!");
						e.printStackTrace();
					}
					catch(URISyntaxException e)
					{
						System.out.println(">>> Argument is not a URL!");
						e.printStackTrace();
					}
					break;
	   		//In SUITE mode, we need at least 6 parameters
			case SUITE:
				if(args.length < 6)
					printArgError();
				//The repository URI
				tdrsLocation = args[2];
				if(!exists(tdrsLocation + "testdata"))
				{
					System.err.println("Specified SEALS repository (" + tdrsLocation + ") could not be accessed, " +
							"please make sure the identifier is correct and if so, check your internet connection!");
					System.exit(-1);
				}
				//The suite ID
				testDataCollectionName = args[3];
				//The version ID
				testDataVersionNumber = args[4];
				//The output folder
				outRawResultFolder = args[5];
				//Other optional parameters
				for(int i = 6; i < args.length; i++)
				{
					if(args[i].equalsIgnoreCase("-z"))
						automated = true;
					else if(args[i].equalsIgnoreCase("-a"))
						skipTestsWithoutRefAlign = false;
					else if(args[i].equalsIgnoreCase("-i") && ++i < args.length)
					{
						double error = Double.parseDouble(args[i]);
						Oracle.startSuite(error,outRawResultFolder);
						interactive = true;
					}
					else if(args[i].equalsIgnoreCase("-s") && i+2 < args.length)
					{
						resultsId = args[++i];
						toolName = args[++i];
					}
					else
						printArgError();
				}
				break;
		}
		
		//Deploy the package
		String sealsHome = Helper.deployPackage(packageLocation);
		if(!automated)
			Helper.stopProgram(">>> All files are copied to SEALS_HOME. Press y to start the matching process: ");
		packagePath = new File(packageLocation);
		try
		{
			bridge = loadBridge();
		}
		catch(PackageCreationException e)
		{
			System.err.println("Cannot create package '" + packageLocation + "': " + e.getMessage());
		}
		catch(ToolBridgeLoadingException e)
		{
			System.err.println("Cannot load tool bridge from package '" + packageLocation + "': " + e.getMessage());
		}

		//Execute the matching suite/task
		if(mode.equals(Mode.SUITE))
			runTestSuite();
		else
		{
			match();
			if(outputFile != null)
			{
				try
				{
					saveAlignment(alignment, outputFile);
				}
				catch(IOException e)
				{
					System.out.println(">>> " + e.getMessage());
					System.out.println(">>> Unable to copy alignment to the specified file: " + outputFile.getAbsolutePath());
					System.out.println(">>> Result stored to URL: " + alignment);
				}
			}
			else
				System.out.println(">>> Result stored to URL: " + alignment);
			Oracle.endSuite();
	   		if(reference != null)
	   		{
	   			HashAlignment output = loadAlignment(alignment);
	   			refAlign = loadAlignment(reference);
	   			
				int[] classif = refAlign.evaluation(output);
				double[] evaluation = evaluationParameters(classif);
				System.out.println(">>> Evaluation:");
				System.out.println("Precision\tRecall\tF-measure\tRun Time");
				System.out.println(evaluation[0] + "\t" + evaluation[1] + "\t" + evaluation[2] + "\t" + runTime);
	   		}
		}
		
		//Clean up and exit
		if(!automated)
			Helper.stopProgram(">>> Matching finished. Press y to clear SEALS_HOME: ");
		System.out.println(">>> Cleaning up environment...");
		Helper.deleteDirectory(new File(sealsHome), 0);
	}
	
	//Computes Precision, Recall and F-measure
	public static double[] evaluationParameters(int[] classif)
	{
		double[] evaluation = new double[3];
		if(classif[0]+classif[1] == 0 || classif[0]+classif[2] == 0)
			return evaluation;
		//Precision
		evaluation[0] = Math.min(Math.round(classif[0] * 1000.0 / (classif[0]+classif[1]))/1000.0, 1.0);
		//Recall
		evaluation[1] = Math.min(Math.round(classif[0] * 1000.0 / (classif[0]+classif[2]))/1000.0, 1.0);
		//F-measure
		evaluation[2] = Math.round((2000.0 * evaluation[0] * evaluation[1]) / (evaluation[0] + evaluation[1]))/1000.0;
		return evaluation;
	}

	//Checks if a URL exists
	private static boolean exists(String URLName)
	{
		try
		{
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
			con.setRequestMethod("HEAD");
			return con.getResponseCode() == HttpURLConnection.HTTP_OK;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	//Loads an Alignment using the Alignment API and returns it as a HashAlignment 
	private static HashAlignment loadAlignment(URL alignmentUri)
	{
		try
		{
			AlignmentParser aparser = new AlignmentParser(0);
			Alignment align = aparser.parse(alignmentUri.toURI());
			return new HashAlignment(align);
		}
		catch(URISyntaxException e)
		{
			System.err.println("URI Syntax Exception: " + e.getMessage());
			System.err.println("Unable to open alignment " + alignmentUri);
		}
		catch(AlignmentException e)
		{
			System.err.println("Alignment Exception: " + e.getMessage());
			System.err.println("Unable to open alignment " + alignmentUri);
		}
		return null;
	}
	
	//Loads the tool bridge
	private static IOntologyMatchingToolBridge loadBridge() throws PackageCreationException, ToolBridgeLoadingException
	{
		if(bridge == null)
		{
			ToolPackageFactory toolPackageFactory = new ToolPackageFactory();
			IToolPackage toolPackage = toolPackageFactory.createPackage(packagePath);
			ToolBridgeLoader<IOntologyMatchingToolBridge> toolBridgeLoader = new ToolBridgeLoader<IOntologyMatchingToolBridge>(
					IOntologyMatchingToolBridge.class);
			bridge = toolBridgeLoader.load(toolPackage).getPlugin();
		}
		return bridge;
	}
	
	//Matches two ontologies and stores the URL of the result
	private static void match()
	{
   		//Open reference alignment
		if(reference != null)
			refAlign = loadAlignment(reference);
		//Interactive mode
	   	if(interactive)
	   	{
	   		//If the reference is available, start Oracle
	   		if(refAlign != null)
	   			Oracle.startTask(refAlign,testCaseId);
	   		else
	   			System.err.println("No reference alignment: matching task will proceed with no user interaction");
	   	}
	   	//Start the timer
		long startTime = System.currentTimeMillis();
		//Execute the matching task
		try
   		{
			//Normal match mode
			if(input == null)
		   		alignment = bridge.align(source, target);
		   	//Extension match mode (with input alignment)
		   	else
		   		alignment = bridge.align(source, target, input);
	   	}
		catch(ToolException e)
		{
			System.err.println("Tool Exception: " + e.getMessage());
			System.err.println("Unable to execute matching task");
			e.printStackTrace();
		}
		catch(ToolBridgeException e)
		{
			System.err.println("Tool Bridge Exception: " + e.getMessage());
			System.err.println("Unable to execute matching task");
			e.printStackTrace();
		}
		catch(Exception e)
		{
			System.err.println("Unknown error: " + e.getMessage());
			if(alignment == null)
				System.err.println("Unable to execute matching task");
			e.printStackTrace();
		}
		//Stop the timer
   		finally
   		{
   			runTime = System.currentTimeMillis() - startTime;
   			totalRunTime += runTime;
   		}
		//Stop the Oracle
   		if(interactive && refAlign != null)
   		{
   			oracleAlign = Oracle.getOracleReference();
   			Oracle.endTask();
   		}
	   	//Check that the output alignment file is not null
	   	if(alignment == null)
			System.err.println("Matching task unsuccessful: null output alignment");
	   	//And check that it can be read
	   	else
	   	{
	   		try
	   		{
				//Check that the alignment file is really accessible
				BufferedReader in = new BufferedReader(new InputStreamReader(alignment.openStream()));
				in.readLine();
				in.close();
	   		}
	   		catch(IOException e)
	   		{
				System.err.println("Matching task unsuccessful: unable to read result '" + alignment + "'");
				e.printStackTrace();
				return;
			}
   		}
	}

	//Prints the argument error message and exits
	private static void printArgError()
	{
		System.out.println("SEALS OMT Client " + VERSION + " (" + DATE + ")\n");
		System.err.println("Illegal arguments: please use -h or --help for instructions on how to run the program!");
		System.exit(-1);
	}
	
	//Prints the help message and exits
	private static void printHelpMessage()
	{
		System.out.println("SEALS OMT Client " + VERSION + " (" + DATE + ")\n");
		System.out.println("Usage: \"java -jar seals-omt-client.jar <packageLocation> OPTIONS\"");
		System.out.println("\nOptions:");
		System.out.println("> Predefined test: \"<-t>\"");
		System.out.println("> Predefined test with input alignment: \"<-ti>\"");
		System.out.println("> Parametrized test: \"<-o> <ontologyURL1> <ontologyURL2> [<referenceAlignURL>] " +
							"[<-f> <ouputFile>] [<-i> <errorRate>] [<-z>]\"");
		System.out.println("> Parametrized test with input alignment: \"<-o> <ontologyURL1> <ontologyURL2> <inputAlignURL> " +
							"[<-f> <ouputFile>] [<-z>]\"");
		System.out.println("> Run suite: \"<-x> <repUri> <suiteId> <versionId> <outputFolder> " +
							"[<-a>] [<-z>] [<-i> <errorRate>] [<-s> <resultsId> <toolName>]\"");
		System.out.println("\nParameters:");
		System.out.println("> -a (-x mode only): all tests in the suite will be run, including those with no reference alignment");
		System.out.println("> -f (-o or -oi mode): saves the output alignment to the specified file");
		System.out.println("> -i (-o or -x mode): activates interactive matching with the given error rate;" +
							" requires a <referenceAlignURL> in -o mode");
		System.out.println("> -s (-x mode only): activates store mode");
		System.out.println("> -z (-o, -oi or -x mode): activates batch mode - no command line input will be required to continue");
		System.exit(0);
	}
	
	//Runs a complete test suite.
	private static void runTestSuite()
	{
		//Store mode: setup
		String rawResultsId = null;
		String interpretationsId = null;
		Content genMetadata = null;
		String repRawResultsMetadata = "";
		String repInterpretationMetadata = "";
		if(resultsId != null)
		{
			rawResultsId = resultsId + "-rr";
			interpretationsId = resultsId + "-ir";
			System.out.println(">>> Client in store mode, raw results id:	 " + rawResultsId);
			System.out.println(">>> Client in store mode, interpretations id: " + interpretationsId);

			//Prepare object metadata generator
			genMetadata = new Content();
			repRawResultsMetadata = genMetadata.initRRMetadata(rawResultsId, toolName, testDataCollectionName, testDataVersionNumber, true);
			repInterpretationMetadata = genMetadata.initIRMetadata(interpretationsId, toolName, testDataCollectionName,
					testDataVersionNumber, rawResultsId, true);
		}
		
		File resultsFolder = new File(outRawResultFolder);
		resultsFolder.mkdir();
		File f = new File(outRawResultFolder, "results.txt");
		if(f.exists())
			f.delete();
		BufferedWriter writer = null;
		String resultString = "";
		try
		{
			writer = new BufferedWriter(new FileWriter(f, true));
			resultString = "Test Case ID\tRun Time\tPrecision\tRecall\tF-measure\t";
			if(interactive)
				resultString += "Precision Oracle\tRecall Oracle\tF-measure Oracle\t";
			resultString += "Notes\n";
			writer.append(resultString);
			System.out.println(resultString);
		}
		catch(IOException e)
		{
			System.err.println("Couldn't write to output file '" + f + "' - " + e.getMessage());
			System.err.println("Please make sure java has write permissions in the output folder and try again!");
			e.printStackTrace();
			System.exit(-1);
		}
		classification = new int[3];
		String emptyResult;
		if(interactive)
		{
			oracleClassification = new int[3];
			emptyResult = "\t-\t-\t-\t-\t-\t-";
		}
		else
			emptyResult = "\t-\t-\t-";
		
		//Run each test case in the suite
		TestDataSuite tds = new TestDataSuite(tdrsLocation, testDataCollectionName, testDataVersionNumber);
		TestCaseIterator it = tds.getTestCases();
		while(it.hasNext())
		{
			testCaseId = it.next();
			//Reset the test case variables
			reference = null;
			refAlign = null;
			oracleAlign = null;
			//Check if the source and target are accessible
			if(!exists(String.valueOf(tds.getDataItem(testCaseId, "source"))))
				resultString = testCaseId + "\t-" + emptyResult + "\tSource ontology not defined";
			else if(!exists(String.valueOf(tds.getDataItem(testCaseId, "target"))))
				resultString = testCaseId + "\t-" + emptyResult + "\tTarget ontology not defined";
			else
			{
				//Get the source, target, and reference (if available)
				source = tds.getDataItem(testCaseId, "source");
				target = tds.getDataItem(testCaseId, "target");
				if(exists(String.valueOf(tds.getDataItem(testCaseId, "reference"))))
					reference = tds.getDataItem(testCaseId, "reference");
				//If reference is unavailable and we're skipping tests without reference, skip this
				else if(skipTestsWithoutRefAlign)
					continue;
				//Reset the output alignment URI
				alignment = null;
				//Match the test case
				try
				{
					match();
				}
				//Process the results (even in the event of an exception
				//if it didn't fully impede the matching task)
				finally
				{
					//If there is no output alignment, then there is nothing to process
					if(alignment == null)
					{
						resultString = testCaseId + "\t" + runTime + emptyResult + "\tNo output alignment found";
						//Store mode: store error message and null results
						if(resultsId != null)
						{
							                                //testId, problemTool, problemPlatform
							genMetadata.addRRDataItemMetadata(testCaseId, "true", "false");
							genMetadata.addIRDataItemMetadata(testCaseId, 0, 0, 0, 0);
						}
					}
					else
					{
						try
						{
							//Store the output alignment
							File file = new File(outRawResultFolder, testCaseId + ".rdf");														
							saveAlignment(alignment, file);
						}
						catch(IOException e)
						{
							System.err.println("Could not save output alignment: " + e.getMessage());
							e.printStackTrace();
						}
						//Store mode: store item raw results
						if(resultsId != null)
							genMetadata.addRRDataItemMetadata(testCaseId, "false", "false");
						//If there is a reference alignment, evaluate the output alignment
						if(refAlign != null)
						{
							HashAlignment output = loadAlignment(alignment);
							if(output == null)
							{
								resultString = testCaseId + "\t" + runTime + emptyResult + "\tUnable to open output alignment";
								//Store mode: add empty interpretation
								if(resultsId != null)
									genMetadata.addIRDataItemMetadata(testCaseId, 0, 0, 0, runTime);
							}
							else
							{
								int[] classif = refAlign.evaluation(output);
								for(int i = 0; i < 3; i++)
									classification[i] += classif[i];
								double[] evaluation = evaluationParameters(classif);
								resultString = testCaseId + "\t" + runTime + "\t" + evaluation[0] + "\t" + evaluation[1] + "\t" + evaluation[2];
								//Store mode: add interpretation
								if(resultsId != null)
									genMetadata.addIRDataItemMetadata(testCaseId, evaluation[0], evaluation[1], evaluation[2], runTime);
								if(interactive)
								{
									if(oracleAlign != null)
									{
										classif = oracleAlign.evaluation(output);
										for(int i = 0; i < 3; i++)
											oracleClassification[i] += classif[i];
										evaluation = evaluationParameters(classif);
										resultString += "\t" + evaluation[0] + "\t" + evaluation[1] + "\t" + evaluation[2] + "\t-";
									}
									else
										resultString += "\t-\t-\t-\tNot interactive";
								}
								else
									resultString += "\t-";
							}
						}
						else
						{
							resultString = testCaseId + "\t" + runTime + emptyResult + "\tNo reference alignment available";
							//Store mode: add empty interpretation
							if(resultsId != null)
								genMetadata.addIRDataItemMetadata(testCaseId, 0, 0, 0, runTime);
						}
					}
				}
			}
			System.out.println(resultString);
			try
			{
				writer.append(resultString + "\n");
				writer.flush();
			}
			catch(IOException e)
			{
				System.err.println("Couldn't write results to '" + f + "' - " + e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		//Global evaluation
		double[] evaluation = evaluationParameters(classification);
		resultString = "Global\t" + totalRunTime + "\t" + evaluation[0] + "\t" + evaluation[1] + "\t" + evaluation[2];
		if(interactive)
		{
			evaluation = evaluationParameters(oracleClassification);
			resultString += "\t" + evaluation[0] + "\t" + evaluation[1] + "\t" + evaluation[2];
			Oracle.endSuite();
		}
		resultString += "\t-";
		System.out.println(resultString);
		try
		{
			writer.append(resultString + "\n");
			writer.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		//Store mode: write raw results suite metadata and interpretation suite metadata
		if(resultsId != null)
		{
			saveStore(outRawResultFolder + File.separatorChar + "metadata.rdf", genMetadata.getRRSuiteMetadata());
			String pathInter = outRawResultFolder + File.separatorChar + "interpretations";
			File interpretation = new File(pathInter);
			if(interpretation.mkdir())
				saveStore(pathInter + File.separatorChar + "metadata.rdf", genMetadata.getIRSuiteMetadata());

			//Zip the folder containing the .rdf in order to upload to the rrs repository
			ZipUpload obj = new ZipUpload();
			//Raw results
			String zip = outRawResultFolder + File.separatorChar + rawResultsId + ".zip";
			System.out.println(outRawResultFolder);
			if(obj.zip(outRawResultFolder, zip))
				if(obj.upload("rr", zip, repRawResultsMetadata))
					System.out.println(">>> Results store at SEALS repository (rr):	 " + rawResultsId);
			//Interpretations
			zip = pathInter + File.separatorChar + interpretationsId + ".zip";
			if(obj.zip(pathInter, zip))
				if(obj.upload("ir", zip, repInterpretationMetadata))
					System.out.println(">>> results store at SEALS repository (ir):	 " + interpretationsId);
		}
	}

	//Saves the alignment specified by the given URL to the given file
	private static void saveAlignment(URL url, File file) throws IOException
	{
		InputStream is = url.openStream();
		FileOutputStream fos = new FileOutputStream(file);
		int oneChar;
		while((oneChar = is.read()) != -1)
			fos.write(oneChar);
		is.close();
		fos.close();
	}

	//Saves suite metadata content to file
	private static void saveStore(String filep, String metadata)
	{
		BufferedWriter writer;
		try
		{
			writer = new BufferedWriter(new FileWriter(new File(filep), true));
			writer.append(metadata);
			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
			System.err.println("Error writing " + filep + " - " + e.getMessage());
		}
	}
}