/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;

public class TypeMappingContextImpl
		implements TypeMappingContext, PojoMappingConfigurationContributor, PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;

	private String indexName;

	private final ErrorCollectingPojoTypeMetadataContributor children = new ErrorCollectingPojoTypeMetadataContributor();

	public TypeMappingContextImpl(PojoRawTypeModel<?> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public void configure(MappingBuildContext buildContext, ConfigurationPropertySource propertySource,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		if ( indexName != null ) {
			try {
				configurationCollector.mapToIndex( typeModel, indexName );
			}
			catch (RuntimeException e) {
				buildContext.getFailureCollector()
						.withContext( PojoEventContexts.fromType( typeModel ) )
						.add( e );
			}
		}
		configurationCollector.collectContributor( typeModel, this );
	}

	@Override
	public void contributeModel(PojoAdditionalMetadataCollectorTypeNode collector) {
		children.contributeModel( collector );
	}

	@Override
	public void contributeMapping(PojoMappingCollectorTypeNode collector) {
		children.contributeMapping( collector );
	}

	@Override
	public TypeMappingContext indexed() {
		return indexed( typeModel.getJavaClass().getName() );
	}

	@Override
	public TypeMappingContext indexed(String indexName) {
		this.indexName = indexName;
		return this;
	}

	@Override
	public TypeMappingContext routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass) {
		return routingKeyBridge( BeanReference.ofType( bridgeClass ) );
	}

	@Override
	public TypeMappingContext routingKeyBridge(BeanReference bridgeReference) {
		return routingKeyBridge( new BeanResolverBridgeBuilder<>( RoutingKeyBridge.class, bridgeReference ) );
	}

	@Override
	public TypeMappingContext routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder) {
		children.add( new RoutingKeyBridgeMappingContributor( builder ) );
		return this;
	}

	@Override
	public TypeMappingContext bridge(Class<? extends TypeBridge> bridgeClass) {
		return bridge( BeanReference.ofType( bridgeClass ) );
	}

	@Override
	public TypeMappingContext bridge(BeanReference bridgeReference) {
		return bridge( new BeanResolverBridgeBuilder<>( TypeBridge.class, bridgeReference ) );
	}

	@Override
	public TypeMappingContext bridge(BridgeBuilder<? extends TypeBridge> builder) {
		children.add( new TypeBridgeMappingContributor( builder ) );
		return this;
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		PojoPropertyModel<?> propertyModel = typeModel.getProperty( propertyName );
		PropertyHandle propertyHandle = propertyModel.getHandle();
		PropertyMappingContextImpl child = new PropertyMappingContextImpl( this, propertyHandle );
		children.add( child );
		return child;
	}

}
