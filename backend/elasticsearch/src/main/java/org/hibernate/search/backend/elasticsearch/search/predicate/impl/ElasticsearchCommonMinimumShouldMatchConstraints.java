/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.search.predicate.spi.MinimumShouldMatchBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ElasticsearchCommonMinimumShouldMatchConstraints implements MinimumShouldMatchBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Map<Integer, CommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;

	@Override
	public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
		addMinimumShouldMatchConstraint(
				ignoreConstraintCeiling,
				new CommonMinimumShouldMatchConstraint( matchingClausesNumber, null )
		);
	}

	@Override
	public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
		addMinimumShouldMatchConstraint(
				ignoreConstraintCeiling,
				new CommonMinimumShouldMatchConstraint( null, matchingClausesPercent )
		);
	}

	private void addMinimumShouldMatchConstraint(int ignoreConstraintCeiling,
			CommonMinimumShouldMatchConstraint constraint) {
		if ( minimumShouldMatchConstraints == null ) {
			// We'll need to go through the data in ascending order, so use a TreeMap
			minimumShouldMatchConstraints = new TreeMap<>();
		}
		Object previous = minimumShouldMatchConstraints.put( ignoreConstraintCeiling, constraint );
		if ( previous != null ) {
			throw log.minimumShouldMatchConflictingConstraints( ignoreConstraintCeiling );
		}
	}

	public String formatMinimumShouldMatchConstraints() {
		StringBuilder builder = new StringBuilder();
		Iterator<Map.Entry<Integer, CommonMinimumShouldMatchConstraint>> iterator =
				minimumShouldMatchConstraints.entrySet().iterator();

		// Process the first constraint differently
		Map.Entry<Integer, CommonMinimumShouldMatchConstraint> entry = iterator.next();
		Integer ignoreConstraintCeiling = entry.getKey();
		CommonMinimumShouldMatchConstraint constraint = entry.getValue();
		if ( ignoreConstraintCeiling.equals( 0 ) && minimumShouldMatchConstraints.size() == 1 ) {
			// Special case: if there's only one constraint and its ignore ceiling is 0, do not mention the ceiling
			constraint.appendTo( builder, null );
			return builder.toString();
		}
		else {
			entry.getValue().appendTo( builder, ignoreConstraintCeiling );
		}

		// Process the other constraints normally
		while ( iterator.hasNext() ) {
			entry = iterator.next();
			ignoreConstraintCeiling = entry.getKey();
			constraint = entry.getValue();
			builder.append( ' ' );
			constraint.appendTo( builder, ignoreConstraintCeiling );
		}

		return builder.toString();
	}

	public boolean isEmpty() {
		return minimumShouldMatchConstraints == null;
	}

	private static final class CommonMinimumShouldMatchConstraint {
		private final Integer matchingClausesNumber;
		private final Integer matchingClausesPercent;

		CommonMinimumShouldMatchConstraint(Integer matchingClausesNumber, Integer matchingClausesPercent) {
			this.matchingClausesNumber = matchingClausesNumber;
			this.matchingClausesPercent = matchingClausesPercent;
		}

		/**
		 * Format the constraint according to
		 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-minimum-should-match.html">
		 * the format specified in the Elasticsearch documentation
		 * </a>.
		 *
		 * @param builder The builder to append the formatted value to.
		 * @param ignoreConstraintCeiling The ceiling above which this constraint is no longer ignored.
		 */
		void appendTo(StringBuilder builder, Integer ignoreConstraintCeiling) {
			if ( ignoreConstraintCeiling != null ) {
				builder.append( ignoreConstraintCeiling ).append( '<' );
			}
			if ( matchingClausesNumber != null ) {
				builder.append( matchingClausesNumber );
			}
			else {
				builder.append( matchingClausesPercent ).append( '%' );
			}
		}
	}
}
