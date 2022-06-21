/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("unused")
public class DependencyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void associationInverseSide_error_missingInversePath() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@AssociationInverseSide(inversePath = @ObjectPath({}))
			public IndexedEntity getOther() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".other" )
						.annotationContextAnyParameters( AssociationInverseSide.class )
						.failure(
								"@AssociationInverseSide.inversePath is empty"
						)
				);
	}

	@Test
	public void derivedFrom() {
		final String indexName = "index1";
		@Indexed(index = indexName)
		class IndexedEntity {
			@DocumentId
			Integer id;
			String source1;
			String source2;
			String unused;
			@GenericField
			@IndexingDependency(derivedFrom = {
					@ObjectPath(@PropertyValue(propertyName = "source1")),
					@ObjectPath(@PropertyValue(propertyName = "source2"))})
			public String getDerived() {
				return source1 + " " + source2;
			}
		}

		backendMock.expectSchema( indexName, b -> b
				.field( "derived", String.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity = new IndexedEntity();
		entity.id = 1;
		entity.source1 = "init1";
		entity.source2 = "init2";
		entity.unused = "init3";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity );

			backendMock.expectWorks( indexName )
					.add( "1", b -> b.field( "derived", "init1 init2" ) );
		}
		backendMock.verifyExpectationsMet();

		// Changed to unused properties are ignored
		try ( SearchSession session = mapping.createSession() ) {
			entity.unused = "updated3";
			session.indexingPlan().addOrUpdate( entity, "unused" );

			// Expect no reindexing at all
		}
		backendMock.verifyExpectationsMet();

		// Changes to source properties trigger reindexing
		try ( SearchSession session = mapping.createSession() ) {
			entity.source1 = "updated1";
			session.indexingPlan().addOrUpdate( entity, "source1" );

			backendMock.expectWorks( indexName )
					.addOrUpdate( "1", b -> b.field( "derived", "updated1 init2" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4148")
	public void derivedFrom_polymorphism() {
		final String index1Name = "index1Name";
		final String index2Name = "index2Name";
		@Indexed
		abstract class AbstractIndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			public abstract String getDerived();
		}
		@Indexed(index = index1Name)
		class IndexedEntity1 extends AbstractIndexedEntity {
			String source1;
			String source2;
			String source3;
			String source4;
			@Override
			@IndexingDependency(derivedFrom = {
					@ObjectPath(@PropertyValue(propertyName = "source1")),
					@ObjectPath(@PropertyValue(propertyName = "source2")),
					@ObjectPath(@PropertyValue(propertyName = "source4"))})
			public String getDerived() {
				return source1 + " " + source2 + " " + source4;
			}
		}
		@Indexed(index = index2Name)
		class IndexedEntity2 extends AbstractIndexedEntity {
			String source1;
			String source2;
			String source3;
			String source5;
			@Override
			@IndexingDependency(derivedFrom = {
					@ObjectPath(@PropertyValue(propertyName = "source1")),
					@ObjectPath(@PropertyValue(propertyName = "source3")),
					@ObjectPath(@PropertyValue(propertyName = "source5"))})
			public String getDerived() {
				return source1 + " " + source3 + " " + source5;
			}
		}

		backendMock.expectSchema( index1Name, b -> b
				.field( "derived", String.class )
		);
		backendMock.expectSchema( index2Name, b -> b
				.field( "derived", String.class )
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( AbstractIndexedEntity.class, IndexedEntity1.class, IndexedEntity2.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity1 entity1 = new IndexedEntity1();
		entity1.id = 1;
		entity1.source1 = "init1";
		entity1.source2 = "init2";
		entity1.source3 = "init3";
		entity1.source4 = "init4";

		IndexedEntity2 entity2 = new IndexedEntity2();
		entity2.id = 2;
		entity2.source1 = "init1";
		entity2.source2 = "init2";
		entity2.source3 = "init3";
		entity2.source5 = "init5";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );

			backendMock.expectWorks( index1Name )
					.add( "1", b -> b.field( "derived", "init1 init2 init4" ) );
			backendMock.expectWorks( index2Name )
					.add( "2", b -> b.field( "derived", "init1 init3 init5" ) );
		}
		backendMock.verifyExpectationsMet();

		// Changed to unused properties are ignored
		try ( SearchSession session = mapping.createSession() ) {
			entity1.source3 = "updated3";
			entity2.source2 = "updated2";
			session.indexingPlan().addOrUpdate( entity1, "source3" );
			session.indexingPlan().addOrUpdate( entity2, "source2" );

			// Expect no reindexing at all
		}
		backendMock.verifyExpectationsMet();

		// Changes to common source properties trigger reindexing
		try ( SearchSession session = mapping.createSession() ) {
			entity1.source1 = "updated1";
			entity2.source1 = "updated1";
			session.indexingPlan().addOrUpdate( entity1, "source1" );
			session.indexingPlan().addOrUpdate( entity2, "source1" );

			backendMock.expectWorks( index1Name )
					.addOrUpdate( "1", b -> b.field( "derived", "updated1 init2 init4" ) );
			backendMock.expectWorks( index2Name )
					.addOrUpdate( "2", b -> b.field( "derived", "updated1 init3 init5" ) );
		}
		backendMock.verifyExpectationsMet();

		// Changes to specific properties that exist in both types, but are source in only one type
		// trigger reindexing for the relevant types.
		try ( SearchSession session = mapping.createSession() ) {
			entity1.source2 = "updated2";
			entity2.source3 = "updated3";
			session.indexingPlan().addOrUpdate( entity1, "source2" );
			session.indexingPlan().addOrUpdate( entity2, "source3" );

			backendMock.expectWorks( index1Name )
					.addOrUpdate( "1", b -> b.field( "derived", "updated1 updated2 init4" ) );
			backendMock.expectWorks( index2Name )
					.addOrUpdate( "2", b -> b.field( "derived", "updated1 updated3 init5" ) );
		}
		backendMock.verifyExpectationsMet();

		// Changes to specific properties that exist in only one type and are source in only one type
		// trigger reindexing for the relevant types.
		try ( SearchSession session = mapping.createSession() ) {
			entity1.source4 = "updated4";
			entity2.source5 = "updated5";
			session.indexingPlan().addOrUpdate( entity1, "source4" );
			session.indexingPlan().addOrUpdate( entity2, "source5" );

			backendMock.expectWorks( index1Name )
					.addOrUpdate( "1", b -> b.field( "derived", "updated1 updated2 updated4" ) );
			backendMock.expectWorks( index2Name )
					.addOrUpdate( "2", b -> b.field( "derived", "updated1 updated3 updated5" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test a polymorphic derivedFrom that is not at the root, e.g. on a property in an @IndexedEmbedded.
	 * <p>
	 * This is sensibly different from a polymorphic derivedFrom at the root,
	 * since we need to handle polymorphism in a single reindexing resolver,
	 * instead of having a separate reindexing resolver per type.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4148")
	public void derivedFrom_nonRoot_polymorphism() {
		final String indexName = "indexName";
		class Model {
			@Indexed(index = indexName)
			class IndexedEntity {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				AbstractContainedEntity contained;
			}

			abstract class AbstractContainedEntity {
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contained")))
				IndexedEntity containing;

				@GenericField
				public abstract String getDerived();
			}

			class ContainedEntity1 extends AbstractContainedEntity {
				String source1;
				String source2;
				String source3;
				String source4;

				@Override
				@IndexingDependency(derivedFrom = {
						@ObjectPath(@PropertyValue(propertyName = "source1")),
						@ObjectPath(@PropertyValue(propertyName = "source2")),
						@ObjectPath(@PropertyValue(propertyName = "source4"))
				})
				public String getDerived() {
					return source1 + " " + source2 + " " + source4;
				}
			}

			class ContainedEntity2 extends AbstractContainedEntity {
				String source1;
				String source2;
				String source3;
				String source5;

				@Override
				@IndexingDependency(derivedFrom = {
						@ObjectPath(@PropertyValue(propertyName = "source1")),
						@ObjectPath(@PropertyValue(propertyName = "source3")),
						@ObjectPath(@PropertyValue(propertyName = "source5"))
				})
				public String getDerived() {
					return source1 + " " + source3 + " " + source5;
				}
			}
		}

		backendMock.expectSchema( indexName, b -> b
				.objectField( "contained", b2 -> b2
						.field( "derived", String.class )
				)
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( Model.IndexedEntity.class, Model.AbstractContainedEntity.class,
						Model.ContainedEntity1.class, Model.ContainedEntity2.class );
		backendMock.verifyExpectationsMet();

		Model model = new Model();

		Model.IndexedEntity indexed1 = model.new IndexedEntity();
		indexed1.id = 1;
		Model.ContainedEntity1 contained1 = model.new ContainedEntity1();
		indexed1.contained = contained1;
		contained1.containing = indexed1;
		contained1.source1 = "init1";
		contained1.source2 = "init2";
		contained1.source3 = "init3";
		contained1.source4 = "init4";

		Model.IndexedEntity indexed2 = model.new IndexedEntity();
		indexed2.id = 2;
		Model.ContainedEntity2 contained2 = model.new ContainedEntity2();
		indexed2.contained = contained2;
		contained2.containing = indexed2;
		contained2.source1 = "init1";
		contained2.source2 = "init2";
		contained2.source3 = "init3";
		contained2.source5 = "init5";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( indexed1 );
			session.indexingPlan().add( indexed2 );

			backendMock.expectWorks( indexName )
					.add( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "init1 init2 init4" ) ) )
					.add( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "init1 init3 init5" ) ) );
		}
		backendMock.verifyExpectationsMet();

		// Changed to unused properties are ignored
		try ( SearchSession session = mapping.createSession() ) {
			contained1.source3 = "updated3";
			contained2.source2 = "updated2";
			session.indexingPlan().addOrUpdate( 1, null, contained1, "source3" );
			session.indexingPlan().addOrUpdate( 2, null, contained2, "source2" );

			// Expect no reindexing at all
		}
		backendMock.verifyExpectationsMet();

		// Changes to common source properties trigger reindexing
		try ( SearchSession session = mapping.createSession() ) {
			contained1.source1 = "updated1";
			contained2.source1 = "updated1";
			session.indexingPlan().addOrUpdate( 1, null, contained1, "source1" );
			session.indexingPlan().addOrUpdate( 2, null, contained2, "source1" );

			backendMock.expectWorks( indexName )
					.addOrUpdate( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 init2 init4" ) ) )
					.addOrUpdate( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 init3 init5" ) ) );
		}
		backendMock.verifyExpectationsMet();

		// Changes to specific properties that exist in both types, but are source in only one type
		// trigger reindexing for the relevant types.
		try ( SearchSession session = mapping.createSession() ) {
			contained1.source2 = "updated2";
			contained2.source3 = "updated3";
			session.indexingPlan().addOrUpdate( 1, null, contained1, "source2" );
			session.indexingPlan().addOrUpdate( 2, null, contained2, "source3" );

			backendMock.expectWorks( indexName )
					.addOrUpdate( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 updated2 init4" ) ) )
					.addOrUpdate( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 updated3 init5" ) ) );
		}
		backendMock.verifyExpectationsMet();

		// Changes to specific properties that exist in only one type and are source in only one type
		// trigger reindexing for the relevant types.
		try ( SearchSession session = mapping.createSession() ) {
			contained1.source4 = "updated4";
			contained2.source5 = "updated5";
			session.indexingPlan().addOrUpdate( 1, null, contained1, "source4" );
			session.indexingPlan().addOrUpdate( 2, null, contained2, "source5" );

			backendMock.expectWorks( indexName )
					.addOrUpdate( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 updated2 updated4" ) ) )
					.addOrUpdate( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 updated3 updated5" ) ) );
		}
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test a polymorphic derivedFrom that is not at the root, e.g. on a property in an @IndexedEmbedded,
	 * and that involves generics (which should thus be preserved).
	 * <p>
	 * This test is useful mainly for non-regression, because the handling of polymorphism involves casts,
	 * and if implemented incorrectly those cases could result in type erasure that would make the whole process fail.
	 *
	 * @see #derivedFrom_nonRoot_polymorphism()
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4148")
	public void derivedFrom_nonRoot_polymorphism_prevervesGenerics() {
		final String indexName = "indexName";
		class Model {
			@Indexed(index = indexName)
			class IndexedEntity {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				AbstractContainedEntity<Model.OtherContainedEntity> contained;
			}

			abstract class AbstractContainedEntity<T> {
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contained")))
				IndexedEntity containing;

				T contained;

				@GenericField
				public abstract String getDerived();
			}

			class ContainedEntity1<T extends OtherContainedSuperClass> extends AbstractContainedEntity<T> {
				@Override
				@IndexingDependency(derivedFrom = {
						@ObjectPath({@PropertyValue(propertyName = "contained"), @PropertyValue(propertyName = "source1")}),
						@ObjectPath({@PropertyValue(propertyName = "contained"), @PropertyValue(propertyName = "source2")})
				})
				public String getDerived() {
					return contained.source1 + " " + contained.source2;
				}
			}

			class ContainedEntity2<T extends OtherContainedSuperClass> extends AbstractContainedEntity<T> {
				@Override
				@IndexingDependency(derivedFrom = {
						@ObjectPath({@PropertyValue(propertyName = "contained"), @PropertyValue(propertyName = "source1")}),
						@ObjectPath({@PropertyValue(propertyName = "contained"), @PropertyValue(propertyName = "source3")})
				})
				public String getDerived() {
					return contained.source1 + " " + contained.source3;
				}
			}

			class OtherContainedSuperClass {
				String source1;
				String source2;
				String source3;
			}

			class OtherContainedEntity extends OtherContainedSuperClass {
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contained")))
				AbstractContainedEntity<OtherContainedEntity> containing;
			}
		}

		backendMock.expectSchema( indexName, b -> b
				.objectField( "contained", b2 -> b2
						.field( "derived", String.class )
				)
		);

		SearchMapping mapping = setupHelper.start().expectCustomBeans()
				.setup( Model.IndexedEntity.class, Model.AbstractContainedEntity.class,
						Model.ContainedEntity1.class, Model.ContainedEntity2.class, Model.OtherContainedEntity.class );
		backendMock.verifyExpectationsMet();

		Model model = new Model();

		Model.IndexedEntity indexed1 = model.new IndexedEntity();
		indexed1.id = 1;
		Model.ContainedEntity1<Model.OtherContainedEntity> contained1 = model.new ContainedEntity1<>();
		indexed1.contained = contained1;
		contained1.containing = indexed1;
		Model.OtherContainedEntity otherContained1 = model.new OtherContainedEntity();
		contained1.contained = otherContained1;
		otherContained1.containing = contained1;
		otherContained1.source1 = "init1";
		otherContained1.source2 = "init2";
		otherContained1.source3 = "init3";

		Model.IndexedEntity indexed2 = model.new IndexedEntity();
		indexed2.id = 2;
		Model.ContainedEntity2<Model.OtherContainedEntity> contained2 = model.new ContainedEntity2<>();
		indexed2.contained = contained2;
		contained2.containing = indexed2;
		Model.OtherContainedEntity otherContained2 = model.new OtherContainedEntity();
		contained2.contained = otherContained2;
		otherContained2.containing = contained2;
		otherContained2.source1 = "init1";
		otherContained2.source2 = "init2";
		otherContained2.source3 = "init3";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( indexed1 );
			session.indexingPlan().add( indexed2 );

			backendMock.expectWorks( indexName )
					.add( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "init1 init2" ) ) )
					.add( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "init1 init3" ) ) );
		}
		backendMock.verifyExpectationsMet();

		// Changed to unused properties are ignored
		try ( SearchSession session = mapping.createSession() ) {
			otherContained1.source3 = "updated3";
			otherContained2.source2 = "updated2";
			session.indexingPlan().addOrUpdate( 1, null, otherContained1, "source3" );
			session.indexingPlan().addOrUpdate( 2, null, otherContained2, "source2" );

			// Expect no reindexing at all
		}
		backendMock.verifyExpectationsMet();

		// Changes to common source properties trigger reindexing
		try ( SearchSession session = mapping.createSession() ) {
			otherContained1.source1 = "updated1";
			otherContained2.source1 = "updated1";
			session.indexingPlan().addOrUpdate( 1, null, otherContained1, "source1" );
			session.indexingPlan().addOrUpdate( 2, null, otherContained2, "source1" );

			backendMock.expectWorks( indexName )
					.addOrUpdate( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 init2" ) ) )
					.addOrUpdate( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 init3" ) ) );
		}
		backendMock.verifyExpectationsMet();

		// Changes to specific properties that exist in both types, but are source in only one type
		// trigger reindexing for the relevant types.
		try ( SearchSession session = mapping.createSession() ) {
			otherContained1.source2 = "updated2";
			otherContained2.source3 = "updated3";
			session.indexingPlan().addOrUpdate( 1, null, otherContained1, "source2" );
			session.indexingPlan().addOrUpdate( 2, null, otherContained2, "source3" );

			backendMock.expectWorks( indexName )
					.addOrUpdate( "1", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 updated2" ) ) )
					.addOrUpdate( "2", b -> b.objectField( "contained", b2 -> b2
							.field( "derived", "updated1 updated3" ) ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void derivedFrom_error_missingPath() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			@IndexingDependency(derivedFrom = @ObjectPath({}))
			public String getDerived() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".derived" )
						.annotationContextAnyParameters( IndexingDependency.class )
						.failure(
								"@IndexingDependency.derivedFrom contains an empty path"
						)
				);
	}

	@Test
	public void derivedFrom_error_invalidPath() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField
			@IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "invalidPath")))
			public String getDerived() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".derived<no value extractors>" )
						.failure( "No readable property named 'invalidPath' on type '"
								+ IndexedEntity.class.getName() + "'" ) );
	}

	@Test
	public void derivedFrom_error_cycle() {
		class DerivedFromCycle {
			@Indexed
			class A {
				@DocumentId
				Integer id;
				B b;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "b"),
						@PropertyValue(propertyName = "derivedB")
				}))
				public String getDerivedA() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class B {
				C c;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "c"),
						@PropertyValue(propertyName = "derivedC")
				}))
				public String getDerivedB() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class C {
				A a;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "a"),
						@PropertyValue(propertyName = "derivedA")
				}))
				public String getDerivedC() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						.withAnnotatedEntityTypes( DerivedFromCycle.A.class )
						.withAnnotatedTypes( DerivedFromCycle.B.class, DerivedFromCycle.C.class )
						.setup()
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( DerivedFromCycle.A.class.getName() )
						.pathContext( ".derivedA<no value extractors>" )
						.multilineFailure( "Unable to resolve dependencies of a derived property:"
										+ " there is a cyclic dependency starting from type '" + DerivedFromCycle.A.class.getName() + "'",
								"Derivation chain starting from that type and ending with a cycle:\n"
										+ "- " + DerivedFromCycle.A.class.getName() + "#.b<default value extractors>.derivedB<default value extractors>\n"
										+ "- " + DerivedFromCycle.B.class.getName() + "#.c<default value extractors>.derivedC<default value extractors>\n"
										+ "- " + DerivedFromCycle.C.class.getName() + "#.a<default value extractors>.derivedA<default value extractors>\n",
								"A derived property cannot be marked as derived from itself",
								"you should consider disabling automatic reindexing"
						) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4565")
	public void derivedFrom_error_cycle_buried() {
		class DerivedFromCycle {
			@Indexed
			class Zero {
				@DocumentId
				Integer id;
				A a;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "a"),
						@PropertyValue(propertyName = "derivedA")
				}))
				public String getDerivedZero() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class A {
				B b;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "b"),
						@PropertyValue(propertyName = "derivedB")
				}))
				public String getDerivedA() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class B {
				C c;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "c"),
						@PropertyValue(propertyName = "derivedC")
				}))
				public String getDerivedB() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class C {
				A a;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "a"),
						@PropertyValue(propertyName = "derivedA")
				}))
				public String getDerivedC() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						.withAnnotatedEntityTypes( DerivedFromCycle.Zero.class )
						.withAnnotatedTypes( DerivedFromCycle.A.class, DerivedFromCycle.B.class, DerivedFromCycle.C.class )
						.setup()
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( DerivedFromCycle.Zero.class.getName() )
						.pathContext( ".derivedZero<no value extractors>" )
						.multilineFailure( "Unable to resolve dependencies of a derived property:"
										+ " there is a cyclic dependency starting from type '" + DerivedFromCycle.A.class.getName() + "'",
								"Derivation chain starting from that type and ending with a cycle:\n"
										+ "- " + DerivedFromCycle.A.class.getName() + "#.b<default value extractors>.derivedB<default value extractors>\n"
										+ "- " + DerivedFromCycle.B.class.getName() + "#.c<default value extractors>.derivedC<default value extractors>\n"
										+ "- " + DerivedFromCycle.C.class.getName() + "#.a<default value extractors>.derivedA<default value extractors>\n",
								"A derived property cannot be marked as derived from itself",
								"you should consider disabling automatic reindexing"
						)
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4423")
	public void derivedFrom_cycleFalsePositive() {
		final String indexName = "myindex";
		class DerivedFromCycle {
			@Indexed(index = indexName)
			class A {
				@DocumentId
				Integer id;
				B b;
				@GenericField
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "b"),
						@PropertyValue(propertyName = "c"),
						@PropertyValue(propertyName = "derivedA")
				}))
				public String getDerivedA() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class B {
				C c;
			}
			class C {
				A a;
				// Important: this property must have the same name as the property in A
				public String getDerivedA() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}

		backendMock.expectSchema( indexName, b -> b
				.field( "derivedA", String.class )
		);

		assertThatCode(
				() -> setupHelper.start()
						.withAnnotatedEntityTypes( DerivedFromCycle.A.class )
						.withAnnotatedTypes( DerivedFromCycle.B.class, DerivedFromCycle.C.class )
						.setup()
		)
				.doesNotThrowAnyException();
	}

	@Test
	public void error_cannotInvertAssociation() {
		class CannotInvertAssociation {
			@Indexed
			class A {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				Embedded embedded;
			}
			class Embedded {
				@IndexedEmbedded
				B b;
			}
			class B {
				A a;
				@GenericField
				String text;
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup(
						CannotInvertAssociation.A.class, CannotInvertAssociation.B.class
				)
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( CannotInvertAssociation.A.class.getName() )
						.pathContext( ".embedded<no value extractors>.b<no value extractors>.text<no value extractors>" )
						.failure(
								"Unable to find the inverse side of the association on type '" + CannotInvertAssociation.A.class.getName() + "'"
										+ " at path '.embedded<no value extractors>.b<no value extractors>'",
								" Hibernate Search needs this information in order to reindex '"
										+ CannotInvertAssociation.A.class.getName() + "' when '"
										+ CannotInvertAssociation.B.class.getName() + "' is modified.",
								// Tips
								"@OneToMany(mappedBy",
								"@AssociationInverseSide",
								"if you do not need to reindex '"
										+ CannotInvertAssociation.A.class.getName() + "' when '"
										+ CannotInvertAssociation.B.class.getName() + "' is modified",
								"@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)"
						) );
	}

	@Test
	public void error_cannotApplyInvertAssociationPath_propertyNotFound() {
		class CannotApplyInvertAssociationPath {
			@Indexed
			class A {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "invalidPath")))
				B b;
			}
			class B {
				A a;
				@GenericField
				String text;
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup(
						CannotApplyInvertAssociationPath.A.class, CannotApplyInvertAssociationPath.B.class
				)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Unable to apply path '.invalidPath<default value extractors>'"
						+ " to type '" + CannotApplyInvertAssociationPath.B.class.getName() + "'"
				)
				.hasMessageContaining(
						"This path was resolved as the inverse side of the association '.b<no value extractors>'"
						+ " on type '" + CannotApplyInvertAssociationPath.A.class.getName() + "'"
				)
				.hasMessageContaining(
						"Hibernate Search needs to apply this path in order to reindex '"
								+ CannotApplyInvertAssociationPath.A.class.getName() + "' when '"
								+ CannotApplyInvertAssociationPath.B.class.getName() + "' is modified."
				)
				.hasMessageContaining( "No readable property named 'invalidPath' on type '"
						+ CannotApplyInvertAssociationPath.B.class.getName() + "'" );
	}

	@Test
	public void error_cannotApplyInvertAssociationPath_incorrectTargetTypeForInverseAssociation() {
		class CannotApplyInvertAssociationPath {
			@Indexed
			class A {
				@DocumentId
				Integer id;
				@IndexedEmbedded
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "a")))
				B b;
			}
			class B {
				String a;
				@GenericField
				String text;
			}
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup(
						CannotApplyInvertAssociationPath.A.class, CannotApplyInvertAssociationPath.B.class
				)
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Unable to apply path '.a<default value extractors>'"
						+ " to type '" + CannotApplyInvertAssociationPath.B.class.getName() + "'"
				)
				.hasMessageContaining(
						"This path was resolved as the inverse side of the association '.b<no value extractors>'"
						+ " on type '" + CannotApplyInvertAssociationPath.A.class.getName() + "'"
				)
				.hasMessageContaining(
						"Hibernate Search needs to apply this path in order to reindex '"
								+ CannotApplyInvertAssociationPath.A.class.getName() + "' when '"
								+ CannotApplyInvertAssociationPath.B.class.getName() + "' is modified."
				)
				.hasMessageContaining(
						"The inverse association targets type '" + String.class.getName()
						+ "', but a supertype or subtype of '" + CannotApplyInvertAssociationPath.A.class.getName()
						+ "' was expected"
				);
	}

}
