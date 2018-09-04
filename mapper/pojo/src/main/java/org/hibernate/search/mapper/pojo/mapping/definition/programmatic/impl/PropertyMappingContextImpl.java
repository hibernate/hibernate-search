/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.common.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.AssociationInverseSideMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyDocumentIdMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyIndexedEmbeddedMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

public class PropertyMappingContextImpl
		implements PropertyMappingContext, PojoTypeMetadataContributor {

	private final TypeMappingContext parent;
	private final PropertyHandle propertyHandle;

	private final ErrorCollectingPojoPropertyMetadataContributor children =
			new ErrorCollectingPojoPropertyMetadataContributor();

	PropertyMappingContextImpl(TypeMappingContext parent, PropertyHandle propertyHandle) {
		this.parent = parent;
		this.propertyHandle = propertyHandle;
	}

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorTypeNode collector) {
		PojoAdditionalMetadataCollectorPropertyNode collectorPropertyNode =
				collector.property( propertyHandle.getName() );
		children.contributeModel( collectorPropertyNode );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		PojoMappingCollectorPropertyNode collectorPropertyNode = collector.property( propertyHandle );
		children.contributeMapping( collectorPropertyNode );
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
		return field( null );
	}

	@Override
	public PropertyFieldMappingContext field(String relativeFieldName) {
		PropertyFieldMappingContextImpl child = new PropertyFieldMappingContextImpl( this, relativeFieldName );
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
	public AssociationInverseSideMappingContext associationInverseSide(PojoModelPathValueNode inversePath) {
		AssociationInverseSideMappingContextImpl child = new AssociationInverseSideMappingContextImpl(
				this, inversePath
		);
		children.add( child );
		return child;
	}

	@Override
	public IndexingDependencyMappingContext indexingDependency() {
		IndexingDependencyMappingContextImpl child = new IndexingDependencyMappingContextImpl( this );
		children.add( child );
		return child;
	}
}
