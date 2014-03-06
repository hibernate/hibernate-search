/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BitsFilteredDocIdSet;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.reader.impl.ManagedMultiReader;
import org.hibernate.search.reader.impl.ReaderProviderHelper;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verified filters don't get stale IndexReader instances after a change is applied.
 * Note that filters operate on per-segment subreaders, while we usually expose
 * top level (recursive) global IndexReader views: this usually should not affect
 * their usage but is relevant to how we test them.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 * @since 4.2
 */
@TestForIssue(jiraKey = "HSEARCH-1230")
public class FreshReadersProvidedTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Guest.class );

	@Test
	public void filtersTest() {
		SearchFactoryImplementor searchFactory = sfHolder.getSearchFactory();
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "guests" ) );

		{ // Store guest "Thorin Oakenshield" in the index
			Guest lastDwarf = new Guest();
			lastDwarf.id = 13l;
			lastDwarf.name = "Thorin Oakenshield";

			Work work = new Work( lastDwarf, lastDwarf.id, WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
			searchFactory.getWorker().performWork( work, tc );
			tc.end();
		}

		QueryBuilder guestQueryBuilder = searchFactory.buildQueryBuilder()
				.forEntity( Guest.class )
				.get();

		Query queryAllGuests = guestQueryBuilder.all().createQuery();

		List<EntityInfo> queryEntityInfos = searchFactory.createHSQuery()
			.luceneQuery( queryAllGuests )
			.targetedEntities( Arrays.asList( new Class<?>[]{ Guest.class } ) )
			.queryEntityInfos();

		Assert.assertEquals( 1, queryEntityInfos.size() );
		Assert.assertEquals( 13L, queryEntityInfos.get( 0 ).getId() );

		RecordingFilter filter = new RecordingFilter( "name" );
		List<EntityInfo> filteredQueryEntityInfos = searchFactory.createHSQuery()
				.luceneQuery( queryAllGuests )
				.targetedEntities( Arrays.asList( new Class<?>[]{ Guest.class } ) )
				.filter( filter )
				.queryEntityInfos();

		checkFilterInspectedAllSegments( filter );
		expectedTermsForFilter( filter, "thorin", "oakenshield" );
		Assert.assertEquals( 1, filteredQueryEntityInfos.size() );
		Assert.assertEquals( 13L, filteredQueryEntityInfos.get( 0 ).getId() );

		{ // Store guest "Balin"
			Guest balin = new Guest();
			balin.id = 7l;
			balin.name = "Balin";

			Work work = new Work( balin, balin.id, WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
			searchFactory.getWorker().performWork( work, tc );
			tc.end();
		}

		List<EntityInfo> queryEntityInfosAgain = searchFactory.createHSQuery()
			.luceneQuery( queryAllGuests )
			.targetedEntities( Arrays.asList( new Class<?>[]{ Guest.class } ) )
			.queryEntityInfos();

		Assert.assertEquals( 2, queryEntityInfosAgain.size() );
		Assert.assertEquals( 13L, queryEntityInfosAgain.get( 0 ).getId() );
		Assert.assertEquals( 7L, queryEntityInfosAgain.get( 1 ).getId() );

		RecordingFilter secondFilter = new RecordingFilter( "name" );
		List<EntityInfo> secondFilteredQueryEntityInfos = searchFactory.createHSQuery()
				.luceneQuery( queryAllGuests )
				.targetedEntities( Arrays.asList( new Class<?>[]{ Guest.class } ) )
				.filter( secondFilter )
				.queryEntityInfos();

		checkFilterInspectedAllSegments( secondFilter );
		expectedTermsForFilter( secondFilter, "thorin", "oakenshield", "balin" );

		Assert.assertEquals( 2, secondFilteredQueryEntityInfos.size() );
		Assert.assertEquals( 13L, secondFilteredQueryEntityInfos.get( 0 ).getId() );
		Assert.assertEquals( 7L, secondFilteredQueryEntityInfos.get( 1 ).getId() );
	}

	private void expectedTermsForFilter(RecordingFilter filter, String... term) {
		Assert.assertEquals( term.length, filter.seenTerms.size() );
		Assert.assertTrue( filter.seenTerms.containsAll( Arrays.asList( term ) ) );
	}

	/**
	 * Verifies that the current RecordingFilter has been fed all the same sub-readers
	 * which would be obtained from a freshly checked out IndexReader.
	 *
	 * @param filter
	 */
	private void checkFilterInspectedAllSegments(RecordingFilter filter) {
		SearchFactoryImplementor searchFactory = sfHolder.getSearchFactory();
		IndexReader currentIndexReader = searchFactory.getIndexReaderAccessor().open( Guest.class );
		try {
			List<IndexReader> allSubReaders = ReaderProviderHelper.getSubIndexReaders( (ManagedMultiReader) currentIndexReader );
			for ( IndexReader ir : allSubReaders ) {
				Assert.assertTrue( filter.visitedReaders.contains( ir ) );
			}
			for ( IndexReader ir : filter.visitedReaders ) {
				Assert.assertTrue( allSubReaders.contains( ir ) );
			}
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( currentIndexReader );
		}
	}

	/**
	 * Filters are invoked once for each segment, so they are invoked multiple times
	 * once for each segment, each time being passed a different IndexReader.
	 * These IndexReader instances are "subreaders", not the global kind representing
	 * the whole index.
	 */
	private static class RecordingFilter extends Filter {

		final List<IndexReader> visitedReaders = new ArrayList<IndexReader>();
		final List<String> seenTerms = new ArrayList<String>();
		final String fieldName;

		public RecordingFilter(String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
			final AtomicReader reader = context.reader();
			this.visitedReaders.add( reader );
			OpenBitSet bitSet = new OpenBitSet( reader.maxDoc() );
			for ( int i = 0; i < reader.maxDoc(); i++ ) {
				bitSet.fastSet( i );
			}
			Terms terms = reader.terms( fieldName );
			TermsEnum iterator = terms.iterator( null );
			BytesRef next = iterator.next();
			while ( next != null ) {
				seenTerms.add( next.utf8ToString() );
				next = iterator.next();
			}
			return BitsFilteredDocIdSet.wrap( bitSet, acceptDocs );
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
