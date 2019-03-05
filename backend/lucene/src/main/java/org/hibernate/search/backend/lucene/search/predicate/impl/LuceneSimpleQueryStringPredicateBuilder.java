/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchScopeModel;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldContext;
import org.hibernate.search.backend.lucene.util.impl.FieldContextSimpleQueryParser;
import org.hibernate.search.engine.search.predicate.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;

public class LuceneSimpleQueryStringPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements SimpleQueryStringPredicateBuilder<LuceneSearchPredicateBuilder> {

	private final LuceneSearchScopeModel scopeModel;

	private final Map<String, LuceneSimpleQueryStringPredicateBuilderFieldContext> fields = new LinkedHashMap<>();
	private Occur defaultOperator = Occur.SHOULD;
	private String simpleQueryString;

	LuceneSimpleQueryStringPredicateBuilder(LuceneSearchScopeModel scopeModel) {
		this.scopeModel = scopeModel;
	}

	@Override
	public void withAndAsDefaultOperator() {
		this.defaultOperator = Occur.MUST;
	}

	@Override
	public FieldContext field(String absoluteFieldPath) {
		LuceneSimpleQueryStringPredicateBuilderFieldContext field = fields.get( absoluteFieldPath );
		if ( field == null ) {
			field = scopeModel.getSchemaNodeComponent(
					absoluteFieldPath,
					LuceneSearchPredicateBuilderFactoryImpl.PREDICATE_BUILDER_FACTORY_RETRIEVAL_STRATEGY,
					DslConverter.DISABLED
			)
					.createSimpleQueryStringFieldContext( absoluteFieldPath );
			fields.put( absoluteFieldPath, field );
		}
		return field;
	}

	@Override
	public void simpleQueryString(String simpleQueryString) {
		this.simpleQueryString = simpleQueryString;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		Analyzer analyzer = buildAnalyzer();
		FieldContextSimpleQueryParser queryParser = new FieldContextSimpleQueryParser( analyzer, fields );
		queryParser.setDefaultOperator( defaultOperator );

		return queryParser.parse( simpleQueryString );
	}

	private Analyzer buildAnalyzer() {
		if ( fields.size() == 1 ) {
			return fields.values().iterator().next().getAnalyzer();
		}
		else {
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

}
