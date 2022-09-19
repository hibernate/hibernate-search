/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test warning message for enabling/disabling the dirty check.
 */
@TestForIssue(jiraKey = "HSEARCH-4866")
public class AutomaticIndexingDirtyCheckIT {

	private static final String DEPRECATED_PROPERTY_MESSAGE = "Configuration property "
			+ "'hibernate.search.automatic_indexing.enable_dirty_check' is deprecated. "
			+ "This setting will be removed in a future version. "
			+ "There will be no alternative provided to replace it. "
			+ "After the removal of this property in a future version, "
			+ "a dirty check will always be performed when considering whether to trigger reindexing.";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private SessionFactory setup(Boolean enabled) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.automatic_indexing.enable_dirty_check", enabled )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
		return sessionFactory;
	}

	@Test
	public void enabled_default() {
		logged.expectMessage( DEPRECATED_PROPERTY_MESSAGE ).never();

		setup( null );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void enabled_explicit() {
		logged.expectMessage( DEPRECATED_PROPERTY_MESSAGE ).never();

		setup( true );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void disabled() {
		logged.expectMessage( DEPRECATED_PROPERTY_MESSAGE ).once();

		setup( false );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "Indexed";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}

}
