/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.document.model.spi.TypedFieldModelContext;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


/**
 * @author Yoann Rodiere
 */
public class PropertyFieldMappingContextImpl extends DelegatingPropertyMappingContext
		implements PropertyFieldMappingContext,
				PojoNodeMetadataContributor<PojoPropertyNodeModelCollector, PojoPropertyNodeMappingCollector> {

	private BeanReference<FunctionBridge<?, ?>> bridgeReference;

	private String fieldName;

	private final CompositeFieldModelContributor fieldModelContributor = new CompositeFieldModelContributor();

	public PropertyFieldMappingContextImpl(PropertyMappingContext parent) {
		super( parent );
	}

	@Override
	public void contributeModel(PojoPropertyNodeModelCollector collector) {
		// Nothing to do
	}

	@Override
	public void contributeMapping(PojoPropertyNodeMappingCollector collector) {
		collector.functionBridge( bridgeReference, fieldName, fieldModelContributor );
	}

	@Override
	public PropertyFieldMappingContext name(String name) {
		this.fieldName = name;
		return this;
	}

	@Override
	public PropertyFieldMappingContext bridge(String bridgeName) {
		this.bridgeReference = new ImmutableBeanReference<>( bridgeName );
		return this;
	}

	@Override
	public PropertyFieldMappingContext bridge(Class<? extends FunctionBridge<?, ?>> bridgeClass) {
		this.bridgeReference = new ImmutableBeanReference<>( bridgeClass );
		return this;
	}

	@Override
	public PropertyFieldMappingContext bridge(String bridgeName, Class<? extends FunctionBridge<?, ?>> bridgeClass) {
		this.bridgeReference = new ImmutableBeanReference<>( bridgeName, bridgeClass );
		return this;
	}

	@Override
	public PropertyFieldMappingContext store(Store store) {
		fieldModelContributor.add( c -> c.store( store ) );
		return this;
	}

	private static class CompositeFieldModelContributor implements FieldModelContributor {
		private final List<FieldModelContributor> delegates = new ArrayList<>();

		public void add(FieldModelContributor delegate) {
			delegates.add( delegate );
		}

		@Override
		public void contribute(TypedFieldModelContext<?> context) {
			delegates.forEach( c -> c.contribute( context ) );
		}
	}

}
