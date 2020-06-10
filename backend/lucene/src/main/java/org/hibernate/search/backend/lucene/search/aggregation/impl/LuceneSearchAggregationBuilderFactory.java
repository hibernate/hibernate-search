/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopedIndexFieldComponent;
import org.hibernate.search.backend.lucene.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory<LuceneSearchAggregationCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final AggregationBuilderFactoryRetrievalStrategy AGGREGATION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new AggregationBuilderFactoryRetrievalStrategy();

	private final LuceneSearchContext searchContext;
	private final LuceneSearchIndexesContext indexes;

	public LuceneSearchAggregationBuilderFactory(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public <A> void contribute(LuceneSearchAggregationCollector collector,
			AggregationKey<A> key, SearchAggregation<A> aggregation) {
		if ( !(aggregation instanceof LuceneSearchAggregation) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherAggregations( aggregation );
		}

		LuceneSearchAggregation<A> casted = (LuceneSearchAggregation<A>) aggregation;
		if ( !indexes.indexNames().equals( casted.getIndexNames() ) ) {
			throw log.aggregationDefinedOnDifferentIndexes(
				aggregation, casted.getIndexNames(), indexes.indexNames()
			);
		}

		collector.collectAggregation( key, casted );
	}

	@Override
	public <T> TermsAggregationBuilder<T> createTermsAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		LuceneScopedIndexFieldComponent<LuceneFieldAggregationBuilderFactory> fieldComponent =
				indexes.schemaNodeComponent( absoluteFieldPath, AGGREGATION_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		checkConverterCompatibility( fieldComponent, convert );

		String nestedDocumentPath = indexes.nestedDocumentPath( absoluteFieldPath );

		return fieldComponent.getComponent().createTermsAggregationBuilder(
				searchContext, nestedDocumentPath, absoluteFieldPath, expectedType, convert
		);
	}

	@Override
	public <T> RangeAggregationBuilder<T> createRangeAggregationBuilder(String absoluteFieldPath, Class<T> expectedType,
			ValueConvert convert) {
		LuceneScopedIndexFieldComponent<LuceneFieldAggregationBuilderFactory> fieldComponent =
				indexes.schemaNodeComponent( absoluteFieldPath, AGGREGATION_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		checkConverterCompatibility( fieldComponent, convert );

		String nestedDocumentPath = indexes.nestedDocumentPath( absoluteFieldPath );

		return fieldComponent.getComponent().createRangeAggregationBuilder(
				searchContext, nestedDocumentPath, absoluteFieldPath, expectedType, convert
		);
	}

	private void checkConverterCompatibility(
			LuceneScopedIndexFieldComponent<LuceneFieldAggregationBuilderFactory> fieldComponent,
			ValueConvert convert) {
		switch ( convert ) {
			case NO:
				break;
			case YES:
			default:
				fieldComponent.getConverterCompatibilityChecker().failIfNotCompatible();
				break;
		}
	}

	private static class AggregationBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<LuceneFieldAggregationBuilderFactory> {

		@Override
		public LuceneFieldAggregationBuilderFactory extractComponent(LuceneIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.type().aggregationBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(LuceneFieldAggregationBuilderFactory component1, LuceneFieldAggregationBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(LuceneFieldAggregationBuilderFactory component1, LuceneFieldAggregationBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(LuceneFieldAggregationBuilderFactory component1, LuceneFieldAggregationBuilderFactory component2) {
			// analyzers are not involved in a aggregation clause
			return true;
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				LuceneFieldAggregationBuilderFactory component1,
				LuceneFieldAggregationBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForSearch( absoluteFieldPath, component1, component2, context );
		}
	}
}
