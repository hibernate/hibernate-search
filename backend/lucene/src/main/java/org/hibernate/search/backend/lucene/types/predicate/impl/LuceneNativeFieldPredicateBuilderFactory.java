/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneNativeFieldPredicateBuilderFactory implements LuceneFieldPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final LuceneNativeFieldPredicateBuilderFactory INSTANCE =
			new LuceneNativeFieldPredicateBuilderFactory();

	private LuceneNativeFieldPredicateBuilderFactory() {
		// Nothing to do
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldPredicateBuilderFactory other) {
		return other == INSTANCE;
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldPredicateBuilderFactory other) {
		return other == INSTANCE;
	}

	@Override
	public boolean hasCompatibleAnalyzer(LuceneFieldPredicateBuilderFactory other) {
		return other == INSTANCE;
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			LuceneCompatibilityChecker converterChecker, LuceneCompatibilityChecker analyzerChecker) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			LuceneCompatibilityChecker converterChecker) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			LuceneCompatibilityChecker analyzerChecker) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public ExistsPredicateBuilder<LuceneSearchPredicateBuilder> createExistsPredicateBuilder(String absoluteFieldPath,
			List<String> nestedPathHierarchy) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		throw unsupported( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		throw unsupported( absoluteFieldPath );
	}

	private SearchException unsupported(String absoluteFieldPath) {
		return log.unsupportedDSLPredicatesForNativeField(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}
}
