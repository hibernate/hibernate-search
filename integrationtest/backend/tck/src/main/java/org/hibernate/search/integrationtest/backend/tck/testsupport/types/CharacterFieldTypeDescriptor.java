/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class CharacterFieldTypeDescriptor extends FieldTypeDescriptor<Character> {

	CharacterFieldTypeDescriptor() {
		super( Character.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<Character>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				(char) 42, (char) 67
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Character>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				(char) 3, (char) 13, (char) 25,
				(char) 10, (char) 19
		) );
	}

	@Override
	public Optional<FieldSortExpectations<Character>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				(char) 1, (char) 3, (char) 5,
				Character.MIN_VALUE, (char) 2, (char) 4, Character.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Character>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				(char) 1, (char) 3, (char) 5
		) );
	}
}
