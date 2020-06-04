/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class LocalDateFieldTypeDescriptor extends FieldTypeDescriptor<LocalDate> {

	public static final LocalDateFieldTypeDescriptor INSTANCE = new LocalDateFieldTypeDescriptor();

	private LocalDateFieldTypeDescriptor() {
		super( LocalDate.class );
	}

	@Override
	protected AscendingUniqueTermValues<LocalDate> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<LocalDate>() {
			@Override
			protected List<LocalDate> createSingle() {
				return Arrays.asList(
						LocalDate.of( -52, 10, 11 ),
						LocalDate.of( 1600, 2, 28 ),
						LocalDate.of( 1900, 1, 1 ),
						LocalDate.of( 1970, 1, 1 ),
						LocalDate.of( 1980, 1, 1 ),
						LocalDate.of( 1980, 12, 31 ),
						LocalDate.of( 2004, 2, 29 ),
						LocalDate.of( 2017, 7, 7 )
				);
			}

			@Override
			protected List<List<LocalDate>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected LocalDate applyDelta(LocalDate value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.YEARS );
			}
		};
	}

	@Override
	protected IndexableValues<LocalDate> createIndexableValues() {
		return new IndexableValues<LocalDate>() {
			@Override
			protected List<LocalDate> createSingle() {
				return Arrays.asList(
						LocalDate.of( 1970, 1, 1 ),
						LocalDate.of( 1980, 1, 1 ),
						LocalDate.of( 2017, 7, 7 ),
						LocalDate.of( 1980, 12, 31 ),
						LocalDate.of( 2004, 2, 29 ),
						LocalDate.of( 1900, 1, 1 ),
						LocalDate.of( 1600, 2, 28 ),
						LocalDate.of( -52, 10, 11 ),
						LocalDate.of( 22500, 10, 11 )
				);
			}
		};
	}

	@Override
	public Optional<MatchPredicateExpectations<LocalDate>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalDate.of( 1980, 10, 11 ),
				LocalDate.of( 1984, 10, 7 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<LocalDate>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalDate.of( 2018, 2, 1 ),
				LocalDate.of( 2018, 3, 1 ),
				LocalDate.of( 2018, 4, 1 ),
				// Values around what is indexed
				LocalDate.of( 2018, 2, 15 ),
				LocalDate.of( 2018, 3, 15 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<LocalDate> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalDate.of( 1970, 1, 1 ),
				LocalDate.of( 1984, 10, 7 )
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<LocalDate>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDate.of( 1970, 1, 1 ),
				LocalDate.of( 1984, 10, 7 )
		) );
	}
}
