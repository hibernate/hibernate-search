/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

import java.util.List;

@SuppressWarnings({ "unchecked", "rawtypes" }) // This implementation works for any E
class IdentityEntityLoader<E> implements EntityLoader<E, E> {

	private static final IdentityEntityLoader INSTANCE = new IdentityEntityLoader();

	public static <E> IdentityEntityLoader<E> get() {
		return INSTANCE;
	}

	@Override
	public List<E> loadBlocking(List<E> references) {
		return references;
	}
}
