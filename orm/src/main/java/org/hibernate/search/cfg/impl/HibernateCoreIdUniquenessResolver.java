/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.cfg.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.cfg.spi.IdUniquenessResolver;

/**
 * Implementation extracting entity classes from the configuration
 * and release the configuration reference for future Garbage Collection.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class HibernateCoreIdUniquenessResolver implements IdUniquenessResolver {
	private final Set<Class<?>> entities;

	public HibernateCoreIdUniquenessResolver(Configuration cfg) {
		Set<Class<?>> entities = new HashSet<>();
		Iterator<PersistentClass> iterator = cfg.getClassMappings();
		while ( iterator.hasNext() ) {
			entities.add( iterator.next().getMappedClass() );
		}
		this.entities = Collections.unmodifiableSet( entities );
	}

	@Override
	public boolean areIdsUniqueForClasses(Class<?> entityInIndex, Class<?> otherEntityInIndex) {
		/*
		 * Look for the top most superclass of each that is also a mapped entity
		 * That should be the root entity for that given class.
		 */
		Class<?> rootOfEntityInIndex = getRootEntity( entityInIndex );
		Class<?> rootOfOtherEntityInIndex = getRootEntity( otherEntityInIndex );
		return rootOfEntityInIndex == rootOfOtherEntityInIndex;
	}

	private Class<?> getRootEntity(Class<?> entityInIndex) {
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
