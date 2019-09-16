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
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;

public class TypeMappingStepImpl
		implements TypeMappingStep, PojoMappingConfigurationContributor, PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;

	private String backendName;
	private String indexName;

	private final ErrorCollectingPojoTypeMetadataContributor children = new ErrorCollectingPojoTypeMetadataContributor();

	public TypeMappingStepImpl(PojoRawTypeModel<?> typeModel) {
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
	public TypeMappingStep indexed() {
		return indexed( null, typeModel.getJavaClass().getName() );
	}

	@Override
	public TypeMappingStep indexed(String indexName) {
		return indexed( null, indexName );
	}

	@Override
	public TypeMappingStep indexed(String backendName, String indexName) {
		this.backendName = backendName;
		this.indexName = indexName;
		children.add( new IndexedMetadataContributor( typeModel, backendName, indexName ) );
		return this;
	}

	@Override
	public TypeMappingStep routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass) {
		return routingKeyBridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public TypeMappingStep routingKeyBridge(BeanReference<? extends RoutingKeyBridge> bridgeReference) {
		return routingKeyBinder( new BeanBinder( bridgeReference ) );
	}

	@Override
	public TypeMappingStep routingKeyBinder(RoutingKeyBinder<?> binder) {
		children.add( new RoutingKeyBridgeMappingContributor( binder ) );
		return this;
	}

	@Override
	public TypeMappingStep bridge(Class<? extends TypeBridge> bridgeClass) {
		return bridge( BeanReference.of( bridgeClass ) );
	}

	@Override
	public TypeMappingStep bridge(BeanReference<? extends TypeBridge> bridgeReference) {
		return binder( new BeanBinder( bridgeReference ) );
	}

	@Override
	public TypeMappingStep binder(TypeBinder<?> binder) {
		children.add( new TypeBridgeMappingContributor( binder ) );
		return this;
	}

	@Override
	public PropertyMappingStep property(String propertyName) {
		PojoPropertyModel<?> propertyModel = typeModel.getProperty( propertyName );
		InitialPropertyMappingStep child = new InitialPropertyMappingStep( this, propertyModel );
		children.add( child );
		return child;
	}

}
