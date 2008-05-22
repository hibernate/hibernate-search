//$Id$
package org.hibernate.search.store;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use a Lucene FSDirectory
 * The base directory is represented by hibernate.search.<index>.indexBase
 * The index is created in <base directory>/<index name>
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Sanne Grinovero
 */
public class FSDirectoryProvider implements DirectoryProvider<FSDirectory> {

	private final Logger log = LoggerFactory.getLogger( FSDirectoryProvider.class );
	
	private FSDirectory directory;
	private String indexName;

	public void initialize(String directoryProviderName, Properties properties, SearchFactoryImplementor searchFactoryImplementor) {
		// on "manual" indexing skip read-write check on index directory
		boolean manual = searchFactoryImplementor.getIndexingStrategy().equals( "manual" );
		File indexDir = DirectoryProviderHelper.getVerifiedIndexDir( directoryProviderName, properties, ! manual );
		try {
			indexName = indexDir.getCanonicalPath();
			//this is cheap so it's not done in start()
			directory = DirectoryProviderHelper.createFSIndex( indexDir );
		}
		catch (IOException e) {
			throw new SearchException( "Unable to initialize index: " + directoryProviderName, e );
		}
	}

	public void start() {
		//all the process is done in initialize
	}

	public void stop() {
		try {
			directory.close();
		}
		catch (Exception e) {
			log.error( "Unable to property close Lucene directory {}" + directory.getFile(), e );
		}
	}

	public FSDirectory getDirectory() {
		return directory;
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) return true;
		if ( obj == null || !( obj instanceof FSDirectoryProvider ) ) return false;
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
