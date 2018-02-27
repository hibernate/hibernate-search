/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

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

/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNodeProcessorBuilder<P, T> extends AbstractPojoNodeProcessorBuilder<P>
		implements PojoPropertyNodeMappingCollector {

	private final PojoTypeModel<P> parentTypeModel;
	private final PropertyHandle propertyHandle;
	private final PojoGenericTypeModel<T> propertyTypeModel;
	private final PojoModelPropertyRootElement pojoModelRootElement;

	private final PojoTypeNodeIdentityMappingCollector identityMappingCollector;

	private final Collection<PropertyBridge> propertyBridges = new ArrayList<>();
	private final PojoValueNodeProcessorCollectionBuilder<T> valueWithoutExtractorProcessorCollectionBuilder;
	private Map<List<? extends Class<? extends ContainerValueExtractor>>,
			PojoContainerNodeProcessorBuilder<? super T, ?>> containerProcessorBuilders = new HashMap<>();

	PojoPropertyNodeProcessorBuilder(
			PojoTypeNodeProcessorBuilder<P> parent, PojoTypeModel<P> parentTypeModel,
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

		this.valueWithoutExtractorProcessorCollectionBuilder = new PojoValueNodeProcessorCollectionBuilder<>(
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
		return valueWithoutExtractorProcessorCollectionBuilder;
	}

	@Override
	public PojoValueNodeMappingCollector valueWithDefaultExtractors() {
		PojoContainerNodeProcessorBuilder<? super T, ?> containerProcessorBuilder =
				containerProcessorBuilders.get( null );
		if ( containerProcessorBuilder == null && !containerProcessorBuilders.containsKey( null ) ) {
			Optional<BoundContainerValueExtractor<? super T, ?>> boundExtractorOptional =
					indexModelBinder.createDefaultExtractors( propertyTypeModel );
			if ( boundExtractorOptional.isPresent() ) {
				containerProcessorBuilder = createContainerProcessorBuilder( boundExtractorOptional.get() );
			}
			containerProcessorBuilders.put( null, containerProcessorBuilder );
		}
		if ( containerProcessorBuilder != null ) {
			return containerProcessorBuilder.value();
		}
		else {
			return valueWithoutExtractors();
		}
	}

	@Override
	public PojoValueNodeMappingCollector valueWithExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses) {
		PojoContainerNodeProcessorBuilder<? super T, ?> containerProcessorBuilder =
				containerProcessorBuilders.get( extractorClasses );
		if ( containerProcessorBuilder == null ) {
			BoundContainerValueExtractor<? super T, ?> boundExtractor =
					indexModelBinder.<T>createExplicitExtractors( propertyTypeModel, extractorClasses );
			containerProcessorBuilder = createContainerProcessorBuilder( boundExtractor );
			containerProcessorBuilders.put( extractorClasses, containerProcessorBuilder );
		}
		return containerProcessorBuilder.value();
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyHandle.getName() );
	}

	/*
	 * This generic method is necessary to make it clear to the compiler
	 * that the extracted type and extractor have compatible generic arguments.
	 */
	private <V> PojoContainerNodeProcessorBuilder<? super T, V> createContainerProcessorBuilder(
			BoundContainerValueExtractor<? super T, V> boundExtractor) {
		return new PojoContainerNodeProcessorBuilder<>(
				this, parentTypeModel, propertyHandle.getName(),
				boundExtractor.getExtractedType(), boundExtractor.getExtractor(),
				contributorProvider, indexModelBinder, bindingContext
		);
	}

	@Override
	Optional<PojoPropertyNodeProcessor<P, T>> build() {
		Collection<PropertyBridge> immutableBridges = propertyBridges.isEmpty() ? Collections.emptyList() : new ArrayList<>( propertyBridges );
		Collection<PojoNodeProcessor<? super T>> valueWithoutExtractorProcessors =
				valueWithoutExtractorProcessorCollectionBuilder.build();
		Collection<PojoNodeProcessor<? super T>> immutableNestedProcessors =
				valueWithoutExtractorProcessors.isEmpty() && containerProcessorBuilders.isEmpty()
				? Collections.emptyList()
				: new ArrayList<>( valueWithoutExtractorProcessors.size() + containerProcessorBuilders.size() );
		if ( !valueWithoutExtractorProcessors.isEmpty() ) {
			immutableNestedProcessors.addAll( valueWithoutExtractorProcessors );
		}
		containerProcessorBuilders.values().stream()
				.filter( Objects::nonNull )
				.map( AbstractPojoNodeProcessorBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedProcessors::add );

		if ( immutableBridges.isEmpty() && immutableNestedProcessors.isEmpty() ) {
			/*
			 * If this processor doesn't have any bridge, nor any nested processor,
			 * it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoPropertyNodeProcessor<>(
					propertyHandle, immutableBridges, immutableNestedProcessors
			) );
		}
	}
}
