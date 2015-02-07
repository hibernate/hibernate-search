/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.store;

import java.io.File;
import java.util.Properties;

import org.hibernate.search.store.spi.DirectoryHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 * @author Sanne Grinovero
 */
public class DirectoryHelperTest {

	@Test
	public void testMkdirsDetermineIndex() {
		String root = "./testDir/dir1/dir2";
		String relative = "dir3";

		Properties properties = new Properties();
		properties.put( "indexBase", root );
		properties.put( "indexName", relative );

		DirectoryHelper.getVerifiedIndexDir( "name", properties, true );

		assertTrue( new File( root ).exists() );

		FileHelper.delete( new File( "./testDir" ) );
	}
}
