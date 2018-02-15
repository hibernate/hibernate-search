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
import org.hibernate.search.engine.mapper.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoIndexableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

abstract class AbstractHibernateOrmTypeModel<T> implements PojoIndexableTypeModel<T> {

	final HibernateOrmIntrospector introspector;
	private final XClass xClass;
	private final Class<T> clazz;

	/**
	 * Note: the main purpose of this cache is not to improve performance,
	 * but to ensure the unicity of {@link PojoPropertyModel}s,
	 * which lowers the risk of generating duplicate {@link org.hibernate.search.mapper.pojo.model.spi.PropertyHandle}s.
	 * <p>
	 * This is necessary because we cannot easily implement {@link GetterPropertyHandle#hashCode()} properly,
	 * because of {@link org.hibernate.property.access.spi.Getter} implementations that do not implement equals.
	 */
	private final Map<String, PojoPropertyModel<?>> propertyModelCache = new HashMap<>();

	private Map<String, XProperty> fieldAccessXPropertiesByName;
	private Map<String, XProperty> methodAccessXPropertiesByName;

	AbstractHibernateOrmTypeModel(HibernateOrmIntrospector introspector, Class<T> clazz) {
		this.introspector = introspector;
		this.xClass = introspector.toXClass( clazz );
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + clazz.getName() + "]";
	}

	@Override
	public final Class<T> getJavaClass() {
		return clazz;
	}

	@Override
	public boolean isSubTypeOf(TypeModel other) {
		return other instanceof AbstractHibernateOrmTypeModel
				&& ((AbstractHibernateOrmTypeModel<?>) other).xClass.isAssignableFrom( xClass );
	}

	@Override
	public <U> Optional<PojoTypeModel<U>> getSuperType(Class<U> superClassCandidate) {
		XClass superClassCandidateXClass = introspector.toXClass( superClassCandidate );
		return superClassCandidateXClass.isAssignableFrom( xClass )
				? Optional.of( introspector.getTypeModel( superClassCandidate ) )
				: Optional.empty();
	}

	@Override
	public Stream<PojoTypeModel<? super T>> getAscendingSuperTypes() {
		return introspector.getAscendingSuperTypes( xClass );
	}

	@Override
	public Stream<PojoTypeModel<? super T>> getDescendingSuperTypes() {
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
						// Ignore this property
						return (PojoPropertyModel<?>) null;
					}
				} )
				.filter( Objects::nonNull );
	}

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
		List<XProperty> xProperties = new ArrayList<>( 2 );
		XProperty fieldAccessXProperty = getFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			xProperties.add( fieldAccessXProperty );
		}
		XProperty methodAccessXProperty = getMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperty != null ) {
			xProperties.add( methodAccessXProperty );
		}
		return createPropertyModel( propertyName, xProperties );
	}

	abstract PojoPropertyModel<?> createPropertyModel(String propertyName, List<XProperty> xProperties);

	@Override
	public T cast(Object instance) {
		return clazz.cast( instance );
	}


}
