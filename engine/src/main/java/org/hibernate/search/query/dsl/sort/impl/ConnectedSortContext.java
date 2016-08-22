/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.apache.lucene.search.SortField;
import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortContext;
import org.hibernate.search.query.dsl.sort.SortDistanceNoFieldContext;
import org.hibernate.search.query.dsl.sort.SortFieldContext;
import org.hibernate.search.query.dsl.sort.SortNativeContext;
import org.hibernate.search.query.dsl.sort.SortOrderTermination;
import org.hibernate.search.query.dsl.sort.SortScoreContext;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortContext extends AbstractConnectedSortContext implements SortContext {

	public ConnectedSortContext(QueryBuildingContext queryContext) {
		super( queryContext, new SortFieldStates( queryContext ) );
	}

	@Override
	public SortScoreContext byScore() {
		states.setCurrentType( SortField.Type.SCORE );
		return new ConnectedSortScoreContext( queryContext, states );
	}

	@Override
	public SortOrderTermination byIndexOrder() {
		states.setCurrentType( SortField.Type.DOC );
		return new ConnectedSortOrderTermination( queryContext, states );
	}

	@Override
	public SortFieldContext byField(String field) {
		states.setCurrentName( field );
		states.determineCurrentSortFieldTypeAutomaticaly();
		return new ConnectedSortFieldContext( queryContext, states );
	}

	@Override
	public SortFieldContext byField(String field, SortField.Type sortFieldType) {
		states.setCurrentName( field );
		states.setCurrentType( sortFieldType );
		return new ConnectedSortFieldContext( queryContext, states );
	}

	@Override
	public SortDistanceNoFieldContext byDistance() {
		return new ConnectedSortDistanceNoFieldContext( queryContext, states );
	}

	@Override
	public SortNativeContext byNative(SortField sortField) {
		states.setCurrentSortFieldNativeSortDescription( sortField );
		return new ConnectedSortNativeContext( queryContext, states );
	}

	@Override
	public SortNativeContext byNative(String field, String nativeDescription) {
		states.setCurrentName( field );
		states.setCurrentStringNativeSortFieldDescription( nativeDescription );
		return new ConnectedSortNativeContext( queryContext, states );
	}

}
