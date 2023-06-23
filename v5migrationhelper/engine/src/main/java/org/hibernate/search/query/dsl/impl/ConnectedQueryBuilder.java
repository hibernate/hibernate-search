/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.AllContext;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.FacetContext;
import org.hibernate.search.query.dsl.PhraseContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.RangeContext;
import org.hibernate.search.query.dsl.SimpleQueryStringContext;
import org.hibernate.search.query.dsl.SpatialContext;
import org.hibernate.search.query.dsl.TermContext;
import org.hibernate.search.query.dsl.sort.SortContext;
import org.hibernate.search.query.dsl.sort.impl.ConnectedSortContext;

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

	@Override
	public TermContext keyword() {
		return new ConnectedTermContext( context );
	}

	@Override
	public RangeContext range() {
		return new ConnectedRangeContext( context );
	}

	@Override
	public PhraseContext phrase() {
		return new ConnectedPhraseContext( context );
	}

	@Override
	public SimpleQueryStringContext simpleQueryString() {
		return new ConnectedSimpleQueryStringContext( context );
	}

	//fixme Have to use raw types but would be nice to not have to
	@Override
	public BooleanJunction bool() {
		return new BooleanQueryBuilder( context );
	}

	@Override
	public AllContext all() {
		return new ConnectedAllContext( context );
	}

	@Override
	public FacetContext facet() {
		return new ConnectedFacetContext( new FacetBuildingContext( context ) );
	}

	@Override
	public SpatialContext spatial() {
		return new ConnectedSpatialContext( context );
	}

	@Override
	public SortContext sort() {
		return new ConnectedSortContext( context );
	}
}
