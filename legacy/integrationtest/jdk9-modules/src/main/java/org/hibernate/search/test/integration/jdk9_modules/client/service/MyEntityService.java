/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jdk9_modules.client.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.integration.jdk9_modules.client.model.MyEntity;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.search.Query;

public class MyEntityService implements AutoCloseable {

	private final EntityManagerFactory emf;

	public MyEntityService() {
		emf = createSessionFactory();
	}

	private EntityManagerFactory createSessionFactory() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( "hibernate.search.default.directory_provider", "local-heap" );
		settings.put( Environment.ANALYZER_CLASS, EnglishAnalyzer.class.getName() );
		settings.put( "hibernate.search.default.indexwriter.merge_factor", "100" );
		settings.put( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySettings( settings );

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) registryBuilder.build();
		Metadata metadata = new MetadataSources( serviceRegistry ).addAnnotatedClass( MyEntity.class ).buildMetadata();
		SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		return sfb.build();
	}

	public void add(int id, String name) {
		EntityManager em = emf.createEntityManager();
		try {
			EntityTransaction tx = em.getTransaction();
			tx.begin();
			try {
				MyEntity entity = new MyEntity();
				entity.setId( id );
				entity.setName( name );
				em.persist( entity );
				tx.commit();
			}
			catch (Throwable e) {
				try {
					tx.rollback();
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
		finally {
			em.close();
		}
	}

	public List<Integer> search(String term) {
		EntityManager em = emf.createEntityManager();
		try {
			EntityTransaction tx = em.getTransaction();
			tx.begin();
			FullTextEntityManager ftEm = Search.getFullTextEntityManager( em );
			QueryBuilder queryBuilder = ftEm.getSearchFactory()
					.buildQueryBuilder()
					.forEntity( MyEntity.class )
					.get();
			Query luceneQuery = queryBuilder.keyword()
					.onField( "name" )
					.matching( term )
					.createQuery();
			FullTextQuery ftQuery = ftEm.createFullTextQuery( luceneQuery );
			List<MyEntity> entities = ftQuery.getResultList();
			List<Integer> ids = entities.stream().map( MyEntity::getId ).collect( Collectors.toList() );
			tx.rollback();
			return ids;
		}
		finally {
			em.close();
		}
	}

	@Override
	public void close() {
		emf.close();
	}
}
