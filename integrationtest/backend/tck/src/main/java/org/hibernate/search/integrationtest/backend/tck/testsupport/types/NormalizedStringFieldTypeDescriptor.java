/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class NormalizedStringFieldTypeDescriptor extends FieldTypeDescriptor<String> {

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
	public Optional<MatchPredicateExpectations<String>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				"irving", "Auster",
				"Irving"
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<String>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				"Aaron", "george", "Zach",
				"cecilia", "Roger"
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
	public Optional<IndexNullAsMatchPredicateExpectactions<String>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				"", "Aaron"
		) );
	}
}
