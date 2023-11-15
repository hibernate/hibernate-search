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

	private final FieldTypeDescriptor<F, ?> fieldType;
	private final String aggregationName;
	private final List<F> mainIndexDocumentFieldValues;
	private final List<F> compatibleIndexDocumentFieldValues;
	private final List<List<F>> multiValuedIndexDocumentFieldValues;

	protected SupportedSingleFieldAggregationExpectations(FieldTypeDescriptor<F, ?> fieldType, String aggregationName,
			List<F> mainIndexDocumentFieldValues,
			List<F> compatibleIndexDocumentFieldValues,
			List<List<F>> multiValuedIndexDocumentFieldValues) {
		this.fieldType = fieldType;
		this.aggregationName = aggregationName;
		this.mainIndexDocumentFieldValues = mainIndexDocumentFieldValues;
		this.compatibleIndexDocumentFieldValues = compatibleIndexDocumentFieldValues;
		this.multiValuedIndexDocumentFieldValues = multiValuedIndexDocumentFieldValues;
	}

	@Override
	public String toString() {
		return aggregationName + " on type " + fieldType.getUniqueName();
	}

	public FieldTypeDescriptor<F, ?> fieldType() {
		return fieldType;
	}

	public String aggregationName() {
		return aggregationName;
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
	 * f -> f.myAggregationType().field( fieldPath, theUnderlyingFieldType )
	 *        .someParam( valueOfUnderlyingFieldType )
	 */
	public final AggregationScenario<?> simple() {
		return withFieldType( TypeAssertionHelper.identity( fieldType ) );
	}

	/*
	 * f -> f.myAggregationType().field( fieldPath, fieldType )
	 *        .someParam( helper.create( valueOfUnderlyingFieldType ) )
	 */
	public abstract <T> AggregationScenario<?> withFieldType(TypeAssertionHelper<F, T> helper);

	/*
	 * Same as simple(), but targeting both the main index and another index,
	 * and expecting an aggregation result taking into account both indexes.
	 */
	public final AggregationScenario<?> onMainAndOtherIndex() {
		return withFieldTypeOnMainAndOtherIndex( TypeAssertionHelper.identity( fieldType ) );
	}

	/*
	 * Same as withFieldType(...), but targeting both the main index and another index,
	 * and expecting an aggregation result taking into account both indexes.
	 */
	public abstract <T> AggregationScenario<?> withFieldTypeOnMainAndOtherIndex(TypeAssertionHelper<F, T> helper);

	/*
	 * Same as simple(), but not expecting any matching document,
	 * and thus expecting the aggregation result to be empty.
	 */
	public abstract AggregationScenario<?> withoutMatch();

	/*
	 * Same as simple(), but targeting the index with multi-valued documents.
	 */
	public abstract AggregationScenario<?> onMultiValuedIndex();

}
