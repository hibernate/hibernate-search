/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

class ComposedPropertyAccessor<R, V, U> implements PropertyAccessor<R, U> {

	private final PropertyAccessor<R, V> first;
	private final Supplier<V> firstDefaultInstanceSupplier;
	private final PropertyAccessor<? super V, U> second;

	ComposedPropertyAccessor(PropertyAccessor<R, V> first, Supplier<V> firstDefaultInstanceSupplier,
			PropertyAccessor<? super V, U> second) {
		this.first = first;
		this.firstDefaultInstanceSupplier = firstDefaultInstanceSupplier;
		this.second = second;
	}

	@Override
	public String toString() {
		return first + " then " + second;
	}

	protected V getFirstOrNull(R root) {
		return first.get( root );
	}

	protected V getFirstOrCreate(R root) {
		V firstInstance = first.get( root );
		if ( firstInstance == null ) {
			firstInstance = firstDefaultInstanceSupplier.get();
			first.set( root, firstInstance );
		}
		return firstInstance;
	}

	protected V getFirstOrFail(R root) {
		V firstInstance = first.get( root );
		if ( firstInstance == null ) {
			throw new NoSuchElementException( first + " is null for " + root );
		}
		return firstInstance;
	}

	@Override
	public void set(R root, U value) {
		V firstInstance = getFirstOrCreate( root );
		second.set( firstInstance, value );
	}

	@Override
	public U get(R root) {
		V firstInstance = getFirstOrNull( root );
		if ( firstInstance == null ) {
			return null;
		}
		return second.get( firstInstance );
	}

	@Override
	public void clear(R root) {
		second.clear( getFirstOrFail( root ) );
	}

}
