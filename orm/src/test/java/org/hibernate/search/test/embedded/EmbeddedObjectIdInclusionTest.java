/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1494")
public class EmbeddedObjectIdInclusionTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		ConfigContext configContext = new ConfigContext(
				searchConfiguration,
				new BuildContextForTest( searchConfiguration )
		);
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testIncludeEmbeddedObjectId() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( A1.class );
		assertTrue(
				"There should be only one embedded metadata instance",
				typeMetadata.getEmbeddedTypeMetadata().size() == 1
		);
		EmbeddedTypeMetadata embeddedTypeMetadata = typeMetadata.getEmbeddedTypeMetadata().get( 0 );
		PropertyMetadata propertyMetadata = embeddedTypeMetadata.getPropertyMetadataForProperty( "id" );
		assertNotNull( "The id property should have been included", propertyMetadata );
	}

	@Test
	public void testExcludeEmbeddedObjectId() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( A2.class );
		assertTrue(
				"There should be only one embedded metadata instance",
				typeMetadata.getEmbeddedTypeMetadata().size() == 1
		);
		EmbeddedTypeMetadata embeddedTypeMetadata = typeMetadata.getEmbeddedTypeMetadata().get( 0 );
		PropertyMetadata propertyMetadata = embeddedTypeMetadata.getPropertyMetadataForProperty( "id" );
		assertNull( "The id property should not have been included", propertyMetadata );
	}

	@Entity
	@Indexed
	public class A1 {
		@Id
		@GeneratedValue
		private long id;

		@OneToOne
		@IndexedEmbedded(includeEmbeddedObjectId = true)
		private B b;
	}

	@Entity
	@Indexed
	public class A2 {
		@Id
		@GeneratedValue
		private long id;

		@OneToOne
		@IndexedEmbedded(includeEmbeddedObjectId = false)
		private B b;
	}

	@Entity
	public class B {
		@Id
		@GeneratedValue
		private long id;
	}
}
