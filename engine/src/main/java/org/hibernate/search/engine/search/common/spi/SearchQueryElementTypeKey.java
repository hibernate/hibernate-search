/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
