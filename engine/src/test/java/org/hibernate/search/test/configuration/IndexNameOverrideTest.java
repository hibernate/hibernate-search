/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2531")
public class IndexNameOverrideTest {
	public static final String INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME = "indexed_annotation_overridden_index_name";
	public static final String CONFIGURATION_OVERRIDDEN_INDEX_NAME = "configuration_overridden_index_name";

	@Test
	public void noOverride() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( NoAnnotationIndexNameOverrideEntity.class );

		try ( SearchIntegrator integrator = init( cfg ) ) {
			assertThat(
					integrator.getIndexBinding( NoAnnotationIndexNameOverrideEntity.class ).getIndexManagers()
					)
					.onProperty( "indexName" )
					.as( "Index names for entity " + NoAnnotationIndexNameOverrideEntity.class )
					.containsOnly( NoAnnotationIndexNameOverrideEntity.class.getName() );

			NoAnnotationIndexNameOverrideEntity entity = new NoAnnotationIndexNameOverrideEntity();
			entity.id = 1L;

			assertIndexingWorksProperly( integrator, entity, entity.id );
		}
	}

	@Test
	public void annotationOverride() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( IndexedAnnotationIndexNameOverriddeEntity.class );

		try ( SearchIntegrator integrator = init( cfg ) ) {
			assertThat(
					integrator.getIndexBinding( IndexedAnnotationIndexNameOverriddeEntity.class ).getIndexManagers()
					)
					.onProperty( "indexName" )
					.as( "Index names for entity " + IndexedAnnotationIndexNameOverriddeEntity.class )
					.containsOnly( INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME );

			IndexedAnnotationIndexNameOverriddeEntity entity = new IndexedAnnotationIndexNameOverriddeEntity();
			entity.id = 1L;

			assertIndexingWorksProperly( integrator, entity, entity.id );
		}
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

		try ( SearchIntegrator integrator = init( cfg ) ) {
			/*
			 * The configuration-based index name override is not expected to affect
			 * the declared index name.
			 */
			assertThat(
					integrator.getIndexBinding( NoAnnotationIndexNameOverrideEntity.class ).getIndexManagers()
					)
					.onProperty( "indexName" )
					.as( "Index names for entity " + NoAnnotationIndexNameOverrideEntity.class )
					.containsOnly( NoAnnotationIndexNameOverrideEntity.class.getName() );

			NoAnnotationIndexNameOverrideEntity entity = new NoAnnotationIndexNameOverrideEntity();
			entity.id = 1L;

			assertIndexingWorksProperly( integrator, entity, entity.id );
		}
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

		try ( SearchIntegrator integrator = init( cfg ) ) {
			/*
			 * The configuration-based index name override is not expected to affect
			 * the declared index name.
			 */
			assertThat(
					integrator.getIndexBinding( IndexedAnnotationIndexNameOverriddeEntity.class ).getIndexManagers()
					)
					.onProperty( "indexName" )
					.as( "Index names for entity " + IndexedAnnotationIndexNameOverriddeEntity.class )
					.containsOnly( INDEXED_ANNOTATION_OVERRIDDEN_INDEX_NAME );

			IndexedAnnotationIndexNameOverriddeEntity entity = new IndexedAnnotationIndexNameOverriddeEntity();
			entity.id = 1L;

			assertIndexingWorksProperly( integrator, entity, entity.id );
		}
	}

	private SearchIntegrator init(SearchConfigurationForTest cfg) {
		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	/*
	 * See HSEARCH-2531 for an example of what could go wrong.
	 */
	private void assertIndexingWorksProperly(SearchIntegrator integrator, Object entity, Serializable id) {
		storeData( integrator, entity, id );

		HSQuery query = integrator.createHSQuery( new MatchAllDocsQuery(), entity.getClass() );
		List<EntityInfo> result = query.queryEntityInfos();
		assertThat( result ).onProperty( "id" ).as( "Indexed entities IDs" )
				.containsOnly( id );
	}

	private void storeData(SearchIntegrator integrator, Object entity, Serializable id) {
		Work work = new Work( entity, id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		integrator.getWorker().performWork( work, tc );
		tc.end();
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
