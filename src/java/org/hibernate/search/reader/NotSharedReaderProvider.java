//$Id$
package org.hibernate.search.reader;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.SearchFactoryImplementor;
import static org.hibernate.search.reader.ReaderProviderHelper.buildMultiReader;
import static org.hibernate.search.reader.ReaderProviderHelper.clean;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Open a reader each time
 *
 * @author Emmanuel Bernard
 */
public class NotSharedReaderProvider implements ReaderProvider {
	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	public IndexReader openReader(DirectoryProvider... directoryProviders) {
		final int length = directoryProviders.length;
		IndexReader[] readers = new IndexReader[length];
		try {
			for (int index = 0; index < length; index++) {
				readers[index] = IndexReader.open( directoryProviders[index].getDirectory(), true );
			}
		}
		catch (IOException e) {
			//TODO more contextual info
			clean( new SearchException( "Unable to open one of the Lucene indexes", e ), readers );
		}
		return buildMultiReader( length, readers );
	}


	@SuppressWarnings( { "ThrowableInstanceNeverThrown" } )
	public void closeReader(IndexReader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			//TODO extract subReaders and close each one individually
			clean( new SearchException( "Unable to close multiReader" ), reader );
		}
	}

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
	}

	public void destroy() {
	}
}
