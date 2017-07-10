/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.cfg.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.cfg.spi.IdUniquenessResolver;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * Implementation extracting entity classes from the configuration
 * and release the configuration reference for future Garbage Collection.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class HibernateCoreIdUniquenessResolver implements IdUniquenessResolver {

	private final Set<Class<?>> entities;

	public HibernateCoreIdUniquenessResolver(Metadata metadata) {
		Set<Class<?>> entities = new HashSet<>();
		for ( PersistentClass pc : metadata.getEntityBindings() ) {
			Class<?> mappedClass = pc.getMappedClass();
			if ( mappedClass != null ) {
				entities.add( mappedClass );
			}
		}
		this.entities = Collections.unmodifiableSet( entities );
	}

	@Override
	public boolean areIdsUniqueForClasses(IndexedTypeIdentifier entityInIndex, IndexedTypeIdentifier otherEntityInIndex) {
		/*
		 * Look for the top most superclass of each that is also a mapped entity
		 * That should be the root entity for that given class.
		 */
		Class<?> rootOfEntityInIndex = getRootEntity( entityInIndex );
		Class<?> rootOfOtherEntityInIndex = getRootEntity( otherEntityInIndex );
		return rootOfEntityInIndex == rootOfOtherEntityInIndex;
	}

	private Class<?> getRootEntity(IndexedTypeIdentifier indexedType) {
		Class entityInIndex = indexedType.getPojoType();
		if ( ! entities.contains( entityInIndex ) ) {
			// the entity is not a mapped entity ?
			// should not happen so we return the entity class itself
			return entityInIndex;
		}
		Class<?> potentialParent = entityInIndex;
		do {
			potentialParent = potentialParent.getSuperclass();
			if ( potentialParent != null && potentialParent != Object.class && entities.contains( potentialParent ) ) {
				entityInIndex = potentialParent;
			}
		}
		while ( potentialParent != null && potentialParent != Object.class );
		return entityInIndex;
	}
}
