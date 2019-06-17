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

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Session;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class EntityLoaderBuilder<E> {

	private final Session session;
	private final Set<? extends PojoScopeTypeContext<? extends E>> concreteIndexedTypes;

	public EntityLoaderBuilder(Session session,
			Set<? extends PojoScopeTypeContext<? extends E>> concreteIndexedTypes) {
		this.session = session;
		this.concreteIndexedTypes = concreteIndexedTypes;
	}

	public EntityLoader<PojoReference, ? extends E> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedTypes.size() == 1 ) {
			PojoScopeTypeContext<? extends E> typeContext = concreteIndexedTypes.iterator().next();
			return buildForSingleType( mutableLoadingOptions, typeContext );
		}
		else {
			return buildForMultipleTypes( mutableLoadingOptions );
		}
	}

	private <E2 extends E> HibernateOrmComposableEntityLoader<PojoReference, E2> buildForSingleType(
			MutableEntityLoadingOptions mutableLoadingOptions,
			PojoScopeTypeContext<E2> typeContext) {
		// TODO HSEARCH-3349 Add support for other types of database retrieval and object lookup?
		//  See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.EntityLoaderBuilder#getObjectInitializer

		Class<E2> javaClass = typeContext.getJavaClass();
		PojoMappingTypeMetadata metadata = typeContext.getMappingMetadata();
		if ( metadata.isDocumentIdMappedToEntityId() ) {
			return new HibernateOrmSingleTypeByIdEntityLoader<>(
					session,
					javaClass,
					mutableLoadingOptions
			);
		}
		else {
			IdentifiableType<E2> indexTypeModel = session.getSessionFactory().getMetamodel().entity( javaClass );
			SingularAttribute<? super E2, ?> documentIdSourceProperty =
					indexTypeModel.getSingularAttribute( metadata.getDocumentIdSourcePropertyName().get() );
			return new HibernateOrmSingleTypeCriteriaEntityLoader<>(
					session,
					javaClass,
					documentIdSourceProperty,
					mutableLoadingOptions
			);
		}

		// TODO HSEARCH-3203 Add support for entities whose document ID is not the entity ID (natural ID, or other)
	}

	private EntityLoader<PojoReference, E> buildForMultipleTypes(MutableEntityLoadingOptions mutableLoadingOptions) {
		/*
		 * TODO HSEARCH-3349 Group together entity types from a same hierarchy, so as to optimize loads
		 *  (one query per entity hierarchy, and not one query per index).
		 *  WARNING: Only do that if the entity types are loaded in a compatible way
		 *  (all by ID, or all by query on a property defined in the common parent entity type)
		 */
		Map<Class<? extends E>, HibernateOrmComposableEntityLoader<PojoReference, ? extends E>> delegateByConcreteType =
				new HashMap<>( concreteIndexedTypes.size() );
		for ( PojoScopeTypeContext<? extends E> typeContext : concreteIndexedTypes ) {
			HibernateOrmComposableEntityLoader<PojoReference, ? extends E> delegate =
					buildForSingleType( mutableLoadingOptions, typeContext );
			delegateByConcreteType.put( typeContext.getJavaClass(), delegate );
		}
		return new HibernateOrmByTypeEntityLoader<>( delegateByConcreteType );
	}

}
