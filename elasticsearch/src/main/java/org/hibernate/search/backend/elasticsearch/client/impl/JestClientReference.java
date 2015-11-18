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

import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;

/**
 * A reference to the JEST client service, simplifying its usage via try-with-resources.
 *
 * @author Gunnar Morling
 */
public class JestClientReference implements AutoCloseable {

	private final ServiceManager serviceManager;
	private final JestClientService service;

	public JestClientReference(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
		this.service = serviceManager.requestService( JestClientService.class );
	}

	public <T extends JestResult> T executeRequest(Action<T> request) {
		return executeRequest( request, true );
	}

	public <T extends JestResult> T executeRequest(Action<T> request, boolean failOnError) {
		T result;
		try {
			result = service.getClient().execute( request );

			if ( failOnError && !result.isSucceeded() ) {
				throw new SearchException( result.getErrorMessage() );
			}

			return result;
		}
		catch (IOException e) {
			throw new SearchException( e );
		}
	}

	@Override
	public void close() {
		serviceManager.releaseService( JestClientService.class );
	}
}
