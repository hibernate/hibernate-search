/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;



final class NoProxyPojoRuntimeIntrospector implements PojoRuntimeIntrospector {

	private static final NoProxyPojoRuntimeIntrospector INSTANCE = new NoProxyPojoRuntimeIntrospector();

	public static NoProxyPojoRuntimeIntrospector get() {
		return INSTANCE;
	}

	private NoProxyPojoRuntimeIntrospector() {
	}

	@Override
	@SuppressWarnings("unchecked") // The class of an object of type T is always a Class<? extends T>
	public <T> Class<? extends T> getClass(T entity) {
		return entity == null ? null : (Class<? extends T>) entity.getClass();
	}

	@Override
	public Object unproxy(Object value) {
		return value;
	}

}
