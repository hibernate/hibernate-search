/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class LocalTimeFieldTypeDescriptor extends FieldTypeDescriptor<LocalTime> {

	static List<LocalTime> getValuesForIndexingExpectations() {
		return Arrays.asList(
				LocalTime.of( 0, 0, 0, 0 ),
				LocalTime.of( 10, 15, 30, 0 ),
				LocalTime.of( 11, 15, 30, 555_000_000 ),
				LocalTime.of( 23, 59, 59, 999_000_000 )
		);
	}

	LocalTimeFieldTypeDescriptor() {
		super( LocalTime.class );
	}

	@Override
	public List<LocalTime> getAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return Arrays.asList(
				LocalTime.of( 0, 0, 0, 0 ),
				LocalTime.of( 1, 0, 0, 0 ),
				LocalTime.of( 10, 15, 30, 0 ),
				LocalTime.of( 11, 15, 30, 555_000_000 ),
				LocalTime.of( 12, 0, 0, 0 ),
				LocalTime.of( 13, 0, 23, 0 ),
				LocalTime.of( 17, 59, 0, 0 ),
				LocalTime.of( 23, 59, 59, 999_000_000 )
		);
	}

	@Override
	public Optional<IndexingExpectations<LocalTime>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>( getValuesForIndexingExpectations() ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<LocalTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalTime.of( 10, 10, 10, 123_000_000 ),
				LocalTime.of( 10, 10, 10, 122_000_000 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<LocalTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalTime.of( 10, 10, 10, 0 ),
				LocalTime.of( 11, 10, 10, 0 ),
				LocalTime.of( 12, 10, 10, 0 ),
				// Values around what is indexed
				LocalTime.of( 10, 40, 10, 0 ),
				LocalTime.of( 11, 40, 10, 0 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<LocalTime> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalTime.of( 0, 0, 0 ),
				LocalTime.of( 12, 14, 52 )
		);
	}

	@Override
	public Optional<FieldProjectionExpectations<LocalTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalTime.of( 10, 10, 10, 123_000_000 ),
				LocalTime.of( 11, 10, 10, 123_450_000 ),
				LocalTime.of( 12, 10, 10, 123_456_789 )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<LocalTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalTime.of( 0, 0, 0 ),
				LocalTime.of( 12, 14, 52 )
		) );
	}
}
