/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchKnnPredicateSpecificsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<PredicateIndexBinding> index =
			SimpleMappedIndex.of( PredicateIndexBinding::new ).name( "exampleKnnSearchPredicate" );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndexes( index ).setup();
		BulkIndexer exampleKnnSearchIndexer = index.bulkIndexer();
		dataset.accept( exampleKnnSearchIndexer );
		exampleKnnSearchIndexer.join();
	}


	@Test
	void useElasticsearchSpecificKnnWithNumberOfCandidatesOption() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearchNumberOfCandidates(),
				"This test is only for an Elasticsearch distribution where we cannot add knn as a predicate inside other predicate."
		);
		int k = 3;
		SearchQuery<Object> query = index.createScope().query()
				.select(
						SearchProjectionFactory::id
				)
				.where( f -> f.extension( ElasticsearchExtension.get() )
						.knn( k )
						.field( "location" )
						.matching( 5f, 4f )
						.numberOfCandidates( 123 )
				).toQuery();

		List<Object> result = query.fetchAll().hits();

		assertThat( result ).hasSize( k )
				.containsOnly( "ID:2", "ID:1", "ID:3" );
	}

	private static class PredicateIndexBinding {

		final IndexFieldReference<Boolean> parking;
		final IndexFieldReference<Integer> rating;
		final IndexFieldReference<float[]> location;

		PredicateIndexBinding(IndexSchemaElement root) {
			parking = root.field( "parking", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
			rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			location = root.field( "location", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES )
					.maxConnections( 16 ).beamWidth( 100 ).vectorSimilarity( VectorSimilarity.L2 ) ).toReference();
		}

	}

	private static final Consumer<BulkIndexer> dataset = bulkIndexer -> bulkIndexer
			.add( "ID:1", document -> {
				document.addValue( index.binding().location, new float[] { 5.2f, 4.4f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 5 );
			} )
			.add( "ID:2", document -> {
				document.addValue( index.binding().location, new float[] { 5.2f, 3.9f } );
				document.addValue( index.binding().parking, false );
				document.addValue( index.binding().rating, 4 );
			} )
			.add( "ID:3", document -> {
				document.addValue( index.binding().location, new float[] { 4.9f, 3.4f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 9 );
			} )
			.add( "ID:4", document -> {
				document.addValue( index.binding().location, new float[] { 4.2f, 4.6f } );
				document.addValue( index.binding().parking, false );
				document.addValue( index.binding().rating, 6 );
			} )
			.add( "ID:5", document -> {
				document.addValue( index.binding().location, new float[] { 3.3f, 4.5f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 8 );
			} )
			.add( "ID:6", document -> {
				document.addValue( index.binding().location, new float[] { 6.4f, 3.4f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 9 );
			} )
			.add( "ID:7", document -> {
				document.addValue( index.binding().location, new float[] { 4.2f, 6.2f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 5 );
			} )
			.add( "ID:8", document -> {
				document.addValue( index.binding().location, new float[] { 2.4f, 4.0f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 8 );
			} )
			.add( "ID:9", document -> {
				document.addValue( index.binding().location, new float[] { 1.4f, 3.2f } );
				document.addValue( index.binding().parking, false );
				document.addValue( index.binding().rating, 5 );
			} )
			.add( "ID:10", document -> {
				document.addValue( index.binding().location, new float[] { 7.0f, 9.9f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 9 );
			} )
			.add( "ID:11", document -> {
				document.addValue( index.binding().location, new float[] { 3.0f, 2.3f } );
				document.addValue( index.binding().parking, false );
				document.addValue( index.binding().rating, 6 );
			} )
			.add( "ID:12", document -> {
				document.addValue( index.binding().location, new float[] { 5.0f, 1.0f } );
				document.addValue( index.binding().parking, true );
				document.addValue( index.binding().rating, 3 );
			} );
}
