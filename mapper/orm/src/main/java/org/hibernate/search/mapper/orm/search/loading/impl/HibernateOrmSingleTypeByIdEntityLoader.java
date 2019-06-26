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
import org.hibernate.search.mapper.orm.common.EntityReference;

public class HibernateOrmSingleTypeByIdEntityLoader<E> implements HibernateOrmComposableEntityLoader<E> {
	private final Session session;
	private final Class<E> entityType;
	private final MutableEntityLoadingOptions loadingOptions;

	private MultiIdentifierLoadAccess<E> multiAccess;

	public HibernateOrmSingleTypeByIdEntityLoader(
			Session session,
			Class<E> entityType,
			MutableEntityLoadingOptions loadingOptions) {
		this.session = session;
		this.entityType = entityType;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public List<E> loadBlocking(List<EntityReference> references) {
		return loadEntities( references );
	}

	@Override
	public void loadBlocking(List<EntityReference> references, Map<? super EntityReference, ? super E> objectsByReference) {
		List<? extends E> loadedObjects = loadEntities( references );
		Iterator<EntityReference> referencesIterator = references.iterator();
		Iterator<? extends E> loadedObjectIterator = loadedObjects.iterator();
		while ( referencesIterator.hasNext() ) {
			EntityReference reference = referencesIterator.next();
			E loadedObject = loadedObjectIterator.next();
			if ( loadedObject != null ) {
				objectsByReference.put( reference, loadedObject );
			}
		}
	}

	private List<E> loadEntities(List<EntityReference> references) {
		List<Serializable> ids = new ArrayList<>( references.size() );
		for ( EntityReference reference : references ) {
			ids.add( (Serializable) reference.getId() );
		}

		return getMultiAccess().multiLoad( ids );
	}

	private MultiIdentifierLoadAccess<E> getMultiAccess() {
		if ( multiAccess == null ) {
			multiAccess = session.byMultipleIds( entityType );
		}
		multiAccess.withBatchSize( loadingOptions.getFetchSize() );
		return multiAccess;
	}
}
