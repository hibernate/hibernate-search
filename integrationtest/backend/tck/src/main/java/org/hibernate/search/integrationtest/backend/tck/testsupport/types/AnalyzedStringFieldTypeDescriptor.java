/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class AnalyzedStringFieldTypeDescriptor extends StandardFieldTypeDescriptor<String> {

	public static final AnalyzedStringFieldTypeDescriptor INSTANCE = new AnalyzedStringFieldTypeDescriptor();

	private AnalyzedStringFieldTypeDescriptor() {
		super( String.class, "analyzedString" );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, String> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name );
	}

	@Override
	protected AscendingUniqueTermValues<String> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<String>() {
			@Override
			public List<String> createSingle() {
				return Arrays.asList(
						"amaretto",
						"Auster",
						"captain",
						"irving",
						"none",
						"platypus",
						"wifi",
						"Zach"
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
						"",
						// Mix capitalized and non-capitalized text on purpose
						"Aaron",
						"george",
						"Zach"
				);
			}
		};
	}

	@Override
	protected List<String> createUniquelyMatchableValues() {
		return Arrays.asList(
				"several tokens",
				"onetoken",
				// Mix capitalized and non-capitalized text on purpose
				"Aaron",
				"george",
				"Zach"
		);
	}

	@Override
	protected List<String> createNonMatchingValues() {
		return Arrays.asList(
				"notmatchingitem1",
				"abracadabravv7",
				"makethisnotmatchable"
		);
	}

	@Override
	public String valueFromInteger(int integer) {
		return "string_" + integer;
	}

	@Override
	public boolean isFieldSortSupported() {
		return false;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<String>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.empty();
	}
}
