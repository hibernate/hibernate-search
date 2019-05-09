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

import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.search.mapper.pojo.search.PojoReference;

class HibernateOrmSingleTypeByIdEntityLoader<O> implements HibernateOrmComposableEntityLoader<PojoReference, O> {
	private final Session session;
	private final Class<? extends O> entityType;
	private final MutableEntityLoadingOptions loadingOptions;

	private MultiIdentifierLoadAccess<? extends O> multiAccess;

	public HibernateOrmSingleTypeByIdEntityLoader(
			Session session,
			Class<? extends O> entityType,
			MutableEntityLoadingOptions loadingOptions) {
		this.session = session;
		this.entityType = entityType;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public List<? extends O> loadBlocking(List<PojoReference> references) {
		return loadEntities( references );
	}

	@Override
	public void loadBlocking(List<PojoReference> references, Map<? super PojoReference, ? super O> objectsByReference) {
		List<? extends O> loadedObjects = loadEntities( references );
		Iterator<PojoReference> referencesIterator = references.iterator();
		Iterator<? extends O> loadedObjectIterator = loadedObjects.iterator();
		while ( referencesIterator.hasNext() ) {
			PojoReference reference = referencesIterator.next();
			O loadedObject = loadedObjectIterator.next();
			if ( loadedObject != null ) {
				objectsByReference.put( reference, loadedObject );
			}
		}
	}

	private List<? extends O> loadEntities(List<PojoReference> references) {
		List<Serializable> ids = new ArrayList<>( references.size() );
		for ( PojoReference reference : references ) {
			ids.add( (Serializable) reference.getId() );
		}

		return getMultiAccess().multiLoad( ids );
	}

	private MultiIdentifierLoadAccess<? extends O> getMultiAccess() {
		if ( multiAccess == null ) {
			multiAccess = session.byMultipleIds( entityType );
		}
		multiAccess.withBatchSize( loadingOptions.getFetchSize() );
		return multiAccess;
	}
}
