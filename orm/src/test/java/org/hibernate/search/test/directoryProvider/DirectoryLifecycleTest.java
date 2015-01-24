/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.directoryProvider;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
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
			SearchIntegrator integrator = builder.getSearchFactory().unwrap( SearchIntegrator.class );
			EntityIndexBinding snowIndexBinder = integrator.getIndexBinding( SnowStorm.class );
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
