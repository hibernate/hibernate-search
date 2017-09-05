/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;

/**
 * A very simple introspector roughly following Java Beans conventions.
 * <p>
 * As per JavaBeans conventions, only public getters are supported, and field access is not.
 *
 * @author Yoann Rodiere
 */
public class JavaBeanIntrospector implements PojoIntrospector {

	private static final JavaBeanIntrospector INSTANCE = new JavaBeanIntrospector();

	public static JavaBeanIntrospector get() {
		return INSTANCE;
	}

	private JavaBeanIntrospector() {
		// Private constructor, use get() instead
	}

	@Override
	public PropertyHandle findReadableProperty(Class<?> holderType, String name) {
		try {
			String normalizedName = Introspector.decapitalize( name );
			PropertyDescriptor propertyDescriptor = getPropertyDescriptor( holderType, normalizedName );
			return new JavaBeanPropertyHandle( normalizedName, propertyDescriptor.getReadMethod() );
		}
		catch (IntrospectionException | IllegalAccessException | RuntimeException e) {
			throw new SearchException( "Exception while retrieving property reference for '"
					+ name + "' on '" + holderType + "'", e );
		}
	}

	private PropertyDescriptor getPropertyDescriptor(Class<?> holderType, String propertyName) throws IntrospectionException {
		PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo( holderType ).getPropertyDescriptors();
		for ( PropertyDescriptor descriptor : propertyDescriptors ) {
			if ( propertyName.equals( descriptor.getName() ) ) {
				return descriptor;
			}
		}
		throw new SearchException( "JavaBean property '" + propertyName + "' not found in '" + holderType + "'" );
	}

}
