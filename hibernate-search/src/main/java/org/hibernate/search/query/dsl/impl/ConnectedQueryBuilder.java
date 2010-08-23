package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.AllContext;
import org.hibernate.search.query.dsl.PhraseContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.RangeContext;
import org.hibernate.search.query.dsl.TermContext;

/**
 * Assuming connection with the search factory
 * 
 * @author Emmanuel Bernard
 */
public class ConnectedQueryBuilder implements QueryBuilder {
	private final QueryBuildingContext context;

	public ConnectedQueryBuilder(QueryBuildingContext context) {
		this.context = context;
	}

	public TermContext keyword() {
		return new ConnectedTermContext(context);
	}

	public RangeContext range() {
		return new ConnectedRangeContext(context);
	}

	public PhraseContext phrase() {
		return new ConnectedPhraseContext(context);
	}

	//fixme Have to use raw types but would be nice to not have to
	public BooleanJunction bool() {
		return new BooleanQueryBuilder();
	}

	public AllContext all() {
		return new ConnectedAllContext();
	}
}
