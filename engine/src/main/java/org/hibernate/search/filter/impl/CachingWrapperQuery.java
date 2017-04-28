/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.ConstantScoreWeight;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.RoaringDocIdSet;
import org.hibernate.search.util.impl.SoftLimitMRUCache;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A slightly different version of Lucene's original <code>CachingWrapperQuery</code> which
 * uses <code>SoftReferences</code> instead of <code>WeakReferences</code> in order to cache
 * the filter <code>DocIdSet</code>.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @see org.apache.lucene.search.CachingWrapperQuery
 * @see <a href="https://hibernate.atlassian.net/browse/HSEARCH-174">HSEARCH-174</a>
 */
@SuppressWarnings("serial")
public class CachingWrapperQuery extends Query implements Cloneable {

	private static final Log log = LoggerFactory.make();

	public static final int DEFAULT_SIZE = 5;

	/**
	 * Under memory pressure the JVM will release all Soft references,
	 * so pushing it too high will invalidate all eventually useful other caches.
	 */
	private static final int HARD_TO_SOFT_RATIO = 15;

	private Query query; // not final because of clone

	/**
	 * The cache using soft references in order to store the filter bit sets.
	 */
	private final SoftLimitMRUCache cache;

	/**
	 * @param query Query to cache results of
	 */
	public CachingWrapperQuery(Query query) {
		this( query, DEFAULT_SIZE );
	}

	/**
	 * @param query Query to cache results of
	 * @param size soft reference size (gets multiplied by {@link #HARD_TO_SOFT_RATIO}.
	 */
	public CachingWrapperQuery(Query query, int size) {
		this.query = query;
		final int softRefSize = size * HARD_TO_SOFT_RATIO;
		if ( log.isDebugEnabled() ) {
			log.debugf( "Initialising SoftLimitMRUCache with hard ref size of %d and a soft ref of %d", (Integer) size, (Integer) softRefSize );
		}
		this.cache = new SoftLimitMRUCache( size, softRefSize );
	}

	/**
	 * Gets the contained query.
	 *
	 * @return the contained query.
	 */
	public Query getQuery() {
		return query;
	}

	/**
	 * Default cache implementation: uses {@link RoaringDocIdSet}.
	 */
	protected DocIdSet cacheImpl(DocIdSetIterator iterator, LeafReader reader) throws IOException {
		return new RoaringDocIdSet.Builder( reader.maxDoc() ).add( iterator ).build();
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		if ( getBoost() != 1f ) {
			return super.rewrite( reader );
		}
		final Query rewritten = query.rewrite( reader );
		if ( query == rewritten ) {
			return super.rewrite( reader );
		}
		else {
			CachingWrapperQuery clone = (CachingWrapperQuery) clone();
			clone.query = rewritten;
			return clone;
		}
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
		final Weight weight = query.createWeight( searcher, needsScores );
		if ( needsScores ) {
			// our cache is not sufficient, we need scores too
			return weight;
		}

		return new ConstantScoreWeight( weight.getQuery() ) {

			@Override
			public void extractTerms(Set<Term> terms) {
				weight.extractTerms( terms );
			}

			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				final LeafReader reader = context.reader();
				final Object key = reader.getCoreCacheKey();

				DocIdSet docIdSet = getDocIdSet( context );

				assert docIdSet != null;
				if ( docIdSet == DocIdSet.EMPTY ) {
					return null;
				}
				final DocIdSetIterator disi = docIdSet.iterator();
				if ( disi == null ) {
					return null;
				}

				return new ConstantScoreScorer( this, 0f, disi );
			}

			private DocIdSet getDocIdSet(LeafReaderContext context) throws IOException {
				final LeafReader reader = context.reader();
				final Object key = reader.getCoreCacheKey();
				Object cached = cache.get( key );
				if ( cached != null ) {
					return (DocIdSet) cached;
				}
				synchronized ( cache ) {
					cached = cache.get( key );
					if ( cached != null ) {
						return (DocIdSet) cached;
					}
					final DocIdSet docIdSet;
					final Scorer scorer = weight.scorer( context );
					if ( scorer == null ) {
						docIdSet = DocIdSet.EMPTY;
					}
					else {
						docIdSet = cacheImpl( scorer.iterator(), reader );
					}
					cache.put( key, docIdSet );
					return docIdSet;
				}
			}
		};
	}

	@Override
	public String toString(String field) {
		return getClass().getSimpleName() + "(" + query.toString( field ) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( !super.equals( o ) ) {
			return false;
		}
		final CachingWrapperQuery other = (CachingWrapperQuery) o;
		return this.query.equals( other.query );
	}

	@Override
	public int hashCode() {
		return ( query.hashCode() ^ super.hashCode() );
	}

}
