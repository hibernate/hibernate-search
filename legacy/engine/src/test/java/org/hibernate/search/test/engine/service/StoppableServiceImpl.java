/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.engine.service.spi.Stoppable;

/**
 * @author Hardy Ferentschik
 */
public class StoppableServiceImpl implements StoppableService, Stoppable {
	AtomicBoolean stopped = new AtomicBoolean( false );

	@Override
	public void stop() {
		stopped.set( true );
	}

	public boolean isStopped() {
		return stopped.get();
	}
}


