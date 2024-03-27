/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4203") // See https://github.com/hibernate/hibernate-search/pull/2564#issuecomment-833808403
class EntityLoadingBinderInheritanceIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private final Map<Integer, RootEntity> entityMap = new HashMap<>();

	@BeforeEach
	void setup() {
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
	void binder_inheritance() throws InterruptedException {
		String rootEntityName = RootEntity.class.getSimpleName();
		String derivedEntityName = DerivedEntity.class.getSimpleName();

		backendMock.expectAnySchema( rootEntityName );
		backendMock.expectAnySchema( derivedEntityName );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					b.programmaticMapping()
							.type( RootEntity.class )
							.searchEntity()
							.loadingBinder( (EntityLoadingBinder) ctx -> {
								// Use a lambda here: it can trigger compiler problems with generics.
								ctx.selectionLoadingStrategy( RootEntity.class, (includedTypes, options) -> {
									return (identifiers, deadline) -> {
										return identifiers.stream().map( entityMap::get ).collect(
												Collectors.toList() );
									};
								} );
								// Pass generic type arguments explicitly here, even if it's not necessary:
								// it can trigger compiler problems with generics.
								ctx.massLoadingStrategy( RootEntity.class,
										MassLoadingStrategy.<RootEntity, Integer>fromMap( entityMap ) );
							} );
					b.programmaticMapping()
							.type( DerivedEntity.class )
							.searchEntity();
				} )
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
			backendMock.expectSearchObjects( Arrays.asList( rootEntityName, derivedEntityName ), c -> {},
					StubSearchWorkBehavior.of( 2, reference( rootEntityName, "1" ),
							reference( derivedEntityName, "2" ) ) );
			assertThat( session.search( Object.class ).where( f -> f.matchAll() ).fetchAllHits() )
					.containsExactly( entityMap.get( 1 ), entityMap.get( 2 ) );
		}
	}

	// Same as the test above, but with explicit names
	@Test
	void inheritance_explicitName() throws InterruptedException {
		String rootEntityName = "customRootName";
		String derivedEntityName = "customDerivedName";

		backendMock.expectAnySchema( rootEntityName );
		backendMock.expectAnySchema( derivedEntityName );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( b -> {
					b.programmaticMapping()
							.type( RootEntity.class )
							.searchEntity()
							.name( rootEntityName )
							.loadingBinder( (EntityLoadingBinder) ctx -> {
								// Use a lambda here: it can trigger compiler problems with generics.
								ctx.selectionLoadingStrategy( RootEntity.class, (includedTypes, options) -> {
									return (identifiers, deadline) -> {
										return identifiers.stream().map( entityMap::get ).collect(
												Collectors.toList() );
									};
								} );
								// Pass generic type arguments explicitly here, even if it's not necessary:
								// it can trigger compiler problems with generics.
								ctx.massLoadingStrategy( RootEntity.class,
										MassLoadingStrategy.<RootEntity, Integer>fromMap( entityMap ) );
							} );
					b.programmaticMapping()
							.type( DerivedEntity.class )
							.searchEntity()
							.name( derivedEntityName );
				} )
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
			backendMock.expectSearchObjects( Arrays.asList( rootEntityName, derivedEntityName ), c -> {},
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
