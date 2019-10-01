/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

public final class JavaClassPojoCaster<T> implements PojoCaster<T> {
	private final Class<T> clazz;

	public JavaClassPojoCaster(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + clazz.getSimpleName() + "]";
	}

	@Override
	public T cast(Object object) {
		return clazz.cast( object );
	}

	@Override
	public T castOrNull(Object object) {
		if ( clazz.isInstance( object ) ) {
			return clazz.cast( object );
		}
		else {
			return null;
		}
	}
}
