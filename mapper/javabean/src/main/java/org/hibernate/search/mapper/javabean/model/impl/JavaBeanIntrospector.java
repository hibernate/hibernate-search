/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.IntrospectionException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.util.spi.AnnotationHelper;
import org.hibernate.search.util.SearchException;

/**
 * A very simple introspector roughly following Java Beans conventions.
 * <p>
 * As per JavaBeans conventions, only public getters are supported, and field access is not.
 *
 * @author Yoann Rodiere
 */
public class JavaBeanIntrospector implements PojoIntrospector {

	private final AnnotationHelper annotationHelper;

	public JavaBeanIntrospector(MethodHandles.Lookup lookup) {
		this.annotationHelper = new AnnotationHelper( lookup );
	}

	@Override
	public <T> TypeModel<T> getTypeModel(Class<T> type) {
		try {
			return new JavaBeanTypeModel<>( this, type );
		}
		catch (IntrospectionException | RuntimeException e) {
			throw new SearchException( "Exception while retrieving the type model for '" + type + "'", e );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The class of an object of type T is always a Class<? extends T>
	public <T> Class<? extends T> getClass(T entity) {
		return entity == null ? null : (Class<? extends T>) entity.getClass();
	}

	<A extends Annotation> Optional<A> getAnnotationByType(AnnotatedElement annotatedElement,
			Class<A> annotationType) {
		return annotationHelper.getAnnotationByType( annotatedElement, annotationType );
	}

	<A extends Annotation> Stream<A> getAnnotationsByType(AnnotatedElement annotatedElement,
			Class<A> annotationType) {
		return annotationHelper.getAnnotationsByType( annotatedElement, annotationType );
	}

	Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(AnnotatedElement annotatedElement,
			Class<? extends Annotation> metaAnnotationType) {
		return annotationHelper.getAnnotationsByMetaAnnotationType( annotatedElement, metaAnnotationType );
	}
}
