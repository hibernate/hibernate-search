/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.search.mapper.pojo.search.PojoReference;

class HibernateOrmSingleTypeByIdEntityLoader<O, T> implements HibernateOrmComposableEntityLoader<PojoReference, T> {
	private final Session session;
	private final Class<O> entityType;
	private final MutableEntityLoadingOptions loadingOptions;
	private final Function<? super O, T> hitTransformer;

	private MultiIdentifierLoadAccess<O> multiAccess;

	public HibernateOrmSingleTypeByIdEntityLoader(
			Session session,
			Class<O> entityType,
			MutableEntityLoadingOptions loadingOptions,
			Function<? super O, T> hitTransformer) {
		this.session = session;
		this.entityType = entityType;
		this.loadingOptions = loadingOptions;
		this.hitTransformer = hitTransformer;
	}

	@Override
	public List<T> loadBlocking(List<PojoReference> references) {
		List<O> loadedObjects = loadEntities( references );

		// TODO avoid creating this list when the transformer is the identity; maybe cast the list in that case, or tranform in-place all the time?
		return loadedObjects.stream().map( hitTransformer ).collect( Collectors.toList() );
	}

	@Override
	public void loadBlocking(List<PojoReference> references, Map<? super PojoReference, ? super T> objectsByReference) {
		List<O> loadedObjects = loadEntities( references );
		Iterator<PojoReference> referencesIterator = references.iterator();
		Iterator<O> loadedObjectIterator = loadedObjects.iterator();
		while ( referencesIterator.hasNext() ) {
			PojoReference reference = referencesIterator.next();
			O loadedObject = loadedObjectIterator.next();
			if ( loadedObject != null ) {
				objectsByReference.put( reference, hitTransformer.apply( loadedObject ) );
			}
		}
	}

	private List<O> loadEntities(List<PojoReference> references) {
		List<Serializable> ids = new ArrayList<>( references.size() );
		for ( PojoReference reference : references ) {
			ids.add( (Serializable) reference.getId() );
		}

		return getMultiAccess().multiLoad( ids );
	}

	private MultiIdentifierLoadAccess<O> getMultiAccess() {
		if ( multiAccess == null ) {
			multiAccess = session.byMultipleIds( entityType );
		}
		multiAccess.withBatchSize( loadingOptions.getFetchSize() );
		return multiAccess;
	}
}
