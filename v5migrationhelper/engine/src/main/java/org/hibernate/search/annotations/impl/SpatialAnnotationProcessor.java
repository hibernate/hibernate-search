/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations.impl;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.bridge.builtin.impl.CoordinatesBridge;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public class SpatialAnnotationProcessor implements TypeMappingAnnotationProcessor<Spatial>,
		PropertyMappingAnnotationProcessor<Spatial> {
	@Override
	public void process(TypeMappingStep mapping, Spatial annotation,
			TypeMappingAnnotationProcessorContext context) {
		mapping.binder( createBinder( annotation ) );
	}

	@Override
	public void process(PropertyMappingStep mapping, Spatial annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.binder( createBinder( annotation ) );
	}

	private CoordinatesBridge.Binder createBinder(Spatial annotation) {
		return new CoordinatesBridge.Binder()
				.fieldName( annotation.name() )
				.markerSet( annotation.name() )
				// The "distance" projection used to be available regardless of configuration,
				// so we need to always mark the field as projectable.
				// As a nasty side-effect, the field will always be stored, regardless of the "store" attribute...
				.projectable( Projectable.YES );
	}

}
