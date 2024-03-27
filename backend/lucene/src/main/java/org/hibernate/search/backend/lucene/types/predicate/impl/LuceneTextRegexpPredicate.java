/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RegexpQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.util.automaton.RegExp;

public class LuceneTextRegexpPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextRegexpPredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F>
			extends AbstractLuceneValueFieldSearchQueryElementFactory<RegexpPredicateBuilder, F> {
		@Override
		public Builder<F> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements RegexpPredicateBuilder {
		private String pattern;
		private int flags = RegExp.NONE;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
		}

		@Override
		public void pattern(String regexpPattern) {
			this.pattern = regexpPattern;
		}

		@Override
		public void flags(Set<RegexpQueryFlag> flags) {
			this.flags = toFlagsMask( flags );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextRegexpPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			// set no optional flag as default
			return new RegexpQuery( new Term( absoluteFieldPath, pattern ), flags );
		}
	}

	private static int toFlagsMask(Set<RegexpQueryFlag> flags) {
		int flag = 0;
		if ( flags == null || flags.isEmpty() ) {
			return RegExp.NONE;
		}

		for ( RegexpQueryFlag operation : flags ) {
			switch ( operation ) {
				case INTERVAL:
					flag |= RegExp.INTERVAL;
					break;
				case INTERSECTION:
					flag |= RegExp.INTERSECTION;
					break;
				case ANY_STRING:
					flag |= RegExp.ANYSTRING;
					break;
			}
		}
		return flag;
	}
}
