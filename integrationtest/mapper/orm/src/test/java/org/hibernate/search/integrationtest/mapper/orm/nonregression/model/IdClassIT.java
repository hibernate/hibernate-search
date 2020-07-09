/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.nonregression.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests behavior when an entity uses {@link javax.persistence.IdClass},
 */
@TestForIssue(jiraKey = "HSEARCH-2496")
public class IdClassIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	// This used to fail with an NPE at bootstrap
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3874")
	public void idClass_indexed() {
		Assertions.assertThatThrownBy( () -> ormSetupHelper.start().setup( IdClassIndexed.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"There isn't any explicit document ID mapping for indexed type '"
								+ IdClassIndexed.class.getName() + "'",
						"the entity ID cannot be used as a default because"
								+ " the property representing the entity ID cannot be found"
				);
	}

	// This used to fail with an NPE at bootstrap
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3874")
	public void idClass_nonIndexed() {
		backendMock.expectAnySchema( NonIdClassIndexed.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( NonIdClassIndexed.class, IdClassNonIndexed.class );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			NonIdClassIndexed entity = new NonIdClassIndexed();
			entity.setId( 1 );

			session.persist( entity );

			backendMock.expectWorks( NonIdClassIndexed.NAME )
					.add( "1", b -> { } )
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = NonIdClassIndexed.NAME)
	@Indexed(index = NonIdClassIndexed.NAME)
	public static class NonIdClassIndexed {
		static final String NAME = "noidclsidx";

		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = IdClassIndexed.NAME)
	@IdClass(MyIdClass.class)
	@Indexed(index = IdClassIndexed.NAME)
	public static class IdClassIndexed {
		static final String NAME = "idclsidx";

		@Id
		private Integer id1;

		@Id
		private Integer id2;

		public Integer getId1() {
			return id1;
		}

		public void setId1(Integer id1) {
			this.id1 = id1;
		}

		public Integer getId2() {
			return id2;
		}

		public void setId2(Integer id2) {
			this.id2 = id2;
		}
	}

	@Entity(name = IdClassNonIndexed.NAME)
	@IdClass(MyIdClass.class)
	public static class IdClassNonIndexed {
		static final String NAME = "idclsnoidx";

		@Id
		private Integer id1;

		@Id
		private Integer id2;

		public Integer getId1() {
			return id1;
		}

		public void setId1(Integer id1) {
			this.id1 = id1;
		}

		public Integer getId2() {
			return id2;
		}

		public void setId2(Integer id2) {
			this.id2 = id2;
		}
	}

	static class MyIdClass implements Serializable {

		public Integer id1;

		public Integer id2;

	}
}
