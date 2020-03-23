/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.index.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;

import org.apache.lucene.index.DirectoryReader;

/**
 * The entry point for low-level I/O operations on a Lucene index.
 * <p>
 * Used to retrieve the index writer and index reader in particular.
 */
public interface IndexAccessor {

	/**
	 * Checks whether the index exists (on disk, ...), and creates it if necessary.
	 */
	void createIndexIfMissing();

	/**
	 * Checks whether the index exists (on disk, ...), and throws an exception if it doesn't.
	 */
	void validateIndexExists();

	/**
	 * Checks whether the index exists (on disk, ...), and drops it if it exists.
	 */
	void dropIndexIfExisting();

	/**
	 * Commits the underlying index writer, if any.
	 */
	void commit();

	/**
	 * Commits the underlying index writer, if any,
	 * or delay the commit if a commit happened recently
	 * and configuration requires to wait longer between two commits.
	 *
	 * @return {@code 0} if the commit occurred or is not necessary (all changes have already been committed).
	 * If a commit is necessary but needs to be delayed,
	 * returns the number of milliseconds until the moment a commit can be executed.
	 */
	long commitOrDelay();

	/**
	 * Refreshes the underlying index readers.
	 */
	void refresh();

	/**
	 * Merge segments files.
	 */
	void mergeSegments();

	/**
	 * @return The index writer delegator.
	 */
	IndexWriterDelegator getIndexWriterDelegator() throws IOException;

	/**
	 * @return The most up-to-date index reader available.
	 */
	DirectoryReader getIndexReader() throws IOException;

	/**
	 * Closes, drops and re-creates any cached resources: index writers, index readers.
	 * <p>
	 * Should be used to clean up the accessor upon write or commit failure,
	 * passing an exception with as much information as possible (operation, document ID, ...).
	 *
	 * @param throwable The failure.
	 * @param failingOperation The operation that failed.
	 */
	void cleanUpAfterFailure(Throwable throwable, Object failingOperation);
}
