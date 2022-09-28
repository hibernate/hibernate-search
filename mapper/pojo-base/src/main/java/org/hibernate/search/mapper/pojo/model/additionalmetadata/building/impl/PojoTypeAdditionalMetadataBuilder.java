/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.mapper.pojo.bridge.binding.impl.MarkerBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.PojoPathsDefinitionAdapter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class PojoTypeAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorTypeNode {

	private final BeanResolver beanResolver;
	private final PojoRawTypeModel<?> rawTypeModel;

	private PojoEntityTypeAdditionalMetadataBuilder entityTypeMetadataBuilder;
	private PojoIndexedTypeAdditionalMetadataBuilder indexedTypeMetadataBuilder;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, List<Consumer<PojoAdditionalMetadataCollectorPropertyNode>>> propertyContributors = new LinkedHashMap<>();

	PojoTypeAdditionalMetadataBuilder(BeanResolver beanResolver, PojoRawTypeModel<?> rawTypeModel) {
		this.beanResolver = beanResolver;
		this.rawTypeModel = rawTypeModel;
	}

	@Override
	public PojoRawTypeIdentifier<?> typeIdentifier() {
		return rawTypeModel.typeIdentifier();
	}


	@Override
	@SuppressWarnings("deprecation")
	public PojoEntityTypeAdditionalMetadataBuilder markAsEntity(String entityName,
			org.hibernate.search.mapper.pojo.model.path.spi.PojoPathsDefinition pathsDefinition) {
		return markAsEntity( entityName, new PojoPathsDefinitionAdapter( pathsDefinition ) );
	}

	@Override
	public PojoEntityTypeAdditionalMetadataBuilder markAsEntity(String entityName,
			PojoPathDefinitionProvider pathDefinitionProvider) {
		if ( entityTypeMetadataBuilder == null ) {
			entityTypeMetadataBuilder = new PojoEntityTypeAdditionalMetadataBuilder(
					entityName, pathDefinitionProvider );
		}
		else {
			entityTypeMetadataBuilder.checkSameEntity( entityName );
		}
		return entityTypeMetadataBuilder;
	}

	@Override
	public PojoIndexedTypeAdditionalMetadataBuilder markAsIndexed(boolean enabled) {
		if ( indexedTypeMetadataBuilder == null ) {
			indexedTypeMetadataBuilder = new PojoIndexedTypeAdditionalMetadataBuilder();
		}
		indexedTypeMetadataBuilder.enabled( enabled );
		return indexedTypeMetadataBuilder;
	}

	@Override
	public void property(String propertyName,
			Consumer<PojoAdditionalMetadataCollectorPropertyNode> propertyMetadataContributor) {
		propertyContributors.computeIfAbsent( propertyName, ignored -> new ArrayList<>() )
				.add( propertyMetadataContributor );
	}

	Object bindMarker(MarkerBinder binder, Map<String, Object> params) {
		MarkerBindingContextImpl bindingContext = new MarkerBindingContextImpl( beanResolver, params );
		return bindingContext.applyBinder( binder );
	}

	public PojoTypeAdditionalMetadata build() {
		Map<String, Supplier<PojoPropertyAdditionalMetadata>> propertiesAdditionalMetadataSuppliers = new HashMap<>();
		for ( Map.Entry<String, List<Consumer<PojoAdditionalMetadataCollectorPropertyNode>>> entry :
				propertyContributors.entrySet() ) {
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
				entityTypeMetadataBuilder == null ? Optional.empty() : Optional.of( entityTypeMetadataBuilder.build() ),
				indexedTypeMetadataBuilder == null ? Optional.empty() : indexedTypeMetadataBuilder.build(),
				propertiesAdditionalMetadataSuppliers
		);
	}
}
