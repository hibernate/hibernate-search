/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;


/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1494")
public class EmbeddedEntityNotIndexedTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new HibernateManualConfiguration();
		ConfigContext configContext = new ConfigContext(
				searchConfiguration,
				new BuildContextForTest( searchConfiguration )
		);
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testMultipleDocumentIdsCauseException() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( A.class, LuceneEmbeddedIndexManagerType.INSTANCE );
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
