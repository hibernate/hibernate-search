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
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.IndexScope;
import org.hibernate.search.util.common.impl.StringHelper;

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
		MappingMetamodel metamodel = sessionFactory.getMappingMetamodel();
		for ( Class<?> type : types ) {
			result.add( createDescriptor( metamodel, type ) );
		}
		return result;
	}

	private static <T> EntityTypeDescriptor createDescriptor(MappingMetamodel metamodel, Class<T> type) {
		EntityMappingType entityMappingType = metamodel.findEntityDescriptor( type );
		IdOrder idOrder = createIdOrder( entityMappingType );
		return new EntityTypeDescriptor( type, idOrder );
	}

	private static IdOrder createIdOrder(EntityMappingType entityMappingType) {
		EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();
		if ( identifierMapping.getPartMappingType() instanceof EmbeddableMappingType ) {
			return new CompositeIdOrder( identifierMapping,
					(EmbeddableMappingType) identifierMapping.getPartMappingType() );
		}
		else {
			return new SingularIdOrder( identifierMapping.getAttributeName() );
		}
	}

}
