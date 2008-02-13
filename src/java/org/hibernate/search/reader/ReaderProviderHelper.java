//$Id$
package org.hibernate.search.reader;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 */
public abstract class ReaderProviderHelper {
	public static IndexReader buildMultiReader(int length, IndexReader[] readers) {
		if ( length == 0 ) {
			return null;
		}
		else if ( length == 1 ) {
			//everything should be the same so wrap in an MultiReader
			//return readers[0];
			try {
				return new CacheableMultiReader( readers );
			}
			catch (Exception e) {
				//Lucene 2.2 used to through IOExceptions here
				clean( new SearchException( "Unable to open a MultiReader", e ), readers );
				return null; //never happens, but please the compiler
			}
		}
		else {
			try {
				return new CacheableMultiReader( readers );
			}
			catch (Exception e) {
				//Lucene 2.2 used to through IOExceptions here
				clean( new SearchException( "Unable to open a MultiReader", e ), readers );
				return null; //never happens, but please the compiler
			}
		}
	}

	public static void clean(SearchException e, IndexReader... readers) {
		for (IndexReader reader : readers) {
			if ( reader != null ) {
				try {
					reader.close();
				}
				catch (IOException ee) {
					//swallow
				}
			}
		}
		throw e;
	}
}
