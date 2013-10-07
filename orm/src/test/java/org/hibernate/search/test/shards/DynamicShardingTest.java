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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
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
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.hcore.impl.HibernateSessionFactoryServiceProvider;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.ShardIdentifierProviderTemplate;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.TestConstants;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DynamicShardingTest extends SearchTestCaseJUnit4 {

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
		EntityIndexBinding entityIndexBinding = getSearchFactoryImpl().getIndexBindings().get( Animal.class );
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
		EntityIndexBinding entityIndexBinding = getSearchFactoryImpl().getIndexBindings().get( Animal.class );
		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 0 );

		insertAnimals( elephant, spider, bear );

		assertThat( entityIndexBinding.getIndexManagers() ).hasSize( 2 );

		SearchFactoryImplementor newSearchFactory = getIndependentNewSearchFactory();
		entityIndexBinding = newSearchFactory.getIndexBindings().get( Animal.class );

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
			IndexReader reader = IndexReader.open( fsDirectory );
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

	private SearchFactoryImplementor getIndependentNewSearchFactory() {
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
		return (SearchFactoryImplementor) fullTextSession.getSearchFactory();
	}

	public static class AnimalShardIdentifierProvider extends ShardIdentifierProviderTemplate {

		@Override
		public String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document) {
			if ( entityType.equals( Animal.class ) ) {
				String type = document.getFieldable( "type" ).stringValue();
				addShard( type );
				return type;
			}
			throw new RuntimeException( "Animal expected but found " + entityType );
		}

		@Override
		protected Set<String> loadInitialShardNames(Properties properties, BuildContext buildContext) {
			ServiceManager serviceManager = buildContext.getServiceManager();
			SessionFactory sessionFactory = serviceManager.requestService( HibernateSessionFactoryServiceProvider.class, buildContext );
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
