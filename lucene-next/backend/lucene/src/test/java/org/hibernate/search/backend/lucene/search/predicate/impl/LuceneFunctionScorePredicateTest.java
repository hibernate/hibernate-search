package org.hibernate.search.backend.lucene.search.predicate.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

class LuceneFunctionScorePredicateTest {

	private Directory directory;
	private DirectoryReader reader;
	private IndexSearcher searcher;
	private Query filter;
	private Weight filterWeight;
	private Scorer filterScorer;
	private DocIdSetIterator approximation;

	@BeforeEach
	void setup() throws IOException {
		directory = new ByteBuffersDirectory();
		try ( IndexWriter writer = new IndexWriter( directory,
				new IndexWriterConfig( new KeywordAnalyzer() ).setMergePolicy( NoMergePolicy.INSTANCE ) ) ) {
			for ( int i = 0; i < 2; i++ ) {
				Document document = new Document();
				document.add( new SortedNumericDocValuesField( "optionalRating", 5 ) );
				writer.addDocument( document );
				writer.addDocument( new Document() );
				writer.commit();
			}
		}
		reader = DirectoryReader.open( directory );
		searcher = new IndexSearcher( reader );
		searcher.setQueryCache( null );
		filter = mock( Query.class );
		filterWeight = mock( Weight.class );
		filterScorer = mock( Scorer.class );
		// The iterator represents a very large filter without building a correspondingly large index.
		approximation = spy( DocIdSetIterator.all( 20_000_000 ) );
		doThrow( new AssertionError( "Filters must only advance to requested documents" ) ).when( approximation ).nextDoc();
		when( filter.rewrite( any( IndexSearcher.class ) ) ).thenReturn( filter );
		when( filter.createWeight( any( IndexSearcher.class ),
				any( org.apache.lucene.search.ScoreMode.class ), eq( 1.0f ) ) ).thenReturn( filterWeight );
		when( filterWeight.scorer( any( LeafReaderContext.class ) ) ).thenReturn( filterScorer );
		when( filterWeight.isCacheable( any( LeafReaderContext.class ) ) ).thenReturn( true );
		when( filterScorer.iterator() ).thenReturn( approximation );
	}

	@AfterEach
	void cleanup() throws IOException {
		reader.close();
		directory.close();
	}

	@Test
	void filterWeightUsesQuerySearcherOnceAcrossLeaves() throws IOException {
		DoubleValuesSource source = source( LuceneFunctionScorePredicate.ScoreMode.MULTIPLY ).rewrite( searcher );
		for ( LeafReaderContext leaf : reader.leaves() ) {
			source.getValues( leaf, null );
		}
		verify( filter ).createWeight( same( searcher ),
				eq( org.apache.lucene.search.ScoreMode.COMPLETE_NO_SCORES ), eq( 1.0f ) );
		verify( filter, times( 1 ) ).createWeight( any( IndexSearcher.class ),
				any( org.apache.lucene.search.ScoreMode.class ), eq( 1.0f ) );
		verify( approximation, never() ).nextDoc();
		verify( approximation, never() ).advance( anyInt() );
	}

	@Test
	void twoPhaseFilterOnlyChecksRequestedDocuments() throws IOException {
		TwoPhaseIterator twoPhase = spy( new TwoPhaseIterator( approximation ) {
			@Override
			public boolean matches() {
				return approximation.docID() % 2 == 1;
			}

			@Override
			public float matchCost() {
				return 1;
			}
		} );
		when( filterScorer.twoPhaseIterator() ).thenReturn( twoPhase );
		DoubleValues values = source( LuceneFunctionScorePredicate.ScoreMode.MULTIPLY ).rewrite( searcher )
				.getValues( reader.leaves().get( 0 ), null );
		verify( twoPhase, never() ).matches();
		assertScore( values, 0, 1 );
		assertScore( values, 1, 10 );
		assertScore( values, 1, 10 );
		verify( twoPhase, times( 2 ) ).matches();
		verify( approximation, times( 2 ) ).advance( anyInt() );
		verify( approximation, never() ).nextDoc();
	}

	@Test
	void firstDoesNotAdvanceLaterFilter() throws IOException {
		DoubleValuesSource source = new LuceneFunctionScorePredicate.CompositeScoreFunctionSource(
				List.of( DoubleValuesSource.constant( 5 ), DoubleValuesSource.constant( 10 ) ),
				List.of( 1f, 1f ), Arrays.asList( null, filter ), LuceneFunctionScorePredicate.ScoreMode.FIRST );
		DoubleValues values = source.rewrite( searcher ).getValues( reader.leaves().get( 0 ), null );
		assertScore( values, 0, 5 );
		assertScore( values, 1, 5 );
		verify( approximation, never() ).advance( anyInt() );
		verify( approximation, never() ).nextDoc();
	}

	@Test
	void nonCacheableFilterMakesFunctionNonCacheable() throws IOException {
		when( filterWeight.isCacheable( any( LeafReaderContext.class ) ) ).thenReturn( false );
		DoubleValuesSource source = source( LuceneFunctionScorePredicate.ScoreMode.MULTIPLY ).rewrite( searcher );
		assertThat( source.isCacheable( reader.leaves().get( 0 ) ) ).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "emptyRating", "optionalRating" })
	void missingFieldValueHasDiagnostic(String fieldPath) throws IOException {
		DoubleValuesSource source = new LuceneFunctionScorePredicate.FieldValueFactorSource(
				fieldPath, value -> value, LuceneFunctionScorePredicate.Modifier.NONE, 1, null,
				EventContexts.fromIndexFieldAbsolutePath( fieldPath ) );
		DoubleValues values = source.getValues( reader.leaves().get( 0 ), null );

		assertThatThrownBy( () -> values.advanceExact( 1 ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH600198" )
				.hasMessageContaining( "Missing value for field '" + fieldPath + "'" )
				.hasMessageContaining( "Context: field '" + fieldPath + "'" );
	}

	@ParameterizedTest
	@ValueSource(strings = { "emptyRating", "optionalRating" })
	void missingFieldValueUsesFallback(String fieldPath) throws IOException {
		DoubleValuesSource source = new LuceneFunctionScorePredicate.FieldValueFactorSource(
				fieldPath, value -> value, LuceneFunctionScorePredicate.Modifier.NONE, 2, 7.0,
				EventContexts.fromIndexFieldAbsolutePath( fieldPath ) );
		DoubleValues values = source.getValues( reader.leaves().get( 0 ), null );

		assertScore( values, 1, 14 );
	}

	private DoubleValuesSource source(LuceneFunctionScorePredicate.ScoreMode scoreMode) {
		return new LuceneFunctionScorePredicate.CompositeScoreFunctionSource(
				List.of( DoubleValuesSource.constant( 5 ) ), List.of( 2f ), List.of( filter ), scoreMode );
	}

	private static void assertScore(DoubleValues values, int doc, double expected) throws IOException {
		assertThat( values.advanceExact( doc ) ).isTrue();
		assertThat( values.doubleValue() ).isEqualTo( expected );
	}
}
