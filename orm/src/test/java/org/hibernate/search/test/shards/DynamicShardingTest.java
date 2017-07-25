/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.impl.HibernateSessionFactoryService;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.ShardIdentifierProviderTemplate;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class DynamicShardingTest extends SearchTestBase {

	private Animal elephant;
	private Animal spider;
	private Animal bear;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		elephant = new Animal();
		elephant.setId( 1 );
		elephant.setName( "Elephant" );
		elephant.setType( "Mammal" );

		spider = new Animal();
		spider.setId( 2 );
		spider.setName( "Spider" );
		spider.setType( "Insect" );

		bear = new Animal();
		bear.setId( 3 );
		bear.setName( "Bear" );
		bear.setType( "Mammal" );
	}

	@Test
	public void testDynamicCreationOfShards() throws Exception {
		EntityIndexBinding entityIndexBinding = getExtendedSearchIntegrator().getIndexBindings().get( Animal.class );
		assertThat( entityIndexBinding.getIndexManagerSelector().all() ).hasSize( 0 );

		insert( elephant );
		assertThat( entityIndexBinding.getIndexManagerSelector().all() ).hasSize( 1 );

		insert( spider );
		assertThat( entityIndexBinding.getIndexManagerSelector().all() ).hasSize( 2 );

		insert( bear );
		assertThat( entityIndexBinding.getIndexManagerSelector().all() ).hasSize( 2 );

		assertEquals( 2, getNumberOfDocumentsInIndex( "Animal.Mammal" ) );
		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal.Insect" ) );
	}

	@Test
	public void testDynamicShardsAreTargetingInQuery() throws Exception {
		insert( elephant, spider, bear );

		Session session = openSession();
		Transaction tx = session.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( session );
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );

		List results = fts.createFullTextQuery( parser.parse( "name:bear OR name:elephant OR name:spider" ) ).list();
		assertEquals( "Either double insert, single update, or query fails with shards", 3, results.size() );
		tx.commit();
		session.close();
	}

	@Test
	public void testInitialiseDynamicShardsOnStartup() throws Exception {
		EntityIndexBinding entityIndexBinding = getExtendedSearchIntegrator().getIndexBindings().get( Animal.class );
		assertThat( entityIndexBinding.getIndexManagerSelector().all() ).hasSize( 0 );

		insert( elephant, spider, bear );

		assertThat( entityIndexBinding.getIndexManagerSelector().all() ).hasSize( 2 );

		assertThat( getIndexManagersAfterReopening() ).hasSize( 2 );
	}

	@Test
	public void testDeletion() throws Exception {
		insert( elephant, spider, bear );

		assertEquals( 2, getNumberOfDocumentsInIndex( "Animal.Mammal" ) );
		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal.Insect" ) );

		deleteAnimal( elephant );

		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal.Mammal" ) );
		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal.Insect" ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2662")
	public void testQueryWhenEmpty() throws Exception {
		HSQuery query = getExtendedSearchIntegrator().createHSQuery( new MatchAllDocsQuery(), Animal.class );
		assertEquals( 0, query.queryResultSize() );

		SomeOtherEntity someOtherIndexedObject = new SomeOtherEntity();
		insert( someOtherIndexedObject );

		HSQuery queryAgain = getExtendedSearchIntegrator().createHSQuery( new MatchAllDocsQuery(), Animal.class );
		assertEquals( 0, queryAgain.queryResultSize() );
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.Animal.sharding_strategy", AnimalShardIdentifierProvider.class.getName() );

		// use filesystem based directory provider to be able to assert against index
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		Path sub = getBaseIndexDir();
		cfg.put( "hibernate.search.default.indexBase", sub.toAbsolutePath().toString() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Animal.class, SomeOtherEntity.class
		};
	}

	private void insert(Object... entities) {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			for ( Object entity : entities ) {
				session.persist( entity );
			}
			tx.commit();
		}
	}

	private void deleteAnimal(Object entity) {
		try (Session session = openSession()) {
			Transaction tx = session.beginTransaction();
			session.delete( entity );
			tx.commit();
		}
	}

	private Set<IndexManager> getIndexManagersAfterReopening() {
		// build a new independent SessionFactory to verify that the shards are available at restart
		Configuration config = new Configuration();

		config.setProperty(
				"hibernate.search.Animal.sharding_strategy",
				DynamicShardingTest.AnimalShardIdentifierProvider.class.getName()
		);

		// use filesystem based directory provider to be able to assert against index
		config.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		Path sub = getBaseIndexDir();
		config.setProperty( "hibernate.search.default.indexBase", sub.toAbsolutePath().toString() );

		config.addAnnotatedClass( Animal.class );

		try ( SessionFactory newSessionFactory = config.buildSessionFactory() ) {
			try ( FullTextSession fullTextSession = Search.getFullTextSession( newSessionFactory.openSession() ) ) {
				ExtendedSearchIntegrator integrator = fullTextSession.getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
				return integrator.getIndexBindings().get( Animal.class ).getIndexManagerSelector().all();
			}
		}
	}

	@Indexed
	@Entity(name = "SomeOtherEntity")
	private static class SomeOtherEntity {
		@DocumentId
		@Field(name = "idField")
		@Id
		@GeneratedValue
		private Integer id;

		protected SomeOtherEntity() {
		}
	}

	public static class AnimalShardIdentifierProvider extends ShardIdentifierProviderTemplate {

		@Override
		public String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document) {
			if ( entityType.equals( Animal.class ) ) {
				final String typeValue = document.getField( "type" ).stringValue();
				addShard( typeValue );
				return typeValue;
			}
			throw new RuntimeException( "Animal expected but found " + entityType );
		}

		@Override
		protected Set<String> loadInitialShardNames(Properties properties, BuildContext buildContext) {
			ServiceManager serviceManager = buildContext.getServiceManager();
			SessionFactory sessionFactory = serviceManager.requestService( HibernateSessionFactoryService.class ).getSessionFactory();
			Session session = sessionFactory.openSession();
			try {
				Criteria initialShardsCriteria = session.createCriteria( Animal.class );
				initialShardsCriteria.setProjection( Projections.distinct( Property.forName( "type" ) ) );

				@SuppressWarnings("unchecked")
				List<String> initialTypes = initialShardsCriteria.list();
				return new HashSet<String>( initialTypes );
			}
			finally {
				session.close();
			}
		}
	}
}
