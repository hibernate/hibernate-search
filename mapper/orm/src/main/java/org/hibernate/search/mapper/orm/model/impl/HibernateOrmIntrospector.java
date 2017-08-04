/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.ReadableProperty;

/**
 * @author Yoann Rodiere
 */
public class HibernateOrmIntrospector implements PojoIntrospector {

	private static final HibernateOrmIntrospector INSTANCE = new HibernateOrmIntrospector();

	public static HibernateOrmIntrospector get() {
		return INSTANCE;
	}

	private HibernateOrmIntrospector() {
	}

	@Override
	public ReadableProperty findReadableProperty(Class<?> holderType, String name) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public ReadableProperty findReadableProperty(Class<?> holderType, String name, Class<?> propertyType) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

}
