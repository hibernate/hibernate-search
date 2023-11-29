/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldOptionsStep;

public class VectorFieldAnnotationProcessor extends AbstractFieldAnnotationProcessor<VectorField>
		implements PropertyMappingAnnotationProcessor<VectorField> {

	@Override
	PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext, VectorField annotation,
			String fieldName) {
		int dimension = annotation.dimension();
		PropertyMappingVectorFieldOptionsStep fieldContext = dimension == AnnotationDefaultValues.DEFAULT_DIMENSION
				? mappingContext.vectorField( fieldName )
				: mappingContext.vectorField( dimension, fieldName );

		int maxConnections = annotation.maxConnections();
		if ( maxConnections != AnnotationDefaultValues.DEFAULT_MAX_CONNECTIONS ) {
			fieldContext.maxConnections( maxConnections );
		}

		int beamWidth = annotation.beamWidth();
		if ( beamWidth != AnnotationDefaultValues.DEFAULT_BEAM_WIDTH ) {
			fieldContext.beamWidth( beamWidth );
		}

		VectorSimilarity vectorSimilarity = annotation.vectorSimilarity();
		if ( !VectorSimilarity.DEFAULT.equals( vectorSimilarity ) ) {
			fieldContext.vectorSimilarity( vectorSimilarity );
		}
		Projectable projectable = annotation.projectable();
		if ( !Projectable.DEFAULT.equals( projectable ) ) {
			fieldContext.projectable( projectable );
		}

		Searchable searchable = annotation.searchable();
		if ( !Searchable.DEFAULT.equals( searchable ) ) {
			fieldContext.searchable( searchable );
		}

		String indexNullAs = annotation.indexNullAs();
		if ( indexNullAs != null && !AnnotationDefaultValues.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			fieldContext.indexNullAs( indexNullAs );
		}

		return fieldContext;
	}

	@Override
	ContainerExtraction getExtraction(VectorField annotation) {
		return annotation.extraction();
	}

	@Override
	String getName(VectorField annotation) {
		return annotation.name();
	}

	@Override
	ValueBridgeRef getValueBridge(VectorField annotation) {
		return annotation.valueBridge();
	}

	@Override
	ValueBinderRef getValueBinder(VectorField annotation) {
		return annotation.valueBinder();
	}

}
