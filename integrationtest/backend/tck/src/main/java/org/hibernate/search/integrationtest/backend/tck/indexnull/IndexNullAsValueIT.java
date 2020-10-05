/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.indexnull;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

public class IndexNullAsValueIT {

	private static final String DOCUMENT_WITH_INDEX_NULL_AS_VALUES = "documentWithIndexNullAsValues";
	private static final String DOCUMENT_WITH_DIFFERENT_VALUES = "documentWithDifferentValues";
	private static final String DOCUMENT_WITH_NULL_VALUES = "documentWithNullValues";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Test
	public void indexNullAsValue_match() {
		setUp();
		StubMappingScope scope = index.createScope();

		for ( ByTypeFieldModel<?> fieldModel : index.binding().matchFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.indexNullAsValue.indexedValue;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.match().field( absoluteFieldPath ).matching( valueToMatch ) )
					.toQuery();

			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_WITH_INDEX_NULL_AS_VALUES, DOCUMENT_WITH_NULL_VALUES );
		}
	}

	@Test
	public void indexNullAsValue_spatial() {
		Assume.assumeTrue(
				"indexNullAs on a GeoPoint field must be supported",
				TckConfiguration.get().getBackendFeatures().geoPointIndexNullAs()
		);

		setUp();
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.spatial().within().field( "geoPointField" ).circle( GeoPoint.of( 0.0, 0.0 ), 1 ) )
				.toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_WITH_INDEX_NULL_AS_VALUES, DOCUMENT_WITH_NULL_VALUES );
	}

	@Test
	public void indexNullAsValue_fullText() {
		Assertions.assertThatThrownBy( () -> setupHelper.start()
				.withIndex( StubMappedIndex.ofNonRetrievable(
						root -> root.field(
								"fullTextField",
								c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name
								)
										.indexNullAs( "bla bla bla" ) )
										.toReference()
				) )
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Index-null-as option is not supported on analyzed field." )
				.hasMessageContaining( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
				.hasMessageContaining( "bla bla bla" );
	}

	private void setUp() {
		setupHelper.start().withIndexes( index ).setup();

		initData();
	}

	private void initData() {
		index.bulkIndexer()
				.add(
						DOCUMENT_WITH_INDEX_NULL_AS_VALUES,
						document -> {
							index.binding().matchFieldModels.forEach( f -> f.indexNullAsValue.write( document ) );
							if ( index.binding().geoPointField != null ) {
								document.addValue( index.binding().geoPointField, GeoPoint.of( 0.0, 0.0 ) );
							}
						}
				)
				.add(
						DOCUMENT_WITH_DIFFERENT_VALUES,
						document -> {
							index.binding().matchFieldModels.forEach( f -> f.differentValue.write( document ) );
							if ( index.binding().geoPointField != null ) {
								document.addValue( index.binding().geoPointField, GeoPoint.of( 40, 70 ) );
							}
						}
				)
				.add(
						DOCUMENT_WITH_NULL_VALUES,
						document -> {
							index.binding().matchFieldModels.forEach( f -> f.nullValue.write( document ) );
							if ( index.binding().geoPointField != null ) {
								document.addValue( index.binding().geoPointField, null );
							}
						}
				)
				.join();
	}

	private static class IndexBinding {
		final List<ByTypeFieldModel<?>> matchFieldModels;
		final IndexFieldReference<GeoPoint> geoPointField;

		IndexBinding(IndexSchemaElement root) {
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
