/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests behavior when an entity uses {@link jakarta.persistence.IdClass},
 */
public class IdClassIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	// This used to fail with an NPE at bootstrap
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3874")
	public void idClass_indexed() {
		assertThatThrownBy( () -> ormSetupHelper.start().setup( IdClassIndexed.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to define a document identifier for indexed type '"
								+ IdClassIndexed.class.getName() + "'",
						"The property representing the entity identifier is unknown",
						"Define the document identifier explicitly by annotating"
								+ " a property whose values are unique with @DocumentId"
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

		with( sessionFactory ).runInTransaction( session -> {
			NonIdClassIndexed entity = new NonIdClassIndexed();
			entity.setId( 1 );

			session.persist( entity );

			backendMock.expectWorks( NonIdClassIndexed.NAME )
					.add( "1", b -> {} );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4025")
	public void idClass_indexed_WithDocumentId() {
		backendMock.expectAnySchema( IdClassIndexedWithDocumentId.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				// See HHH-14241
				.withProperty( "hibernate.implicit_naming_strategy", "default" )
				.setup( IdClassIndexedWithDocumentId.class );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IdClassIndexedWithDocumentId entity = new IdClassIndexedWithDocumentId();
			entity.setId1( 10 );
			entity.setId2( 7 );
			entity.setDocId( 8 );

			session.persist( entity );

			backendMock.expectWorks( IdClassIndexedWithDocumentId.NAME )
					.add( "8", b -> {} );
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

	@Entity(name = IdClassIndexedWithDocumentId.NAME)
	@IdClass(MyIdClass.class)
	@Indexed(index = IdClassIndexedWithDocumentId.NAME)
	public static class IdClassIndexedWithDocumentId {
		static final String NAME = "idclsidxwdocid";

		@Id
		private Integer id1;

		@Id
		private Integer id2;

		@DocumentId
		private Integer docId;

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

		public Integer getDocId() {
			return docId;
		}

		public void setDocId(Integer docId) {
			this.docId = docId;
		}
	}

	public static class MyIdClass implements Serializable {

		Integer id1;

		Integer id2;

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

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MyIdClass myIdClass = (MyIdClass) o;
			return Objects.equals( id1, myIdClass.id1 )
					&& Objects.equals( id2, myIdClass.id2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id1, id2 );
		}
	}
}
