/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.types.dsl.impl.IntegerIndexSchemaFieldContext;

public final class IntegerFieldPredicateBuilderFactory extends AbstractStandardLuceneFieldPredicateBuilderFactory {

	public static final IntegerFieldPredicateBuilderFactory INSTANCE = new IntegerFieldPredicateBuilderFactory();

	private IntegerFieldPredicateBuilderFactory() {
	}

	@Override
	public IntegerMatchPredicateBuilder createMatchPredicateBuilder(String absoluteFieldPath) {
		return new IntegerMatchPredicateBuilder( absoluteFieldPath, IntegerIndexSchemaFieldContext.FORMATTER );
	}

	@Override
	public IntegerRangePredicateBuilder createRangePredicateBuilder(String absoluteFieldPath) {
		return new IntegerRangePredicateBuilder( absoluteFieldPath, IntegerIndexSchemaFieldContext.FORMATTER );
	}
}
