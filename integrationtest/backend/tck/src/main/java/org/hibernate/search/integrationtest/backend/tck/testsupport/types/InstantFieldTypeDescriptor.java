/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;

public class InstantFieldTypeDescriptor extends FieldTypeDescriptor<Instant> {

	public static final InstantFieldTypeDescriptor INSTANCE = new InstantFieldTypeDescriptor();

	private InstantFieldTypeDescriptor() {
		super( Instant.class );
	}

	@Override
	protected AscendingUniqueTermValues<Instant> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Instant>() {
			@Override
			protected List<Instant> createSingle() {
				return Arrays.asList(
						Instant.parse( "2018-01-01T10:58:30.00Z" ),
						Instant.parse( "2018-02-01T10:15:30.00Z" ),
						Instant.parse( "2018-02-01T12:15:30.00Z" ),
						Instant.parse( "2018-02-15T10:15:30.00Z" ),
						Instant.parse( "2018-03-01T08:15:30.00Z" ),
						Instant.parse( "2018-03-01T08:15:32.00Z" ),
						Instant.parse( "2018-03-15T09:15:30.00Z" ),
						Instant.parse( "2018-03-15T10:15:30.00Z" ),
						Instant.parse( "2018-04-01T10:15:30.00Z" )
				);
			}

			@Override
			protected List<List<Instant>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected Instant applyDelta(Instant value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.DAYS );
			}
		};
	}

	@Override
	public Optional<IndexingExpectations<Instant>> getIndexingExpectations() {
		List<Instant> values = new ArrayList<>();
		LocalDateTimeFieldTypeDescriptor.getValuesForIndexingExpectations().forEach( localDateTime -> {
			values.add( localDateTime.atOffset( ZoneOffset.UTC ).toInstant() );
		} );
		values.add( Instant.EPOCH );
		return Optional.of( new IndexingExpectations<>( values ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Instant>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				Instant.parse( "1980-10-11T10:15:30.00Z" ),
				Instant.parse( "1984-10-07T10:15:30.00Z" )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Instant>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				Instant.parse( "2018-02-01T10:15:30.00Z" ),
				Instant.parse( "2018-03-01T10:15:30.00Z" ),
				Instant.parse( "2018-04-01T10:15:30.00Z" ),
				// Values around what is indexed
				Instant.parse( "2018-02-15T10:15:30.00Z" ),
				Instant.parse( "2018-03-15T10:15:30.00Z" )
		) );
	}

	@Override
	public ExistsPredicateExpectations<Instant> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				Instant.EPOCH, Instant.parse( "2018-02-01T10:15:30.00Z" )
		);
	}

	@Override
	public FieldProjectionExpectations<Instant> getFieldProjectionExpectations() {
		return new FieldProjectionExpectations<>(
				Instant.parse( "2018-02-01T10:15:30.00Z" ),
				Instant.parse( "2018-03-01T10:15:30.00Z" ),
				Instant.parse( "2018-04-01T10:15:30.00Z" )
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Instant>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				Instant.EPOCH, Instant.parse( "2018-02-01T10:15:30.00Z" )
		) );
	}
}
