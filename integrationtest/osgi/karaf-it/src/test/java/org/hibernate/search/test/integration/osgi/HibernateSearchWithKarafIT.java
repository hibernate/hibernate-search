/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.integration.osgi;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.SearchFactory;
import org.hibernate.search.engine.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

/**
 * A basic integration test that executes Hibernate Search in Apache Karaf using
 * PaxExam (see <a href="https://ops4j1.jira.com/wiki/display/PAXEXAM3/Pax+Exam">online docs</a>).
 *
 * @author Hardy Ferentschik
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HibernateSearchWithKarafIT {
	private String currentVersion = Version.getVersionString();

	@Inject
	private BundleContext bundleContext;

	private ServiceReference serviceReference;

	@Configuration
	public Option[] config() {
		MavenArtifactUrlReference karafUrl = maven()
				.groupId( "org.apache.karaf" )
				.artifactId( "apache-karaf" )
				.version( "3.0.0" )
				.type( "tar.gz" );

		MavenUrlReference karafStandardRepo = maven()
				.groupId( "org.apache.karaf.features" )
				.artifactId( "standard" )
				.classifier( "features" )
				.type( "xml" )
				.versionAsInProject();

		File examDir = new File( "target/exam" );
		File ariesLogDir = new File( examDir, "/aries/log" );
		return new Option[] {
//				debugConfiguration( "5005", true ),
				logLevel( LogLevelOption.LogLevel.WARN ),
				karafDistributionConfiguration()
						.frameworkUrl( karafUrl )
						.unpackDirectory( examDir )
						.useDeployFolder( false ),
				keepRuntimeFolder(),
				features( karafStandardRepo, "scr" ),
				features(
						"mvn:org.hibernate/hibernate-search-integrationtest-osgi-features/" + currentVersion + "/xml/features",
						"hibernate-search"
				),

				// configure Aries transaction manager
				editConfigurationFilePut(
						"etc/org.apache.aries.transaction.cfg",
						"aries.transaction.howl.logFileDir", ariesLogDir.getAbsolutePath()
				),
				editConfigurationFilePut(
						"etc/org.apache.aries.transaction.cfg",
						"aries.transaction.recoverable", "true"
				),
				editConfigurationFilePut(
						"etc/org.apache.aries.transaction.cfg",
						"aries.transaction.timeout", "600"
				),
				// set the log level for the in container logging to INFO
				// also just logging to file (out), check data/log/karaf.log w/i the Karaf installation for the test execution log
				editConfigurationFilePut(
						"etc/org.ops4j.pax.logging.cfg",
						"log4j.rootLogger", "INFO, out"
				)
		};
	}

	@Before
	public void setUp() {
		serviceReference = bundleContext.getServiceReference( SessionFactory.class.getName() );
		assertNotNull( "The service reference should not be null", serviceReference );
	}

	@After
	public void tearDown() {
		bundleContext.ungetService( serviceReference );
	}

	@Test
	public void testAccessSessionFactory() throws Exception {
		SessionFactory sessionFactory = (SessionFactory) bundleContext.getService( serviceReference );
		assertNotNull( "The session factory should not be null", sessionFactory );
	}

	@Test
	public void testAccessSearchFactory() throws Exception {
		SessionFactory sessionFactory = (SessionFactory) bundleContext.getService( serviceReference );
		FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );
		assertNotNull( "Unable to create fulltext session from ORM Session", fullTextSession );

		SearchFactory searchFactory = fullTextSession.getSearchFactory();
		assertNotNull( "Unable to access SearchFactory", searchFactory );

		assertEquals( "There should only be one indexed type", 1, searchFactory.getIndexedTypes().size() );
		assertEquals( "Wrong indexed type", Muppet.class, searchFactory.getIndexedTypes().iterator().next() );
	}

	@Test
	public void testIndexAndSearch() throws Exception {
		SessionFactory sessionFactory = (SessionFactory) bundleContext.getService( serviceReference );
		FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );

		assertElmoIndexed( fullTextSession, false );

		persistElmo( fullTextSession );

		assertElmoIndexed( fullTextSession, true );
	}

	@Test
	public void testBatchIndexing() throws Exception {
		SessionFactory sessionFactory = (SessionFactory) bundleContext.getService( serviceReference );
		FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );

		AssertingMassIndexerProgressMonitor progressMonitor = new AssertingMassIndexerProgressMonitor( 0 );
		fullTextSession.createIndexer( Muppet.class ).progressMonitor( progressMonitor ).startAndWait();
		progressMonitor.assertExpectedProgressMade();

		persistElmo( fullTextSession );

		progressMonitor = new AssertingMassIndexerProgressMonitor( 1 );
		fullTextSession.createIndexer( Muppet.class ).progressMonitor( progressMonitor ).startAndWait();
		progressMonitor.assertExpectedProgressMade();
	}

	private void persistElmo(FullTextSession fullTextSession) {
		Transaction transaction = fullTextSession.beginTransaction();
		Muppet elmo = new Muppet( "Elmo" );
		fullTextSession.persist( elmo );
		transaction.commit();
		fullTextSession.clear();
	}

	private void assertElmoIndexed(FullTextSession fullTextSession, boolean indexed) {
		Query matchAllQuery = new MatchAllDocsQuery();
		FullTextQuery query = fullTextSession.createFullTextQuery( matchAllQuery, Muppet.class );
		if ( indexed ) {
			assertEquals( "Elmo should be there", 1, query.list().size() );
			Muppet muppet = (Muppet) query.list().get( 0 );
			assertEquals( "Index muppet is not Elmo", "Elmo", muppet.getName() );
		}
		else {
			assertEquals( "Elmo should not be there", 0, query.list().size() );
		}
	}

	public class AssertingMassIndexerProgressMonitor implements MassIndexerProgressMonitor {
		private final AtomicLong totalCount = new AtomicLong();
		private final AtomicLong finishedCount = new AtomicLong();

		private final int expectedTotalCount;

		public AssertingMassIndexerProgressMonitor(int expectedTotalCount) {
			this.expectedTotalCount = expectedTotalCount;
		}

		@Override
		public void documentsAdded(long increment) {
		}

		@Override
		public void documentsBuilt(int number) {
		}

		@Override
		public void entitiesLoaded(int size) {
		}

		@Override
		public void addToTotalCount(long count) {
			totalCount.addAndGet( count );
		}

		@Override
		public void indexingCompleted() {
			finishedCount.incrementAndGet();
		}

		public void assertExpectedProgressMade() {
			assertEquals( "Unexpected total count", expectedTotalCount, totalCount.get() );
			assertEquals( "Finished called more than once", 1, finishedCount.get() );
		}
	}
}


