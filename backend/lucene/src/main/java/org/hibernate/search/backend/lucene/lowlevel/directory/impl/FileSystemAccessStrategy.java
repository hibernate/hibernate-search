/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.apache.lucene.store.SimpleFSDirectory;

enum FileSystemAccessStrategy {
	AUTO {
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return FSDirectory.open( indexDir, factory );
		}
	},
	SIMPLE {
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return new SimpleFSDirectory( indexDir, factory );
		}
	},
	NIO {
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return new NIOFSDirectory( indexDir, factory );
		}
	},
	MMAP {
		public FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException {
			return new MMapDirectory( indexDir, factory );
		}
	};

	public abstract FSDirectory createDirectory(Path indexDir, LockFactory factory) throws IOException;

	public static FileSystemAccessStrategy get(FileSystemAccessStrategyName name) {
		switch ( name ) {
			case AUTO:
				return AUTO;
			case SIMPLE:
				return SIMPLE;
			case NIO:
				return NIO;
			case MMAP:
				return MMAP;
		}
		throw new AssertionFailure( "Unexpected name: " + name );
	}
}