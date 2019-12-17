/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class FailSafeMassIndexingFailureHandlerWrapper implements MassIndexingFailureHandler {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MassIndexingFailureHandler delegate;

	public FailSafeMassIndexingFailureHandlerWrapper(MassIndexingFailureHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(MassIndexingFailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			log.failureInMassIndexingFailureHandler( t );
		}
	}

	@Override
	public void handle(MassIndexingEntityFailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			log.failureInMassIndexingFailureHandler( t );
		}
	}

}
