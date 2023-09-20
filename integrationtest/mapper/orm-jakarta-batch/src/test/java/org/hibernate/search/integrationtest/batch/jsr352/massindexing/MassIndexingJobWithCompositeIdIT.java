/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil.JOB_TIMEOUT_MS;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.util.BackendConfigurations;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Tests that mass indexing job can handle entity having
 * {@link jakarta.persistence.EmbeddedId} annotation, or
 * {@link jakarta.persistence.IdClass} annotation.
 *
 * @author Mincong Huang
 */
@TestForIssue(jiraKey = "HSEARCH-2615")
public class MassIndexingJobWithCompositeIdIT {

	private static final LocalDate START = LocalDate.of( 2017, 6, 1 );

	private static final LocalDate END = LocalDate.of( 2017, 8, 1 );

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private EntityManagerFactory emf;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( EntityWithIdClass.class, EntityWithEmbeddedId.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false );
	}

	@Before
	public void initData() throws Exception {
		emf = setupHolder.entityManagerFactory();

		with( emf ).runInTransaction( entityManager -> {
			for ( LocalDate d = START; d.isBefore( END ); d = d.plusDays( 1 ) ) {
				entityManager.persist( new EntityWithIdClass( d ) );
				entityManager.persist( new EntityWithEmbeddedId( d ) );
			}
		} );

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class ) ).isEqualTo( 0 );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class ) ).isEqualTo( 0 );
	}

	@Test
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
	public void canHandleIdClass_strategyFull() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithIdClass.class )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( START, END );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class ) ).isEqualTo( expectedDays );
	}

	@Test
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
	public void canHandleIdClass_strategyHql() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithIdClass.class )
				.restrictedBy( "select e from EntityWithIdClass e where e.month = 6" )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	@Test
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
	public void canHandleEmbeddedId_strategyFull() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithEmbeddedId.class )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();

		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( START, END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	@Test
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
	public void canHandleEmbeddedId_strategyHql() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithEmbeddedId.class )
				.restrictedBy( "select e from EntityWithIdClass e where e.embeddableDateId.month = 6" )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	/**
	 * @author Mincong Huang
	 */
	@Entity(name = "EntityWithEmbeddedId")
	@Indexed
	public static class EntityWithEmbeddedId {

		@EmbeddedId
		@DocumentId(identifierBridge = @IdentifierBridgeRef(type = DateIdBridge.class))
		private EmbeddableDateId embeddableDateId;

		@FullTextField
		private String value;

		public EntityWithEmbeddedId() {
		}

		public EntityWithEmbeddedId(LocalDate d) {
			this.embeddableDateId = new EmbeddableDateId( d );
			this.value = DateTimeFormatter.ofPattern( "yyyyMMdd", Locale.ROOT ).format( d );
		}

		public EmbeddableDateId getEmbeddableDateId() {
			return embeddableDateId;
		}

		public void setEmbeddableDateId(EmbeddableDateId embeddableDateId) {
			this.embeddableDateId = embeddableDateId;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "MyDate [embeddableDateId=" + embeddableDateId + ", value=" + value + "]";
		}

		/**
		 * Primary key for {@link EntityWithEmbeddedId}.
		 *
		 * @author Mincong Huang
		 */
		@Embeddable
		public static class EmbeddableDateId implements Serializable {

			private static final long serialVersionUID = 1L;

			private int year;

			private int month;

			private int day;

			public EmbeddableDateId() {

			}

			public EmbeddableDateId(LocalDate d) {
				this.year = d.getYear();
				this.month = d.getMonthValue();
				this.day = d.getDayOfMonth();
			}

			public int getYear() {
				return year;
			}

			public void setYear(int year) {
				this.year = year;
			}

			public int getMonth() {
				return month;
			}

			public void setMonth(int month) {
				this.month = month;
			}

			public int getDay() {
				return day;
			}

			public void setDay(int day) {
				this.day = day;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + day;
				result = prime * result + month;
				result = prime * result + year;
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if ( this == obj ) {
					return true;
				}
				if ( obj == null ) {
					return false;
				}
				if ( getClass() != obj.getClass() ) {
					return false;
				}
				EmbeddableDateId that = (EmbeddableDateId) obj;
				if ( day != that.day ) {
					return false;
				}
				if ( month != that.month ) {
					return false;
				}
				if ( year != that.year ) {
					return false;
				}
				return true;
			}

			@Override
			public String toString() {
				return String.format( Locale.ROOT, "%04d-%02d-%02d", year, month, day );
			}

		}
	}

	/**
	 * Entity containing multiple {@link Id} attributes.
	 *
	 * @author Mincong Huang
	 */
	@Entity(name = "EntityWithIdClass")
	@Indexed
	@IdClass(EntityWithIdClass.DatePK.class)
	public static class EntityWithIdClass implements Serializable {

		private static final long serialVersionUID = 1L;

		private int year;

		private int month;

		private int day;

		private int documentId;

		public EntityWithIdClass() {
		}

		public EntityWithIdClass(LocalDate d) {
			year = d.getYear();
			month = d.getMonthValue();
			day = d.getDayOfMonth();
			documentId = year * 1_00_00 + month * 1_00 + day;
		}

		public void setYear(int year) {
			this.year = year;
		}

		@Id
		public int getYear() {
			return year;
		}

		public void setMonth(int month) {
			this.month = month;
		}

		@Id
		public int getMonth() {
			return month;
		}

		public void setDay(int day) {
			this.day = day;
		}

		@Id
		public int getDay() {
			return day;
		}

		@DocumentId
		public int getDocumentId() {
			return documentId;
		}

		public void setDocumentId(int documentId) {
			this.documentId = documentId;
		}

		/**
		 * Primary key for {@link EntityWithIdClass}.
		 *
		 * @author Mincong Huang
		 */
		public static class DatePK implements Serializable {

			private static final long serialVersionUID = 1L;

			private int year;

			private int month;

			private int day;

			public DatePK() {

			}

			public void setYear(int year) {
				this.year = year;
			}

			public int getYear() {
				return year;
			}

			public void setMonth(int month) {
				this.month = month;
			}

			public int getMonth() {
				return month;
			}

			public void setDay(int day) {
				this.day = day;
			}

			public int getDay() {
				return day;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + day;
				result = prime * result + month;
				result = prime * result + year;
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if ( this == obj ) {
					return true;
				}
				if ( obj == null ) {
					return false;
				}
				if ( getClass() != obj.getClass() ) {
					return false;
				}
				DatePK that = (DatePK) obj;
				if ( day != that.day ) {
					return false;
				}
				if ( month != that.month ) {
					return false;
				}
				if ( year != that.year ) {
					return false;
				}
				return true;
			}

			@Override
			public String toString() {
				return String.format( Locale.ROOT, "%04d-%02d-%02d", year, month, day );
			}

		}
	}

	public static class DateIdBridge implements IdentifierBridge<EntityWithEmbeddedId.EmbeddableDateId> {

		private static final Pattern DATE_PATTERN = Pattern.compile( "^\\d{4}-\\d{2}-\\d{2}$" );

		@Override
		public String toDocumentIdentifier(EntityWithEmbeddedId.EmbeddableDateId propertyValue,
				IdentifierBridgeToDocumentIdentifierContext context) {

			return String.format( Locale.ROOT, "%04d-%02d-%02d",
					propertyValue.getYear(), propertyValue.getMonth(), propertyValue.getDay()
			);
		}

		@Override
		public EntityWithEmbeddedId.EmbeddableDateId fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {

			Matcher matcher = DATE_PATTERN.matcher( documentIdentifier );
			if ( !matcher.find() ) {
				throw new RuntimeException( "Date does not match the pattern d{4}-d{2}-d{2}: " + documentIdentifier );
			}

			EntityWithEmbeddedId.EmbeddableDateId result = new EntityWithEmbeddedId.EmbeddableDateId();
			result.setYear( Integer.parseInt( matcher.group( 1 ) ) );
			result.setMonth( Integer.parseInt( matcher.group( 2 ) ) );
			result.setDay( Integer.parseInt( matcher.group( 3 ) ) );
			return result;
		}
	}
}
