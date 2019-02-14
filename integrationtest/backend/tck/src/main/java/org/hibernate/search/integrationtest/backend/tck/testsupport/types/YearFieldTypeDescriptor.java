/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Year;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class YearFieldTypeDescriptor extends FieldTypeDescriptor<Year> {

	YearFieldTypeDescriptor() {
		super( Year.class );
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
}
