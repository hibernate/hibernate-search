/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.configuration;

import java.util.Set;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.SortableFieldMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class TypeMetadataTest {

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
	public void testMultipleDocumentIdsCauseException() {
		try {
			metadataProvider.getTypeMetadataFor( Foo.class, LuceneEmbeddedIndexManagerType.INSTANCE );
			fail( "An exception should have been thrown" );
		}
		catch (SearchException e) { // getting a HibernateException here, because the listener registration fails
			assertEquals(
					"HSEARCH000167: More than one @DocumentId specified on entity 'org.hibernate.search.test.configuration.TypeMetadataTest$Foo'",
					e.getMessage()
			);
		}
	}

	@Test
	public void testRetrievalOfSortableFieldMetadata() {
		TypeMetadata metadata = metadataProvider.getTypeMetadataFor( Bar.class, LuceneEmbeddedIndexManagerType.INSTANCE );

		Set<SortableFieldMetadata> fieldMetadata = metadata.getPropertyMetadataForProperty( "name" ).getSortableFieldMetadata();
		assertThat( fieldMetadata ).onProperty( "absoluteName" ).containsOnly( "name" );

		fieldMetadata = metadata.getPropertyMetadataForProperty( "age" ).getSortableFieldMetadata();
		assertThat( fieldMetadata ).onProperty( "absoluteName" ).containsOnly( "ageForStringSorting", "ageForIntSorting" );
	}

	@Indexed
	public class Foo {
		@DocumentId
		private Integer id;

		@DocumentId
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Indexed
	public class Bar {
		@DocumentId
		private Integer id;

		@Field(analyze = Analyze.NO)
		@SortableField
		private String name;

		@SortableField(forField = "ageForStringSorting")
		@SortableField(forField = "ageForIntSorting")
		@Fields({
			@Field(name = "ageForStringSorting", analyze = Analyze.NO, bridge = @FieldBridge(impl = IntegerBridge.class) ),
			@Field(name = "ageForIntSorting", analyze = Analyze.NO),
		})
		private long age;
	}
}
