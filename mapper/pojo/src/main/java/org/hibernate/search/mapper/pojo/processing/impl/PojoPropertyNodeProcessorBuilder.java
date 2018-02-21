/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.impl.CollectionValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.IterableValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.MapValueValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoContainerTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNodeProcessorBuilder<P, T> extends AbstractPojoNodeProcessorBuilder<P>
		implements PojoPropertyNodeMappingCollector {

	private final PojoTypeModel<P> parentTypeModel;
	private final PojoPropertyModel<T> propertyModel;
	private final PropertyHandle propertyHandle;
	private final PojoTypeModel<T> propertyTypeModel;
	private final PojoModelPropertyRootElement pojoModelRootElement;

	private final PojoTypeNodeIdentityMappingCollector identityMappingCollector;

	private final Collection<PropertyBridge> propertyBridges = new ArrayList<>();
	private final Collection<FunctionBridgeProcessor<? super T, ?>> functionBridgeProcessors = new ArrayList<>();
	private final Collection<AbstractPojoNodeProcessorBuilder<? super T>> nestedProcessorBuilders = new ArrayList<>();
	// Note: if this value is set, it is always also added to nestedProcessorBuilders
	private PojoContainerNodeProcessorBuilder<? super T, ?> containerProcessorBuilder = null;

	PojoPropertyNodeProcessorBuilder(
			PojoTypeNodeProcessorBuilder<P> parent, PojoTypeModel<P> parentTypeModel,
			PojoPropertyModel<T> propertyModel, PropertyHandle propertyHandle,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		super( parent, contributorProvider, indexModelBinder, bindingContext );
		this.parentTypeModel = parentTypeModel;
		this.propertyHandle = propertyHandle;
		this.propertyModel = propertyModel;
		this.propertyTypeModel = propertyModel.getTypeModel();

		// FIXME do something more with the pojoModelRootElement, to be able to use it in containedIn processing in particular
		this.pojoModelRootElement = new PojoModelPropertyRootElement( propertyModel, contributorProvider );

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	public void bridge(BridgeBuilder<? extends PropertyBridge> builder) {
		propertyBridges.add( indexModelBinder.addPropertyBridge( bindingContext, pojoModelRootElement, builder ) );
	}

	@Override
	public void functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder,
			String fieldName, FieldModelContributor fieldModelContributor) {
		String defaultedFieldName = fieldName;
		if ( defaultedFieldName == null ) {
			defaultedFieldName = propertyHandle.getName();
		}

		PojoContainerNodeProcessorBuilder<? super T, ?> containerProcessorBuilder = getContainerProcessorBuilder();
		if ( containerProcessorBuilder != null ) {
			containerProcessorBuilder.functionBridge( builder, defaultedFieldName, fieldModelContributor );
		}
		else {
			FunctionBridgeProcessor<? super T, ?> processor = indexModelBinder.addFunctionBridge(
					bindingContext, propertyTypeModel, builder, defaultedFieldName,
					fieldModelContributor
			);
			functionBridgeProcessors.add( processor );
		}
	}

	@Override
	@SuppressWarnings( {"rawtypes", "unchecked"} )
	public void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		IdentifierBridge<T> bridge = indexModelBinder.createIdentifierBridge( pojoModelRootElement, propertyTypeModel, builder );
		identityMappingCollector.identifierBridge( propertyTypeModel, propertyHandle, bridge );
	}

	@Override
	public void containedIn() {
		// FIXME implement ContainedIn
		// FIXME also contribute containedIns to indexedEmbeddeds using the parent's metadata here, if possible?
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, ObjectFieldStorage storage,
			Integer maxDepth, Set<String> includePaths) {
		// TODO handle collections

		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = propertyHandle.getName() + ".";
		}

		Optional<IndexModelBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				parentTypeModel, defaultedRelativePrefix, storage, maxDepth, includePaths );
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			PojoContainerNodeProcessorBuilder<? super T, ?> containerProcessorBuilder = getContainerProcessorBuilder();

			if ( containerProcessorBuilder != null ) {
				containerProcessorBuilder.indexedEmbedded( nestedBindingContext );
			}
			else {
				PojoTypeNodeProcessorBuilder<T> nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder<>(
						this, propertyTypeModel, contributorProvider, indexModelBinder, nestedBindingContext,
						PojoTypeNodeIdentityMappingCollector.noOp() // Do NOT propagate the identity mapping collector to IndexedEmbeddeds
				);
				nestedProcessorBuilders.add( nestedProcessorBuilder );
				contributorProvider.get( propertyTypeModel ).forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
			}
		} );
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyHandle.getName() );
	}

	@SuppressWarnings("unchecked") // Checks are implemented using reflection
	private PojoContainerNodeProcessorBuilder<? super T, ?> getContainerProcessorBuilder() {
		if ( containerProcessorBuilder == null ) {
			Optional<PojoContainerTypeModel<?>> containerTypeModelOptional = propertyModel.getContainerTypeModel();
			if ( containerTypeModelOptional.isPresent() ) {
				PojoContainerTypeModel<?> containerTypeModel = containerTypeModelOptional.get();
				if ( containerTypeModel.isSubTypeOf( Map.class ) ) {
					containerProcessorBuilder = new PojoContainerNodeProcessorBuilder(
							this, containerTypeModel, MapValueValueExtractor.get(),
							contributorProvider, indexModelBinder, bindingContext
					);
					nestedProcessorBuilders.add( containerProcessorBuilder );
				}
				else if ( containerTypeModel.isSubTypeOf( Collection.class ) ) {
					containerProcessorBuilder = new PojoContainerNodeProcessorBuilder(
							this, containerTypeModel, CollectionValueExtractor.get(),
							contributorProvider, indexModelBinder, bindingContext
					);
					nestedProcessorBuilders.add( containerProcessorBuilder );
				}
				else if ( containerTypeModel.isSubTypeOf( Iterable.class ) ) {
					containerProcessorBuilder = new PojoContainerNodeProcessorBuilder(
							this, containerTypeModel, IterableValueExtractor.get(),
							contributorProvider, indexModelBinder, bindingContext
					);
					nestedProcessorBuilders.add( containerProcessorBuilder );
				}
			}
		}
		return containerProcessorBuilder;
	}

	@Override
	PojoPropertyNodeProcessor<P, T> build() {
		return new PojoPropertyNodeProcessor<>(
				propertyHandle, propertyBridges, functionBridgeProcessors, nestedProcessorBuilders
		);
	}
}
