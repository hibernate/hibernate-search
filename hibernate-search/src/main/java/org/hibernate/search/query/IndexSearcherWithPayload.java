package org.hibernate.search.query;

import org.apache.lucene.search.IndexSearcher;

/**
* @author Emmanuel Bernard
*/
//meant to be package-private, was opened up for Infinispan temporarily. Don't use outside of Hibernate Search codebase!
@Deprecated//(warning to other frameworks only: this class is not part of public API)
public class IndexSearcherWithPayload {
	private final IndexSearcher searcher;
	private boolean fieldSortDoTrackScores;
	private boolean fieldSortDoMaxScore;

	public IndexSearcherWithPayload(IndexSearcher searcher, boolean fieldSortDoTrackScores, boolean fieldSortDoMaxScore) {
		this.searcher = searcher;
		this.fieldSortDoTrackScores = fieldSortDoTrackScores;
		this.fieldSortDoMaxScore = fieldSortDoMaxScore;
		searcher.setDefaultFieldSortScoring( fieldSortDoTrackScores, fieldSortDoMaxScore );
	}

	public IndexSearcher getSearcher() {
		return searcher;
	}

	public boolean isFieldSortDoTrackScores() {
		return fieldSortDoTrackScores;
	}

	public boolean isFieldSortDoMaxScore() {
		return fieldSortDoMaxScore;
	}
}
