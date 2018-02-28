/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

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
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.integrationtest.util.common.rule.BackendMock;
import org.hibernate.search.integrationtest.util.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.integrationtest.util.orm.OrmUtils;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
public class OrmAnnotationMappingAccessTypeIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "stubBackend" );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( ParentIndexedEntity.class )
				.addAnnotatedClass( IndexedEntityWithoutIdSetter.class )
				.addAnnotatedClass( EmbeddableWithDefaultFieldAccess.class )
				.addAnnotatedClass( EmbeddableWithDefaultMethodAccess.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
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

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
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

	@MappedSuperclass
	@Access( AccessType.FIELD )
	public static class ParentIndexedEntity {

		@Basic
		protected String fieldWithDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

		@Access( AccessType.PROPERTY )
		@Basic
		@Field
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
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String INDEX = "IndexedEntity";

		@DocumentId
		private Integer id;

		@Access( AccessType.FIELD )
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

		@Field
		public String getFieldWithDefaultMethodAccess() {
			return internalFieldWithDifferentName;
		}

		public void setFieldWithDefaultMethodAccess(String value) {
			this.internalFieldWithDifferentName = value;
		}

		@IndexedEmbedded
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
	@Indexed(index = IndexedEntityWithoutIdSetter.INDEX)
	public static class IndexedEntityWithoutIdSetter {

		public static final String INDEX = "IndexedEntityWithoutIdSetter";

		@Id
		@DocumentId
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

		@Access( AccessType.PROPERTY )
		@Basic
		@Field
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

		@Basic
		@Field
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

		@Field
		public String getField() {
			return internalFieldWithDifferentName;
		}

		public void setField(String field) {
			this.internalFieldWithDifferentName = field;
		}

	}
}
