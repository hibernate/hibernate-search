/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Month;
import java.time.YearMonth;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class YearMonthFieldTypeDescriptor extends FieldTypeDescriptor<YearMonth> {

	YearMonthFieldTypeDescriptor() {
		super( YearMonth.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<YearMonth>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
			YearMonth.of( -348, Month.NOVEMBER ), YearMonth.of( 2200, Month.NOVEMBER )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<YearMonth>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
			// Indexed
			YearMonth.of( 1797, Month.JANUARY ), YearMonth.of( 1980, Month.NOVEMBER ), YearMonth.of( 3001, Month.JANUARY ),
			// Values around what is indexed
			YearMonth.of( 1980, Month.OCTOBER ), YearMonth.of( 1981, Month.JANUARY )
		) );
	}

	@Override
	public ExistsPredicateExpectations<YearMonth> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				YearMonth.of( 0, Month.JANUARY ),
				YearMonth.of( 2017, Month.NOVEMBER )
		);
	}

	@Override
	public Optional<FieldSortExpectations<YearMonth>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
			// Indexed
			YearMonth.of( 1797, Month.JANUARY ), YearMonth.of( 1980, Month.NOVEMBER ), YearMonth.of( 1989, Month.JANUARY ),
			// Values around what is indexed
			YearMonth.of( -1200, Month.DECEMBER ), YearMonth.of( 1980, Month.OCTOBER ), YearMonth.of( 1981, Month.JANUARY ), YearMonth.of( 2050, Month.DECEMBER )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<YearMonth>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
			YearMonth.of( -320, Month.NOVEMBER ), YearMonth.of( 1984, Month.JANUARY ), YearMonth.of( 6001, Month.NOVEMBER )
		) );
	}
}
