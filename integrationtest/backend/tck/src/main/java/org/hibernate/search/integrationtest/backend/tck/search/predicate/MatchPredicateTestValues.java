/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public final class MatchPredicateTestValues<F> extends AbstractPredicateTestValues<F> {
	private final List<F> values;

	public MatchPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getUniquelyMatchableValues();
	}

	@Override
	public F fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public F matchingArg(int docOrdinal) {
		return fieldValue( docOrdinal );
	}

	@Override
	public int size() {
		return values.size();
	}
}
