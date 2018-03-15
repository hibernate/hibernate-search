/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoModelCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoModelCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

/**
 * @author Yoann Rodiere
 */
public class PropertyMappingContextImpl
		implements PropertyMappingContext, PojoTypeMetadataContributor {

	private final TypeMappingContext parent;
	private final PropertyHandle propertyHandle;

	private final List<PojoMetadataContributor<? super PojoModelCollectorPropertyNode, ? super PojoMappingCollectorPropertyNode>>
			children = new ArrayList<>();

	public PropertyMappingContextImpl(TypeMappingContext parent, PropertyHandle propertyHandle) {
		this.parent = parent;
		this.propertyHandle = propertyHandle;
	}

	@Override
	public void contributeModel(PojoModelCollectorTypeNode collector) {
		PojoModelCollectorPropertyNode propertyNodeCollector = collector.property( propertyHandle.getName() );
		children.forEach( child -> child.contributeModel( propertyNodeCollector ) );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		PojoMappingCollectorPropertyNode propertyNodeCollector = collector.property( propertyHandle );
		children.forEach( child -> child.contributeMapping( propertyNodeCollector ) );
	}

	@Override
	public PropertyDocumentIdMappingContext documentId() {
		PropertyDocumentIdMappingContextImpl child = new PropertyDocumentIdMappingContextImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		return parent.property( propertyName );
	}

	@Override
	public PropertyMappingContext bridge(String bridgeName) {
		return bridge( new ImmutableBeanReference( bridgeName ) );
	}

	@Override
	public PropertyMappingContext bridge(Class<? extends PropertyBridge> bridgeClass) {
		return bridge( new ImmutableBeanReference( bridgeClass ) );
	}

	@Override
	public PropertyMappingContext bridge(String bridgeName, Class<? extends PropertyBridge> bridgeClass) {
		return bridge( new ImmutableBeanReference( bridgeName, bridgeClass ) );
	}

	private PropertyMappingContext bridge(BeanReference bridgeReference) {
		return bridge( new BeanResolverBridgeBuilder<>( PropertyBridge.class, bridgeReference ) );
	}

	@Override
	public PropertyMappingContext bridge(BridgeBuilder<? extends PropertyBridge> builder) {
		children.add( new PropertyBridgeMappingContributor( builder ) );
		return this;
	}

	@Override
	public PropertyMappingContext marker(MarkerBuilder builder) {
		children.add( new MarkerMappingContributor( builder ) );
		return this;
	}

	@Override
	public PropertyFieldMappingContext field() {
		PropertyFieldMappingContextImpl child = new PropertyFieldMappingContextImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyIndexedEmbeddedMappingContext indexedEmbedded() {
		PropertyIndexedEmbeddedMappingContextImpl child = new PropertyIndexedEmbeddedMappingContextImpl( this );
		children.add( child );
		return child;
	}

	@Override
	public PropertyMappingContext containedIn() {
		children.add( new ContainedInMappingContributor() );
		return this;
	}

}
