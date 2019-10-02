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
	 * Allocate internal resources (filesystem directories, ...) as necessary,
	 * along with the directory itself.
	 * <p>
	 * After this method has been called, {@link #get()} can be safely called.
	 *
	 * @throws IOException If an error occurs while creating resources.
	 * @throws RuntimeException If an error occurs while creating resources.
	 */
	void start() throws IOException;

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

}
