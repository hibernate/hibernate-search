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
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmByTypeEntityLoader<O, T> implements EntityLoader<PojoReference, T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<? extends O>, HibernateOrmComposableEntityLoader<PojoReference, ? extends T>> delegatesByConcreteType;

	public HibernateOrmByTypeEntityLoader(Map<Class<? extends O>, HibernateOrmComposableEntityLoader<PojoReference, ? extends T>> delegatesByConcreteType) {
		this.delegatesByConcreteType = delegatesByConcreteType;
	}

	@Override
	public List<T> loadBlocking(List<PojoReference> references) {
		LinkedHashMap<PojoReference, T> objectsByReference = new LinkedHashMap<>( references.size() );
		Map<HibernateOrmComposableEntityLoader<PojoReference, ? extends T>, List<PojoReference>> referencesByDelegate = new HashMap<>();

		// Split references by delegate (by entity type)
		for ( PojoReference reference : references ) {
			objectsByReference.put( reference, null );
			HibernateOrmComposableEntityLoader<PojoReference, ? extends T> delegate = getDelegate( reference.getType() );
			referencesByDelegate.computeIfAbsent( delegate, ignored -> new ArrayList<>() )
					.add( reference );
		}

		// Load all references
		for ( Map.Entry<HibernateOrmComposableEntityLoader<PojoReference, ? extends T>, List<PojoReference>> entry :
				referencesByDelegate.entrySet() ) {
			HibernateOrmComposableEntityLoader<PojoReference, ? extends T> delegate = entry.getKey();
			List<PojoReference> referencesForDelegate = entry.getValue();
			delegate.loadBlocking( referencesForDelegate, objectsByReference );
		}

		// Re-create the list of objects in the same order
		List<T> result = new ArrayList<>( references.size() );

		for ( T value : objectsByReference.values() ) {
			/*
			 * TODO remove null values? We used to do it in Search 5...
			 * Note that if we do, we have to change the javadoc
			 * for this method and also change the other EntityLoader implementations.
			 */
			result.add( value );
		}
		return result;
	}

	private HibernateOrmComposableEntityLoader<PojoReference, ? extends T> getDelegate(Class<?> entityType) {
		HibernateOrmComposableEntityLoader<PojoReference, ? extends T> delegate = delegatesByConcreteType.get( entityType );
		if ( delegate == null ) {
			throw log.unexpectedSearchHitType( entityType, delegatesByConcreteType.keySet() );
		}
		return delegate;
	}
}
