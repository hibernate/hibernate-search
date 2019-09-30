/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1108")
public class MappedSuperclassIT {

	private static final String INDEX_NAME = "IndexedEntity";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "text", String.class ) );

		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, (HibernateOrmSearchMappingConfigurer) context -> {
					ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
					mapping.type( IndexedEntity.class ).indexed( INDEX_NAME )
							.property( "id" ).documentId()
							.property( "text" ).fullTextField();
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.save( indexedPojo );

			backendMock.expectWorks( IndexedEntity.class.getSimpleName() ).add( "1", b -> b
					.field( "text", "Using some text here" )
			).processedThenExecuted();
		} );
	}

	@MappedSuperclass
	public abstract static class TextEntity {

		protected String text;

		public String getText() {
			return text;
		}
	}

	@Entity
	public static class IndexedEntity extends TextEntity {

		@Id
		private Integer id;

		private IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}
	}
}
