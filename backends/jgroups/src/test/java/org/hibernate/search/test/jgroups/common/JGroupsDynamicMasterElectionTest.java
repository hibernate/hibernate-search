/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.jgroups.impl.DispatchMessageSender;
import org.hibernate.search.backend.jgroups.impl.NodeSelectorService;
import org.hibernate.search.backend.jgroups.impl.NodeSelectorStrategy;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.test.DefaultTestResourceManager;
import org.hibernate.search.test.TestResourceManager;
import org.hibernate.search.test.jgroups.master.TShirt;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Moren
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-2675")
public class JGroupsDynamicMasterElectionTest extends DynamicMasterSlaveSearchTestCase {

	public static final String TESTING_JGROUPS_CONFIGURATION_FILE = "testing-flush-loopback.xml";
	public static final Poller POLLER = Poller.milliseconds( 10_000, 100 );

	/*
	 * Must be at least 3 so as to highlight the bug mentioned in HSEARCH-2675:
	 * for some reason, the master will change automatically when we spawn the second
	 * node, so the first two nodes will always be able to handle master works.
	 * The third one and later will not, however.
	 */
	private static final int DEFAULT_NUMBER_OF_NODES = 10;

	/**
	 * Name of the JGroups channel used in test
	 */
	public static final String CHANNEL_NAME = UUID.randomUUID().toString();

	private final QueryParser parser = new QueryParser(
			"id",
			TestConstants.stopAnalyzer
	);

	@Override
	protected int getExpectedNumberOfNodes() {
		return DEFAULT_NUMBER_OF_NODES;
	}

	@Test
	public void masterElection() throws Exception {
		TestResourceManager masterResourceManager = determineJGroupsMaster().get();
		List<DefaultTestResourceManager> slaveResourceManagers = determineJGroupsSlaves();
		Assert.assertEquals( getExpectedNumberOfNodes() - 1, slaveResourceManagers.size() );

		// Check that the first master works fine
		TShirt ts = new TShirt();
		ts.setLogo( "Boston" );
		ts.setSize( "XXL" );
		ts.setLength( 23.4d );
		testAdd( masterResourceManager, slaveResourceManagers, ts, 1 );

		// Kill the master
		masterResourceManager.getSessionFactory().close();

		// ... check that a new master is elected
		POLLER.pollAssertion( () -> {
			Assert.assertTrue( "Lots of time waited and still no new master has been elected!", determineJGroupsMaster().isPresent() );
		} );

		masterResourceManager = determineJGroupsMaster().get();
		slaveResourceManagers = determineJGroupsSlaves();
		Assert.assertEquals( getExpectedNumberOfNodes() - 2, slaveResourceManagers.size() );

		// ... and check that the new master actually performs work
		TShirt ts2 = new TShirt();
		ts2.setLogo( "Mapple leaves" );
		ts2.setSize( "L" );
		ts2.setLength( 23.42d );
		testAdd( masterResourceManager, slaveResourceManagers, ts2, 2 );
	}

	private void testAdd(TestResourceManager masterResourceManager, List<DefaultTestResourceManager> slaveResourceManagers,
			TShirt ts, int expectedResults) throws ParseException {
		try ( Session slaveSession = slaveResourceManagers.get( 0 ).openSession() ) {
			Transaction tx = slaveSession.beginTransaction();
			slaveSession.persist( ts );
			tx.commit();

			try ( Session masterSession = masterResourceManager.openSession() ) {
				// since this is an async backend, we expect to see
				// the values in the index *eventually*.
				POLLER.pollAssertion( () -> {
					List<?> result = doQuery( masterSession );
					Assert.assertEquals( "Lots of time waited and still the document is not indexed on master yet!",
							expectedResults, result.size() );
				} );
			}
		}

		// Wait for the changes to be visible from the slaves
		POLLER.pollAssertion( () -> {
			for ( TestResourceManager resourceManager : slaveResourceManagers ) {
				try ( Session slaveSession = resourceManager.openSession() ) {
					List<?> result = doQuery( slaveSession );
					Assert.assertEquals( "Lots of time waited and still the document is not visible from the slave yet!",
							expectedResults, result.size() );
				}
			}
		} );
	}

	private List<?> doQuery(Session slaveSession) throws ParseException {
		FullTextSession ftSession = Search.getFullTextSession( slaveSession );
		Query luceneQuery = parser.parse( "logo:Boston or logo:Mapple leaves" );
		slaveSession.getTransaction().begin();
		FullTextQuery query = ftSession.createFullTextQuery( luceneQuery );
		List<?> result = query.list();
		slaveSession.getTransaction().commit();
		return result;
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		//master jgroups configuration
		super.configure( cfg );
		cfg.put( "hibernate.search.default.retry_initialize_period", "1" );
		cfg.put( "hibernate.search.default." + DispatchMessageSender.CLUSTER_NAME, CHANNEL_NAME );
		cfg.put( DispatchMessageSender.CONFIGURATION_FILE, TESTING_JGROUPS_CONFIGURATION_FILE );

		/*
		 * Do *not* drop the schema upon factory closing, or the slave won't be able to use it.
		 */
		cfg.put( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "drop-and-create" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { TShirt.class };
	}

	private boolean isActive(TestResourceManager manager) {
		return !manager.getSessionFactory().isClosed();
	}

	private boolean isJGroupsMaster(TestResourceManager manager) {
		ExtendedSearchIntegrator integrator = manager.getExtendedSearchIntegrator();

		try ( ServiceReference<NodeSelectorService> service =
				integrator.getServiceManager().requestReference( NodeSelectorService.class ) ) {
			NodeSelectorStrategy nodeSelector = service.get().getMasterNodeSelector( TShirt.INDEX_NAME );
			return nodeSelector.isIndexOwnerLocal();
		}
	}

	private Optional<DefaultTestResourceManager> determineJGroupsMaster() {
		return getResourceManagers().stream()
				.filter( this::isActive )
				.filter( this::isJGroupsMaster )
				.findFirst();
	}

	private List<DefaultTestResourceManager> determineJGroupsSlaves() {
		return getResourceManagers().stream()
				.filter( this::isActive )
				.filter( (manager) -> !isJGroupsMaster(manager) )
				.collect( Collectors.toList() );
	}
}
