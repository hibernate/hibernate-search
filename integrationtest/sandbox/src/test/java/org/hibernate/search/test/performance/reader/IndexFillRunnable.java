/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

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

	@Override
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
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
