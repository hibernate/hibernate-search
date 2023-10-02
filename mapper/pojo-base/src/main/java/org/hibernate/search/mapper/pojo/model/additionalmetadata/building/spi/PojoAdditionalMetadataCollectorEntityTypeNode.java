/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import org.hibernate.search.engine.environment.bean.spi.ParameterizedBeanReference;
import org.hibernate.search.mapper.pojo.loading.definition.spi.PojoEntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

public interface PojoAdditionalMetadataCollectorEntityTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @param entityName The name of this entity type.
	 */
	void entityName(String entityName);

	/**
	 * @param secondaryEntityName A secondary name for this entity type,
	 * for instance the "native" Hibernate ORM entity name (generally just the fully qualified class name).
	 * Secondary names may conflict with the primary {@link #entityName(String)}.
	 * The primary name takes precedence in case of lookup by name.
	 */
	void secondaryEntityName(String secondaryEntityName);

	/**
	 * @param pathDefinitionProvider A provider of path definition for this entity type,
	 * i.e. the object supporting the creation of path filters that will be used in particular
	 * when performing dirty checking during automatic reindexing.
	 */
	void pathDefinitionProvider(PojoPathDefinitionProvider pathDefinitionProvider);

	/**
	 * @param propertyName The name of a property hosting the entity ID.
	 * This ID will be used by default to generate document IDs when no document ID was configured in the mapping.
	 */
	void entityIdPropertyName(String propertyName);

	/**
	 * @param binderRef A reference to a binder for loading of entities of this type.
	 * @see org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector#applyLoadingBinder(Object, PojoEntityLoadingBindingContext)
	 */
	void loadingBinder(ParameterizedBeanReference<?> binderRef);

}
