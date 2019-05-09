/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

import java.util.List;

@SuppressWarnings({ "unchecked", "rawtypes" }) // This implementation works for any T
class IdentityEntityLoader<T> implements EntityLoader<T, T> {

	private static final IdentityEntityLoader INSTANCE = new IdentityEntityLoader();

	public static <T> IdentityEntityLoader<T> get() {
		return INSTANCE;
	}

	@Override
	public List<T> loadBlocking(List<T> references) {
		return references;
	}
}
