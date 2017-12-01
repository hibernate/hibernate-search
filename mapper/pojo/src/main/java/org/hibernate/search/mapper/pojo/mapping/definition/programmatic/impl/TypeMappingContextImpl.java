/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.engine.mapper.processing.RoutingKeyBridge;

/**
 * @author Yoann Rodiere
 */
public class TypeMappingContextImpl implements TypeMappingContext, MetadataContributor, PojoTypeNodeMetadataContributor {

	private final PojoMapperFactory<?> mapperFactory;
	private final Class<?> type;

	private String indexName;
	private BeanReference<RoutingKeyBridge> routingKeyBridgeReference;

	private final List<PojoNodeMetadataContributor<? super PojoTypeNodeModelCollector, ? super PojoTypeNodeMappingCollector>>
			children = new ArrayList<>();

	public TypeMappingContextImpl(PojoMapperFactory<?> mapperFactory, Class<?> type) {
		this.mapperFactory = mapperFactory;
		this.type = type;
	}

	@Override
	public void contribute(TypeMetadataCollector collector) {
		collector.collect( mapperFactory, new PojoIndexedTypeIdentifier( type ), indexName, this );
	}

	@Override
	public void contributeModel(PojoTypeNodeModelCollector collector) {
		children.stream().forEach( c -> c.contributeModel( collector ) );
	}

	@Override
	public void contributeMapping(PojoTypeNodeMappingCollector collector) {
		if ( routingKeyBridgeReference != null ) {
			collector.routingKeyBridge( routingKeyBridgeReference );
		}
		children.stream().forEach( c -> c.contributeMapping( collector ) );
	}

	@Override
	public TypeMappingContext indexed() {
		return indexed( type.getName() );
	}

	@Override
	public TypeMappingContext indexed(String indexName) {
		this.indexName = indexName;
		return this;
	}

	@Override
	public TypeMappingContext routingKeyBridge(String bridgeName) {
		this.routingKeyBridgeReference = new ImmutableBeanReference<>( bridgeName );
		return this;
	}

	@Override
	public TypeMappingContext routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass) {
		this.routingKeyBridgeReference = new ImmutableBeanReference<>( bridgeClass );
		return this;
	}

	@Override
	public TypeMappingContext routingKeyBridge(
			String bridgeName, Class<? extends RoutingKeyBridge> bridgeClass) {
		this.routingKeyBridgeReference = new ImmutableBeanReference<>( bridgeName, bridgeClass );
		return this;
	}

	@Override
	public TypeMappingContext bridge(BridgeDefinition<?> definition) {
		children.add( new BridgeMappingContributor( definition ) );
		return this;
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		PropertyMappingContextImpl child = new PropertyMappingContextImpl( this, propertyName );
		children.add( child );
		return child;
	}

}
