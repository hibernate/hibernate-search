/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

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
public class IndexingFieldTypesIT<F> {

	private static final List<FieldTypeDescriptor<?>> supportedTypeDescriptors = FieldTypeDescriptor.getAll();

	@Parameterized.Parameters(name = "{0}")
	public static List<FieldTypeDescriptor<?>> parameters() {
		return supportedTypeDescriptors;
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final FieldTypeDescriptor<F> typeDescriptor;

	public IndexingFieldTypesIT(FieldTypeDescriptor<F> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
	}

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void withReference() {
		List<F> values = new ArrayList<>( this.typeDescriptor.getIndexableValues().getSingle() );
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
		plan.execute( OperationSubmitter.BLOCKING ).join();

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

	@Test
	public void withPath() {
		List<F> values = new ArrayList<>( this.typeDescriptor.getIndexableValues().getSingle() );
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
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// If we get here, indexing went well.
		// However, it may have failed silently... Let's check the documents are there, with the right value.

		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = fieldModel.relativeFieldName;

		SearchQuery<IdAndValue<F>> query = scope.query()
				.select( f -> f.composite()
						.from( f.entityReference(),
								f.field( absoluteFieldPath, typeDescriptor.getJavaType() ) )
						.as( (ref, val) -> new IdAndValue<>( ref.id(), val ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( expectedDocuments );
	}

	@Test
	public void dynamic_withPath() {
		assumeTrue(
				"This backend does not support dynamic fields for this type",
				TckConfiguration.get().getBackendFeatures()
						.supportsValuesForDynamicField( typeDescriptor.getJavaType() )
		);

		List<F> values = new ArrayList<>( this.typeDescriptor.getIndexableValues().getSingle() );
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
		plan.execute( OperationSubmitter.BLOCKING ).join();

		// If we get here, indexing went well.
		// However, it may have failed silently... Let's check the documents are there, with the right value.

		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = relativeFieldName;

		SearchQuery<IdAndValue<F>> query = scope.query()
				.select( f -> f.composite()
						.from( f.entityReference(),
								f.field( absoluteFieldPath, typeDescriptor.getJavaType() ) )
						.as( (ref, val) -> new IdAndValue<>( ref.id(), val ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder( expectedDocuments );
	}

	private class IndexBinding {
		final SimpleFieldModel<F> fieldModel;

		IndexBinding(IndexSchemaElement root) {
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
			return Objects.equals( documentId, that.documentId ) &&
					Objects.equals( fieldValue, that.fieldValue );
		}

		@Override
		public int hashCode() {
			return Objects.hash( documentId, fieldValue );
		}
	}
}
