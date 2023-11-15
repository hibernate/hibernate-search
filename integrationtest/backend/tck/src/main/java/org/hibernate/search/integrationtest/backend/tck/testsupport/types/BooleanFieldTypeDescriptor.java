/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class BooleanFieldTypeDescriptor extends StandardFieldTypeDescriptor<Boolean> {

	public static final BooleanFieldTypeDescriptor INSTANCE = new BooleanFieldTypeDescriptor();

	private BooleanFieldTypeDescriptor() {
		super( Boolean.class );
	}

	@Override
	protected AscendingUniqueTermValues<Boolean> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Boolean>() {
			@Override
			public List<Boolean> createSingle() {
				return Arrays.asList( false, true );
			}

			@Override
			protected List<List<Boolean>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected List<List<Boolean>> createMultiResultingInSingleAfterAvg() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected List<List<Boolean>> createMultiResultingInSingleAfterMedian() {
				return valuesThatWontBeUsed();
			}
		};
	}

	@Override
	protected IndexableValues<Boolean> createIndexableValues() {
		return new IndexableValues<Boolean>() {
			@Override
			protected List<Boolean> createSingle() {
				return Arrays.asList(
						true,
						false,
						true
				);
			}
		};
	}

	@Override
	protected List<Boolean> createUniquelyMatchableValues() {
		return Arrays.asList(
				true,
				false
		);
	}

	@Override
	protected List<Boolean> createNonMatchingValues() {
		return Collections.emptyList();
	}

	@Override
	public Boolean valueFromInteger(int integer) {
		return integer % 2 == 0;
	}

	@Override
	public boolean isFieldSortSupported() {
		return false;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Boolean>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				true, false
		) );
	}
}
