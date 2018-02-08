/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.IdentifierLoadAccess;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.search.mapper.pojo.search.PojoReference;

class SingleTypeByIdObjectLoader<O, T> implements ComposableObjectLoader<PojoReference, T> {
	private final Session session;
	private final Class<O> entityType;
	private final MutableObjectLoadingOptions loadingOptions;
	private final Function<? super O, T> hitTransformer;

	private IdentifierLoadAccess<O> singleAccess;
	private MultiIdentifierLoadAccess<O> multiAccess;

	public SingleTypeByIdObjectLoader(
			Session session,
			Class<O> entityType,
			MutableObjectLoadingOptions loadingOptions,
			Function<? super O, T> hitTransformer) {
		this.session = session;
		this.entityType = entityType;
		this.loadingOptions = loadingOptions;
		this.hitTransformer = hitTransformer;
	}

	@Override
	public List<T> load(List<PojoReference> references) {
		List<O> loadedObjects = loadEntities( references );

		// TODO avoid creating this list when the transformer is the identity; maybe cast the list in that case, or tranform in-place all the time?
		return loadedObjects.stream().map( hitTransformer ).collect( Collectors.toList() );
	}

	@Override
	public void load(List<PojoReference> references, Map<? super PojoReference, ? super T> objectsByReference) {
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

	private IdentifierLoadAccess<O> getSingleAccess() {
		if ( singleAccess == null ) {
			singleAccess = session.byId( entityType );
		}
		return singleAccess;
	}

	private MultiIdentifierLoadAccess<O> getMultiAccess() {
		if ( multiAccess == null ) {
			multiAccess = session.byMultipleIds( entityType );
		}
		multiAccess.withBatchSize( loadingOptions.getFetchSize() );
		return multiAccess;
	}
}
