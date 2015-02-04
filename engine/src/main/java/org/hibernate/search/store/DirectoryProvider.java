/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store;

import java.util.Properties;

import org.apache.lucene.store.Directory;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;

/**
 * Set up and provide a Lucene {@code Directory}
 * {@code equals()} and {@code hashCode()} must guaranty equality
 * between two providers pointing to the same underlying Lucene Store.
 * Besides that, {@code hashCode} ordering is used to avoid deadlock when locking a directory provider.
 *
 * This class must be thread safe regarding {@code getDirectory()} calls
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 */
public interface DirectoryProvider<TDirectory extends Directory> {

	/**
	 * @param indexName
	 * @param properties
	 * @param context
	 *
	 * get the information to initialize the directory and build its hashCode/equals method
	 */
	void initialize(String indexName, Properties properties, BuildContext context);

	/**
	 * Executed after initialize, this method set up the heavy process of starting up the DirectoryProvider
	 * IO processing as well as background processing are expected to be set up here
	 *
	 */
	void start(DirectoryBasedIndexManager indexManager);

	/**
	 * Executed when the search factory is closed. This method should stop any background process as well as
	 * releasing any resource.
	 * This method should avoid raising exceptions and log potential errors instead
	 */
	void stop();

	/**
	 * Returns an initialized Lucene Directory. This method call <b>must</b> be threadsafe
	 */
	TDirectory getDirectory();
}
