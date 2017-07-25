/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2531")
public class IndexNameOverrideTest {
	public static final String INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME = "indexed_annotation_overridden_index_name";
	public static final String CONFIGURATION_OVERRIDDEN_INDEX_NAME = "configuration_overridden_index_name";

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private SearchIntegrator integrator;

	private final SearchITHelper helper = new SearchITHelper( () -> this.integrator );

	@Test
	public void noOverride() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( NoAnnotationIndexNameOverrideEntity.class );

		integrator = init( cfg );
		assertThat(
				integrator.getIndexBindings().get( NoAnnotationIndexNameOverrideEntity.class )
						.getIndexManagerSelector().all()
				)
				.onProperty( "indexName" )
				.as( "Index names for entity " + NoAnnotationIndexNameOverrideEntity.class )
				.containsOnly( NoAnnotationIndexNameOverrideEntity.class.getName() );

		NoAnnotationIndexNameOverrideEntity entity = new NoAnnotationIndexNameOverrideEntity();
		entity.id = 1L;

		assertIndexingWorksProperly( entity, entity.id );
	}

	@Test
	public void annotationOverride() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedAnnotationIndexNameOverriddeEntity.class );

		integrator = init( cfg );
		assertThat(
				integrator.getIndexBindings().get( IndexedAnnotationIndexNameOverriddeEntity.class )
						.getIndexManagerSelector().all()
				)
				.onProperty( "indexName" )
				.as( "Index names for entity " + IndexedAnnotationIndexNameOverriddeEntity.class )
				.containsOnly( INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME );

		IndexedAnnotationIndexNameOverriddeEntity entity = new IndexedAnnotationIndexNameOverriddeEntity();
		entity.id = 1L;

		assertIndexingWorksProperly( entity, entity.id );
	}

	@Test
	public void configurationOverride() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( NoAnnotationIndexNameOverrideEntity.class );
		cfg.addProperty(
				"hibernate.search." + NoAnnotationIndexNameOverrideEntity.class.getName()
						+ "." + Environment.INDEX_NAME_PROP_NAME,
				CONFIGURATION_OVERRIDDEN_INDEX_NAME
				);

		integrator = init( cfg );
		/*
		 * The configuration-based index name override is not expected to affect
		 * the declared index name.
		 */
		assertThat(
				integrator.getIndexBindings().get( NoAnnotationIndexNameOverrideEntity.class )
						.getIndexManagerSelector().all()
				)
				.onProperty( "indexName" )
				.as( "Index names for entity " + NoAnnotationIndexNameOverrideEntity.class )
				.containsOnly( NoAnnotationIndexNameOverrideEntity.class.getName() );

		NoAnnotationIndexNameOverrideEntity entity = new NoAnnotationIndexNameOverrideEntity();
		entity.id = 1L;

		assertIndexingWorksProperly( entity, entity.id );
	}

	@Test
	public void configurationAndAnnotationOverride() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedAnnotationIndexNameOverriddeEntity.class );
		cfg.addProperty(
				"hibernate.search." + IndexedAnnotationIndexNameOverriddeEntity.class.getName()
						+ "." + Environment.INDEX_NAME_PROP_NAME,
				CONFIGURATION_OVERRIDDEN_INDEX_NAME
				);

		integrator = init( cfg );
		/*
		 * The configuration-based index name override is not expected to affect
		 * the declared index name.
		 */
		assertThat(
				integrator.getIndexBindings().get( IndexedAnnotationIndexNameOverriddeEntity.class )
						.getIndexManagerSelector().all()
				)
				.onProperty( "indexName" )
				.as( "Index names for entity " + IndexedAnnotationIndexNameOverriddeEntity.class )
				.containsOnly( INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME );

		IndexedAnnotationIndexNameOverriddeEntity entity = new IndexedAnnotationIndexNameOverriddeEntity();
		entity.id = 1L;

		assertIndexingWorksProperly( entity, entity.id );
	}

	private SearchIntegrator init(SearchConfigurationForTest cfg) {
		return integratorResource.create( cfg );
	}

	/*
	 * See HSEARCH-2531 for an example of what could go wrong.
	 */
	private void assertIndexingWorksProperly(Object entity, Serializable id) {
		helper.add( entity );
		helper.assertThat()
				.from( entity.getClass() )
				.matchesExactlyIds( id );
	}

	@Indexed(index = INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME)
	public static final class IndexedAnnotationIndexNameOverriddeEntity {
		@DocumentId
		private long id;
		@Field
		private String field;
	}

	@Indexed
	public static final class NoAnnotationIndexNameOverrideEntity {
		@DocumentId
		private long id;
		@Field
		private String field;
	}

}
