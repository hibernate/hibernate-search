/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.spi.DirectoryHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Use a Lucene {@link FSDirectory}. The base directory is represented by the property <i>hibernate.search.default.indexBase</i>
 * or <i>hibernate.search.&lt;index&gt;.indexBase</i>. The former defines the default base directory for all indexes whereas the
 * latter allows to override the base directory on a per index basis.<i> &lt;index&gt;</i> has to be replaced with the fully qualified
 * classname of the indexed class or the value of the <i>index</i> property of the <code>@Indexed</code> annotation.
 * <p>
 * The actual index files are then created in <i>&lt;indexBase&gt;/&lt;index name&gt;</i>, <i>&lt;index name&gt;</i> is
 * per default the name of the indexed entity, or the value of the <i>index</i> property of the <code>@Indexed</code> or can be specified
 * as property in the configuration file using <i>hibernate.search.&lt;index&gt;.indexName</i>.
 * </p>
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Sanne Grinovero
 */
public class FSDirectoryProvider implements DirectoryProvider<FSDirectory> {

	private static final Log log = LoggerFactory.make();

	private FSDirectory directory;
	private String indexName;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		// on "manual" indexing skip read-write check on index directory
		boolean manual = IndexingMode.MANUAL == context.getIndexingMode();
		File indexDir = DirectoryHelper.getVerifiedIndexDir( directoryProviderName, properties, !manual );
		try {
			indexName = indexDir.getCanonicalPath();
			//this is cheap so it's not done in start()
			directory = DirectoryProviderHelper.createFSIndex( indexDir, properties, context.getServiceManager() );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		//all the process is done in initialize
	}

	@Override
	public void stop() {
		try {
			directory.close();
		}
		catch (Exception e) {
			log.unableToCloseLuceneDirectory( directory.getDirectory(), e );
		}
	}

	@Override
	public FSDirectory getDirectory() {
		return directory;
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || !( obj instanceof FSDirectoryProvider ) ) {
			return false;
		}
		return indexName.equals( ( (FSDirectoryProvider) obj ).indexName );
	}

	@Override
	public int hashCode() {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		int hash = 11;
		return 37 * hash + indexName.hashCode();
	}
}
