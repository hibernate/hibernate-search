/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.beans.Transient;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.integrationtest.mapper.pojo.test.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class DependencyIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void associationInverseSide_error_missingInversePath() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@AssociationInverseSide(inversePath = @ObjectPath({}))
			public IndexedEntity getOther() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".other" )
						.annotationContextAnyParameters( AssociationInverseSide.class )
						.failure(
								"Property 'other'",
								IndexedEntity.class.getName(),
								"is annotated with @AssociationInverseSide",
								"the inverse path is empty"
						)
						.build()
				);
	}

	@Test
	public void derivedFrom_error_missingPath() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@Transient
			@Field
			@IndexingDependency(derivedFrom = @ObjectPath({}))
			public String getDerived() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".derived" )
						.annotationContextAnyParameters( IndexingDependency.class )
						.failure(
								"Property 'derived'",
								IndexedEntity.class.getName(),
								"is annotated with @IndexingDependency",
								"'derivedFrom' contains an empty path"
						)
						.build()
				);
	}

	@Test
	public void derivedFrom_error_invalidPath() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@Transient
			@Field
			@IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "invalidPath")))
			public String getDerived() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".derived<no value extractors>" )
						.failure(
								"Unable to find property 'invalidPath' on type '"
										+ IndexedEntity.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void derivedFrom_error_cycle() {
		class DerivedFromCycle {
			@Indexed
			class A {
				Integer id;
				B b;
				@DocumentId
				public Integer getId() {
					return id;
				}
				public B getB() {
					return b;
				}
				@Transient
				@Field
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
				public C getC() {
					return c;
				}
				@Transient
				@Field
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
				public A getA() {
					return a;
				}
				@Transient
				@Field
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "a"),
						@PropertyValue(propertyName = "derivedA")
				}))
				public String getDerivedC() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup(
						CollectionHelper.asLinkedHashSet( DerivedFromCycle.A.class ),
						CollectionHelper.asLinkedHashSet(
								DerivedFromCycle.A.class, DerivedFromCycle.B.class, DerivedFromCycle.C.class
						)
				)
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( DerivedFromCycle.A.class.getName() )
						.pathContext( ".derivedA<no value extractors>" )
						.failure(
								"Found a cyclic dependency between derived properties"
										+ " involving path '.derivedA<no value extractors>'"
										+ " on type '" + DerivedFromCycle.A.class.getName() + "'",
								"you should consider disabling automatic reindexing"
						)
						.build()
				);
	}

	@Test
	public void error_cannotInvertAssociation() {
		class CannotInvertAssociation {
			@Indexed
			class A {
				Integer id;
				Embedded embedded;
				@DocumentId
				public Integer getId() {
					return id;
				}
				@IndexedEmbedded
				public Embedded getEmbedded() {
					return embedded;
				}
			}
			class Embedded {
				B b;
				@IndexedEmbedded
				public B getB() {
					return b;
				}
			}
			class B {
				A a;
				String text;
				public A getA() {
					return a;
				}
				@Field
				public String getText() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup(
						CannotInvertAssociation.A.class, CannotInvertAssociation.B.class
				)
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( CannotInvertAssociation.A.class.getName() )
						.pathContext( ".embedded<no value extractors>.b<no value extractors>.text<no value extractors>" )
						.failure(
								"Cannot find the inverse side of the association at path "
										+ "'.embedded<no value extractors>.b<no value extractors>'",
								"from type '" + CannotInvertAssociation.A.class.getName() + "'",
								"on type '" + CannotInvertAssociation.B.class.getName() + "'"
						)
						.build()
				);
	}

	@Test
	public void error_cannotApplyInvertAssociationPath_propertyNotFound() {
		class CannotApplyInvertAssociationPath {
			@Indexed
			class A {
				Integer id;
				B b;
				@DocumentId
				public Integer getId() {
					return id;
				}
				@IndexedEmbedded
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "invalidPath")))
				public B getB() {
					return b;
				}
			}
			class B {
				A a;
				String text;
				public A getA() {
					return a;
				}
				@Field
				public String getText() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup(
						CannotApplyInvertAssociationPath.A.class, CannotApplyInvertAssociationPath.B.class
				)
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Cannot apply the path of the inverse association '.invalidPath<default value extractors>'"
						+ " from type '" + CannotApplyInvertAssociationPath.B.class.getName() + "'"
				)
				.hasMessageContaining(
						"Association on the original side (which was being inverted) was '.b<no value extractors>'"
						+ " on type '" + CannotApplyInvertAssociationPath.A.class.getName() + "'"
				)
				.hasMessageContaining(
						"Unable to find property 'invalidPath' on type '"
						+ CannotApplyInvertAssociationPath.B.class.getName() + "'"
				);
	}

	@Test
	public void error_cannotApplyInvertAssociationPath_incorrectTargetTypeForInverseAssociation() {
		class CannotApplyInvertAssociationPath {
			@Indexed
			class A {
				Integer id;
				B b;
				@DocumentId
				public Integer getId() {
					return id;
				}
				@IndexedEmbedded
				@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "a")))
				public B getB() {
					return b;
				}
			}
			class B {
				String a;
				String text;
				public String getA() {
					return a;
				}
				@Field
				public String getText() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup(
						CannotApplyInvertAssociationPath.A.class, CannotApplyInvertAssociationPath.B.class
				)
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Cannot apply the path of the inverse association '.a<default value extractors>'"
								+ " from type '" + CannotApplyInvertAssociationPath.B.class.getName() + "'"
				)
				.hasMessageContaining(
						"Association on the original side (which was being inverted) was '.b<no value extractors>'"
								+ " on type '" + CannotApplyInvertAssociationPath.A.class.getName() + "'"
				)
				.hasMessageContaining(
						"The inverse association targets type '" + String.class.getName()
						+ "', but a supertype or subtype of '" + CannotApplyInvertAssociationPath.A.class.getName()
						+ "' was expected"
				);
	}

}
