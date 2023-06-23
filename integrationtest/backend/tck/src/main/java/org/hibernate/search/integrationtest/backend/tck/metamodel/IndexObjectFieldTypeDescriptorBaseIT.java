/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Basic tests for object field type descriptor features.
 */
@TestForIssue(jiraKey = "HSEARCH-3589")
public class IndexObjectFieldTypeDescriptorBaseIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@Test
	public void isNested() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( false, IndexObjectFieldTypeDescriptor::nested );
		assertThat( getTypeDescriptor( "flattened" ) )
				.returns( false, IndexObjectFieldTypeDescriptor::nested );
		assertThat( getTypeDescriptor( "nested" ) )
				.returns( true, IndexObjectFieldTypeDescriptor::nested );
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
