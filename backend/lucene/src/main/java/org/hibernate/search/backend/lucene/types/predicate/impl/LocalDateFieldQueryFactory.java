/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Objects;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.types.formatter.impl.LocalDateFieldFormatter;

public final class LocalDateFieldQueryFactory implements LuceneFieldQueryFactory {

	private final LocalDateFieldFormatter formatter;

	public LocalDateFieldQueryFactory(LocalDateFieldFormatter formatter) {
		this.formatter = formatter;
	}

	@Override
	public LocalDateMatchPredicateBuilder createMatchPredicateBuilder(String absoluteFieldPath) {
		return new LocalDateMatchPredicateBuilder( absoluteFieldPath, formatter );
	}

	@Override
	public LocalDateRangePredicateBuilder createRangePredicateBuilder(String absoluteFieldPath) {
		return new LocalDateRangePredicateBuilder( absoluteFieldPath, formatter );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( LocalDateFieldQueryFactory.class != obj.getClass() ) {
			return false;
		}

		LocalDateFieldQueryFactory other = (LocalDateFieldQueryFactory) obj;

		return Objects.equals( formatter, other.formatter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( formatter );
	}
}
