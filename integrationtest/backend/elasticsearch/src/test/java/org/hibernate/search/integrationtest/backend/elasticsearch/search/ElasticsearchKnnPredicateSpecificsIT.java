/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

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
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

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
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearch(),
				"These tests only make sense for a backend where Vector Search is supported and implemented."
		);
		setupHelper.start( tckBackendHelper -> tckBackendHelper.createHashBasedShardingBackendSetupStrategy( 1 ) )
				.withIndexes( index ).setup();
		BulkIndexer exampleKnnSearchIndexer = index.bulkIndexer();
		dataset.accept( exampleKnnSearchIndexer );
		exampleKnnSearchIndexer.join();
	}

	@Test
	void knnPredicateInOkPlace_knnAsFilter() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.knn( 5 ).field( "location" ).matching( 5.0f, 5.0f )
								.filter( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolMustNot() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().mustNot( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolMustIsNotWrong() {
		// Since we are doing optimizations, and also because we are adding only a single knn must, it is actually ok to do so:
		assertThat(
				index.query().select().where(
						f -> f.bool().must( f.knn( 1 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery().fetchAllHits()
		).hasSize( 1 );
	}

	@Test
	void knnPredicateInOkPlace_boolMultipleMust() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().must( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
								.must( f.knn( 15 ).field( "location" ).matching( 5.0f, 5.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolFilter() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().filter( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolBoolFilter() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.must( f.bool().filter( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolBoolShould() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.must( f.bool().should( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolBoolMust() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.mustNot( f.bool().must( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_boolBoolMustNot() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.bool().must( f.matchAll() )
								.mustNot( f.bool().mustNot( f.knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_nested() {
		knnPredicateIsOk(
				index.query().select().where(
						f -> f.nested( "object" )
								.add( f.bool().must( f.knn( 15 ).field( "object.location" ).matching( 50.0f, 50.0f ) ) )
				).toQuery() );
	}

	@Test
	void knnPredicateInOkPlace_addingPrebuiltKnn() {
		StubMappingScope scope = index.createScope();

		// all good:
		SearchPredicate inlineKnn = scope.predicate().knn( 15 ).field( "location" ).matching( 50.0f, 50.0f ).toPredicate();
		SearchPredicate namedKnn = scope.predicate().named( "knn-named" )
				.param( "k", 25 )
				.param( "vector", new float[] { 50.0f, 50.0f } ).toPredicate();

		for ( SearchPredicate knn : Arrays.asList( inlineKnn, namedKnn ) ) {
			// adding knn to non-should bool clauses is ok,
			knnPredicateIsOk( scope.predicate().bool().must( knn ) );
			knnPredicateIsOk( scope.predicate().bool().mustNot( knn ) );
			knnPredicateIsOk( scope.predicate().bool().filter( knn ) );

			knnPredicateIsOk( scope.predicate().bool()
					// should is ok
					.should( knn )
					// ok since we are already have a knn in should we can add more should clauses
					.must( scope.predicate().match().field( "parking" ).matching( Boolean.TRUE ) ) );
			knnPredicateIsOk( scope.predicate().bool()
					// so far so good:
					.must( scope.predicate().match().field( "parking" ).matching( Boolean.TRUE ) )
					// add a knn through should while we already have a non-should clause
					.should( knn ) );
			knnPredicateIsOk( scope.predicate().bool()
					// so far so good:
					.mustNot( scope.predicate().match().field( "parking" ).matching( Boolean.TRUE ) )
					// add a knn through should while we already have a non-should clause
					.should( knn ) );
			knnPredicateIsOk( scope.predicate().bool()
					// so far so good:
					.filter( scope.predicate().match().field( "parking" ).matching( Boolean.TRUE ) )
					// add a knn through should while we already have a non-should clause
					.should( knn ) );

			// we add multiple clauses to prevent "optimizations" leading to bool predicate being replaced by a simple knn predicate
			SearchPredicate inlineBoolKnnInShould = scope.predicate().bool().should( knn )
					.should( scope.predicate().match().field( "parking" ).matching( Boolean.TRUE ) ).toPredicate();
			SearchPredicate namedBoolKnnInShould = scope.predicate().named( "bool-knn-in-should-named" )
					.param( "knn", knn )
					.toPredicate();

			for ( SearchPredicate boolKnnInShould : Arrays.asList( inlineBoolKnnInShould, namedBoolKnnInShould ) ) {
				// adding boolean predicate with a should knn clause as any boolean clause (nesting a correct bool into another one)
				//  is ok !
				knnPredicateIsOk( scope.predicate().bool().should( boolKnnInShould ) );
				knnPredicateIsOk( scope.predicate().bool().must( boolKnnInShould ) );
				knnPredicateIsOk( scope.predicate().bool().mustNot( boolKnnInShould ) );
				knnPredicateIsOk( scope.predicate().bool().filter( boolKnnInShould ) );

				// adding as a knn filter:
				knnPredicateIsOk( scope.predicate().knn( 10 ).field( "location" ).matching( 50.0f, 50.0f ).filter( knn ) );
				knnPredicateIsOk(
						scope.predicate().knn( 10 ).field( "location" ).matching( 50.0f, 50.0f ).filter( boolKnnInShould ) );
			}
		}
	}

	@Test
	void knnPredicateInOkPlace_aggregation() {
		AggregationKey<Map<Boolean, Long>> countsByParking = AggregationKey.of( "countsByParking" );
		knnPredicateIsOk(
				index.query().select()
						.where( f -> f.matchAll() )
						.aggregation( countsByParking, agg -> agg.terms().field( "object.nestedParking", Boolean.class )
								.filter( f -> f.knn( 10 ).field( "object.location" ).matching( 50.0f, 50.0f ) ) )
						.toQuery()
		);
	}

	@Test
	void knnPredicateInOkPlace_sorting() {
		knnPredicateIsOk(
				index.query().select()
						.where( f -> f.matchAll() )
						.sort( s -> s.field( "object.nestedRating" )
								.filter( f -> f.knn( 10 ).field( "object.location" )
										.matching( 50.0f, 50.0f ) ) )
						.toQuery()
		);
	}

	void knnPredicateIsOk(SearchQuery<?> query) {
		assertThat( query.fetchAllHits() )
				.hasSizeGreaterThanOrEqualTo( 0 );
	}

	void knnPredicateIsOk(PredicateFinalStep predicate) {
		knnPredicateIsOk( index.query().select().where( predicate.toPredicate() ).toQuery() );
	}

	private static class PredicateIndexBinding {

		final IndexFieldReference<Boolean> parking;
		final IndexFieldReference<Integer> rating;
		final IndexFieldReference<float[]> location;
		final IndexObjectFieldReference object;
		final IndexFieldReference<Boolean> nestedParking;
		final IndexFieldReference<Integer> nestedRating;
		final IndexFieldReference<float[]> nestedLocation;

		PredicateIndexBinding(IndexSchemaElement root) {
			parking = root.field( "parking", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
			rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			location = root.field( "location", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES )
					.m( 16 ).efConstruction( 100 ).vectorSimilarity( VectorSimilarity.L2 ) ).toReference();
			IndexSchemaObjectField nested = root.objectField( "object", ObjectStructure.NESTED );
			object = nested.toReference();
			nestedParking = nested.field( "nestedParking", f -> f.asBoolean().aggregable( Aggregable.YES ) ).toReference();
			nestedRating =
					nested.field( "nestedRating", f -> f.asInteger().projectable( Projectable.YES ).sortable( Sortable.YES ) )
							.toReference();
			nestedLocation = nested.field( "location", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES )
					.m( 16 ).efConstruction( 100 ).vectorSimilarity( VectorSimilarity.L2 ) ).toReference();

			root.namedPredicate( "knn-named", context -> {
				int k = context.param( "k", Integer.class );
				float[] vector = context.param( "vector", float[].class );

				return context.predicate().knn( k ).field( "location" )
						.matching( vector )
						.toPredicate();
			} );

			root.namedPredicate( "bool-knn-in-should-named", context -> {
				SearchPredicate knn = context.param( "knn", SearchPredicate.class );

				return context.predicate().bool().should( knn )
						.should( context.predicate().match().field( "parking" ).matching( Boolean.TRUE ) ).toPredicate();
			} );
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
