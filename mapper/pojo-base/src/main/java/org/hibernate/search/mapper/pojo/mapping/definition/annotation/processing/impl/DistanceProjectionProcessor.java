/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DistanceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.DistanceProjectionBinder;

public final class DistanceProjectionProcessor implements MethodParameterMappingAnnotationProcessor<DistanceProjection> {

	@Override
	public void process(MethodParameterMappingStep mapping, DistanceProjection annotation,
			MethodParameterMappingAnnotationProcessorContext context) {
		mapping.projection( DistanceProjectionBinder.create(
				context.toNullIfDefault( annotation.path(), "" ),
				annotation.fromParam()
		).unit( annotation.unit() ) );
	}

}
