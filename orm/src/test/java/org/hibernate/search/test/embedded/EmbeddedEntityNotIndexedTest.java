/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import static org.junit.Assert.assertNull;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.AbstractAnnotationMetadataTest;
import org.junit.Test;


/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1494")
public class EmbeddedEntityNotIndexedTest extends AbstractAnnotationMetadataTest {

	@Test
	public void testMultipleDocumentIdsCauseException() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( A.class );
		assertNull( "The id of B should not be indexed ", typeMetadata.getDocumentFieldMetadataFor( "b.id" ) );
	}

	@Entity
	@Indexed
	public class A {
		@Id
		@GeneratedValue
		private long id;

		@OneToOne
		@IndexedEmbedded
		private B b;
	}

	@Entity
	public class B {
		@Id
		@GeneratedValue
		private Timestamp id;

		@Field
		private String foo;

		public Timestamp getId() {
			return id;
		}

		public String getFoo() {
			return foo;
		}
	}
}
