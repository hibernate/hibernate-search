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

public class ByteFieldTypeDescriptor extends FieldTypeDescriptor<Byte> {

	ByteFieldTypeDescriptor() {
		super( Byte.class );
	}

	@Override
	public List<Byte> getAscendingUniqueTermValues() {
		return Arrays.asList(
				Byte.MIN_VALUE,
				(byte) -58,
				(byte) 0,
				(byte) 42,
				(byte) 55,
				(byte) 70,
				(byte) 101,
				Byte.MAX_VALUE
		);
	}

	@Override
	public Optional<IndexingExpectations<Byte>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				Byte.MIN_VALUE, Byte.MAX_VALUE,
				(byte) -42, (byte) -1, (byte) 0, (byte) 1, (byte) 3, (byte) 42
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Byte>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				(byte) 42, (byte) 67
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Byte>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				(byte) 3, (byte) 13, (byte) 25,
				(byte) 10, (byte) 19
		) );
	}

	@Override
	public ExistsPredicateExpectations<Byte> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				(byte) 0, (byte) 42
		);
	}

	@Override
	public Optional<FieldSortExpectations<Byte>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				(byte) 1, (byte) 3, (byte) 5,
				Byte.MIN_VALUE, (byte) 2, (byte) 4, Byte.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Byte>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				(byte) 1, (byte) 3, (byte) 5
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Byte>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				(byte) 0, (byte) 42
		) );
	}
}
