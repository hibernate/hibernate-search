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

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchFactoryIntegrator;
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
			SearchFactoryIntegrator searchFactory = (SearchFactoryIntegrator) builder.getSearchFactory();
			EntityIndexBinding snowIndexBinder = searchFactory.getIndexBinding( SnowStorm.class );
			IndexManager[] indexManagers = snowIndexBinder.getIndexManagers();
			assertThat( indexManagers.length ).isEqualTo( 1 );
			assertThat( indexManagers[0] ).isInstanceOf( DirectoryBasedIndexManager.class );
			DirectoryBasedIndexManager dbBasedManager = (DirectoryBasedIndexManager)indexManagers[0];
			assertThat( dbBasedManager.getDirectoryProvider() ).isInstanceOf( CloseCheckingDirectoryProvider.class );
			directoryProvider = (CloseCheckingDirectoryProvider) dbBasedManager.getDirectoryProvider();
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
