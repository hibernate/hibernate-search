/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class PojoTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorTypeNode {

	private final BeanResolver beanResolver;
	private final PojoRawTypeModel<?> rawTypeModel;

	private PojoEntityTypeAdditionalMetadataBuilder entityTypeMetadataBuilder;
	private PojoIndexedTypeAdditionalMetadataBuilder indexedTypeMetadataBuilder;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, List<Consumer<PojoAdditionalMetadataCollectorPropertyNode>>> propertyContributors =
			new LinkedHashMap<>();

	PojoTypeAdditionalMetadataBuilder(BeanResolver beanResolver, PojoRawTypeModel<?> rawTypeModel) {
		this.beanResolver = beanResolver;
		this.rawTypeModel = rawTypeModel;
	}

	@Override
	public PojoRawTypeIdentifier<?> typeIdentifier() {
		return rawTypeModel.typeIdentifier();
	}

	@Override
	public PojoEntityTypeAdditionalMetadataBuilder markAsEntity() {
		if ( entityTypeMetadataBuilder == null ) {
			entityTypeMetadataBuilder = new PojoEntityTypeAdditionalMetadataBuilder();
		}
		return entityTypeMetadataBuilder;
	}

	@Override
	public PojoIndexedTypeAdditionalMetadataBuilder markAsIndexed() {
		if ( indexedTypeMetadataBuilder == null ) {
			indexedTypeMetadataBuilder = new PojoIndexedTypeAdditionalMetadataBuilder();
		}
		return indexedTypeMetadataBuilder;
	}

	@Override
	public void property(String propertyName,
			Consumer<PojoAdditionalMetadataCollectorPropertyNode> propertyMetadataContributor) {
		propertyContributors.computeIfAbsent( propertyName, ignored -> new ArrayList<>() )
				.add( propertyMetadataContributor );
	}

	public PojoTypeAdditionalMetadata build() {
		Map<String, Supplier<PojoPropertyAdditionalMetadata>> propertiesAdditionalMetadataSuppliers = new HashMap<>();
		for ( Map.Entry<String, List<Consumer<PojoAdditionalMetadataCollectorPropertyNode>>> entry : propertyContributors
				.entrySet() ) {
			String propertyName = entry.getKey();
			List<Consumer<PojoAdditionalMetadataCollectorPropertyNode>> contributors = entry.getValue();
			propertiesAdditionalMetadataSuppliers.put( propertyName, () -> {
				PojoPropertyAdditionalMetadataBuilder builder =
						new PojoPropertyAdditionalMetadataBuilder( beanResolver );
				for ( Consumer<PojoAdditionalMetadataCollectorPropertyNode> contributor : contributors ) {
					contributor.accept( builder );
				}
				return builder.build();
			} );
		}
		return new PojoTypeAdditionalMetadata(
				entityTypeMetadataBuilder == null
						? Optional.empty()
						: Optional.of( entityTypeMetadataBuilder.build( rawTypeModel ) ),
				indexedTypeMetadataBuilder == null ? Optional.empty() : indexedTypeMetadataBuilder.build(),
				propertiesAdditionalMetadataSuppliers
		);
	}
}
