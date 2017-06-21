/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.optimizations.deletebyterm;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.WorkspaceHolder;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Category(SkipOnElasticsearch.class) // This optimization is specific to the Lucene backend
public class DeleteByTermTest {

	@Test
	public void testRelatedHierarchiesWithRootNonIndexed() throws Exception {
		// Create two entities whose root entity is common but not indexed
		// delete by term should be used
		// create a unrelated Lucene Document with the same id
		// it should be deleted when the entity sharing the id is deleted
		FullTextSessionBuilder sessionBuilder = new FullTextSessionBuilder();
		sessionBuilder
				.addAnnotatedClass( ASubOfRoot.class )
				.addAnnotatedClass( BSubOfRoot.class )
				.build();
		FullTextSession fts = sessionBuilder.openFullTextSession();
		fts.beginTransaction();
		ASubOfRoot a = new ASubOfRoot();
		a.id = "1";
		a.name = "Foo";
		fts.persist( a );
		BSubOfRoot b = new BSubOfRoot();
		b.id = "2";
		b.otherName = "Bar";
		fts.persist( b );
		fts.getTransaction().commit();

		fts.clear();

		// add a document that matches the entity a identifier to see if it is removed when the entity is removed
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) fts.getSearchFactory().unwrap( SearchIntegrator.class ).getIndexManager( "index1" );
		WorkspaceHolder backendProcessor = (WorkspaceHolder) indexManager.getWorkspaceHolder();
		IndexWriter writer = backendProcessor.getIndexResources().getWorkspace().getIndexWriter();
		Document document = new Document();
		document.add( new StringField( "id", "1", org.apache.lucene.document.Field.Store.NO ) );
		document.add( new TextField( "name", "Baz", org.apache.lucene.document.Field.Store.NO ) );
		writer.addDocument( document );
		writer.commit();

		fts.getTransaction().begin();
		fts.delete( fts.get( ASubOfRoot.class, a.id ) );
		fts.delete( fts.get( BSubOfRoot.class, b.id ) );
		fts.getTransaction().commit();
		fts.close();

		// Verify that the index is empty
		IndexReader indexReader = fts.getSearchFactory().getIndexReaderAccessor().open( "index1" );
		try {
			assertThat( indexReader.numDocs() ).isEqualTo( 0 );
		}
		finally {
			indexReader.close();
		}
		sessionBuilder.close();
	}

	@Test
	public void testUnrelatedHierarchies() throws Exception {
		// Create two entities whose root entities are unrelated
		// delete by term should not be used
		// create a unrelated Lucene Document with the same id
		// it should not be deleted when the entity sharing the id is deleted
		FullTextSessionBuilder sessionBuilder = new FullTextSessionBuilder();
		sessionBuilder
				.addAnnotatedClass( ASubOfRoot.class )
				.addAnnotatedClass( Unrelated.class )
				.build();
		FullTextSession fts = sessionBuilder.openFullTextSession();
		fts.beginTransaction();
		ASubOfRoot a = new ASubOfRoot();
		a.id = "1";
		a.name = "Foo";
		fts.persist( a );
		Unrelated b = new Unrelated();
		b.id = "2";
		b.name = "Bar";
		fts.persist( b );
		fts.getTransaction().commit();

		fts.clear();

		// add a document that matches the entity a identifier to see if it is removed when the entity is removed
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) fts.getSearchFactory().unwrap( SearchIntegrator.class ).getIndexManager( "index1" );
		WorkspaceHolder backendProcessor = (WorkspaceHolder) indexManager.getWorkspaceHolder();
		IndexWriter writer = backendProcessor.getIndexResources().getWorkspace().getIndexWriter();
		Document document = new Document();
		document.add( new StringField( "id", "1", org.apache.lucene.document.Field.Store.NO ) );
		document.add( new TextField( "name", "Baz", org.apache.lucene.document.Field.Store.NO ) );
		writer.addDocument( document );
		writer.commit();

		fts.getTransaction().begin();
		fts.delete( fts.get( ASubOfRoot.class, a.id ) );
		fts.delete( fts.get( Unrelated.class, b.id ) );
		fts.getTransaction().commit();
		fts.close();

		// Verify that the index is empty
		IndexReader indexReader = fts.getSearchFactory().getIndexReaderAccessor().open( "index1" );
		try {
			assertThat( indexReader.numDocs() ).isEqualTo( 1 );
		}
		finally {
			indexReader.close();
		}
		sessionBuilder.close();
	}

	@Entity(name = "RootNonIndexed")
	public static class RootNonIndexed {
		@Id
		public String id;
	}

	@Entity(name = "ASubOfRoot")
	@Indexed(index = "index1")
	public static class ASubOfRoot extends RootNonIndexed {
		@Field
		public String name;
	}

	@Entity(name = "BSubOfRoot")
	@Indexed(index = "index1")
	public static class BSubOfRoot extends RootNonIndexed {
		@Field
		public String otherName;
	}

	@Entity(name = "Unrelated")
	@Indexed(index = "index1")
	public static class Unrelated {
		@Id
		public String id;

		@Field
		public String name;
	}
}
