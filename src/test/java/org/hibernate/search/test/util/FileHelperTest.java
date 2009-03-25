//$Id$
package org.hibernate.search.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;
import org.slf4j.Logger;

import org.hibernate.search.util.FileHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class FileHelperTest extends TestCase {
	private static final Logger log = LoggerFactory.make();

	private static File root;

	static {
		String buildDir = System.getProperty( "build.dir" );
		if ( buildDir == null ) {
			buildDir = ".";
		}
		root = new File( buildDir, "filehelper" );
		log.info( "Using {} as test directory.", root.getAbsolutePath() );
	}

	/**
	 * Source directory
	 */
	private String srcDir = "filehelpersrc";

	/**
	 * Destination directory
	 */
	private String destDir = "filehelperdest";


	private File createFile(File dir, String name) throws IOException {
		File file = new File( dir, name );
		file.createNewFile();
		writeDummyDataToFile( file );
		return file;
	}

	private void writeDummyDataToFile(File file) throws IOException {
		FileOutputStream os = new FileOutputStream( file, true );
		os.write( 1 );
		os.write( 2 );
		os.write( 3 );
		os.flush();
		os.close();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		File dir = new File( root, srcDir );
		FileHelper.delete( dir );
		dir = new File( root, destDir );
		FileHelper.delete( dir );
		FileHelper.delete( root );
	}

	public void testSynchronize() throws Exception {
		// create a src directory structure
		File src = new File( root, srcDir );
		src.mkdirs();
		String name = "a";
		createFile( src, name );
		name = "b";
		createFile( src, name );
		File subDir = new File( src, "subdir" );
		subDir.mkdirs();
		name = "c";
		createFile( subDir, name );

		// create destination and sync
		File dest = new File( root, destDir );
		assertFalse( "Directories should be out of sync", FileHelper.areInSync( src, dest ) );
		FileHelper.synchronize( src, dest, true );
		assertTrue( "Directories should be in sync", FileHelper.areInSync( src, dest ) );
		File destTestFile1 = new File( dest, "b" );
		assertTrue( destTestFile1.exists() );
		File destTestFile2 = new File( new File( dest, "subdir" ), "c" );
		assertTrue( destTestFile2.exists() );

		// create a new file in destination which does not exists in src. should be deleted after next sync
		File destTestFile3 = createFile( dest, "foo" );

		// create a file in the src directory and write some data to it
		File srcTestFile = new File( src, "c" );
		writeDummyDataToFile( srcTestFile );
		File destTestFile = new File( dest, "c" );
		assertNotSame( srcTestFile.lastModified(), destTestFile.lastModified() );
		assertFalse( "Directories should be out of sync", FileHelper.areInSync( src, dest ) );

		FileHelper.synchronize( src, dest, true );

		assertTrue("Directories should be in sync", FileHelper.areInSync( src, dest ));
		assertEquals( srcTestFile.lastModified(), destTestFile.lastModified() );
		assertEquals( srcTestFile.length(), destTestFile.length() );
		assertTrue( destTestFile1.exists() );
		assertTrue( destTestFile2.exists() );
		assertTrue( !destTestFile3.exists() );

		// delete src test file
		srcTestFile.delete();
		FileHelper.synchronize( src, dest, true );
		assertTrue( !destTestFile.exists() );
		assertTrue("Directories should be in sync", FileHelper.areInSync( src, dest ));
	}
}
