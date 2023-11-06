/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoPropertyMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldOptionsStep;

class PropertyMappingVectorFieldOptionsStepImpl
		extends AbstractPropertyMappingFieldOptionsStep<PropertyMappingVectorFieldOptionsStepImpl>
		implements PropertyMappingVectorFieldOptionsStep, PojoPropertyMetadataContributor {

	PropertyMappingVectorFieldOptionsStepImpl(PropertyMappingStep parent, int dimension, String relativeFieldName) {
		super( parent, relativeFieldName, c -> c.vectorTypeOptionsStep().dimension( dimension ) );
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
	public PropertyMappingVectorFieldOptionsStep beamWidth(int beamWidth) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().beamWidth( beamWidth ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep maxConnections(int maxConnections) {
		fieldModelContributor.add( c -> c.vectorTypeOptionsStep().maxConnections( maxConnections ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingVectorFieldOptionsStep indexNullAs(String indexNullAs) {
		fieldModelContributor.add( c -> c.indexNullAs( indexNullAs ) );
		return thisAsS();
	}

	@Override
	PropertyMappingVectorFieldOptionsStepImpl thisAsS() {
		return this;
	}
}
