/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public abstract class FieldTypeDescriptor<F> {

	private static List<FieldTypeDescriptor<?>> all;

	public static List<FieldTypeDescriptor<?>> getAll() {
		if ( all == null ) {
			List<FieldTypeDescriptor<?>> list = new ArrayList<>();
			Collections.addAll(
					list,
					KeywordStringFieldTypeDescriptor.INSTANCE,
					AnalyzedStringFieldTypeDescriptor.INSTANCE,
					NormalizedStringFieldTypeDescriptor.INSTANCE,
					IntegerFieldTypeDescriptor.INSTANCE,
					FloatFieldTypeDescriptor.INSTANCE,
					LongFieldTypeDescriptor.INSTANCE,
					BooleanFieldTypeDescriptor.INSTANCE,
					ByteFieldTypeDescriptor.INSTANCE,
					ShortFieldTypeDescriptor.INSTANCE,
					DoubleFieldTypeDescriptor.INSTANCE,
					InstantFieldTypeDescriptor.INSTANCE,
					LocalDateFieldTypeDescriptor.INSTANCE,
					LocalDateTimeFieldTypeDescriptor.INSTANCE,
					LocalTimeFieldTypeDescriptor.INSTANCE,
					ZonedDateTimeFieldTypeDescriptor.INSTANCE,
					YearMonthFieldTypeDescriptor.INSTANCE,
					MonthDayFieldTypeDescriptor.INSTANCE,
					OffsetDateTimeFieldTypeDescriptor.INSTANCE,
					OffsetTimeFieldTypeDescriptor.INSTANCE,
					GeoPointFieldTypeDescriptor.INSTANCE,
					BigDecimalFieldTypeDescriptor.INSTANCE,
					BigIntegerFieldTypeDescriptor.INSTANCE
			);
			if ( TckConfiguration.get().getBackendFeatures().supportsYearType() ) {
				list.add( YearFieldTypeDescriptor.INSTANCE );
			}
			all = Collections.unmodifiableList( list );
		}
		return all;
	}

	public static FieldTypeDescriptor<?> getIncompatible(FieldTypeDescriptor<?> typeDescriptor) {
		if ( IntegerFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
			return LongFieldTypeDescriptor.INSTANCE;
		}
		else {
			return IntegerFieldTypeDescriptor.INSTANCE;
		}
	}

	private final Class<F> javaType;
	private final String uniqueName;

	private final AscendingUniqueTermValues<F> ascendingUniqueTermValues = createAscendingUniqueTermValues();

	private final IndexableValues<F> indexableValues = createIndexableValues();

	private final List<F> uniquelyMatchableValues = Collections.unmodifiableList( createUniquelyMatchableValues() );

	private final List<F> nonMatchingValues = Collections.unmodifiableList( createNonMatchingValues() );

	protected FieldTypeDescriptor(Class<F> javaType) {
		this( javaType, javaType.getSimpleName() );
	}

	protected FieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		this.javaType = javaType;
		this.uniqueName = uniqueName;
	}

	@Override
	public String toString() {
		return getUniqueName();
	}

	public final Class<F> getJavaType() {
		return javaType;
	}

	public final String getUniqueName() {
		return uniqueName;
	}

	public StandardIndexFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.as( javaType );
	}

	/**
	 * @param indexed The value that was indexed.
	 * @return The value that will returned by the backend,
	 * which could be different due to normalization.
	 * In particular, date/time types with an offset/zone will be normalized to UTC and lose the offset/zone information.
	 */
	public F toExpectedDocValue(F indexed) {
		return indexed;
	}

	/**
	 * @return A sequence of "term" (single-token) values, in strictly ascending order,
	 * that are guaranteed to have a unique indexed value (after analysis/normalization).
	 * @throws UnsupportedOperationException If value lookup is not supported for this field type
	 * (hence this method should never be called).
	 */
	public final AscendingUniqueTermValues<F> getAscendingUniqueTermValues() {
		if ( ascendingUniqueTermValues == null ) {
			throw new UnsupportedOperationException( "Value lookup isn't supported for " + this + "." );
		}
		return ascendingUniqueTermValues;
	}

	protected abstract AscendingUniqueTermValues<F> createAscendingUniqueTermValues();

	/**
	 * @return A set of indexables values, not necessarily unique.
	 */
	public final IndexableValues<F> getIndexableValues() {
		return indexableValues;
	}

	protected abstract IndexableValues<F> createIndexableValues();

	/**
	 * @return A set of values that can be uniquely matched using predicates.
	 * This excludes empty strings in particular.
	 * This also means distinct values for analyzed/normalized text cannot share the same token.
	 */
	public final List<F> getUniquelyMatchableValues() {
		return uniquelyMatchableValues;
	}

	protected abstract List<F> createUniquelyMatchableValues();

	public final List<F> getNonMatchingValues() {
		return nonMatchingValues;
	}

	protected abstract List<F> createNonMatchingValues();

	public abstract F valueFromInteger(int integer);

	public boolean isFieldSortSupported() {
		// Assume supported by default: this way, we'll get test failures if we forget to override this method.
		return true;
	}

	public abstract Optional<IndexNullAsMatchPredicateExpectactions<F>> getIndexNullAsMatchPredicateExpectations();
}
