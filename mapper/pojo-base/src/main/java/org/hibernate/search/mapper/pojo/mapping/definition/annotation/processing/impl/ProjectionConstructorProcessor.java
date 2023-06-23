/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public class ProjectionConstructorProcessor
		implements ConstructorMappingAnnotationProcessor<ProjectionConstructor>,
		TypeMappingAnnotationProcessor<ProjectionConstructor> {

	@Override
	public void process(ConstructorMappingStep mapping, ProjectionConstructor annotation,
			ConstructorMappingAnnotationProcessorContext context) {
		doProcess( mapping );
	}

	@Override
	public void process(TypeMappingStep mapping, ProjectionConstructor annotation,
			TypeMappingAnnotationProcessorContext context) {
		doProcess( mapping.mainConstructor() );
	}

	private void doProcess(ConstructorMappingStep constructor) {
		constructor.projectionConstructor();
	}

}
