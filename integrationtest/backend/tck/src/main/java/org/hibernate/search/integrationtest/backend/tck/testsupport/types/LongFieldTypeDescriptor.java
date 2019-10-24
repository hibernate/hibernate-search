/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

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

public class LongFieldTypeDescriptor extends FieldTypeDescriptor<Long> {

	LongFieldTypeDescriptor() {
		super( Long.class );
	}

	@Override
	public List<Long> getAscendingUniqueTermValues() {
		return Arrays.asList(
				Long.MIN_VALUE,
				-251_484_254L,
				0L,
				42L,
				55L,
				2500L,
				151_484_254L,
				Long.MAX_VALUE
		);
	}

	@Override
	public Optional<IndexingExpectations<Long>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				Long.MIN_VALUE, Long.MAX_VALUE,
				(long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE,
				-251_484_254L, -42L, -1L, 0L, 1L, 3L, 42L, 151_484_254L
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Long>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42L, 67L
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Long>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3L, 13L, 25L,
				10L, 19L
		) );
	}

	@Override
	public ExistsPredicateExpectations<Long> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				0L, 67L
		);
	}

	@Override
	public Optional<FieldSortExpectations<Long>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				1L, 3L, 5L,
				Long.MIN_VALUE, 2L, 4L, Long.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Long>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				1L, 3L, 5L
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Long>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0L, 67L
		) );
	}
}
