/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class NormalizedStringFieldTypeDescriptor extends StandardFieldTypeDescriptor<String> {

	public static final NormalizedStringFieldTypeDescriptor INSTANCE = new NormalizedStringFieldTypeDescriptor();

	private NormalizedStringFieldTypeDescriptor() {
		super( String.class, "normalizedString" );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, String> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name );
	}

	@Override
	public String toExpectedDocValue(String indexed) {
		return indexed == null ? null : indexed.toLowerCase( Locale.ROOT );
	}

	@Override
	protected AscendingUniqueTermValues<String> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<String>() {
			@Override
			protected List<String> createSingle() {
				return Arrays.asList(
						"amaretto y croutons",
						"Auster",
						"Irving",
						"irving and company",
						"none the wiser",
						"per the captain's log",
						"platypus",
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
				"", "Aaron"
		) );
	}
}
