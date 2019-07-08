/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.store.Directory;

public interface DirectoryHolder extends Closeable {

	/**
	 * Release any resource currently held by the {@link DirectoryHolder},
	 * including (but not limiting to) the directory itself.
	 * <p>
	 * After this method has been called, the result of calling any other method on the same instance is undefined.
	 *
	 * @throws IOException If an error occurs while releasing resources.
	 * @throws RuntimeException If an error occurs while releasing resources.
	 */
	@Override
	void close() throws IOException;

	/**
	 * @return The directory held by this {@link DirectoryHolder}.
	 */
	Directory get();

	/**
	 * @param directory The {@link Directory} to hold.
	 * @return A {@link DirectoryHolder} that returns the given directory when {@link #get()} is called
	 * and simply closes the directory when {@link #close()} is called.
	 */
	static DirectoryHolder of(Directory directory) {
		return new SimpleDirectoryHolder( directory );
	}

}
