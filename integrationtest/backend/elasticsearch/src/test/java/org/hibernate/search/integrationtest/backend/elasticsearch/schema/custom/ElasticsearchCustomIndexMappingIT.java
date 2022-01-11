/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.custom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchCustomIndexMappingIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	// Valid cases are tested elsewhere, e.g. ElasticsearchIndexSchemaManagerCreationCustomMappingIT

	@Test
	public void notExisting() {
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
	public void notParsable() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/not-parsable.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"There are some JSON syntax errors on the given custom index mapping file:",
						"custom-index-mapping/not-parsable.json"
				);
	}

	@Test
	public void unknownParameter() {
		assertThatThrownBy( () -> setupHelper.start().withIndex( index )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/unknown-parameter.json"
				).setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Hibernate Search encountered failures during bootstrap",
						"Elasticsearch response indicates a failure",
						"mapper_parsing_exception", "unknown parameter", "someUnknownParameter"
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
