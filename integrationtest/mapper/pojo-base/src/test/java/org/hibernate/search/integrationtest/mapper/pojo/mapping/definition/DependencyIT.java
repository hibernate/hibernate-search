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

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("unused")
public class DependencyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".other" )
						.annotationContextAnyParameters( AssociationInverseSide.class )
						.failure(
								"@AssociationInverseSide.inversePath is empty"
						)
						.build()
				);
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".derived" )
						.annotationContextAnyParameters( IndexingDependency.class )
						.failure(
								"@IndexingDependency.derivedFrom contains an empty path"
						)
						.build()
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".derived<no value extractors>" )
						.failure( "No readable property named 'invalidPath' on type '"
								+ IndexedEntity.class.getName() + "'" )
						.build()
				);
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( DerivedFromCycle.A.class.getName() )
						.pathContext( ".derivedA<no value extractors>" )
						.failure( "Unable to resolve dependencies of a derived property:"
								+ " there is a cyclic dependency involving path '.derivedA<no value extractors>'"
								+ " on type '" + DerivedFromCycle.A.class.getName() + "'",
								"A derived property cannot be marked as derived from itself",
								"you should consider disabling automatic reindexing"
						)
						.build()
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
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
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
						)
						.build()
				);
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
