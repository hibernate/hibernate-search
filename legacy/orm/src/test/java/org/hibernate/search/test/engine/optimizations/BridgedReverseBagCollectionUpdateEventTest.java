/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine.optimizations;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Test that adding elements to a persistent bag representing the reverse side of an association
 * (which shouldn't initialize the bag) will trigger reindexing if that bag is used in a field.
 *
 * @author Yoann Rodiere
 */
public class BridgedReverseBagCollectionUpdateEventTest extends SearchTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2868")
	public void collectionUpdateTriggersReindex() {
		try ( Session session = openSession() ) {
			Transaction tx = getSession().beginTransaction();

			ContainingEntity parent = new ContainingEntity();
			parent.id = 1;
			parent.nested = new ArrayList<>();

			NestedEntity nested = new NestedEntity();
			nested.id = 11;
			nested.name = "one";
			nested.parent = parent;
			parent.nested.add( nested );

			session.persist( parent );

			tx.commit();
		}

		try ( Session session = openSession() ) {
			assertNotNull( doQuery( "one" ) );
			assertNull( doQuery( "two" ) );
		}

		try ( Session session = openSession() ) {
			Transaction tx = getSession().beginTransaction();

			ContainingEntity parent = session.find( ContainingEntity.class, 1 );
			NestedEntity nested = new NestedEntity();
			nested.id = 12;
			nested.name = "two";
			nested.parent = parent;
			parent.nested.add( nested );

			tx.commit();
		}

		try ( Session session = openSession() ) {
			assertNotNull( doQuery( "two" ) );
		}
	}

	private ContainingEntity doQuery(String twiceNestedEntityName) {
		Transaction tx = getSession().beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( getSession() );
		Query termQuery = new TermQuery( new Term( "nested.name", twiceNestedEntityName ) );
		FullTextQuery fullTextQuery =
				fullTextSession.createFullTextQuery( termQuery, ContainingEntity.class );
		ContainingEntity result = (ContainingEntity) fullTextQuery.uniqueResult();

		tx.commit();

		return result;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ContainingEntity.class,
				NestedEntity.class
		};
	}

	@Entity
	@Indexed
	private static class ContainingEntity {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@Field(bridge = @FieldBridge(impl = NestedEntityCollectionFieldBridge.class) )
		private List<NestedEntity> nested;

	}

	@Entity
	private static class NestedEntity {

		@Id
		private Integer id;

		@ManyToOne
		private ContainingEntity parent;

		@Basic
		@Field
		private String name;

	}

	public static class NestedEntityCollectionFieldBridge implements MetadataProvidingFieldBridge {

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( name, FieldType.OBJECT );
			builder.field( name + ".name", FieldType.STRING );
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			@SuppressWarnings("unchecked")
			Collection<NestedEntity> collection = (Collection<NestedEntity>) value;
			for ( NestedEntity item : collection ) {
				document.add( new TextField( name + ".name", item.name, Store.NO ) );
			}
		}

	}
}
