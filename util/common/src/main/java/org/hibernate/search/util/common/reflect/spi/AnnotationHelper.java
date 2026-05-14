/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reflect.spi;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.accessor.HibernateAccessorFactory;
import org.hibernate.accessor.HibernateAccessorValueReader;
import org.hibernate.search.util.common.logging.impl.CommonMiscLog;

public final class AnnotationHelper {

	private final HibernateAccessorFactory handleFactory;

	private final Map<Class<? extends Annotation>, HibernateAccessorValueReader<Annotation[]>> containedAnnotationsHandleCache =
			new HashMap<>();

	public AnnotationHelper(HibernateAccessorFactory handleFactory) {
		this.handleFactory = handleFactory;
	}

	public Stream<? extends Annotation> expandRepeatableContainingAnnotation(Annotation containingAnnotationCandidate) {
		Class<? extends Annotation> containingAnnotationCandidateType = containingAnnotationCandidate.annotationType();
		HibernateAccessorValueReader<Annotation[]> containedAnnotationsHandle = containedAnnotationsHandleCache.computeIfAbsent(
				containingAnnotationCandidateType, this::createContainedAnnotationsHandle
		);
		if ( containedAnnotationsHandle != null ) {
			try {
				Annotation[] annotationArray = containedAnnotationsHandle.get( containingAnnotationCandidate );
				return Arrays.stream( annotationArray );
			}
			catch (Throwable e) {
				CommonMiscLog.INSTANCE.cannotAccessRepeateableContainingAnnotationValue(
						containingAnnotationCandidateType, e
				);
			}
		}
		// Not a containing annotation
		return Stream.of( containingAnnotationCandidate );
	}

	private HibernateAccessorValueReader<Annotation[]> createContainedAnnotationsHandle(
			Class<? extends Annotation> containingAnnotationCandidateType) {
		Method valueMethod;
		try {
			valueMethod = containingAnnotationCandidateType.getDeclaredMethod( "value" );
		}
		catch (NoSuchMethodException e) {
			// Not a containing annotation
			return null;
		}
		Class<?> valueMethodReturnType = valueMethod.getReturnType();
		if ( valueMethodReturnType.isArray() ) {
			Class<?> elementType = valueMethodReturnType.getComponentType();
			if ( Annotation.class.isAssignableFrom( elementType ) ) {
				Repeatable repeatable = elementType.getAnnotation( Repeatable.class );
				if ( repeatable != null && containingAnnotationCandidateType.equals( repeatable.value() ) ) {
					@SuppressWarnings("unchecked") // Checked using reflection just above
					HibernateAccessorValueReader<Annotation[]> result =
							(HibernateAccessorValueReader<Annotation[]>) handleFactory.valueReader( valueMethod );
					return result;
				}
			}
		}
		return null;
	}
}
