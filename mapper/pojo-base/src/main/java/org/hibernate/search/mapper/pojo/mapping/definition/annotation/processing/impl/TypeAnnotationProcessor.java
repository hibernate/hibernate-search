/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public abstract class TypeAnnotationProcessor<A extends Annotation> {

	final AnnotationProcessorHelper helper;

	TypeAnnotationProcessor(AnnotationProcessorHelper helper) {
		this.helper = helper;
	}

	/**
	 * Interpret annotations in the given context and push the translated metadata
	 * to the given mapping context.
	 *
	 * @param mappingContext The mapping context to push translated information to.
	 * @param typeModel A model of the type holding the annotation.
	 * @return {@code true} if at least one annotation was processed, {@code false} otherwise.
	 */
	public final boolean process(TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel) {
		List<A> annotationList = extractAnnotations( typeModel ).collect( Collectors.toList() );
		processEach( mappingContext, typeModel, annotationList );
		return !annotationList.isEmpty();
	}

	abstract Stream<? extends A> extractAnnotations(PojoRawTypeModel<?> typeModel);

	abstract void doProcess(TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel, A annotation);

	private void processEach(TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel,
			List<A> annotationList) {
		for ( A annotation : annotationList ) {
			try {
				doProcess( mappingContext, typeModel, annotation );
			}
			catch (RuntimeException e) {
				helper.getRootFailureCollector()
						.withContext( PojoEventContexts.fromType( typeModel ) )
						.withContext( PojoEventContexts.fromAnnotation( annotation ) )
						.add( e );
			}
		}
	}
}
