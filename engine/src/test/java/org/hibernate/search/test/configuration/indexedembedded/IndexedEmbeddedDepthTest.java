/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.indexedembedded;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertTrue;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Rule;
import org.junit.Test;

public class IndexedEmbeddedDepthTest {

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();
	private SearchIntegrator integrator;
	private final SearchITHelper helper = new SearchITHelper( () -> this.integrator );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1467")
	public void test() {
		SearchConfigurationForTest config = new SearchConfigurationForTest().addClasses( IndexedEntity.class );
		integrator = integratorResource.create( config );

		IndexedEntity entity = new IndexedEntity( 1, "level-1", "level-2" );
		helper.add( entity );

		helper.assertThat(
				helper.queryBuilder( IndexedEntity.class ).keyword().onField( "level1Depth2.level1Property" ).matching( "level-1" ).createQuery()
		).asResultIds().containsExactly( 1 );

		helper.assertThat(
				helper.queryBuilder( IndexedEntity.class ).keyword().onField( "level1Depth2.level2.level2Property" ).matching( "level-2" ).createQuery()
		).asResultIds().containsExactly( 1 );

		helper.assertThat(
				helper.queryBuilder( IndexedEntity.class ).keyword().onField( "level1Depth1.level1Property" ).matching( "level-1" ).createQuery()
		).asResultIds().containsExactly( 1 );

		try {
			helper.queryBuilder( IndexedEntity.class ).keyword().onField( "level1Depth1.level2.level2Property" ).matching( "level-2" ).createQuery();
			fail( "Invalid field path should throw an exception" );
		}
		catch (SearchException ex) {
			assertTrue( "Invalid exception message", ex.getMessage().contains( "Unable to find field level1Depth1.level2.level2Property" ) );
		}
	}

	public static class IndexedEmbeddedLevel2 {
		String level2Property;

		@Field
		public String getLevel2Property() {
			return level2Property;
		}
	}

	public static class IndexedEmbeddedLevel1 {
		String level1Property;

		@IndexedEmbedded
		IndexedEmbeddedLevel2 level2;

		@Field
		public String getLevel1Property() {
			return level1Property;
		}

		@IndexedEmbedded
		public IndexedEmbeddedLevel2 getLevel2() {
			return level2;
		}
	}

	@Indexed
	public static class IndexedEntity {
		Integer id;
		IndexedEmbeddedLevel1 level1Depth1;
		IndexedEmbeddedLevel1 level1Depth2;

		public IndexedEntity(int id, String level1Value, String level2Value) {
			this.id = id;
			level1Depth1 = create( level1Value, level2Value );
			level1Depth2 = create( level1Value, level2Value );
		}

		@DocumentId
		public Integer getId() {
			return id;
		}

		@IndexedEmbedded(depth = 1)
		public IndexedEmbeddedLevel1 getLevel1Depth1() {
			return level1Depth1;
		}

		@IndexedEmbedded(depth = 2)
		public IndexedEmbeddedLevel1 getLevel1Depth2() {
			return level1Depth2;
		}

		private IndexedEmbeddedLevel1 create(String level1Value, String level2Value) {
			IndexedEmbeddedLevel1 level = new IndexedEmbeddedLevel1();
			level.level1Property = level1Value;
			level.level2 = new IndexedEmbeddedLevel2();
			level.level2.level2Property = level2Value;
			return level;
		}
	}
}
