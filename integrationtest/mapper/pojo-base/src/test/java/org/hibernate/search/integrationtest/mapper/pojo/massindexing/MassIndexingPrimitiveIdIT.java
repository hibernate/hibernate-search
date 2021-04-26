/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.assertj.core.api.Fail.fail;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.massindexing.MassIndexer;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingStrategies;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3529")
public class MassIndexingPrimitiveIdIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	private Map<Integer, EntityWithPrimitiveId> entitymap = new LinkedHashMap<>();

	@Before
	public void setup() {
		backendMock.expectAnySchema( EntityWithPrimitiveId.INDEX );

		mapping = setupHelper.start()
				.setup( EntityWithPrimitiveId.class );

		backendMock.verifyExpectationsMet();

		initData();
	}

	@Test
	public void entityWithPrimitiveId() {
		try ( SearchSession searchSession = createSessionFromMap() ) {
			MassIndexer indexer = searchSession.massIndexer().mergeSegmentsOnFinish( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					EntityWithPrimitiveId.INDEX, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> {
					} )
					.add( "2", b -> {
					} )
					.add( "3", b -> {
					} )
					.createdThenExecuted();

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( EntityWithPrimitiveId.INDEX, searchSession.tenantIdentifier() )
					.purge()
					.mergeSegments()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		}

		backendMock.verifyExpectationsMet();
	}

	private SearchSession createSessionFromMap() {
		return mapping.createSessionWithOptions().loading( (o) -> {
			o.massIndexingLoadingStrategy( EntityWithPrimitiveId.class, JavaBeanIndexingStrategies.from( entitymap ) );
		} ).build();
	}

	private void initData() {
		persist( new EntityWithPrimitiveId( 1 ) );
		persist( new EntityWithPrimitiveId( 2 ) );
		persist( new EntityWithPrimitiveId( 3 ) );
	}

	private void persist(EntityWithPrimitiveId entity) {
		entitymap.put( entity.id, entity );
	}

	@Indexed(index = EntityWithPrimitiveId.INDEX)
	public static class EntityWithPrimitiveId {

		public static final String INDEX = "EntityWithPrimitiveId";

		@DocumentId
		private int id;

		public EntityWithPrimitiveId() {
		}

		public EntityWithPrimitiveId(int id) {
			this.id = id;
		}
	}
}
