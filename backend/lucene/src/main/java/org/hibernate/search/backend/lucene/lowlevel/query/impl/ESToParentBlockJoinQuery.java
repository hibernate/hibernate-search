/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.io.IOException;
import java.util.Objects;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;

/**
 * <p>
 * Copied and adapted from {@code org.elasticsearch.search.MultiValueMode} class
 * of <a href="https://github.com/elastic/elasticsearch">Elasticsearch
 * project</a>.
 */
public class ESToParentBlockJoinQuery extends Query {

	private final ToParentBlockJoinQuery query;
	private final String path;
	private final ScoreMode scoreMode;

	public ESToParentBlockJoinQuery(Query childQuery, BitSetProducer parentsFilter, ScoreMode scoreMode, String path) {
		this( new ToParentBlockJoinQuery( childQuery, parentsFilter, scoreMode ), path, scoreMode );
	}

	private ESToParentBlockJoinQuery(ToParentBlockJoinQuery query, String path, ScoreMode scoreMode) {
		this.query = query;
		this.path = path;
		this.scoreMode = scoreMode;
	}

	/**
	 * Return the child query.
	 *
	 * @return Query
	 */
	public Query getChildQuery() {
		return query.getChildQuery();
	}

	/**
	 * Return the path of results of this query, or {@code null} if matches
	 * are at the root level.
	 *
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Return the score mode for the matching children.
	 *
	 * @return ScoreMode
	 */
	public ScoreMode getScoreMode() {
		return scoreMode;
	}

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
		Query innerRewrite = query.rewrite( reader );
		if ( innerRewrite != query ) {
			// Right now ToParentBlockJoinQuery always rewrites to a ToParentBlockJoinQuery
			// so the else block will never be used. It is useful in the case that
			// ToParentBlockJoinQuery one day starts to rewrite to a different query, eg.
			// a MatchNoDocsQuery if it realizes that it cannot match any docs and rewrites
			// to a MatchNoDocsQuery. In that case it would be fine to lose information
			// about the nested path.
			if ( innerRewrite instanceof ToParentBlockJoinQuery ) {
				return new ESToParentBlockJoinQuery( (ToParentBlockJoinQuery) innerRewrite, path, scoreMode );
			}
			else {
				return innerRewrite;
			}
		}
		return super.rewrite( reader );
	}

	@Override
	public void visit(QueryVisitor visitor) {
		// Highlighters must visit the child query to extract terms
		query.getChildQuery().visit( visitor.getSubVisitor( BooleanClause.Occur.MUST, this ) );
	}

	@Override
	public Weight createWeight(IndexSearcher searcher, org.apache.lucene.search.ScoreMode scoreMode, float boost) throws IOException {
		return query.createWeight( searcher, scoreMode, boost );
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object obj) {
		if ( sameClassAs( obj ) == false ) {
			return false;
		}
		ESToParentBlockJoinQuery that = (ESToParentBlockJoinQuery) obj;
		return query.equals( that.query ) && Objects.equals( path, that.path );
	}

	@Override
	public int hashCode() {
		return Objects.hash( getClass(), query, path );
	}

	@Override
	public String toString(String field) {
		return query.toString( field );
	}

}
