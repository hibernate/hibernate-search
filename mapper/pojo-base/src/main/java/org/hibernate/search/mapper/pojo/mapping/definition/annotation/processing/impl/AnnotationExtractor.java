/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

public abstract class AnnotationExtractor<A extends Annotation> {

	public abstract Stream<? extends A> extractAnnotations(Stream<Annotation> annotations,
			AnnotationHelper annotationHelper);

	protected static <A extends Annotation> Stream<? extends A> extractByType(Stream<Annotation> annotations,
			Class<A> annotationType) {
		return annotations
				.filter( annotation -> annotationType.isAssignableFrom( annotation.annotationType() ) )
				.map( annotationType::cast );
	}

	protected static Stream<Annotation> extractByMetaAnnotationType(Stream<Annotation> annotations,
			AnnotationHelper annotationHelper, Class<? extends Annotation> metaAnnotationType) {
		return annotations
				.filter( annotation -> annotationHelper.isMetaAnnotated( annotation, metaAnnotationType ) );
	}
}
