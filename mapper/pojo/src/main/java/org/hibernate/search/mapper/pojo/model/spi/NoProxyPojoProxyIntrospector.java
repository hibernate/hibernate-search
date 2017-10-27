/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;


/**
 * @author Yoann Rodiere
 */
class NoProxyPojoProxyIntrospector implements PojoProxyIntrospector {

	private static final NoProxyPojoProxyIntrospector INSTANCE = new NoProxyPojoProxyIntrospector();

	public static NoProxyPojoProxyIntrospector get() {
		return INSTANCE;
	}

	private NoProxyPojoProxyIntrospector() {
	}

	@Override
	public Object unproxy(Object value) {
		return value;
	}

}
