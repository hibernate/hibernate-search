/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that a binding using markers to retrieve and access properties
 * will behave as expected regarding access mode.
 * <p>
 * We use the GeoPointBinding to test this, but theoretically
 * any other binding relying on property markers would do.
 *
 * @param <TIndexed> The entity class under test.
 */
@TestForIssue(jiraKey = { "HSEARCH-2759", "HSEARCH-2847" })
@SuppressWarnings("unused")
public class BindingUsingPropertyMarkerAccessIT<TIndexed> {

	private static final String INDEX_NAME = "IndexedEntity";

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( PrivateFieldAccessEntity.PRIMITIVES ),
				Arguments.of( ProtectedFieldAccessEntity.PRIMITIVES ),
				Arguments.of( PublicFieldAccessEntity.PRIMITIVES ),
				Arguments.of( PublicMethodAccessEntity.PRIMITIVES ),
				Arguments.of( ProtectedMethodAccessEntity.PRIMITIVES ),
				Arguments.of( PrivateMethodAccessEntity.PRIMITIVES )
		);
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	/**
	 * This just tests that Hibernate Search manages to extract data from the entity.
	 * This used to fail when the only way to extract latitude was a private field, for example.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void indexing(ModelPrimitives<TIndexed> modelPrimitives) {
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "location", GeoPoint.class,
						b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
		);
		sessionFactory = ormSetupHelper.start()
				.setup( modelPrimitives.getModelClass() );
		backendMock.verifyExpectationsMet();
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = modelPrimitives.create( 1, 42.0, 42.0 );

			session.persist( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field( "location", GeoPoint.of( 42.0, 42.0 ) )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	private interface ModelPrimitives<T> {
		Class<T> getModelClass();

		T create(int id, double latitude, double longitude);
	}

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	@GeoPointBinding(fieldName = "location")
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

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	@GeoPointBinding(fieldName = "location")
	public static final class ProtectedFieldAccessEntity {
		static final ModelPrimitives<ProtectedFieldAccessEntity> PRIMITIVES =
				new ModelPrimitives<ProtectedFieldAccessEntity>() {
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

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	@GeoPointBinding(fieldName = "location")
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

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	@GeoPointBinding(fieldName = "location")
	public static final class PublicMethodAccessEntity {
		static final ModelPrimitives<PublicMethodAccessEntity> PRIMITIVES = new ModelPrimitives<PublicMethodAccessEntity>() {
			@Override
			public Class<PublicMethodAccessEntity> getModelClass() {
				return PublicMethodAccessEntity.class;
			}

			@Override
			public PublicMethodAccessEntity create(int id, double latitude, double longitude) {
				PublicMethodAccessEntity entity = new PublicMethodAccessEntity();
				entity.idWithUnpredictableName = id;
				entity.latitudeWithUnpredictableName = latitude;
				entity.longitudeWithUnpredictableName = longitude;
				return entity;
			}
		};

		/*
		 * Give unpredictable names to properties, so that we're sure Hibernate Search
		 * accessed the data through methods, and not direct field access.
		 */

		private Integer idWithUnpredictableName;

		private Double latitudeWithUnpredictableName;

		private Double longitudeWithUnpredictableName;

		// Put the @Id annotation here to set the Hibernate ORM access type to "property"
		@Id
		@DocumentId
		public Integer getId() {
			return idWithUnpredictableName;
		}

		public void setId(Integer id) {
			this.idWithUnpredictableName = id;
		}

		@Latitude
		public Double getLatitude() {
			return latitudeWithUnpredictableName;
		}

		public void setLatitude(Double latitude) {
			this.latitudeWithUnpredictableName = latitude;
		}

		@Longitude
		public Double getLongitude() {
			return longitudeWithUnpredictableName;
		}

		public void setLongitude(Double longitude) {
			this.longitudeWithUnpredictableName = longitude;
		}
	}

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	@GeoPointBinding(fieldName = "location")
	public static final class ProtectedMethodAccessEntity {
		static final ModelPrimitives<ProtectedMethodAccessEntity> PRIMITIVES =
				new ModelPrimitives<ProtectedMethodAccessEntity>() {
					@Override
					public Class<ProtectedMethodAccessEntity> getModelClass() {
						return ProtectedMethodAccessEntity.class;
					}

					@Override
					public ProtectedMethodAccessEntity create(int id, double latitude, double longitude) {
						ProtectedMethodAccessEntity entity = new ProtectedMethodAccessEntity();
						entity.idWithUnpredictableName = id;
						entity.latitudeWithUnpredictableName = latitude;
						entity.longitudeWithUnpredictableName = longitude;
						return entity;
					}
				};

		/*
		 * Give unpredictable names to properties, so that we're sure Hibernate Search
		 * accessed the data through methods, and not direct field access.
		 */

		private Integer idWithUnpredictableName;

		private Double latitudeWithUnpredictableName;

		private Double longitudeWithUnpredictableName;

		// Put the @Id annotation here to set the Hibernate ORM access type to "property"
		@Id
		@DocumentId
		protected Integer getId() {
			return idWithUnpredictableName;
		}

		protected void setId(Integer id) {
			this.idWithUnpredictableName = id;
		}

		@Latitude
		protected Double getLatitude() {
			return latitudeWithUnpredictableName;
		}

		protected void setLatitude(Double latitude) {
			this.latitudeWithUnpredictableName = latitude;
		}

		@Longitude
		protected Double getLongitude() {
			return longitudeWithUnpredictableName;
		}

		protected void setLongitude(Double longitude) {
			this.longitudeWithUnpredictableName = longitude;
		}
	}

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	@GeoPointBinding(fieldName = "location")
	public static final class PrivateMethodAccessEntity {
		static final ModelPrimitives<PrivateMethodAccessEntity> PRIMITIVES = new ModelPrimitives<PrivateMethodAccessEntity>() {
			@Override
			public Class<PrivateMethodAccessEntity> getModelClass() {
				return PrivateMethodAccessEntity.class;
			}

			@Override
			public PrivateMethodAccessEntity create(int id, double latitude, double longitude) {
				PrivateMethodAccessEntity entity = new PrivateMethodAccessEntity();
				entity.idWithUnpredictableName = id;
				entity.latitudeWithUnpredictableName = latitude;
				entity.longitudeWithUnpredictableName = longitude;
				return entity;
			}
		};

		private Integer idWithUnpredictableName;

		private Double latitudeWithUnpredictableName;

		private Double longitudeWithUnpredictableName;

		// Put the @Id annotation here to set the Hibernate ORM access type to "property"
		@Id
		@DocumentId
		private Integer getId() {
			return idWithUnpredictableName;
		}

		private void setId(Integer id) {
			this.idWithUnpredictableName = id;
		}

		@Latitude
		private Double getLatitude() {
			return latitudeWithUnpredictableName;
		}

		private void setLatitude(Double latitude) {
			this.latitudeWithUnpredictableName = latitude;
		}

		@Longitude
		private Double getLongitude() {
			return longitudeWithUnpredictableName;
		}

		private void setLongitude(Double longitude) {
			this.longitudeWithUnpredictableName = longitude;
		}
	}
}
