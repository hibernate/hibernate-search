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
package org.hibernate.search.test.directoryProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.hibernate.search.SearchException;
import org.hibernate.search.store.impl.DirectoryProviderHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.Test;

/**
 * @author Gavin King
 * @author Sanne Grinovero
 */
public class DirectoryProviderHelperTest {

	@Test
	public void testMkdirsDetermineIndex() {
		String root = "./testDir/dir1/dir2";
		String relative = "dir3";

		Properties properties = new Properties();
		properties.put( "indexBase", root );
		properties.put( "indexName", relative );

		DirectoryProviderHelper.getVerifiedIndexDir( "name", properties, true );

		assertTrue( new File( root ).exists() );

		FileHelper.delete( new File( "./testDir" ) );
	}

	@Test
	public void testMkdirsGetSource() {
		String root = "./testDir";
		String relative = "dir1/dir2/dir3";

		Properties properties = new Properties();
		properties.put( "sourceBase", root );
		properties.put( "source", relative );

		File rel = DirectoryProviderHelper.getSourceDirectory( "name", properties, true );

		assertTrue( rel.exists() );

		FileHelper.delete( new File( root ) );
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
