/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;

public abstract class FieldTypeDescriptor<F> {

	private static List<FieldTypeDescriptor<?>> all;

	public static List<FieldTypeDescriptor<?>> getAll() {
		if ( all == null ) {
			all = Collections.unmodifiableList( Arrays.asList(
					new KeywordStringFieldTypeDescriptor(),
					new AnalyzedStringFieldTypeDescriptor(),
					new NormalizedStringFieldTypeDescriptor(),
					new IntegerFieldTypeDescriptor(),
					new FloatFieldTypeDescriptor(),
					new LongFieldTypeDescriptor(),
					new BooleanFieldTypeDescriptor(),
					new ByteFieldTypeDescriptor(),
					new ShortFieldTypeDescriptor(),
					new DoubleFieldTypeDescriptor(),
					new InstantFieldTypeDescriptor(),
					new LocalDateFieldTypeDescriptor(),
					new LocalDateTimeFieldTypeDescriptor(),
					new LocalTimeFieldTypeDescriptor(),
					new ZonedDateTimeFieldTypeDescriptor(),
					new YearFieldTypeDescriptor(),
					new YearMonthFieldTypeDescriptor(),
					new MonthDayFieldTypeDescriptor(),
					new OffsetDateTimeFieldTypeDescriptor(),
					new OffsetTimeFieldTypeDescriptor(),
					new GeoPointFieldTypeDescriptor(),
					new BigDecimalFieldTypeDescriptor(),
					new BigIntegerFieldTypeDescriptor()
			) );
		}
		return all;
	}

	public static FieldTypeDescriptor<?> getIncompatible(FieldTypeDescriptor<?> typeDescriptor) {
		if ( Integer.class.equals( typeDescriptor.getJavaType() ) ) {
			return new LongFieldTypeDescriptor();
		}
		else {
			return new IntegerFieldTypeDescriptor();
		}
	}

	private final Class<F> javaType;
	private final String uniqueName;

	private final AscendingUniqueTermValues<F> ascendingUniqueTermValues = createAscendingUniqueTermValues();

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

	public abstract Optional<IndexingExpectations<F>> getIndexingExpectations();

	public abstract Optional<MatchPredicateExpectations<F>> getMatchPredicateExpectations();

	public abstract Optional<RangePredicateExpectations<F>> getRangePredicateExpectations();

	public abstract ExistsPredicateExpectations<F> getExistsPredicateExpectations();

	public ExpectationsAlternative<?, ?> getFieldSortExpectations() {
		// Assume supported by default: this way, we'll get test failures if we forget to override this method.
		return ExpectationsAlternative.supported( this );
	}

	public abstract Optional<FieldProjectionExpectations<F>> getFieldProjectionExpectations();

	public abstract Optional<IndexNullAsMatchPredicateExpectactions<F>> getIndexNullAsMatchPredicateExpectations();
}
