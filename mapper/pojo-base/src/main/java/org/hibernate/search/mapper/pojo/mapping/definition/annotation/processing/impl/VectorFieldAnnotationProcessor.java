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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldOptionsStep;

public class VectorFieldAnnotationProcessor implements PropertyMappingAnnotationProcessor<VectorField> {

	@Override
	public final void process(PropertyMappingStep mappingContext, VectorField annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String cleanedUpRelativeFieldName = context.toNullIfDefault( annotation.name(), "" );

		PropertyMappingVectorFieldOptionsStep fieldContext =
				mappingContext.vectorField( annotation.dimension(), cleanedUpRelativeFieldName );

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
	}
}
