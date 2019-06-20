/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class TypeMappingContextImpl
		implements TypeMappingContext, PojoMappingConfigurationContributor, PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;

	private String backendName;
	private String indexName;

	private final ErrorCollectingPojoTypeMetadataContributor children = new ErrorCollectingPojoTypeMetadataContributor();

	public TypeMappingContextImpl(PojoRawTypeModel<?> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		if ( indexName != null ) {
			try {
				configurationCollector.mapToIndex( typeModel, backendName, indexName );
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
	public void contributeAdditionalMetadata(PojoAdditionalMetadataCollectorTypeNode collector) {
		children.contributeAdditionalMetadata( collector );
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
	public TypeMappingContext indexed(String backendName, String indexName) {
		this.backendName = backendName;
		this.indexName = indexName;
		return this;
	}

	@Override
	public TypeMappingContext routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass) {
		return routingKeyBridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public TypeMappingContext routingKeyBridge(BeanReference<? extends RoutingKeyBridge> bridgeReference) {
		return routingKeyBridge( new BeanBridgeBuilder<>( bridgeReference ) );
	}

	@Override
	public TypeMappingContext routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder) {
		children.add( new RoutingKeyBridgeMappingContributor( builder ) );
		return this;
	}

	@Override
	public TypeMappingContext bridge(Class<? extends TypeBridge> bridgeClass) {
		return bridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public TypeMappingContext bridge(BeanReference<? extends TypeBridge> bridgeReference) {
		return bridge( new BeanBridgeBuilder<>( bridgeReference ) );
	}

	@Override
	public TypeMappingContext bridge(BridgeBuilder<? extends TypeBridge> builder) {
		children.add( new TypeBridgeMappingContributor( builder ) );
		return this;
	}

	@Override
	public PropertyMappingContext property(String propertyName) {
		PojoPropertyModel<?> propertyModel = typeModel.getProperty( propertyName );
		InitialPropertyMappingContext child = new InitialPropertyMappingContext( this, propertyModel );
		children.add( child );
		return child;
	}

}
