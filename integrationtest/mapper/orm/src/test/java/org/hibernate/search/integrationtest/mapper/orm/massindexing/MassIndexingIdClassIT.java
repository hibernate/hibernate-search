/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

@TestForIssue(jiraKey = "HSEARCH-4033")
public class MassIndexingIdClassIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectAnySchema( IdClassEntity.INDEX );

		setupContext.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.withAnnotatedTypes( IdClassEntity.class );
	}

	@Before
	public void initData() {
		setupHolder.runInTransaction( session -> {
			session.persist( new IdClassEntity( 1, 1, 1, "key-A" ) );
			session.persist( new IdClassEntity( 1, 2, 2, "key-C" ) );
			session.persist( new IdClassEntity( 2, 1, 3, "key-C" ) );
			session.persist( new IdClassEntity( 2, 2, 4, "key-A" ) );
		} );
	}

	@Test
	public void defaultMassIndexerStartAndWait() throws Exception {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					IdClassEntity.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b.field( "keyword", "key-A" ) )
					.add( "2", b -> b.field( "keyword", "key-C" ) )
					.add( "3", b -> b.field( "keyword", "key-C" ) )
					.add( "4", b -> b.field( "keyword", "key-A" ) );

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( IdClassEntity.INDEX, session.getTenantIdentifier() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IdClassEntity.INDEX)
	@IdClass(MyIdClass.class)
	@Indexed(index = IdClassEntity.INDEX)
	public static class IdClassEntity {
		static final String INDEX = "IdClassEntity";

		@Id
		private Integer id1;

		@Id
		private Integer id2;

		@DocumentId
		private Integer docId;

		@KeywordField
		private String keyword;

		public IdClassEntity() {
		}

		public IdClassEntity(Integer id1, Integer id2, Integer docId, String keyword) {
			this.id1 = id1;
			this.id2 = id2;
			this.docId = docId;
			this.keyword = keyword;
		}

		public Integer getId1() {
			return id1;
		}

		public void setId1(Integer id1) {
			this.id1 = id1;
		}

		public Integer getId2() {
			return id2;
		}

		public void setId2(Integer id2) {
			this.id2 = id2;
		}

		public Integer getDocId() {
			return docId;
		}

		public void setDocId(Integer docId) {
			this.docId = docId;
		}

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}
	}

	public static class MyIdClass implements Serializable {

		Integer id1;

		Integer id2;

		public Integer getId1() {
			return id1;
		}

		public void setId1(Integer id1) {
			this.id1 = id1;
		}

		public Integer getId2() {
			return id2;
		}

		public void setId2(Integer id2) {
			this.id2 = id2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MyIdClass myIdClass = (MyIdClass) o;
			return Objects.equals( id1, myIdClass.id1 )
					&& Objects.equals( id2, myIdClass.id2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id1, id2 );
		}
	}
}
