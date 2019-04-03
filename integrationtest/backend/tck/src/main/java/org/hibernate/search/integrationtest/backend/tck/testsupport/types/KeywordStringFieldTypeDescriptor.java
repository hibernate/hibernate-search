/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class KeywordStringFieldTypeDescriptor extends FieldTypeDescriptor<String> {

	KeywordStringFieldTypeDescriptor() {
		super( String.class, "keywordString" );
	}

	@Override
	public Optional<MatchPredicateExpectations<String>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				"Irving", "Auster"
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<String>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				"aaron", "george", "zach",
				"bastian", "marc"
		) );
	}

	@Override
	public ExistsPredicateExpectations<String> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				"", // No token, but still non-null: should be considered as existing
				"Aaron"
		);
	}

	@Override
	public Optional<FieldSortExpectations<String>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				"aaron", "george", "zach",
				"aaaa", "bastian", "marc", "zzzz"
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<String>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				"aaron", "george", "zach"
		) );
	}
}
