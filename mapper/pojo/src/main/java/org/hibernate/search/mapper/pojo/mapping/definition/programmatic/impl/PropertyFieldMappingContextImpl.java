/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoModelCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


public class PropertyFieldMappingContextImpl extends DelegatingPropertyMappingContext
		implements PropertyFieldMappingContext,
		PojoMetadataContributor<PojoModelCollectorPropertyNode, PojoMappingCollectorPropertyNode> {

	private final String fieldName;

	private BridgeBuilder<? extends FunctionBridge<?, ?>> bridgeBuilder;

	private final CompositeFieldModelContributor fieldModelContributor = new CompositeFieldModelContributor();

	private List<Class<? extends ContainerValueExtractor>> extractorClasses = null;

	PropertyFieldMappingContextImpl(PropertyMappingContext parent, String fieldName) {
		super( parent );
		this.fieldName = fieldName;
	}

	@Override
	public void contributeModel(PojoModelCollectorPropertyNode collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoMappingCollectorPropertyNode collector) {
		PojoMappingCollectorValueNode valueNodeMappingCollector;
		if ( extractorClasses == null ) {
			valueNodeMappingCollector = collector.valueWithDefaultExtractors();
		}
		else if ( extractorClasses.isEmpty() ) {
			valueNodeMappingCollector = collector.valueWithoutExtractors();
		}
		else {
			valueNodeMappingCollector = collector.valueWithExtractors( extractorClasses );
		}
		valueNodeMappingCollector.functionBridge( bridgeBuilder, fieldName, fieldModelContributor );
	}

	@Override
	public PropertyFieldMappingContext functionBridge(String bridgeName) {
		return functionBridge( new ImmutableBeanReference( bridgeName ) );
	}

	@Override
	public PropertyFieldMappingContext functionBridge(Class<? extends FunctionBridge<?, ?>> bridgeClass) {
		return functionBridge( new ImmutableBeanReference( bridgeClass ) );
	}

	@Override
	public PropertyFieldMappingContext functionBridge(String bridgeName, Class<? extends FunctionBridge<?, ?>> bridgeClass) {
		return functionBridge( new ImmutableBeanReference( bridgeName, bridgeClass ) );
	}

	// The builder will return an object of some class T where T extends FunctionBridge<?, ?>, so this is safe
	@SuppressWarnings( "unchecked" )
	private PropertyFieldMappingContext functionBridge(BeanReference bridgeReference) {
		return functionBridge(
				(BeanResolverBridgeBuilder<? extends FunctionBridge<?, ?>>)
						new BeanResolverBridgeBuilder( FunctionBridge.class, bridgeReference )
		);
	}

	@Override
	public PropertyFieldMappingContext functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder) {
		this.bridgeBuilder = builder;
		return this;
	}

	@Override
	public PropertyFieldMappingContext store(Store store) {
		fieldModelContributor.add( c -> c.store( store ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext withExtractors(
			List<? extends Class<? extends ContainerValueExtractor>> extractorClasses) {
		this.extractorClasses = new ArrayList<>( extractorClasses );
		return this;
	}

	@Override
	public PropertyFieldMappingContext withoutExtractors() {
		this.extractorClasses = Collections.emptyList();
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
