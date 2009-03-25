// $Id$
package org.hibernate.search.test.reader.performance;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;

/**
 * @author Sanne Grinovero
 */
public class IndexFillRunnable implements Runnable {

	private volatile int jobSeed = 0;
	private final IndexWriter iw;

	public IndexFillRunnable(IndexWriter iw) {
		super();
		this.iw = iw;
	}

	public void run() {
		Field f1 = new Field( "name", "Some One " + jobSeed++, Store.NO, Index.ANALYZED );
		Field f2 = new Field(
				"physicalDescription",
				" just more people sitting around and filling my index... ",
				Store.NO,
				Index.ANALYZED
		);
		Document d = new Document();
		d.add( f1 );
		d.add( f2 );
		try {
			iw.addDocument( d );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}

}
