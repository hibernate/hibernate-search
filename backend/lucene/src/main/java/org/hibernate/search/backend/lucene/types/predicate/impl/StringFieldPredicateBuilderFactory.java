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

public final class StringFieldPredicateBuilderFactory
		extends AbstractStandardLuceneFieldPredicateBuilderFactory<StringFieldConverter> {

	private final boolean tokenized;

	private final QueryBuilder queryBuilder;

	public StringFieldPredicateBuilderFactory(StringFieldConverter converter, boolean tokenized, QueryBuilder queryBuilder) {
		super( converter );
		this.tokenized = tokenized;
		this.queryBuilder = queryBuilder;
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldPredicateBuilderFactory other) {
		if ( !super.isDslCompatibleWith( other ) ) {
			return false;
		}
		StringFieldPredicateBuilderFactory castedOther = (StringFieldPredicateBuilderFactory) other;
		return tokenized == castedOther.tokenized
				&& Objects.equals( queryBuilder, castedOther.queryBuilder );
	}

	@Override
	public StringMatchPredicateBuilder createMatchPredicateBuilder(String absoluteFieldPath) {
		return new StringMatchPredicateBuilder( absoluteFieldPath, converter, queryBuilder );
	}

	@Override
	public StringRangePredicateBuilder createRangePredicateBuilder(String absoluteFieldPath) {
		return new StringRangePredicateBuilder( absoluteFieldPath, converter );
	}
}
