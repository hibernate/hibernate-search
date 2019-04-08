/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IndexNullAsMappingIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_WITH_INDEX_NULL_AS_VALUES = "documentWithIndexNullAsValues";
	private static final String DOCUMENT_WITH_DIFFERENT_VALUES = "documentWithDifferentValues";
	private static final String DOCUMENT_WITH_NULL_VALUES = "documentWithNullValues";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void before() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void indexNullAsValue() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.indexNullAsValue.indexedValue;

			IndexSearchQuery<DocumentReference> query = scope.query()
					.asReference()
					.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_WITH_INDEX_NULL_AS_VALUES, DOCUMENT_WITH_NULL_VALUES );
		}
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add(
				referenceProvider( DOCUMENT_WITH_INDEX_NULL_AS_VALUES ),
				document -> indexMapping.supportedFieldModels.forEach( f -> f.indexNullAsValue.write( document ) )
		);
		workPlan.add(
				referenceProvider( DOCUMENT_WITH_DIFFERENT_VALUES ),
				document -> indexMapping.supportedFieldModels.forEach( f -> f.differentValue.write( document ) )
		);
		workPlan.add(
				referenceProvider( DOCUMENT_WITH_NULL_VALUES ),
				document -> indexMapping.supportedFieldModels.forEach( f -> f.nullValue.write( document ) )
		);
		workPlan.execute().join();
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = FieldTypeDescriptor.getAll().stream()
					.filter( typeDescriptor -> typeDescriptor.getIndexNullAsExpectations().isPresent() )
					.map( typeDescriptor -> ByTypeFieldModel.mapper( root, typeDescriptor ) )
					.collect( Collectors.toList() );
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> ByTypeFieldModel<F> mapper(IndexSchemaElement root, FieldTypeDescriptor<F> typeDescriptor) {
			IndexNullAsExpectactions<F> expectations = typeDescriptor.getIndexNullAsExpectations().get();
			F indexNullAsValue = expectations.getIndexNullAsValue();

			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(reference, name) -> new ByTypeFieldModel<>( reference, name, expectations )
			).map( root, "field_" + typeDescriptor.getUniqueName(), c -> c.indexNullAs( indexNullAsValue ) );
		}

		final String relativeFieldName;
		final ValueModel<F> indexNullAsValue;
		final ValueModel<F> differentValue;
		final ValueModel<F> nullValue;

		public ByTypeFieldModel(IndexFieldReference<F> reference, String relativeFieldName, IndexNullAsExpectactions<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.indexNullAsValue = new ValueModel<>( reference, expectations.getIndexNullAsValue() );
			this.differentValue = new ValueModel<>( reference, expectations.getDifferentValue() );
			this.nullValue = new ValueModel<>( reference, null );
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldReference<F> reference;
		final F indexedValue;

		private ValueModel(IndexFieldReference<F> reference, F indexedValue) {
			this.reference = reference;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			target.addValue( reference, indexedValue );
		}
	}

}
