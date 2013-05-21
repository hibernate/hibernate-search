/*
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

package org.hibernate.search.test.directoryProvider;

import java.util.Properties;

import org.apache.lucene.store.RAMDirectory;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.impl.RAMDirectoryProvider;

/**
 * Verifies that a DirectoryProvider lifecycle is managed properly:
 * it's initialized and started at SearchFactory initialization and closed at shutdown.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
