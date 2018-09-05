/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.lucene.document.Document;

import org.hibernate.Transaction;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "1045")
public class BridgeConversionErrorTest extends SearchTestBase {

	@Test
	public void testClassBridgeError() throws Exception {
		ClassBridged classBridged = new ClassBridged();
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( classBridged );
		try {
			tx.commit();
			fail();
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertTrue( cause instanceof BridgeException );
			String expectedErrorMessage = "Exception while calling bridge#set"
					+ "\n\tentity class: org.hibernate.search.test.bridge.BridgeConversionErrorTest$ClassBridged"
					+ "\n\tdocument field name: test"
					+ "\n\tfield bridge: <result of ExceptionThrowingBridge.toString()>";
			assertEquals( "Wrong error message", expectedErrorMessage, cause.getMessage() );
		}
	}

	@Test
	public void testFieldBridgeError() throws Exception {
		SimpleEntity entity = new SimpleEntity( "foo" );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( entity );
		try {
			tx.commit();
			fail();
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertTrue( cause instanceof BridgeException );
			String expectedErrorMessage = "Exception while calling bridge#set"
					+ "\n\tentity class: org.hibernate.search.test.bridge.BridgeConversionErrorTest$SimpleEntity"
					+ "\n\tentity property path: name"
					+ "\n\tdocument field name: overriddenFieldName"
					+ "\n\tfield bridge: <result of ExceptionThrowingBridge.toString()>";
			assertEquals( "Wrong error message", expectedErrorMessage, cause.getMessage() );
		}
	}

	@Test
	public void testEmbeddedBridgeError() throws Exception {
		SimpleEntity entity = new SimpleEntity( null ); // null won't throw an exception
		EmbeddedEntity embedded = new EmbeddedEntity( "foo" );
		entity.setEmbedded( embedded );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( entity );
		try {
			tx.commit();
			fail();
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertTrue( cause instanceof BridgeException );
			String expectedErrorMessage = "Exception while calling bridge#set"
					+ "\n\tentity class: org.hibernate.search.test.bridge.BridgeConversionErrorTest$SimpleEntity"
					+ "\n\tentity property path: embedded.name"
					+ "\n\tdocument field name: overriddenEmbeddedPrefix.overriddenEmbeddedFieldName"
					+ "\n\tfield bridge: <result of ExceptionThrowingBridge.toString()>";
			assertEquals( "Wrong error message", expectedErrorMessage, cause.getMessage() );
		}
	}

	@Test
	public void testEmbeddedEmbeddedBridgeError() throws Exception {
		SimpleEntity entity = new SimpleEntity( null ); // null won't throw an exception
		EmbeddedEntity embedded = new EmbeddedEntity( null ); // null won't throw an exception
		entity.setEmbedded( embedded );
		embedded.setEmbeddedEmbedded( new EmbeddedEmbeddedEntity( "foo" ) );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( entity );
		try {
			tx.commit();
			fail();
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertTrue( cause instanceof BridgeException );
			String expectedErrorMessage = "Exception while calling bridge#set"
					+ "\n\tentity class: org.hibernate.search.test.bridge.BridgeConversionErrorTest$SimpleEntity"
					+ "\n\tentity property path: embedded.embeddedEmbedded.name"
					+ "\n\tdocument field name: overriddenEmbeddedPrefix.overriddenEmbeddedEmbeddedPrefix.overriddenEmbeddedEmbeddedFieldName"
					+ "\n\tfield bridge: <result of ExceptionThrowingBridge.toString()>";
			assertEquals( "Wrong error message", expectedErrorMessage, cause.getMessage() );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ClassBridged.class,
				SimpleEntity.class,
				EmbeddedEntity.class,
				EmbeddedEmbeddedEntity.class
		};
	}

	public static class ExceptionThrowingBridge implements FieldBridge {
		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value != null ) {
				throw new RuntimeException( "boom" );
			}
		}

		@Override
		public String toString() {
			return "<result of ExceptionThrowingBridge.toString()>";
		}
	}

	@Entity
	@Indexed
	@Table(name = "CLASSBRIDGED")
	@ClassBridge(impl = ExceptionThrowingBridge.class, name = "test")
	public static class ClassBridged {
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity
	@Indexed
	@Table(name = "SIMPLEENTITY")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private long id;

		@Field(name = "overriddenFieldName")
		@org.hibernate.search.annotations.FieldBridge(impl = ExceptionThrowingBridge.class)
		private String name;

		@IndexedEmbedded(prefix = "overriddenEmbeddedPrefix.")
		@OneToOne(cascade = CascadeType.ALL)
		private EmbeddedEntity embedded;

		public SimpleEntity(String name) {
			this.name = name;
		}

		public SimpleEntity() {
		}

		public void setEmbedded(EmbeddedEntity embedded) {
			this.embedded = embedded;
		}
	}

	@Entity
	@Table(name = "EMBEDDEDENTITY")
	public static class EmbeddedEntity {
		@Id
		@GeneratedValue
		private long id;

		@Field(name = "overriddenEmbeddedFieldName")
		@org.hibernate.search.annotations.FieldBridge(impl = ExceptionThrowingBridge.class)
		private String name;

		public EmbeddedEntity(String name) {
			this.name = name;
		}

		public EmbeddedEntity() {
		}

		@IndexedEmbedded(prefix = "overriddenEmbeddedEmbeddedPrefix.")
		@OneToOne(cascade = CascadeType.ALL)
		private EmbeddedEmbeddedEntity embeddedEmbedded;

		public void setEmbeddedEmbedded(EmbeddedEmbeddedEntity embedded) {
			this.embeddedEmbedded = embedded;
		}
	}

	@Entity
	@Table(name = "EMBEDDEDTWICEENTITY")
	public static class EmbeddedEmbeddedEntity {
		@Id
		@GeneratedValue
		private long id;

		@Field(name = "overriddenEmbeddedEmbeddedFieldName")
		@org.hibernate.search.annotations.FieldBridge(impl = ExceptionThrowingBridge.class)
		private String name;

		public EmbeddedEmbeddedEntity(String name) {
			this.name = name;
		}

		public EmbeddedEmbeddedEntity() {
		}
	}
}
