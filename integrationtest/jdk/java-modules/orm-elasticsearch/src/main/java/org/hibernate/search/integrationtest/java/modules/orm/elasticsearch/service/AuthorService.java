/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.service;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.entity.Author;
import org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.entity.AuthorWithSlightlyDifferentIndex;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class AuthorService implements AutoCloseable {

	private final SessionFactory sessionFactory;

	public AuthorService() {
		sessionFactory = createSessionFactory();
	}

	private SessionFactory createSessionFactory() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) registryBuilder.build();
		Metadata metadata = new MetadataSources( serviceRegistry ).addAnnotatedClass( Author.class ).buildMetadata();
		SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		return sfb.build();
	}

	public void triggerValidationFailure() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.applySetting( HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, "none" );
		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) registryBuilder.build();
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( AuthorWithSlightlyDifferentIndex.class )
				.buildMetadata();
		SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		try ( SessionFactory sessionFactory = sfb.build() ) {
			Search.mapping( sessionFactory ).scope( Object.class ).schemaManager().validate();
			throw new AssertionFailure( "Validation failure was not triggered?" );
		}
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
