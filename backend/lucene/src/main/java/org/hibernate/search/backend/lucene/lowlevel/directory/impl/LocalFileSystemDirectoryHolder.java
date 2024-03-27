/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;

final class LocalFileSystemDirectoryHolder implements DirectoryHolder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Path directoryPath;
	private final FileSystemAccessStrategy accessStrategy;
	private final Supplier<LockFactory> lockFactorySupplier;
	private final EventContext eventContext;

	private Directory directory;

	LocalFileSystemDirectoryHolder(Path directoryPath, FileSystemAccessStrategy accessStrategy,
			Supplier<LockFactory> lockFactorySupplier, EventContext eventContext) {
		this.directoryPath = directoryPath;
		this.accessStrategy = accessStrategy;
		this.lockFactorySupplier = lockFactorySupplier;
		this.eventContext = eventContext;
	}

	@Override
	public void start() throws IOException {
		try {
			FileSystemUtils.initializeWriteableDirectory( directoryPath );
		}
		catch (Exception e) {
			throw log.unableToInitializeIndexDirectory( e.getMessage(), eventContext, e );
		}

		this.directory = accessStrategy.createDirectory( directoryPath, lockFactorySupplier.get() );
	}

	@Override
	public void close() throws IOException {
		if ( directory != null ) {
			directory.close();
		}
		directory = null;
	}

	@Override
	public Directory get() {
		return directory;
	}
}
