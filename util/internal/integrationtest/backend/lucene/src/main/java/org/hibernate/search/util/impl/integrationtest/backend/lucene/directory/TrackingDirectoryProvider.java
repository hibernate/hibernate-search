/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.directory;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;

import org.apache.lucene.store.SingleInstanceLockFactory;

/**
 * A directory provider that delegates to another, but makes sure to wrap the created directories
 * to register all opened resources (index input or index ouput)
 * to the provided {@link OpenResourceTracker}.
 */
public class TrackingDirectoryProvider implements DirectoryProvider {

	private final DirectoryProvider delegate;
	private final OpenResourceTracker tracker;

	public TrackingDirectoryProvider(DirectoryProvider delegate, OpenResourceTracker tracker) {
		this.delegate = delegate;
		this.tracker = tracker;
	}

	@Override
	public DirectoryHolder createDirectoryHolder(DirectoryCreationContext context) {
		return new TrackingDirectoryHolder( delegate.createDirectoryHolder( context ),
				context.createConfiguredLockFactorySupplier()
						.orElseGet( () -> SingleInstanceLockFactory::new ),
				tracker );
	}
}
