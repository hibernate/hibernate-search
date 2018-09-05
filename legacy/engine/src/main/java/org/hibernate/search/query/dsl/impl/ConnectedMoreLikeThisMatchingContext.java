/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.MoreLikeThisOpenedMatchingContext;
import org.hibernate.search.query.dsl.MoreLikeThisTerminalMatchingContext;
import org.hibernate.search.query.dsl.MoreLikeThisTermination;
import org.hibernate.search.query.dsl.MoreLikeThisToEntityContentAndTermination;

import static org.hibernate.search.query.dsl.impl.ConnectedMoreLikeThisQueryBuilder.INPUT_TYPE.ENTITY;
import static org.hibernate.search.query.dsl.impl.ConnectedMoreLikeThisQueryBuilder.INPUT_TYPE.ID;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMoreLikeThisMatchingContext implements MoreLikeThisOpenedMatchingContext, MoreLikeThisTerminalMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final MoreLikeThisQueryContext moreLikeThisContext;
	private final FieldsContext fieldsContext;

	public ConnectedMoreLikeThisMatchingContext(String[] fieldNames, MoreLikeThisQueryContext moreLikeThisContext, QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
		this.moreLikeThisContext = moreLikeThisContext;
		this.fieldsContext = new FieldsContext( fieldNames, queryContext );
	}

	@Override
	public MoreLikeThisOpenedMatchingContext andField(String fieldname) {
		fieldsContext.add( fieldname );
		return this;
	}

	@Override
	public MoreLikeThisOpenedMatchingContext boostedTo(float boost) {
		fieldsContext.boostedTo( boost );
		return this;
	}

	@Override
	public MoreLikeThisOpenedMatchingContext ignoreAnalyzer() {
		fieldsContext.ignoreAnalyzer();
		return this;
	}

	@Override
	public MoreLikeThisOpenedMatchingContext ignoreFieldBridge() {
		fieldsContext.ignoreFieldBridge();
		return this;
	}

	@Override
	public MoreLikeThisTermination toEntityWithId(Object id) {
		return new ConnectedMoreLikeThisQueryBuilder.MoreLikeThisTerminationImpl(
				id,
				ID,
				fieldsContext,
				moreLikeThisContext,
				queryCustomizer,
				queryContext
		);
	}

	@Override
	public MoreLikeThisToEntityContentAndTermination toEntity(Object entity) {
		return new ConnectedMoreLikeThisQueryBuilder.MoreLikeThisToEntityContentAndTerminationImpl(
				entity,
				ENTITY,
				fieldsContext,
				moreLikeThisContext,
				queryCustomizer,
				queryContext
		);
	}

}
