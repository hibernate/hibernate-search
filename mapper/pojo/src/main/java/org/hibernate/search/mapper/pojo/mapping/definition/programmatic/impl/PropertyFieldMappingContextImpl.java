/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;


abstract class PropertyFieldMappingContextImpl<S extends PropertyFieldMappingContext<?>, C extends StandardIndexSchemaFieldTypedContext<?, ?>>
		extends DelegatingPropertyMappingContext
		implements PojoPropertyMetadataContributor, PropertyFieldMappingContext<S> {

	private final String relativeFieldName;

	private BridgeBuilder<? extends ValueBridge<?, ?>> bridgeBuilder;

	final CompositeFieldModelContributor<C> fieldModelContributor;

	private ContainerValueExtractorPath extractorPath = ContainerValueExtractorPath.defaultExtractors();

	PropertyFieldMappingContextImpl(PropertyMappingContext parent, String relativeFieldName,
			Function<StandardIndexSchemaFieldTypedContext<?, ?>, C> contextConverter) {
		super( parent );
		this.relativeFieldName = relativeFieldName;
		this.fieldModelContributor = new CompositeFieldModelContributor<>( contextConverter );
	}

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		collector.value( extractorPath )
				.valueBridge( bridgeBuilder, relativeFieldName, fieldModelContributor );
	}

	abstract S thisAsS();

	@Override
	public S store(Store store) {
		fieldModelContributor.add( c -> c.store( store ) );
		return thisAsS();
	}

	@Override
	public S valueBridge(String bridgeName) {
		return valueBridge( BeanReference.ofName( bridgeName ) );
	}

	@Override
	public S valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass) {
		return valueBridge( BeanReference.ofType( bridgeClass ) );
	}

	@Override
	public S valueBridge(String bridgeName, Class<? extends ValueBridge<?, ?>> bridgeClass) {
		return valueBridge( BeanReference.of( bridgeName, bridgeClass ) );
	}

	// The builder will return an object of some class T where T extends ValueBridge<?, ?>, so this is safe
	@SuppressWarnings( "unchecked" )
	private S valueBridge(BeanReference bridgeReference) {
		return valueBridge(
				(BeanResolverBridgeBuilder<? extends ValueBridge<?, ?>>)
						new BeanResolverBridgeBuilder( ValueBridge.class, bridgeReference )
		);
	}

	@Override
	public S valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder) {
		this.bridgeBuilder = builder;
		return thisAsS();
	}

	@Override
	public S withExtractors(ContainerValueExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return thisAsS();
	}

	static class CompositeFieldModelContributor<C extends StandardIndexSchemaFieldTypedContext<?, ?>>
			implements FieldModelContributor {
		private final Function<StandardIndexSchemaFieldTypedContext<?, ?>, C> contextConverter;
		private final List<Consumer<C>> delegates = new ArrayList<>();

		private CompositeFieldModelContributor(Function<StandardIndexSchemaFieldTypedContext<?, ?>, C> contextConverter) {
			this.contextConverter = contextConverter;
		}

		public void add(Consumer<C> delegate) {
			delegates.add( delegate );
		}

		@Override
		public void contribute(StandardIndexSchemaFieldTypedContext<?, ?> context) {
			C convertedContext = contextConverter.apply( context );
			delegates.forEach( c -> c.accept( convertedContext ) );
		}
	}

}
