/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.javabean.log.impl.Log;
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
	private final RawTypeDeclaringContext<T> rawTypeDeclaringContext;
	private final PojoCaster<T> caster;
	private final XClass xClass;
	private final Map<String, XProperty> declaredProperties;

	JavaBeanTypeModel(JavaBeanBootstrapIntrospector introspector, Class<T> clazz, RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		this.introspector = introspector;
		this.clazz = clazz;
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
		this.caster = new JavaClassPojoCaster<>( clazz );
		this.xClass = introspector.toXClass( clazz );
		this.declaredProperties = introspector.getDeclaredMethodAccessXPropertiesByName( xClass );
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
	public Stream<JavaBeanTypeModel<? super T>> getAscendingSuperTypes() {
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
		return introspector.getAnnotationByType( xClass, annotationType );
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return introspector.getAnnotationsByType( xClass, annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return introspector.getAnnotationsByMetaAnnotationType( xClass, metaAnnotationType );
	}

	@Override
	public PojoPropertyModel<?> getProperty(String propertyName) {
		return getAscendingSuperTypes()
				.map( model -> model.declaredProperties.get( propertyName ) )
				.filter( Objects::nonNull )
				.findFirst().map( this::createProperty )
				.orElseThrow( () -> log.cannotFindProperty( this, propertyName ) );
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		return declaredProperties.values().stream()
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

	private PojoPropertyModel<?> createProperty(XProperty property) {
		return new JavaBeanPropertyModel<>( introspector, this, property );
	}
}
