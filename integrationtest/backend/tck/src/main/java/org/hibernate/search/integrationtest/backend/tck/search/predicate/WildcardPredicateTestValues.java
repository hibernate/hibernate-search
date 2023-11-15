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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public final class WildcardPredicateTestValues extends AbstractPredicateTestValues<String> {
	private final List<String> values;

	public WildcardPredicateTestValues(FieldTypeDescriptor<String, ?> fieldType) {
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
				&& !TckConfiguration.get().getBackendFeatures()
						.normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			valueToMatch = valueToMatch.toLowerCase( Locale.ROOT );
		}
		return valueToMatch;
	}

	@Override
	public int size() {
		return values.size();
	}
}
