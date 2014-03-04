/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.MoreLikeThisOpenedMatchingContext;
import org.hibernate.search.query.dsl.MoreLikeThisTerminalMatchingContext;
import org.hibernate.search.query.dsl.MoreLikeThisTermination;
import org.hibernate.search.query.dsl.MoreLikeThisToEntityContentAndTermination;

import static org.hibernate.search.query.dsl.impl.ConnectedMoreLikeThisQueryBuilder.INPUT_TYPE.ENTITY;
import static org.hibernate.search.query.dsl.impl.ConnectedMoreLikeThisQueryBuilder.INPUT_TYPE.ID;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
		this.fieldsContext = new FieldsContext( fieldNames );
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
