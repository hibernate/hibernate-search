/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.SearchException;

class HibernateOrmPropertyModel<T> implements PojoPropertyModel<T> {

	private final HibernateOrmIntrospector introspector;
	private final PojoTypeModel<?> holderTypeModel;

	private final String name;
	private final Getter getter;
	private final List<XProperty> xProperties;

	private PropertyHandle handle;
	private PojoTypeModel<T> typeModel;

	HibernateOrmPropertyModel(HibernateOrmIntrospector introspector, PojoTypeModel<?> holderTypeModel,
			String name, List<XProperty> xProperties, Getter getter) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.getter = getter;
		this.xProperties = xProperties;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getJavaClass() {
		return getter.getReturnType();
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return xProperties.stream().flatMap(
				xProperty -> introspector.getAnnotationsByType( xProperty, annotationType )
		);
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return xProperties.stream().flatMap(
				xProperty -> introspector.getAnnotationsByMetaAnnotationType( xProperty, metaAnnotationType )
		);
	}

	@Override
	public PojoTypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = introspector.getTypeModel( getJavaClass() );
			}
			catch (RuntimeException e) {
				throw new SearchException( "Exception while retrieving property type model for '"
						+ getName() + "' on '" + holderTypeModel + "'", e );
			}
		}
		return typeModel;
	}

	@Override
	public PropertyHandle getHandle() {
		if ( handle == null ) {
			handle = new GetterPropertyHandle( name, getter );
		}
		return handle;
	}

}
