/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.fest.assertions.Assertions;
import org.hibernate.Session;
import org.hibernate.search.Search;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Groups together tests to check the defaults used when a custom field is created using a
 * {@link MetadataProvidingFieldBridge}.
 * <p>
 * The entity is expected to have three fields: an id, an analyzed field and a not analyzed field.
 * Each of these fields will have a copy created using {@link AdditionalFieldBridge}.
 *
 * @author Davide D'Alto
 */
public abstract class CheckCustomFieldDefaultAnalyzer extends SearchTestBase {

	@Before
	public void before() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			session.persist( entity() );
			session.getTransaction().commit();
		}
	}

	private Object entity() {
		return entity( "GLaDOS", "CHELL", "WELL DONE. HERE COME THE TEST RESULTS: 'YOU ARE A HORRIBLE PERSON." );
	}

	protected abstract Object entity(String id, String notAnalyzedField, String analyzedField);

	protected abstract Class<?> getEntityType();

	@After
	public void after() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			session.delete( entity() );
			session.getTransaction().commit();
		}
	}

	@Test
	public void shouldBeAbleToFindTheCustomIdField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "copy_of_id", "GLaDOS" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isEqualTo( entity() );
			session.getTransaction().commit();
		}
	}

	@Test
	public void shouldNotAnalyzeCustomIdField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "copy_of_id", "glados" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isNull();
			session.getTransaction().commit();
		}
	}

	@Test
	public void shouldBeAbleToFindNotAnalyzedCustomField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "copy_of_subject", "CHELL" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isEqualTo( entity() );
			session.getTransaction().commit();
		}
	}

	@Test
	public void shouldNotAnalyzeCustomField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "copy_of_subject", "chell" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isNull();
			session.getTransaction().commit();
		}
	}

	@Test
	// The analyzer is applied for the annotated field
	public void shouldBeAbleToFindAnalyzedAnnotatedField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "result", "HORRIBLE" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isEqualTo( entity() );
			session.getTransaction().commit();
		}
	}

	@Test
	// The custom field will use the default analyzer instead of the one defined on the field
	public void shouldNotBeAbleToFindAnalyzedCustomField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "copy_of_result", "HORRIBLE" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isNull();
			session.getTransaction().commit();
		}
	}

	@Test
	// THe custom field used the default analyzer instead of the one defined on the field
	public void shouldBeAbleToFindAnalyzedCustomField() throws Exception {
		try ( Session session = openSession() ) {
			session.beginTransaction();
			TermQuery termQuery = new TermQuery( new Term( "copy_of_result", "horrible" ) );
			Object result = Search.getFullTextSession( session ).createFullTextQuery( termQuery, getEntityType() ).uniqueResult();
			Assertions.assertThat( result ).isEqualTo( entity() );
			session.getTransaction().commit();
		}
	}

	public static class AdditionalFieldBridge implements MetadataProvidingFieldBridge, TwoWayFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, (String) value, document );
			luceneOptions.addFieldToDocument( copyOf( name ), (String) value, document );
		}

		private String copyOf(String name) {
			return "copy_of_" + name;
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( copyOf( name ), FieldType.STRING );
		}

		@Override
		public Object get(String name, Document document) {
			return document.get( name );
		}

		@Override
		public String objectToString(Object object) {
			return (String) object;
		}
	}
}
