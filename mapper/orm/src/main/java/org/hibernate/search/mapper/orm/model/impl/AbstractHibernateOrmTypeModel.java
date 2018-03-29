/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

abstract class AbstractHibernateOrmTypeModel<T> implements PojoRawTypeModel<T> {

	final HibernateOrmBootstrapIntrospector introspector;
	private final XClass xClass;
	private final Class<T> clazz;
	private final RawTypeDeclaringContext<T> rawTypeDeclaringContext;

	/**
	 * Note: the main purpose of this cache is not to improve performance,
	 * but to ensure the unicity of {@link PojoPropertyModel}s for two reasons.
	 * <p>
	 * First, having unique {@link PojoPropertyModel} lowers the risk of generating duplicate
	 * {@link org.hibernate.search.mapper.pojo.model.spi.PropertyHandle}s,
	 * which lowers the risk of fetching a property's value multiple times.
	 * We could also solve this issue by implementing {@link GetterPropertyHandle#hashCode()} properly,
	 * but this is not possible because of {@link org.hibernate.property.access.spi.Getter} implementations
	 * that do not implement equals/hashCode.
	 * <p>
	 * Second, if property models are unique, they can be used as cache keys in
	 * {@link HibernateOrmGenericContextHelper#getPropertyCacheKey(PojoPropertyModel)}.
	 */
	private final Map<String, PojoPropertyModel<?>> propertyModelCache = new HashMap<>();

	private Map<String, XProperty> fieldAccessXPropertiesByName;
	private Map<String, XProperty> methodAccessXPropertiesByName;

	AbstractHibernateOrmTypeModel(HibernateOrmBootstrapIntrospector introspector, Class<T> clazz,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		this.introspector = introspector;
		this.xClass = introspector.toXClass( clazz );
		this.clazz = clazz;
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractHibernateOrmTypeModel<?> that = (AbstractHibernateOrmTypeModel<?>) o;
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
	public boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof AbstractHibernateOrmTypeModel
				&& ((AbstractHibernateOrmTypeModel<?>) other).xClass.isAssignableFrom( xClass );
	}

	@Override
	public PojoRawTypeModel<? super T> getRawType() {
		return this;
	}

	@Override
	public <U> Optional<PojoRawTypeModel<U>> getSuperType(Class<U> superClassCandidate) {
		XClass superClassCandidateXClass = introspector.toXClass( superClassCandidate );
		return superClassCandidateXClass.isAssignableFrom( xClass )
				? Optional.of( introspector.getTypeModel( superClassCandidate ) )
				: Optional.empty();
	}

	@Override
	public Stream<PojoRawTypeModel<? super T>> getAscendingSuperTypes() {
		return introspector.getAscendingSuperTypes( xClass );
	}

	@Override
	public Stream<PojoRawTypeModel<? super T>> getDescendingSuperTypes() {
		return introspector.getDescendingSuperTypes( xClass );
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
	public final PojoPropertyModel<?> getProperty(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		return Stream.concat(
						getFieldAccessXPropertiesByName().keySet().stream(),
						getMethodAccessXPropertiesByName().keySet().stream()
				)
				.distinct()
				.map( propertyName -> {
					try {
						return getProperty( propertyName );
					}
					catch (PropertyNotFoundException e) {
						// Error resolving the property through Hibernate internals
						// Ignore this property
						return (PojoPropertyModel<?>) null;
					}
					catch (IllegalArgumentException e) {
						// Error resolving the property through the JPA metamodel
						// Ignore this property
						return null;
					}
				} )
				.filter( Objects::nonNull );
	}

	@Override
	public PojoCaster<T> getCaster() {
		return clazz::cast;
	}

	@Override
	public final Class<T> getJavaClass() {
		return clazz;
	}

	RawTypeDeclaringContext<T> getRawTypeDeclaringContext() {
		return rawTypeDeclaringContext;
	}

	abstract PojoPropertyModel<?> createPropertyModel(String propertyName, List<XProperty> declaredXProperties);

	private Map<String, XProperty> getFieldAccessXPropertiesByName() {
		if ( fieldAccessXPropertiesByName == null ) {
			fieldAccessXPropertiesByName = introspector.getFieldAccessPropertiesByName( xClass );
		}
		return fieldAccessXPropertiesByName;
	}

	private Map<String, XProperty> getMethodAccessXPropertiesByName() {
		if ( methodAccessXPropertiesByName == null ) {
			methodAccessXPropertiesByName = introspector.getMethodAccessPropertiesByName( xClass );
		}
		return methodAccessXPropertiesByName;
	}

	private PojoPropertyModel<?> createPropertyModel(String propertyName) {
		List<XProperty> declaredXProperties = new ArrayList<>( 2 );
		XProperty fieldAccessXProperty = getFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			declaredXProperties.add( fieldAccessXProperty );
		}
		XProperty methodAccessXProperty = getMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperty != null ) {
			declaredXProperties.add( methodAccessXProperty );
		}
		return createPropertyModel( propertyName, declaredXProperties );
	}


}
