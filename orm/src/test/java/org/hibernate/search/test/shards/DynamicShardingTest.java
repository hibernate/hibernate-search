/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.impl.HibernateSessionFactoryService;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.ShardIdentifierProviderTemplate;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 0 );

		insertAnimals( elephant );
		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 1 );

		insertAnimals( spider );
		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 2 );

		insertAnimals( bear );
		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 2 );

		assertNumberOfEntitiesInIndex( "Animal.Mammal", 2 );
		assertNumberOfEntitiesInIndex( "Animal.Insect", 1 );
	}

	@Test
	public void testDynamicShardsAreTargetingInQuery() throws Exception {
		insertAnimals( elephant, spider, bear );

		Session session = openSession();
		Transaction tx = session.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( session );
		QueryParser parser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"id",
				TestConstants.stopAnalyzer
		);

		List results = fts.createFullTextQuery( parser.parse( "name:bear OR name:elephant OR name:spider" ) ).list();
		assertEquals( "Either double insert, single update, or query fails with shards", 3, results.size() );
		tx.commit();
		session.close();
	}

	@Test
	public void testInitialiseDynamicShardsOnStartup() throws Exception {
		EntityIndexBinding entityIndexBinding = getExtendedSearchIntegrator().getIndexBindings().get( Animal.class );
		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 0 );

		insertAnimals( elephant, spider, bear );

		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 2 );

		ExtendedSearchIntegrator integrator = getIndependentNewSearchIntegrator();
		entityIndexBinding = integrator.getIndexBindings().get( Animal.class );

		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 2 );
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );

		cfg.setProperty( "hibernate.search.Animal.sharding_strategy", AnimalShardIdentifierProvider.class.getName() );

		// use filesystem based directory provider to be able to assert against index
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Animal.class
		};
	}

	private void assertNumberOfEntitiesInIndex(String indexName, int expectedCount) throws IOException {
		FSDirectory fsDirectory = FSDirectory.open( new File( getBaseIndexDir(), indexName ) );
		try {
			IndexReader reader = DirectoryReader.open( fsDirectory );
			try {
				int actualCount = reader.numDocs();
				assertEquals( "Unexpected document count", expectedCount, actualCount );
			}
			finally {
				reader.close();
			}
		}
		finally {
			fsDirectory.close();
		}
	}

	private void insertAnimals(Animal... animals) {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		for ( Animal animal : animals ) {
			session.persist( animal );
		}
		tx.commit();
		session.clear();
	}

	private ExtendedSearchIntegrator getIndependentNewSearchIntegrator() {
		// build a new independent SessionFactory to verify that the shards are available at restart
		Configuration config = new Configuration();

		config.setProperty(
				"hibernate.search.Animal.sharding_strategy",
				DynamicShardingTest.AnimalShardIdentifierProvider.class.getName()
		);

		// use filesystem based directory provider to be able to assert against index
		config.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		File sub = getBaseIndexDir();
		config.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );

		config.addAnnotatedClass( Animal.class );

		SessionFactory newSessionFactory = config.buildSessionFactory();
		FullTextSession fullTextSession = Search.getFullTextSession( newSessionFactory.openSession() );
		return fullTextSession.getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
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
