/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneDifferentNestedObjectCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;

public class LuceneSimpleQueryStringPredicateBuilder extends AbstractLuceneNestablePredicateBuilder
		implements SimpleQueryStringPredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchIndexesContext indexes;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private final Map<String, LuceneSimpleQueryStringPredicateBuilderFieldState> fields = new LinkedHashMap<>();
	private Occur defaultOperator = Occur.SHOULD;
	private String simpleQueryString;
	private Analyzer overrideAnalyzer;
	private boolean ignoreAnalyzer = false;
	private EnumSet<SimpleQueryFlag> flags;
	private LuceneDifferentNestedObjectCompatibilityChecker nestedCompatibilityChecker;

	LuceneSimpleQueryStringPredicateBuilder(LuceneSearchContext searchContext, LuceneSearchIndexesContext indexes) {
		this.indexes = indexes;
		this.analysisDefinitionRegistry = searchContext.analysisDefinitionRegistry();
		this.nestedCompatibilityChecker = LuceneDifferentNestedObjectCompatibilityChecker.empty( indexes );
	}

	@Override
	public void defaultOperator(BooleanOperator operator) {
		switch ( operator ) {
			case AND:
				this.defaultOperator = Occur.MUST;
				break;
			case OR:
				this.defaultOperator = Occur.SHOULD;
				break;
		}
	}

	@Override
	public void flags(Set<SimpleQueryFlag> flags) {
		this.flags = EnumSet.copyOf( flags );
	}

	@Override
	public FieldState field(String absoluteFieldPath) {
		LuceneSimpleQueryStringPredicateBuilderFieldState field = fields.get( absoluteFieldPath );
		if ( field == null ) {
			LuceneSearchFieldContext<?> fieldContext = indexes.field( absoluteFieldPath );
			field = fieldContext.createSimpleQueryStringFieldState();
			nestedCompatibilityChecker = nestedCompatibilityChecker.combineAndCheck( absoluteFieldPath );
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
			throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexNames( indexes.indexNames() ) );
		}
	}

	@Override
	public void skipAnalysis() {
		this.ignoreAnalyzer = true;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		Analyzer analyzer = buildAnalyzer();

		int flag = -1;
		if ( flags != null ) {
			flag = 0;
			for ( SimpleQueryFlag operation : flags ) {
				switch ( operation ) {
					case AND:
						flag |= SimpleQueryParser.AND_OPERATOR;
						break;
					case NOT:
						flag |= SimpleQueryParser.NOT_OPERATOR;
						break;
					case OR:
						flag |= SimpleQueryParser.OR_OPERATOR;
						break;
					case PREFIX:
						flag |= SimpleQueryParser.PREFIX_OPERATOR;
						break;
					case PHRASE:
						flag |= SimpleQueryParser.PHRASE_OPERATOR;
						break;
					case PRECEDENCE:
						flag |= SimpleQueryParser.PRECEDENCE_OPERATORS;
						break;
					case ESCAPE:
						flag |= SimpleQueryParser.ESCAPE_OPERATOR;
						break;
					case WHITESPACE:
						flag |= SimpleQueryParser.WHITESPACE_OPERATOR;
						break;
					case FUZZY:
						flag |= SimpleQueryParser.FUZZY_OPERATOR;
						break;
					case NEAR:
						flag |= SimpleQueryParser.NEAR_OPERATOR;
						break;
				}
			}
		}

		Map<String, Float> weights = new LinkedHashMap<>();
		for ( Map.Entry<String, LuceneSimpleQueryStringPredicateBuilderFieldState> entry : fields.entrySet() ) {
			LuceneSimpleQueryStringPredicateBuilderFieldState state = entry.getValue();
			Float boost = state.getBoost();
			if ( boost == null ) {
				boost = 1f;
			}

			weights.put( entry.getKey(), boost );
		}

		SimpleQueryParser queryParser = new SimpleQueryParser( analyzer, weights, flag );
		queryParser.setDefaultOperator( defaultOperator );

		return queryParser.parse( simpleQueryString );
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedCompatibilityChecker.getNestedPathHierarchy();
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return new ArrayList<>( fields.keySet() );
	}

	private Analyzer buildAnalyzer() {
		if ( ignoreAnalyzer ) {
			return AnalyzerConstants.KEYWORD_ANALYZER;
		}
		if ( overrideAnalyzer != null ) {
			return overrideAnalyzer;
		}
		if ( fields.size() == 1 ) {
			return fields.values().iterator().next().getAnalyzerOrNormalizer();
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
		for ( Map.Entry<String, LuceneSimpleQueryStringPredicateBuilderFieldState> entry : fields.entrySet() ) {
			builder.setAnalyzer( entry.getKey(), entry.getValue().getAnalyzerOrNormalizer() );
		}
		return builder.build();
	}
}
