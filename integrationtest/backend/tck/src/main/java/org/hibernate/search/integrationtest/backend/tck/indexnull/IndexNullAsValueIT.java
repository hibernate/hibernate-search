/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.indexnull;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

public class IndexNullAsValueIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String ANOTHER_INDEX_NAME = "AnotherIndexName";

	private static final String DOCUMENT_WITH_INDEX_NULL_AS_VALUES = "documentWithIndexNullAsValues";
	private static final String DOCUMENT_WITH_DIFFERENT_VALUES = "documentWithDifferentValues";
	private static final String DOCUMENT_WITH_NULL_VALUES = "documentWithNullValues";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Test
	public void indexNullAsValue_match() {
		setUp();
		StubMappingSearchScope scope = indexManager.createSearchScope();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.matchFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.indexNullAsValue.indexedValue;

			SearchQuery<DocumentReference> query = scope.query()
					.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_WITH_INDEX_NULL_AS_VALUES, DOCUMENT_WITH_NULL_VALUES );
		}
	}

	@Test
	public void indexNullAsValue_spatial() {
		Assume.assumeTrue(
				"indexNullAs on a GeoPoint field must be supported",
				TckConfiguration.get().getBackendFeatures().geoPointIndexNullAs()
		);

		setUp();
		SearchQuery<DocumentReference> query = indexManager.createSearchScope().query()
				.predicate( f -> f.spatial().within().onField( "geoPointField" ).circle( GeoPoint.of( 0.0, 0.0 ), 1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_WITH_INDEX_NULL_AS_VALUES, DOCUMENT_WITH_NULL_VALUES );
	}

	@Test
	public void indexNullAsValue_fullText() {
		SubTest.expectException( () -> setupHelper.withDefaultConfiguration()
				.withIndex( ANOTHER_INDEX_NAME, ctx -> ctx.getSchemaElement()
								.field( "fullTextField", c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ).indexNullAs( "bla bla bla" ) )
								.toReference()
						, ignored -> {
						} )
				.setup()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Index-null-as option is not supported on analyzed field." )
				.hasMessageContaining( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
				.hasMessageContaining( "bla bla bla" );
	}

	private void setUp() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add(
				referenceProvider( DOCUMENT_WITH_INDEX_NULL_AS_VALUES ),
				document -> {
					indexMapping.matchFieldModels.forEach( f -> f.indexNullAsValue.write( document ) );
					if ( indexMapping.geoPointField != null ) {
						document.addValue( indexMapping.geoPointField, GeoPoint.of( 0.0, 0.0 ) );
					}
				}
		);
		workPlan.add(
				referenceProvider( DOCUMENT_WITH_DIFFERENT_VALUES ),
				document -> {
					indexMapping.matchFieldModels.forEach( f -> f.differentValue.write( document ) );
					if ( indexMapping.geoPointField != null ) {
						document.addValue( indexMapping.geoPointField, GeoPoint.of( 40, 70 ) );
					}
				}
		);
		workPlan.add(
				referenceProvider( DOCUMENT_WITH_NULL_VALUES ),
				document -> {
					indexMapping.matchFieldModels.forEach( f -> f.nullValue.write( document ) );
					if ( indexMapping.geoPointField != null ) {
						document.addValue( indexMapping.geoPointField, null );
					}
				}
		);
		workPlan.execute().join();
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> matchFieldModels;
		final IndexFieldReference<GeoPoint> geoPointField;

		IndexMapping(IndexSchemaElement root) {
			matchFieldModels = FieldTypeDescriptor.getAll().stream()
					.filter( typeDescriptor -> typeDescriptor.getIndexNullAsMatchPredicateExpectations().isPresent() )
					.map( typeDescriptor -> ByTypeFieldModel.mapper( root, typeDescriptor ) )
					.collect( Collectors.toList() );

			if ( TckConfiguration.get().getBackendFeatures().geoPointIndexNullAs() ) {
				geoPointField = root.field(
						"geoPointField",
						c -> c.asGeoPoint().indexNullAs( GeoPoint.of( 0.0, 0.0 ) )
				)
						.toReference();
			}
			else {
				geoPointField = null;
			}
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> ByTypeFieldModel<F> mapper(IndexSchemaElement root, FieldTypeDescriptor<F> typeDescriptor) {
			IndexNullAsMatchPredicateExpectactions<F> expectations = typeDescriptor.getIndexNullAsMatchPredicateExpectations().get();
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

		public ByTypeFieldModel(IndexFieldReference<F> reference, String relativeFieldName, IndexNullAsMatchPredicateExpectactions<F> expectations) {
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
