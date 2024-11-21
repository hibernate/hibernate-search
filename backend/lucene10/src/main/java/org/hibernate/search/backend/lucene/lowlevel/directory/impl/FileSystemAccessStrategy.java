/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.impl;

import java.io.IOException;
import java.nio.file.Path;

import org.hibernate.search.backend.lucene.lowlevel.directory.FileSystemAccessStrategyName;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;

enum FileSystemAccessStrategy {
	AUTO {
		@Override
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return FSDirectory.open( indexDir, factory );
		}
	},
	NIO {
		@Override
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return new NIOFSDirectory( indexDir, factory );
		}
	},
	MMAP {
		@Override
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return new MMapDirectory( indexDir, factory );
		}
	};

	public abstract FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException;

	@SuppressWarnings("deprecation")
	public static FileSystemAccessStrategy get(FileSystemAccessStrategyName name) {
		switch ( name ) {
			case AUTO:
				return AUTO;
			case NIO:
				return NIO;
			case MMAP:
				return MMAP;
		}
		throw new AssertionFailure( "Unexpected name: " + name );
	}
}
