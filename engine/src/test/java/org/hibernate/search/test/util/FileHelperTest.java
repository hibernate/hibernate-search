/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

	private static File root;

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
		root = new File( buildDir, "filehelper" );
		log.infof( "Using %s as test directory.", root.getAbsolutePath() );
	}

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

	@After
	public void tearDown() throws Exception {
		File dir = new File( root, srcDir );
		FileHelper.delete( dir );
		dir = new File( root, destDir );
		FileHelper.delete( dir );
		FileHelper.delete( root );
	}

	@Test
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

		assertTrue( "Directories should be in sync", FileHelper.areInSync( src, dest ) );
		assertEquals( srcTestFile.lastModified(), destTestFile.lastModified() );
		assertEquals( srcTestFile.length(), destTestFile.length() );
		assertTrue( destTestFile1.exists() );
		assertTrue( destTestFile2.exists() );
		assertTrue( !destTestFile3.exists() );

		// delete src test file
		srcTestFile.delete();
		FileHelper.synchronize( src, dest, true );
		assertTrue( !destTestFile.exists() );
		assertTrue( "Directories should be in sync", FileHelper.areInSync( src, dest ) );
	}
}
