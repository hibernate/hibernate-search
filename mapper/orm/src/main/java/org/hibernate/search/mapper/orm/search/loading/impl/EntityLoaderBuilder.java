/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Session;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class EntityLoaderBuilder<E> {

	private final Session session;
	private final Map<Class<? extends E>, PojoMappingTypeMetadata> concreteIndexedClassesToMetadata;

	public EntityLoaderBuilder(Session session,
			Map<Class<? extends E>, PojoMappingTypeMetadata> concreteIndexedClassesToMetadata) {
		this.session = session;
		this.concreteIndexedClassesToMetadata = concreteIndexedClassesToMetadata;
	}

	public EntityLoader<PojoReference, ? extends E> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedClassesToMetadata.size() == 1 ) {
			Map.Entry<Class<? extends E>, PojoMappingTypeMetadata> entry =
					concreteIndexedClassesToMetadata.entrySet().iterator().next();
			return buildForSingleType( mutableLoadingOptions, entry.getKey(), entry.getValue() );
		}
		else {
			return buildForMultipleTypes( mutableLoadingOptions );
		}
	}

	private <E2 extends E> HibernateOrmComposableEntityLoader<PojoReference, E2> buildForSingleType(
			MutableEntityLoadingOptions mutableLoadingOptions,
			Class<E2> concreteIndexedType, PojoMappingTypeMetadata metadata) {
		// TODO HSEARCH-3349 Add support for other types of database retrieval and object lookup?
		//  See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.EntityLoaderBuilder#getObjectInitializer

		if ( metadata.isDocumentIdMappedToEntityId() ) {
			return new HibernateOrmSingleTypeByIdEntityLoader<>(
					session,
					concreteIndexedType,
					mutableLoadingOptions
			);
		}
		else {
			IdentifiableType<E2> indexTypeModel = session.getSessionFactory().getMetamodel().entity( concreteIndexedType );
			SingularAttribute<? super E2, ?> documentIdSourceProperty =
					indexTypeModel.getSingularAttribute( metadata.getDocumentIdSourcePropertyName().get() );
			return new HibernateOrmSingleTypeCriteriaEntityLoader<>(
					session,
					concreteIndexedType,
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
				new HashMap<>( concreteIndexedClassesToMetadata.size() );
		for ( Map.Entry<Class<? extends E>, PojoMappingTypeMetadata> entry :
				concreteIndexedClassesToMetadata.entrySet() ) {
			HibernateOrmComposableEntityLoader<PojoReference, ? extends E> delegate =
					buildForSingleType( mutableLoadingOptions, entry.getKey(), entry.getValue() );
			delegateByConcreteType.put( entry.getKey(), delegate );
		}
		return new HibernateOrmByTypeEntityLoader<>( delegateByConcreteType );
	}

}
