/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class ZonedDateTimeFieldTypeDescriptor extends FieldTypeDescriptor<ZonedDateTime> {

	ZonedDateTimeFieldTypeDescriptor() {
		super( ZonedDateTime.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<ZonedDateTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalDateTime.of( 1980, 10, 11, 0, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 1984, 10, 7, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<ZonedDateTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 0, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 2018, 4, 1, 0, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 2, 15, 0, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) )
		) );
	}

	@Override
	public ExistsPredicateExpectations<ZonedDateTime> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0 ).atZone( ZoneId.of( "UTC" ) ),
				LocalDateTime.of( 2018, 3, 1, 12, 14, 52 ).atZone( ZoneId.of( "Europe/Paris" ) )
		);
	}

	@Override
	public Optional<FieldSortExpectations<ZonedDateTime>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) ),
				LocalDateTime.of( 2018, 4, 1, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 1, 1, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) ),
				LocalDateTime.of( 2018, 2, 15, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atZone( ZoneId.of( "US/Alaska" ) ),
				LocalDateTime.of( 2018, 5, 1, 0, 0 ).atZone( ZoneId.of( "America/Chicago" ) )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<ZonedDateTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalDateTime.of( 2018, 2, 1, 0, 0, 0, 1 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atZone( ZoneId.of( "UTC" ) )
		) );
	}
}
