/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.service;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.spi.BuildContext;

/**
 * @author Hardy Ferentschik
 */
public class StartableServiceImpl implements StartableService, Startable {
	AtomicBoolean started = new AtomicBoolean( false );

	@Override
	public void start(Properties properties, BuildContext context) {
		started.set( true );
	}

	public boolean isStarted() {
		return started.get();
	}
}


