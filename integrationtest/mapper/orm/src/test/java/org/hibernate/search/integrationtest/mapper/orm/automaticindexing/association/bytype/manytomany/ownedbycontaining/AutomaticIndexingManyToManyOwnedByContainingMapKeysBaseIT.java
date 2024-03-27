/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.manytomany.ownedbycontaining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Transient;

import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.AbstractAutomaticIndexingMultiValuedAssociationBaseIT;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.ContainerPrimitives;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.MultiValuedPropertyAccessor;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association.bytype.accessor.PropertyAccessor;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

/**
 * Test automatic indexing caused by multi-valued association updates
 * or by updates of associated (contained) entities,
 * with a {@code @ManyToMany Map} association owned by the containing side
 * where associated entities are map keys.
 */
public class AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT
		extends AbstractAutomaticIndexingMultiValuedAssociationBaseIT<
				AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.IndexedEntity,
				AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainingEntity,
				AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainingEmbeddable,
				AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainedEntity,
				AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainedEmbeddable,
				Map<AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT.ContainedEntity, String>> {

	private static final ContainerPrimitives<Map<ContainedEntity, String>, ContainedEntity> MAP_KEYS_PRIMITIVES =
			ContainerPrimitives.mapKeys( containedEntity -> containedEntity.getIndexedField() + "_value" );

	public AutomaticIndexingManyToManyOwnedByContainingMapKeysBaseIT() {
		super( IndexedEntity.PRIMITIVES, ContainingEntity.PRIMITIVES, ContainingEmbeddable.PRIMITIVES,
				ContainedEntity.PRIMITIVES, ContainedEmbeddable.PRIMITIVES );
	}

	@Override
	protected boolean isAssociationMultiValuedOnContainedSide() {
		return true;
	}

	@Override
	protected boolean isAssociationOwnedByContainedSide() {
		return false;
	}

	@Override
	protected OrmSetupHelper.SetupContext additionalSetup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.dataClearing( config -> config.preClear( ContainedEntity.class, contained -> {
			contained.getContainingAsIndexedEmbedded().clear();
			contained.getContainingAsNonIndexedEmbedded().clear();
			contained.getContainingAsIndexedEmbeddedShallowReindexOnUpdate().clear();
			contained.getContainingAsIndexedEmbeddedNoReindexOnUpdate().clear();
			contained.getContainingAsUsedInCrossEntityDerivedProperty().clear();
			contained.getContainingAsIndexedEmbeddedWithCast().clear();
			ContainedEmbeddable embeddedAssociations = contained.getEmbeddedAssociations();
			if ( embeddedAssociations != null ) {
				embeddedAssociations.getContainingAsIndexedEmbedded().clear();
				embeddedAssociations.getContainingAsNonIndexedEmbedded().clear();
			}
		} ) );
		return setupContext;
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		private String nonIndexedField;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedIndexedEmbedded.indexedField",
				"containedIndexedEmbedded.indexedElementCollectionField",
				"containedIndexedEmbedded.containedDerivedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedShallowReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedField",
				"containedIndexedEmbeddedNoReindexOnUpdate.indexedElementCollectionField",
				"containedIndexedEmbeddedNoReindexOnUpdate.containedDerivedField",
				"containedIndexedEmbeddedWithCast.indexedField",
				"embeddedAssociations.containedIndexedEmbedded.indexedField",
				"embeddedAssociations.containedIndexedEmbedded.indexedElementCollectionField",
				"embeddedAssociations.containedIndexedEmbedded.containedDerivedField",
				"crossEntityDerivedField"
		})
		private ContainingEntity child;

		@ElementCollection
		@JoinTable(
				name = "i_containedIndexedEmbedded",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_containedNonIndexedEmbedded",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedNonIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_indexedEmbeddedShallow",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.SHALLOW,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbeddedShallowReindexOnUpdate = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_indexedEmbeddedNoReindex",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		@IndexingDependency(
				reindexOnUpdate = ReindexOnUpdate.NO,
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbeddedNoReindexOnUpdate = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_containedCrossEntityDP",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedUsedInCrossEntityDerivedProperty = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_containedIndexedEmbeddedCast",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyClass(ContainedEntity.class)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY),
				targetType = ContainedEntity.class
		)
		private Map<Object, String> containedIndexedEmbeddedWithCast = new LinkedHashMap<>();

		@IndexedEmbedded
		@Embedded
		private ContainingEmbeddable embeddedAssociations;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Map<ContainedEntity, String> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(Map<ContainedEntity, String> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbeddedShallowReindexOnUpdate() {
			return containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedShallowReindexOnUpdate(
				Map<ContainedEntity, String> containedIndexedEmbeddedShallowReindexOnUpdate) {
			this.containedIndexedEmbeddedShallowReindexOnUpdate = containedIndexedEmbeddedShallowReindexOnUpdate;
		}

		public Map<ContainedEntity, String> getContainedIndexedEmbeddedNoReindexOnUpdate() {
			return containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public void setContainedIndexedEmbeddedNoReindexOnUpdate(
				Map<ContainedEntity, String> containedIndexedEmbeddedNoReindexOnUpdate) {
			this.containedIndexedEmbeddedNoReindexOnUpdate = containedIndexedEmbeddedNoReindexOnUpdate;
		}

		public Map<ContainedEntity, String> getContainedUsedInCrossEntityDerivedProperty() {
			return containedUsedInCrossEntityDerivedProperty;
		}

		public Map<Object, String> getContainedIndexedEmbeddedWithCast() {
			return containedIndexedEmbeddedWithCast;
		}

		public ContainingEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainingEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath({
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField1")
				}),
				@ObjectPath({
						@PropertyValue(
								propertyName = "containedUsedInCrossEntityDerivedProperty",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						),
						@PropertyValue(propertyName = "fieldUsedInCrossEntityDerivedField2")
				})
		})
		public Optional<String> getCrossEntityDerivedField() {
			return AbstractAutomaticIndexingAssociationBaseIT.computeDerived(
					containedUsedInCrossEntityDerivedProperty.keySet().stream().flatMap( c -> Stream.of(
							c.getFieldUsedInCrossEntityDerivedField1(),
							c.getFieldUsedInCrossEntityDerivedField2()
					) )
			);
		}

		static final ContainingEntityPrimitives<ContainingEntity,
				ContainingEmbeddable,
				ContainedEntity,
				Map<ContainedEntity, String>> PRIMITIVES = new ContainingEntityPrimitives<ContainingEntity,
						ContainingEmbeddable,
						ContainedEntity,
						Map<ContainedEntity, String>>() {
					@Override
					public Class<ContainingEntity> entityClass() {
						return ContainingEntity.class;
					}

					@Override
					public ContainingEntity newInstance(int id) {
						ContainingEntity entity = new ContainingEntity();
						entity.setId( id );
						return entity;
					}

					@Override
					public PropertyAccessor<ContainingEntity, ContainingEntity> child() {
						return PropertyAccessor.create( ContainingEntity::setChild );
					}

					@Override
					public PropertyAccessor<ContainingEntity, ContainingEntity> parent() {
						return PropertyAccessor.create( ContainingEntity::setParent );
					}

					@Override
					public Map<ContainedEntity, String> newContainedAssociation(Map<ContainedEntity, String> original) {
						return new LinkedHashMap<>( original );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainingEntity,
							ContainedEntity,
							Map<ContainedEntity, String>> containedIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEntity::getContainedIndexedEmbedded, ContainingEntity::setContainedIndexedEmbedded );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainingEntity,
							ContainedEntity,
							Map<ContainedEntity, String>> containedNonIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEntity::getContainedNonIndexedEmbedded,
								ContainingEntity::setContainedNonIndexedEmbedded );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainingEntity,
							ContainedEntity,
							Map<ContainedEntity, String>> containedIndexedEmbeddedShallowReindexOnUpdate() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEntity::getContainedIndexedEmbeddedShallowReindexOnUpdate,
								ContainingEntity::setContainedIndexedEmbeddedShallowReindexOnUpdate );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainingEntity,
							ContainedEntity,
							Map<ContainedEntity, String>> containedIndexedEmbeddedNoReindexOnUpdate() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEntity::getContainedIndexedEmbeddedNoReindexOnUpdate,
								ContainingEntity::setContainedIndexedEmbeddedNoReindexOnUpdate );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainingEntity,
							ContainedEntity,
							Map<ContainedEntity, String>> containedUsedInCrossEntityDerivedProperty() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEntity::getContainedUsedInCrossEntityDerivedProperty );
					}

					@Override
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public MultiValuedPropertyAccessor<ContainingEntity,
							ContainedEntity,
							Map<ContainedEntity, String>> containedIndexedEmbeddedWithCast() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								root -> (Map) root.getContainedIndexedEmbeddedWithCast() );
					}

					@Override
					public PropertyAccessor<ContainingEntity, ContainingEmbeddable> embeddedAssociations() {
						return PropertyAccessor.create( ContainingEntity::setEmbeddedAssociations,
								ContainingEntity::getEmbeddedAssociations );
					}

					@Override
					public PropertyAccessor<ContainingEntity, String> nonIndexedField() {
						return PropertyAccessor.create( ContainingEntity::setNonIndexedField );
					}
				};
	}


	public static class ContainingEmbeddable {

		@ElementCollection
		@JoinTable(
				name = "i_emb_containedIdxEmbedded",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = { "indexedField", "indexedElementCollectionField", "containedDerivedField" },
				extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
		)
		private Map<ContainedEntity, String> containedIndexedEmbedded = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "i_emb_containedNonIdxEmbedded",
				joinColumns = @JoinColumn(name = "containing")
		)
		@MapKeyJoinColumn(name = "contained")
		@Column(name = "\"value\"")
		@OrderBy("contained asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedNonIndexedEmbedded = new LinkedHashMap<>();

		public Map<ContainedEntity, String> getContainedIndexedEmbedded() {
			return containedIndexedEmbedded;
		}

		public void setContainedIndexedEmbedded(Map<ContainedEntity, String> containedIndexedEmbedded) {
			this.containedIndexedEmbedded = containedIndexedEmbedded;
		}

		public Map<ContainedEntity, String> getContainedNonIndexedEmbedded() {
			return containedNonIndexedEmbedded;
		}

		public void setContainedNonIndexedEmbedded(Map<ContainedEntity, String> containedNonIndexedEmbedded) {
			this.containedNonIndexedEmbedded = containedNonIndexedEmbedded;
		}

		static final ContainingEmbeddablePrimitives<ContainingEmbeddable,
				ContainedEntity,
				Map<ContainedEntity, String>> PRIMITIVES = new ContainingEmbeddablePrimitives<ContainingEmbeddable,
						ContainedEntity,
						Map<ContainedEntity, String>>() {
					@Override
					public ContainingEmbeddable newInstance() {
						return new ContainingEmbeddable();
					}


					@Override
					public MultiValuedPropertyAccessor<ContainingEmbeddable,
							ContainedEntity,
							Map<ContainedEntity, String>> containedIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEmbeddable::getContainedIndexedEmbedded,
								ContainingEmbeddable::setContainedIndexedEmbedded );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainingEmbeddable,
							ContainedEntity,
							Map<ContainedEntity, String>> containedNonIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( MAP_KEYS_PRIMITIVES,
								ContainingEmbeddable::getContainedNonIndexedEmbedded,
								ContainingEmbeddable::setContainedNonIndexedEmbedded );
					}
				};
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

		static final IndexedEntityPrimitives<IndexedEntity> PRIMITIVES = new IndexedEntityPrimitives<IndexedEntity>() {
			@Override
			public Class<IndexedEntity> entityClass() {
				return IndexedEntity.class;
			}

			@Override
			public String indexName() {
				return IndexedEntity.INDEX;
			}

			@Override
			public IndexedEntity newInstance(int id) {
				IndexedEntity entity = new IndexedEntity();
				entity.setId( id );
				return entity;
			}
		};
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indEmd",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
					@PropertyValue(
							propertyName = "containedIndexedEmbedded",
							extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
					)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_nonIndEmd",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indEmdShallow",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
					@PropertyValue(
							propertyName = "containedIndexedEmbeddedShallowReindexOnUpdate",
							extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
					)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbeddedShallowReindexOnUpdate = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_indEmdNoReindex",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
					@PropertyValue(
							propertyName = "containedIndexedEmbeddedNoReindexOnUpdate",
							extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
					)
				)
		)
		private List<ContainingEntity> containingAsIndexedEmbeddedNoReindexOnUpdate = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_usedInCEDP",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
					@PropertyValue(
							propertyName = "containedUsedInCrossEntityDerivedProperty",
							extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
					)
				)
		)
		private List<ContainingEntity> containingAsUsedInCrossEntityDerivedProperty = new ArrayList<>();

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany(targetEntity = ContainingEntity.class)
		@JoinTable(name = "contained_indEmdWithCast",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath(
					@PropertyValue(
							propertyName = "containedIndexedEmbeddedWithCast",
							extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
					)
				)
		)
		private List<Object> containingAsIndexedEmbeddedWithCast = new ArrayList<>();

		@Embedded
		private ContainedEmbeddable embeddedAssociations;

		@Basic
		@GenericField
		private String indexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "contained_IElementCF")
		@GenericField
		private List<String> indexedElementCollectionField = new ArrayList<>();

		@Basic
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private String nonIndexedField;

		@ElementCollection
		@OrderColumn(name = "idx")
		@CollectionTable(name = "nonIndexedECF")
		@Column(name = "nonIndexed")
		@GenericField
		// Keep this annotation, it should be ignored because the field is not included in the @IndexedEmbedded
		private List<String> nonIndexedElementCollectionField = new ArrayList<>();

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF1")
		private String fieldUsedInContainedDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUIContainedDF2")
		private String fieldUsedInContainedDerivedField2;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF1")
		private String fieldUsedInCrossEntityDerivedField1;

		@Basic // Do not annotate with @GenericField, this would make the test pointless
		@Column(name = "FUICrossEntityDF2")
		private String fieldUsedInCrossEntityDerivedField2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbeddedShallowReindexOnUpdate() {
			return containingAsIndexedEmbeddedShallowReindexOnUpdate;
		}

		public List<ContainingEntity> getContainingAsIndexedEmbeddedNoReindexOnUpdate() {
			return containingAsIndexedEmbeddedNoReindexOnUpdate;
		}

		public List<ContainingEntity> getContainingAsUsedInCrossEntityDerivedProperty() {
			return containingAsUsedInCrossEntityDerivedProperty;
		}

		public List<Object> getContainingAsIndexedEmbeddedWithCast() {
			return containingAsIndexedEmbeddedWithCast;
		}

		public ContainedEmbeddable getEmbeddedAssociations() {
			return embeddedAssociations;
		}

		public void setEmbeddedAssociations(ContainedEmbeddable embeddedAssociations) {
			this.embeddedAssociations = embeddedAssociations;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

		public List<String> getIndexedElementCollectionField() {
			return indexedElementCollectionField;
		}

		public void setIndexedElementCollectionField(List<String> indexedElementCollectionField) {
			this.indexedElementCollectionField = indexedElementCollectionField;
		}

		public String getNonIndexedField() {
			return nonIndexedField;
		}

		public void setNonIndexedField(String nonIndexedField) {
			this.nonIndexedField = nonIndexedField;
		}

		public List<String> getNonIndexedElementCollectionField() {
			return nonIndexedElementCollectionField;
		}

		public void setNonIndexedElementCollectionField(List<String> nonIndexedElementCollectionField) {
			this.nonIndexedElementCollectionField = nonIndexedElementCollectionField;
		}

		public String getFieldUsedInContainedDerivedField1() {
			return fieldUsedInContainedDerivedField1;
		}

		public void setFieldUsedInContainedDerivedField1(String fieldUsedInContainedDerivedField1) {
			this.fieldUsedInContainedDerivedField1 = fieldUsedInContainedDerivedField1;
		}

		public String getFieldUsedInContainedDerivedField2() {
			return fieldUsedInContainedDerivedField2;
		}

		public void setFieldUsedInContainedDerivedField2(String fieldUsedInContainedDerivedField2) {
			this.fieldUsedInContainedDerivedField2 = fieldUsedInContainedDerivedField2;
		}

		public String getFieldUsedInCrossEntityDerivedField1() {
			return fieldUsedInCrossEntityDerivedField1;
		}

		public void setFieldUsedInCrossEntityDerivedField1(String fieldUsedInCrossEntityDerivedField1) {
			this.fieldUsedInCrossEntityDerivedField1 = fieldUsedInCrossEntityDerivedField1;
		}

		public String getFieldUsedInCrossEntityDerivedField2() {
			return fieldUsedInCrossEntityDerivedField2;
		}

		public void setFieldUsedInCrossEntityDerivedField2(String fieldUsedInCrossEntityDerivedField2) {
			this.fieldUsedInCrossEntityDerivedField2 = fieldUsedInCrossEntityDerivedField2;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField1")),
				@ObjectPath(@PropertyValue(propertyName = "fieldUsedInContainedDerivedField2"))
		})
		public Optional<String> getContainedDerivedField() {
			return AbstractAutomaticIndexingAssociationBaseIT
					.computeDerived( Stream.of( fieldUsedInContainedDerivedField1, fieldUsedInContainedDerivedField2 ) );
		}

		static ContainedEntityPrimitives<ContainedEntity, ContainedEmbeddable, ContainingEntity> PRIMITIVES =
				new ContainedEntityPrimitives<ContainedEntity, ContainedEmbeddable, ContainingEntity>() {
					@Override
					public Class<ContainedEntity> entityClass() {
						return ContainedEntity.class;
					}

					@Override
					public ContainedEntity newInstance(int id) {
						ContainedEntity entity = new ContainedEntity();
						entity.setId( id );
						return entity;
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity,
							ContainingEntity,
							List<ContainingEntity>> containingAsIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getContainingAsIndexedEmbedded );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity,
							ContainingEntity,
							List<ContainingEntity>> containingAsNonIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getContainingAsNonIndexedEmbedded );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity,
							ContainingEntity,
							List<ContainingEntity>> containingAsIndexedEmbeddedShallowReindexOnUpdate() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getContainingAsIndexedEmbeddedShallowReindexOnUpdate );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity,
							ContainingEntity,
							List<ContainingEntity>> containingAsIndexedEmbeddedNoReindexOnUpdate() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getContainingAsIndexedEmbeddedNoReindexOnUpdate );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity,
							ContainingEntity,
							List<ContainingEntity>> containingAsUsedInCrossEntityDerivedProperty() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getContainingAsUsedInCrossEntityDerivedProperty );
					}

					@Override
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public MultiValuedPropertyAccessor<ContainedEntity,
							ContainingEntity,
							List<ContainingEntity>> containingAsIndexedEmbeddedWithCast() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								root -> (List) root.getContainingAsIndexedEmbeddedWithCast() );
					}

					@Override
					public PropertyAccessor<ContainedEntity, ContainedEmbeddable> embeddedAssociations() {
						return PropertyAccessor.create( ContainedEntity::setEmbeddedAssociations,
								ContainedEntity::getEmbeddedAssociations );
					}

					@Override
					public PropertyAccessor<ContainedEntity, String> indexedField() {
						return PropertyAccessor.create( ContainedEntity::setIndexedField );
					}

					@Override
					public PropertyAccessor<ContainedEntity, String> nonIndexedField() {
						return PropertyAccessor.create( ContainedEntity::setNonIndexedField );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity, String, List<String>> indexedElementCollectionField() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getIndexedElementCollectionField,
								ContainedEntity::setIndexedElementCollectionField );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEntity,
							String,
							List<String>> nonIndexedElementCollectionField() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEntity::getNonIndexedElementCollectionField,
								ContainedEntity::setNonIndexedElementCollectionField );
					}

					@Override
					public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField1() {
						return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField1 );
					}

					@Override
					public PropertyAccessor<ContainedEntity, String> fieldUsedInContainedDerivedField2() {
						return PropertyAccessor.create( ContainedEntity::setFieldUsedInContainedDerivedField2 );
					}

					@Override
					public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField1() {
						return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField1 );
					}

					@Override
					public PropertyAccessor<ContainedEntity, String> fieldUsedInCrossEntityDerivedField2() {
						return PropertyAccessor.create( ContainedEntity::setFieldUsedInCrossEntityDerivedField2 );
					}
				};
	}

	public static class ContainedEmbeddable {

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@JoinTable(name = "contained_emb_idxEmd",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @ObjectPath({
						@PropertyValue(
								propertyName = "embeddedAssociations"
						),
						@PropertyValue(
								propertyName = "containedIndexedEmbedded",
								extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_KEY)
						)
				})
		)
		private List<ContainingEntity> containingAsIndexedEmbedded = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_emb_nonIdxEmd",
				joinColumns = @JoinColumn(name = "contained"),
				inverseJoinColumns = @JoinColumn(name = "containing"))
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbedded = new ArrayList<>();

		public List<ContainingEntity> getContainingAsIndexedEmbedded() {
			return containingAsIndexedEmbedded;
		}

		public List<ContainingEntity> getContainingAsNonIndexedEmbedded() {
			return containingAsNonIndexedEmbedded;
		}

		static ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity> PRIMITIVES =
				new ContainedEmbeddablePrimitives<ContainedEmbeddable, ContainingEntity>() {
					@Override
					public ContainedEmbeddable newInstance() {
						return new ContainedEmbeddable();
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEmbeddable,
							ContainingEntity,
							List<ContainingEntity>> containingAsIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEmbeddable::getContainingAsIndexedEmbedded );
					}

					@Override
					public MultiValuedPropertyAccessor<ContainedEmbeddable,
							ContainingEntity,
							List<ContainingEntity>> containingAsNonIndexedEmbedded() {
						return MultiValuedPropertyAccessor.create( ContainerPrimitives.collection(),
								ContainedEmbeddable::getContainingAsNonIndexedEmbedded );
					}
				};
	}

}
