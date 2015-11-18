/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;

import java.io.IOException;

import org.hibernate.search.exception.SearchException;

/**
 *
 * @author Gunnar Morling
 *
 */
public class RequestHelper {

	public static <T extends JestResult > T executeRequest(Action<T> request) {
		return executeRequest( request, true );
	}

	public static <T extends JestResult > T executeRequest(Action<T> request, boolean failOnError) {
		T result;
		try {
			result = JestClientHolder.getClient().execute( request );

			System.out.println( result.getJsonString() );

			if ( failOnError && !result.isSucceeded() ) {
				throw new SearchException( result.getErrorMessage() );
			}

			return result;
		}
		catch (IOException e) {
			throw new SearchException( e );
		}
	}
}
