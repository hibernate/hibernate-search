/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.JavaClassPojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.util.spi.JavaClassOrdering;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class JavaBeanTypeModel<T> implements PojoRawTypeModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanBootstrapIntrospector introspector;
	private final Class<T> clazz;
	private final BeanInfo beanInfo;
	private final BeanInfo declaredBeanInfo;
	private final RawTypeDeclaringContext<T> rawTypeDeclaringContext;
	private final PojoCaster<T> caster;

	JavaBeanTypeModel(JavaBeanBootstrapIntrospector introspector, Class<T> clazz,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) throws IntrospectionException {
		this.introspector = introspector;
		this.clazz = clazz;
		this.beanInfo = Introspector.getBeanInfo( clazz );
		this.declaredBeanInfo = Introspector.getBeanInfo( clazz, clazz.getSuperclass() );
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
		this.caster = new JavaClassPojoCaster<>( clazz );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JavaBeanTypeModel<?> that = (JavaBeanTypeModel<?>) o;
		return Objects.equals( introspector, that.introspector ) &&
				Objects.equals( clazz, that.clazz );
	}

	@Override
	public int hashCode() {
		return Objects.hash( introspector, clazz );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + clazz.getName() + "]";
	}

	@Override
	public String getName() {
		return clazz.getName();
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract( clazz.getModifiers() );
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof JavaBeanTypeModel
				&& ( (JavaBeanTypeModel<?>) other ).clazz.isAssignableFrom( clazz );
	}

	@Override
	public PojoRawTypeModel<? super T> getRawType() {
		return this;
	}

	@Override
	public boolean isSubTypeOf(Class<?> superClassCandidate) {
		return superClassCandidate.isAssignableFrom( clazz );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public Stream<? extends PojoRawTypeModel<? super T>> getAscendingSuperTypes() {
		return JavaClassOrdering.get().getAscendingSuperTypes( clazz )
				.map( clazz -> introspector.getTypeModel( (Class<? super T>) clazz ) );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public Stream<? extends PojoRawTypeModel<? super T>> getDescendingSuperTypes() {
		return JavaClassOrdering.get().getDescendingSuperTypes( clazz )
				.map( clazz -> introspector.getTypeModel( (Class<? super T>) clazz ) );
	}

	@Override
	public <A extends Annotation> Optional<A> getAnnotationByType(Class<A> annotationType) {
		return introspector.getAnnotationByType( clazz, annotationType );
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return introspector.getAnnotationsByType( clazz, annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return introspector.getAnnotationsByMetaAnnotationType( clazz, metaAnnotationType );
	}

	@Override
	public PojoPropertyModel<?> getProperty(String propertyName) {
		String normalizedName = Introspector.decapitalize( propertyName );
		// The methods below may throw SearchExceptions, which include all the necessary context in their message
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor( normalizedName );
		return createProperty( propertyDescriptor );
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		return Arrays.stream( declaredBeanInfo.getPropertyDescriptors() )
				.filter( descriptor -> descriptor.getReadMethod() != null )
				.map( this::createProperty );
	}

	@Override
	public PojoCaster<T> getCaster() {
		return caster;
	}

	@Override
	public Class<T> getJavaClass() {
		return clazz;
	}

	RawTypeDeclaringContext<T> getRawTypeDeclaringContext() {
		return rawTypeDeclaringContext;
	}

	private PropertyDescriptor getPropertyDescriptor(String normalizedName) {
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for ( PropertyDescriptor descriptor : propertyDescriptors ) {
			if ( normalizedName.equals( descriptor.getName() ) ) {
				return descriptor;
			}
		}
		throw log.cannotFindProperty( this, normalizedName );
	}

	private PojoPropertyModel<?> createProperty(PropertyDescriptor propertyDescriptor) {
		if ( propertyDescriptor.getReadMethod() == null ) {
			throw log.cannotReadProperty( this, propertyDescriptor.getName() );
		}
		return new JavaBeanPropertyModel<>( introspector, this, propertyDescriptor );
	}
}
