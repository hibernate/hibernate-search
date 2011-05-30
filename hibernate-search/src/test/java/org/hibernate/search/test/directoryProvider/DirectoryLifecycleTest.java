/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.directoryProvider;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectoryLifecycleTest {

	@Test
	public void testLifecycle() {
		//test it once
		testOnce();
		// and test it again to verify the instances are not the same
		testOnce();
		
	}

	private void testOnce() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder()
		.setProperty(
			"hibernate.search.default.directory_provider",
			org.hibernate.search.test.directoryProvider.CloseCheckingDirectoryProvider.class.getName() )
		.addAnnotatedClass( SnowStorm.class )
		.build();
		CloseCheckingDirectoryProvider directoryProvider;
		try {
			SearchFactory searchFactory = builder.getSearchFactory();
			DirectoryProvider[] directoryProviders = searchFactory.getDirectoryProviders( SnowStorm.class );
			assertThat( directoryProviders.length ).isEqualTo( 1 );
			assertThat( directoryProviders[0] ).isInstanceOf( CloseCheckingDirectoryProvider.class );
			directoryProvider = (CloseCheckingDirectoryProvider) directoryProviders[0];
			assertThat( directoryProvider.isInitialized() ).isTrue();
			assertThat( directoryProvider.isStarted() ).isTrue();
			assertThat( directoryProvider.isStopped() ).isFalse();
		}
		finally {
			builder.close();
		}
		assertThat( directoryProvider.isStopped() ).isTrue();
	}

}
