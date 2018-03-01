/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;

class HibernateOrmPropertyModel<T> implements PojoPropertyModel<T> {

	private final HibernateOrmIntrospector introspector;
	private final AbstractHibernateOrmTypeModel<?> holderTypeModel;

	private final String name;
	private final Getter getter;
	private final List<XProperty> declaredXProperties;

	private PropertyHandle handle;
	private PojoGenericTypeModel<T> typeModel;

	HibernateOrmPropertyModel(HibernateOrmIntrospector introspector, AbstractHibernateOrmTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties, Getter getter) {
		this.introspector = introspector;
		this.holderTypeModel = holderTypeModel;
		this.name = name;
		this.getter = getter;
		this.declaredXProperties = declaredXProperties;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return declaredXProperties.stream().flatMap(
				xProperty -> introspector.getAnnotationsByType( xProperty, annotationType )
		);
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return declaredXProperties.stream().flatMap(
				xProperty -> introspector.getAnnotationsByMetaAnnotationType( xProperty, metaAnnotationType )
		);
	}

	@Override
	/*
	 * The cast is safe as long as both type parameter T and getGetterGenericReturnType
	 * match the actual type for this property.
	 */
	@SuppressWarnings( "unchecked" )
	public PojoGenericTypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = (PojoGenericTypeModel<T>) holderTypeModel.getRawTypeDeclaringContext()
						.createGenericTypeModel( getGetterGenericReturnType() );
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

	Type getGetterGenericReturnType() {
		Method method = getter.getMethod();
		Member member = getter.getMember();
		// Try to preserve generics information if possible
		if ( method != null ) {
			return method.getGenericReturnType();
		}
		else if ( member != null && member instanceof Field ) {
			return ( (Field) member ).getGenericType();
		}
		else {
			return getter.getReturnType();
		}
	}
}
