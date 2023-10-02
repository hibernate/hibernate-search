/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.data;

import java.util.Objects;

public class Triplet<T0, T1, T2> {

	private final T0 elem0;
	private final T1 elem1;
	private final T2 elem2;

	public Triplet(T0 elem0, T1 elem1, T2 elem2) {
		this.elem0 = elem0;
		this.elem1 = elem1;
		this.elem2 = elem2;
	}

	@Override
	public String toString() {
		return "Pair["
				+ "elem0=" + elem0
				+ ", elem1=" + elem1
				+ ", elem2=" + elem2
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
		Triplet<?, ?, ?> other = (Triplet<?, ?, ?>) o;
		return Objects.equals( elem0, other.elem0 )
				&& Objects.equals( elem1, other.elem1 )
				&& Objects.equals( elem2, other.elem2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( elem0, elem1, elem2 );
	}

	public T0 elem0() {
		return elem0;
	}

	public T1 elem1() {
		return elem1;
	}

	public T2 elem2() {
		return elem2;
	}
}
