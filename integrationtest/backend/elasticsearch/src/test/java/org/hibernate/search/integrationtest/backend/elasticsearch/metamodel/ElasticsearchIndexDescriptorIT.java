/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ElasticsearchIndexDescriptorIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.createGlobal();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	public static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@Test
	public void test() {
		ElasticsearchIndexDescriptor indexDescriptor = index.toApi()
				.unwrap( ElasticsearchIndexManager.class ).descriptor();

		assertThat( indexDescriptor.readName() ).isEqualTo( "indexname-read" );
		assertThat( indexDescriptor.writeName() ).isEqualTo( "indexname-write" );
		assertThat( indexDescriptor.hibernateSearchName() ).isEqualTo( index.name() );
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
