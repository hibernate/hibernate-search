/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopedIndexFieldComponent;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneSucceedingCompatibilityChecker;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldContext;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.util.impl.FieldContextSimpleQueryParser;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;

public class LuceneSimpleQueryStringPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements SimpleQueryStringPredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneScopeModel scopeModel;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private final Map<String, LuceneSimpleQueryStringPredicateBuilderFieldContext> fields = new LinkedHashMap<>();
	private Occur defaultOperator = Occur.SHOULD;
	private String simpleQueryString;
	private Analyzer overrideAnalyzer;
	private boolean ignoreAnalyzer = false;
	private LuceneCompatibilityChecker analyzerChecker = new LuceneSucceedingCompatibilityChecker();

	LuceneSimpleQueryStringPredicateBuilder(LuceneSearchContext searchContext, LuceneScopeModel scopeModel) {
		this.scopeModel = scopeModel;
		this.analysisDefinitionRegistry = searchContext.getAnalysisDefinitionRegistry();
	}

	@Override
	public void withAndAsDefaultOperator() {
		this.defaultOperator = Occur.MUST;
	}

	@Override
	public FieldContext field(String absoluteFieldPath) {
		LuceneSimpleQueryStringPredicateBuilderFieldContext field = fields.get( absoluteFieldPath );
		if ( field == null ) {
			LuceneScopedIndexFieldComponent<LuceneFieldPredicateBuilderFactory> fieldComponent = scopeModel.getSchemaNodeComponent(
					absoluteFieldPath, LuceneSearchPredicateBuilderFactoryImpl.PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
			field = fieldComponent.getComponent().createSimpleQueryStringFieldContext( absoluteFieldPath );
			analyzerChecker = analyzerChecker.combine( fieldComponent.getAnalyzerCompatibilityChecker() );
			fields.put( absoluteFieldPath, field );
		}
		return field;
	}

	@Override
	public void simpleQueryString(String simpleQueryString) {
		this.simpleQueryString = simpleQueryString;
	}

	@Override
	public void analyzer(String analyzerName) {
		this.overrideAnalyzer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
		if ( overrideAnalyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexNames( scopeModel.getIndexNames() ) );
		}
	}

	@Override
	public void skipAnalysis() {
		this.ignoreAnalyzer = true;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		if ( !ignoreAnalyzer && overrideAnalyzer == null ) {
			analyzerChecker.failIfNotCompatible();
		}

		Analyzer analyzer = buildAnalyzer();
		FieldContextSimpleQueryParser queryParser = new FieldContextSimpleQueryParser( analyzer, fields );
		queryParser.setDefaultOperator( defaultOperator );

		return queryParser.parse( simpleQueryString );
	}

	private Analyzer buildAnalyzer() {
		if ( ignoreAnalyzer ) {
			return AnalyzerConstants.KEYWORD_ANALYZER;
		}
		if ( overrideAnalyzer != null ) {
			return overrideAnalyzer;
		}
		if ( fields.size() == 1 ) {
			return fields.values().iterator().next().getAnalyzer();
		}

		/*
		 * We need to build a new scoped analyzer to address the case of search queries targeting
		 * multiple indexes, where index A defines "field1" but not "field2",
		 * and index B defines "field2" but not "field1".
		 * In that case, neither the scoped analyzer for index A nor the scoped analyzer for index B would work.
		 *
		 * An alternative exists, but I am not sure it would perform significantly better.
		 * Let us consider that all targeted indexes are compatible for the targeted fields,
		 * i.e. if an index defines a field, it always has the same analyzer as the same field in other indexes.
		 * This compatibility would allow us to simply use a "chaining" analyzer,
		 * which would hold a list of each scoped analyzer for each index,
		 * and, when asked for the analyzer to delegate to,
		 * would pick the first analyzer returned by any of the scoped analyzers in its list.
		 */
		ScopedAnalyzer.Builder builder = new ScopedAnalyzer.Builder();
		for ( Map.Entry<String, LuceneSimpleQueryStringPredicateBuilderFieldContext> entry : fields.entrySet() ) {
			builder.setAnalyzer( entry.getKey(), entry.getValue().getAnalyzer() );
		}
		return builder.build();
	}
}
