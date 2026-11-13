package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreBoostModeStep;
import org.hibernate.search.engine.search.predicate.dsl.FunctionScoreScoreModeStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.scorefunction.FieldValueFactorFieldReference;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class FunctionScorePredicateSpecificsIT {
	//CHECKSTYLE:ON

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( "1", document -> {
					document.addValue( index.binding().popularity, 10 );
					document.addValue( index.binding().rating, 5 );
					document.addValue( index.binding().decimalRating, new BigDecimal( "1.25" ) );
					document.addValue( index.binding().integerRating, BigInteger.valueOf( 1000 ) );
					document.addObject( index.binding().nested ).addValue( index.binding().nestedRating, 7 );
					document.addValue( index.binding().optionalRating, 5 );
					document.addValue( index.binding().featured, true );
					document.addValue( index.binding().label, "featured" );
					document.addValue( index.binding().text, "quick brown fox" );
				} )
				.add( "2", document -> {
					document.addValue( index.binding().popularity, 20 );
					document.addValue( index.binding().rating, 3 );
					document.addObject( index.binding().nested ).addValue( index.binding().nestedRating, 11 );
					document.addValue( index.binding().featured, false );
					document.addValue( index.binding().label, "ordinary" );
					document.addValue( index.binding().text, "brown quick fox" );
				} )
				.add( "3", document -> {
					document.addValue( index.binding().popularity, 5 );
					document.addValue( index.binding().rating, 8 );
					document.addValue( index.binding().decimalRating, new BigDecimal( "3.75" ) );
					document.addValue( index.binding().integerRating, BigInteger.valueOf( 3000 ) );
					document.addObject( index.binding().nested ).addValue( index.binding().nestedRating, 13 );
					document.addValue( index.binding().optionalRating, 8 );
					document.addValue( index.binding().featured, true );
					document.addValue( index.binding().label, "featured" );
					document.addValue( index.binding().text, "quick red brown fox" );
				} )
				.join();
	}

	@Nested
	class SingleFieldValueFactor {

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void rootFieldInsideExplicitNestingRejected(boolean withMissingValue) {
			var f = index.createScope().predicate();
			var function = f.functionScore().fieldValueFactor( "popularity" );
			if ( withMissingValue ) {
				function.missing( 42 );
			}
			assertThatThrownBy( () -> f.nested( "nested" ).add( function ).toPredicate() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "only fields that are contained in the nested object with path 'nested'" );
		}

		@Test
		void functionOnSiblingNestedPathRejected() {
			var f = index.createScope().predicate();
			assertThatThrownBy( () -> f.nested( "nested" )
					.add( f.functionScore().fieldValueFactor( "otherNested.rating" ) ).toPredicate() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "only fields that are contained in the nested object with path 'nested'" );
		}

		@Test
		void rootInnerPredicateWithNestedFunctionRejected() {
			assertThatThrownBy( () -> index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.match().field( "featured" ).matching( true ) )
							.fieldValueFactor( "nested.rating" ).boostMode().replace() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "only fields that are contained in the nested object with path 'nested'" );
		}

		@Test
		void nestedInnerPredicateWithNestedFunction() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.match().field( "nested.rating" ).matching( 7 ) )
							.fieldValueFactor( "nested.rating" ).boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactly( 7f );
		}

		@Test
		void scopedFieldFunctionFilterContributor() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( "popularity" )
							.filter( FunctionScorePredicateSpecificsIT::featuredFilter )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 10f, 1f, 5f );
		}

		@ParameterizedTest
		@ValueSource(strings = { "convertedRating", "optionalRating" })
		void missingFieldValueWithoutFallbackRejected(String fieldPath) {
			assertThatThrownBy( () -> index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( fieldPath ).boostMode().replace() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( fieldPath );
		}

		@Test
		void typedReferenceMissingUsesIndexValueModel() {
			var reference = new ScoreFunctionReference<>( "convertedRating", Integer.class, ValueModel.INDEX );
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( reference ).missing( 7 ).boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 7f, 7f, 7f );
		}

		@Test
		void typedReferenceMissingAllowsExplicitValueModel() {
			var reference = new ScoreFunctionReference<>( "convertedRating", Integer.class, ValueModel.INDEX );
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( reference ).missing( "9", ValueModel.MAPPING )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 9f, 9f, 9f );
		}

		@Test
		void typedReferenceMissingDefaultAfterExplicitOverride() {
			var reference = new ScoreFunctionReference<>( "convertedRating", Integer.class, ValueModel.INDEX );
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( reference )
							.missing( "9", ValueModel.MAPPING ).missing( 7 ).boostMode().replace() )
					.fetchAllHits();

			// An explicit override applies to that call only; the default still uses the reference's model.
			assertThat( scores ).containsExactlyInAnyOrder( 7f, 7f, 7f );
		}

		@Test
		void stringFieldMissingResetsToMappingValueModel() {
			var reference = new ScoreFunctionReference<>( "convertedRating", Integer.class, ValueModel.INDEX );
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( reference ).missing( 7 )
							.fieldValueFactor( "convertedRating" ).missing( "3" )
							.scoreMode().sum().boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 10f, 10f, 10f );
		}

		@Test
		void scaledBigDecimalAndMissingUseLogicalValues() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore()
							.fieldValueFactor( "decimalRating" ).missing( new BigDecimal( "2.50" ) ).factor( 2 )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 2.5f, 5f, 7.5f );
		}

		@Test
		void scaledBigIntegerAndMissingUseLogicalValues() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore()
							.fieldValueFactor( "integerRating" ).missing( BigInteger.valueOf( 2000 ) ).factor( 2 )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 2000f, 4000f, 6000f );
		}

		@Test
		void implicitNesting() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore()
							.fieldValueFactor( "nested.rating" )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 7f, 11f, 13f );
		}

		@Test
		void explicitNesting() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.nested( "nested" ).add( f.functionScore()
							.fieldValueFactor( "nested.rating" )
							.boostMode().replace() ) )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 7f, 11f, 13f );
		}

		@Test
		void withFunctionWeight() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" ).functionWeight( 3 )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 30f, 60f, 15f );
		}

		@Test
		void simple_boostModeMultiply() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.boostMode().multiply() )
					.fetchAllHits();

			// matchAll gives score=1.0 for all docs, multiply by popularity
			// doc1: 1.0 * 10 = 10, doc2: 1.0 * 20 = 20, doc3: 1.0 * 5 = 5
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 20f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 10f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 5f, within( 0.1f ) ) );
		}

		@Test
		void boostModeReplace() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.boostMode().replace() )
					.fetchAllHits();

			// replace: score = popularity
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 20f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 10f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 5f, within( 0.1f ) ) );
		}

		@Test
		void boostModeSum() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.boostMode().sum() )
					.fetchAllHits();

			// sum: 1.0 + popularity
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 21f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 11f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 6f, within( 0.1f ) ) );
		}

		@Test
		void modifierLog1p() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.modifier().log1p()
							.boostMode().replace() )
					.fetchAllHits();

			// replace: score = log10(1 + popularity)
			// doc1: log10(11) ≈ 1.041, doc2: log10(21) ≈ 1.322, doc3: log10(6) ≈ 0.778
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( (float) Math.log10( 21 ), within( 0.01f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( (float) Math.log10( 11 ), within( 0.01f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( (float) Math.log10( 6 ), within( 0.01f ) ) );
		}

		@Test
		void withFactor() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.factor( 2 )
							.boostMode().replace() )
					.fetchAllHits();

			// replace: score = 2 * popularity
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 40f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 20f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 10f, within( 0.1f ) ) );
		}
	}

	@Nested
	class MultipleFunctions {

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void retainedFieldFunctionStep(boolean weightAddedAfterwards) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						var first = predicate.fieldValueFactor( "popularity" );
						if ( weightAddedAfterwards ) {
							predicate.weight( 5 );
						}
						else {
							predicate.fieldValueFactor( "rating" );
						}
						first.factor( 2 );
						return predicate.scoreMode().sum().boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 25f, weightAddedAfterwards ? 45f : 43f,
					weightAddedAfterwards ? 15f : 18f );
		}


		@Test
		void retainedFieldFunctionBuildKeepsOrderAndCachesPredicate() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						var first = predicate.fieldValueFactor( "popularity" );
						var second = predicate.fieldValueFactor( "rating" );
						first.factor( 2 );
						predicate.scoreMode().first().boostMode().replace();
						var built = first.toPredicate();
						assertThat( predicate.toPredicate() ).isSameAs( built );
						assertThat( second.toPredicate() ).isSameAs( built );
						return predicate;
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 20f, 40f, 10f );
		}

		@Test
		void retainedFieldFunctionModifierStep() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						var modifier = predicate.fieldValueFactor( "popularity" ).modifier();
						predicate.fieldValueFactor( "rating" );
						modifier.square();
						return predicate.scoreMode().sum().boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 105f, 403f, 33f );
		}

		@Test
		void retainedFieldFunctionWeightAndFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						var first = predicate.fieldValueFactor( "popularity" );
						predicate.fieldValueFactor( "rating" );
						first.functionWeight( 3 ).filter( FunctionScorePredicateSpecificsIT::featuredFilter );
						return predicate.scoreMode().sum().boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 35f, 3f, 23f );
		}

		@Test
		void retainedWeightFunctionFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						var first = predicate.weight( 10 );
						predicate.weight( 3 );
						first.filter( FunctionScorePredicateSpecificsIT::featuredFilter );
						return predicate.scoreMode().sum().boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 13f, 3f, 13f );
		}

		@Test
		void retainedTypedReferenceMissingValueModel() {
			var reference = new ScoreFunctionReference<>( "convertedRating", Integer.class, ValueModel.INDEX );
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						var first = predicate.fieldValueFactor( reference );
						predicate.fieldValueFactor( "convertedRating" ).missing( "3" );
						first.missing( 7 );
						return predicate.scoreMode().sum().boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactly( 10f, 10f, 10f );
		}


		@ParameterizedTest
		@CsvSource({
				"rating, nested.rating",
				"nested.rating, rating",
				"nested.rating, otherNested.rating",
				"otherNested.rating, nested.rating" })
		void mixedNestedPathsRejected(String firstField, String secondField) {

			assertThatThrownBy( () -> index.createScope().predicate().functionScore()
					.fieldValueFactor( firstField )
					.fieldValueFactor( secondField )
					.toPredicate() )
					.isInstanceOf( SearchException.class )
					.hasMessageMatching( "(?s)HSEARCH[0-9]+:.*" )
					.hasMessageContainingAll( "at different nested paths", secondField, "indexName" );
		}

		@Test
		void filteredFunctionIgnoresMissingValueOutsideFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "optionalRating" )
							.filter( pf -> pf.match().field( "featured" ).matching( true ) )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 5f, 1f, 8f );
		}

		@Test
		void firstIgnoresMissingValueInLaterFunction() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.weight( 10 ).filter( pf -> pf.match().field( "featured" ).matching( false ) )
							.fieldValueFactor( "optionalRating" )
							.scoreMode().first()
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 5f, 10f, 8f );
		}

		@Test
		void filteredFieldValueFactor() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" ).functionWeight( 3 )
							.filter( pf -> pf.match().field( "featured" ).matching( true ) )
							.fieldValueFactor( "rating" )
							.scoreMode().sum()
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 35f, 3f, 23f );
		}

		@Test
		void scoreModeSum() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.fieldValueFactor( "rating" )
							.scoreMode().sum()
							.boostMode().replace() )
					.fetchAllHits();

			// sum of popularity + rating: doc1: 10+5=15, doc2: 20+3=23, doc3: 5+8=13
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 23f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 15f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 13f, within( 0.1f ) ) );
		}

		@Test
		void scoreModeMultiply() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.fieldValueFactor( "rating" )
							.scoreMode().multiply()
							.boostMode().replace() )
					.fetchAllHits();

			// multiply: doc1: 10*5=50, doc2: 20*3=60, doc3: 5*8=40
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 60f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 50f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 40f, within( 0.1f ) ) );
		}

		@Test
		void scoreModeAvg() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" )
							.fieldValueFactor( "rating" )
							.scoreMode().avg()
							.boostMode().replace() )
					.fetchAllHits();

			// avg (unweighted, both weight=1): doc1: (10+5)/2=7.5, doc2: (20+3)/2=11.5, doc3: (5+8)/2=6.5
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 11.5f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 7.5f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 6.5f, within( 0.1f ) ) );
		}

		@Test
		void weightedAvg() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.fieldValueFactor( "popularity" ).functionWeight( 3 )
							.fieldValueFactor( "rating" ).functionWeight( 4 )
							.scoreMode().avg()
							.boostMode().replace() )
					.fetchAllHits();

			// weighted avg: doc1: (10*3 + 5*4)/(3+4) = 50/7 ≈ 7.14
			// doc2: (20*3 + 3*4)/(3+4) = 72/7 ≈ 10.29
			// doc3: (5*3 + 8*4)/(3+4) = 47/7 ≈ 6.71
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 72f / 7f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 50f / 7f, within( 0.1f ) ) );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 47f / 7f, within( 0.1f ) ) );
		}
	}

	@Nested
	class WeightFunction {

		@Test
		void scopedWeightFilterContributor() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().weight( 10 )
							.filter( FunctionScorePredicateSpecificsIT::featuredFilter )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 10f, 1f, 10f );
		}

		@Test
		void weightInsideExplicitNesting() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.nested( "nested" ).add( f.functionScore().weight( 5 ).boostMode().replace() ) )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 5f, 5f, 5f );
		}

		@Test
		void weightWithWildcardFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.weight( 10 ).filter( pf -> pf.wildcard().field( "label" ).matching( "feat*" ) )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 10f, 1f, 10f );
		}

		@Test
		void weightWithPhraseFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore()
							.weight( 10 ).filter( pf -> pf.phrase().field( "text" ).matching( "quick brown" ) )
							.boostMode().replace() )
					.fetchAllHits();

			// All documents contain both terms, but only one matches their order and positions.
			assertThat( scores ).containsExactlyInAnyOrder( 10f, 1f, 1f );
		}

		@Test
		void simpleWeight() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.weight( 5 )
							.boostMode().replace() )
					.fetchAllHits();

			// standalone weight(5) = base score 1.0 * weight 5 = 5
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).allSatisfy( s -> assertThat( s ).isCloseTo( 5f, within( 0.1f ) ) );
		}

		@ParameterizedTest
		@CsvSource({
				"multiply, 3, 9",
				"sum, 3, 9",
				"avg, 3, 3",
				"first, 3, 9",
				"max, 3, 9",
				"min, 3, 9",
				"multiply, 0, 0",
				"sum, 0, 3",
				"avg, 0, 3",
				"first, 0, 0",
				"max, 0, 0",
				"min, 0, 0" })
		void singleWeightMultipliesQueryScore(String mode, float weight, float expected) {
			for ( boolean filtered : new boolean[] { false, true } ) {
				List<Float> scores = index.createScope().query()
						.select( SearchProjectionFactory::score )
						.where( f -> {
							var predicate = f.functionScore( f.matchAll().boost( 3 ) );
							var function = predicate.weight( weight );
							if ( filtered ) {
								function.filter( FunctionScorePredicateSpecificsIT::featuredFilter );
							}
							return scoreMode( predicate.scoreMode(), mode ).boostMode().multiply();
						} )
						.fetchAllHits();

				assertThat( scores ).containsExactlyInAnyOrder( expected, filtered ? 3f : expected, expected );
			}
		}

		@Test
		void multiplyingWeightWithPhraseFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll().boost( 3 ) )
							.weight( 10 ).filter( pf -> pf.phrase().field( "text" ).matching( "quick brown" ) )
							.boostMode().multiply() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 30f, 3f, 3f );
		}

		@Test
		void weightWithFilter() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll() )
							.weight( 10 ).filter( pf -> pf.match().field( "featured" ).matching( true ) )
							.boostMode().replace() )
					.fetchAllHits();

			// featured docs (1,3): weight=10, non-featured (2): no function matches → score=1.0
			assertThat( scores ).hasSize( 3 );
			assertThat( scores.stream().filter( s -> s > 5 ).count() ).isEqualTo( 2 );
			assertThat( scores.stream().filter( s -> s < 5 ).count() ).isEqualTo( 1 );
		}
	}

	@Nested
	class DefaultsAndEdgeCases {

		@ParameterizedTest
		@CsvSource({ "multiply, 30", "replace, 10", "sum, 16", "avg, 8", "max, 10", "min, 6" })
		void predicateBoostAppliedAfterCombination(String mode, float expected) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> boostMode( f.functionScore( f.matchAll().boost( 3 ) )
							.weight( 5 ).boostMode(), mode ).boost( 2 ) )
					.fetchAllHits();

			assertThat( scores ).containsExactly( expected, expected, expected );
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void nestedPredicateBoost(boolean explicitNesting) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore().fieldValueFactor( "nested.rating" )
								.boostMode().replace().boost( 2 );
						return explicitNesting ? f.nested( "nested" ).add( predicate ) : predicate;
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 14f, 22f, 26f );
		}

		@Test
		void emptyFunctionsPreserveMatches() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.match().field( "featured" ).matching( true ).constantScore().boost( 3 ) )
							.boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactly( 3f, 3f );
		}

		@ParameterizedTest
		@ValueSource(strings = { "multiply", "replace", "sum", "avg", "max", "min" })
		void emptyFunctionsPreserveInnerScore(String mode) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> boostMode( f.functionScore( f.matchAll().boost( 3 ) ).boostMode(), mode ).boost( 2 ) )
					.fetchAllHits();

			assertThat( scores ).containsExactly( 6f, 6f, 6f );
		}

		@ParameterizedTest
		@CsvSource({ "multiply, 3", "replace, 1", "sum, 4", "avg, 2", "max, 3", "min, 1" })
		void noMatchingFunctionsUseNeutralFactor(String mode, float expected) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> boostMode( f.functionScore( f.matchAll().boost( 3 ) )
							.weight( 5 ).filter( pf -> pf.matchNone() ).boostMode(), mode ) )
					.fetchAllHits();

			assertThat( scores ).containsExactly( expected, expected, expected );
		}

		@ParameterizedTest
		@CsvSource({ "multiply, 5", "sum, 5", "avg, 1", "first, 5", "max, 5", "min, 5" })
		void singleWeightScoreModes(String mode, float expected) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> scoreMode( f.functionScore().weight( 5 ).scoreMode(), mode ).boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactly( expected, expected, expected );
		}

		@ParameterizedTest
		@ValueSource(booleans = { false, true })
		void singleFieldWeightedAverage(boolean filtered) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var function = f.functionScore().fieldValueFactor( "popularity" ).functionWeight( 5 );
						if ( filtered ) {
							function.filter( pf -> pf.match().field( "featured" ).matching( true ) );
						}
						return function.scoreMode().avg().boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 10f, filtered ? 1f : 20f, 5f );
		}

		@ParameterizedTest
		@CsvSource({ "sum, 1", "sum, 2", "avg, 1", "avg, 2" })
		void zeroMatchingWeights(String mode, int count) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						for ( int i = 0; i < count; i++ ) {
							predicate.weight( 0 );
						}
						return scoreMode( predicate.scoreMode(), mode ).boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactly( 1f, 1f, 1f );
		}

		@ParameterizedTest
		@ValueSource(strings = { "sum", "avg" })
		void zeroMatchingWeightsIgnoreNonMatchingWeights(String mode) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> scoreMode( f.functionScore()
							.fieldValueFactor( "popularity" ).functionWeight( 0 )
							.weight( 5 ).filter( pf -> pf.match().field( "featured" ).matching( true ) )
							.scoreMode(), mode ).boostMode().replace() )
					.fetchAllHits();

			float featuredScore = "sum".equals( mode ) ? 5f : 1f;
			assertThat( scores ).containsExactlyInAnyOrder( featuredScore, 1f, featuredScore );
		}

		@Test
		void constantScoreOverridesFunctionsAndRetainsBoost() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll().boost( 3 ) ).weight( 5 )
							.boostMode().replace().constantScore().boost( 2 ) )
					.fetchAllHits();

			assertThat( scores ).containsExactly( 2f, 2f, 2f );
		}


		@ParameterizedTest
		@ValueSource(strings = { "negative", "nan", "negativeInfinity" })
		void invalidFieldValueFactorResult(String invalidResult) {
			assertThatThrownBy( () -> index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var function = f.functionScore().fieldValueFactor( "popularity" );
						switch ( invalidResult ) {
							case "negative" -> function.factor( -1 );
							case "nan" -> function.factor( -1 ).modifier().sqrt();
							case "negativeInfinity" -> function.factor( 0 ).modifier().log();
							default -> throw new AssertionFailure( invalidResult );
						}
						return function.boostMode().replace();
					} )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class );
		}

		@Test
		void invalidFunctionCannotBeMaskedByMax() {
			assertThatThrownBy( () -> index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( "popularity" ).factor( -1 )
							.weight( 10 ).scoreMode().max().boostMode().replace() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class );
		}

		@ParameterizedTest
		@CsvSource({ "multiply, 2", "multiply, 10", "sum, 2" })
		void combinedFunctionScoreIsCapped(String mode, int count) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						for ( int i = 0; i < count; i++ ) {
							predicate.weight( Float.MAX_VALUE );
						}
						return scoreMode( predicate.scoreMode(), mode ).boostMode().replace();
					} )
					.fetchAllHits();

			assertThat( scores ).containsExactly( Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE );
		}

		@Test
		void infiniteFieldValueFactorIsCapped() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( "popularity" )
							.factor( 0 ).modifier().reciprocal().boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactly( Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE );
		}

		@ParameterizedTest
		@CsvSource({ "1, false", "1, true", "2, false", "2, true" })
		void infiniteMinimum(int count, boolean filtered) {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> {
						var predicate = f.functionScore();
						for ( int i = 0; i < count; i++ ) {
							var function = predicate.fieldValueFactor( "popularity" ).factor( 0 ).modifier().reciprocal();
							if ( filtered ) {
								function.filter( FunctionScorePredicateSpecificsIT::featuredFilter );
							}
						}
						return predicate.scoreMode().min().boostMode().replace();
					} )
					.fetchAllHits();

			// Elasticsearch bypasses score_mode for a single unfiltered function. Otherwise, min
			// treats an infinite minimum as if no function matched and returns the neutral factor.
			float expected = count == 1 && !filtered ? Float.MAX_VALUE : 1f;
			assertThat( scores ).containsExactly( expected, expected, expected );
		}

		@Test
		void functionScoreIsCappedBeforeBoostMode() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll().boost( 0.5f ) )
							.weight( Float.MAX_VALUE ).weight( 2 ).boostMode().multiply() )
					.fetchAllHits();

			assertThat( scores ).containsExactly( Float.MAX_VALUE / 2, Float.MAX_VALUE / 2, Float.MAX_VALUE / 2 );
		}

		@Test
		void multipliedQueryScoreCanOverflow() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.matchAll().boost( Float.MAX_VALUE ) )
							.weight( 2 ).boostMode().multiply() )
					.fetchAllHits();

			assertThat( scores ).containsExactly( Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY );
		}

		@Test
		void infiniteQueryScoreTimesZeroIsRejected() {
			assertThatThrownBy( () -> index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore( f.functionScore( f.matchAll().boost( Float.MAX_VALUE ) )
							.weight( 2 ).boostMode().multiply() )
							.weight( 0 ).boostMode().multiply() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class );
		}

		@Test
		void zeroFunctionScoreIsValid() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().fieldValueFactor( "popularity" ).factor( 0 ).boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 0f, 0f, 0f );
		}

		@Test
		void invalidFunctionSkippedByFirst() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore().weight( 10 )
							.fieldValueFactor( "popularity" ).factor( -1 )
							.scoreMode().first().boostMode().replace() )
					.fetchAllHits();

			assertThat( scores ).containsExactlyInAnyOrder( 10f, 10f, 10f );
		}

		@Test
		void scorableMetamodelAndTraits() {
			var descriptor = index.toApi().descriptor();
			var enabled = descriptor.field( "popularity" ).orElseThrow().toValueField().type();
			assertThat( enabled.scorable() ).isTrue();
			assertThat( enabled.traits() ).contains( IndexFieldTraits.ScoreFunctions.FIELD_VALUE_FACTOR );
			var disabled = descriptor.field( "unscorableRating" ).orElseThrow().toValueField().type();
			assertThat( disabled.scorable() ).isFalse();
			assertThat( disabled.traits() ).doesNotContain( IndexFieldTraits.ScoreFunctions.FIELD_VALUE_FACTOR );
			var useDefault = descriptor.field( "defaultScorableRating" ).orElseThrow().toValueField().type();
			assertThat( useDefault.scorable() ).isFalse();
			assertThat( useDefault.traits() ).doesNotContain( IndexFieldTraits.ScoreFunctions.FIELD_VALUE_FACTOR );
		}

		@Test
		void defaultsToMatchAll() {
			List<Float> scores = index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.functionScore()
							.fieldValueFactor( "popularity" )
							.boostMode().replace() )
					.fetchAllHits();

			// No inner predicate = matchAll, replace = popularity
			assertThat( scores ).hasSize( 3 );
			assertThat( scores ).anySatisfy( s -> assertThat( s ).isCloseTo( 20f, within( 0.1f ) ) );
		}
	}

	private static <N> N boostMode(FunctionScoreBoostModeStep<N> step, String mode) {
		return switch ( mode ) {
			case "multiply" -> step.multiply();
			case "replace" -> step.replace();
			case "sum" -> step.sum();
			case "avg" -> step.avg();
			case "max" -> step.max();
			case "min" -> step.min();
			default -> throw new AssertionFailure( mode );
		};
	}

	private static <N> N scoreMode(FunctionScoreScoreModeStep<N> step, String mode) {
		return switch ( mode ) {
			case "multiply" -> step.multiply();
			case "sum" -> step.sum();
			case "avg" -> step.avg();
			case "first" -> step.first();
			case "max" -> step.max();
			case "min" -> step.min();
			default -> throw new AssertionFailure( mode );
		};
	}

	private static class IndexBinding {
		final IndexFieldReference<BigDecimal> decimalRating;
		final IndexFieldReference<BigInteger> integerRating;
		final IndexObjectFieldReference nested;
		final IndexFieldReference<Integer> nestedRating;
		final IndexFieldReference<Integer> popularity;
		final IndexFieldReference<Integer> rating;
		final IndexFieldReference<Integer> optionalRating;
		final IndexFieldReference<Boolean> featured;
		final IndexFieldReference<String> label;
		final IndexFieldReference<String> text;

		IndexBinding(IndexSchemaElement root) {
			root.field( "convertedRating", f -> f.asInteger().scorable( Scorable.YES )
					.dslConverter( String.class, (value, context) -> Integer.valueOf( value ) ) ).toReference();
			root.field( "unscorableRating", f -> f.asInteger().scorable( Scorable.NO ) ).toReference();
			root.field( "defaultScorableRating", f -> f.asInteger() ).toReference();
			decimalRating = root.field( "decimalRating", f -> f.asBigDecimal().decimalScale( 2 ).scorable( Scorable.YES ) )
					.toReference();
			integerRating = root.field( "integerRating", f -> f.asBigInteger().decimalScale( -2 ).scorable( Scorable.YES ) )
					.toReference();
			IndexSchemaObjectField nestedField = root.objectField( "nested", ObjectStructure.NESTED );
			nested = nestedField.toReference();
			nestedRating = nestedField.field( "rating", f -> f.asInteger().scorable( Scorable.YES ) ).toReference();
			IndexSchemaObjectField otherNestedField = root.objectField( "otherNested", ObjectStructure.NESTED );
			otherNestedField.toReference();
			otherNestedField.field( "rating", f -> f.asInteger().scorable( Scorable.YES ) ).toReference();
			popularity = root.field( "popularity", f -> f.asInteger().scorable( Scorable.YES ) ).toReference();
			rating = root.field( "rating", f -> f.asInteger().scorable( Scorable.YES ) ).toReference();
			optionalRating = root.field( "optionalRating", f -> f.asInteger().scorable( Scorable.YES ) ).toReference();
			featured = root.field( "featured", f -> f.asBoolean().searchable( Searchable.YES ) ).toReference();
			label = root.field( "label", f -> f.asString() ).toReference();
			text = root.field( "text", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();
		}
	}

	private record ScoreFunctionReference<T>(String absolutePath, Class<T> scoreFunctionType, ValueModel valueModel)
			implements FieldValueFactorFieldReference<Object, T> {
		@Override
		public Class<Object> scopeRootType() {
			return Object.class;
		}
	}

	private static PredicateFinalStep featuredFilter(TypedSearchPredicateFactory<Object> factory) {
		return factory.match().field( new MatchReference<>( "featured", Boolean.class ) ).matching( true );
	}

	private record MatchReference<T>(String absolutePath, Class<T> predicateType)
			implements MatchPredicateFieldReference<Object, T> {
		@Override
		public Class<Object> scopeRootType() {
			return Object.class;
		}
	}

}
