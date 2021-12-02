/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.data;

import java.util.Objects;

public class Pair<T0, T1> {

	private final T0 elem0;
	private final T1 elem1;

	public Pair(T0 elem0, T1 elem1) {
		this.elem0 = elem0;
		this.elem1 = elem1;
	}

	@Override
	public String toString() {
		return "Pair["
				+ "elem0=" + elem0
				+ ", elem1=" + elem1
				+ "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Pair<?, ?> other = (Pair<?, ?>) o;
		return Objects.equals( elem0, other.elem0 )
				&& Objects.equals( elem1, other.elem1 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( elem0, elem1 );
	}

	public T0 elem0() {
		return elem0;
	}

	public T1 elem1() {
		return elem1;
	}
}
