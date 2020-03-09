/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class LocalDateTimeFieldTypeDescriptor extends FieldTypeDescriptor<LocalDateTime> {

	static List<LocalDateTime> getValuesForIndexingExpectations() {
		return Arrays.asList(
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1980, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1985, 5, 13, 10, 15, 30, 0 ),
				LocalDateTime.of( 2017, 7, 7, 11, 15, 30, 555_000_000 ),
				LocalDateTime.of( 1980, 10, 5, 12, 0, 0, 0 ),
				LocalDateTime.of( 1980, 12, 31, 23, 59, 59, 999_000_000 ),
				LocalDateTime.of( 2004, 2, 29, 1, 0, 0, 0 ),
				LocalDateTime.of( 1900, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1600, 2, 28, 13, 0, 23, 0 ),
				LocalDateTime.of( -52, 10, 11, 10, 15, 30, 0 ),
				LocalDateTime.of( 22500, 10, 11, 17, 44, 0, 0 ),
				/*
				 * Minimum and maximum years that can be represented as number of millisecond since the epoch in a long.
				 * The minimum and maximum dates that can be represented are slightly before/after,
				 * but there's no point telling users these years are supported if not all months are supported.
				 */
				LocalDateTime.of( -292_275_054, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 292_278_993, 12, 31, 23, 59, 59, 999_000_000 )
		);
	}

	LocalDateTimeFieldTypeDescriptor() {
		super( LocalDateTime.class );
	}

	@Override
	public List<LocalDateTime> getAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return Arrays.asList(
				LocalDateTime.of( -52, 10, 11, 10, 15, 30, 0 ),
				LocalDateTime.of( 1600, 2, 28, 13, 0, 23, 0 ),
				LocalDateTime.of( 1900, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1980, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1980, 12, 31, 12, 0, 0, 0 ),
				LocalDateTime.of( 2004, 2, 29, 1, 0, 0, 0 ),
				LocalDateTime.of( 2017, 7, 7, 11, 15, 30, 555_000_000 )
		);
	}

	@Override
	public Optional<IndexingExpectations<LocalDateTime>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>( getValuesForIndexingExpectations() ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<LocalDateTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalDateTime.of( 1980, 10, 11, 0, 15 ),
				LocalDateTime.of( 1984, 10, 7, 15, 37, 37 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<LocalDateTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 10, 0 ),
				LocalDateTime.of( 2018, 3, 1, 15, 15, 15 ),
				LocalDateTime.of( 2018, 4, 1, 0, 59 ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 2, 1, 10, 0, 1 ),
				LocalDateTime.of( 2018, 3, 15, 10, 10 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<LocalDateTime> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0 ),
				LocalDateTime.of( 1984, 10, 7, 12, 14, 52 )
		);
	}

	@Override
	public Optional<FieldProjectionExpectations<LocalDateTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalDateTime.of( 2018, 2, 1, 14, 0, 15, 1 ),
				LocalDateTime.of( 2018, 3, 1, 0, 59 ),
				LocalDateTime.of( 2018, 4, 1, 23, 0 )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<LocalDateTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0 ),
				LocalDateTime.of( 1984, 10, 7, 12, 14, 52 )
		) );
	}
}
