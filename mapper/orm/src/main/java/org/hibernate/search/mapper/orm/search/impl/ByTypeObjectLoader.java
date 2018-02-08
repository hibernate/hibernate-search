/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.util.spi.LoggerFactory;

public class ByTypeObjectLoader<O, T> implements ObjectLoader<PojoReference, T> {

	private static final Log log = LoggerFactory.make( Log.class );

	private final Map<Class<? extends O>, ComposableObjectLoader<PojoReference, ? extends T>> delegatesByConcreteType;

	public ByTypeObjectLoader(Map<Class<? extends O>, ComposableObjectLoader<PojoReference, ? extends T>> delegatesByConcreteType) {
		this.delegatesByConcreteType = delegatesByConcreteType;
	}

	@Override
	public List<T> load(List<PojoReference> references) {
		LinkedHashMap<PojoReference, T> objectsByReference = new LinkedHashMap<>( references.size() );
		Map<ComposableObjectLoader<PojoReference, ? extends T>, List<PojoReference>> referencesByDelegate = new HashMap<>();

		// Split references by delegate (by entity type)
		for ( PojoReference reference : references ) {
			objectsByReference.put( reference, null );
			ComposableObjectLoader<PojoReference, ? extends T> delegate = getDelegate( reference.getType() );
			referencesByDelegate.computeIfAbsent( delegate, ignored -> new ArrayList<>() )
					.add( reference );
		}

		// Load all references
		for ( Map.Entry<ComposableObjectLoader<PojoReference, ? extends T>, List<PojoReference>> entry :
				referencesByDelegate.entrySet() ) {
			ComposableObjectLoader<PojoReference, ? extends T> delegate = entry.getKey();
			List<PojoReference> referencesForDelegate = entry.getValue();
			delegate.load( referencesForDelegate, objectsByReference );
		}

		// Re-create the list of objects in the same order
		List<T> result = new ArrayList<>( references.size() );

		for ( T value : objectsByReference.values() ) {
			/*
			 * TODO remove null values? We used to do it in Search 5...
			 * Note that if we do, we have to change the javadoc
			 * for this method and also change the other ObjectLoader implementations.
			 */
			result.add( value );
		}
		return result;
	}

	private ComposableObjectLoader<PojoReference, ? extends T> getDelegate(Class<?> entityType) {
		ComposableObjectLoader<PojoReference, ? extends T> delegate = delegatesByConcreteType.get( entityType );
		if ( delegate == null ) {
			throw log.unexpectedSearchHitType( entityType, delegatesByConcreteType.keySet() );
		}
		return delegate;
	}
}
