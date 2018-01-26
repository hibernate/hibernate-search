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
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;

abstract class AbstractHibernateOrmTypeModel<T> implements TypeModel<T> {

	protected final HibernateOrmIntrospector introspector;
	protected final Class<T> type;
	protected final XClass xClass;

	/**
	 * Note: the main purpose of this cache is not to improve performance,
	 * but to ensure the unicity of {@link PropertyModel}s,
	 * which lowers the risk of generating duplicate {@link org.hibernate.search.mapper.pojo.model.spi.PropertyHandle}s.
	 * <p>
	 * This is necessary because we cannot easily implement {@link GetterPropertyHandle#hashCode()} properly,
	 * because of {@link org.hibernate.property.access.spi.Getter} implementations that do not implement equals.
	 */
	private final Map<String, PropertyModel<?>> propertyModelCache = new HashMap<>();

	private Map<String, XProperty> fieldAccessXPropertiesByName;
	private Map<String, XProperty> methodAccessXPropertiesByName;

	protected AbstractHibernateOrmTypeModel(HibernateOrmIntrospector introspector, Class<T> type) {
		this.introspector = introspector;
		this.type = type;
		this.xClass = introspector.toXClass( type );
	}

	@Override
	public final Class<T> getJavaType() {
		return type;
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
	public final PropertyModel<?> getProperty(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

	@Override
	public Stream<PropertyModel<?>> getDeclaredProperties() {
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
						return (PropertyModel<?>) null;
					}
				} )
				.filter( Objects::nonNull );
	}

	XClass getXClass() {
		return xClass;
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

	private PropertyModel<?> createPropertyModel(String propertyName) {
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

	abstract PropertyModel<?> createPropertyModel(String propertyName, List<XProperty> xProperties);
}
