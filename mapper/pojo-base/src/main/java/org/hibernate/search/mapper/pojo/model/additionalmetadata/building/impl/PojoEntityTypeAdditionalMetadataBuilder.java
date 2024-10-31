/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.spi.ParameterizedBeanReference;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorEntityTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.SimplePojoPathsDefinitionProvider;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class PojoEntityTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorEntityTypeNode {

	private String entityName;
	private String secondaryEntityName;
	private PojoPathDefinitionProvider pathDefinitionProvider = SimplePojoPathsDefinitionProvider.INSTANCE;
	private String entityIdPropertyName;
	private ParameterizedBeanReference<?> loadingBinderRef;

	PojoEntityTypeAdditionalMetadataBuilder() {
	}

	@Override
	public void entityName(String entityName) {
		if ( this.entityName != null && !this.entityName.equals( entityName ) ) {
			throw MappingLog.INSTANCE.multipleEntityNames(
					this.entityName,
					entityName
			);
		}
		this.entityName = entityName;
	}

	@Override
	public void secondaryEntityName(String secondaryEntityName) {
		if ( this.secondaryEntityName != null && !this.secondaryEntityName.equals( secondaryEntityName ) ) {
			throw MappingLog.INSTANCE.multipleSecondaryEntityNames(
					this.secondaryEntityName,
					secondaryEntityName
			);
		}
		this.secondaryEntityName = secondaryEntityName;
	}

	@Override
	public void pathDefinitionProvider(PojoPathDefinitionProvider pathDefinitionProvider) {
		this.pathDefinitionProvider = pathDefinitionProvider;
	}

	@Override
	public void entityIdPropertyName(String propertyName) {
		this.entityIdPropertyName = propertyName;
	}

	@Override
	public void loadingBinder(ParameterizedBeanReference<?> binderRef) {
		this.loadingBinderRef = binderRef;
	}

	public PojoEntityTypeAdditionalMetadata build(PojoRawTypeModel<?> typeModel) {
		return new PojoEntityTypeAdditionalMetadata(
				entityName != null ? entityName : typeModel.typeIdentifier().javaClass().getSimpleName(),
				secondaryEntityName,
				pathDefinitionProvider,
				Optional.ofNullable( entityIdPropertyName ),
				loadingBinderRef
		);
	}
}
