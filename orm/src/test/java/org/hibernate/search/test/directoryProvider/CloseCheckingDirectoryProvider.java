/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.directoryProvider;

import java.util.Properties;

import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.impl.RAMDirectoryProvider;

/**
 * Verifies that a DirectoryProvider lifecycle is managed properly:
 * it's initialized and started at SearchFactory initialization and closed at shutdown.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class CloseCheckingDirectoryProvider extends RAMDirectoryProvider {

	private volatile boolean initialized = false;
	private volatile boolean stopped = false;
	private volatile boolean started = false;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		super.initialize( directoryProviderName, properties, context );
		if ( initialized ) {
			throw new SearchException( "Initialized twice" );
		}
		if ( started ) {
			throw new SearchException( "Initialized after start" );
		}
		if ( stopped ) {
			throw new SearchException( "Initialized after stop" );
		}
		this.initialized = true;
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		super.start( indexManager );
		if ( ! initialized ) {
			throw new SearchException( "Started without being initialized" );
		}
		if ( started ) {
			throw new SearchException( "Started twice" );
		}
		if ( stopped ) {
			throw new SearchException( "Can not be started after being stopped" );
		}
		this.started = true;
	}

	@Override
	public RAMDirectory getDirectory() {
		if ( ! initialized ) {
			throw new SearchException( "Can not be used before initialization" );
		}
		if ( ! started ) {
			throw new SearchException( "Can not be used before being started" );
		}
		if ( stopped ) {
			throw new SearchException( "Can not be used after being stopped" );
		}
		return super.getDirectory();
	}

	@Override
	public void stop() {
		super.stop();
		if ( ! initialized ) {
			throw new SearchException( "Stopped before initialization" );
		}
		if ( ! started ) {
			throw new SearchException( "Stopped before being started" );
		}
		if ( stopped ) {
			throw new SearchException( "Stopped twice" );
		}
		this.stopped = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public boolean isStopped() {
		return stopped;
	}

	public boolean isStarted() {
		return started;
	}

}
