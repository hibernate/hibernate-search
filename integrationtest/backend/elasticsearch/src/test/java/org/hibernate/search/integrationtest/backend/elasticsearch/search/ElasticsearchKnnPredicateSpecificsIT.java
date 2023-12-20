/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.ThrowableAssert;

class ElasticsearchKnnPredicateSpecificsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<PredicateIndexBinding> index =
			SimpleMappedIndex.of( PredicateIndexBinding::new ).name( "exampleKnnSearchPredicate" );

	@BeforeAll
	static void setup() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearch(),
				"These tests only make sense for a backend where Vector Search is supported and implemented."
		);
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

	@Test
	void knnPredicateInWrongPlace_knnAsFilter() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.knn( 5 ).field( "location" ).matching( 5.0f, 5.0f )
								.filter( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolMustNot() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().mustNot( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolMustIsNotWrong() {
		// Since we are doing optimizations, and also because we are adding only a single knn must, it is actually ok to do so:
		assertThat(
				index.query().select().where(
						f -> f.bool().must( f.knn( 1 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery().fetchAllHits()
		).hasSize( 1 );
	}

	@Test
	void knnPredicateInWrongPlace_boolMultipleMust() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().must( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
								.must( f.knn( 15 ).field( "location" ).matching( 5.0f, 5.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolFilter() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().filter( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolBoolFilter() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.must( f.bool().filter( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolBoolShould() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.must( f.bool().should( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolBoolMust() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.mustNot( f.bool().must( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_boolBoolMustNot() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.mustNot( f.bool().mustNot( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_nested() {
		knnPredicateInWrongPlace(
				() -> index.query().select().where(
						f -> f.nested( "object" )
								.add( f.bool().must( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInWrongPlace_aggregation() {
		knnPredicateInWrongPlace(
				() -> {
					AggregationKey<Map<Boolean, Long>> countsByParking = AggregationKey.of( "countsByParking" );

					index.query().select()
							.where( f -> f.matchAll() )
							.aggregation( countsByParking, agg -> agg.terms().field( "object.nestedParking", Boolean.class )
									.filter( f -> f.knn( 10 ).field( "location" ).matching(50.0f, 50.0f) ) )
							.toQuery();
				} );
	}

	@Test
	void knnPredicateInWrongPlace_sorting() {
		knnPredicateInWrongPlace(
				() -> {
					index.query().select()
							.where( f -> f.matchAll() )
							.sort( s -> s.field( "object.nestedRating" )
									.filter( f -> f.knn( 10 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
							.toQuery();
				} );
	}

	void knnPredicateInWrongPlace(ThrowableAssert.ThrowingCallable query) {
		assumeFalse(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearchInsideOtherPredicates(),
				"This test is only for an Elasticsearch distribution where we cannot add knn as a predicate inside other predicate."
		);
		assertThatThrownBy( query )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A knn predicate cannot be added",
						"With Elasticsearch, a knn predicate can only be a top-level predicate or a should clause of a top-level bool predicate."
				);
	}

	private static class PredicateIndexBinding {

		final IndexFieldReference<Boolean> parking;
		final IndexFieldReference<Integer> rating;
		final IndexFieldReference<float[]> location;
		final IndexObjectFieldReference object;
		final IndexFieldReference<Boolean> nestedParking;
		final IndexFieldReference<Integer> nestedRating;

		PredicateIndexBinding(IndexSchemaElement root) {
			parking = root.field( "parking", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
			rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			location = root.field( "location", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES )
					.maxConnections( 16 ).beamWidth( 100 ).vectorSimilarity( VectorSimilarity.L2 ) ).toReference();
			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedParking = nested.field( "nestedParking", f -> f.asBoolean().aggregable( Aggregable.YES ) ).toReference();
			nestedRating = nested.field( "nestedRating", f -> f.asInteger().projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();
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
