/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.ParameterizedBeanReference;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

final class HibernateOrmEntityTypeMetadataContributor implements PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;
	private final PersistentClass persistentClass;
	private final Optional<String> identifierPropertyNameOptional;

	HibernateOrmEntityTypeMetadataContributor(PojoRawTypeModel<?> typeModel,
			PersistentClass persistentClass, Optional<String> identifierPropertyNameOptional) {
		this.typeModel = typeModel;
		this.persistentClass = persistentClass;
		this.identifierPropertyNameOptional = identifierPropertyNameOptional;
	}

	@Override
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		if ( !typeModel.typeIdentifier().equals( collector.typeIdentifier() ) ) {
			// Entity metadata is not inherited; only contribute it to the exact type.
			return;
		}
		var node = collector.markAsEntity();

		// There are two names for each entity type: the JPA name and the Hibernate ORM name.
		// They are often different:
		//  - the Hibernate ORM name is the fully-qualified class name for class entities,
		//    or the name defined in the hbm.xml for dynamic-map entities.
		//  - by default, the JPA name is the unqualified class name by default for class entities,
		//    or the name defined in the hbm.xml for dynamic-map entities.
		//    It can be overridden with @Entity(name = ...) for class entities.
		//
		// In theory, there could be conflicts where a given name points to one entity for JPA
		// and another entity for ORM. However that would require a very strange mapping:
		// one would need to set the JPA name of one entity to the fully qualified name of another entity class.
		//
		// In Hibernate Search APIs, we accept that conflicts can arise:
		// JPA entity names will always work,
		// and when there is no naming conflict (99% of the time) Hibernate ORM entity names will work too.
		//
		// We still keep around the map by Hibernate ORM entity name because Search sometimes needs to use
		// the Hibernate ORM name internally when using Hibernate ORM features (Session.load, ...).
		node.entityName( persistentClass.getJpaEntityName() );
		node.secondaryEntityName( persistentClass.getEntityName() );

		node.pathDefinitionProvider( new HibernateOrmPathDefinitionProvider( typeModel, persistentClass ) );
		node.entityIdPropertyName( identifierPropertyNameOptional.orElse( null ) );
		node.loadingBinder( ParameterizedBeanReference.of(
				BeanReference.ofInstance( new HibernateOrmEntityLoadingBinder() ),
				Map.of() ) );
	}
}
