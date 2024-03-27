/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Iterator;
import java.util.Map;

final class ElasticsearchCommonMinimumShouldMatchConstraint {
	private final Integer matchingClausesNumber;
	private final Integer matchingClausesPercent;

	ElasticsearchCommonMinimumShouldMatchConstraint(Integer matchingClausesNumber, Integer matchingClausesPercent) {
		this.matchingClausesNumber = matchingClausesNumber;
		this.matchingClausesPercent = matchingClausesPercent;
	}

	static String formatMinimumShouldMatchConstraints(
			Map<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints) {
		StringBuilder builder = new StringBuilder();
		Iterator<Map.Entry<Integer, ElasticsearchCommonMinimumShouldMatchConstraint>> iterator =
				minimumShouldMatchConstraints.entrySet().iterator();

		// Process the first constraint differently
		Map.Entry<Integer, ElasticsearchCommonMinimumShouldMatchConstraint> entry = iterator.next();
		Integer ignoreConstraintCeiling = entry.getKey();
		ElasticsearchCommonMinimumShouldMatchConstraint constraint = entry.getValue();
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
