/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.List;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.query.dsl.SimpleQueryStringTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class ConnectedMultiFieldsSimpleQueryStringQueryBuilder implements SimpleQueryStringTermination {

	private static final Log LOG = LoggerFactory.make();

	private final String simpleQueryString;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldsContext> fieldsContexts;
	private final QueryBuildingContext queryContext;

	private final boolean withAndAsDefaultOperator;

	public ConnectedMultiFieldsSimpleQueryStringQueryBuilder(String simpleQueryString,
			List<FieldsContext> fieldsContexts,
			boolean withAndAsDefaultOperator,
			QueryCustomizer queryCustomizer,
			QueryBuildingContext queryContext) {
		this.simpleQueryString = simpleQueryString;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContexts = fieldsContexts;

		this.withAndAsDefaultOperator = withAndAsDefaultOperator;
	}

	@Override
	public Query createQuery() {
		if ( simpleQueryString == null ) {
			throw LOG.simpleQueryParserDoesNotSupportNullQueries();
		}

		Query query;

		if ( queryContext.getQueryAnalyzerReference().is( RemoteAnalyzerReference.class ) ) {
			RemoteSimpleQueryStringQuery.Builder builder = new RemoteSimpleQueryStringQuery.Builder()
					.query( simpleQueryString )
					.withAndAsDefaultOperator( withAndAsDefaultOperator )
					.originalRemoteAnalyzerReference( queryContext.getOriginalAnalyzerReference().unwrap( RemoteAnalyzerReference.class ) )
					.queryRemoteAnalyzerReference( queryContext.getQueryAnalyzerReference().unwrap( RemoteAnalyzerReference.class ) );

			fieldsContexts.forEach( fieldsContext -> {
				fieldsContext.forEach( fieldContext -> {
					builder.field( fieldContext.getField(), fieldContext.getFieldCustomizer().getBoost() );
				} );
			} );

			query = builder.build();
		}
		else {
			ConnectedSimpleQueryParser queryParser = new ConnectedSimpleQueryParser(
					queryContext.getQueryAnalyzerReference().unwrap( LuceneAnalyzerReference.class ).getAnalyzer(), fieldsContexts );
			queryParser.setDefaultOperator( withAndAsDefaultOperator ? Occur.MUST : Occur.SHOULD );

			query = queryParser.parse( simpleQueryString );
		}

		return queryCustomizer.setWrappedQuery( query ).createQuery();
	}

}
