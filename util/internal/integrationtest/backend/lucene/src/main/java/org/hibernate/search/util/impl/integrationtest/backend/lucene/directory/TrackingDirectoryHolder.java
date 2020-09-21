/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.directory;

import java.io.IOException;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;

class TrackingDirectoryHolder implements DirectoryHolder {

	private final DirectoryHolder delegate;
	private final Supplier<LockFactory> lockFactorySupplier;
	private final OpenResourceTracker tracker;

	private TrackingDirectory directory;

	public TrackingDirectoryHolder(DirectoryHolder delegate, Supplier<LockFactory> lockFactorySupplier, OpenResourceTracker tracker) {
		this.delegate = delegate;
		this.lockFactorySupplier = lockFactorySupplier;
		this.tracker = tracker;
	}

	@Override
	public void start() throws IOException {
		delegate.start();
		this.directory = new TrackingDirectory( delegate.get(), lockFactorySupplier.get(), tracker );
		tracker.onOpen( this );
	}

	@Override
	public Directory get() {
		return directory;
	}

	@Override
	public void close() throws IOException {
		if ( directory != null ) {
			directory.close();
		}
		this.directory = null;
		delegate.close();
		tracker.onClose( this );
	}
}
