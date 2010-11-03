package org.hibernate.search.query;

import org.apache.lucene.search.IndexSearcher;

/**
* @author Emmanuel Bernard
*/
//package
class IndexSearcherWithPayload {
	private final IndexSearcher searcher;
	private  boolean fieldSortDoTrackScores;
	private  boolean fieldSortDoMaxScore;

	IndexSearcherWithPayload(IndexSearcher searcher, boolean fieldSortDoTrackScores, boolean fieldSortDoMaxScore) {
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
