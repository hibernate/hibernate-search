/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;


public class PropertyFieldMappingContextImpl extends DelegatingPropertyMappingContext
		implements PropertyFieldMappingContext,
		PojoMetadataContributor<PojoAdditionalMetadataCollectorPropertyNode, PojoMappingCollectorPropertyNode> {

	private final String fieldName;

	private BridgeBuilder<? extends ValueBridge<?, ?>> bridgeBuilder;

	private final CompositeFieldModelContributor fieldModelContributor = new CompositeFieldModelContributor();

	private ContainerValueExtractorPath extractorPath = ContainerValueExtractorPath.defaultExtractors();

	PropertyFieldMappingContextImpl(PropertyMappingContext parent, String fieldName) {
		super( parent );
		this.fieldName = fieldName;
	}

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		collector.value( extractorPath )
				.valueBridge( bridgeBuilder, fieldName, fieldModelContributor );
	}

	@Override
	public PropertyFieldMappingContext valueBridge(String bridgeName) {
		return valueBridge( new ImmutableBeanReference( bridgeName ) );
	}

	@Override
	public PropertyFieldMappingContext valueBridge(Class<? extends ValueBridge<?, ?>> bridgeClass) {
		return valueBridge( new ImmutableBeanReference( bridgeClass ) );
	}

	@Override
	public PropertyFieldMappingContext valueBridge(String bridgeName, Class<? extends ValueBridge<?, ?>> bridgeClass) {
		return valueBridge( new ImmutableBeanReference( bridgeName, bridgeClass ) );
	}

	// The builder will return an object of some class T where T extends ValueBridge<?, ?>, so this is safe
	@SuppressWarnings( "unchecked" )
	private PropertyFieldMappingContext valueBridge(BeanReference bridgeReference) {
		return valueBridge(
				(BeanResolverBridgeBuilder<? extends ValueBridge<?, ?>>)
						new BeanResolverBridgeBuilder( ValueBridge.class, bridgeReference )
		);
	}

	@Override
	public PropertyFieldMappingContext valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder) {
		this.bridgeBuilder = builder;
		return this;
	}

	@Override
	public PropertyFieldMappingContext analyzer(String analyzerName) {
		fieldModelContributor.add( c -> c.analyzer( analyzerName ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext normalizer(String normalizerName) {
		fieldModelContributor.add( c -> c.normalizer( normalizerName ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext store(Store store) {
		fieldModelContributor.add( c -> c.store( store ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext sortable(Sortable sortable) {
		fieldModelContributor.add( c -> c.sortable( sortable ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext withExtractors(ContainerValueExtractorPath extractorPath) {
		this.extractorPath = extractorPath;
		return this;
	}

	private static class CompositeFieldModelContributor implements FieldModelContributor {
		private final List<FieldModelContributor> delegates = new ArrayList<>();

		public void add(FieldModelContributor delegate) {
			delegates.add( delegate );
		}

		@Override
		public void contribute(IndexSchemaFieldTypedContext<?> context) {
			delegates.forEach( c -> c.contribute( context ) );
		}
	}

}
