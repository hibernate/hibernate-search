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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;

public class KeywordStringFieldTypeDescriptor extends FieldTypeDescriptor<String> {

	public static final KeywordStringFieldTypeDescriptor INSTANCE = new KeywordStringFieldTypeDescriptor();

	private KeywordStringFieldTypeDescriptor() {
		super( String.class, "keywordString" );
	}

	@Override
	protected AscendingUniqueTermValues<String> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<String>() {
			@Override
			protected List<String> createSingle() {
				return Arrays.asList(
						"Auster",
						"Irving",
						"Zach",
						"amaretto y croutons",
						"irving and company",
						"none the wiser",
						"per the captain's log",
						"platypus"
				);
			}

			@Override
			protected List<List<String>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected List<List<String>> createMultiResultingInSingleAfterAvg() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected List<List<String>> createMultiResultingInSingleAfterMedian() {
				return valuesThatWontBeUsed();
			}
		};
	}

	@Override
	public Optional<IndexingExpectations<String>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				"several tokens",
				"onetoken",
				"to the", // Only stopwords
				"    trailingspaces   ",
				"      ",
				""
		) );
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
	public Optional<FieldProjectionExpectations<String>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				"aaron", "george", "zach"
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<String>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				"NULL", "Aaron"
		) );
	}
}
