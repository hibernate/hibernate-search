/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.envers;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.Optional;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = { "HSEARCH-1293", "HSEARCH-3667" })
@PortedFromSearch5(original = "org.hibernate.search.test.envers.SearchAndEnversIntegrationTest")
public class EnversIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class )
				.objectField( "contained", b2 -> b2
						.field( "text", String.class )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.setup(
						IndexedEntity.class,
						ContainedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		// Initial insert
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexed = new IndexedEntity();
			indexed.setId( 1 );
			indexed.setText( "initial" );
			ContainedEntity contained = new ContainedEntity();
			contained.setId( 1 );
			contained.setText( "initial" );
			indexed.setContained( contained );
			contained.setContaining( indexed );

			session.persist( indexed );
			session.persist( contained );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", "initial" )
							.objectField( "contained", b2 -> b2
									.field( "text", "initial" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
		checkEnversAuditedCorrectly( IndexedEntity.class,
				1, 1, 1, 1 );
		checkEnversAuditedCorrectly( ContainedEntity.class,
				1, 1, 1, 1 );
		checkSearchLoadedEntityIsLastVersion( "1", "initial", "initial" );

		// Update the indexed entity
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexed = session.getReference( IndexedEntity.class, 1 );
			indexed.setText( "updated" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "updated" )
							.objectField( "contained", b2 -> b2
									.field( "text", "initial" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
		checkEnversAuditedCorrectly( IndexedEntity.class,
				2, 2, 1, 2 );
		checkEnversAuditedCorrectly( ContainedEntity.class,
				2, 1, 0, 1 );
		checkSearchLoadedEntityIsLastVersion( "1", "updated", "initial" );

		// Update the contained entity
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity contained = session.getReference( ContainedEntity.class, 1 );
			contained.setText( "updated" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "updated" )
							.objectField( "contained", b2 -> b2
									.field( "text", "updated" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
		checkEnversAuditedCorrectly( IndexedEntity.class,
				3, 2, 0, 2 );
		checkEnversAuditedCorrectly( ContainedEntity.class,
				3, 3, 1, 2 );
		checkSearchLoadedEntityIsLastVersion( "1", "updated", "updated" );

		// Delete the indexed entity
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexed = session.getReference( IndexedEntity.class, 1 );
			session.remove( indexed );

			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();
		checkEnversAuditedCorrectly( IndexedEntity.class,
				4, 4, 1, 3 );
		checkEnversAuditedCorrectly( ContainedEntity.class,
				4, 4, 1, 3 );
	}

	private void checkEnversAuditedCorrectly(Class<?> type,
			int lastRevisionOverall,
			int expectedLastRevisionForType,
			int expectedEntityChangeCountAtLastRevisionOverall,
			int expectedAuditedObjectCountSoFar) {
		with( sessionFactory ).runInTransaction( session -> {
			AuditReader auditReader = AuditReaderFactory.get( session );
			assertSoftly( assertions -> {
				assertions.assertThat( findLastRevisionForEntity( auditReader, type ) )
						.as( "Last revision for entity type " + type )
						.isEqualTo( expectedLastRevisionForType );
				assertions.assertThat( howManyEntitiesChangedAtRevisionNumber( auditReader, type, lastRevisionOverall ) )
						.as( "Number of entity changed at revision " + lastRevisionOverall + " for entity type " + type )
						.isEqualTo( expectedEntityChangeCountAtLastRevisionOverall );
				assertions.assertThat( howManyAuditedObjectsSoFar( auditReader, type ) )
						.as( "Number of audited objects so far for entity type " + type )
						.isEqualTo( expectedAuditedObjectCountSoFar );
			} );
		} );
	}

	private void checkSearchLoadedEntityIsLastVersion(String id,
			String expectedIndexedEntityText, String expectedContainedEntityText) {
		with( sessionFactory ).runInTransaction( session -> {
			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.limit( 2 ), // fetchSingleHit() (see below) sets the limit to 2 to check if there really is a single hit
					StubSearchWorkBehavior.of(
							1L,
							reference( IndexedEntity.NAME, id )
					)
			);
			Optional<IndexedEntity> loadedEntity = Search.session( session ).search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchSingleHit();
			assertSoftly( assertions -> {
				assertions.assertThat( loadedEntity ).get()
						.as( "getText()" )
						.extracting( IndexedEntity::getText )
						.isEqualTo( expectedIndexedEntityText );
				assertions.assertThat( loadedEntity ).get()
						.as( "getContained().getText()" )
						.extracting( e -> e.getContained().getText() )
						.isEqualTo( expectedContainedEntityText );
			} );
		} );
	}

	/**
	 * It returns how many entities are modified for a specific class and number revision.
	 */
	private int howManyEntitiesChangedAtRevisionNumber(AuditReader auditReader, Class<?> clazz, Number revision) {
		return ( (Long) auditReader.createQuery().forEntitiesModifiedAtRevision( clazz, revision )
				.addProjection( AuditEntity.id().count() ).getSingleResult() ).intValue();
	}

	/**
	 * It returns how many audited objects are there globally for a specific class.
	 */
	private int howManyAuditedObjectsSoFar(AuditReader auditReader, Class<?> clazz) {
		return auditReader.createQuery().forRevisionsOfEntity( clazz, true, true ).getResultList().size();
	}

	/**
	 * It returns the last revision for a specific class.
	 */
	private Number findLastRevisionForEntity(AuditReader auditReader, Class<?> clazz) {
		return (Number) auditReader.createQuery().forRevisionsOfEntity( clazz, false, true )
				.addProjection( AuditEntity.revisionNumber().max() ).getSingleResult();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	@Audited(withModifiedFlag = true)
	public static final class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String text;

		@OneToOne
		@IndexedEmbedded
		private ContainedEntity contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(
				ContainedEntity contained) {
			this.contained = contained;
		}
	}

	@Entity(name = "idxembedded")
	@Audited(withModifiedFlag = true)
	public static class ContainedEntity {

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String text;

		@OneToOne(mappedBy = "contained")
		private IndexedEntity containing;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public IndexedEntity getContaining() {
			return containing;
		}

		public void setContaining(IndexedEntity containing) {
			this.containing = containing;
		}
	}

}
