/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.listAll;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.hibernate.testing.RequiresDialect;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.search.Query;

/**
 * This is a test class to check that search can be used with ORM in multi-tenancy.
 * <p>
 * The test will create one database for each tenant identifier.
 * The two tenant identifiers are "metamec" and "geochron".
 * <p>
 * Before running a test the DBs are populated with some clock instances.
 * Note that some instances have the same ID reused for different tenants, this is important to test the case
 * where a hit is found in the search but it's from the wrong tenant.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 * @since 5.2
 */
@RequiresDialect(
		comment = "The connection provider for this test ignores configuration and requires H2",
		strictMatching = true,
		value = org.hibernate.dialect.H2Dialect.class
)
public class DatabaseMultitenancyTest extends SearchTestBase {

	/**
	 * Metamec tenant identifier
	 */
	private static final String METAMEC_TID = "metamec";

	/**
	 * Geochron tenant identifier
	 */
	private static final String GEOCHRON_TID = "geochron";

	private static Clock[] METAMEC_MODELS = {
			new Clock( 1, "Metamec - Model A850" ),
			new Clock( 2, "Metamec - Model 4562" ),
			new Clock( 5, "Metamec - Model 792" )
	};

	private static Clock[] GEOCHRON_MODELS = {
			new Clock( 1, "Geochron - Model The Original Kilburg" ),
			new Clock( 2, "Geochron - Model The Boardroom" ),
			new Clock( 9, "Geochron - Model Designer Series" )
	};

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		Session sessionMetamec = openSessionWithTenantId( METAMEC_TID );
		persist( sessionMetamec, (Object[]) METAMEC_MODELS );
		sessionMetamec.close();

		Session sessionGeochron = openSessionWithTenantId( GEOCHRON_TID );
		persist( sessionGeochron, (Object[]) GEOCHRON_MODELS );
		sessionGeochron.close();
	}

	@Test
	public void shouldOnlyFindMetamecModels() throws Exception {
		List<Clock> list = searchAll( METAMEC_TID );
		assertThat( list ).isNotEmpty();
		assertThat( list ).containsExactlyInAnyOrder( METAMEC_MODELS );
	}

	@Test
	public void shouldOnlyFindGeochronModels() throws Exception {
		List<Clock> list = searchAll( GEOCHRON_TID );
		assertThat( list ).isNotEmpty();
		assertThat( list ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	public void shouldMatchOnlyElementsFromOneTenant() throws Exception {
		List<Clock> list = searchModel( "model", GEOCHRON_TID );
		assertThat( list ).isNotEmpty();
		assertThat( list ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	public void shouldBeAbleToPurgeTheIndex() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );

		List<Clock> listg = searchAll( GEOCHRON_TID );
		assertThat( listg ).isEmpty();
		List<Clock> listm = searchAll( METAMEC_TID );
		assertThat( listm ).isNotEmpty();
		assertThat( listm ).containsExactlyInAnyOrder( METAMEC_MODELS );
	}

	@Test
	public void shouldBeAbleToRebuildTheIndexForTheTenantId() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );
		purgeAll( Clock.class, METAMEC_TID );
		rebuildIndexWithMassIndexer( Clock.class, GEOCHRON_TID );

		List<Clock> listg = searchAll( GEOCHRON_TID );
		assertThat( listg ).isNotEmpty();
		assertThat( listg ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
		List<Clock> listm = searchAll( METAMEC_TID );
		assertThat( listm ).isEmpty();
	}

	@Test
	public void shouldOnlyPurgeTheEntitiesOfTheSelectedTenant() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );

		List<Clock> list = searchAll( METAMEC_TID );
		assertThat( list ).containsExactlyInAnyOrder( METAMEC_MODELS );
	}

	@Test
	public void shouldPurgeOnStartOnlyTheSelectedTenant() throws Exception {
		// This will run a purgeOnStart
		rebuildIndexWithMassIndexer( Clock.class, GEOCHRON_TID );

		List<Clock> metamecList = searchAll( METAMEC_TID );
		assertThat( metamecList ).containsExactlyInAnyOrder( METAMEC_MODELS );

		List<Clock> geochronList = searchAll( GEOCHRON_TID );
		assertThat( geochronList ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	public void shouldOnlyReturnResultsOfTheSpecificTenant() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );
		purgeAll( Clock.class, METAMEC_TID );
		rebuildIndexWithMassIndexer( Clock.class, GEOCHRON_TID );

		List<Clock> list = searchAll( METAMEC_TID );
		assertThat( list ).isEmpty();
	}

	@Test
	public void shouldSearchOtherTenantsDocuments() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );
		purgeAll( Clock.class, METAMEC_TID );
		rebuildIndexWithMassIndexer( Clock.class, GEOCHRON_TID );

		List<Clock> list = searchModel( "geochron", METAMEC_TID );
		assertThat( list ).isEmpty();
	}

	private List<Clock> searchModel(String searchString, String tenantId) {
		FullTextSession session = Search.getFullTextSession( openSessionWithTenantId( tenantId ) );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query luceneQuery = queryBuilder.keyword().wildcard().onField( "brand" ).matching( searchString ).createQuery();
		Transaction transaction = session.beginTransaction();
		@SuppressWarnings("unchecked")
		List<Clock> list = session.createFullTextQuery( luceneQuery ).list();
		transaction.commit();
		session.clear();
		session.close();
		return list;
	}

	private List<Clock> searchAll(String tenantId) {
		FullTextSession session = Search.getFullTextSession( openSessionWithTenantId( tenantId ) );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query luceneQuery = queryBuilder.all().createQuery();
		Transaction transaction = session.beginTransaction();
		@SuppressWarnings("unchecked")
		List<Clock> list = session.createFullTextQuery( luceneQuery ).list();
		transaction.commit();
		session.clear();
		session.close();
		return list;
	}

	private void rebuildIndexWithMassIndexer(Class<?> entityType, String tenantId) throws Exception {
		FullTextSession session = Search.getFullTextSession( openSessionWithTenantId( tenantId ) );
		session.createIndexer( entityType ).purgeAllOnStart( true ).startAndWait();
		session.close();
	}

	private void purgeAll(Class<?> entityType, String tenantId) throws IOException {
		FullTextSession session = Search.getFullTextSession( openSessionWithTenantId( tenantId ) );
		session.purgeAll( entityType );
		session.flushToIndexes();
		session.close();
	}

	private Session openSessionWithTenantId(String tenantId) {
		return getSessionFactory().withOptions().tenantIdentifier( tenantId ).openSession();
	}

	private void persist(Session session, Object... clocks) {
		session.beginTransaction();
		for ( Object clock : clocks ) {
			session.persist( clock );
		}
		session.getTransaction().commit();
		session.clear();
	}

	@After
	public void deleteEntities() throws Exception {
		Session session = openSessionWithTenantId( METAMEC_TID );
		deleteClocks( session );
		session.close();

		session = openSessionWithTenantId( GEOCHRON_TID );
		deleteClocks( session );
		session.close();
	}

	private void deleteClocks(Session session) {
		session.beginTransaction();
		List<Clock> clocks = listAll( session, Clock.class );
		for ( Clock clock : clocks ) {
			session.delete( clock );
		}
		session.getTransaction().commit();
		session.clear();
	}

	// Test setup configuration:

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Clock.class };
	}

	@Override
	public Set<String> multiTenantIds() {
		return CollectionHelper.asSet( METAMEC_TID, GEOCHRON_TID );
	}

}
