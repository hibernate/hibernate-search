/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
