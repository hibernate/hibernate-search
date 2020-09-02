/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.PhraseTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsPhraseQueryBuilder implements PhraseTermination {
	private final PhraseQueryContext phraseContext;
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContext;

	public ConnectedMultiFieldsPhraseQueryBuilder(PhraseQueryContext phraseContext, QueryCustomizer queryCustomizer,
			FieldsContext fieldsContext, QueryBuildingContext queryContext) {
		this.phraseContext = phraseContext;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;
	}

	@Override
	public Query createQuery() {
		final int size = fieldsContext.size();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldsContext.getFirst() ) ).createQuery();
		}
		else {
			BooleanQuery.Builder aggregatedFieldsQueryBuilder = new BooleanQuery.Builder();
			for ( FieldContext fieldContext : fieldsContext ) {
				aggregatedFieldsQueryBuilder.add( createQuery( fieldContext ), BooleanClause.Occur.SHOULD );
			}
			BooleanQuery aggregatedFieldsQuery = aggregatedFieldsQueryBuilder.build();
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	public Query createQuery(FieldContext fieldContext) {
		throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
	}
}
