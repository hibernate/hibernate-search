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
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class EntityLoaderBuilder<E> {

	private final Session session;
	private final Set<Class<? extends E>> concreteIndexedClasses;

	public EntityLoaderBuilder(Session session, Set<Class<? extends E>> concreteIndexedClasses) {
		this.session = session;
		this.concreteIndexedClasses = concreteIndexedClasses;
	}

	public EntityLoader<PojoReference, ? extends E> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedClasses.size() == 1 ) {
			Class<? extends E> concreteIndexedType = concreteIndexedClasses.iterator().next();
			return buildForSingleType( mutableLoadingOptions, concreteIndexedType );
		}
		else {
			return buildForMultipleTypes( mutableLoadingOptions );
		}
	}

	private HibernateOrmComposableEntityLoader<PojoReference, ? extends E> buildForSingleType(
			MutableEntityLoadingOptions mutableLoadingOptions, Class<? extends E> concreteIndexedType) {
		// TODO HSEARCH-3203 Add support for entities whose document ID is not the entity ID (natural ID, or other)
		// TODO HSEARCH-3349 Add support for other types of database retrieval and object lookup? See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.EntityLoaderBuilder#getObjectInitializer
		return new HibernateOrmSingleTypeByIdEntityLoader<>( session, concreteIndexedType, mutableLoadingOptions );
	}

	private EntityLoader<PojoReference, E> buildForMultipleTypes(MutableEntityLoadingOptions mutableLoadingOptions) {
		/*
		 * TODO HSEARCH-3349 Group together entity types from a same hierarchy, so as to optimize loads
		 * (one query per entity hierarchy, and not one query per index).
		 */
		Map<Class<? extends E>, HibernateOrmComposableEntityLoader<PojoReference, ? extends E>> delegateByConcreteType =
				new HashMap<>( concreteIndexedClasses.size() );
		for ( Class<? extends E> concreteIndexedClass : concreteIndexedClasses ) {
			HibernateOrmComposableEntityLoader<PojoReference, ? extends E> delegate =
					buildForSingleType( mutableLoadingOptions, concreteIndexedClass );
			delegateByConcreteType.put( concreteIndexedClass, delegate );
		}
		return new HibernateOrmByTypeEntityLoader<>( delegateByConcreteType );
	}

}
