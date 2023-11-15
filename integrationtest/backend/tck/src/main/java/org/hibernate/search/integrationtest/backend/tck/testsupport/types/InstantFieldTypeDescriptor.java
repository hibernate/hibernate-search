/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class InstantFieldTypeDescriptor extends StandardFieldTypeDescriptor<Instant> {

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
	protected IndexableValues<Instant> createIndexableValues() {
		return new IndexableValues<Instant>() {
			@Override
			protected List<Instant> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Instant> createUniquelyMatchableValues() {
		Set<Instant> values = new LinkedHashSet<>();
		for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getIndexableValues().getSingle() ) {
			values.add( localDateTime.atOffset( ZoneOffset.UTC ).toInstant() );
		}
		values.add( Instant.EPOCH );
		return new ArrayList<>( values );
	}

	@Override
	protected List<Instant> createNonMatchingValues() {
		Set<Instant> values = new LinkedHashSet<>();
		for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getNonMatchingValues() ) {
			values.add( localDateTime.atOffset( ZoneOffset.UTC ).toInstant() );
		}
		values.add( Instant.EPOCH );
		return new ArrayList<>( values );
	}

	@Override
	public Instant valueFromInteger(int integer) {
		return LocalDateTimeFieldTypeDescriptor.INSTANCE.valueFromInteger( integer )
				.atOffset( ZoneOffset.UTC ).toInstant();
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Instant>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				Instant.EPOCH, Instant.parse( "2018-02-01T10:15:30.00Z" )
		) );
	}
}
