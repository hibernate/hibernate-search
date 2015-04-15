/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.h2.Driver;
import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.tool.hbm2ddl.ConnectionHelper;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
 */
@RequiresDialect(
	comment = "The connection provider for this test requires H2",
	strictMatching = true,
	value = org.hibernate.dialect.H2Dialect.class
)
public class DatabaseMultitenancyTest extends SearchTestBase {

	public static final Dialect DIALECT = new H2Dialect();

	/**
	 * Metamec tenant identifier
	 */
	private static final String METAMEC_TID = "metamec";

	/**
	 * Geochron tenant identifier
	 */
	private static final String GEOCHRON_TID = "geochron";

	private static Clock[] METAMEC_MODELS = {
		new Clock( 1, "Metamec - Model A850"),
		new Clock( 2, "Metamec - Model 4562"),
		new Clock( 5, "Metamec - Model 792")
		};

	private static Clock[] GEOCHRON_MODELS = {
		new Clock( 1, "Geochron - Model The Original Kilburg" ),
		new Clock( 2, "Geochron - Model The Boardroom" ),
		new Clock( 9, "Geochron - Model Designer Series")
		};

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name() );
		cfg.setProperty( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, ClockMultitenantConnectionProvider.class.getName() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );

		// Multi-tenancy does not work well with SchemaExport
		cfg.getProperties().remove( AvailableSettings.HBM2DDL_AUTO );
	}

	@Override
	@Before
	public void setUp() throws Exception {
		ClockMultitenantConnectionProvider.start();
		super.setUp();

		exportSchema( ClockMultitenantConnectionProvider.GEOCHRON_PROVIDER, getCfg() );
		exportSchema( ClockMultitenantConnectionProvider.METAMEC_PROVIDER, getCfg() );

		Session sessionMetamec = openSessionWithTenantId( METAMEC_TID );
		persist( sessionMetamec, METAMEC_MODELS );
		sessionMetamec.close();

		Session sessionGeochron = openSessionWithTenantId( GEOCHRON_TID );
		persist( sessionGeochron, GEOCHRON_MODELS );
		sessionGeochron.close();
	}

	@Test
	public void shouldOnlyFindMetamecModels() throws Exception {
		List<Clock> list = searchAll( METAMEC_TID );
		assertThat( list ).isNotEmpty();
		assertThat( list ).containsOnly( METAMEC_MODELS );
	}

	@Test
	public void shouldOnlyFindGeochronModels() throws Exception {
		List<Clock> list = searchAll( GEOCHRON_TID );
		assertThat( list ).isNotEmpty();
		assertThat( list ).containsOnly( GEOCHRON_MODELS );
	}

	@Test
	public void shouldMatchOnlyElementsFromOneTenant() throws Exception {
		List<Clock> list = searchModel( "model", GEOCHRON_TID );
		assertThat( list ).isNotEmpty();
		assertThat( list ).containsOnly( GEOCHRON_MODELS );
	}

	@Test
	public void shouldBeAbleToPurgeTheIndex() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );

		List<Clock> listg = searchAll( GEOCHRON_TID );
		assertThat( listg ).isEmpty();
		List<Clock> listm = searchAll( METAMEC_TID );
		assertThat( listm ).isNotEmpty();
		assertThat( listm ).containsOnly( METAMEC_MODELS );
	}

	@Test
	public void shouldBeAbleToRebuildTheIndexForTheTenantId() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );
		purgeAll( Clock.class, METAMEC_TID );
		rebuildIndexWithMassIndexer( Clock.class, GEOCHRON_TID );

		List<Clock> listg = searchAll( GEOCHRON_TID );
		assertThat( listg ).isNotEmpty();
		assertThat( listg ).containsOnly( GEOCHRON_MODELS );
		List<Clock> listm = searchAll( METAMEC_TID );
		assertThat( listm ).isEmpty();
	}

	@Test
	public void shouldOnlyPurgeTheEntitiesOfTheSelectedTenant() throws Exception {
		purgeAll( Clock.class, GEOCHRON_TID );

		List<Clock> list = searchAll( METAMEC_TID );
		assertThat( list ).containsOnly( METAMEC_MODELS );
	}

	@Test
	public void shouldPurgeOnStartOnlyTheSelectedTenant() throws Exception {
		// This will run a purgeOnStart
		rebuildIndexWithMassIndexer( Clock.class, GEOCHRON_TID );

		List<Clock> metamecList = searchAll( METAMEC_TID );
		assertThat( metamecList ).containsOnly( METAMEC_MODELS );

		List<Clock> geochronList = searchAll( GEOCHRON_TID );
		assertThat( geochronList ).containsOnly( GEOCHRON_MODELS );
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
		IndexReader indexReader = session.getSearchFactory().getIndexReaderAccessor().open( entityType );
		long tenantIdTermFreq = indexReader.docFreq( new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId ) );
		session.close();
		assertThat( tenantIdTermFreq ).isGreaterThan( 0 );
	}

	private void purgeAll(Class<?> entityType, String tenantId) throws IOException {
		FullTextSession session = Search.getFullTextSession( openSessionWithTenantId( tenantId ) );
		session.purgeAll( entityType );
		session.flushToIndexes();
		IndexReader indexReader = session.getSearchFactory().getIndexReaderAccessor().open( entityType );
		long tenantIdTermFreq = indexReader.docFreq( new Term( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId ) );
		session.close();
		assertThat( tenantIdTermFreq ).isEqualTo( 0 );
	}

	private Session openSessionWithTenantId(String tenantId) {
		return getSessionFactory().withOptions().tenantIdentifier( tenantId ).openSession();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Clock.class };
	}

	private void persist(Session session, Clock... clocks) {
		session.beginTransaction();
		for ( Clock clock : clocks ) {
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
		ClockMultitenantConnectionProvider.stop();
	}

	private void deleteClocks(Session session) {
		session.beginTransaction();
		@SuppressWarnings("unchecked")
		List<Clock> clocks = session.createCriteria( Clock.class ).list();
		for ( Clock clock : clocks ) {
			session.delete( clock );
		}
		session.getTransaction().commit();
		session.clear();
	}

	/*
	 * Hibernate does not generate the schema when using multi-tenancy.
	 * We have to call the SchemaExport class explicitly.
	 */
	private void exportSchema(final ConnectionProvider provider, Configuration cfg) {
		String[] generateDropSchemaScript = cfg.generateDropSchemaScript( DIALECT );
		String[] generateSchemaCreationScript = cfg.generateSchemaCreationScript( DIALECT );
		new SchemaExport( new ConnectionHelper() {

			private Connection connection;

			@Override
			public void prepare(boolean needsAutoCommit) throws SQLException {
				connection = provider.getConnection();
			}

			@Override
			public Connection getConnection() throws SQLException {
				return connection;
			}

			@Override
			public void release() throws SQLException {
				provider.closeConnection( connection );
			}
		},

		generateDropSchemaScript, generateSchemaCreationScript ).execute( false, true, false, false );
	}

	public static class ClockMultitenantConnectionProvider extends AbstractMultiTenantConnectionProvider {

		private static DriverManagerConnectionProviderImpl METAMEC_PROVIDER;
		private static DriverManagerConnectionProviderImpl GEOCHRON_PROVIDER;

		@Override
		protected ConnectionProvider getAnyConnectionProvider() {
			return GEOCHRON_PROVIDER;
		}

		@Override
		protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
			if ( METAMEC_TID.equals( tenantIdentifier ) ) {
				return METAMEC_PROVIDER;
			}
			else if ( GEOCHRON_TID.equals( tenantIdentifier ) ) {
				return GEOCHRON_PROVIDER;
			}
			throw new HibernateException( "Unknown tenant identifier: " + tenantIdentifier );
		}

		private static DriverManagerConnectionProviderImpl buildConnectionProvider(String tenantId) {
			DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
			connectionProvider.configure( getConnectionProviderProperties( tenantId + "-db" ) );
			return connectionProvider;
		}

		public static Properties getConnectionProviderProperties(String dbName) {
			Properties props = new Properties( null );
			props.put( Environment.DRIVER, Driver.class.getName() );
			props.put( Environment.URL, String.format( "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MVCC=TRUE", dbName) );
			props.put( Environment.USER, "sa" );
			props.put( Environment.PASS, "" );
			return props;
		}

		static void stop() {
			METAMEC_PROVIDER.stop();
			GEOCHRON_PROVIDER.stop();
			//Cleanup hacky static variables
			METAMEC_PROVIDER = null;
			GEOCHRON_PROVIDER = null;
		}

		static void start() {
			METAMEC_PROVIDER = buildConnectionProvider( METAMEC_TID );
			GEOCHRON_PROVIDER = buildConnectionProvider( GEOCHRON_TID );
		}
	}

}
