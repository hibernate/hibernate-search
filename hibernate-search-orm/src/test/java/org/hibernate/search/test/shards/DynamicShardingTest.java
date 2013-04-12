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

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DynamicShardingTest extends SearchTestCase {

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		//is the default when multiple shards are set up
		//cfg.setProperty( "hibernate.search.Animal.sharding_strategy", IdHashShardingStrategy.class );
		cfg.setProperty( "hibernate.search.Animal.sharding_strategy.nbr_of_shards", "dynamic" );
		cfg.setProperty( "hibernate.search.Animal.sharding_strategy.shard_identity_provider", AnimalShardProvider.class.getName() );
	}


	public void testSharding() throws Exception {
		Session s = openSession();
		EntityIndexBinder binder = getSearchFactoryImpl().getIndexBindingForEntity().get(Animal.class);
		assertThat(binder.getIndexManagers()).hasSize( 0 );
		Transaction tx = s.beginTransaction();
		Animal a = new Animal();
		a.setId( 1 );
		a.setName( "Elephant" );
		a.setType( "Mammal" );
		s.persist( a );
		tx.commit();
		s.clear();

		assertThat(binder.getIndexManagers()).hasSize( 1 );

		tx = s.beginTransaction();
		a = new Animal();
		a.setId( 2 );
		a.setName( "Spider" );
		a.setType( "Insect" );
		s.persist( a );
		tx.commit();
		s.clear();

		assertThat(binder.getIndexManagers()).hasSize( 2 );

		tx = s.beginTransaction();
		a = new Animal();
		a.setId( 3 );
		a.setName( "Bear" );
		a.setType( "Mammal" );
		s.persist( a );
		tx.commit();
		s.clear();

		assertThat(binder.getIndexManagers()).hasSize( 2 );

		FSDirectory animalMammalDirectory = FSDirectory.open( new File( getBaseIndexDir(), "Animal.Mammal" ) );
		try {
			IndexReader reader = IndexReader.open( animalMammalDirectory );
			try {
				int num = reader.numDocs();
				assertEquals( 2, num );
			}
			finally {
				reader.close();
			}
		}
		finally {
			animalMammalDirectory.close();
		}

		FSDirectory animalInsectDirectory = FSDirectory.open( new File( getBaseIndexDir(), "Animal.Insect" ) );
		try {
			IndexReader reader = IndexReader.open( animalInsectDirectory );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
			}
			finally {
				reader.close();
			}
		}
		finally {
			animalInsectDirectory.close();
		}

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );

		List results = fts.createFullTextQuery( parser.parse( "name:bear OR name:elephant OR name:spider" ) ).list();
		assertEquals( "Either double insert, single update, or query fails with shards", 3, results.size() );
		for ( Object o : results ) {
			s.delete( o );
		}
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Animal.class,
				Furniture.class
		};
	}

	public static class AnimalShardProvider implements ShardIdentifierProvider {
		private ConcurrentHashMap<String,String> shards = new ConcurrentHashMap<String,String>();

		@Override
		public void initialize(Properties properties) {
		}

		@Override
		public String getShardIdentifier(Class<?> entity, Serializable id, String idInString, Document document) {
			if ( entity.equals( Animal.class ) ) {
				String type = document.getFieldable( "type" ).stringValue();
				shards.put( type, type );
				return type;
			}
			throw new RuntimeException( "Animal expected but found " + entity );
		}

		@Override
		public String[] getShardIdentifiers(Class<?> entity, Serializable id, String idInString) {
			return getAllShardIdentifiers();
		}

		@Override
		public String[] getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters) {
			return getAllShardIdentifiers();
		}

		@Override
		public String[] getAllShardIdentifiers() {
			return shards.keySet().toArray( new String[shards.size()] );
		}
	}
}
