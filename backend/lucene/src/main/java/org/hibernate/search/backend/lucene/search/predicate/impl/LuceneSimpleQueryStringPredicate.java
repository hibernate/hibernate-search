/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;

public class LuceneSimpleQueryStringPredicate extends AbstractLuceneNestablePredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;
	private final List<String> fieldPaths;

	private final Query query;

	private LuceneSimpleQueryStringPredicate(Builder builder) {
		super( builder );
		nestedPathHierarchy = builder.firstFieldState.field().nestedPathHierarchy();
		fieldPaths = new ArrayList<>( builder.fieldStates.keySet() );
		query = builder.buildQuery();
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return fieldPaths;
	}

	public static class Builder extends AbstractBuilder implements SimpleQueryStringPredicateBuilder {
		private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

		private LuceneSimpleQueryStringPredicateBuilderFieldState firstFieldState;
		private final Map<String, LuceneSimpleQueryStringPredicateBuilderFieldState> fieldStates = new LinkedHashMap<>();
		private Occur defaultOperator = Occur.SHOULD;
		private String simpleQueryString;
		private Analyzer overrideAnalyzer;
		private boolean ignoreAnalyzer = false;
		private int flags = -1;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
			this.analysisDefinitionRegistry = scope.analysisDefinitionRegistry();
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
			this.flags = toFlagsMask( flags );
		}

		@Override
		public FieldState field(String fieldPath) {
			LuceneSimpleQueryStringPredicateBuilderFieldState fieldState = fieldStates.get( fieldPath );
			if ( fieldState == null ) {
				fieldState = scope.fieldQueryElement( fieldPath, LucenePredicateTypeKeys.SIMPLE_QUERY_STRING );
				if ( firstFieldState == null ) {
					firstFieldState = fieldState;
				}
				else {
					SearchIndexSchemaElementContextHelper.checkNestedDocumentPathCompatibility( firstFieldState.field(),
							fieldState.field() );
				}
				fieldStates.put( fieldPath, fieldState );
			}
			return fieldState;
		}

		@Override
		public void simpleQueryString(String simpleQueryString) {
			this.simpleQueryString = simpleQueryString;
		}

		@Override
		public void analyzer(String analyzerName) {
			this.overrideAnalyzer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
			if ( overrideAnalyzer == null ) {
				throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexNames( scope.hibernateSearchIndexNames() ) );
			}
		}

		@Override
		public void skipAnalysis() {
			this.ignoreAnalyzer = true;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneSimpleQueryStringPredicate( this );
		}

		private Query buildQuery() {
			SimpleQueryParser queryParser = new SimpleQueryParser( buildAnalyzer(), buildWeights(), flags );
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
			if ( fieldStates.size() == 1 ) {
				return fieldStates.values().iterator().next().field().type().searchAnalyzerOrNormalizer();
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
			for ( LuceneSimpleQueryStringPredicateBuilderFieldState state : fieldStates.values() ) {
				// Warning: we must use field().absolutePath(), not the key in the map,
				// because that key may be a relative path when using SearchPredicateFactory.withRoot(...)
				builder.setAnalyzer( state.field().absolutePath(), state.field().type().searchAnalyzerOrNormalizer() );
			}
			return builder.build();
		}

		private Map<String, Float> buildWeights() {
			Map<String, Float> weights = new LinkedHashMap<>();
			for ( LuceneSimpleQueryStringPredicateBuilderFieldState state : fieldStates.values() ) {
				Float boost = state.boost();
				if ( boost == null ) {
					boost = 1f;
				}

				// Warning: we must use field().absolutePath(), not the key in the map,
				// because that key may be a relative path when using SearchPredicateFactory.withRoot(...)
				weights.put( state.field().absolutePath(), boost );
			}
			return weights;
		}

		private static int toFlagsMask(Set<SimpleQueryFlag> flags) {
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
			return flag;
		}
	}
}
