/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests for value field type descriptor features that are specific to String field types
 * and are not already tested in {@link IndexValueFieldTypeDescriptorBaseIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-3589")
class IndexValueFieldTypeDescriptorStringSpecificsIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@Test
	void analyzerName() {
		assertThat( getTypeDescriptor( "noSearchAnalyzer" ).analyzerName() )
				.contains( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name );
		assertThat( getTypeDescriptor( "searchAnalyzer" ).analyzerName() )
				.contains( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name );
	}

	@Test
	void searchAnalyzerName() {
		assertThat( getTypeDescriptor( "noSearchAnalyzer" ).searchAnalyzerName() )
				.contains( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name );
		assertThat( getTypeDescriptor( "searchAnalyzer" ).searchAnalyzerName() )
				.contains( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name );
	}

	private IndexValueFieldTypeDescriptor getTypeDescriptor(String fieldName) {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexValueFieldDescriptor fieldDescriptor = indexDescriptor.field( fieldName ).get().toValueField();
		return fieldDescriptor.type();
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			root.field( "noSearchAnalyzer",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ) )
					.toReference();

			root.field( "searchAnalyzer",
					f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
					.toReference();
		}
	}
}
