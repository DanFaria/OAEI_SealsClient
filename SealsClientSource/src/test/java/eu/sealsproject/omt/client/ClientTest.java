package eu.sealsproject.omt.client;


import org.junit.BeforeClass;
import org.junit.Test;

import eu.sealsproject.platform.res.tool.bundle.factory.PackageCreationException;
import eu.sealsproject.platform.res.tool.bundle.loaders.ToolBridgeLoadingException;

/**
 * This unit test does not really test the relevant align methods.
 * This is problematic because it requires to have the environment set up in the appropriate way,
 * which requires also some manual steps (due to the manual deployment).
 * 
 */
public class ClientTest extends ToolBridgeTest {
	
	
	@BeforeClass
	public static void setUpBefore() throws ToolBridgeLoadingException, PackageCreationException {

	}

	
	@Test
	public void testBaseOperations() throws ToolBridgeLoadingException, PackageCreationException {

	}
	
	
}