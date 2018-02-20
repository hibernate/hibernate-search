/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class SearchSortFactoryImpl implements LuceneSearchSortFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchTargetModel searchTargetModel;

	public SearchSortFactoryImpl(LuceneSearchTargetModel searchTargetModel) {
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public SearchSort toSearchSort(SearchSortContributor<LuceneSearchSortCollector> contributor) {
		LuceneSearchQueryElementCollector collector = new LuceneSearchQueryElementCollector();
		contributor.contribute( collector );
		return new LuceneSearchSort( collector.toLuceneSortFields() );
	}

	@Override
	public SearchSortContributor<LuceneSearchSortCollector> toContributor(SearchSort predicate) {
		if ( !( predicate instanceof LuceneSearchSort ) ) {
			throw log.cannotMixLuceneSearchSortWithOtherSorts( predicate );
		}
		return (LuceneSearchSort) predicate;
	}

	@Override
	public ScoreSortBuilder<LuceneSearchSortCollector> score() {
		return new ScoreSortBuilderImpl();
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortCollector> field(String absoluteFieldPath) {
		return new FieldSortBuilderImpl( absoluteFieldPath, searchTargetModel.getFieldFormatter( absoluteFieldPath ) );
	}

	@Override
	public SearchSortContributor<LuceneSearchSortCollector> indexOrder() {
		return IndexOrderSortContributor.INSTANCE;
	}

	@Override
	public SearchSortContributor<LuceneSearchSortCollector> fromLuceneSortField(SortField sortField) {
		return new UserProvidedLuceneSortFieldSortContributor( sortField );
	}
}
