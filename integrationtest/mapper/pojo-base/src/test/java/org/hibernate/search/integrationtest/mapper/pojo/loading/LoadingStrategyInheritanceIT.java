/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4203") // See https://github.com/hibernate/hibernate-search/pull/2564#issuecomment-833808403
public class LoadingStrategyInheritanceIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final Map<Integer, RootEntity> entityMap = new HashMap<>();

	@Before
	public void setup() {
		RootEntity root1 = new RootEntity();
		root1.id = 1;
		root1.name = "name1";
		entityMap.put( root1.id, root1 );
		DerivedEntity derived1 = new DerivedEntity();
		derived1.id = 2;
		derived1.name = "name2";
		entityMap.put( derived1.id, derived1 );
	}

	@Test
	public void addEntity_configurer_inheritance() throws InterruptedException {
		String rootEntityName = RootEntity.class.getSimpleName();
		String derivedEntityName = DerivedEntity.class.getSimpleName();

		backendMock.expectAnySchema( rootEntityName );
		backendMock.expectAnySchema( derivedEntityName );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> b
						.addEntityType( RootEntity.class, c -> {
							// Use a lambda here: it can trigger compiler problems with generics.
							c.selectionLoadingStrategy( (includedTypes, options) -> {
								return (identifiers, deadline) -> {
									return identifiers.stream().map( entityMap::get ).collect( Collectors.toList() );
								};
							} );
							// Pass generic type arguments explicitly here, even if it's not necessary:
							// it can trigger compiler problems with generics.
							c.massLoadingStrategy( MassLoadingStrategy.<RootEntity, Integer>fromMap( entityMap ) );
						} )
						.addEntityType( DerivedEntity.class )
				)
				.setup();
		backendMock.verifyExpectationsMet();

		// Smoke test loading: just check that it works even for the derived type,
		// which proves configuration is inherited.
		backendMock.expectIndexScaleWorks( rootEntityName )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();
		backendMock.expectIndexScaleWorks( derivedEntityName )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();
		backendMock.expectWorks( rootEntityName, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
				.add( "1", b -> b.field( "name", "name1" ) );
		backendMock.expectWorks( derivedEntityName, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
				.add( "2", b -> b.field( "name", "name2" ) );
		mapping.scope( Object.class ).massIndexer().startAndWait();
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchObjects( Arrays.asList( rootEntityName, derivedEntityName ), c -> { },
					StubSearchWorkBehavior.of( 2, reference( rootEntityName, "1" ),
							reference( derivedEntityName, "2" ) ) );
			assertThat( session.search( Object.class ).where( f -> f.matchAll() ).fetchAllHits() )
					.containsExactly( entityMap.get( 1 ), entityMap.get( 2 ) );
		}
	}

	// Same as the test above, but with explicit names
	@Test
	public void addEntity_name_configurer_inheritance() throws InterruptedException {
		String rootEntityName = "customRootName";
		String derivedEntityName = "customDerivedName";

		backendMock.expectAnySchema( rootEntityName );
		backendMock.expectAnySchema( derivedEntityName );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> b
						.addEntityType( RootEntity.class, rootEntityName, c -> {
							// Use a lambda here: it can trigger compiler problems with generics.
							c.selectionLoadingStrategy( (includedTypes, options) -> {
								return (identifiers, deadline) -> {
									return identifiers.stream().map( entityMap::get ).collect( Collectors.toList() );
								};
							} );
							// Pass generic type arguments explicitly here, even if it's not necessary:
							// it can trigger compiler problems with generics.
							c.massLoadingStrategy( MassLoadingStrategy.<RootEntity, Integer>fromMap( entityMap ) );
						} )
						.addEntityType( DerivedEntity.class, derivedEntityName )
				)
				.setup();
		backendMock.verifyExpectationsMet();

		// Smoke test loading: just check that it works even for the derived type,
		// which proves configuration is inherited.
		backendMock.expectIndexScaleWorks( rootEntityName )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();
		backendMock.expectIndexScaleWorks( derivedEntityName )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();
		backendMock.expectWorks( rootEntityName, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
				.add( "1", b -> b.field( "name", "name1" ) );
		backendMock.expectWorks( derivedEntityName, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
				.add( "2", b -> b.field( "name", "name2" ) );
		mapping.scope( Object.class ).massIndexer().startAndWait();
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchObjects( Arrays.asList( rootEntityName, derivedEntityName ), c -> { },
					StubSearchWorkBehavior.of( 2, reference( rootEntityName, "1" ),
							reference( derivedEntityName, "2" ) ) );
			assertThat( session.search( Object.class ).where( f -> f.matchAll() ).fetchAllHits() )
					.containsExactly( entityMap.get( 1 ), entityMap.get( 2 ) );
		}
	}

	@Indexed
	private static class RootEntity {
		@DocumentId
		public Integer id;
		@GenericField
		public String name;

		public RootEntity() {
		}
	}

	private static class DerivedEntity extends RootEntity {
		public DerivedEntity() {
		}
	}
}
