/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test various cases of the {@code @IndexedEmbedded} leading to potential cycles, which are either broken by some
 * attribute of an {@code @IndexedEmbedded} or lead to an exception letting the user know that there's a cycle.
 */
@SuppressWarnings({ "unused", "deprecation" }) // deprecated IndexedEmbedded#prefix
class IndexedEmbeddedCycleIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	@RegisterExtension
	public StaticCounters counters = StaticCounters.create();


	@Test
	void cycle_brokenByExcludePaths() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;

				@IndexedEmbedded
				@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW) // just to not bother with inverse sides
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;

				@KeywordField
				String bString;
				@IndexedEmbedded(excludePaths = "b.a")
				EntityA a;

				public EntityB(Integer id, String bString, EntityA a) {
					this.id = id;
					this.bString = bString;
					this.a = a;
				}
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "aString", String.class )
				.objectField( "b", b2 -> b2
						.field( "bString", String.class )
						.objectField( "a", b3 -> b3
								.field( "aString", String.class )
								.objectField( "b", b4 -> b4
										.field( "bString", String.class )
								)
						)
				)
		);

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		Model model = new Model();

		doTestEmbeddedRuntime(
				mapping,
				id -> model.new EntityA(
						id, "a",
						model.new EntityB(
								1, "b",
								model.new EntityA( 2, "aa",
										model.new EntityB( 3, "bb",
												model.new EntityA( 4, "aaa",
														model.new EntityB(
																5, "bbb", model.new EntityA( 6, "aaaa", null ) )
												)
										)
								)
						)
				),
				document -> document.field( "aString", "a" )
						.objectField( "b", b2 -> b2
								.field( "bString", "b" )
								.objectField( "a", b3 -> b3
										.field( "aString", "aa" )
										.objectField( "b", b4 -> b4
												.field( "bString", "bb" )
										)
								)
						)
		);
	}

	@Test
	void cycle_brokenByExcludePathsWithPrefixEndingWithDot() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;

				@IndexedEmbedded(prefix = "prefixForB.")
				@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW) // just to not bother with inverse sides
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;

				@KeywordField
				String bString;
				@IndexedEmbedded(prefix = "prefixForA.", excludePaths = { "prefixForB.prefixForA" })
				EntityA a;

				public EntityB(Integer id, String bString, EntityA a) {
					this.id = id;
					this.bString = bString;
					this.a = a;
				}
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "aString", String.class )
				.objectField( "prefixForB", b2 -> b2
						.field( "bString", String.class )
						.objectField( "prefixForA", b3 -> b3
								.field( "aString", String.class )
								.objectField( "prefixForB", b4 -> b4
										.field( "bString", String.class )
								)
						)
				)
		);

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		Model model = new Model();

		doTestEmbeddedRuntime(
				mapping,
				id -> model.new EntityA(
						id, "a",
						model.new EntityB(
								1, "b",
								model.new EntityA( 2, "aa",
										model.new EntityB( 3, "bb",
												model.new EntityA( 4, "aaa",
														model.new EntityB(
																5, "bbb", model.new EntityA( 6, "aaaa", null ) )
												)
										)
								)
						)
				),
				document -> document.field( "aString", "a" )
						.objectField( "prefixForB", b2 -> b2
								.field( "bString", "b" )
								.objectField( "prefixForA", b3 -> b3
										.field( "aString", "aa" )
										.objectField( "prefixForB", b4 -> b4
												.field( "bString", "bb" )
										)
								)
						)
		);
	}

	@Test
	void cycle_brokenByExcludePathsWithPrefixWithMultipleDotsAndEndingWithDot() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;

				@IndexedEmbedded(prefix = "prefixB1.prefixB2.")
				@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW) // just to not bother with inverse sides
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;

				@KeywordField
				String bString;
				@IndexedEmbedded(prefix = "prefixA1.prefixA2.prefixA3.",
						excludePaths = { "prefixB1.prefixB2.prefixA1.prefixA2.prefixA3" })
				EntityA a;

				public EntityB(Integer id, String bString, EntityA a) {
					this.id = id;
					this.bString = bString;
					this.a = a;
				}
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "aString", String.class )
				.objectField( "prefixB1", b2 -> b2
						.objectField( "prefixB2", b3 -> b3
								.field( "bString", String.class )
								.objectField( "prefixA1", b4 -> b4
										.objectField( "prefixA2", b5 -> b5
												.objectField( "prefixA3", b6 -> b6
														.field( "aString", String.class )
														.objectField( "prefixB1", b7 -> b7
																.objectField( "prefixB2", b8 -> b8
																		.field( "bString", String.class )
																		.objectField( "prefixA1", b9 -> b9
																				.objectField( "prefixA2", b10 -> {}
																				) ) ) ) )

										)
								)
						) ) );

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		Model model = new Model();

		doTestEmbeddedRuntime(
				mapping,
				id -> model.new EntityA(
						id, "a",
						model.new EntityB(
								1, "b",
								model.new EntityA( 2, "aa",
										model.new EntityB( 3, "bb",
												model.new EntityA( 4, "aaa",
														model.new EntityB(
																5, "bbb", model.new EntityA( 6, "aaaa", null ) )
												)
										)
								)
						)
				),
				document -> document.field( "aString", "a" )
						.objectField( "prefixB1", b2 -> b2
								.objectField( "prefixB2", b3 -> b3
										.field( "bString", "b" )
										.objectField( "prefixA1", b4 -> b4
												.objectField( "prefixA2", b5 -> b5
														.objectField( "prefixA3", b6 -> b6
																.field( "aString", "aa" )
																.objectField( "prefixB1", b7 -> b7
																		.objectField( "prefixB2", b8 -> b8
																				.field( "bString", "bb" )
																		) ) )

												)
										)
								) )
		);
	}

	@Test
	void cycle_cannotBeBrokenByExcludePathsWithPrefixNoDot() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;

				@IndexedEmbedded(prefix = "prefixForB")
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;

				@KeywordField
				String bString;
				@IndexedEmbedded(prefix = "prefixForA", excludePaths = { "prefixForBprefixForA" })
				EntityA a;

				public EntityB(Integer id, String bString, EntityA a) {
					this.id = id;
					this.bString = bString;
					this.a = a;
				}
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.EntityA.class.getName() )
						.pathContext( ".b<no value extractors>.a<no value extractors>.b" )
						.failure(
								"Cyclic recursion starting from '@IndexedEmbedded(prefix = \"prefixForB\", ...)' on type '"
										+ Model.EntityA.class.getName() + "', path '.b'",
								"Index field path starting from that location and ending with a cycle: 'prefixForBprefixForAprefixForB'",
								"A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ..."
						)
				);
	}

	@Test
	void cycle_brokenByExcludePaths_deeply() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;

				@IndexedEmbedded
				@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW) // just to not bother with inverse sides
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;

				@KeywordField
				String bString;
				@IndexedEmbedded(excludePaths = "b.a.b.a")
				EntityA a;

				public EntityB(Integer id, String bString, EntityA a) {
					this.id = id;
					this.bString = bString;
					this.a = a;
				}
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "aString", String.class )
				.objectField( "b", b2 -> b2
						.field( "bString", String.class )
						.objectField( "a", b3 -> b3
								.field( "aString", String.class )
								.objectField( "b", b4 -> b4
										.field( "bString", String.class )
										.objectField( "a", b5 -> b5
												.field( "aString", String.class )
												.objectField( "b", b6 -> b6
														.field( "bString", String.class )
												)
										)
								)
						)
				)
		);

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		Model model = new Model();

		doTestEmbeddedRuntime(
				mapping,
				id -> model.new EntityA( id, "a",
						model.new EntityB( 1, "b",
								model.new EntityA( 2, "aa",
										model.new EntityB( 3, "bb",
												model.new EntityA( 4, "aaa",
														model.new EntityB( 5, "bbb",
																model.new EntityA( 6, "aaaa",
																		model.new EntityB( 7, "bbbb",
																				null
																		)
																)
														)
												)
										)
								)
						)
				),
				document -> document.field( "aString", "a" )
						.objectField( "b", b2 -> b2
								.field( "bString", "b" )
								.objectField( "a", b3 -> b3
										.field( "aString", "aa" )
										.objectField( "b", b4 -> b4
												.field( "bString", "bb" )
												.objectField( "a", b5 -> b5
														.field( "aString", "aaa" )
														.objectField( "b", b6 -> b6
																.field( "bString", "bbb" )
														)
												)
										)
								)
						)
		);
	}

	@Test
	void cycle_brokenByExcludePaths_deeply_nonRoot() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;
				@IndexedEmbedded(excludePaths = "c.b.c.b")
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;
				@KeywordField
				String bString;
				@IndexedEmbedded
				EntityC c;

				public EntityB(Integer id, String bString, EntityC c) {
					this.id = id;
					this.bString = bString;
					this.c = c;
				}
			}

			class EntityC {
				Integer id;
				@GenericField
				String cString;
				@IndexedEmbedded
				EntityB b;

				public EntityC(Integer id, String cString, EntityB b) {
					this.id = id;
					this.cString = cString;
					this.b = b;
				}
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "aString", String.class )
				.objectField( "b", b2 -> b2
						.field( "bString", String.class )
						.objectField( "c", b3 -> b3
								.field( "cString", String.class )
								.objectField( "b", b4 -> b4
										.field( "bString", String.class )
										.objectField( "c", b5 -> b5
												.field( "cString", String.class )
										)
								)
						)
				)
		);

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		Model model = new Model();

		doTestEmbeddedRuntime(
				mapping,
				id -> model.new EntityA( id, "a",
						model.new EntityB( 1, "b",
								model.new EntityC( 2, "c",
										model.new EntityB( 3, "bb",
												model.new EntityC( 4, "cc",
														model.new EntityB( 5, "bbb",
																model.new EntityC( 6, "ccc",
																		model.new EntityB( 7, "bbbb",
																				null
																		)
																)
														)
												)
										)
								)
						)
				),
				document -> document.field( "aString", "a" )
						.objectField( "b", b2 -> b2
								.field( "bString", "b" )
								.objectField( "c", b3 -> b3
										.field( "cString", "c" )
										.objectField( "b", b4 -> b4
												.field( "bString", "bb" )
												.objectField( "c", b5 -> b5
														.field( "cString", "cc" )
												)
										)
								)
						)
		);
	}

	@Test
	void cycle_brokenByExcludePathsSomewhereMidCycle_deeply_nonRoot() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@KeywordField
				String aString;
				@IndexedEmbedded
				EntityB b;

				public EntityA(Integer id, String aString, EntityB b) {
					this.id = id;
					this.aString = aString;
					this.b = b;
				}
			}

			class EntityB {
				Integer id;
				@KeywordField
				String bString;
				@IndexedEmbedded
				EntityC c;

				public EntityB(Integer id, String bString, EntityC c) {
					this.id = id;
					this.bString = bString;
					this.c = c;
				}
			}

			class EntityC {
				Integer id;
				@GenericField
				String cString;
				@IndexedEmbedded(excludePaths = "c.b.c")
				EntityB b;

				public EntityC(Integer id, String cString, EntityB b) {
					this.id = id;
					this.cString = cString;
					this.b = b;
				}
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "aString", String.class )
				.objectField( "b", b2 -> b2
						.field( "bString", String.class )
						.objectField( "c", b3 -> b3
								.field( "cString", String.class )
								.objectField( "b", b4 -> b4
										.field( "bString", String.class )
										.objectField( "c", b5 -> b5
												.field( "cString", String.class )
												.objectField( "b", b6 -> b6
														.field( "bString", String.class )
												)
										)
								)
						)
				)
		);

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		Model model = new Model();

		doTestEmbeddedRuntime(
				mapping,
				id -> model.new EntityA( id, "a",
						model.new EntityB( 1, "b",
								model.new EntityC( 2, "c",
										model.new EntityB( 3, "bb",
												model.new EntityC( 4, "cc",
														model.new EntityB( 5, "bbb",
																model.new EntityC( 6, "ccc",
																		model.new EntityB( 7, "bbbb",
																				null
																		)
																)
														)
												)
										)
								)
						)
				),
				document -> document.field( "aString", "a" )
						.objectField( "b", b2 -> b2
								.field( "bString", "b" )
								.objectField( "c", b3 -> b3
										.field( "cString", "c" )
										.objectField( "b", b4 -> b4
												.field( "bString", "bb" )
												.objectField( "c", b5 -> b5
														.field( "cString", "cc" )
														.objectField( "b", b6 -> b6
																.field( "bString", "bbb" )
														)
												)
										)
								)
						)
		);
	}

	@Test
	void cycle() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				EntityB b;
			}

			class EntityB {
				Integer id;
				@IndexedEmbedded
				EntityA a;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.EntityA.class.getName() )
						.pathContext( ".b<no value extractors>.a<no value extractors>.b" )
						.failure(
								"Cyclic recursion starting from '@IndexedEmbedded(...)' on type '"
										+ Model.EntityA.class.getName() + "', path '.b'",
								"Index field path starting from that location and ending with a cycle: 'b.a.b.'",
								"A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ..."
						)
				);
	}

	@Test
	void cycle_nonRoot() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				EntityB b;
			}

			class EntityB {
				Integer id;
				@IndexedEmbedded
				EntityC c;
			}

			class EntityC {
				Integer id;
				@IndexedEmbedded
				EntityB b;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.EntityA.class.getName() )
						.pathContext( ".b<no value extractors>.c<no value extractors>.b<no value extractors>.c" )
						.failure(
								"Cyclic recursion starting from '@IndexedEmbedded(...)' on type '"
										+ Model.EntityB.class.getName() + "', path '.c'.",
								"Index field path starting from that location and ending with a cycle: 'c.b.c.'",
								"A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ..."
						)
				);
	}

	@Test
	void cycle_irrelevantExcludePaths() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@IndexedEmbedded(excludePaths = "bString")
				EntityB b;
			}

			class EntityB {
				Integer id;
				@GenericField
				String bString;
				@IndexedEmbedded
				EntityA a;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.EntityA.class.getName() )
						.pathContext( ".b<no value extractors>.a<no value extractors>.b" )
						.failure(
								"Cyclic recursion starting from '@IndexedEmbedded(...)' on type '"
										+ Model.EntityA.class.getName() + "', path '.b'.",
								"Index field path starting from that location and ending with a cycle: 'b.a.b.'",
								"A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ..."
						)
				);
	}

	@Test
	void cycle_nonRoot_irrelevantExcludePaths() {
		class Model {
			@SearchEntity
			@Indexed(index = INDEX_NAME)
			class EntityA {
				@DocumentId
				Integer id;
				@IndexedEmbedded(excludePaths = "c.b.c.b.c.cString")
				EntityB b;
			}

			class EntityB {
				Integer id;
				@IndexedEmbedded
				EntityC c;
			}

			class EntityC {
				Integer id;
				@GenericField
				String cString;
				@IndexedEmbedded
				EntityB b;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Model.EntityA.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Model.EntityA.class.getName() )
						.pathContext(
								".b<no value extractors>.c<no value extractors>.b<no value extractors>.c<no value extractors>.b<no value extractors>.c<no value extractors>.b<no value extractors>.c" )
						.failure(
								"Cyclic recursion starting from '@IndexedEmbedded(...)' on type '"
										+ Model.EntityB.class.getName() + "', path '.c'.",
								"Index field path starting from that location and ending with a cycle: 'c.b.c.'",
								"A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ..."
						)
				);
	}

	@Test
	void cycle_selfReferenceBreaksEventuallyWithExclude() {
		@SearchEntity
		@Indexed(index = INDEX_NAME)
		class EntityA {
			@DocumentId
			Integer id;
			@KeywordField
			String string;
			@IndexedEmbedded(excludePaths = "a.a.a")
			@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
			EntityA a;

			public EntityA(Integer id, String string, EntityA a) {
				this.id = id;
				this.string = string;
				this.a = a;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "string", String.class )
				.objectField( "a", b2 -> b2
						.field( "string", String.class )
						.objectField( "a", b3 -> b3
								.field( "string", String.class )
								.objectField( "a", b4 -> b4
										.field( "string", String.class )
								)
						)
				)
		);

		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( EntityA.class )
				.setup();

		backendMock.verifyExpectationsMet();

		doTestEmbeddedRuntime(
				mapping,
				id -> new EntityA( id, "a",
						new EntityA( 1, "aa",
								new EntityA( 2, "aaa",
										new EntityA( 3, "aaaa",
												new EntityA( 4, "aaaaa",
														new EntityA( 5, "aaaaa",
																new EntityA( 6, "aaaaaa",
																		new EntityA( 7, "aaaaaaa",
																				null
																		)
																)
														)
												)
										)
								)
						)
				),
				document -> document.field( "string", "a" )
						.objectField( "a", b2 -> b2
								.field( "string", "aa" )
								.objectField( "a", b3 -> b3
										.field( "string", "aaa" )
										.objectField( "a", b4 -> b4
												.field( "string", "aaaa" )
										)
								)
						)
		);
	}

	@Test
	void cycle_selfReferenceWontBreak_excludeSomePropertyNotInCycle() {
		@SearchEntity
		@Indexed(index = INDEX_NAME)
		class EntityA {
			@DocumentId
			Integer id;
			@KeywordField
			String aString;
			@IndexedEmbedded(excludePaths = "a.a.aString")
			EntityA a;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( EntityA.class )
				.setup() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( EntityA.class.getName() )
						.pathContext(
								".a<no value extractors>.a<no value extractors>.a<no value extractors>.a<no value extractors>.a" )
						.failure(
								"Cyclic recursion starting from '@IndexedEmbedded(...)' on type '" + EntityA.class.getName()
										+ "', path '.a'",
								"Index field path starting from that location and ending with a cycle: 'a.a.'",
								"A type cannot declare an unrestricted @IndexedEmbedded to itself, even indirectly",
								"To break the cycle, you should consider adding filters to your @IndexedEmbedded: includePaths, includeDepth, excludePaths, ..."
						)
				);
	}

	private <E> void doTestEmbeddedRuntime(SearchMapping mapping,
			Function<Integer, E> newEntityFunction,
			Consumer<StubDocumentNode.Builder> expectedDocumentContributor) {
		try ( SearchSession session = mapping.createSession() ) {
			E entity1 = newEntityFunction.apply( 1 );

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", expectedDocumentContributor );
		}
		backendMock.verifyExpectationsMet();
	}
}
