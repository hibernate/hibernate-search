/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests behavior when an entity binds a {@code @OneToOne} association as its {@code @Id}.
 */
class IdDerivedFromAssociationIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	// This is not currently supported, so we expect a failure at bootstrap,
	// with an appropriate error message giving at least a hint of how to solve the problem.
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4352")
	void indexed_withoutDocumentId() {
		assertThatThrownBy( () -> ormSetupHelper.start().setup( NonIndexedBaseForIndexedDerived.class,
				IndexedDerived.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to define a document identifier for indexed type '"
								+ IndexedDerived.class.getName() + "'",
						"The property representing the entity identifier is unknown",
						"Define the document identifier explicitly by annotating"
								+ " a property whose values are unique with @DocumentId"
				);
	}

	// This used to fail with an exception at bootstrap,
	// even though the class with a derived ID was not indexed.
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4352")
	void nonIndexed() {
		backendMock.expectAnySchema( IndexedBaseForNonIndexedDerived.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( IndexedBaseForNonIndexedDerived.class, NonIndexedDerived.class );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedBaseForNonIndexedDerived base = new IndexedBaseForNonIndexedDerived();
			session.persist( base );

			NonIndexedDerived derived = new NonIndexedDerived( base );
			session.persist( derived );

			backendMock.expectWorks( IndexedBaseForNonIndexedDerived.NAME )
					.add( String.valueOf( base.getId() ), b -> {} );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4352")
	void indexed_withDocumentId() {
		backendMock.expectAnySchema( IndexedDerivedWithDocumentId.NAME );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( NonIndexedBaseForIndexedDerivedWithDocumentId.class, IndexedDerivedWithDocumentId.class );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			NonIndexedBaseForIndexedDerivedWithDocumentId base = new NonIndexedBaseForIndexedDerivedWithDocumentId();
			session.persist( base );

			IndexedDerivedWithDocumentId derived = new IndexedDerivedWithDocumentId( base, base.getId() );
			session.persist( derived );

			backendMock.expectWorks( IndexedDerivedWithDocumentId.NAME )
					.add( String.valueOf( base.getId() ), b -> {} );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = NonIndexedBaseForIndexedDerived.NAME)
	public static class NonIndexedBaseForIndexedDerived {
		static final String NAME = "base";

		@Id
		@GeneratedValue
		private Integer id;

		@OneToOne(fetch = FetchType.EAGER, mappedBy = "base")
		private IndexedDerived derived;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedDerived getDerived() {
			return derived;
		}

		public void setDerived(IndexedDerived derived) {
			this.derived = derived;
		}
	}

	@Indexed
	@Entity(name = IndexedDerived.NAME)
	public static class IndexedDerived implements Serializable {
		static final String NAME = "derived";

		@Id
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "base_id", nullable = false)
		private NonIndexedBaseForIndexedDerived base;

		public IndexedDerived() {
		}

		public IndexedDerived(NonIndexedBaseForIndexedDerived base) {
			this.base = base;
		}

		public NonIndexedBaseForIndexedDerived getBase() {
			return base;
		}

		public void setBase(NonIndexedBaseForIndexedDerived base) {
			this.base = base;
		}
	}

	@Indexed
	@Entity(name = IndexedBaseForNonIndexedDerived.NAME)
	public static class IndexedBaseForNonIndexedDerived {
		static final String NAME = "base";

		@Id
		@GeneratedValue
		private Integer id;

		@OneToOne(fetch = FetchType.EAGER, mappedBy = "base")
		private NonIndexedDerived derived;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public NonIndexedDerived getDerived() {
			return derived;
		}

		public void setDerived(NonIndexedDerived derived) {
			this.derived = derived;
		}
	}

	@Entity(name = NonIndexedDerived.NAME)
	public static class NonIndexedDerived implements Serializable {
		static final String NAME = "derived";

		@Id
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "other_id", nullable = false)
		private IndexedBaseForNonIndexedDerived base;

		public NonIndexedDerived() {
		}

		public NonIndexedDerived(IndexedBaseForNonIndexedDerived other) {
			this.base = other;
		}

		public IndexedBaseForNonIndexedDerived getBase() {
			return base;
		}

		public void setBase(IndexedBaseForNonIndexedDerived base) {
			this.base = base;
		}
	}

	@Entity(name = NonIndexedBaseForIndexedDerived.NAME)
	public static class NonIndexedBaseForIndexedDerivedWithDocumentId {
		static final String NAME = "base";

		@Id
		@GeneratedValue
		private Integer id;

		@OneToOne(fetch = FetchType.EAGER, mappedBy = "base")
		private IndexedDerivedWithDocumentId derived;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedDerivedWithDocumentId getDerived() {
			return derived;
		}

		public void setDerived(IndexedDerivedWithDocumentId derived) {
			this.derived = derived;
		}
	}

	@Indexed
	@Entity(name = IndexedDerived.NAME)
	public static class IndexedDerivedWithDocumentId implements Serializable {
		static final String NAME = "derived";

		@Id
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "base_id", nullable = false)
		private NonIndexedBaseForIndexedDerivedWithDocumentId base;

		@DocumentId
		private Integer docId;

		public IndexedDerivedWithDocumentId() {
		}

		public IndexedDerivedWithDocumentId(NonIndexedBaseForIndexedDerivedWithDocumentId base, Integer docId) {
			this.base = base;
			this.docId = docId;
		}

		public NonIndexedBaseForIndexedDerivedWithDocumentId getBase() {
			return base;
		}

		public void setBase(NonIndexedBaseForIndexedDerivedWithDocumentId base) {
			this.base = base;
		}

		public Integer getDocId() {
			return docId;
		}

		public void setDocId(Integer docId) {
			this.docId = docId;
		}
	}

}
