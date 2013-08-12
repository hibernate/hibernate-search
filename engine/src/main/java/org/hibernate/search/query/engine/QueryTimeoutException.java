/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.query.engine;

import org.apache.lucene.search.Query;

import org.hibernate.search.SearchException;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;

/**
 * Represent a timeout during a Fulltext search in the HSQuery.
 * The object source integration should catch this and throw a
 * relevant exception for the object source. For example in Hibernate Core, an
 * {@link org.hibernate.QueryTimeoutException}.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class QueryTimeoutException extends SearchException {

	public static final TimeoutExceptionFactory DEFAULT_TIMEOUT_EXCEPTION_FACTORY = new DefaultSearchTimeoutException();

	private QueryTimeoutException(String message, Query query) {
		super( message + " \"" + query + '\"' );
	}

	private static class DefaultSearchTimeoutException implements TimeoutExceptionFactory {

		@Override
		public QueryTimeoutException createTimeoutException(String message, Query query) {
			return new QueryTimeoutException( message, query );
		}

	}

}
