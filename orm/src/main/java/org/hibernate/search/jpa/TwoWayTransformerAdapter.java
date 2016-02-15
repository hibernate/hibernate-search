/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.util.List;

import org.hibernate.search.exception.SearchException;

/**
 * Created by Martin on 20.11.2015.
 */
public class TwoWayTransformerAdapter implements ResultTransformerAdapter {

	private final ResultTransformer transformer;
	private final org.hibernate.transform.ResultTransformer hibernateTransfomer;

	public TwoWayTransformerAdapter(ResultTransformer transformer) {
		this.transformer = transformer;
		this.hibernateTransfomer = null;
	}

	public TwoWayTransformerAdapter(org.hibernate.transform.ResultTransformer hibernateTransfomer) {
		this.transformer = null;
		this.hibernateTransfomer = hibernateTransfomer;
	}

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		if ( this.transformer != null ) {
			return this.transformer.transformTuple( tuple, aliases );
		}
		else if ( this.hibernateTransfomer != null ) {
			return this.hibernateTransfomer.transformTuple( tuple, aliases );
		}
		else {
			throw new SearchException( "internal transformer was null" );
		}
	}

	@Override
	public List transformList(List collection) {
		if ( this.transformer != null ) {
			return this.transformer.transformList( collection );
		}
		else if ( this.hibernateTransfomer != null ) {
			return this.hibernateTransfomer.transformList( collection );
		}
		else {
			throw new SearchException( "internal transformer was null" );
		}
	}

}
