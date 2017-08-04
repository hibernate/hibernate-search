/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;


/**
 * @author Yoann Rodiere
 */
public class HibernateOrmProxyIntrospector implements PojoProxyIntrospector {

	private static final HibernateOrmProxyIntrospector INSTANCE = new HibernateOrmProxyIntrospector();

	public static HibernateOrmProxyIntrospector get() {
		return INSTANCE;
	}

	private HibernateOrmProxyIntrospector() {
	}

	@Override
	public <T> Class<? extends T> getClass(T entity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Object unproxy(Object value) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

}
