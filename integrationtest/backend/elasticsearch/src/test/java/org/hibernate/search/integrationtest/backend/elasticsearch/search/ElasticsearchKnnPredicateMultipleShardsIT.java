/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.function.Consumer;

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

class ElasticsearchKnnPredicateMultipleShardsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index =
			SimpleMappedIndex.of( IndexBinding::new ).name( "multipleShardsKnnSearchPredicate" );

	@BeforeAll
	static void setup() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearch(),
				"These tests only make sense for a backend where Vector Search is supported and implemented."
		);
		setupHelper.start( tckBackendHelper -> tckBackendHelper.createHashBasedShardingBackendSetupStrategy( 4 ) )
				.withIndexes( index ).setup();
		BulkIndexer exampleKnnSearchIndexer = index.bulkIndexer();
		dataset.accept( exampleKnnSearchIndexer );
		exampleKnnSearchIndexer.join();
	}


	@Test
	void moreThanK() {
		int k = 3;
		SearchQuery<Object> query = index.createScope().query()
				.select( SearchProjectionFactory::id )
				.where( f -> f.knn( k ).field( "location" ).matching( 5f, 4f ) )
				.toQuery();

		List<Object> result = query.fetchAll().hits();

		assertThat( result )
				.containsOnly( "ID:2", "ID:1", "ID:3", "ID:4", "ID:6", "ID:5", "ID:7", "ID:12", "ID:9", "ID:17", "ID:13",
						"ID:20" );
	}

	@Test
	void knnDoesNotMatchOtherPredicate() {
		int k = 1;
		SearchQuery<Object> query = index.createScope().query()
				.select( SearchProjectionFactory::id )
				.where( f -> f.bool()
						.must( f.range().field( "rating" ).greaterThan( 9 ) )
						.must( f.knn( k ).field( "location" ).matching( 5f, 4f ) ) )
				.toQuery();

		List<Object> result = query.fetchAll().hits();

		assertThat( result ).isEmpty();
	}

	@Test
	void knnWithShould() {
		int k = 1;
		SearchQuery<Object> query = index.createScope().query()
				.select( SearchProjectionFactory::id )
				.where( f -> f.bool()
						.should( f.range().field( "rating" ).greaterThan( 9 ).boost( 1000.0f ) )
						.should( f.knn( k ).field( "location" ).matching( 5f, 4f ) ) )
				.toQuery();

		List<Object> result = query.fetchAll().hits();

		assertThat( result ).contains(
				// we have 12 docs from rating clause
				"ID:15",
				"ID:22",
				"ID:20",
				"ID:14",
				"ID:17",
				"ID:13",
				"ID:21",
				"ID:16",
				"ID:23",
				"ID:19",
				"ID:24",
				"ID:18",
				// and 4 are based on the knn predicate (k = 1 and shards = 4)
				"ID:2",
				"ID:1",
				"ID:3",
				"ID:4"
		);
	}

	private static class IndexBinding {

		final IndexFieldReference<Integer> rating;
		final IndexFieldReference<float[]> location;

		IndexBinding(IndexSchemaElement root) {
			rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			location = root.field( "location", f -> f.asFloatVector().dimension( 2 ).vectorSimilarity( VectorSimilarity.L2 ) )
					.toReference();
		}
	}

	private static final Consumer<BulkIndexer> dataset = bulkIndexer -> bulkIndexer
			.add( "ID:1", document -> {
				document.addValue( index.binding().location, new float[] { 5.2f, 4.4f } );
				document.addValue( index.binding().rating, 5 );
			} )
			.add( "ID:2", document -> {
				document.addValue( index.binding().location, new float[] { 5.2f, 3.9f } );
				document.addValue( index.binding().rating, 4 );
			} )
			.add( "ID:3", document -> {
				document.addValue( index.binding().location, new float[] { 4.9f, 3.4f } );
				document.addValue( index.binding().rating, 9 );
			} )
			.add( "ID:4", document -> {
				document.addValue( index.binding().location, new float[] { 4.2f, 4.6f } );
				document.addValue( index.binding().rating, 6 );
			} )
			.add( "ID:5", document -> {
				document.addValue( index.binding().location, new float[] { 3.3f, 4.5f } );
				document.addValue( index.binding().rating, 8 );
			} )
			.add( "ID:6", document -> {
				document.addValue( index.binding().location, new float[] { 6.4f, 3.4f } );
				document.addValue( index.binding().rating, 9 );
			} )
			.add( "ID:7", document -> {
				document.addValue( index.binding().location, new float[] { 4.2f, 6.2f } );
				document.addValue( index.binding().rating, 5 );
			} )
			.add( "ID:8", document -> {
				document.addValue( index.binding().location, new float[] { 2.4f, 4.0f } );
				document.addValue( index.binding().rating, 8 );
			} )
			.add( "ID:9", document -> {
				document.addValue( index.binding().location, new float[] { 1.4f, 3.2f } );
				document.addValue( index.binding().rating, 5 );
			} )
			.add( "ID:10", document -> {
				document.addValue( index.binding().location, new float[] { 7.0f, 9.9f } );
				document.addValue( index.binding().rating, 9 );
			} )
			.add( "ID:11", document -> {
				document.addValue( index.binding().location, new float[] { 3.0f, 2.3f } );
				document.addValue( index.binding().rating, 6 );
			} )
			.add( "ID:12", document -> {
				document.addValue( index.binding().location, new float[] { 5.0f, 1.0f } );
				document.addValue( index.binding().rating, 3 );
			} )

			.add( "ID:13", document -> {
				document.addValue( index.binding().location, new float[] { 50.0f, 10.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:14", document -> {
				document.addValue( index.binding().location, new float[] { 50.0f, 60.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:15", document -> {
				document.addValue( index.binding().location, new float[] { 50.0f, 50.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:16", document -> {
				document.addValue( index.binding().location, new float[] { 20.0f, 50.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:17", document -> {
				document.addValue( index.binding().location, new float[] { 30.0f, 40.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:18", document -> {
				document.addValue( index.binding().location, new float[] { 20.0f, 10.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:19", document -> {
				document.addValue( index.binding().location, new float[] { 20.0f, 20.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:20", document -> {
				document.addValue( index.binding().location, new float[] { 20.0f, 50.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:21", document -> {
				document.addValue( index.binding().location, new float[] { 60.0f, 10.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:22", document -> {
				document.addValue( index.binding().location, new float[] { 70.0f, 20.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:23", document -> {
				document.addValue( index.binding().location, new float[] { 60.0f, 30.0f } );
				document.addValue( index.binding().rating, 10 );
			} )
			.add( "ID:24", document -> {
				document.addValue( index.binding().location, new float[] { 60.0f, 40.0f } );
				document.addValue( index.binding().rating, 10 );
			} );
}
