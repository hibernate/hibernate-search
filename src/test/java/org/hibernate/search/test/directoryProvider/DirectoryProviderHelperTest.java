// $Id$
package org.hibernate.search.test.directoryProvider;

import java.io.File;
import java.util.Properties;
import junit.framework.TestCase;
import org.hibernate.search.SearchException;
import org.hibernate.search.store.DirectoryProviderHelper;
import org.hibernate.search.util.FileHelper;

/**
 * @author Gavin King
 * @author Sanne Grinovero
 */
public class DirectoryProviderHelperTest extends TestCase {

	public void testMkdirsDetermineIndex() throws Exception {
		String root = "./testDir/dir1/dir2";
		String relative = "dir3";

		Properties properties = new Properties();
		properties.put( "indexBase", root );
		properties.put( "indexName", relative );

		File f = DirectoryProviderHelper.getVerifiedIndexDir( "name", properties, true );

		assertTrue( new File( root ).exists() );

		FileHelper.delete( new File( "./testDir" ) );
	}
	
	public void testMkdirsGetSource() throws Exception {
		String root = "./testDir";
		String relative = "dir1/dir2/dir3";

		Properties properties = new Properties();
		properties.put( "sourceBase", root );
		properties.put( "source", relative );

		File rel = DirectoryProviderHelper.getSourceDirectory( "name", properties, true );

		assertTrue( rel.exists() );

		FileHelper.delete( new File( root ) );
	}
	
	public void testConfiguringCopyBufferSize() {
		Properties prop = new Properties();
		long mB = 1024 * 1024;
		
		//default to FileHelper default:
		assertEquals( FileHelper.DEFAULT_COPY_BUFFER_SIZE, DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop ) );
		
		//any value from MegaBytes:
		prop.setProperty( DirectoryProviderHelper.COPYBUFFERSIZE_PROP_NAME, "4" );
		assertEquals( 4*mB, DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop ) );
		prop.setProperty( DirectoryProviderHelper.COPYBUFFERSIZE_PROP_NAME, "1000" );
		assertEquals( 1000*mB, DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop ) );
		
		//invalid values
		prop.setProperty( DirectoryProviderHelper.COPYBUFFERSIZE_PROP_NAME, "0" );
		boolean testOk = false;
		try {
			DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop );
		} catch (SearchException e){
			testOk = true;
		}
		assertTrue( testOk );
		prop.setProperty( DirectoryProviderHelper.COPYBUFFERSIZE_PROP_NAME, "-100" );
		testOk = false;
		try {
			DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop );
		} catch (SearchException e){
			testOk = true;
		}
		assertTrue( testOk );
	}
	
}
