/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Objects;

import org.apache.lucene.util.QueryBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.StringFieldConverter;

public class StringFieldPredicateBuilderFactory extends AbstractStandardLuceneFieldPredicateBuilderFactory {

	private final StringFieldConverter converter;

	private final boolean tokenized;

	private final QueryBuilder queryBuilder;

	public StringFieldPredicateBuilderFactory(StringFieldConverter converter, boolean tokenized, QueryBuilder queryBuilder) {
		this.converter = converter;
		this.tokenized = tokenized;
		this.queryBuilder = queryBuilder;
	}

	@Override
	public StringMatchPredicateBuilder createMatchPredicateBuilder(String absoluteFieldPath) {
		return new StringMatchPredicateBuilder( absoluteFieldPath, converter, queryBuilder );
	}

	@Override
	public StringRangePredicateBuilder createRangePredicateBuilder(String absoluteFieldPath) {
		return new StringRangePredicateBuilder( absoluteFieldPath, converter );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( StringFieldPredicateBuilderFactory.class != obj.getClass() ) {
			return false;
		}

		StringFieldPredicateBuilderFactory other = (StringFieldPredicateBuilderFactory) obj;

		return Objects.equals( converter, other.converter ) &&
				tokenized == other.tokenized &&
				Objects.equals( queryBuilder, other.queryBuilder );
	}

	@Override
	public int hashCode() {
		return Objects.hash( converter, tokenized, queryBuilder );
	}
}
