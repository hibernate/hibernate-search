/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.Objects;

@SuppressWarnings("unused")
public final class SearchQueryElementTypeKey<T> {

	public static <T> SearchQueryElementTypeKey<T> of(String name) {
		return new SearchQueryElementTypeKey<>( name );
	}

	private final String name;

	private SearchQueryElementTypeKey(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SearchQueryElementTypeKey<?> that = (SearchQueryElementTypeKey<?>) o;
		return Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}

	public String name() {
		return name;
	}
}
