/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoValueNodeMappingCollector;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorPropertyNode;

public class PojoIndexingProcessorPropertyNodeBuilder<P, T> extends AbstractPojoProcessorNodeBuilder<P>
		implements PojoPropertyNodeMappingCollector {

	private final PojoTypeModel<P> parentTypeModel;
	private final PropertyHandle propertyHandle;
	private final PojoGenericTypeModel<T> propertyTypeModel;
	private final PojoModelPropertyRootElement pojoModelRootElement;

	private final PojoTypeNodeIdentityMappingCollector identityMappingCollector;

	private final Collection<PropertyBridge> propertyBridges = new ArrayList<>();
	private final PojoIndexingProcessorValueNodeBuilderDelegate<T> valueWithoutExtractorBuilderDelegate;
	private Map<List<? extends Class<? extends ContainerValueExtractor>>,
			PojoIndexingProcessorContainerElementNodeBuilder<? super T, ?>> containerElementNodeBuilders = new HashMap<>();

	PojoIndexingProcessorPropertyNodeBuilder(
			PojoIndexingProcessorTypeNodeBuilder<P> parent, PojoTypeModel<P> parentTypeModel,
			PojoPropertyModel<T> propertyModel, PropertyHandle propertyHandle,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		super( parent, contributorProvider, indexModelBinder, bindingContext );
		this.parentTypeModel = parentTypeModel;
		this.propertyHandle = propertyHandle;
		this.propertyTypeModel = propertyModel.getTypeModel();

		// FIXME do something more with the pojoModelRootElement, to be able to use it in containedIn processing in particular
		this.pojoModelRootElement = new PojoModelPropertyRootElement( propertyModel, contributorProvider );

		this.identityMappingCollector = identityMappingCollector;

		this.valueWithoutExtractorBuilderDelegate = new PojoIndexingProcessorValueNodeBuilderDelegate<>(
				this, parentTypeModel, propertyHandle.getName(), propertyTypeModel,
				contributorProvider, indexModelBinder, bindingContext
		);
	}

	@Override
	public void bridge(BridgeBuilder<? extends PropertyBridge> builder) {
		indexModelBinder.addPropertyBridge( bindingContext, pojoModelRootElement, builder )
				.ifPresent( propertyBridges::add );
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
	public PojoValueNodeMappingCollector valueWithoutExtractors() {
		return valueWithoutExtractorBuilderDelegate;
	}

	@Override
	public PojoValueNodeMappingCollector valueWithDefaultExtractors() {
		PojoIndexingProcessorContainerElementNodeBuilder<? super T, ?> containerElementNodeBuilder =
				containerElementNodeBuilders.get( null );
		if ( containerElementNodeBuilder == null && !containerElementNodeBuilders.containsKey( null ) ) {
			Optional<BoundContainerValueExtractor<? super T, ?>> boundExtractorOptional =
					indexModelBinder.createDefaultExtractors( propertyTypeModel );
			if ( boundExtractorOptional.isPresent() ) {
				containerElementNodeBuilder = createContainerElementNodeBuilder( boundExtractorOptional.get() );
			}
			containerElementNodeBuilders.put( null, containerElementNodeBuilder );
		}
		if ( containerElementNodeBuilder != null ) {
			return containerElementNodeBuilder.value();
		}
		else {
			return valueWithoutExtractors();
		}
	}

	@Override
	public PojoValueNodeMappingCollector valueWithExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses) {
		PojoIndexingProcessorContainerElementNodeBuilder<? super T, ?> containerElementNodeBuilder =
				containerElementNodeBuilders.get( extractorClasses );
		if ( containerElementNodeBuilder == null ) {
			BoundContainerValueExtractor<? super T, ?> boundExtractor =
					indexModelBinder.<T>createExplicitExtractors( propertyTypeModel, extractorClasses );
			containerElementNodeBuilder = createContainerElementNodeBuilder( boundExtractor );
			containerElementNodeBuilders.put( extractorClasses, containerElementNodeBuilder );
		}
		return containerElementNodeBuilder.value();
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyHandle.getName() );
	}

	/*
	 * This generic method is necessary to make it clear to the compiler
	 * that the extracted type and extractor have compatible generic arguments.
	 */
	private <V> PojoIndexingProcessorContainerElementNodeBuilder<? super T, V> createContainerElementNodeBuilder(
			BoundContainerValueExtractor<? super T, V> boundExtractor) {
		return new PojoIndexingProcessorContainerElementNodeBuilder<>(
				this, parentTypeModel, propertyHandle.getName(),
				boundExtractor.getExtractedType(), boundExtractor.getExtractor(),
				contributorProvider, indexModelBinder, bindingContext
		);
	}

	@Override
	Optional<PojoIndexingProcessorPropertyNode<P, T>> build() {
		Collection<PropertyBridge> immutableBridges = propertyBridges.isEmpty() ? Collections.emptyList() : new ArrayList<>( propertyBridges );
		Collection<PojoIndexingProcessor<? super T>> valueWithoutExtractorNodes =
				valueWithoutExtractorBuilderDelegate.build();
		Collection<PojoIndexingProcessor<? super T>> immutableNestedNodes =
				valueWithoutExtractorNodes.isEmpty() && containerElementNodeBuilders.isEmpty()
				? Collections.emptyList()
				: new ArrayList<>( valueWithoutExtractorNodes.size() + containerElementNodeBuilders.size() );
		if ( !valueWithoutExtractorNodes.isEmpty() ) {
			immutableNestedNodes.addAll( valueWithoutExtractorNodes );
		}
		containerElementNodeBuilders.values().stream()
				.filter( Objects::nonNull )
				.map( AbstractPojoProcessorNodeBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedNodes::add );

		if ( immutableBridges.isEmpty() && immutableNestedNodes.isEmpty() ) {
			/*
			 * If this node doesn't have any bridge, nor any nested node,
			 * it is useless and we don't need to build it.
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoIndexingProcessorPropertyNode<>(
					propertyHandle, immutableBridges, immutableNestedNodes
			) );
		}
	}
}
