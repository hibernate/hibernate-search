/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateCollector;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchQueryElementCollector
		implements LuceneSearchPredicateCollector, LuceneSearchSortCollector {

	private Query luceneQueryPredicate;
	private List<SortField> sortFields;

	@Override
	public void collectPredicate(Query luceneQuery) {
		this.luceneQueryPredicate = luceneQuery;
	}

	@Override
	public void collectSortField(SortField sortField) {
		if ( sortFields == null ) {
			sortFields = new ArrayList<>( 5 );
		}
		sortFields.add( sortField );
	}

	@Override
	public void collectSortFields(List<SortField> sortFields) {
		if ( sortFields == null || sortFields.isEmpty() ) {
			return;
		}

		if ( this.sortFields == null ) {
			this.sortFields = new ArrayList<>( sortFields.size() );
		}
		this.sortFields.addAll( sortFields );
	}

	public Query toLuceneQueryPredicate() {
		return luceneQueryPredicate;
	}

	public List<SortField> toLuceneSortFields() {
		return sortFields;
	}

	public Sort toLuceneSort() {
		if ( sortFields == null || sortFields.isEmpty() ) {
			return null;
		}

		return new Sort( sortFields.toArray( new SortField[sortFields.size()] ) );
	}
}
