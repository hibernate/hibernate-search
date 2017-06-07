/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import javax.inject.Inject;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;

/**
 * A basic integration test that executes Hibernate Search in Apache Karaf using
 * PaxExam (see <a href="https://ops4j1.jira.com/wiki/display/PAXEXAM3/Pax+Exam">online docs</a>).
 *
 * If there is a problem with the actual feature configuration, debugging this tests won't help,
 * since the Karaf container won't even start. In this case it is easiest to start a local Karaf installation:
 * <pre>
 * {@code
 * > ./karaf
 * }
 * </pre>
 *
 * Then in the Karaf console type:
 * <pre>
 * {@code
 * feature:repo-add mvn:org.hibernate/hibernate-search-integrationtest-osgi-features/<version>/xml/features
 * feature:install hibernate-search
 * }
 * </pre>
 *
 * You can then verify the bundle existence with command:
 * <pre>
 * {@code
 * feature:list
 * }
 * </pre>
 *
 * If/when investigating strange failures due to RMI registries, remember to check that
 * your local hostname resolves to 127.0.0.1.
 * In Fedora this isn't the case by default, and causes problems with Karaf.
 *
 * @author Hardy Ferentschik
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class HibernateSearchWithKarafIT {

	/**
	 * Switch to simplify debugging of this test
	 * when it fails
	 */
	private static final boolean DEBUG = false;

	@Inject
	private BundleContext bundleContext;

	private ServiceReference serviceReference;

	@Configuration
	public Option[] config() {
		MavenArtifactUrlReference karafUrl = maven()
				.groupId( "org.apache.karaf" )
				.artifactId( "apache-karaf" )
				.type( "tar.gz" )
				.versionAsInProject();

		MavenUrlReference karafStandardRepo = maven()
				.groupId( "org.apache.karaf.features" )
				.artifactId( "standard" )
				.classifier( "features" )
				.type( "xml" )
				.versionAsInProject();

		MavenUrlReference hibernateSearchFeature = maven()
				.groupId( "org.hibernate" )
				.artifactId( "hibernate-search-integrationtest-osgi-features" )
				.classifier( "features" )
				.type( "xml" )
				.versionAsInProject();

		File examDir = new File( "target/exam" );
		File ariesLogDir = new File( examDir, "/aries/log" );
		return new Option[] {
				DEBUG ? debugConfiguration( "5005", true ) : null,
				logLevel( LogLevelOption.LogLevel.WARN ),
				karafDistributionConfiguration()
						.frameworkUrl( karafUrl )
						.unpackDirectory( examDir )
						.useDeployFolder( false ),
				DEBUG ? keepRuntimeFolder() : null,
				configureConsole()
					.ignoreRemoteShell()
					.ignoreLocalConsole() ,
				features( karafStandardRepo, "scr" ),
				features( hibernateSearchFeature, "hibernate-search" ),

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
				editConfigurationFilePut(
						"etc/org.apache.karaf.management.cfg",
						"rmiServerHost", "127.0.0.1"
				),
				editConfigurationFilePut(
						"etc/org.apache.karaf.management.cfg",
						"rmiRegistryHost", "127.0.0.1"
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

	/**
	 * Starts all bundles to make sure there are no "uses constraint violations" caused by packages exported from the
	 * bundles contributed by the HSEARCH Karaf feature.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-1774")
	public void testStartingAllBundles() throws Exception {
		for ( Bundle bundle : bundleContext.getBundles() ) {
			if ( !isFragmentBundle( bundle ) ) {
				bundle.start();
			}
		}
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

	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleQueryStringDSL() throws Exception {
		SessionFactory sessionFactory = (SessionFactory) bundleContext.getService( serviceReference );
		FullTextSession fullTextSession = Search.getFullTextSession( sessionFactory.openSession() );

		persistElmo( fullTextSession );

		QueryBuilder qb = fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Muppet.class )
				.get();

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( qb.simpleQueryString().onField( "name" ).matching( "Elmo" ).createQuery(), Muppet.class );
		List<Muppet> results = fullTextQuery.getResultList();

		assertEquals( "Elmo should be there", 1, results.size() );
		Muppet muppet = results.get( 0 );
		assertEquals( "Index muppet is not Elmo", "Elmo", muppet.getName() );
	}

	private boolean isFragmentBundle(Bundle bundle) {
		return ( bundle.adapt( BundleRevision.class ).getTypes() & BundleRevision.TYPE_FRAGMENT ) != 0;
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
		private final LongAdder totalCount = new LongAdder();
		private final LongAdder finishedCount = new LongAdder();

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
			totalCount.add( count );
		}

		@Override
		public void indexingCompleted() {
			finishedCount.increment();
		}

		public void assertExpectedProgressMade() {
			assertEquals( "Unexpected total count", expectedTotalCount, totalCount.intValue() );
			assertEquals( "Finished called more than once", 1, finishedCount.intValue() );
		}
	}
}


