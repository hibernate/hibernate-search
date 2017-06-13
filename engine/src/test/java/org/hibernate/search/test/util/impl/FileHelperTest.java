/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class FileHelperTest {

	private static final Log log = LoggerFactory.make( Log.class );

	private static Path root;

	/**
	 * Source directory
	 */
	private String srcDir = "filehelpersrc";

	/**
	 * Destination directory
	 */
	private String destDir = "filehelperdest";

	@BeforeClass
	public static void prepareRootDirectory() {
		String buildDir = System.getProperty( "build.dir" );
		if ( buildDir == null ) {
			buildDir = ".";
		}
		root = Paths.get( buildDir, "filehelper" );
		log.infof( "Using %s as test directory.", root.toAbsolutePath() );
	}

	private Path createFile(Path dir, String name) throws IOException {
		Path file = dir.resolve( name );
		createDummyDatafile( file );
		return file;
	}

	private void createDummyDatafile(Path file) throws IOException {
		try ( OutputStream os = Files.newOutputStream( file, StandardOpenOption.CREATE_NEW ) ) {
			os.write( "Hello! anyone there?".getBytes( StandardCharsets.UTF_8 ) );
			os.flush();
		}
	}

	@After
	public void tearDown() throws Exception {
		FileHelper.tryDelete( root );
	}

	@Test
	public void testSynchronize() throws Exception {
		// create a src directory structure
		Path src = root.resolve( srcDir );
		Files.createDirectories( src );
		String name = "a";
		createFile( src, name );
		name = "b";
		createFile( src, name );
		Path subDir = src.resolve( "subdir" );
		Files.createDirectories( subDir );
		name = "c";
		createFile( subDir, name );

		// create destination and sync
		Path dest = root.resolve( destDir );
		assertFalse( "Directories should be out of sync", FileHelper.areInSync( src, dest ) );
		FileHelper.synchronize( src, dest, true );
		assertTrue( "Directories should be in sync", FileHelper.areInSync( src, dest ) );
		Path destTestFile1 = dest.resolve( "b" );
		assertTrue( Files.exists( destTestFile1 ) );
		Path destTestFile2 = dest.resolve( "subdir" ).resolve( "c" );
		assertTrue( Files.exists( destTestFile2 ) );

		// create a new file in destination which does not exist in src. Should be deleted after next sync
		Path destTestFile3 = createFile( dest, "foo" );

		// create a file in the src directory and write some data to it
		Path srcTestFile = src.resolve( "c" );
		createDummyDatafile( srcTestFile );
		Path destTestFile = dest.resolve( "c" );
		assertNotSame( srcTestFile.toFile().lastModified(), destTestFile.toFile().lastModified() );
		assertFalse( "Directories should be out of sync", FileHelper.areInSync( src, dest ) );

		FileHelper.synchronize( src, dest, true );

		assertTrue( "Directories should be in sync", FileHelper.areInSync( src, dest ) );
		assertEquals( srcTestFile.toFile().lastModified(), destTestFile.toFile().lastModified() );
		assertEquals( srcTestFile.toFile().length(), destTestFile.toFile().length() );
		assertTrue( Files.exists( destTestFile1 ) );
		assertTrue( Files.exists( destTestFile2 ) );
		assertTrue( ! Files.exists( destTestFile3 ) );

		// delete src test file
		Files.deleteIfExists( srcTestFile );
		FileHelper.synchronize( src, dest, true );
		assertTrue( ! Files.exists( destTestFile ) );
		assertTrue( "Directories should be in sync", FileHelper.areInSync( src, dest ) );
	}
}
