/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;

public final class TermsPredicateTestValues<F> extends AbstractPredicateTestValues<F> {
	private final List<F> values;
	private final List<F> nonMatchingValues;

	public TermsPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getAscendingUniqueTermValues().getSingle();
		this.nonMatchingValues = fieldType.getNonMatchingValues();
	}

	@Override
	public F fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	@Override
	public int size() {
		return values.size();
	}

	@SuppressWarnings("unchecked") // F == String for analyzed|normalized fields
	public F matchingArg(int docOrdinal) {
		F valueToMatch = fieldValue( docOrdinal );
		if ( AnalyzedStringFieldTypeDescriptor.INSTANCE.equals( fieldType )
				|| NormalizedStringFieldTypeDescriptor.INSTANCE.equals( fieldType ) ) {
			valueToMatch = (F) ( (String) valueToMatch ).toLowerCase( Locale.ROOT );
		}
		return valueToMatch;
	}

	public F nonMatchingArg(int ordinal) {
		if ( nonMatchingValues.isEmpty() ) {
			return null;
		}

		int index = ordinal % nonMatchingValues.size();
		return nonMatchingValues.get( index );
	}

	public boolean providesNonMatchingArgs() {
		return !nonMatchingValues.isEmpty();
	}

	public int nonMatchingArgsSize() {
		return nonMatchingValues.size();
	}

	@SuppressWarnings("unchecked") // the type is preserved
	public F[] createArray(int size) {
		F[] result = (F[]) Array.newInstance( fieldType.getJavaType(), size );
		return result;
	}
}
