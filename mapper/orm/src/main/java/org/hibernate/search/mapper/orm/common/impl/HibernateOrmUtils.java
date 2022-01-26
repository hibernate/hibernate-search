/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class HibernateOrmUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private HibernateOrmUtils() {
	}

	public static SessionFactoryImplementor toSessionFactoryImplementor(EntityManagerFactory entityManagerFactory) {
		try {
			return entityManagerFactory.unwrap( SessionFactoryImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionFactoryAccessError( e.getMessage(), e );
		}
	}

	public static Session toSession(EntityManager entityManager) {
		try {
			return entityManager.unwrap( Session.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e.getMessage(), e );
		}
	}

	public static SessionImplementor toSessionImplementor(EntityManager entityManager) {
		try {
			return entityManager.unwrap( SessionImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e.getMessage(), e );
		}
	}

	private static boolean isSuperTypeOf(EntityPersister type1, EntityPersister type2) {
		return type1.isSubclassEntityName( type2.getEntityName() );
	}

	public static EntityPersister toRootEntityType(SessionFactoryImplementor sessionFactory,
			EntityPersister entityType) {
		/*
		 * We need to rely on Hibernate ORM's SPIs: this is complex stuff.
		 * For example there may be class hierarchies such as A > B > C
		 * where A and C are entity types and B is a mapped superclass.
		 * So we need to exclude non-entity types, and for that we need the Hibernate ORM metamodel.
		 */
		MappingMetamodel metamodel = sessionFactory.getMappingMetamodel();
		String rootEntityName = entityType.getRootEntityName();
		return metamodel.getEntityDescriptor( rootEntityName );
	}

	public static EntityPersister toMostSpecificCommonEntitySuperType(MappingMetamodel metamodel,
			EntityPersister type1, EntityPersister type2) {
		/*
		 * We need to rely on Hibernate ORM's SPIs: this is complex stuff.
		 * For example there may be class hierarchies such as A > B > C
		 * where A and C are entity types and B is a mapped superclass.
		 * So even if we know the two types have a common superclass,
		 * we need to skip non-entity superclasses, and for that we need the Hibernate ORM metamodel.
		 */
		EntityPersister superTypeCandidate = type1;
		while ( superTypeCandidate != null && !isSuperTypeOf( superTypeCandidate, type2 ) ) {
			String superSuperTypeEntityName = superTypeCandidate.getEntityMetamodel().getSuperclass();
			superTypeCandidate = superSuperTypeEntityName == null ? null
					: metamodel.getEntityDescriptor( superSuperTypeEntityName ).getEntityPersister();
		}
		if ( superTypeCandidate == null ) {
			throw new AssertionFailure(
					"Cannot find a common entity supertype for " + type1.getEntityName()
							+ " and " + type2.getEntityName() + "."
							+ " There is a bug in Hibernate Search, please report it."
			);
		}
		return superTypeCandidate;
	}

	public static boolean targetsAllConcreteSubTypes(SessionFactoryImplementor sessionFactory,
			EntityPersister parentType, Collection<?> targetConcreteSubTypes) {
		@SuppressWarnings("unchecked")
		Set<String> subClassEntityNames = parentType.getEntityMetamodel().getSubclassEntityNames();
		// Quick check to return true immediately if all subtypes are concrete
		if ( subClassEntityNames.size() == targetConcreteSubTypes.size() ) {
			return true;
		}

		MappingMetamodel metamodel = sessionFactory.getMappingMetamodel();
		int concreteSubTypesCount = 0;
		for ( String subClassEntityName : subClassEntityNames ) {
			if ( !metamodel.getEntityDescriptor( subClassEntityName ).getEntityMetamodel().isAbstract() ) {
				++concreteSubTypesCount;
			}
		}
		return concreteSubTypesCount == targetConcreteSubTypes.size();
	}

	@SuppressForbiddenApis(reason = "Safer wrapper")
	public static <T extends Service> T getServiceOrFail(ServiceRegistry serviceRegistry,
			Class<T> serviceClass) {
		T service = serviceRegistry.getService( serviceClass );
		if ( service == null ) {
			throw new org.hibernate.search.util.common.AssertionFailure( "A required service was missing. Missing service: " + serviceClass );
		}
		return service;
	}

	@SuppressForbiddenApis(reason = "Safer wrapper")
	public static <T extends Service> Optional<T> getServiceOrEmpty(ServiceRegistry serviceRegistry,
			Class<T> serviceClass) {
		/*
		 * First check the service binding, because if it does not exist,
		 * a call to serviceRegistry.getService would throw an exception.
 		 */
		ServiceBinding<T> binding = ( (ServiceRegistryImplementor) serviceRegistry )
				.locateServiceBinding( serviceClass );
		if ( binding == null ) {
			// The service binding does not exist, so the service does not exist
			return Optional.empty();
		}
		else {
			// The service binding exists, so the service may exist
			// Retrieve it from the service registry, not from the binding, to be sure it's initialized
			// Note the service may be null, even if the binding is defined
			return Optional.ofNullable( serviceRegistry.getService( serviceClass ) );
		}
	}

	public static List<Property> sortedNonSyntheticProperties(Iterator<Property> propertyIterator) {
		List<Property> properties = new ArrayList<>();
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			if ( property.isSynthetic() ) {
				continue;
			}
			properties.add( property );
		}
		properties.sort( PropertyComparator.INSTANCE );
		return properties;
	}
}
