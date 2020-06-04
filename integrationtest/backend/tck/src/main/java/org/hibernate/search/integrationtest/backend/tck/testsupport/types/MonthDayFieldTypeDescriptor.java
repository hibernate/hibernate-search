/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class MonthDayFieldTypeDescriptor extends FieldTypeDescriptor<MonthDay> {

	public static final MonthDayFieldTypeDescriptor INSTANCE = new MonthDayFieldTypeDescriptor();

	private MonthDayFieldTypeDescriptor() {
		super( MonthDay.class );
	}

	@Override
	protected AscendingUniqueTermValues<MonthDay> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<MonthDay>() {
			@Override
			protected List<MonthDay> createSingle() {
				return Arrays.asList(
						MonthDay.of( Month.JANUARY, 1 ),
						MonthDay.of( Month.FEBRUARY, 29 ),
						MonthDay.of( Month.MARCH, 7 ),
						MonthDay.of( Month.JUNE, 6 ),
						MonthDay.of( Month.JULY, 14 ),
						MonthDay.of( Month.SEPTEMBER, 18 ),
						MonthDay.of( Month.NOVEMBER, 6 ),
						MonthDay.of( Month.DECEMBER, 31 )
				);
			}

			@Override
			protected List<List<MonthDay>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected MonthDay applyDelta(MonthDay value, int multiplierForDelta) {
				LocalDate date = value.atYear( 0 ).plus( multiplierForDelta, ChronoUnit.DAYS );
				return MonthDay.of( date.getMonth(), date.getDayOfMonth() );
			}
		};
	}

	@Override
	protected IndexableValues<MonthDay> createIndexableValues() {
		return new IndexableValues<MonthDay>() {
			@Override
			protected List<MonthDay> createSingle() {
				List<MonthDay> values = new ArrayList<>();
				Arrays.stream( Month.values() ).forEach( month -> {
					values.add( MonthDay.of( month, 1 ) );
					values.add( MonthDay.of( month, 3 ) );
					values.add( MonthDay.of( month, 14 ) );
					values.add( MonthDay.of( month, 28 ) );
				} );
				Collections.addAll(
						values,
						MonthDay.of( Month.FEBRUARY, 28 ),
						MonthDay.of( Month.FEBRUARY, 29 ), // HSEARCH-3549
						MonthDay.of( Month.JUNE, 30 ),
						MonthDay.of( Month.DECEMBER, 31 )
				);
				return values;
			}
		};
	}

	@Override
	public Optional<MatchPredicateExpectations<MonthDay>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
			MonthDay.of( Month.NOVEMBER, 7 ), MonthDay.of( Month.NOVEMBER, 21 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<MonthDay>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
			// Indexed
			MonthDay.of( Month.FEBRUARY, 7 ), MonthDay.of( Month.JUNE, 21 ), MonthDay.of( Month.NOVEMBER, 7 ),
			// Values around what is indexed
			MonthDay.of( Month.FEBRUARY, 21 ), MonthDay.of( Month.OCTOBER, 1 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<MonthDay> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				MonthDay.of( Month.JANUARY, 1 ),
				MonthDay.of( Month.FEBRUARY, 28 )
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<MonthDay>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				MonthDay.of( Month.JANUARY, 1 ),
				MonthDay.of( Month.FEBRUARY, 28 )
		) );
	}
}
