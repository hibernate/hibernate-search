/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Year;
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

public class YearFieldTypeDescriptor extends FieldTypeDescriptor<Year> {

	static List<Year> getValuesForIndexingExpectations() {
		return Arrays.asList(
				Year.of( -25435 ), Year.of( -42 ), Year.of( -1 ),
				Year.of( 0 ),
				Year.of( 1 ), Year.of( 3 ), Year.of( 42 ), Year.of( 18353 ),
				Year.of( 1989 ),
				Year.of( 1999 ),
				Year.of( 2000 ),
				Year.of( 2019 ),
				Year.of( 2050 ),
				/*
				 * Minimum and maximum years that can be represented as number of millisecond since the epoch in a long.
				 * The minimum and maximum dates that can be represented are slightly before/after,
				 * but there's no point telling users these years are supported if not all months are supported.
				 */
				Year.of( -292_275_054 ),
				Year.of( 292_278_993 )
		);
	}

	YearFieldTypeDescriptor() {
		super( Year.class );
	}

	@Override
	public Optional<IndexingExpectations<Year>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>( getValuesForIndexingExpectations() ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Year>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				Year.of( 1980 ), Year.of( 4302 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Year>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				Year.of( 1980 ), Year.of( 1982 ), Year.of( 1984 ),
				// Values around what is indexed
				Year.of( 1981 ), Year.of( 1983 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<Year> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				Year.of( 1980 ), Year.of( 4302 )
		);
	}

	@Override
	public Optional<FieldSortExpectations<Year>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				Year.of( 1980 ), Year.of( 1982 ), Year.of( 1984 ),
				// Values around what is indexed
				Year.of( -340 ), Year.of( 1981 ), Year.of( 1983 ), Year.of( 3000 )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Year>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				Year.of( -1200 ), Year.of( 1797 ), Year.of( 1979 )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Year>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				Year.of( 1970 ), Year.of( 4302 )
		) );
	}
}
