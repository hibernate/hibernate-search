/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import java.io.Serializable;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Integration tests checking that we get the expected access type for properties when using programmatic mapping.
 * <p>
 * Similar to {@link AnnotationMappingAccessTypeIT}, which tests annotation mapping.
 * <p>
 * Note that more thorough testing is performed in {@code HibernateOrmBootstrapIntrospectorAccessTypeTest},
 * including tests of access type on an embeddable that is only ever mentioned in an element collection.
 */
public class ProgrammaticMappingAccessTypeIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
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
		backendMock.expectSchema( IndexedEntityWithoutIdSetter.INDEX, b -> { } );

		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, new MyMappingConfigurer() )
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
	public void index() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
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
									.field( "fieldWithDefaultFieldAccess", embeddableWithDefaultFieldAccess.fieldWithDefaultFieldAccess )
									.field( "fieldWithNonDefaultMethodAccess", embeddableWithDefaultFieldAccess.getFieldWithNonDefaultMethodAccess() )
							)
							.objectField( "embeddedWithDefaultMethodAccess", b2 -> b2
									.field( "fieldWithNonDefaultFieldAccess", embeddableWithDefaultMethodAccess.fieldWithNonDefaultFieldAccess )
									.field( "fieldWithDefaultMethodAccess", embeddableWithDefaultMethodAccess.getFieldWithDefaultMethodAccess() )
							)
							.objectField( "nonManaged", b2 -> b2
									.field( "field", nonManaged.getField() )
							)
					)
					.preparedThenExecuted();
		} );
	}

	private static <T> T methodShouldNotBeCalled() {
		Assert.fail( "This method should not be called" );
		return null;
	}

	private class MyMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
		@Override
		public void configure(HibernateOrmMappingConfigurationContext context) {
			ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
			mapping.type( IndexedEntity.class )
					.indexed( IndexedEntity.INDEX )
					.property( "id" ).documentId()
					.property( "fieldWithNonDefaultFieldAccess" ).genericField()
					.property( "fieldWithDefaultFieldAccess" ).genericField()
					.property( "fieldWithNonDefaultMethodAccess" ).genericField()
					.property( "fieldWithDefaultMethodAccess" ).genericField()
					.property( "embeddedWithDefaultFieldAccess" ).indexedEmbedded()
					.property( "embeddedWithDefaultMethodAccess" ).indexedEmbedded()
					.property( "nonManaged" ).indexedEmbedded();
			mapping.type( IndexedEntityWithoutIdSetter.class )
					.indexed( IndexedEntityWithoutIdSetter.INDEX )
					.property( "id" ).documentId();
			mapping.type( EmbeddableWithDefaultFieldAccess.class )
					.property( "fieldWithDefaultFieldAccess" ).genericField()
					.property( "fieldWithNonDefaultMethodAccess" ).genericField();
			mapping.type( EmbeddableWithDefaultMethodAccess.class )
					.property( "fieldWithNonDefaultFieldAccess" ).genericField()
					.property( "fieldWithDefaultMethodAccess" ).genericField();
			mapping.type( NonManaged.class )
					.property( "field" ).genericField();
		}
	}

	@MappedSuperclass
	@Access( AccessType.FIELD )
	public static class ParentIndexedEntity {

		@Basic
		protected String fieldWithDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		public String getFieldWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access( AccessType.PROPERTY )
		@Basic
		public String getFieldWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@Entity
	@Table(name = "indexed")
	@Access( AccessType.PROPERTY )
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		@Access( AccessType.FIELD )
		@Basic
		protected String fieldWithNonDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		@Embedded
		private EmbeddableWithDefaultFieldAccess embeddedWithDefaultFieldAccess;

		@Embedded
		private EmbeddableWithDefaultMethodAccess embeddedWithDefaultMethodAccess;

		@Basic
		private NonManaged nonManaged;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFieldWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		public String getFieldWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}

		public EmbeddableWithDefaultFieldAccess getEmbeddedWithDefaultFieldAccess() {
			return embeddedWithDefaultFieldAccess;
		}

		public void setEmbeddedWithDefaultFieldAccess(EmbeddableWithDefaultFieldAccess embeddedWithDefaultFieldAccess) {
			this.embeddedWithDefaultFieldAccess = embeddedWithDefaultFieldAccess;
		}

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
	public static class IndexedEntityWithoutIdSetter {

		public static final String INDEX = "IndexedEntityWithoutIdSetter";

		@Id
		@GeneratedValue
		private Integer id;

	}

	@javax.persistence.Embeddable
	@Access( AccessType.FIELD )
	public static class EmbeddableWithDefaultFieldAccess {
		@Basic
		protected String fieldWithDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		public String getFieldWithDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithDefaultFieldAccess(String fieldWithDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Access( AccessType.PROPERTY )
		@Basic
		public String getFieldWithNonDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithNonDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}
	}

	@javax.persistence.Embeddable
	@Access( AccessType.PROPERTY )
	public static class EmbeddableWithDefaultMethodAccess {
		@Access( AccessType.FIELD )
		@Basic
		protected String fieldWithNonDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		public String getFieldWithNonDefaultFieldAccess() {
			return methodShouldNotBeCalled();
		}

		public void setFieldWithNonDefaultFieldAccess(String fieldWithNonDefaultFieldAccess) {
			methodShouldNotBeCalled();
		}

		@Basic
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

		public String getField() {
			return internalFieldWithDifferentName;
		}

		public void setField(String field) {
			this.internalFieldWithDifferentName = field;
		}

	}
}
