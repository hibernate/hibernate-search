/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;

public abstract class ConditionalExpressionQueryFactory<E, I> implements TypeQueryFactory<E, I> {

	private static final String TYPES_PARAM_NAME = "HIBERNATE_SEARCH_INCLUDED_TYPES_FILTER";
	protected final Class<I> uniquePropertyType;
	protected final String uniquePropertyName;
	private final boolean uniquePropertyIsTheEntityId;

	public ConditionalExpressionQueryFactory(Class<I> uniquePropertyType, String uniquePropertyName,
			boolean uniquePropertyIsTheEntityId) {
		this.uniquePropertyType = uniquePropertyType;
		this.uniquePropertyName = uniquePropertyName;
		this.uniquePropertyIsTheEntityId = uniquePropertyIsTheEntityId;
	}

	@Override
	public final boolean uniquePropertyIsTheEntityId() {
		return uniquePropertyIsTheEntityId;
	}

	@Override
	public Query<Long> createQueryForCount(SharedSessionContractImplementor session, EntityMappingType entityMappingType,
			Set<? extends Class<? extends E>> includedTypesFilter,
			List<ConditionalExpression> conditionalExpressions) {
		return createQueryWithConditionalExpressionsOrOrder( session,
				"select count(e) from " + entityMappingType.getEntityName() + " e",
				Long.class, "e", includedTypesFilter, conditionalExpressions, null
		);
	}

	@Override
	public Query<I> createQueryForIdentifierListing(SharedSessionContractImplementor session,
			EntityMappingType entityMappingType,
			Set<? extends Class<? extends E>> includedTypesFilter,
			List<ConditionalExpression> conditionalExpressions, String order) {
		return createQueryWithConditionalExpressionsOrOrder( session,
				"select e." + uniquePropertyName + " from " + entityMappingType.getEntityName() + " e",
				uniquePropertyType, "e",
				includedTypesFilter, conditionalExpressions, order
		);
	}

	private <T> Query<T> createQueryWithConditionalExpressionsOrOrder(SharedSessionContractImplementor session,
			String hql, Class<T> returnedType, String entityAlias,
			Set<? extends Class<? extends E>> includedTypesFilter,
			List<ConditionalExpression> conditionalExpressions, String order) {
		List<ConditionalExpression> allConditionalExpressions;
		if ( !includedTypesFilter.isEmpty() ) {
			ConditionalExpression typeFilter =
					new ConditionalExpression( "type(" + entityAlias + ") in (:" + TYPES_PARAM_NAME + ")" );
			typeFilter.param( TYPES_PARAM_NAME, includedTypesFilter );
			allConditionalExpressions = new ArrayList<>();
			allConditionalExpressions.add( typeFilter );
			allConditionalExpressions.addAll( conditionalExpressions );
		}
		else {
			allConditionalExpressions = conditionalExpressions;
		}
		return createQueryWithConditionalExpressionsOrOrder( session, hql, returnedType, allConditionalExpressions, order );
	}

	private <T> Query<T> createQueryWithConditionalExpressionsOrOrder(SharedSessionContractImplementor session,
			String hql, Class<T> returnedType,
			List<ConditionalExpression> conditionalExpressions, String order) {
		StringBuilder hqlBuilder = new StringBuilder( hql );
		if ( !conditionalExpressions.isEmpty() ) {
			hqlBuilder.append( " where " );
			boolean first = true;
			for ( ConditionalExpression expression : conditionalExpressions ) {
				if ( first ) {
					first = false;
				}
				else {
					hqlBuilder.append( " and " );
				}
				hqlBuilder.append( "(" ).append( expression.hql() ).append( ")" );
			}
		}
		if ( order != null ) {
			hqlBuilder.append( " order by " ).append( order );
		}
		Query<T> query = session.createQuery( hqlBuilder.toString(), returnedType );
		for ( ConditionalExpression expression : conditionalExpressions ) {
			expression.applyParams( query );
		}
		return query;
	}
}
