/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test indexing with various values.
 * <p>
 * Useful to test corner cases: 0, Integer.MAX_VALUE, empty string, date at epoch, february 29th, ...
 *
 * @param <F> The type of field values.
 */
class IndexingFieldTypesIT<F> {

	private static final List<FieldTypeDescriptor<?>> supportedTypeDescriptors = FieldTypeDescriptor.getAll();

	public static List<? extends Arguments> params() {
		return supportedTypeDescriptors.stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private SimpleMappedIndex<IndexBinding> index;

	public void init(FieldTypeDescriptor<F> typeDescriptor) {
		index = SimpleMappedIndex.of(
				root -> new IndexBinding( root, typeDescriptor ) );
		setupHelper.start().withIndex( index ).setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withReference(FieldTypeDescriptor<F> typeDescriptor) {
		init( typeDescriptor );
		List<F> values = new ArrayList<>( typeDescriptor.getIndexableValues().getSingle() );
		values.add( null ); // Also test null
		List<IdAndValue<F>> expectedDocuments = new ArrayList<>();

		SimpleFieldModel<F> fieldModel = index.binding().fieldModel;

		// Index all values, each in its own document
		IndexIndexingPlan plan = index.createIndexingPlan();
		for ( int i = 0; i < values.size(); i++ ) {
			String documentId = "document_" + i;
			F value = values.get( i );
			plan.add( referenceProvider( documentId ), document -> {
				document.addValue( fieldModel.reference, value );
			} );
			expectedDocuments.add( new IdAndValue<>( documentId, value ) );
		}
		plan.execute( OperationSubmitter.blocking() ).join();

		// If we get here, indexing went well.
		// However, it may have failed silently... Let's check the documents are there, with the right value.

		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = fieldModel.relativeFieldName;

		SearchQuery<IdAndValue<F>> query = scope.query()
				.select( f -> f.composite()
						.from( f.id( String.class ),
								f.field( absoluteFieldPath, typeDescriptor.getJavaType() ) )
						.as( (id, val) -> new IdAndValue<>( id, val ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( expectedDocuments );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withPath(FieldTypeDescriptor<F> typeDescriptor) {
		init( typeDescriptor );
		List<F> values = new ArrayList<>( typeDescriptor.getIndexableValues().getSingle() );
		values.add( null ); // Also test null
		List<IdAndValue<F>> expectedDocuments = new ArrayList<>();

		SimpleFieldModel<F> fieldModel = index.binding().fieldModel;

		// Index all values, each in its own document
		IndexIndexingPlan plan = index.createIndexingPlan();
		for ( int i = 0; i < values.size(); i++ ) {
			String documentId = "document_" + i;
			F value = values.get( i );
			plan.add( referenceProvider( documentId ), document -> {
				document.addValue( fieldModel.relativeFieldName, value );
			} );
			expectedDocuments.add( new IdAndValue<>( documentId, value ) );
		}
		plan.execute( OperationSubmitter.blocking() ).join();

		// If we get here, indexing went well.
		// However, it may have failed silently... Let's check the documents are there, with the right value.

		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = fieldModel.relativeFieldName;

		SearchQuery<IdAndValue<F>> query = scope.query()
				.select( f -> f.composite()
						.from( f.documentReference(),
								f.field( absoluteFieldPath, typeDescriptor.getJavaType() ) )
						.as( (ref, val) -> new IdAndValue<>( ref.id(), val ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( expectedDocuments );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void dynamic_withPath(FieldTypeDescriptor<F> typeDescriptor) {
		init( typeDescriptor );
		List<F> values = new ArrayList<>( typeDescriptor.getIndexableValues().getSingle() );
		values.add( null ); // Also test null
		List<IdAndValue<F>> expectedDocuments = new ArrayList<>();

		// Matches the template defined in IndexBinding
		String relativeFieldName = "foo_" + typeDescriptor.getUniqueName();

		// Index all values, each in its own document
		IndexIndexingPlan plan = index.createIndexingPlan();
		for ( int i = 0; i < values.size(); i++ ) {
			String documentId = "document_" + i;
			F value = values.get( i );
			plan.add( referenceProvider( documentId ), document -> {
				document.addValue( relativeFieldName, value );
			} );
			expectedDocuments.add( new IdAndValue<>( documentId, value ) );
		}
		plan.execute( OperationSubmitter.blocking() ).join();

		// If we get here, indexing went well.
		// However, it may have failed silently... Let's check the documents are there, with the right value.

		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = relativeFieldName;

		SearchQuery<IdAndValue<F>> query = scope.query()
				.select( f -> f.composite()
						.from( f.documentReference(),
								f.field( absoluteFieldPath, typeDescriptor.getJavaType() ) )
						.as( (ref, val) -> new IdAndValue<>( ref.id(), val ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( expectedDocuments );
	}

	private class IndexBinding {
		final SimpleFieldModel<F> fieldModel;

		IndexBinding(IndexSchemaElement root, FieldTypeDescriptor<F> typeDescriptor) {
			this.fieldModel = SimpleFieldModel.mapper( typeDescriptor, c -> c.projectable( Projectable.YES ) )
					.map( root, "field" );
			supportedTypeDescriptors.forEach( fieldType -> {
				root.fieldTemplate( fieldType.getUniqueName(),
						f -> fieldType.configure( f ).projectable( Projectable.YES ) )
						.matchingPathGlob( "*_" + fieldType.getUniqueName() );
			} );
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
			return Objects.equals( documentId, that.documentId )
					&& Objects.equals( fieldValue, that.fieldValue );
		}

		@Override
		public int hashCode() {
			return Objects.hash( documentId, fieldValue );
		}
	}
}
