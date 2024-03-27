/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.Serializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration tests checking that we get the expected access type for properties when using annotation mapping.
 * <p>
 * Similar to {@link ProgrammaticMappingAccessTypeIT}, which tests programmatic mapping.
 * <p>
 * Note that more thorough testing is performed in {@code HibernateOrmBootstrapIntrospectorAccessTypeTest},
 * including tests of access type on an embeddable that is only ever mentioned in an element collection.
 */
class AnnotationMappingAccessTypeIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "fieldWithNonDefaultFieldAccess", String.class )
				.field( "fieldWithDefaultFieldAccess", String.class )
				.field( "fieldWithNonDefaultMethodAccess", String.class )
				.field( "fieldWithDefaultMethodAccess", String.class )
				.objectField( "embeddedWithDefaultFieldAccess", b2 -> b2
						.field( "fieldWithDefaultFieldAccess", String.class )
						.field( "fieldWithNonDefaultMethodAccess", String.class )
				)
				.objectField( "embeddedWithDefaultMethodAccess", b2 -> b2
						.field( "fieldWithNonDefaultFieldAccess", String.class )
						.field( "fieldWithDefaultMethodAccess", String.class )
				)
				.objectField( "nonManaged", b2 -> b2
						.field( "field", String.class )
				)
		);
		backendMock.expectSchema( IndexedEntityWithoutIdSetter.INDEX, b -> {} );

		sessionFactory = ormSetupHelper.start()
				.setup(
						IndexedEntity.class,
						ParentIndexedEntity.class,
						IndexedEntityWithoutIdSetter.class,
						EmbeddableWithDefaultFieldAccess.class,
						EmbeddableWithDefaultMethodAccess.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			entity1.fieldWithNonDefaultFieldAccess = "nonDefaultFieldAccess";
			entity1.fieldWithDefaultFieldAccess = "defaultFieldAccess";
			entity1.setFieldWithDefaultMethodAccess( "defaultMethodAccess" );
			entity1.setFieldWithNonDefaultMethodAccess( "nonDefaultMethodAccess" );

			EmbeddableWithDefaultFieldAccess embeddableWithDefaultFieldAccess = new EmbeddableWithDefaultFieldAccess();
			entity1.setEmbeddedWithDefaultFieldAccess( embeddableWithDefaultFieldAccess );
			embeddableWithDefaultFieldAccess.fieldWithDefaultFieldAccess = "defaultFieldAccess";
			embeddableWithDefaultFieldAccess.setFieldWithNonDefaultMethodAccess( "nonDefaultMethodAccess" );

			EmbeddableWithDefaultMethodAccess embeddableWithDefaultMethodAccess = new EmbeddableWithDefaultMethodAccess();
			entity1.setEmbeddedWithDefaultMethodAccess( embeddableWithDefaultMethodAccess );
			embeddableWithDefaultMethodAccess.fieldWithNonDefaultFieldAccess = "nonDefaultFieldAccess";
			embeddableWithDefaultMethodAccess.setFieldWithDefaultMethodAccess( "defaultMethodAccess" );

			NonManaged nonManaged = new NonManaged();
			entity1.setNonManaged( nonManaged );
			nonManaged.setField( "value" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "fieldWithNonDefaultFieldAccess", entity1.fieldWithNonDefaultFieldAccess )
							.field( "fieldWithDefaultFieldAccess", entity1.fieldWithDefaultFieldAccess )
							.field( "fieldWithNonDefaultMethodAccess", entity1.getFieldWithNonDefaultMethodAccess() )
							.field( "fieldWithDefaultMethodAccess", entity1.getFieldWithDefaultMethodAccess() )
							.objectField( "embeddedWithDefaultFieldAccess", b2 -> b2
									.field( "fieldWithDefaultFieldAccess",
											embeddableWithDefaultFieldAccess.fieldWithDefaultFieldAccess )
									.field( "fieldWithNonDefaultMethodAccess",
											embeddableWithDefaultFieldAccess.getFieldWithNonDefaultMethodAccess() )
							)
							.objectField( "embeddedWithDefaultMethodAccess", b2 -> b2
									.field( "fieldWithNonDefaultFieldAccess",
											embeddableWithDefaultMethodAccess.fieldWithNonDefaultFieldAccess )
									.field( "fieldWithDefaultMethodAccess",
											embeddableWithDefaultMethodAccess.getFieldWithDefaultMethodAccess() )
							)
							.objectField( "nonManaged", b2 -> b2
									.field( "field", nonManaged.getField() )
							)
					);
		} );
	}

	private static <T> T methodShouldNotBeCalled() {
		fail( "This method should not be called" );
		return null;
	}

	@MappedSuperclass
	@Access(AccessType.FIELD)
	public static class ParentIndexedEntity {

		@Basic
		protected String fieldWithDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		@GenericField
		public String getFieldWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access(AccessType.PROPERTY)
		@Basic
		@Column(name = "nonDefaultMethodAccess")
		@GenericField
		public String getFieldWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@Entity
	@Table(name = "indexed")
	@Access(AccessType.PROPERTY)
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		@Access(AccessType.FIELD)
		@Basic
		protected String fieldWithNonDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		@Embedded
		private EmbeddableWithDefaultFieldAccess embeddedWithDefaultFieldAccess;

		@Embedded
		@IndexedEmbedded
		private EmbeddableWithDefaultMethodAccess embeddedWithDefaultMethodAccess;

		@Basic
		@IndexedEmbedded
		private NonManaged nonManaged;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getFieldWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@GenericField
		public String getFieldWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}

		@AttributeOverrides({
				@AttributeOverride(name = "fieldWithDefaultFieldAccess", column = @Column(name = "ef_defaultFieldAccess")),
				@AttributeOverride(name = "fieldWithNonDefaultMethodAccess",
						column = @Column(name = "ef_nonDefaultMethodAccess"))
		})
		@IndexedEmbedded
		public EmbeddableWithDefaultFieldAccess getEmbeddedWithDefaultFieldAccess() {
			return embeddedWithDefaultFieldAccess;
		}

		public void setEmbeddedWithDefaultFieldAccess(EmbeddableWithDefaultFieldAccess embeddedWithDefaultFieldAccess) {
			this.embeddedWithDefaultFieldAccess = embeddedWithDefaultFieldAccess;
		}

		@AttributeOverrides({
				@AttributeOverride(name = "fieldWithDefaultMethodAccess", column = @Column(name = "em_defaultMethodAccess")),
				@AttributeOverride(name = "fieldWithNonDefaultFieldAccess", column = @Column(name = "em_nonDefaultFieldAccess"))
		})
		public EmbeddableWithDefaultMethodAccess getEmbeddedWithDefaultMethodAccess() {
			return embeddedWithDefaultMethodAccess;
		}

		public void setEmbeddedWithDefaultMethodAccess(EmbeddableWithDefaultMethodAccess embeddedWithDefaultMethodAccess) {
			this.embeddedWithDefaultMethodAccess = embeddedWithDefaultMethodAccess;
		}

		public NonManaged getNonManaged() {
			return nonManaged;
		}

		public void setNonManaged(NonManaged nonManaged) {
			this.nonManaged = nonManaged;
		}
	}

	@Entity
	@Table(name = "withoutidsetter")
	@Indexed(index = IndexedEntityWithoutIdSetter.INDEX)
	public static class IndexedEntityWithoutIdSetter {

		public static final String INDEX = "IndexedEntityWithoutIdSetter";

		@Id
		@GeneratedValue
		private Integer id;

	}

	@jakarta.persistence.Embeddable
	@Access(AccessType.FIELD)
	public static class EmbeddableWithDefaultFieldAccess {
		@Basic
		protected String fieldWithDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		@GenericField
		public String getFieldWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access(AccessType.PROPERTY)
		@Basic
		@GenericField
		public String getFieldWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@jakarta.persistence.Embeddable
	@Access(AccessType.PROPERTY)
	public static class EmbeddableWithDefaultMethodAccess {
		@Access(AccessType.FIELD)
		@Basic
		protected String fieldWithNonDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		@GenericField
		public String getFieldWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Basic
		@GenericField
		public String getFieldWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	public static final class NonManaged implements Serializable {

		private String internalFieldWithDifferentName;

		public String getInternalFieldWithDifferentName() {
			return internalFieldWithDifferentName;
		}

		public void setInternalFieldWithDifferentName(String internalFieldWithDifferentName) {
			this.internalFieldWithDifferentName = internalFieldWithDifferentName;
		}

		@GenericField
		public String getField() {
			return internalFieldWithDifferentName;
		}

		public void setField(String field) {
			this.internalFieldWithDifferentName = field;
		}

	}
}
