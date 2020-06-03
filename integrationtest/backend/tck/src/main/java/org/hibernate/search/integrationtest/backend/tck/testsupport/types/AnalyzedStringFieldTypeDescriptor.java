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

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;

public class AnalyzedStringFieldTypeDescriptor extends FieldTypeDescriptor<String> {

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
	public IndexingExpectations<String> getIndexingExpectations() {
		return new IndexingExpectations<>(
				"several tokens",
				"onetoken",
				"to the", // Only stopwords
				"    trailingspaces   ",
				"      ",
				""
		);
	}

	@Override
	public Optional<MatchPredicateExpectations<String>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				"irving and company", "Auster",
				"Irving"
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<String>> getRangePredicateExpectations() {
		// TODO also test range predicates, be it only to check that we correctly throw exceptions?
		return Optional.empty();
	}

	@Override
	public ExistsPredicateExpectations<String> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				"", // No token, but still non-null: should be considered as existing
				"irving and company"
		);
	}

	@Override
	public ExpectationsAlternative<?, ?> getFieldSortExpectations() {
		return ExpectationsAlternative.unsupported( this );
	}

	@Override
	public FieldProjectionExpectations<String> getFieldProjectionExpectations() {
		return new FieldProjectionExpectations<>(
				// Mix capitalized and non-capitalized text on purpose
				"Aaron", "george", "Zach"
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<String>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.empty();
	}
}
