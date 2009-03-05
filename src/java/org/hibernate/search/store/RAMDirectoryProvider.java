//$Id$
package org.hibernate.search.store;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.hibernate.HibernateException;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Use a Lucene RAMDirectory
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 */
public class RAMDirectoryProvider implements DirectoryProvider<RAMDirectory> {

	private final RAMDirectory directory = new RAMDirectory();
	private String indexName;

	public void initialize(String directoryProviderName, Properties properties, SearchFactoryImplementor searchFactoryImplementor) {
		indexName = directoryProviderName;
		directory.setLockFactory( DirectoryProviderHelper.createLockFactory( null, properties ) );
	}

	public void start() {
		try {
			IndexWriter.MaxFieldLength fieldLength = new IndexWriter.MaxFieldLength( IndexWriter.DEFAULT_MAX_FIELD_LENGTH );
			IndexWriter iw = new IndexWriter( directory, new StandardAnalyzer(), true, fieldLength );
			iw.close();
		}
		catch (IOException e) {
			throw new HibernateException( "Unable to initialize index: " + indexName, e );
		}
	}


	public RAMDirectory getDirectory() {
		return directory;
	}

	public void stop() {
		directory.close();
	}

	@Override
	public boolean equals(Object obj) {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		if ( obj == this ) return true;
		if ( obj == null || !( obj instanceof RAMDirectoryProvider ) ) return false;
		return indexName.equals( ( (RAMDirectoryProvider) obj ).indexName );
	}

	@Override
	public int hashCode() {
		// this code is actually broken since the value change after initialize call
		// but from a practical POV this is fine since we only call this method
		// after initialize call
		int hash = 7;
		return 29 * hash + indexName.hashCode();
	}

}
