/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search;

import java.util.List;

@SuppressWarnings({ "unchecked", "rawtypes" }) // This implementation works for any T
class IdentityObjectLoader<T> implements ObjectLoader<T, T> {

	private static final IdentityObjectLoader INSTANCE = new IdentityObjectLoader();

	public static <T> IdentityObjectLoader<T> get() {
		return INSTANCE;
	}

	@Override
	public List<T> load(List<T> references) {
		return references;
	}
}
