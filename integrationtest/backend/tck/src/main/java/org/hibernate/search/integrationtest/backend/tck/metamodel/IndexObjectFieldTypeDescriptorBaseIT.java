/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.InstanceOfAssertFactories;

/**
 * Basic tests for object field type descriptor features.
 */
@TestForIssue(jiraKey = "HSEARCH-3589")
class IndexObjectFieldTypeDescriptorBaseIT {

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
	void isNested() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( false, IndexObjectFieldTypeDescriptor::nested );
		assertThat( getTypeDescriptor( "flattened" ) )
				.returns( false, IndexObjectFieldTypeDescriptor::nested );
		assertThat( getTypeDescriptor( "nested" ) )
				.returns( true, IndexObjectFieldTypeDescriptor::nested );
	}

	@Test
	void nestedTraits() {
		assertThat( getTypeDescriptor( "default" ) )
				.extracting( IndexObjectFieldTypeDescriptor::traits, InstanceOfAssertFactories.collection( String.class ) )
				.doesNotContain( "predicate:nested" );
		assertThat( getTypeDescriptor( "flattened" ) )
				.extracting( IndexObjectFieldTypeDescriptor::traits, InstanceOfAssertFactories.collection( String.class ) )
				.doesNotContain( "predicate:nested" );
		assertThat( getTypeDescriptor( "nested" ) )
				.extracting( IndexObjectFieldTypeDescriptor::traits, InstanceOfAssertFactories.collection( String.class ) )
				.contains( "predicate:nested" );
	}

	private IndexObjectFieldTypeDescriptor getTypeDescriptor(String fieldName) {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexObjectFieldDescriptor fieldDescriptor = indexDescriptor.field( fieldName ).get().toObjectField();
		return fieldDescriptor.type();
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			root.objectField( "default" ).toReference();

			root.objectField( "flattened", ObjectStructure.FLATTENED ).toReference();
			root.objectField( "nested", ObjectStructure.NESTED ).toReference();
		}
	}
}
