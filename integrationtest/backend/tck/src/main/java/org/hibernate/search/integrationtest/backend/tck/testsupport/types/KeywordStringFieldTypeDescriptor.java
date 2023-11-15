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

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class KeywordStringFieldTypeDescriptor extends StandardFieldTypeDescriptor<String> {

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
	protected IndexableValues<String> createIndexableValues() {
		return new IndexableValues<String>() {
			@Override
			protected List<String> createSingle() {
				return Arrays.asList(
						"several tokens",
						"onetoken",
						"to the", // Only stopwords
						"    trailingspaces   ",
						"      ",
						""
				);
			}
		};
	}

	@Override
	protected List<String> createUniquelyMatchableValues() {
		return Arrays.asList(
				"several tokens",
				"onetoken",
				"    trailingspaces   "
		);
	}

	@Override
	protected List<String> createNonMatchingValues() {
		return Arrays.asList(
				" notmatchingitem1 ",
				"abracadabravv7",
				"    makethisnotmatchable   "
		);
	}

	@Override
	public String valueFromInteger(int integer) {
		return "string_" + integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<String>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				"NULL", "Aaron"
		) );
	}
}
