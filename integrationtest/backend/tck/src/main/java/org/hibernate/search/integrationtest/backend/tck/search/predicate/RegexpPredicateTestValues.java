/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;
import java.util.Locale;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;

public final class RegexpPredicateTestValues extends AbstractPredicateTestValues<String> {
	private final List<String> values;

	public RegexpPredicateTestValues(FieldTypeDescriptor<String> fieldType) {
		super( fieldType );
		this.values = fieldType.getAscendingUniqueTermValues().getSingle();
	}

	@Override
	public String fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public String matchingArg(int docOrdinal) {
		String valueToMatch = fieldValue( docOrdinal );
		if ( AnalyzedStringFieldTypeDescriptor.INSTANCE.equals( fieldType )
				|| NormalizedStringFieldTypeDescriptor.INSTANCE.equals( fieldType ) ) {
			valueToMatch = valueToMatch.toLowerCase( Locale.ROOT );
		}
		return valueToMatch;
	}

	@Override
	public int size() {
		return values.size();
	}
}
