/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class YearMonthFieldTypeDescriptor extends StandardFieldTypeDescriptor<YearMonth> {

	public static final YearMonthFieldTypeDescriptor INSTANCE = new YearMonthFieldTypeDescriptor();

	private YearMonthFieldTypeDescriptor() {
		super( YearMonth.class );
	}

	@Override
	protected AscendingUniqueTermValues<YearMonth> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<YearMonth>() {
			@Override
			protected List<YearMonth> createSingle() {
				return Arrays.asList(
						YearMonth.of( -25435, Month.MAY ),
						YearMonth.of( 0, Month.JUNE ),
						YearMonth.of( 0, Month.OCTOBER ),
						YearMonth.of( 1989, Month.MARCH ),
						YearMonth.of( 1989, Month.JULY ),
						YearMonth.of( 2019, Month.JANUARY ),
						YearMonth.of( 2019, Month.NOVEMBER ),
						YearMonth.of( 2019, Month.DECEMBER )
				);
			}

			@Override
			protected List<List<YearMonth>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected YearMonth applyDelta(YearMonth value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.YEARS );
			}
		};
	}

	@Override
	protected IndexableValues<YearMonth> createIndexableValues() {
		return new IndexableValues<YearMonth>() {
			@Override
			protected List<YearMonth> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<YearMonth> createUniquelyMatchableValues() {
		List<YearMonth> values = new ArrayList<>();
		for ( Year year : YearFieldTypeDescriptor.INSTANCE.getIndexableValues().getSingle() ) {
			for ( Month month : Month.values() ) {
				values.add( year.atMonth( month ) );
			}
		}
		return values;
	}

	@Override
	protected List<YearMonth> createNonMatchingValues() {
		List<YearMonth> values = new ArrayList<>();
		for ( Year year : YearFieldTypeDescriptor.INSTANCE.getNonMatchingValues() ) {
			for ( Month month : Month.values() ) {
				values.add( year.atMonth( month ) );
			}
		}
		return values;
	}

	@Override
	public YearMonth valueFromInteger(int integer) {
		return YearMonth.of( 2000 + integer, Month.SEPTEMBER );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<YearMonth>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				YearMonth.of( 0, Month.JANUARY ),
				YearMonth.of( 2017, Month.NOVEMBER )
		) );
	}
}
