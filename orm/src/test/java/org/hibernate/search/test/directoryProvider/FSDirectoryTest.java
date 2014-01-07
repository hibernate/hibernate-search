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
package org.hibernate.search.test.directoryProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.Session;
import org.hibernate.search.Environment;
import org.hibernate.search.test.Document;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.Assert;

/**
 * @author Gavin King
 * @author Sanne Grinovero
 */
public class FSDirectoryTest extends SearchTestCase {

	public void testEventIntegration() throws Exception {
		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist(
				new Document( "Hibernate in Action", "Object/relational mapping with Hibernate", "blah blah blah" )
		);
		s.getTransaction().commit();
		s.close();

		Directory dir = FSDirectory.open( new File( getBaseIndexDir().toString(), "Documents" ) );
		try {
			IndexReader reader = DirectoryReader.open( dir );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
				assertEquals( 1, reader.docFreq( new Term( "Abstract", "hibernate" ) ) );
				assertEquals( 1, reader.docFreq( new Term( "title", "action" ) ) );
				assertEquals( "1", projectSingleField( reader, "id", new Term( "title", "action" ) ) );

			}
			finally {
				reader.close();
			}

			s = getSessionFactory().openSession();
			s.getTransaction().begin();
			Document entity = (Document) s.get( Document.class, Long.valueOf( 1 ) );
			entity.setSummary( "Object/relational mapping with EJB3" );
			s.persist( new Document( "Seam in Action", "", "blah blah blah blah" ) );
			s.getTransaction().commit();
			s.close();

			reader = DirectoryReader.open( dir );
			try {
				int num = reader.numDocs();
				assertEquals( 2, num );
				assertEquals( 1, reader.docFreq( new Term( "Abstract", "ejb" ) ) );
			}
			finally {
				reader.close();
			}

			s = getSessionFactory().openSession();
			s.getTransaction().begin();
			s.delete( entity );
			s.getTransaction().commit();
			s.close();

			reader = DirectoryReader.open( dir );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
				assertEquals( 1, reader.docFreq( new Term( "title", "seam" ) ) );
				assertEquals( "2", projectSingleField( reader, "id", new Term( "title", "seam" ) ) );
			}
			finally {
				reader.close();
			}
		}
		finally {
			dir.close();
		}

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.delete( s.createCriteria( Document.class ).uniqueResult() );
		s.getTransaction().commit();
		s.close();
	}

	/**
	 * Project a field as a String from a Lucene Document matching the provided term.
	 * The method asserts that one match is found, and no more.
	 */
	private String projectSingleField(IndexReader reader, String fieldName, Term term) throws IOException {
		String projection = null;
		for ( AtomicReaderContext leaf : reader.leaves() ) {
			final AtomicReader atomicReader = leaf.reader();
			final DocsEnum termDocsEnum = atomicReader.termDocsEnum( term );
			while ( termDocsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS ) {
				final int docID = termDocsEnum.docID();
				org.apache.lucene.document.Document document = reader.document( docID );
				String value = document.get( fieldName );
				Assert.assertNull( "duplicate matches found! This method assumes a single document will match the Term.", projection );
				projection = value;
			}
		}
		Assert.assertNotNull( projection );
		return projection;
	}

	public void testBoost() throws Exception {
		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist(
				new Document( "Hibernate in Action", "Object and Relational", "blah blah blah" )
		);
		s.persist(
				new Document( "Object and Relational", "Hibernate in Action", "blah blah blah" )
		);
		s.getTransaction().commit();
		s.close();

		FSDirectory dir = FSDirectory.open( new File( getBaseIndexDir(), "Documents" ) );
		IndexReader indexReader = DirectoryReader.open( dir );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		try {
			QueryParser qp = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
			Query query = qp.parse( "title:Action OR Abstract:Action" );
			TopDocs hits = searcher.search( query, 1000 );
			assertEquals( 2, hits.totalHits );
			assertTrue( hits.scoreDocs[0].score == 2 * hits.scoreDocs[1].score );
			org.apache.lucene.document.Document doc = searcher.doc( 0 );
			assertEquals( "Hibernate in Action", doc.get( "title" ) );
		}
		finally {
			indexReader.close();
			dir.close();
		}

		s = getSessionFactory().openSession();
		s.getTransaction().begin();
		List list = s.createQuery( "from Document" ).list();
		for ( Document document : (List<Document>) list ) {
			s.delete( document );
		}
		s.getTransaction().commit();
		s.close();
		getSessionFactory().close(); //run the searchfactory.close() operations
	}

	public void testSearchOnDeletedIndex() throws Exception {
		Session s = getSessionFactory().openSession();
		s.getTransaction().begin();
		s.persist( new Document( "Hibernate Search in Action", "", "" ) );
		s.getTransaction().commit();
		s.close();

		Directory dir = FSDirectory.open( new File( getBaseIndexDir(), "Documents" ) );
		IndexReader indexReader = DirectoryReader.open( dir );
		IndexSearcher searcher = new IndexSearcher( indexReader );
		// deleting before search, but after IndexSearcher creation:
		// ( fails when deleting -concurrently- to IndexSearcher initialization! )
		FileHelper.delete( getBaseIndexDir() );
		TermQuery query = new TermQuery( new Term( "title", "action" ) );
		TopDocs hits = searcher.search( query, 1000 );
		assertEquals( 1, hits.totalHits );
		org.apache.lucene.document.Document doc = searcher.doc( 0 );
		assertEquals( "Hibernate Search in Action", doc.get( "title" ) );
		indexReader.close();
		dir.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Document.class
		};
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		File baseIndexDir = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", baseIndexDir.getAbsolutePath() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

}
