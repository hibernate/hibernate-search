/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public final class SimpleQueryStringPredicateTestValues extends AbstractPredicateTestValues<String> {
	private final List<String> values;

	public SimpleQueryStringPredicateTestValues(FieldTypeDescriptor<String, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getUniquelyMatchableValues();
	}

	@Override
	public String fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public String matchingArg(int docOrdinal) {
		// Using phrase queries, because that's the easiest way to achieve
		// simple, unique matches with the simple query string predicate.
		// Other types of queries are tested separately in SimpleQueryStringPredicateSpecificsIT.
		return "\"" + fieldValue( docOrdinal ) + "\"";
	}

	@Override
	public int size() {
		return values.size();
	}
}
