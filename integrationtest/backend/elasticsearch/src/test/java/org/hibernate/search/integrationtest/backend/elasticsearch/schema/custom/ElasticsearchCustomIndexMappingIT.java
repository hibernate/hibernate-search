/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.custom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchCustomIndexMappingIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	// Valid cases are tested elsewhere, e.g. ElasticsearchIndexSchemaManagerCreationCustomMappingIT

	@Test
	void notExisting() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/not-existing.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to find the given custom index mapping file:",
						"custom-index-mapping/not-existing.json"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4438")
	void notParsable() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/not-parsable.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"There are some JSON syntax errors on the given custom index mapping file",
						"custom-index-mapping/not-parsable.json",
						"Expected BEGIN_OBJECT but was STRING at line 1 column 1"
				);
	}

	@Test
	void unknownParameter() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/unknown-parameter.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Hibernate Search encountered failures during bootstrap",
						"Elasticsearch response indicates a failure",
						// We cannot check for the presence of "unknown parameter"
						// because in ES 5.6 -> 7.9 it's "unsupported parameter" instead.
						"mapper_parsing_exception", "someUnknownParameter"
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
