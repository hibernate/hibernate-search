/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.spi.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.spi.IdentifierBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;

/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNodeProcessorBuilder extends AbstractPojoProcessorBuilder
		implements PojoPropertyNodeMappingCollector {

	private final PropertyModel<?> propertyModel;

	private final Collection<PojoTypeNodeProcessorBuilder> indexedEmbeddedProcessorBuilders = new ArrayList<>();

	public PojoPropertyNodeProcessorBuilder(
			PropertyModel<?> propertyModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		super( propertyModel.getTypeModel(), contributorProvider, indexModelBinder, bindingContext,
				identityMappingCollector );
		this.propertyModel = propertyModel;
	}

	@Override
	public void functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder,
			String fieldName, FieldModelContributor fieldModelContributor) {
		String defaultedFieldName = fieldName;
		if ( defaultedFieldName == null ) {
			defaultedFieldName = propertyModel.getName();
		}

		ValueProcessor processor = indexModelBinder.addFunctionBridge(
				bindingContext, indexableModel, propertyModel.getJavaType(), builder, defaultedFieldName, fieldModelContributor );
		processors.add( processor );
	}

	@Override
	public void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		IdentifierBridge<?> bridge = indexModelBinder.createIdentifierBridge( propertyModel.getJavaType(), builder );
		identityMappingCollector.identifierBridge( propertyModel.getHandle(), bridge );
	}

	@Override
	public void containedIn() {
		// FIXME implement ContainedIn
		// FIXME also contribute containedIns to indexedEmbeddeds using the parent's metadata here, if possible?
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, Integer maxDepth, Set<String> pathFilters) {
		// TODO handle collections

		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = propertyModel.getName() + ".";
		}

		PojoIndexedTypeIdentifier typeId = new PojoIndexedTypeIdentifier( propertyModel.getJavaType() );

		Optional<IndexModelBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				typeId, defaultedRelativePrefix, maxDepth, pathFilters );
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			PojoTypeNodeProcessorBuilder nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder(
					propertyModel.getTypeModel(), contributorProvider, indexModelBinder, nestedBindingContext,
					PojoTypeNodeIdentityMappingCollector.noOp() // Do NOT propagate the identity mapping collector to IndexedEmbeddeds
					);
			indexedEmbeddedProcessorBuilders.add( nestedProcessorBuilder );
			contributorProvider.get( typeId ).forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
		} );
	}

	public PojoPropertyNodeProcessor build() {
		return new PojoPropertyNodeProcessor( propertyModel.getHandle(), processors, indexedEmbeddedProcessorBuilders );
	}

}
