/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.ErrorCollectingPojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingIndexedStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class TypeMappingStepImpl
		implements TypeMappingStep, PojoMappingConfigurationContributor, PojoTypeMetadataContributor {

	private final PojoRawTypeModel<?> typeModel;

	private final ErrorCollectingPojoTypeMetadataContributor children = new ErrorCollectingPojoTypeMetadataContributor();

	public TypeMappingStepImpl(PojoRawTypeModel<?> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public void configure(MappingBuildContext buildContext, PojoMappingConfigurationContext configurationContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
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
	public TypeMappingIndexedStep indexed() {
		TypeMappingIndexedStepImpl child = new TypeMappingIndexedStepImpl( typeModel.typeIdentifier() );
		children.add( child );
		return child;
	}

	@Override
	public TypeMappingStep binder(TypeBinder binder, Map<String, Object> params) {
		children.add( new TypeBridgeMappingContributor( binder, params ) );
		return this;
	}

	@Override
	public PropertyMappingStep property(String propertyName) {
		PojoPropertyModel<?> propertyModel = typeModel.property( propertyName );
		InitialPropertyMappingStep child = new InitialPropertyMappingStep( this, propertyModel );
		children.add( child );
		return child;
	}

	PojoRawTypeModel<?> getTypeModel() {
		return typeModel;
	}
}
