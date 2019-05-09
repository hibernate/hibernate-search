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
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class EntityLoaderBuilder<O> {

	private final Session session;
	private final Set<Class<? extends O>> concreteIndexedClasses;

	public EntityLoaderBuilder(Session session, Set<Class<? extends O>> concreteIndexedClasses) {
		this.session = session;
		this.concreteIndexedClasses = concreteIndexedClasses;
	}

	public EntityLoader<PojoReference, O> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedClasses.size() == 1 ) {
			Class<? extends O> concreteIndexedType = concreteIndexedClasses.iterator().next();
			return buildForSingleType( mutableLoadingOptions, concreteIndexedType );
		}
		else {
			return buildForMultipleTypes( mutableLoadingOptions );
		}
	}

	private HibernateOrmComposableEntityLoader<PojoReference, O> buildForSingleType(
			MutableEntityLoadingOptions mutableLoadingOptions, Class<? extends O> concreteIndexedType) {
		// TODO Add support for entities whose document ID is not the entity ID (natural ID, or other)
		// TODO Add support for other types of database retrieval and object lookup? See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.EntityLoaderBuilder#getObjectInitializer
		return new HibernateOrmSingleTypeByIdEntityLoader<>( session, concreteIndexedType, mutableLoadingOptions );
	}

	private EntityLoader<PojoReference, O> buildForMultipleTypes(MutableEntityLoadingOptions mutableLoadingOptions) {
		/*
		 * TODO Group together entity types from a same hierarchy, so as to optimize loads
		 * (one query per entity hierarchy, and not one query per index).
		 */
		Map<Class<? extends O>, HibernateOrmComposableEntityLoader<PojoReference, ? extends O>> delegateByConcreteType =
				new HashMap<>( concreteIndexedClasses.size() );
		for ( Class<? extends O> concreteIndexedClass : concreteIndexedClasses ) {
			HibernateOrmComposableEntityLoader<PojoReference, O> delegate =
					buildForSingleType( mutableLoadingOptions, concreteIndexedClass );
			delegateByConcreteType.put( concreteIndexedClass, delegate );
		}
		return new HibernateOrmByTypeEntityLoader<>( delegateByConcreteType );
	}

}
