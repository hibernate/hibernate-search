/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Month;
import java.time.MonthDay;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class MonthDayFieldTypeDescriptor extends FieldTypeDescriptor<MonthDay> {

	MonthDayFieldTypeDescriptor() {
		super( MonthDay.class );
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
	public Optional<FieldSortExpectations<MonthDay>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
			// Indexed
			MonthDay.of( Month.FEBRUARY, 7 ), MonthDay.of( Month.JUNE, 21 ), MonthDay.of( Month.NOVEMBER, 7 ),
			// Values around what is indexed
			MonthDay.of( Month.JANUARY, 30 ), MonthDay.of( Month.FEBRUARY, 21 ), MonthDay.of( Month.OCTOBER, 1 ), MonthDay.of( Month.NOVEMBER, 21 )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<MonthDay>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
			MonthDay.of( Month.JANUARY, 7 ), MonthDay.of( Month.NOVEMBER, 7 ), MonthDay.of( Month.NOVEMBER, 21 )
		) );
	}
}
