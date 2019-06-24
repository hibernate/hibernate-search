/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class AnalyzedStringFieldTypeDescriptor extends FieldTypeDescriptor<String> {

	AnalyzedStringFieldTypeDescriptor() {
		super( String.class, "analyzedString" );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, String> configure(IndexFieldTypeFactoryContext fieldContext) {
		return fieldContext.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name );
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
	public Optional<FieldSortExpectations<String>> getFieldSortExpectations() {
		// TODO also test sorts, be it only to check that we correctly throw exceptions?
		return Optional.empty();
	}

	@Override
	public Optional<FieldProjectionExpectations<String>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				// Mix capitalized and non-capitalized text on purpose
				"Aaron", "george", "Zach"
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<String>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.empty();
	}
}
