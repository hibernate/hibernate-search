/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test indexing with various values.
 * <p>
 * Useful to test corner cases: 0, Integer.MAX_VALUE, empty string, date at epoch, february 29th, ...
 *
 * @param <F> The type of field values.
 */
@RunWith(Parameterized.class)
public class IndexingIT<F> {

	private static final String INDEX_NAME = "IndexName";

	@Parameterized.Parameters(name = "{0}")
	public static Object[] types() {
		return FieldTypeDescriptor.getAll().stream()
				.map( typeDescriptor -> new Object[] { typeDescriptor, typeDescriptor.getIndexingExpectations() } )
				.toArray();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final FieldTypeDescriptor<F> typeDescriptor;
	private final IndexingExpectations<F> expectations;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	public IndexingIT(FieldTypeDescriptor<F> typeDescriptor, Optional<IndexingExpectations<F>> expectations) {
		Assume.assumeTrue(
				"Type " + typeDescriptor + " does not define indexing expectations", expectations.isPresent()
		);
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations.get();
	}

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void index() {
		List<F> values = new ArrayList<>( expectations.getValues() );
		values.add( null ); // Also test null
		List<IdAndValue<F>> expectedDocuments = new ArrayList<>();

		// Index all values, each in its own document
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		for ( int i = 0; i < values.size(); i++ ) {
			String documentId = "document_" + i;
			F value = values.get( i );
			plan.add( referenceProvider( documentId ), document -> {
				document.addValue( indexMapping.fieldModel.reference, value );
			} );
			expectedDocuments.add( new IdAndValue<>( documentId, value ) );
		}
		plan.execute().join();

		// If we get here, indexing went well.
		// However, it may have failed silently... Let's check the documents are there, with the right value.

		StubMappingScope scope = indexManager.createScope();
		String absoluteFieldPath = indexMapping.fieldModel.relativeFieldName;

		for ( int i = 0; i < values.size(); i++ ) {
			SearchQuery<IdAndValue<F>> query = scope.query()
					.asProjection( f -> f.composite(
							(ref, val) -> new IdAndValue<>( ref.getId(), val ),
							f.entityReference(),
							f.field( absoluteFieldPath, typeDescriptor.getJavaType() )
					) )
					.where( f -> f.matchAll() )
					.toQuery();

			assertThat( query ).hasHitsAnyOrder( expectedDocuments );
		}
	}

	private class IndexMapping {
		final FieldModel<F> fieldModel;

		IndexMapping(IndexSchemaElement root) {
			this.fieldModel = FieldModel.mapper( typeDescriptor )
					.map( root, "field_" + typeDescriptor.getUniqueName(), c -> c.projectable( Projectable.YES ) );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					FieldModel::new
			);
		}

		final IndexFieldReference<F> reference;
		final String relativeFieldName;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}
	}

	private static class IdAndValue<F> {
		final String documentId;
		final F fieldValue;

		private IdAndValue(String documentId, F fieldValue) {
			this.documentId = documentId;
			this.fieldValue = fieldValue;
		}

		@Override
		public String toString() {
			return "IdAndValue{" +
					"documentId='" + documentId + '\'' +
					", fieldValue=" + fieldValue +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			IdAndValue<?> that = (IdAndValue<?>) o;
			return Objects.equals( documentId, that.documentId ) &&
					Objects.equals( fieldValue, that.fieldValue );
		}

		@Override
		public int hashCode() {
			return Objects.hash( documentId, fieldValue );
		}
	}
}
