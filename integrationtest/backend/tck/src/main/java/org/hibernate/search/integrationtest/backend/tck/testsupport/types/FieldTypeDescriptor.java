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

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public abstract class FieldTypeDescriptor<F> extends AbstractFieldTypeDescriptor<F> {

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

	private final AscendingUniqueTermValues<F> ascendingUniqueTermValues = createAscendingUniqueTermValues();

	private final IndexableValues<F> indexableValues = createIndexableValues();


	protected FieldTypeDescriptor(Class<F> javaType) {
		super( javaType );
	}

	protected FieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		super( javaType, uniqueName );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.as( javaType );
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

}
