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

import org.hibernate.search.store.impl.DirectoryProviderHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.Test;

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

}
