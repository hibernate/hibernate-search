/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.batchindexing;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.search.hcore.impl.MassIndexerFactoryProvider.MASS_INDEXER_FACTORY_CLASSNAME;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.MassIndexerFactory;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class CustomMassIndexerFactoryTest extends SearchTestCaseJUnit4 {

	@Test
	public void testCreationOfTheSelectedMassIndexer() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		MassIndexer indexer = fullTextSession.createIndexer( Clock.class );

		assertThat( indexer, is( NoopMassIndexer.class ) );
	}

	@Test
	public void testFactoryCanReadConfiguration() throws Exception {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		NoopMassIndexer indexer = (NoopMassIndexer) fullTextSession.createIndexer( Clock.class );

		assertTrue( "The factory cannot read the configuration properties", indexer.isConfigurationReadable() );
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( MASS_INDEXER_FACTORY_CLASSNAME, NoopMassIndexerFactory.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
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
		public MassIndexer createMassIndexer(SearchFactoryImplementor searchFactory, SessionFactoryImplementor sessionFactory,
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
		@Deprecated
		public MassIndexer threadsForIndexWriter(int numberOfThreads) {
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
	}
}
