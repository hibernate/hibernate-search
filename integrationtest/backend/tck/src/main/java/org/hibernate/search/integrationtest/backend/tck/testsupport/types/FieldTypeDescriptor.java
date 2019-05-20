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

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

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
					new BigDecimalFieldTypeDescriptor()
					// TODO: unlock the tests when the type is supported on Lucene backend too
					// new BigIntegerFieldTypeDescriptor()
			) );
		}
		return all;
	}

	private final Class<F> javaType;
	private final String uniqueName;

	protected FieldTypeDescriptor(Class<F> javaType) {
		this( javaType, javaType.getSimpleName() );
	}

	protected FieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		this.javaType = javaType;
		this.uniqueName = uniqueName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[javaType=" + javaType + "]";
	}

	public final Class<F> getJavaType() {
		return javaType;
	}

	public final String getUniqueName() {
		return uniqueName;
	}

	public StandardIndexFieldTypeContext<?, F> configure(IndexFieldTypeFactoryContext fieldContext) {
		return fieldContext.as( javaType );
	}

	public abstract Optional<IndexingExpectations<F>> getIndexingExpectations();

	public abstract Optional<MatchPredicateExpectations<F>> getMatchPredicateExpectations();

	public abstract Optional<RangePredicateExpectations<F>> getRangePredicateExpectations();

	public abstract ExistsPredicateExpectations<F> getExistsPredicateExpectations();

	public abstract Optional<FieldSortExpectations<F>> getFieldSortExpectations();

	public abstract Optional<FieldProjectionExpectations<F>> getFieldProjectionExpectations();

	public abstract Optional<IndexNullAsMatchPredicateExpectactions<F>> getIndexNullAsMatchPredicateExpectations();
}
