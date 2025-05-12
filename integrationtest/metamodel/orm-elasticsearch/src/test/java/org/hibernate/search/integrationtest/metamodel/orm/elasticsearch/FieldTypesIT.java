/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.metamodel.orm.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FieldTypesIT {

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( new ElasticsearchBackendConfiguration() );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		assumeTrue( isVectorSearchSupported() );
		sessionFactory = setupHelper.start()
				.withAnnotatedTypes( FieldTypesEntity.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.SYNC )
				.setup();
	}

	@Test
	void smoke() {
		with( sessionFactory ).runInTransaction( session -> {
			session.persist( new FieldTypesEntity( 1L ) );
			session.persist( new FieldTypesEntity( 2L ) );
		} );

		try ( var s = sessionFactory.openSession() ) {
			SearchSession session = Search.session( s );
			SearchScope<FieldTypesIT_FieldTypesEntity__, FieldTypesIT.FieldTypesEntity> scope =
					FieldTypesIT_FieldTypesEntity__.INDEX.scope( session );
			assertThat( session.search( scope )
					.where( f -> f.bool()
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aText ).matching( "text" ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.keywordString )
									.matching( "keywordString" ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.bigDecimal )
									.matching( BigDecimal.TEN ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.bigInteger )
									.matching( BigInteger.ONE ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aBool ).matching( false ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aBoolean ).matching( true ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aByte ).matching( (byte) 1 ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aDouble ).matching( 1.0 ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aFloat ).matching( 1.0f ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aInt ).matching( 1 ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aLong ).matching( 10L ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aShort ).matching( (short) 2 ) )
							.should( f.spatial().within().field( FieldTypesIT_FieldTypesEntity__.INDEX.geoPoint )
									.circle( GeoPoint.of( 10.0, 20.0 ), 20.0 ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.anInstant )
									.matching( Instant.ofEpochMilli( 1000L ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aLocalDate )
									.matching( LocalDate.of( 2000, 1, 1 ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aLocalDateTime )
									.matching( LocalDateTime.of( 2000, 1, 1, 1, 1 ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aLocalTime )
									.matching( LocalTime.of( 1, 1 ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aMonthDay )
									.matching( MonthDay.of( 1, 1 ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.anOffsetDateTime )
									.matching( OffsetDateTime.of( LocalDateTime.of( 2000, 1, 1, 1, 1 ), ZoneOffset.UTC ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.anOffsetTime )
									.matching( OffsetTime.of( LocalTime.of( 1, 1 ), ZoneOffset.UTC ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aYear )
									.matching( Year.of( 2000 ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aYearMonth )
									.matching( YearMonth.of( 2000, 1 ) ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.aZonedDateTime )
									.matching( ZonedDateTime.of( LocalDateTime.of( 2000, 1, 1, 1, 1 ), ZoneOffset.UTC ) ) )
							.should( f.knn( 10 ).field( FieldTypesIT_FieldTypesEntity__.INDEX.floatVector )
									.matching( new float[] { 1.0f, 2.0f, 3.0f } ) )
							.should( f.knn( 10 ).field( FieldTypesIT_FieldTypesEntity__.INDEX.byteVector )
									.matching( new byte[] { 1, 2, 3 } ) )
							.should( f.match().field( FieldTypesIT_FieldTypesEntity__.INDEX.myEnum ).matching( MyEnum.B ) )
					)
					.fetchHits( 20 ) )
					.hasSize( 2 );

			if ( isVectorSearchSupported() ) {
				assertThat( session.search( scope )
						.where( f -> f.bool()
								.should( f.knn( 10 ).field( FieldTypesIT_FieldTypesEntity__.INDEX.floatVector )
										.matching( new float[] { 1.0f, 2.0f, 3.0f } ) )
								.should( f.knn( 10 ).field( FieldTypesIT_FieldTypesEntity__.INDEX.byteVector )
										.matching( new byte[] { 1, 2, 3 } ) )
						)
						.fetchHits( 20 ) )
						.hasSize( 2 );
			}
		}
	}

	private static boolean isVectorSearchSupported() {
		return ElasticsearchTestDialect.isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> !os.isLessThan( "2.9.0" ),
				aoss -> true
		);
	}

	@Entity(name = "FieldTypesEntity")
	@Indexed
	public static class FieldTypesEntity {
		@Id
		public Long id;
		@FullTextField
		public String aText;
		@KeywordField
		public String keywordString;
		@ScaledNumberField
		public BigDecimal bigDecimal;
		@ScaledNumberField
		public BigInteger bigInteger;
		@GenericField
		public boolean aBool;
		@GenericField
		public Boolean aBoolean;
		@GenericField
		public byte aByte;
		@GenericField
		public double aDouble;
		@GenericField
		public float aFloat;
		@GenericField
		public int aInt;
		@GenericField
		public long aLong;
		@GenericField
		public short aShort;
		@Transient
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.NO)
		@GenericField
		public GeoPoint geoPoint;
		@GenericField
		public Instant anInstant;
		@GenericField
		public LocalDate aLocalDate;
		@GenericField
		public LocalDateTime aLocalDateTime;
		@GenericField
		public LocalTime aLocalTime;
		@GenericField
		public MonthDay aMonthDay;
		@GenericField
		public OffsetDateTime anOffsetDateTime;
		@GenericField
		public OffsetTime anOffsetTime;
		@GenericField
		public Year aYear;
		@GenericField
		public YearMonth aYearMonth;
		@GenericField
		public ZonedDateTime aZonedDateTime;

		@VectorField(dimension = 3)
		public float[] floatVector;
		@VectorField(dimension = 3)
		public byte[] byteVector;

		@KeywordField
		public MyEnum myEnum;

		public FieldTypesEntity() {
		}

		public FieldTypesEntity(Long id) {
			this.id = id;
			this.aText = "text";
			this.keywordString = "keywordString";
			this.bigDecimal = BigDecimal.TEN;
			this.bigInteger = BigInteger.ONE;
			this.aBool = false;
			this.aBoolean = true;
			this.aByte = 1;
			this.aDouble = 1.0;
			this.aFloat = 1.0f;
			this.aInt = 1;
			this.aLong = 10L;
			this.aShort = 2;
			this.geoPoint = GeoPoint.of( 10.0, 20.0 );
			this.anInstant = Instant.ofEpochMilli( 1000L );
			this.aLocalDate = LocalDate.of( 2000, 1, 1 );
			this.aLocalDateTime = LocalDateTime.of( 2000, 1, 1, 1, 1 );
			this.aLocalTime = LocalTime.of( 1, 1 );
			this.aMonthDay = MonthDay.of( 1, 1 );
			this.anOffsetDateTime = OffsetDateTime.of( LocalDateTime.of( 2000, 1, 1, 1, 1 ), ZoneOffset.UTC );
			this.anOffsetTime = OffsetTime.of( LocalTime.of( 1, 1 ), ZoneOffset.UTC );
			this.aYear = Year.of( 2000 );
			this.aYearMonth = YearMonth.of( 2000, 1 );
			this.aZonedDateTime = ZonedDateTime.of( LocalDateTime.of( 2000, 1, 1, 1, 1 ), ZoneOffset.UTC );
			this.floatVector = new float[] { 1.0f, 2.0f, 3.0f };
			this.byteVector = new byte[] { 1, 2, 3 };
			this.myEnum = MyEnum.B;
		}
	}

	public enum MyEnum {
		A, B, C, D;
	}

	@Entity
	public static class ContainedEntity {
		// Not setting @DocumentId here because it shouldn't be necessary
		@Id
		public String id;
		@FullTextField
		public String text;
		@ManyToOne
		public FieldTypesEntity containing;

		public ContainedEntity() {
		}

		public ContainedEntity(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
