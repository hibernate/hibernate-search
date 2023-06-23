/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding.NO_ADDITIONAL_CONFIGURATION;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.AbstractObjectBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior of projections on a single-valued field, common to all supported types.
 * <p>
 * See {@link FieldProjectionMultiValuedBaseIT} for multi-valued fields.
 */
@RunWith(Parameterized.class)
public class FieldProjectionSingleValuedBaseIT<F> {

	private static final List<FieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAll();
	private static List<DataSet<?>> dataSets;

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] parameters() {
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( FieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
			for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
				if ( fieldStructure.isMultiValued() ) {
					continue;
				}
				DataSet<?> dataSet = new DataSet<>( fieldStructure, fieldType );
				dataSets.add( dataSet );
				parameters.add( new Object[] { fieldStructure, fieldType, dataSet } );
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.createWithSingleValuedNestedFields(
					root,
					supportedFieldTypes,
					TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
							? NO_ADDITIONAL_CONFIGURATION
							: c -> c.projectable( Projectable.YES )
			);

	private static final SimpleMappedIndex<SingleFieldIndexBinding> index = SimpleMappedIndex.of( bindingFactory );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.contribute( indexer );
		}
		indexer.join();
	}

	private final TestedFieldStructure fieldStructure;
	private final FieldTypeDescriptor<F> fieldType;
	private final DataSet<F> dataSet;

	public FieldProjectionSingleValuedBaseIT(TestedFieldStructure fieldStructure,
			FieldTypeDescriptor<F> fieldType, DataSet<F> dataSet) {
		this.fieldStructure = fieldStructure;
		this.fieldType = fieldType;
		this.dataSet = dataSet;
	}

	@Test
	public void simple() {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValue( 1 ),
						dataSet.getFieldValue( 2 ),
						dataSet.getFieldValue( 3 ),
						null // Empty document
				);
	}

	@Test
	public void noClass() {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValue( 1 ),
						dataSet.getFieldValue( 2 ),
						dataSet.getFieldValue( 3 ),
						null // Empty document
				);
	}

	/**
	 * Test requesting a multi-valued projection on a single-valued field.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3391")
	public void multi() {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath();

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ).multi() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						Collections.singletonList( dataSet.getFieldValue( 1 ) ),
						Collections.singletonList( dataSet.getFieldValue( 2 ) ),
						Collections.singletonList( dataSet.getFieldValue( 3 ) ),
						// Empty document
						TckConfiguration.get().getBackendFeatures().projectionPreservesNulls()
								? Collections.singletonList( null )
								: Collections.emptyList()
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@Test
	public void duplicated() {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath();

		assertThatQuery( scope.query()
				.select( f -> f.composite(
						f.field( fieldPath, fieldType.getJavaType() ),
						f.field( fieldPath, fieldType.getJavaType() )
				)
				)
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						Arrays.asList( dataSet.getFieldValue( 1 ), dataSet.getFieldValue( 1 ) ),
						Arrays.asList( dataSet.getFieldValue( 2 ), dataSet.getFieldValue( 2 ) ),
						Arrays.asList( dataSet.getFieldValue( 3 ), dataSet.getFieldValue( 3 ) ),
						Arrays.asList( null, null ) // Empty document
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot() {
		AbstractObjectBinding parentObjectBinding = index.binding().getParentObject( fieldStructure );

		assumeTrue( "This test is only relevant when the field is located on an object field",
				parentObjectBinding.absolutePath != null );

		assertThatQuery( index.query()
				.select( f -> f.withRoot( parentObjectBinding.absolutePath )
						.field( parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ),
								fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValue( 1 ),
						dataSet.getFieldValue( 2 ),
						dataSet.getFieldValue( 3 ),
						null // Empty document
				);
	}

	private String getFieldPath() {
		return index.binding().getFieldPath( fieldStructure, fieldType );
	}

	private static class DataSet<F> {
		private final TestedFieldStructure fieldStructure;
		private final FieldTypeDescriptor<F> fieldType;
		private final String routingKey;

		private DataSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F> fieldType) {
			this.fieldStructure = fieldStructure;
			this.fieldType = fieldType;
			this.routingKey = fieldType.getUniqueName() + "_" + fieldStructure.getUniqueName();
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer indexer) {
			indexer.add( documentProvider( emptyDocId( 1 ), routingKey,
					document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
							document, null ) ) );
			indexer.add( documentProvider( docId( 1 ), routingKey,
					document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
							document, getFieldValue( 1 ) ) ) );
			indexer.add( documentProvider( docId( 2 ), routingKey,
					document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
							document, getFieldValue( 2 ) ) ) );
			indexer.add( documentProvider( docId( 3 ), routingKey,
					document -> index.binding().initSingleValued( fieldType, fieldStructure.location,
							document, getFieldValue( 3 ) ) ) );
		}

		private F getFieldValue(int documentNumber) {
			return fieldType.getIndexableValues().getSingle().get( documentNumber - 1 );
		}
	}
}
