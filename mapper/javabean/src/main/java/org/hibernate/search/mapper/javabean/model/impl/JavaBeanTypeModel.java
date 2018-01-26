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

import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.util.SearchException;

public class JavaBeanTypeModel<T> implements TypeModel<T> {

	private final Class<T> type;
	private final BeanInfo beanInfo;

	public JavaBeanTypeModel(Class<T> type) throws IntrospectionException {
		this.type = type;
		this.beanInfo = Introspector.getBeanInfo( type );
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}

	@Override
	public PropertyModel<?> getProperty(String propertyName) {
		try {
			String normalizedName = Introspector.decapitalize( propertyName );
			PropertyDescriptor propertyDescriptor = getPropertyDescriptor( normalizedName );
			return new JavaBeanPropertyModel<>( this, propertyDescriptor.getPropertyType(), propertyDescriptor );
		}
		catch (RuntimeException e) {
			throw new SearchException( "Exception while retrieving property model for '"
					+ propertyName + "' on '" + getJavaType() + "'", e );
		}
	}

	private PropertyDescriptor getPropertyDescriptor(String normalizedName) {
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for ( PropertyDescriptor descriptor : propertyDescriptors ) {
			if ( normalizedName.equals( descriptor.getName() ) ) {
				return descriptor;
			}
		}
		throw new SearchException( "JavaBean property '" + normalizedName + "' not found in '"
				+ getJavaType() + "'" );
	}
}
