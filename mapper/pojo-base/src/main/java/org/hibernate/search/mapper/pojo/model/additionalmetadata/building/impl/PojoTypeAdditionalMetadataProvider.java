/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

public class PojoTypeAdditionalMetadataProvider {

	private final BeanResolver beanResolver;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider;
	private final Map<PojoRawTypeModel<?>, PojoTypeAdditionalMetadata> cache = new HashMap<>();

	public PojoTypeAdditionalMetadataProvider(BeanResolver beanResolver,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> modelContributorProvider) {
		this.beanResolver = beanResolver;
		this.modelContributorProvider = modelContributorProvider;
	}

	public PojoTypeAdditionalMetadata get(PojoRawTypeModel<?> typeModel) {
		return cache.computeIfAbsent( typeModel, this::createTypeAdditionalMetadata );
	}

	public PojoValueAdditionalMetadata get(BoundPojoModelPathValueNode<?, ?, ?> valueNode) {
		BoundPojoModelPathPropertyNode<?, ?> propertyNode = valueNode.getParent();
		BoundPojoModelPathTypeNode<?> typeNode = propertyNode.getParent();
		PojoTypeModel<?> typeModel = typeNode.getTypeModel();
		return get( typeModel.rawType() )
				.getPropertyAdditionalMetadata( propertyNode.getPropertyModel().name() )
				.getValueAdditionalMetadata( valueNode.getExtractorPath() );
	}

	private PojoTypeAdditionalMetadata createTypeAdditionalMetadata(PojoRawTypeModel<?> typeModel) {
		PojoTypeAdditionalMetadataBuilder builder = new PojoTypeAdditionalMetadataBuilder( beanResolver, typeModel );
		for ( PojoTypeMetadataContributor contributor : modelContributorProvider.get( typeModel ) ) {
			contributor.contributeAdditionalMetadata( builder );
		}
		return builder.build();
	}
}
