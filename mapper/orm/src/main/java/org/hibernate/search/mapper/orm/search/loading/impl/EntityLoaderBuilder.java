/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class EntityLoaderBuilder<E> {

	private final Session session;
	private final Set<? extends HibernateOrmLoadingIndexedTypeContext<? extends E>> concreteIndexedTypes;

	public EntityLoaderBuilder(Session session,
			Set<? extends HibernateOrmLoadingIndexedTypeContext<? extends E>> concreteIndexedTypes) {
		this.session = session;
		this.concreteIndexedTypes = concreteIndexedTypes;
	}

	public EntityLoader<EntityReference, ? extends E> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedTypes.size() == 1 ) {
			HibernateOrmLoadingIndexedTypeContext<? extends E> typeContext = concreteIndexedTypes.iterator().next();
			return typeContext.createLoader( session, mutableLoadingOptions );
		}
		else {
			return buildForMultipleTypes( mutableLoadingOptions );
		}
	}

	private EntityLoader<EntityReference, E> buildForMultipleTypes(MutableEntityLoadingOptions mutableLoadingOptions) {
		/*
		 * TODO HSEARCH-3349 Group together entity types from a same hierarchy, so as to optimize loads
		 *  (one query per entity hierarchy, and not one query per index).
		 *  WARNING: Only do that if the entity types are loaded in a compatible way
		 *  (all by ID, or all by query on a property defined in the common parent entity type)
		 */
		Map<Class<? extends E>, HibernateOrmComposableEntityLoader<EntityReference, ? extends E>> delegateByConcreteType =
				new HashMap<>( concreteIndexedTypes.size() );
		for ( HibernateOrmLoadingIndexedTypeContext<? extends E> typeContext : concreteIndexedTypes ) {
			HibernateOrmComposableEntityLoader<EntityReference, ? extends E> delegate =
					typeContext.createLoader( session, mutableLoadingOptions );
			delegateByConcreteType.put( typeContext.getJavaClass(), delegate );
		}
		return new HibernateOrmByTypeEntityLoader<>( delegateByConcreteType );
	}

}
