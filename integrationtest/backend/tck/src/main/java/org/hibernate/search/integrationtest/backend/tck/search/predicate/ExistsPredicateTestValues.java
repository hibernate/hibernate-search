/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public final class ExistsPredicateTestValues<F> extends AbstractPredicateTestValues<F> {
	private final F value;

	public ExistsPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
		this.value = fieldType.getUniquelyMatchableValues().get( 0 );
	}

	public F value() {
		return value;
	}

	@Override
	public F fieldValue(int docOrdinal) {
		if ( docOrdinal != 0 ) {
			throw new IllegalArgumentException( "The basic tests of the 'exists' predicate can only index one document"
					+ " with a value, otherwise we wouldn't be able to distinguish between the indexed documents." );
		}
		return value;
	}

	@Override
	public int size() {
		return 1;
	}
}
