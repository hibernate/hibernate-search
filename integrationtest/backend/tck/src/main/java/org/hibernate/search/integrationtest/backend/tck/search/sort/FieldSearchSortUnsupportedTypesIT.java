/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of field sort common to all unsupported types,
 * i.e. error messages.
 */
@RunWith(Parameterized.class)
public class FieldSearchSortUnsupportedTypesIT<F> {

	private static Stream<FieldTypeDescriptor<?>> unsupportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> ! typeDescriptor.getFieldSortExpectations().isSupported() );
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		unsupportedTypeDescriptors().forEach( fieldTypeDescriptor -> {
			parameters.add( new Object[] { fieldTypeDescriptor } );
		} );
		return parameters.toArray( new Object[0][] );
	}

	private static final String INDEX_NAME = "IndexName";

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static IndexMapping indexMapping;
	private static StubMappingIndexManager indexManager;

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> FieldSearchSortUnsupportedTypesIT.indexManager = indexManager
				)
				.setup();
	}

	private final FieldTypeDescriptor<F> fieldTypeDescriptor;

	public FieldSearchSortUnsupportedTypesIT(FieldTypeDescriptor<F> fieldTypeDescriptor) {
		this.fieldTypeDescriptor = fieldTypeDescriptor;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3798")
	public void error_notSupported() {
		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = getFieldPath();

		SubTest.expectException(
				() -> scope.sort().field( absoluteFieldPath )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( absoluteFieldPath );
	}

	private String getFieldPath() {
		return indexMapping.fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static class IndexMapping {
		final FieldModelsByType fieldModels;

		IndexMapping(IndexSchemaElement root) {
			fieldModels = FieldModelsByType.mapUnsupported( root, "", ignored -> { } );
		}
	}

	private static class FieldModelsByType {
		public static FieldModelsByType mapUnsupported(IndexSchemaElement parent, String prefix,
				Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration) {
			FieldModelsByType result = new FieldModelsByType();
			unsupportedTypeDescriptors().forEach( typeDescriptor -> {
				result.content.put(
						typeDescriptor,
						FieldModel.mapper( typeDescriptor )
								.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration )
				);
			} );
			return result;
		}

		private final Map<FieldTypeDescriptor<?>, FieldModel<?>> content = new LinkedHashMap<>();

		@SuppressWarnings("unchecked")
		private <F> FieldModel<F> get(FieldTypeDescriptor<F> typeDescriptor) {
			return (FieldModel<F>) content.get( typeDescriptor );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					c -> c.sortable( Sortable.NO ),
					(reference, name) -> new FieldModel<>( reference, name, typeDescriptor.getJavaType() )
			);
		}

		final IndexFieldReference<F> reference;
		final String relativeFieldName;
		final Class<F> type;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName,
				Class<F> type) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
			this.type = type;
		}
	}
}
