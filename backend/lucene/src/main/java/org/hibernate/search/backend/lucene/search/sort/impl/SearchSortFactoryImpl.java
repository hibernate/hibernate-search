/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

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
	public SearchSort toSearchSort(List<LuceneSearchSortBuilder> builders) {
		return new LuceneSearchSort( builders );
	}

	@Override
	public void toImplementation(SearchSort sort, Consumer<LuceneSearchSortBuilder> implementationConsumer) {
		if ( !( sort instanceof LuceneSearchSort ) ) {
			throw log.cannotMixLuceneSearchSortWithOtherSorts( sort );
		}
		((LuceneSearchSort) sort).getBuilders().forEach( implementationConsumer );
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector, List<LuceneSearchSortBuilder> builders) {
		for ( LuceneSearchSortBuilder builder : builders ) {
			builder.buildAndContribute( collector );
		}
	}

	@Override
	public ScoreSortBuilder<LuceneSearchSortBuilder> score() {
		return new ScoreSortBuilderImpl();
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> field(String absoluteFieldPath) {
		LuceneIndexSchemaFieldNode<?> schemaNode = searchTargetModel.getSchemaNode( absoluteFieldPath );

		return new FieldSortBuilderImpl(
				absoluteFieldPath,
				schemaNode.getConverter(),
				schemaNode.getSortContributor()
		);
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> distance(String absoluteFieldPath, GeoPoint location) {
		return new DistanceSortBuilderImpl(
				absoluteFieldPath,
				location,
				searchTargetModel.getSchemaNode( absoluteFieldPath ).getSortContributor()
		);
	}

	@Override
	public LuceneSearchSortBuilder indexOrder() {
		return IndexOrderSortContributor.INSTANCE;
	}

	@Override
	public LuceneSearchSortBuilder fromLuceneSortField(SortField luceneSortField) {
		return new UserProvidedLuceneSortFieldSortContributor( luceneSortField );
	}

	@Override
	public LuceneSearchSortBuilder fromLuceneSort(Sort luceneSort) {
		return new UserProvidedLuceneSortSortContributor( luceneSort );
	}
}
