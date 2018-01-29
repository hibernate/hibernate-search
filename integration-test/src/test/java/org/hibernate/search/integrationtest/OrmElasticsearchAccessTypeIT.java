/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.mapper.orm.cfg.AvailableSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingContributor;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.integrationtest.util.OrmUtils;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.json.JSONException;

import static org.hibernate.search.integrationtest.util.StubAssert.assertRequest;

/**
 * @author Yoann Rodiere
 */
public class OrmElasticsearchAccessTypeIT {

	private static final String PREFIX = "hibernate.search.";

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";
	private static final String HOST_2 = "http://es2.mycompany.com:9200/";

	private SessionFactory sessionFactory;

	@Before
	public void setup() throws JSONException {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "backend.elasticsearchBackend_1.host", HOST_1 )
				.applySetting( PREFIX + "backend.elasticsearchBackend_2.type", ElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "backend.elasticsearchBackend_2.host", HOST_2 )
				.applySetting( PREFIX + "index.default.backend", "elasticsearchBackend_1" )
				.applySetting( PREFIX + "index.OtherIndexedEntity.backend", "elasticsearchBackend_2" )
				.applySetting( AvailableSettings.MAPPING_CONTRIBUTOR, new MyMappingContributor() );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( ParentIndexedEntity.class )
				.addAnnotatedClass( EmbeddableWithDefaultFieldAccess.class )
				.addAnnotatedClass( EmbeddableWithDefaultMethodAccess.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		this.sessionFactory = sfb.build();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "createIndex", null,
				"{"
					+ "'mapping': {"
						+ "'properties': {"
							+ "'fieldWithNonDefaultFieldAccess': {"
								+ "'type': 'keyword'"
							+ "},"
							+ "'fieldWithDefaultFieldAccess': {"
							+ "'type': 'keyword'"
							+ "},"
							+ "'fieldWithNonDefaultMethodAccess': {"
								+ "'type': 'keyword'"
							+ "},"
							+ "'fieldWithDefaultMethodAccess': {"
								+ "'type': 'keyword'"
							+ "},"
							+ "'embeddedWithDefaultFieldAccess': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'fieldWithDefaultFieldAccess': {"
										+ "'type': 'keyword'"
									+ "},"
									+ "'fieldWithNonDefaultMethodAccess': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'embeddedWithDefaultMethodAccess': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'fieldWithNonDefaultFieldAccess': {"
										+ "'type': 'keyword'"
									+ "},"
									+ "'fieldWithDefaultMethodAccess': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "},"
							+ "'nonManaged': {"
								+ "'type': 'object',"
								+ "'properties': {"
									+ "'field': {"
										+ "'type': 'keyword'"
									+ "}"
								+ "}"
							+ "}"
						+ "}"
					+ "}"
				+ "}" );
	}

	@After
	public void cleanup() {
		StubElasticsearchClient.drainRequestsByIndex();
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void index() throws JSONException {
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
		} );

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();
		// We expect the first add to be removed due to the delete
		assertRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "add", "1",
				"{"
					+ "'fieldWithNonDefaultFieldAccess': 'nonDefaultFieldAccess',"
					+ "'fieldWithDefaultFieldAccess': 'defaultFieldAccess',"
					+ "'fieldWithNonDefaultMethodAccess': 'nonDefaultMethodAccess',"
					+ "'fieldWithDefaultMethodAccess': 'defaultMethodAccess',"
					+ "'embeddedWithDefaultFieldAccess': {"
						+ "'fieldWithNonDefaultMethodAccess': 'nonDefaultMethodAccess',"
						+ "'fieldWithDefaultFieldAccess': 'defaultFieldAccess'"
					+ "},"
					+ "'embeddedWithDefaultMethodAccess': {"
						+ "'fieldWithNonDefaultFieldAccess': 'nonDefaultFieldAccess',"
						+ "'fieldWithDefaultMethodAccess': 'defaultMethodAccess'"
					+ "},"
					+ "'nonManaged': {"
						+ "'field': 'value'"
					+ "}"
				+ "}" );
	}

	private class MyMappingContributor implements HibernateOrmSearchMappingContributor {
		@Override
		public void contribute(HibernateOrmMappingContributor contributor) {
			ProgrammaticMappingDefinition mapping = contributor.programmaticMapping();
			mapping.type( IndexedEntity.class )
					.indexed( IndexedEntity.INDEX )
					.property( "id" ).documentId()
					.property( "fieldWithNonDefaultFieldAccess" ).field()
					.property( "fieldWithDefaultFieldAccess" ).field()
					.property( "fieldWithNonDefaultMethodAccess" ).field()
					.property( "fieldWithDefaultMethodAccess" ).field()
					.property( "embeddedWithDefaultFieldAccess" ).indexedEmbedded()
					.property( "embeddedWithDefaultMethodAccess" ).indexedEmbedded()
					.property( "nonManaged" ).indexedEmbedded();
			mapping.type( EmbeddableWithDefaultFieldAccess.class )
					.property( "fieldWithDefaultFieldAccess" ).field()
					.property( "fieldWithNonDefaultMethodAccess" ).field();
			mapping.type( EmbeddableWithDefaultMethodAccess.class )
					.property( "fieldWithNonDefaultFieldAccess" ).field()
					.property( "fieldWithDefaultMethodAccess" ).field();
			mapping.type( NonManaged.class )
					.property( "field" ).field();
		}
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

	@javax.persistence.Embeddable
	@Access( AccessType.FIELD )
	public static class EmbeddableWithDefaultFieldAccess {
		@Basic
		protected String fieldWithDefaultFieldAccess;

		@Transient
		private String internalFieldWithDifferentName;

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

		public String getField() {
			return internalFieldWithDifferentName;
		}

		public void setField(String field) {
			this.internalFieldWithDifferentName = field;
		}

	}
}
