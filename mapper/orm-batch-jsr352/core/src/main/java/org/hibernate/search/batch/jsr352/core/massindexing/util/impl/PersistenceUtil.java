/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.IndexScope;
import org.hibernate.search.util.common.impl.StringHelper;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

/**
 * Internal utility class for persistence usage.
 *
 * @author Mincong Huang
 */
public final class PersistenceUtil {

	private PersistenceUtil() {
		// Private constructor, do not use it.
	}

	/**
	 * Open a session with specific tenant ID. If the tenant ID argument is {@literal null} or empty, then a normal
	 * session will be returned. The entity manager factory should be not null and opened when calling this method.
	 *
	 * @param entityManagerFactory entity manager factory
	 * @param tenantId tenant ID, can be {@literal null} or empty.
	 * @return a new session
	 */
	public static Session openSession(EntityManagerFactory entityManagerFactory, String tenantId) {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		@SuppressWarnings("rawtypes")
		SessionBuilder builder = sessionFactory.withOptions();
		if ( StringHelper.isNotEmpty( tenantId ) ) {
			builder.tenantIdentifier( tenantId );
		}
		Session session = builder.openSession();
		// We don't need to write to the database
		session.setDefaultReadOnly( true );
		// ... thus flushes are not necessary.
		session.setHibernateFlushMode( FlushMode.MANUAL );
		return session;
	}

	/**
	 * Open a stateless session with specific tenant ID. If the tenant ID argument is {@literal null} or empty, then a
	 * normal stateless session will be returned. The entity manager factory should be not null and opened when calling
	 * this method.
	 *
	 * @param entityManagerFactory entity manager factory
	 * @param tenantId tenant ID, can be {@literal null} or empty.
	 * @return a new stateless session
	 */
	public static StatelessSession openStatelessSession(EntityManagerFactory entityManagerFactory, String tenantId) {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		@SuppressWarnings("rawtypes")
		StatelessSessionBuilder builder = sessionFactory.withStatelessOptions();
		if ( StringHelper.isNotEmpty( tenantId ) ) {
			builder.tenantIdentifier( tenantId );
		}
		return builder.openStatelessSession();
	}

	/**
	 * Determines the index scope using the input parameters.
	 *
	 * @see IndexScope
	 */
	public static IndexScope getIndexScope(String hql) {
		if ( StringHelper.isNotEmpty( hql ) ) {
			return IndexScope.HQL;
		}
		else {
			return IndexScope.FULL_ENTITY;
		}
	}

	public static List<EntityTypeDescriptor> createDescriptors(EntityManagerFactory entityManagerFactory, Set<Class<?>> types) {
		SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap( SessionFactoryImplementor.class );
		List<EntityTypeDescriptor> result = new ArrayList<>( types.size() );
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		for ( Class<?> type : types ) {
			result.add( createDescriptor( metamodel, type ) );
		}
		return result;
	}

	private static <T> EntityTypeDescriptor createDescriptor(MetamodelImplementor metamodel, Class<T> type) {
		EntityPersister entityPersister = metamodel.entityPersister( type );
		IdOrder idOrder = createIdOrder( entityPersister );
		return new EntityTypeDescriptor( type, idOrder );
	}

	private static IdOrder createIdOrder(EntityPersister entityPersister) {
		final String identifierPropertyName = entityPersister.getIdentifierPropertyName();
		final Type identifierType = entityPersister.getIdentifierType();
		if ( identifierType instanceof ComponentType ) {
			final ComponentType componentType = (ComponentType) identifierType;
			return new CompositeIdOrder( identifierPropertyName, componentType );
		}
		else {
			return new SingularIdOrder( identifierPropertyName );
		}
	}

}
