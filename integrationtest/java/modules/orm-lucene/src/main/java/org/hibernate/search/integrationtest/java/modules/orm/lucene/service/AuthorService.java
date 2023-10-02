/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.orm.lucene.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.java.modules.orm.lucene.entity.Author;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class AuthorService implements AutoCloseable {

	private final SessionFactory sessionFactory;

	public AuthorService() {
		sessionFactory = createSessionFactory();
	}

	private SessionFactory createSessionFactory() {
		StandardServiceRegistryBuilder registryBuilder = applyConnectionProperties( new StandardServiceRegistryBuilder() );
		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) registryBuilder.build();
		Metadata metadata = new MetadataSources( serviceRegistry ).addAnnotatedClass( Author.class ).buildMetadata();
		SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		return sfb.build();
	}

	private StandardServiceRegistryBuilder applyConnectionProperties(StandardServiceRegistryBuilder registryBuilder) {
		// DB properties:
		Map<String, Object> db = new HashMap<>();
		DatabaseContainer.configuration().add( db );
		registryBuilder.applySettings( db );
		// Other settings:

		// # Hibernate ORM properties:
		// ## Connection info: see above
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		registryBuilder.applySetting( "hibernate.show_sql", true );
		registryBuilder.applySetting( "hibernate.format_sql", true );
		registryBuilder.applySetting( "hibernate.max_fetch_depth", 5 );
		//  We can't use classes from the hibernate-testing module unless we add an explicit dependency to that module.
		// registryBuilder.applySetting( "hibernate.cache.region_prefix","hibernate.test");
		// registryBuilder.applySetting( "hibernate.cache.region.factory_class", "org.hibernate.testing.cache.CachingRegionFactory");

		// # Hibernate Search properties:
		// ## Connection info: see above
		registryBuilder.applySetting( "hibernate.search.backend.type", "lucene" );
		registryBuilder.applySetting( "hibernate.search.indexing.plan.synchronization.strategy", "sync" );
		registryBuilder.applySetting( "hibernate.search.backend.directory.type", "local-heap" );
		registryBuilder.applySetting( "hibernate.search.backend.analysis.configurer",
				org.hibernate.search.integrationtest.java.modules.orm.lucene.config.MyLuceneAnalysisConfigurer.class );

		return registryBuilder;
	}

	public void add(String name) {
		try ( Session session = sessionFactory.openSession() ) {
			session.getTransaction().begin();
			try {
				Author entity = new Author();
				entity.setName( name );
				session.persist( entity );
				session.getTransaction().commit();
			}
			catch (Throwable e) {
				try {
					session.getTransaction().rollback();
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
	}

	public List<Author> search(String term) {
		try ( Session session = sessionFactory.openSession() ) {
			SearchSession ftSession = Search.session( session );
			SearchQuery<Author> query = ftSession.search( Author.class )
					.where( f -> f.match().field( "name" ).matching( term ) )
					.toQuery();

			return query.fetchAllHits();
		}
	}

	@Override
	public void close() {
		sessionFactory.close();
	}
}
