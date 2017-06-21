/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filters;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.CompositeReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BulkScorer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verified queries don't get stale IndexReader instances after a change is applied.
 * Note that queries operate on per-segment sub-readers, while we usually expose
 * top level (recursive) global IndexReader views: this usually should not affect
 * their usage but is relevant to how we test them.
 *
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 * @since 4.2
 */
@TestForIssue(jiraKey = "HSEARCH-1230")
@Category(SkipOnElasticsearch.class) // IndexReaders are specific to Lucene
public class FreshReadersProvidedTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Guest.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void filtersTest() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "guests" ) );

		{ // Store guest "Thorin Oakenshield" in the index
			Guest lastDwarf = new Guest();
			lastDwarf.id = 13l;
			lastDwarf.name = "Thorin Oakenshield";
			helper.add( lastDwarf );
		}

		QueryBuilder guestQueryBuilder = helper.queryBuilder( Guest.class );

		Query queryAllGuests = guestQueryBuilder.all().createQuery();

		helper.assertThat( queryAllGuests )
				.from( Guest.class )
				.matchesExactlyIds( 13L );

		RecordingQueryWrapper recordingWrapper = new RecordingQueryWrapper( queryAllGuests, "name" );
		helper.assertThat( recordingWrapper )
				.from( Guest.class )
				.matchesExactlyIds( 13L );

		checkQueryInspectedAllSegments( recordingWrapper );
		expectedTermsForQuery( recordingWrapper, "thorin", "oakenshield" );

		{ // Store guest "Balin"
			Guest balin = new Guest();
			balin.id = 7l;
			balin.name = "Balin";
			helper.add( balin );
		}

		helper.assertThat( queryAllGuests )
				.from( Guest.class )
				.matchesExactlyIds( 13L, 7L );

		RecordingQueryWrapper secondRecordingWrapper = new RecordingQueryWrapper( queryAllGuests, "name" );
		helper.assertThat( secondRecordingWrapper )
				.from( Guest.class )
				.matchesExactlyIds( 13L, 7L );

		checkQueryInspectedAllSegments( secondRecordingWrapper );
		expectedTermsForQuery( secondRecordingWrapper, "thorin", "oakenshield", "balin" );
	}

	private void expectedTermsForQuery(RecordingQueryWrapper recordingWrapper, String... term) {
		Assert.assertEquals( term.length, recordingWrapper.seenTerms.size() );
		assertThat( recordingWrapper.seenTerms ).as( "seen terms" ).contains( (Object[]) term );
	}

	/**
	 * Verifies that the current {@link RecordingQueryWrapper} has been fed all the same sub-readers
	 * which would be obtained from a freshly checked out IndexReader.
	 *
	 * @param recordingWrapper test {@link RecordingQueryWrapper} instance
	 */
	private void checkQueryInspectedAllSegments(RecordingQueryWrapper recordingWrapper) {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		IndexReader currentIndexReader = searchFactory.getIndexReaderAccessor().open( Guest.class );
		try {
			List<IndexReader> allSubReaders = getSubIndexReaders( (MultiReader) currentIndexReader );
			assertThat( recordingWrapper.visitedReaders ).as( "visited readers" )
					.contains( allSubReaders.toArray() );
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( currentIndexReader );
		}
	}

	public static List<IndexReader> getSubIndexReaders(MultiReader compositeReader) {
		CompositeReaderContext compositeReaderContext = compositeReader.getContext();
		ArrayList<IndexReader> segmentReaders = new ArrayList<IndexReader>( 20 );

		for ( LeafReaderContext readerContext : compositeReaderContext.leaves() ) {
			segmentReaders.add( readerContext.reader() );
		}

		return segmentReaders;
	}

	/**
	 * Scorers are created once for each segment, each time being passed a different IndexReader.
	 * These IndexReader instances are "subreaders", not the global kind representing
	 * the whole index.
	 */
	private static class RecordingQueryWrapper extends Query {

		final Query delegate;
		final List<IndexReader> visitedReaders = new ArrayList<IndexReader>();
		final List<String> seenTerms = new ArrayList<String>();
		final String fieldName;

		public RecordingQueryWrapper(Query delegate, String fieldName) {
			this.delegate = delegate;
			this.fieldName = fieldName;
		}

		@Override
		public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
			Weight delegateWeight = delegate.createWeight( searcher, needsScores );
			return new ForwardingWeight( this, delegateWeight ) {
				@Override
				public Scorer scorer(LeafReaderContext context) throws IOException {
					record( context );
					return super.scorer( context );
				}
				@Override
				public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
					record( context );
					return super.bulkScorer( context );
				}
			};
		}

		private void record(LeafReaderContext context) throws IOException {
			final LeafReader reader = context.reader();
			this.visitedReaders.add( reader );
			Terms terms = reader.terms( fieldName );
			TermsEnum iterator = terms.iterator();
			BytesRef next = iterator.next();
			while ( next != null ) {
				seenTerms.add( next.utf8ToString() );
				next = iterator.next();
			}
		}

		@Override
		public String toString(String fieldName) {
			return new StringBuilder( "RecordingQueryWrapper(" )
					.append( this.delegate )
					.append( ", " )
					.append( this.fieldName )
					.append( ")" )
					.toString();
		}
	}

	@Indexed(index = "guests")
	public static final class Guest {

		private long id;
		private String name;

		@DocumentId
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		@Field
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
