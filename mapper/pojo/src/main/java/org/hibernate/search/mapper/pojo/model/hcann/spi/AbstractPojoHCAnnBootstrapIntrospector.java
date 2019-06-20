/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.common.impl.StreamHelper;

public abstract class AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final ReflectionManager reflectionManager;
	private final AnnotationHelper annotationHelper;

	public AbstractPojoHCAnnBootstrapIntrospector(ReflectionManager reflectionManager, AnnotationHelper annotationHelper) {
		this.reflectionManager = reflectionManager;
		this.annotationHelper = annotationHelper;
	}

	public <A extends Annotation> Optional<A> getAnnotationByType(XAnnotatedElement xAnnotated, Class<A> annotationType) {
		return Optional.ofNullable( xAnnotated.getAnnotation( annotationType ) );
	}

	public <A extends Annotation> Stream<A> getAnnotationsByType(XAnnotatedElement xAnnotated, Class<A> annotationType) {
		return Arrays.stream( xAnnotated.getAnnotations() )
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.filter( annotation -> annotationType.isAssignableFrom( annotation.annotationType() ) )
				.map( annotationType::cast );
	}

	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(
			XAnnotatedElement xAnnotated, Class<? extends Annotation> metaAnnotationType) {
		return Arrays.stream( xAnnotated.getAnnotations() )
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.filter( annotation -> annotationHelper.isMetaAnnotated( annotation, metaAnnotationType ) );
	}

	public XClass toXClass(Class<?> type) {
		return reflectionManager.toXClass( type );
	}

	public Map<String, XProperty> getDeclaredFieldAccessXPropertiesByName(XClass xClass) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return xClass.getDeclaredProperties( XClass.ACCESS_FIELD ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	public Map<String, XProperty> getDeclaredMethodAccessXPropertiesByName(XClass xClass) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return xClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	protected <T> Stream<? extends Class<T>> getAscendingSuperClasses(XClass xClass) {
		return PojoXClassOrdering.get().getAscendingSuperTypes( xClass ).map( this::toClass );
	}

	protected <T> Stream<? extends Class<T>> getDescendingSuperClasses(XClass xClass) {
		return PojoXClassOrdering.get().getDescendingSuperTypes( xClass ).map( this::toClass );
	}

	private <T> Class<T> toClass(XClass xClass) {
		return reflectionManager.toClass( xClass );
	}

	private Collector<XProperty, ?, Map<String, XProperty>> xPropertiesByNameNoDuplicate() {
		return StreamHelper.toMap(
				XProperty::getName, Function.identity(),
				TreeMap::new // Sort properties by name for deterministic iteration
		);
	}
}
