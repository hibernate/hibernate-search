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

public class FloatFieldTypeDescriptor extends FieldTypeDescriptor<Float> {

	FloatFieldTypeDescriptor() {
		super( Float.class );
	}

	@Override
	public List<Float> getAscendingUniqueTermValues() {
		return Arrays.asList(
				-251_484_254.849f,
				-42.42f,
				0.0f,
				Float.MIN_VALUE,
				55f,
				2500.5100000045f,
				1584514514.000000184f,
				Float.MAX_VALUE
		);
	}

	@Override
	public Optional<IndexingExpectations<Float>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				Float.MIN_VALUE, Float.MAX_VALUE,
				- Float.MIN_VALUE, - Float.MAX_VALUE,
				// Elasticsearch doesn't support these: it fails when parsing them
				//Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
				0.0f,
				-0.0f, // Negative 0 is a different float
				Math.nextDown( 0.0f ),
				Math.nextUp( 0.0f ),
				42.42f,
				1584514514.000000184f
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Float>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42.1f, 67.0f
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Float>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3.0f, 13.2f, 25.79f,
				10.0f, 19.101f
		) );
	}

	@Override
	public ExistsPredicateExpectations<Float> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				0.0f, 67.0f
		);
	}

	@Override
	public Optional<FieldSortExpectations<Float>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				1.01f, 3.2f, 5.1f,
				-Float.MIN_VALUE, 2.0f, 4.00001f, Float.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Float>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				-1.001f, 3.0f, 5.1f
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Float>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0.0f, 67.0f
		) );
	}
}
