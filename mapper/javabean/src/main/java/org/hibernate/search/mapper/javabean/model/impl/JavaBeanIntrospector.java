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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
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
	private final JavaBeanGenericContextHelper genericContextHelper;
	private final RawTypeDeclaringContext<?> missingRawTypeDeclaringContext;

	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	public JavaBeanIntrospector(MethodHandles.Lookup lookup) {
		this.annotationHelper = new AnnotationHelper( lookup );
		this.genericContextHelper = new JavaBeanGenericContextHelper( this );
		this.missingRawTypeDeclaringContext = new RawTypeDeclaringContext<>(
				genericContextHelper, Object.class
		);
	}

	@Override
	public <T> PojoRawTypeModel<T> getTypeModel(Class<T> clazz) {
		return (PojoRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public <T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz) {
		return missingRawTypeDeclaringContext.createGenericTypeModel( clazz );
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

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> clazz) {
		try {
			return new JavaBeanTypeModel<>(
					this, clazz,
					new RawTypeDeclaringContext<>( genericContextHelper, clazz )
			);
		}
		catch (IntrospectionException | RuntimeException e) {
			throw new SearchException( "Exception while retrieving the type model for '" + clazz + "'", e );
		}
	}
}
