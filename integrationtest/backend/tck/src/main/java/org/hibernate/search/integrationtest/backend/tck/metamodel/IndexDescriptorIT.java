/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests for basic index descriptor features that are not tested elsewhere.
 */
@TestForIssue(jiraKey = "HSEARCH-3589")
public class IndexDescriptorIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@Test
	public void name() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();

		assertThat( indexDescriptor.hibernateSearchName() ).isEqualTo( index.name() );
	}

	@Test
	public void root() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();

		assertThat( indexDescriptor.root() ).isNotNull()
				.returns( true, IndexCompositeElementDescriptor::isRoot )
				.returns( false, IndexCompositeElementDescriptor::isObjectField );
	}

	@Test
	public void staticFields() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();

		Optional<IndexFieldDescriptor> valueFieldDescriptorOptional = indexDescriptor.field( "myValueField" );
		assertThat( valueFieldDescriptorOptional ).isPresent();

		Optional<IndexFieldDescriptor> objectFieldDescriptorOptional = indexDescriptor.field( "myObjectField" );
		assertThat( objectFieldDescriptorOptional ).isPresent();

		Collection<IndexFieldDescriptor> staticFields = indexDescriptor.staticFields();
		assertThat( staticFields ).contains(
				valueFieldDescriptorOptional.get(),
				objectFieldDescriptorOptional.get()
		);
	}

	@Test
	public void dynamicFields() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();

		Optional<IndexFieldDescriptor> valueFieldDescriptorOptional = indexDescriptor.field( "myDynamicField_txt" );
		assertThat( valueFieldDescriptorOptional ).isPresent();

		Optional<IndexFieldDescriptor> objectFieldDescriptorOptional = indexDescriptor.field( "myDynamicField_obj" );
		assertThat( objectFieldDescriptorOptional ).isPresent();

		Collection<IndexFieldDescriptor> staticFields = indexDescriptor.staticFields();
		assertThat( staticFields )
				.isNotEmpty()
				.doesNotContain( valueFieldDescriptorOptional.get(), objectFieldDescriptorOptional.get() );
	}

	@Test
	public void missingField() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		Optional<IndexFieldDescriptor> fieldDescriptorOptional = indexDescriptor.field( "unknownField" );
		assertThat( fieldDescriptorOptional ).isEmpty();
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			root.objectField( "myObjectField" ).toReference();
			root.field( "myValueField", f -> f.asString() ).toReference();

			root.objectFieldTemplate( "obj" ).matchingPathGlob( "*_obj" );
			root.fieldTemplate( "txt", f -> f.asString() ).matchingPathGlob( "*_txt" );
		}
	}
}
