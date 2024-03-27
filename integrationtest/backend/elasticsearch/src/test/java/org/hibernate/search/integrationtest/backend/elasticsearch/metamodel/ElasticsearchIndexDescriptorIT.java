/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchIndexDescriptorIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@Test
	void test() {
		ElasticsearchIndexDescriptor indexDescriptor = index.toApi()
				.unwrap( ElasticsearchIndexManager.class ).descriptor();

		assertThat( indexDescriptor.readName() ).isEqualTo( "indexname-read" );
		assertThat( indexDescriptor.writeName() ).isEqualTo( "indexname-write" );
		assertThat( indexDescriptor.hibernateSearchName() ).isEqualTo( index.name() );
	}

	@Test
	void implicitFields() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();

		Optional<IndexFieldDescriptor> valueFieldDescriptorOptional = indexDescriptor.field( "string" );
		assertThat( valueFieldDescriptorOptional ).isPresent();

		Optional<IndexFieldDescriptor> idDescriptorOptional = indexDescriptor.field( "_id" );
		assertThat( idDescriptorOptional ).isPresent();

		Optional<IndexFieldDescriptor> indexDescriptorOptional = indexDescriptor.field( "_index" );
		assertThat( indexDescriptorOptional ).isPresent();

		Optional<IndexFieldDescriptor> entityTypeDescriptorOptional = indexDescriptor.field( "_entity_type" );
		assertThat( entityTypeDescriptorOptional ).isPresent();

		Collection<IndexFieldDescriptor> staticFields = indexDescriptor.staticFields();
		assertThat( staticFields ).containsOnly(
				idDescriptorOptional.get(),
				indexDescriptorOptional.get(),
				entityTypeDescriptorOptional.get(),
				valueFieldDescriptorOptional.get()
		);
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			root.field( "string", f -> f.asString() ).toReference();
		}
	}
}
