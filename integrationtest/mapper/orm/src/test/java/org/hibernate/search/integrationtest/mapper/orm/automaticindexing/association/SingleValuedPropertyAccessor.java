/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import java.util.function.BiConsumer;

public final class SingleValuedPropertyAccessor<R, V> implements PropertyAccessor<R, V> {

	private final BiConsumer<R, V> setMethod;

	public SingleValuedPropertyAccessor(BiConsumer<R, V> setMethod) {
		this.setMethod = setMethod;
	}

	@Override
	public void set(R root, V value) {
		setMethod.accept( root, value );
	}

	@Override
	public void clear(R root) {
		set( root, null );
	}

}
