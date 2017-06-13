/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.store.impl.DirectoryProviderHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 * @author Sanne Grinovero
 */
public class DirectoryProviderHelperTest {

	@Test
	public void testMkdirsGetSource() throws Exception {
		String root = "./testDir";
		String relative = "dir1/dir2/dir3";

		Properties properties = new Properties();
		properties.put( "sourceBase", root );
		properties.put( "source", relative );

		Path rel = DirectoryProviderHelper.getSourceDirectoryPath( "name", properties, true );

		assertTrue( Files.exists( rel ) );

		FileHelper.delete( Paths.get( "root" ) );
	}

	@Test
	public void testConfiguringCopyBufferSize() {
		Properties prop = new Properties();
		long mB = 1024 * 1024;

		//default to FileHelper default:
		assertEquals(
				FileHelper.DEFAULT_COPY_BUFFER_SIZE, DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop )
		);

		//any value from MegaBytes:
		prop.setProperty( "buffer_size_on_copy", "4" );
		assertEquals( 4 * mB, DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop ) );
		prop.setProperty( "buffer_size_on_copy", "1000" );
		assertEquals( 1000 * mB, DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop ) );

		//invalid values
		prop.setProperty( "buffer_size_on_copy", "0" );
		boolean testOk = false;
		try {
			DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop );
		}
		catch (SearchException e) {
			testOk = true;
		}
		assertTrue( testOk );
		prop.setProperty( "buffer_size_on_copy", "-100" );
		testOk = false;
		try {
			DirectoryProviderHelper.getCopyBufferSize( "testIdx", prop );
		}
		catch (SearchException e) {
			testOk = true;
		}
		assertTrue( testOk );
	}
}
