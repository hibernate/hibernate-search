/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations;

import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;

public abstract class SupportedSingleFieldAggregationExpectations<F> {

	private final List<F> mainIndexDocumentFieldValues;
	private final List<F> compatibleIndexDocumentFieldValues;
	private final List<List<F>> multiValuedIndexDocumentFieldValues;

	protected SupportedSingleFieldAggregationExpectations(List<F> mainIndexDocumentFieldValues,
			List<F> compatibleIndexDocumentFieldValues,
			List<List<F>> multiValuedIndexDocumentFieldValues) {
		this.mainIndexDocumentFieldValues = mainIndexDocumentFieldValues;
		this.compatibleIndexDocumentFieldValues = compatibleIndexDocumentFieldValues;
		this.multiValuedIndexDocumentFieldValues = multiValuedIndexDocumentFieldValues;
	}

	public List<F> getMainIndexDocumentFieldValues() {
		return mainIndexDocumentFieldValues;
	}

	public List<F> getOtherIndexDocumentFieldValues() {
		return compatibleIndexDocumentFieldValues;
	}

	public List<List<F>> getMultiValuedIndexDocumentFieldValues() {
		return multiValuedIndexDocumentFieldValues;
	}

	/*
	 * f -> f.myAggregationType().field( fieldPath, theActualFieldType )
	 *        .someParam( valueOfActualFieldType )
	 */
	public final AggregationScenario<?> simple(FieldTypeDescriptor<F> typeDescriptor) {
		return withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );
	}

	/*
	 * f -> f.myAggregationType().field( fieldPath, fieldType )
	 *        .someParam( fieldValueConverter.apply( valueOfActualFieldType ) )
	 */
	public abstract <T> AggregationScenario<?> withFieldType(TypeAssertionHelper<F, T> helper);

	/*
	 * Same as simple(...), but targeting both the main index and another index,
	 * and expecting an aggregation result taking into account both indexes.
	 */
	public final AggregationScenario<?> onMainAndOtherIndex(FieldTypeDescriptor<F> typeDescriptor) {
		return withFieldTypeOnMainAndOtherIndex( TypeAssertionHelper.identity( typeDescriptor ) );
	}

	/*
	 * Same as withFieldType(...), but targeting both the main index and another index,
	 * and expecting an aggregation result taking into account both indexes.
	 */
	public abstract <T> AggregationScenario<?> withFieldTypeOnMainAndOtherIndex(TypeAssertionHelper<F, T> helper);

	/*
	 * Same as simple(...), but not expecting any matching document,
	 * and thus expecting the aggregation result to be empty.
	 */
	public abstract AggregationScenario<?> withoutMatch(FieldTypeDescriptor<F> typeDescriptor);

	/*
	 * Same as simple(...), but targeting the index with multi-valued documents.
	 */
	public abstract AggregationScenario<?> onMultiValuedIndex(FieldTypeDescriptor<F> typeDescriptor);

}
