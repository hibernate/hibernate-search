/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldOptionsStep;

class PropertyMappingVectorFieldOptionsStepImpl
		extends AbstractPropertyMappingFieldOptionsStep<PropertyMappingVectorFieldOptionsStep>
		implements PropertyMappingVectorFieldOptionsStep, PojoPropertyMetadataContributor {

	PropertyMappingVectorFieldOptionsStepImpl(PropertyMappingStep parent, Integer dimension, String relativeFieldName) {
		super( parent, relativeFieldName,
				IndexFieldTypeFactory::asVector,
				FieldModelContributorContext::vectorTypeOptionsStep );
		if ( dimension != null ) {
			fieldModelContributor.add( c -> c.vectorTypeOptionsStep().dimension( dimension ) );
		}
		extractors( ContainerExtractorPath.noExtractors() );
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep projectable(Projectable projectable) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().projectable( projectable ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep searchable(Searchable searchable) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().searchable( searchable ) );
		return this;
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep vectorSimilarity(VectorSimilarity vectorSimilarity) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().vectorSimilarity( vectorSimilarity ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep efConstruction(int efConstruction) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().efConstruction( efConstruction ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep m(int m) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().m( m ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep indexNullAs(String indexNullAs) {
		fieldModelContributor.add( c -> c.indexNullAs( indexNullAs ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep extractors(ContainerExtractorPath extractorPath) {
		if ( extractorPath.isDefault() ) {
			throw MappingLog.INSTANCE.vectorFieldMustUseExplicitExtractorPath();
		}
		return super.extractors( extractorPath );
	}

	@Override
	PropertyMappingVectorFieldOptionsStepImpl thisAsS() {
		return this;
	}
}
