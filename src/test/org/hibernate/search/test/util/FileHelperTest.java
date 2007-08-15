//$Id$
package org.hibernate.search.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import org.hibernate.search.util.FileHelper;

/**
 * @author Emmanuel Bernard
 */
public class FileHelperTest extends TestCase {
	public void testTiti() throws Exception {
		File titi = new File("file:/c:/titi", "file:/d:/toito");
		assertFalse ( titi.exists() );
	}

	protected void setUp() throws Exception {
		super.setUp();
		File dir =  new File("./filehelpersrc");
		dir.mkdir();
		String name = "a";
		createFile( dir, name );
		name = "b";
		createFile( dir, name );
		dir =  new File(dir, "subdir");
		dir.mkdir();
		name = "c";
		createFile( dir, name );
	}

	private void createFile(File dir, String name) throws IOException {
		File a = new File(dir, name);
		a.createNewFile();
		FileOutputStream os = new FileOutputStream( a, false );
		os.write( 1 );
		os.write( 2 );
		os.write( 3 );
		os.flush();
		os.close();
	}

	protected void tearDown() throws Exception {
		super.setUp();
		File dir =  new File("./filehelpersrc");
		FileHelper.delete( dir );
		dir =  new File("./filehelperdest");
		FileHelper.delete( dir );
	}

	public void testSynchronize() throws Exception {
		File src =  new File("./filehelpersrc");
		File dest =  new File("./filehelperdest");
		FileHelper.synchronize( src, dest, true );
		File test = new File(dest, "b");
		assertTrue( test.exists() );
		test = new File( new File(dest, "subdir"), "c");
		assertTrue( test.exists() );

		//change
		Thread.sleep( 2*2000 );
		test = new File( src, "c");
		FileOutputStream os = new FileOutputStream( test, true );
		os.write( 1 );
		os.write( 2 );
		os.write( 3 );
		os.flush();
		os.close();
		File destTest = new File(dest, "c");
		assertNotSame( test.lastModified(), destTest.lastModified() );
		FileHelper.synchronize( src, dest, true );
		assertEquals( test.lastModified(), destTest.lastModified() );
		assertEquals( test.length(), destTest.length() );

		//delete file
		test.delete();
		FileHelper.synchronize( src, dest, true );
		assertTrue( ! destTest.exists() );
	}
}
