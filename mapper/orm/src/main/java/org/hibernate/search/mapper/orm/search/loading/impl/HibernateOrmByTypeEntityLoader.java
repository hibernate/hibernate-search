/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmByTypeEntityLoader<E, T> implements EntityLoader<EntityReference, T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<? extends E>, HibernateOrmComposableEntityLoader<? extends T>> delegatesByConcreteType;

	HibernateOrmByTypeEntityLoader(Map<Class<? extends E>, HibernateOrmComposableEntityLoader<? extends T>> delegatesByConcreteType) {
		this.delegatesByConcreteType = delegatesByConcreteType;
	}

	@Override
	public List<T> loadBlocking(List<EntityReference> references) {
		LinkedHashMap<EntityReference, T> objectsByReference = new LinkedHashMap<>( references.size() );
		Map<HibernateOrmComposableEntityLoader<? extends T>, List<EntityReference>> referencesByDelegate = new HashMap<>();

		// Split references by delegate (by entity type)
		// Note that multiple entity types may share the same loader
		for ( EntityReference reference : references ) {
			objectsByReference.put( reference, null );
			HibernateOrmComposableEntityLoader<? extends T> delegate = getDelegate( reference.getType() );
			referencesByDelegate.computeIfAbsent( delegate, ignored -> new ArrayList<>() )
					.add( reference );
		}

		// Load all references
		for ( Map.Entry<HibernateOrmComposableEntityLoader<? extends T>, List<EntityReference>> entry :
				referencesByDelegate.entrySet() ) {
			HibernateOrmComposableEntityLoader<? extends T> delegate = entry.getKey();
			List<EntityReference> referencesForDelegate = entry.getValue();
			delegate.loadBlocking( referencesForDelegate, objectsByReference );
		}

		// Re-create the list of objects in the same order
		List<T> result = new ArrayList<>( references.size() );
		for ( EntityReference reference : references ) {
			result.add( objectsByReference.get( reference ) );
		}
		return result;
	}

	private HibernateOrmComposableEntityLoader<? extends T> getDelegate(Class<?> entityType) {
		HibernateOrmComposableEntityLoader<? extends T> delegate = delegatesByConcreteType.get( entityType );
		if ( delegate == null ) {
			throw log.unexpectedSearchHitType( entityType, delegatesByConcreteType.keySet() );
		}
		return delegate;
	}
}
