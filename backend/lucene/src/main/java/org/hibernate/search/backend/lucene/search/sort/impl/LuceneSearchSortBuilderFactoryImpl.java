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

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class LuceneSearchSortBuilderFactoryImpl implements LuceneSearchSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final SortBuilderFactoryRetrievalStrategy SORT_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new SortBuilderFactoryRetrievalStrategy();

	private final LuceneSearchContext searchContext;
	private final LuceneSearchTargetModel searchTargetModel;

	public LuceneSearchSortBuilderFactoryImpl(LuceneSearchContext searchContext,
			LuceneSearchTargetModel searchTargetModel) {
		this.searchContext = searchContext;
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public SearchSort toSearchSort(List<LuceneSearchSortBuilder> builders) {
		return new LuceneSearchSort( builders );
	}

	@Override
	public void toImplementation(SearchSort sort, Consumer<? super LuceneSearchSortBuilder> implementationConsumer) {
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
		return new LuceneScoreSortBuilder();
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> field(String absoluteFieldPath) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, SORT_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createFieldSortBuilder( searchContext, absoluteFieldPath );
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> distance(String absoluteFieldPath, GeoPoint location) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, SORT_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createDistanceSortBuilder( absoluteFieldPath, location );
	}

	@Override
	public LuceneSearchSortBuilder indexOrder() {
		return LuceneIndexOrderSortBuilder.INSTANCE;
	}

	@Override
	public LuceneSearchSortBuilder fromLuceneSortField(SortField luceneSortField) {
		return new LuceneUserProvidedLuceneSortFieldSortBuilder( luceneSortField );
	}

	@Override
	public LuceneSearchSortBuilder fromLuceneSort(Sort luceneSort) {
		return new LuceneUserProvidedLuceneSortSortBuilder( luceneSort );
	}

	private static class SortBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<LuceneFieldSortBuilderFactory> {

		@Override
		public LuceneFieldSortBuilderFactory extractComponent(LuceneIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getSortBuilderFactory();
		}

		@Override
		public boolean areCompatible(LuceneFieldSortBuilderFactory component1,
				LuceneFieldSortBuilderFactory component2) {
			return component1.isDslCompatibleWith( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				LuceneFieldSortBuilderFactory component1, LuceneFieldSortBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForSort( absoluteFieldPath, component1, component2, context );
		}
	}
}
