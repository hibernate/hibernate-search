/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test that a bridge using markers to retrieve and access properties
 * will behave as expected regarding access mode.
 * <p>
 * We use the GeoPointBridge to test this, but theoretically any other bridge relying on property markers would do.
 *
 * @param <TIndexed> The entity class under test.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-2759")
public class BridgeUsingPropertyMarkerAccessIT<TIndexed> {

	private static final String INDEX_NAME = "IndexedEntity";

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		return new Object[][] {
				{ PrivateFieldAccessEntity.PRIMITIVES },
				{ ProtectedFieldAccessEntity.PRIMITIVES },
				{ PublicFieldAccessEntity.PRIMITIVES }
		};
	}

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	private ModelPrimitives<TIndexed> modelPrimitives;

	private SessionFactory sessionFactory;

	public BridgeUsingPropertyMarkerAccessIT(ModelPrimitives<TIndexed> modelPrimitives) {
		this.modelPrimitives = modelPrimitives;
	}

	@Before
	public void setup() {
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "location", GeoPoint.class, b2 -> b2.store( Store.DEFAULT ) )
		);
		sessionFactory = ormSetupHelper.withBackendMock( backendMock )
				.setup( modelPrimitives.getModelClass() );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * This just tests that Hibernate Search manages to extract data from the entity.
	 * This used to fail when the only way to extract latitude was a private field, for example.
	 */
	@Test
	public void indexing() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			TIndexed entity1 = modelPrimitives.create( 1, 42.0, 42.0 );

			session.persist( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field( "location", new ImmutableGeoPoint( 42.0, 42.0 ) )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	private interface ModelPrimitives<T> {
		Class<T> getModelClass();
		T create(int id, double latitude, double longitude);
	}

	@Entity
	@Indexed(index = INDEX_NAME)
	@GeoPointBridge(fieldName = "location")
	public static final class PublicFieldAccessEntity {
		static final ModelPrimitives<PublicFieldAccessEntity> PRIMITIVES = new ModelPrimitives<PublicFieldAccessEntity>() {
			@Override
			public Class<PublicFieldAccessEntity> getModelClass() {
				return PublicFieldAccessEntity.class;
			}

			@Override
			public PublicFieldAccessEntity create(int id, double latitude, double longitude) {
				PublicFieldAccessEntity entity = new PublicFieldAccessEntity();
				entity.id = id;
				entity.latitude = latitude;
				entity.longitude = longitude;
				return entity;
			}
		};

		@Id
		@DocumentId
		private Integer id;

		@Latitude
		public Double latitude;

		@Longitude
		public Double longitude;
	}

	@Entity
	@Indexed(index = INDEX_NAME)
	@GeoPointBridge(fieldName = "location")
	public static final class ProtectedFieldAccessEntity {
		static final ModelPrimitives<ProtectedFieldAccessEntity> PRIMITIVES = new ModelPrimitives<ProtectedFieldAccessEntity>() {
			@Override
			public Class<ProtectedFieldAccessEntity> getModelClass() {
				return ProtectedFieldAccessEntity.class;
			}

			@Override
			public ProtectedFieldAccessEntity create(int id, double latitude, double longitude) {
				ProtectedFieldAccessEntity entity = new ProtectedFieldAccessEntity();
				entity.id = id;
				entity.latitude = latitude;
				entity.longitude = longitude;
				return entity;
			}
		};

		@Id
		@DocumentId
		private Integer id;

		@Latitude
		protected Double latitude;

		@Longitude
		protected Double longitude;
	}

	@Entity
	@Indexed(index = INDEX_NAME)
	@GeoPointBridge(fieldName = "location")
	public static final class PrivateFieldAccessEntity {
		static final ModelPrimitives<PrivateFieldAccessEntity> PRIMITIVES = new ModelPrimitives<PrivateFieldAccessEntity>() {
			@Override
			public Class<PrivateFieldAccessEntity> getModelClass() {
				return PrivateFieldAccessEntity.class;
			}

			@Override
			public PrivateFieldAccessEntity create(int id, double latitude, double longitude) {
				PrivateFieldAccessEntity entity = new PrivateFieldAccessEntity();
				entity.id = id;
				entity.latitude = latitude;
				entity.longitude = longitude;
				return entity;
			}
		};

		@Id
		@DocumentId
		private Integer id;

		@Latitude
		private Double latitude;

		@Longitude
		private Double longitude;
	}
}
