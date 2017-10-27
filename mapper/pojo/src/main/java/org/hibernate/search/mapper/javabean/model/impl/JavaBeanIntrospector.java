/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.IntrospectionException;

import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
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
	public <T> TypeModel<T> getEntityTypeModel(Class<T> type) {
		try {
			return new JavaBeanTypeModel<>( type );
		}
		catch (IntrospectionException | RuntimeException e) {
			throw new SearchException( "Exception while retrieving the type model for '" + type + "'", e );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The class of an object of type T is always a Class<? extends T>
	public <T> Class<? extends T> getClass(T entity) {
		return entity == null ? null : (Class<? extends T>) entity.getClass();
	}
}
