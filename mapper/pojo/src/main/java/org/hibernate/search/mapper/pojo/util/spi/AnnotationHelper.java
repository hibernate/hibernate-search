/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.util.spi;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class AnnotationHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MethodHandles.Lookup lookup;

	private final Map<Class<? extends Annotation>, MethodHandle> containedAnnotationsHandleCache = new HashMap<>();

	public AnnotationHelper(MethodHandles.Lookup lookup) {
		this.lookup = lookup;
	}

	public boolean isMetaAnnotated(Annotation annotation, Class<? extends Annotation> metaAnnotationType) {
		return annotation.annotationType().getAnnotationsByType( metaAnnotationType ).length > 0;
	}

	public Stream<? extends Annotation> expandRepeatableContainingAnnotation(Annotation containingAnnotationCandidate) {
		Class<? extends Annotation> containingAnnotationCandidateType = containingAnnotationCandidate.annotationType();
		MethodHandle containedAnnotationsHandle = containedAnnotationsHandleCache.computeIfAbsent(
				containingAnnotationCandidateType, this::createContainedAnnotationsHandle
		);
		if ( containedAnnotationsHandle != null ) {
			try {
				Annotation[] annotationArray = (Annotation[]) containedAnnotationsHandle.invoke( containingAnnotationCandidate );
				return Arrays.stream( annotationArray );
			}
			catch (Throwable e) {
				log.cannotAccessRepeateableContainingAnnotationValue(
						containingAnnotationCandidateType, e
				);
			}
		}
		// Not a containing annotation
		return Stream.of( containingAnnotationCandidate );
	}

	private MethodHandle createContainedAnnotationsHandle(Class<? extends Annotation> containingAnnotationCandidateType) {
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
					try {
						return lookup.unreflect( valueMethod );
					}
					catch (IllegalAccessException e) {
						log.cannotAccessRepeateableContainingAnnotationValue(
								containingAnnotationCandidateType, e
						);
					}
				}
			}
		}
		return null;
	}
}
