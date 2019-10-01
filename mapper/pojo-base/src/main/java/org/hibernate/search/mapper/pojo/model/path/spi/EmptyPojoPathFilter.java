/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.path.spi;

final class EmptyPojoPathFilter implements PojoPathFilter<Object> {

	private static final EmptyPojoPathFilter INSTANCE = new EmptyPojoPathFilter();

	@SuppressWarnings( "unchecked" ) // This instance works for any S
	public static <S> PojoPathFilter<S> get() {
		return (PojoPathFilter<S>) INSTANCE;
	}

	@Override
	public boolean test(Object paths) {
		return false;
	}
}
