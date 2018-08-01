/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Objects;

import org.hibernate.search.backend.lucene.types.converter.impl.LocalDateFieldConverter;

public final class LocalDateFieldPredicateBuilderFactory extends AbstractStandardLuceneFieldPredicateBuilderFactory {

	private final LocalDateFieldConverter converter;

	public LocalDateFieldPredicateBuilderFactory(LocalDateFieldConverter converter) {
		this.converter = converter;
	}

	@Override
	public LocalDateMatchPredicateBuilder createMatchPredicateBuilder(String absoluteFieldPath) {
		return new LocalDateMatchPredicateBuilder( absoluteFieldPath, converter );
	}

	@Override
	public LocalDateRangePredicateBuilder createRangePredicateBuilder(String absoluteFieldPath) {
		return new LocalDateRangePredicateBuilder( absoluteFieldPath, converter );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( LocalDateFieldPredicateBuilderFactory.class != obj.getClass() ) {
			return false;
		}

		LocalDateFieldPredicateBuilderFactory other = (LocalDateFieldPredicateBuilderFactory) obj;

		return Objects.equals( converter, other.converter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( converter );
	}
}
