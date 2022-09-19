/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test automatic indexing based on Hibernate ORM entity events when embeddable objects are involved.
 */
public class AutomaticIndexingEmbeddableIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		// Embedded deserialization rules in ORM are just weird:
		// when all columns are empty, an embedded is sometimes deserialized as `null`
		// (though that can be prevented with hibernate.create_empty_composites.enabled = true),
		// and this applies to embeddeds in @ElementCollections as well.
		// That's just too much noise when writing indexing assertions,
		// so we'll consider null equivalent to an empty instance for embeddeds.
		backendMock.documentDiffer( IndexedEntity.INDEX, StubTreeNodeDiffer.<StubDocumentNode>builder()
				.missingEquivalentToEmptyForPath( "containedEmbeddedSingle" )
				.missingEquivalentToEmptyForPath( "containedEmbeddedList" )
				.missingEquivalentToEmptyForPath( "containedElementCollection" )
				.missingEquivalentToEmptyForPath( "containedBidirectionalEmbedded" )
				.missingEquivalentToEmptyForPath( "containedSingleWithInverseSideEmbedded" )
				.missingEquivalentToEmptyForPath( "child.containedEmbeddedSingle" )
				.missingEquivalentToEmptyForPath( "child.containedEmbeddedList" )
				.missingEquivalentToEmptyForPath( "child.containedElementCollection" )
				.missingEquivalentToEmptyForPath( "child.containedBidirectionalEmbedded" )
				.missingEquivalentToEmptyForPath( "child.containedSingleWithInverseSideEmbedded" )
				.build() );


		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "child", b4 -> b4
						.objectField( "containedEmbeddedSingle", b2 -> b2
								.objectField( "containedSingle", b3 -> b3
										.field( "includedInEmbeddedSingle", String.class )
								)
						)
						.objectField( "containedEmbeddedList", b2 -> b2
								.objectField( "containedList", b3 -> b3
										.multiValued( true )
										.field( "includedInEmbeddedList", String.class )
								)
						)
						.objectField( "containedElementCollection", b2 -> b2
								.multiValued( true )
								.objectField( "containedSingle", b3 -> b3
										.field( "includedInElementCollection", String.class )
								)
						)
						.objectField( "containedBidirectionalEmbedded", b2 -> b2
								.objectField( "containedSingle", b3 -> b3
										.field( "includedInBidirectionalEmbedded", String.class )
								)
						)
						.objectField( "containedSingleWithInverseSideEmbedded", b2 -> b2
								.field( "includedInContainedSingleWithInverseSideEmbedded", String.class )
						)
				)
		);

		setupContext
				.withAnnotatedTypes(
						IndexedEntity.class,
						ContainingEntity.class,
						ContainedEntity.class
				);

		dataClearConfig.preClear( ContainedEntity.class, contained -> {
			contained.getContainingAsElementCollection().clear();
			contained.getContainingAsSingleWithInverseSideEmbedded().setContainingAsSingle( null );
		} );
		dataClearConfig.clearOrder( ContainingEntity.class, IndexedEntity.class, ContainedEntity.class );
	}

	@Test
	public void indirectEmbeddedUpdate_embeddedSingle() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInEmbeddedSingle( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedEmbeddedSingle( new SingleContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsEmbeddedSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInEmbeddedSingle", "initialValue" )
											)
									)
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInEmbeddedSingle( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedSingle().getContainedSingle().getContainingAsEmbeddedSingle().clear();
			containingEntity1.setContainedEmbeddedSingle( new SingleContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsEmbeddedSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInEmbeddedSingle", "updatedValue" )
											)
									)
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedSingle().getContainedSingle().getContainingAsEmbeddedSingle().clear();
			containingEntity1.setContainedEmbeddedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> {
								b2.objectField( "containedEmbeddedList", b3 -> { } );
								b2.objectField( "containedBidirectionalEmbedded", b3 -> { } );
								b2.objectField( "containedEmbeddedSingle", b3 -> { } );
							} )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_embeddedSingle() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInEmbeddedSingle( "initialValue" );
			containingEntity1.setContainedEmbeddedSingle( new SingleContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsEmbeddedSingle().add( containingEntity1 );

			session.persist( containedEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInEmbeddedSingle", "initialValue" )
											)
									)
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInEmbeddedSingle( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedSingle().getContainedSingle().getContainingAsEmbeddedSingle().clear();
			containingEntity1.getContainedEmbeddedSingle().setContainedSingle( containedEntity );
			containedEntity.getContainingAsEmbeddedSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInEmbeddedSingle", "updatedValue" )
											)
									)
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedSingle().getContainedSingle().getContainingAsEmbeddedSingle().clear();
			containingEntity1.getContainedEmbeddedSingle().setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_embeddedSingle() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInEmbeddedSingle( "initialValue" );
			containingEntity1.setContainedEmbeddedSingle( new SingleContainingEmbeddable( containedEntity1 ) );
			containedEntity1.getContainingAsEmbeddedSingle().add( containingEntity1 );

			session.persist( containedEntity1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInEmbeddedSingle", "initialValue" )
											)
									)
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInEmbeddedSingle( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInEmbeddedSingle", "updatedValue" )
											)
									)
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the @IndexedEmbedded
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectEmbeddedUpdate_embeddedList() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInEmbeddedList( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedEmbeddedList( new ListContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsEmbeddedList().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "initialValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInEmbeddedList( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedList().getContainedList().get( 0 ).getContainingAsEmbeddedList().clear();
			containingEntity1.setContainedEmbeddedList( new ListContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsEmbeddedList().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "updatedValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedList().getContainedList().get( 0 ).getContainingAsEmbeddedList().clear();
			containingEntity1.setContainedEmbeddedList( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> {
								b2.objectField( "containedEmbeddedSingle", b3 -> { } );
								b2.objectField( "containedBidirectionalEmbedded", b3 -> { } );
								b2.objectField( "containedEmbeddedList", b3 -> { } );
							} )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_embeddedList() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInEmbeddedList( "firstValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedList().getContainedList().add( containedEntity );
			containedEntity.getContainingAsEmbeddedList().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "firstValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInEmbeddedList( "secondValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedEmbeddedList().getContainedList().add( containedEntity );
			containedEntity.getContainingAsEmbeddedList().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "firstValue" )
											)
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "secondValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			ContainedEntity containedEntity = containingEntity1.getContainedEmbeddedList().getContainedList().get( 0 );
			containedEntity.getContainingAsEmbeddedList().clear();
			containingEntity1.getContainedEmbeddedList().getContainedList().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "secondValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_embeddedList() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInEmbeddedList( "initialValue" );
			containingEntity1.setContainedEmbeddedList( new ListContainingEmbeddable( containedEntity1 ) );
			containedEntity1.getContainingAsEmbeddedList().add( containingEntity1 );

			session.persist( containedEntity1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "initialValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInEmbeddedList( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> b3
											.objectField( "containedList", b4 -> b4
													.field( "includedInEmbeddedList", "updatedValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the @IndexedEmbedded
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectEmbeddedUpdate_elementCollection() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInElementCollection( "firstValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedElementCollection().add( new SingleContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsElementCollection().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "firstValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInElementCollection( "secondValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedElementCollection().add( new SingleContainingEmbeddable( containedEntity ) );
			containedEntity.getContainingAsElementCollection().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "firstValue" )
											)
									)
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "secondValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			SingleContainingEmbeddable embeddable = containingEntity1.getContainedElementCollection().get( 0 );
			embeddable.getContainedSingle().getContainingAsElementCollection().clear();
			containingEntity1.getContainedElementCollection().remove( embeddable );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "secondValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_elementCollection() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInElementCollection( "initialValue" );
			containingEntity1.getContainedElementCollection().add( new SingleContainingEmbeddable() );
			containedEntity.getContainingAsElementCollection().add( containingEntity1 );

			session.persist( containedEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInElementCollection( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			SingleContainingEmbeddable embedded;
			if ( containingEntity1.getContainedElementCollection().isEmpty() ) { // Hibernate ORM 5
				embedded = new SingleContainingEmbeddable();
				containingEntity1.getContainedElementCollection().add( embedded );
			}
			else { // Hibernate ORM 6
				embedded = containingEntity1.getContainedElementCollection().get( 0 );
				if ( embedded == null ) {
					embedded = new SingleContainingEmbeddable();
					containingEntity1.getContainedElementCollection().set( 0, embedded );
				}
			}
			embedded.setContainedSingle( containedEntity );
			containedEntity.getContainingAsElementCollection().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "initialValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInElementCollection( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			SingleContainingEmbeddable embeddable = containingEntity1.getContainedElementCollection().get( 0 );
			embeddable.getContainedSingle().getContainingAsElementCollection().clear();
			embeddable.setContainedSingle( containedEntity );
			containedEntity.getContainingAsElementCollection().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
												.field( "includedInElementCollection", "updatedValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			SingleContainingEmbeddable embeddable = containingEntity1.getContainedElementCollection().get( 0 );
			embeddable.getContainedSingle().getContainingAsElementCollection().clear();
			embeddable.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_elementCollection() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInElementCollection( "initialValue" );
			containingEntity1.getContainedElementCollection().add( new SingleContainingEmbeddable( containedEntity1 ) );
			containedEntity1.getContainingAsElementCollection().add( containingEntity1 );

			session.persist( containedEntity1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "initialValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInElementCollection( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedElementCollection", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInElementCollection", "updatedValue" )
											)
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the @IndexedEmbedded
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_inverseSideEmbedded() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInContainedSingleWithInverseSideEmbedded( "initialValue" );
			containingEntity1.setContainedSingleWithInverseSideEmbedded( containedEntity );
			containedEntity.setContainingAsSingleWithInverseSideEmbedded( new InverseSideEmbeddable( containingEntity1 ) );

			session.persist( containedEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedSingleWithInverseSideEmbedded", b3 -> b3
											.field( "includedInContainedSingleWithInverseSideEmbedded", "initialValue" )
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInContainedSingleWithInverseSideEmbedded( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingleWithInverseSideEmbedded()
					.getContainingAsSingleWithInverseSideEmbedded().setContainingAsSingle( null );
			// Clear the one-to-one association and flush it first, to avoid problems with unique key constraints. See details in HHH-15767
			containingEntity1.setContainedSingleWithInverseSideEmbedded( null );

			if ( setupHolder.areEntitiesProcessedInSession() ) {
				backendMock.expectWorks( IndexedEntity.INDEX )
						.addOrUpdate( "1", b -> b
								.objectField( "child", b2 -> b2
										.objectField( "containedEmbeddedSingle", b3 -> {
										} )
										.objectField( "containedEmbeddedList", b3 -> {
										} )
										.objectField( "containedBidirectionalEmbedded", b3 -> {
										} )
								)
						);
			}
			session.flush();

			containingEntity1.setContainedSingleWithInverseSideEmbedded( containedEntity );
			containedEntity.setContainingAsSingleWithInverseSideEmbedded( new InverseSideEmbeddable( containingEntity1 ) );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedSingleWithInverseSideEmbedded", b3 -> b3
											.field( "includedInContainedSingleWithInverseSideEmbedded", "updatedValue" )
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingleWithInverseSideEmbedded()
					.getContainingAsSingleWithInverseSideEmbedded().setContainingAsSingle( null );
			containingEntity1.setContainedSingleWithInverseSideEmbedded( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_inverseSideEmbedded() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInContainedSingleWithInverseSideEmbedded( "initialValue" );
			containingEntity1.setContainedSingleWithInverseSideEmbedded( containedEntity1 );
			containedEntity1.setContainingAsSingleWithInverseSideEmbedded( new InverseSideEmbeddable( containingEntity1 ) );

			session.persist( containedEntity1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedSingleWithInverseSideEmbedded", b3 -> b3
											.field( "includedInContainedSingleWithInverseSideEmbedded", "initialValue" )
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInContainedSingleWithInverseSideEmbedded( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedSingleWithInverseSideEmbedded", b3 -> b3
											.field( "includedInContainedSingleWithInverseSideEmbedded", "updatedValue" )
									)
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the @IndexedEmbedded
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectEmbeddedUpdate_bidirectionalEmbedded() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInBidirectionalEmbedded( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedBidirectionalEmbedded( new BidirectionalEmbeddable( containedEntity ) );
			containedEntity.setContainingAsBidirectionalEmbedded( new InverseSideBidirectionalEmbeddable( containingEntity1 ) );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInBidirectionalEmbedded", "initialValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInBidirectionalEmbedded( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedBidirectionalEmbedded().getContainedSingle()
					.getContainingAsBidirectionalEmbedded().setContainingAsSingle( null );
			containingEntity1.setContainedBidirectionalEmbedded( new BidirectionalEmbeddable( containedEntity ) );
			containedEntity.setContainingAsBidirectionalEmbedded( new InverseSideBidirectionalEmbeddable( containingEntity1 ) );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInBidirectionalEmbedded", "updatedValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedBidirectionalEmbedded().getContainedSingle()
					.getContainingAsBidirectionalEmbedded().setContainingAsSingle( null );
			containingEntity1.setContainedBidirectionalEmbedded( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> {
								b2.objectField( "containedEmbeddedSingle", b3 -> { } );
								b2.objectField( "containedEmbeddedList", b3 -> { } );
								b2.objectField( "containedBidirectionalEmbedded", b3 -> { } );
							} )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_bidirectionalEmbedded() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInBidirectionalEmbedded( "initialValue" );
			containingEntity1.setContainedBidirectionalEmbedded( new BidirectionalEmbeddable( containedEntity ) );
			containedEntity.setContainingAsBidirectionalEmbedded( new InverseSideBidirectionalEmbeddable( containingEntity1 ) );

			session.persist( containedEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInBidirectionalEmbedded", "initialValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInBidirectionalEmbedded( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedBidirectionalEmbedded().getContainedSingle()
					.getContainingAsBidirectionalEmbedded().setContainingAsSingle( null );
			containingEntity1.getContainedBidirectionalEmbedded().setContainedSingle( containedEntity );
			containedEntity.setContainingAsBidirectionalEmbedded( new InverseSideBidirectionalEmbeddable( containingEntity1 ) );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInBidirectionalEmbedded", "updatedValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		setupHolder.runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedBidirectionalEmbedded().getContainedSingle()
					.getContainingAsBidirectionalEmbedded().setContainingAsSingle( null );
			containingEntity1.getContainedBidirectionalEmbedded().setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> { } )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_bidirectionalEmbedded() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInBidirectionalEmbedded( "initialValue" );
			containingEntity1.setContainedBidirectionalEmbedded( new BidirectionalEmbeddable( containedEntity1 ) );
			containedEntity1.setContainingAsBidirectionalEmbedded( new InverseSideBidirectionalEmbeddable( containingEntity1 ) );

			session.persist( containedEntity1 );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInBidirectionalEmbedded", "initialValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInBidirectionalEmbedded( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedEmbeddedSingle", b3 -> { } )
									.objectField( "containedEmbeddedList", b3 -> { } )
									.objectField( "containedBidirectionalEmbedded", b3 -> b3
											.objectField( "containedSingle", b4 -> b4
													.field( "includedInBidirectionalEmbedded", "updatedValue" )
											)
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the @IndexedEmbedded
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private IndexedEntity parent;

		@Embedded
		@IndexedEmbedded(includePaths = "containedSingle.includedInEmbeddedSingle")
		@Access(AccessType.PROPERTY)
		private SingleContainingEmbeddable containedEmbeddedSingle = new SingleContainingEmbeddable();

		@Embedded
		@IndexedEmbedded(includePaths = "containedList.includedInEmbeddedList")
		@AssociationOverride(
				name = "containedList",
				joinTable = @JoinTable(name = "containing_embeddedList", inverseJoinColumns = @JoinColumn(name = "CEList_containedList"))
		)
		@Access(AccessType.PROPERTY)
		private ListContainingEmbeddable containedEmbeddedList = new ListContainingEmbeddable();

		@ElementCollection
		@OrderBy("containedSingle.id asc") // Make sure the iteration order is predictable
		@JoinTable(name = "containing_elementCollection")
		@AssociationOverride(
				name = "containedSingle",
				/*
				 * For some reason Hibernate ORM sets the column name to
				 * "containedElementCollection_collection&&element_containedSingle_id".
				 * Let's work around the problem...
				 */
				joinColumns = @JoinColumn(name = "ec_containedSingle")
		)
		@IndexedEmbedded(includePaths = "containedSingle.includedInElementCollection")
		private List<SingleContainingEmbeddable> containedElementCollection = new ArrayList<>();

		@OneToOne
		@JoinColumn(name = "singleWithInverse")
		@IndexedEmbedded(includePaths = "includedInContainedSingleWithInverseSideEmbedded")
		private ContainedEntity containedSingleWithInverseSideEmbedded;

		@Embedded
		@IndexedEmbedded(includePaths = "containedSingle.includedInBidirectionalEmbedded")
		@Access(AccessType.PROPERTY)
		private BidirectionalEmbeddable containedBidirectionalEmbedded = new BidirectionalEmbeddable();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getParent() {
			return parent;
		}

		public void setParent(IndexedEntity parent) {
			this.parent = parent;
		}

		public SingleContainingEmbeddable getContainedEmbeddedSingle() {
			if ( containedEmbeddedSingle == null ) {
				containedEmbeddedSingle = new SingleContainingEmbeddable();
			}
			return containedEmbeddedSingle;
		}

		public void setContainedEmbeddedSingle(SingleContainingEmbeddable containedEmbeddedSingle) {
			this.containedEmbeddedSingle = containedEmbeddedSingle;
		}

		public ListContainingEmbeddable getContainedEmbeddedList() {
			if ( containedEmbeddedList == null ) {
				containedEmbeddedList = new ListContainingEmbeddable();
			}
			return containedEmbeddedList;
		}

		public void setContainedEmbeddedList(ListContainingEmbeddable containedEmbeddedList) {
			this.containedEmbeddedList = containedEmbeddedList;
		}

		public List<SingleContainingEmbeddable> getContainedElementCollection() {
			return containedElementCollection;
		}

		public ContainedEntity getContainedSingleWithInverseSideEmbedded() {
			return containedSingleWithInverseSideEmbedded;
		}

		public void setContainedSingleWithInverseSideEmbedded(ContainedEntity containedSingleWithInverseSideEmbedded) {
			this.containedSingleWithInverseSideEmbedded = containedSingleWithInverseSideEmbedded;
		}

		public BidirectionalEmbeddable getContainedBidirectionalEmbedded() {
			if ( containedBidirectionalEmbedded == null ) {
				containedBidirectionalEmbedded = new BidirectionalEmbeddable();
			}
			return containedBidirectionalEmbedded;
		}

		public void setContainedBidirectionalEmbedded(BidirectionalEmbeddable containedBidirectionalEmbedded) {
			this.containedBidirectionalEmbedded = containedBidirectionalEmbedded;
		}
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedEmbeddedSingle.containedSingle.includedInEmbeddedSingle",
				"containedEmbeddedList.containedList.includedInEmbeddedList",
				"containedElementCollection.containedSingle.includedInElementCollection",
				"containedBidirectionalEmbedded.containedSingle.includedInBidirectionalEmbedded",
				"containedSingleWithInverseSideEmbedded.includedInContainedSingleWithInverseSideEmbedded"
		})
		private ContainingEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedEmbeddedSingle.containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsEmbeddedSingle = new ArrayList<>();

		@ManyToMany(mappedBy = "containedEmbeddedList.containedList")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsEmbeddedList = new ArrayList<>();

		/*
		 * No mappedBy here: the other side is configured as a ManyToOne because the property is single-valued,
		 * but it is actually a ManyToMany: there are multiple elements in the elementCollection,
		 * meaning we can target multiple entities from a single entity.
		 * Anyway, we can't use mappedBy in this specific case.
		 */
		@ManyToMany
		@JoinTable(name = "contained_containingEC", inverseJoinColumns = { @JoinColumn( name = "containingEC_id" ) })
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath({
						@PropertyValue(propertyName = "containedElementCollection"),
						@PropertyValue(propertyName = "containedSingle")
				})
		)
		private List<ContainingEntity> containingAsElementCollection = new ArrayList<>();

		@Embedded
		@Access(AccessType.PROPERTY)
		private InverseSideEmbeddable containingAsSingleWithInverseSideEmbedded = new InverseSideEmbeddable();

		@Embedded
		@Access(AccessType.PROPERTY)
		private InverseSideBidirectionalEmbeddable containingAsBidirectionalEmbedded = new InverseSideBidirectionalEmbeddable();

		@Basic
		@GenericField
		private String includedInEmbeddedSingle;

		@Basic
		@GenericField
		private String includedInEmbeddedList;

		@Basic
		@GenericField
		private String includedInElementCollection;

		@Basic
		@Column(name = "inContainedSingle")
		@GenericField
		private String includedInContainedSingleWithInverseSideEmbedded;

		@Basic
		@Column(name = "inBidirectionalEmbedded")
		@GenericField
		private String includedInBidirectionalEmbedded;

		@Basic
		@GenericField
		private String excludedFromAll;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsEmbeddedSingle() {
			return containingAsEmbeddedSingle;
		}

		public List<ContainingEntity> getContainingAsEmbeddedList() {
			return containingAsEmbeddedList;
		}

		public List<ContainingEntity> getContainingAsElementCollection() {
			return containingAsElementCollection;
		}

		public InverseSideEmbeddable getContainingAsSingleWithInverseSideEmbedded() {
			if ( containingAsSingleWithInverseSideEmbedded == null ) {
				containingAsSingleWithInverseSideEmbedded = new InverseSideEmbeddable();
			}
			return containingAsSingleWithInverseSideEmbedded;
		}

		public void setContainingAsSingleWithInverseSideEmbedded(
				InverseSideEmbeddable containingAsSingleWithInverseSideEmbedded) {
			this.containingAsSingleWithInverseSideEmbedded = containingAsSingleWithInverseSideEmbedded;
		}

		public InverseSideBidirectionalEmbeddable getContainingAsBidirectionalEmbedded() {
			if ( containingAsBidirectionalEmbedded == null ) {
				containingAsBidirectionalEmbedded = new InverseSideBidirectionalEmbeddable();
			}
			return containingAsBidirectionalEmbedded;
		}

		public void setContainingAsBidirectionalEmbedded(InverseSideBidirectionalEmbeddable containingAsBidirectionalEmbedded) {
			this.containingAsBidirectionalEmbedded = containingAsBidirectionalEmbedded;
		}

		public String getIncludedInEmbeddedSingle() {
			return includedInEmbeddedSingle;
		}

		public void setIncludedInEmbeddedSingle(String includedInEmbeddedSingle) {
			this.includedInEmbeddedSingle = includedInEmbeddedSingle;
		}

		public String getIncludedInEmbeddedList() {
			return includedInEmbeddedList;
		}

		public void setIncludedInEmbeddedList(String includedInEmbeddedList) {
			this.includedInEmbeddedList = includedInEmbeddedList;
		}

		public String getIncludedInElementCollection() {
			return includedInElementCollection;
		}

		public void setIncludedInElementCollection(String includedInElementCollection) {
			this.includedInElementCollection = includedInElementCollection;
		}

		public String getIncludedInContainedSingleWithInverseSideEmbedded() {
			return includedInContainedSingleWithInverseSideEmbedded;
		}

		public void setIncludedInContainedSingleWithInverseSideEmbedded(
				String includedInContainedSingleWithInverseSideEmbedded) {
			this.includedInContainedSingleWithInverseSideEmbedded = includedInContainedSingleWithInverseSideEmbedded;
		}

		public String getIncludedInBidirectionalEmbedded() {
			return includedInBidirectionalEmbedded;
		}

		public void setIncludedInBidirectionalEmbedded(String includedInBidirectionalEmbedded) {
			this.includedInBidirectionalEmbedded = includedInBidirectionalEmbedded;
		}

		public String getExcludedFromAll() {
			return excludedFromAll;
		}

		public void setExcludedFromAll(String excludedFromAll) {
			this.excludedFromAll = excludedFromAll;
		}
	}

	@Embeddable
	@Access(AccessType.FIELD)
	public static class SingleContainingEmbeddable {

		@ManyToOne
		@JoinColumn(name = "emb_containedSingle")
		@IndexedEmbedded
		private ContainedEntity containedSingle;

		public SingleContainingEmbeddable() {
		}

		public SingleContainingEmbeddable(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}
	}

	@Embeddable
	@Access(AccessType.FIELD)
	public static class ListContainingEmbeddable {

		@ManyToMany
		@IndexedEmbedded
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainedEntity> containedList = new ArrayList<>();

		protected ListContainingEmbeddable() {
			// For Hibernate ORM
		}

		public ListContainingEmbeddable(ContainedEntity containedEntity) {
			containedList.add( containedEntity );
		}

		public List<ContainedEntity> getContainedList() {
			return containedList;
		}
	}

	@Embeddable
	@Access(AccessType.FIELD)
	public static class InverseSideEmbeddable {
		/*
		 * This association must not use "mappedBy".
		 * This is because we want to test defining the "association mapping" in an embeddable on the inverse side
		 * without relying on Hibernate ORM.
		 * This has specific implications internally, as the embeddable type
		 * will not have been discovered when we start building reindexing resolvers,
		 * and we used to expect all types to have been discovered at that point in order
		 * to know the full list of entity types.
		 * Now we allow new types to be discovered while building reindexing resolvers
		 * (though we still expect all entity types to have been discovered),
		 * so there should be no problem, but this test should prevent any regression.
		 */
		@OneToOne
		@JoinColumn(name = "containing")
		@AssociationInverseSide(
				inversePath = @ObjectPath(
						@PropertyValue(propertyName = "containedSingleWithInverseSideEmbedded")
				)
		)
		private ContainingEntity containingAsSingle;

		public InverseSideEmbeddable() {
		}

		public InverseSideEmbeddable(ContainingEntity containingAsSingle) {
			this.containingAsSingle = containingAsSingle;
		}

		public ContainingEntity getContainingAsSingle() {
			return containingAsSingle;
		}

		public void setContainingAsSingle(ContainingEntity containingAsSingle) {
			this.containingAsSingle = containingAsSingle;
		}
	}

	@Embeddable
	@Access(AccessType.FIELD)
	public static class BidirectionalEmbeddable {

		@OneToOne
		@JoinColumn(name = "biDir_containedSingle")
		@IndexedEmbedded
		private ContainedEntity containedSingle;

		public BidirectionalEmbeddable() {
		}

		public BidirectionalEmbeddable(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}
	}

	@Embeddable
	@Access(AccessType.FIELD)
	public static class InverseSideBidirectionalEmbeddable {

		@OneToOne(mappedBy = "containedBidirectionalEmbedded.containedSingle")
		private ContainingEntity containingAsSingle;

		public InverseSideBidirectionalEmbeddable() {
		}

		public InverseSideBidirectionalEmbeddable(ContainingEntity containingAsSingle) {
			this.containingAsSingle = containingAsSingle;
		}

		public ContainingEntity getContainingAsSingle() {
			return containingAsSingle;
		}

		public void setContainingAsSingle(ContainingEntity containingAsSingle) {
			this.containingAsSingle = containingAsSingle;
		}
	}

}
