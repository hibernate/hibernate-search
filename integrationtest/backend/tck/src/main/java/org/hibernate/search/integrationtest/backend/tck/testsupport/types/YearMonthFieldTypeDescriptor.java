/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class YearMonthFieldTypeDescriptor extends FieldTypeDescriptor<YearMonth> {

	YearMonthFieldTypeDescriptor() {
		super( YearMonth.class );
	}

	@Override
	public List<YearMonth> getAscendingUniqueTermValues() {
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
	public Optional<IndexingExpectations<YearMonth>> getIndexingExpectations() {
		List<YearMonth> values = new ArrayList<>();
		YearFieldTypeDescriptor.getValuesForIndexingExpectations().forEach( year -> {
			Arrays.stream( Month.values() ).forEach( month -> {
				values.add( year.atMonth( month ) );
			} );
		} );
		return Optional.of( new IndexingExpectations<>( values ) );
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

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<YearMonth>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				YearMonth.of( 0, Month.JANUARY ),
				YearMonth.of( 2017, Month.NOVEMBER )
		) );
	}
}
