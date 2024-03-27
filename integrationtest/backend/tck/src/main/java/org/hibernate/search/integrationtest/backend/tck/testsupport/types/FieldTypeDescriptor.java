/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public abstract class FieldTypeDescriptor<F, S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>> {

	private static List<FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> all;
	private static List<StandardFieldTypeDescriptor<?>> allStandard;
	private static List<VectorFieldTypeDescriptor<?>> allVector;
	private static List<FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> allNonStandard;

	public static List<FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> getAll() {
		if ( all == null ) {
			List<FieldTypeDescriptor<?, ?>> list = new ArrayList<>();
			list.addAll( StandardFieldTypeDescriptor.getAllStandard() );
			list.addAll( VectorFieldTypeDescriptor.getAllVector() );
			all = Collections.unmodifiableList( list );
		}
		return all;
	}

	public static List<StandardFieldTypeDescriptor<?>> getAllStandard() {
		if ( allStandard == null ) {
			List<StandardFieldTypeDescriptor<?>> list = new ArrayList<>();
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
			Collections.addAll( VectorFieldTypeDescriptor.getAllVector() );
			if ( TckConfiguration.get().getBackendFeatures().supportsYearType() ) {
				list.add( YearFieldTypeDescriptor.INSTANCE );
			}
			allStandard = Collections.unmodifiableList( list );
		}
		return allStandard;
	}

	public static List<VectorFieldTypeDescriptor<?>> getAllVector() {
		if ( allVector == null ) {
			List<VectorFieldTypeDescriptor<?>> list = new ArrayList<>();
			if ( TckConfiguration.get().getBackendFeatures().supportsVectorSearch() ) {
				Collections.addAll(
						list,
						ByteVectorFieldTypeDescriptor.INSTANCE,
						FloatVectorFieldTypeDescriptor.INSTANCE
				);
			}
			allVector = Collections.unmodifiableList( list );
		}
		return allVector;
	}

	public static List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> getAllNonStandard() {
		if ( allNonStandard == null ) {
			List<FieldTypeDescriptor<?, ?>> list = new ArrayList<>( getAll() );
			list.removeAll( getAllStandard() );
			allNonStandard = Collections.unmodifiableList( list );
		}
		return allNonStandard;
	}

	public static StandardFieldTypeDescriptor<?> getIncompatible(FieldTypeDescriptor<?, ?> typeDescriptor) {
		if ( IntegerFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
			return LongFieldTypeDescriptor.INSTANCE;
		}
		else {
			return IntegerFieldTypeDescriptor.INSTANCE;
		}
	}

	protected final Class<F> javaType;
	private final String uniqueName;

	private AscendingUniqueTermValues<F> ascendingUniqueTermValues;

	private IndexableValues<F> indexableValues;

	private List<F> uniquelyMatchableValues;

	private List<F> nonMatchingValues;

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

	public abstract S configure(IndexFieldTypeFactory fieldContext);

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
			ascendingUniqueTermValues = createAscendingUniqueTermValues();
			if ( ascendingUniqueTermValues == null ) {
				throw new UnsupportedOperationException( "Value lookup isn't supported for " + this + "." );
			}
		}
		return ascendingUniqueTermValues;
	}

	protected abstract AscendingUniqueTermValues<F> createAscendingUniqueTermValues();

	/**
	 * @return A set of indexables values, not necessarily unique.
	 */
	public final IndexableValues<F> getIndexableValues() {
		if ( indexableValues == null ) {
			indexableValues = createIndexableValues();
		}
		return indexableValues;
	}

	protected abstract IndexableValues<F> createIndexableValues();

	/**
	 * @return A set of values that can be uniquely matched using predicates.
	 * This excludes empty strings in particular.
	 * This also means distinct values for analyzed/normalized text cannot share the same token.
	 */
	public final List<F> getUniquelyMatchableValues() {
		if ( uniquelyMatchableValues == null ) {
			uniquelyMatchableValues = createUniquelyMatchableValues();
		}
		return uniquelyMatchableValues;
	}

	protected abstract List<F> createUniquelyMatchableValues();

	public final List<F> getNonMatchingValues() {
		if ( nonMatchingValues == null ) {
			nonMatchingValues = createNonMatchingValues();
		}
		return nonMatchingValues;
	}

	protected abstract List<F> createNonMatchingValues();

	public abstract F valueFromInteger(int integer);

	public boolean isFieldSortSupported() {
		// Assume supported by default: this way, we'll get test failures if we forget to override this method.
		return true;
	}

	public boolean isFieldAggregationSupported() {
		// Assume supported by default: this way, we'll get test failures if we forget to override this method.
		return true;
	}

	public boolean isMultivaluable() {
		// Assume supported by default: this way, we'll get test failures if we forget to override this method.
		return true;
	}

	public abstract Optional<IndexNullAsMatchPredicateExpectactions<F>> getIndexNullAsMatchPredicateExpectations();
}
