/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.util.impl;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Created by Martin on 12.11.2015.
 */
public final class ORMQueryWrapper implements QueryWrapper {

	private final SQLQuery sqlQuery;
	private final Query query;

	public ORMQueryWrapper(Query query, SQLQuery sqlQuery) {
		this.sqlQuery = sqlQuery;
		this.query = query;
	}

	@Override
	public void setMaxResults(long maxResults) {
		if ( this.sqlQuery != null ) {
			this.sqlQuery.setMaxResults( (int) maxResults );
		}
		else if ( this.query != null ) {
			this.query.setMaxResults( (int) maxResults );
		}
		else {
			throw new AssertionFailure( "one of sqlQuery and query must not be null" );
		}
	}

	@Override
	public void setFirstResult(long firstResult) {
		if ( this.sqlQuery != null ) {
			this.sqlQuery.setFirstResult( (int) firstResult );
		}
		else if ( this.query != null ) {
			this.query.setFirstResult( (int) firstResult );
		}
		else {
			throw new AssertionFailure( "one of sqlQuery and query must not be null" );
		}
	}

	@Override
	public List getResultList() {
		if ( this.sqlQuery != null ) {
			return this.sqlQuery.list();
		}
		else if ( this.query != null ) {
			return this.query.list();
		}
		else {
			throw new AssertionFailure( "one of sqlQuery and query must not be null" );
		}
	}

	@Override
	public Object getSingleResult() {
		if ( this.sqlQuery != null ) {
			return this.sqlQuery.uniqueResult();
		}
		else if ( this.query != null ) {
			return this.query.uniqueResult();
		}
		else {
			throw new AssertionFailure( "one of sqlQuery and query must not be null" );
		}
	}

	@Override
	public int executeUpdate() {
		if ( this.sqlQuery != null ) {
			return this.sqlQuery.executeUpdate();
		}
		else if ( this.query != null ) {
			return this.query.executeUpdate();
		}
		else {
			throw new AssertionFailure( "one of sqlQuery and query must not be null" );
		}
	}

}
