/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProviderInitializationContext;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;

public class LocalHeapDirectoryProvider implements DirectoryProvider {

	public static final String NAME = "local-heap";

	private LockFactory lockFactory;

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void initialize(DirectoryProviderInitializationContext context) {
		this.lockFactory = context.createConfiguredLockFactory().orElseGet( SingleInstanceLockFactory::new );
	}

	@Override
	public DirectoryHolder createDirectoryHolder(DirectoryCreationContext context) {
		return new LocalHeapDirectoryHolder( lockFactory );
	}
}
