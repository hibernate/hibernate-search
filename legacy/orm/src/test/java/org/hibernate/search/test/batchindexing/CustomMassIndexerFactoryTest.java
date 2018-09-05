/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.spi.MassIndexerFactory;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hibernate.search.batchindexing.spi.MassIndexerFactory.MASS_INDEXER_FACTORY_CLASSNAME;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Davide D'Alto
 */
public class CustomMassIndexerFactoryTest extends SearchTestBase {

	@Test
	public void testCreationOfTheSelectedMassIndexer() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		MassIndexer indexer = fullTextSession.createIndexer( Clock.class );

		assertThat( indexer, instanceOf( NoopMassIndexer.class ) );
	}

	@Test
	public void testFactoryCanReadConfiguration() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		NoopMassIndexer indexer = (NoopMassIndexer) fullTextSession.createIndexer( Clock.class );

		assertTrue( "The factory cannot read the configuration properties", indexer.isConfigurationReadable() );
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( MASS_INDEXER_FACTORY_CLASSNAME, NoopMassIndexerFactory.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	public static class NoopMassIndexerFactory implements MassIndexerFactory {

		private boolean configurationReadable = false;

		@Override
		public void initialize(Properties properties) {
			configurationReadable = properties.get( MASS_INDEXER_FACTORY_CLASSNAME ) == NoopMassIndexerFactory.class
					.getName();
		}

		@Override
		public MassIndexer createMassIndexer(SearchIntegrator searchFactory, SessionFactoryImplementor sessionFactory,
				Class<?>... entities) {
			return new NoopMassIndexer( configurationReadable );
		}
	}

	private static class NoopMassIndexer implements MassIndexer {

		private final boolean configurationReadable;

		public NoopMassIndexer(boolean configurationAccessible) {
			this.configurationReadable = configurationAccessible;
		}

		public boolean isConfigurationReadable() {
			return configurationReadable;
		}

		@Override
		public MassIndexer typesToIndexInParallel(int threadsToIndexObjects) {
			return null;
		}

		@Override
		public MassIndexer threadsToLoadObjects(int numberOfThreads) {
			return null;
		}

		@Override
		public MassIndexer batchSizeToLoadObjects(int batchSize) {
			return null;
		}

		@Override
		public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
			return null;
		}

		@Override
		public MassIndexer progressMonitor(MassIndexerProgressMonitor monitor) {
			return null;
		}

		@Override
		public MassIndexer cacheMode(CacheMode cacheMode) {
			return null;
		}

		@Override
		public MassIndexer optimizeOnFinish(boolean optimize) {
			return null;
		}

		@Override
		public MassIndexer optimizeAfterPurge(boolean optimize) {
			return null;
		}

		@Override
		public MassIndexer purgeAllOnStart(boolean purgeAll) {
			return null;
		}

		@Override
		public MassIndexer limitIndexedObjectsTo(long maximum) {
			return null;
		}

		@Override
		public Future<?> start() {
			return null;
		}

		@Override
		public void startAndWait() throws InterruptedException {
		}

		@Override
		public MassIndexer idFetchSize(int idFetchSize) {
			return null;
		}

		@Override
		public MassIndexer transactionTimeout(int timeoutInSeconds) {
			return null;
		}
	}
}
