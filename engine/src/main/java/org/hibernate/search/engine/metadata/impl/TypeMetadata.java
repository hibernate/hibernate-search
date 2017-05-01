/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.document.Field;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.MutableAnalyzerRegistry;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Class containing all the meta data extracted for a single type ( and all classes in its hierarchy ).
 *
 * @author Hardy Ferentschik
 */
public class TypeMetadata {
	private static final Log log = LoggerFactory.make();
	private static final String COMPONENT_PATH_SEPARATOR = ".";

	/**
	 * The type for this metadata
	 */
	private final IndexedTypeIdentifier type;

	/**
	 * The class boost for this type (class level @Boost)
	 */
	private final float boost;

	/**
	 * Optional analyzer discriminator. There can only be one per class.
	 */
	private final Discriminator discriminator;

	/**
	 * {@code AnalyzerDiscriminator} can be defined on class level or on property. In the latter case this is the member
	 * on which it is defined.
	 */
	private final XMember discriminatorGetter;

	/**
	 * Optional dynamic boost strategy.
	 */
	private final BoostStrategy classBoostStrategy;

	/**
	 * Set of all Java property (field or getter) metadata instances
	 */
	private final Set<PropertyMetadata> propertyMetadata;

	/**
	 * Metadata for all document fields.
	 * <p>This does not include document fields from embedded types.
	 */
	private final Set<DocumentFieldMetadata> documentFieldMetadata;

	/**
	 * Metadata for a document field keyed against the field absolute name.
	 * <p>This does not include document fields from embedded types.
	 * <p><strong>Caution</strong>: this may not include every document metadata, most notably
	 * because document fields generated for class bridges may not have a name, and there may
	 * be multiple ones (so only the last one is present in the map).
	 */
	private final Map<String, DocumentFieldMetadata> documentFieldNameToFieldMetadata;

	/**
	 * Metadata for a bridge-defined field keyed against the field absolute name.
	 */
	private final Map<String, BridgeDefinedField> bridgeDefinedFieldNameToFieldMetadata;

	/**
	 * Metadata for a facet keyed against the facet field name
	 */
	private final Map<String, FacetMetadata> facetFieldNameToFacetMetadata;

	/**
	 * Metadata for a Java property (field or getter) keyed against the property name.
	 */
	private final Map<String, PropertyMetadata> propertyGetterNameToPropertyMetadata;

	/**
	 * The property metadata for the id getter. {@code null} for non indexed types ( indexed embedded only )
	 */
	private final PropertyMetadata idPropertyMetadata;

	/**
	 * Set of field meta data instances contributed by class bridges
	 */
	private final Set<DocumentFieldMetadata> classBridgeFields;

	/**
	 * Document field metadata keyed against the field absolute name.
	 * <p>These fields are contributed by class bridges.
	 */
	private final Map<String, DocumentFieldMetadata> classBridgeFieldNameToDocumentFieldMetadata;

	/**
	 * Metadata of embedded types ({@code @IndexedEmbedded})
	 */
	private final Set<EmbeddedTypeMetadata> embeddedTypeMetadata;

	/**
	 * List of contained in properties ({@code @ContainedIn})
	 */
	private final Set<ContainedInMetadata> containedInMetadata;

	/**
	 * The scoped analyzer reference for this entity
	 */
	private final ScopedAnalyzerReference scopedAnalyzerReference;

	/**
	 * Whether we can optimize the indexing work by inspecting the changed fields
	 */
	private boolean stateInspectionOptimizationsEnabled;

	/**
	 * Encountered property names of field, embedded and contained in collections. Used to optimize indexing based
	 * on ORM collection update events.
	 */
	private final Set<String> collectionRoles;

	/**
	 * Flag indicating whether the JPA @Id is used as document id. See {@link org.hibernate.search.engine.impl.WorkPlan}
	 */
	// TODO - would be nice to not need this in TypeMetadata (HF)
	private final boolean jpaIdUsedAsDocumentId;

	/**
	 * List of classes discovered during metadata processing for which collection event optimizations needs to be
	 * disabled.
	 */
	// TODO - would be nice to not need this in TypeMetadata (HF)
	private final Set<XClass> optimizationBlackList;

	private final Set<SortableFieldMetadata> classBridgeSortableFieldMetadata;

	protected TypeMetadata(Builder builder) {
		this.type = builder.indexedType;
		this.boost = builder.boost;
		this.scopedAnalyzerReference = builder.scopedAnalyzerReferenceBuilder == null ? null
				: builder.scopedAnalyzerReferenceBuilder.build();
		this.discriminator = builder.discriminator;
		this.discriminatorGetter = builder.discriminatorGetter;
		this.classBoostStrategy = builder.classBoostStrategy;
		this.stateInspectionOptimizationsEnabled = builder.stateInspectionOptimizationsEnabled;
		this.idPropertyMetadata = builder.idPropertyMetadata;
		this.embeddedTypeMetadata = Collections.unmodifiableSet( builder.embeddedTypeMetadata );
		this.containedInMetadata = Collections.unmodifiableSet( builder.containedInMetadata );
		this.optimizationBlackList = Collections.unmodifiableSet( builder.optimizationClassList );
		this.collectionRoles = Collections.unmodifiableSet( builder.collectionRoles );
		this.jpaIdUsedAsDocumentId = determineWhetherDocumentIdPropertyIsTheSameAsJpaIdProperty( builder.jpaProperty );
		this.classBridgeFields = Collections.unmodifiableSet( builder.classBridgeFields );
		this.propertyMetadata = Collections.unmodifiableSet( builder.propertyMetadataSet );
		this.propertyGetterNameToPropertyMetadata = buildPropertyMetadataMap( builder.propertyMetadataSet );
		this.documentFieldMetadata = collectFieldMetadata( builder.propertyMetadataSet, builder.classBridgeFields, builder.idPropertyMetadata );
		this.documentFieldNameToFieldMetadata = buildFieldMetadataMap( documentFieldMetadata );
		this.facetFieldNameToFacetMetadata = buildFacetMetadataMap( documentFieldMetadata );
		this.bridgeDefinedFieldNameToFieldMetadata = buildBridgeDefinedFieldMetadataMap( documentFieldNameToFieldMetadata.values() );
		this.classBridgeFieldNameToDocumentFieldMetadata = copyClassBridgeMetadata( builder.classBridgeFields );
		this.classBridgeSortableFieldMetadata = Collections.unmodifiableSet( builder.classBridgeSortableFieldMetadata );
	}

	public IndexedTypeIdentifier getType() {
		return type;
	}

	public Set<PropertyMetadata> getAllPropertyMetadata() {
		return propertyMetadata;
	}

	public PropertyMetadata getPropertyMetadataForProperty(String propertyName) {
		return propertyGetterNameToPropertyMetadata.get( propertyName );
	}

	public PropertyMetadata getIdPropertyMetadata() {
		return idPropertyMetadata;
	}

	public Set<DocumentFieldMetadata> getClassBridgeMetadata() {
		return classBridgeFields;
	}

	public Set<SortableFieldMetadata> getClassBridgeSortableFieldMetadata() {
		return classBridgeSortableFieldMetadata;
	}

	public DocumentFieldMetadata getDocumentFieldMetadataFor(String fieldName) {
		DocumentFieldMetadata result = documentFieldNameToFieldMetadata.get( fieldName );
		if ( result != null ) {
			return result;
		}
		for ( EmbeddedTypeMetadata element : embeddedTypeMetadata ) {
			result = element.getDocumentFieldMetadataFor( fieldName );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	public BridgeDefinedField getBridgeDefinedFieldMetadataFor(String fieldName) {
		BridgeDefinedField result = bridgeDefinedFieldNameToFieldMetadata.get( fieldName );
		if ( result != null ) {
			return result;
		}
		for ( EmbeddedTypeMetadata element : embeddedTypeMetadata ) {
			result = element.getBridgeDefinedFieldMetadataFor( fieldName );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	public FacetMetadata getFacetMetadataFor(String facetFieldName) {
		FacetMetadata result = facetFieldNameToFacetMetadata.get( facetFieldName );
		if ( result != null ) {
			return result;
		}
		for ( EmbeddedTypeMetadata element : embeddedTypeMetadata ) {
			result = element.getFacetMetadataFor( facetFieldName );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Return all {@link DocumentFieldMetadata}.
	 * Instances are not duplicated in the collection. We use {@code Collection} instead of {@code Set} for
	 * implementation reasons.
	 * @return all {@link DocumentFieldMetadata}
	 */
	public Collection<DocumentFieldMetadata> getAllDocumentFieldMetadata() {
		if ( embeddedTypeMetadata.isEmpty() ) {
			return documentFieldMetadata;
		}
		else {
			Collection<DocumentFieldMetadata> allMetadata = new ArrayList<DocumentFieldMetadata>(
					documentFieldMetadata.size()
			);
			for ( EmbeddedTypeMetadata element : embeddedTypeMetadata ) {
				allMetadata.addAll( element.getAllDocumentFieldMetadata() );
			}
			allMetadata.addAll( documentFieldMetadata );
			return Collections.unmodifiableCollection( allMetadata );
		}
	}

	public Collection<DocumentFieldMetadata> getNonEmbeddedDocumentFieldMetadata() {
		return documentFieldMetadata;
	}

	// TODO HSEARCH-1867 change return type to set
	public List<EmbeddedTypeMetadata> getEmbeddedTypeMetadata() {
		return Collections.unmodifiableList( new ArrayList<EmbeddedTypeMetadata>( embeddedTypeMetadata ) );
	}

	public Set<ContainedInMetadata> getContainedInMetadata() {
		return containedInMetadata;
	}

	public Collection<XClass> getOptimizationBlackList() {
		return optimizationBlackList;
	}

	public boolean containsCollectionRole(String role) {
		for ( String knownRolls : collectionRoles ) {
			if ( isSubRole( knownRolls, role ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean areClassBridgesUsed() {
		return !classBridgeFieldNameToDocumentFieldMetadata.isEmpty();
	}

	public DocumentFieldMetadata getFieldMetadataForClassBridgeField(String fieldName) {
		return classBridgeFieldNameToDocumentFieldMetadata.get( fieldName );
	}

	public Discriminator getDiscriminator() {
		return discriminator;
	}

	public XMember getDiscriminatorGetter() {
		return discriminatorGetter;
	}

	public boolean areStateInspectionOptimizationsEnabled() {
		return stateInspectionOptimizationsEnabled;
	}

	public void disableStateInspectionOptimizations() {
		stateInspectionOptimizationsEnabled = false;
	}

	public LuceneOptions getClassLuceneOptions(DocumentFieldMetadata fieldMetadata, float documentLevelBoost) {
		return new LuceneOptionsImpl( fieldMetadata, 1f, documentLevelBoost );
	}

	public LuceneOptions getFieldLuceneOptions(PropertyMetadata propertyMetadata,
			DocumentFieldMetadata fieldMetadata,
			Object value, float inheritedBoost) {
		return new LuceneOptionsImpl(
				fieldMetadata,
				fieldMetadata.getBoost() * propertyMetadata.getDynamicBoostStrategy().defineBoost( value ),
				inheritedBoost
		);
	}

	public BoostStrategy getDynamicBoost() {
		return classBoostStrategy;
	}

	public float getStaticBoost() {
		return boost;
	}

	public float getClassBoost(Object value) {
		return boost * classBoostStrategy.defineBoost( value );
	}

	public ScopedAnalyzerReference getDefaultAnalyzerReference() {
		return scopedAnalyzerReference;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "TypeMetadata{" );
		sb.append( "boost=" ).append( boost );
		sb.append( ", discriminator=" ).append( discriminator );
		sb.append( ", discriminatorGetter=" ).append( discriminatorGetter );
		sb.append( ", classBoostStrategy=" ).append( classBoostStrategy );
		sb.append( ", documentFieldNameToFieldMetadata=" ).append( documentFieldNameToFieldMetadata );
		sb.append( ", propertyGetterNameToFieldMetadata=" ).append( propertyGetterNameToPropertyMetadata );
		sb.append( ", idPropertyMetadata=" ).append( idPropertyMetadata );
		sb.append( ", classBridgeFields=" ).append( classBridgeFieldNameToDocumentFieldMetadata );
		sb.append( ", embeddedTypeMetadata=" ).append( embeddedTypeMetadata );
		sb.append( ", containedInMetadata=" ).append( containedInMetadata );
		sb.append( ", optimizationBlackList=" ).append( optimizationBlackList );
		sb.append( ", stateInspectionOptimizationsEnabled=" ).append( stateInspectionOptimizationsEnabled );
		sb.append( ", scopedAnalyzerReference=" ).append( scopedAnalyzerReference );
		sb.append( ", collectionRoles=" ).append( collectionRoles );
		sb.append( '}' );
		return sb.toString();
	}

	private boolean isSubRole(String subRole, String role) {
		if ( role.equals( subRole ) ) {
			return true; // direct match
		}
		if ( role.startsWith( subRole + COMPONENT_PATH_SEPARATOR ) ) {
			return true; // role == subRole.<something>
		}
		return false;
	}

	private boolean determineWhetherDocumentIdPropertyIsTheSameAsJpaIdProperty(XProperty jpaIdProperty) {
		if ( idPropertyMetadata == null ) {
			return false; // not an indexed type
		}
		if ( jpaIdProperty == null ) {
			return false;
		}
		else {
			return jpaIdProperty.equals( idPropertyMetadata.getPropertyAccessor() );
		}
	}

	private Map<String, PropertyMetadata> buildPropertyMetadataMap(Set<PropertyMetadata> propertyMetadataSet) {
		Map<String, PropertyMetadata> tmpMap = new LinkedHashMap<String, PropertyMetadata>();
		for ( PropertyMetadata propertyMetadata : propertyMetadataSet ) {
			tmpMap.put( propertyMetadata.getPropertyAccessorName(), propertyMetadata );
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	private Set<DocumentFieldMetadata> collectFieldMetadata(Set<PropertyMetadata> propertyMetadataSet,
			Set<DocumentFieldMetadata> classBridgeFields, PropertyMetadata idPropertyMetadata) {
		Set<DocumentFieldMetadata> tmpSet = new LinkedHashSet<DocumentFieldMetadata>(); // Preserve order to make buildFieldMetadataMap deterministic
		for ( PropertyMetadata propertyMetadata : propertyMetadataSet ) {
			for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
				tmpSet.add( documentFieldMetadata );
			}
		}

		for ( DocumentFieldMetadata documentFieldMetadata : classBridgeFields ) {
			tmpSet.add( documentFieldMetadata );
		}

		if ( idPropertyMetadata != null ) {
			for ( DocumentFieldMetadata documentFieldMetadata : idPropertyMetadata.getFieldMetadataSet() ) {
				tmpSet.add( documentFieldMetadata );
			}
		}

		return Collections.unmodifiableSet( tmpSet );
	}

	private Map<String, DocumentFieldMetadata> buildFieldMetadataMap(Set<DocumentFieldMetadata> documentFieldMetadataSet) {
		Map<String, DocumentFieldMetadata> tmpMap = new LinkedHashMap<String, DocumentFieldMetadata>();
		for ( DocumentFieldMetadata documentFieldMetadata : documentFieldMetadataSet ) {
			String name = documentFieldMetadata.getAbsoluteName();
			if ( StringHelper.isEmpty( name ) ) {
				continue;
			}
			DocumentFieldMetadata oldFieldMetadata = tmpMap.put( name, documentFieldMetadata );
			if ( oldFieldMetadata != null ) {
				if ( !documentFieldMetadata.getIndex().equals( oldFieldMetadata.getIndex() ) ) {
					// Try to use the actual declaring type, if possible
					PropertyMetadata sourceProperty = documentFieldMetadata.getSourceProperty();
					String sourceTypeName = sourceProperty != null
							? sourceProperty.getPropertyAccessor().getDeclaringClass().getName()
							: documentFieldMetadata.getSourceType().getType().getName();
					log.inconsistentFieldConfiguration( sourceTypeName, name );
				}
			}
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	private Map<String, FacetMetadata> buildFacetMetadataMap(Collection<DocumentFieldMetadata> documentFieldMetadataCollection) {
		Map<String, FacetMetadata> tmpMap = new LinkedHashMap<String, FacetMetadata>();
		for ( DocumentFieldMetadata documentFieldMetadata : documentFieldMetadataCollection ) {
			for ( FacetMetadata facetMetadata : documentFieldMetadata.getFacetMetadata() ) {
				tmpMap.put( facetMetadata.getAbsoluteName(), facetMetadata );
			}
		}
		// Class bridge fields, etc. are already included in documentFieldMetadataCollection
		return Collections.unmodifiableMap( tmpMap );
	}

	private Map<String, BridgeDefinedField> buildBridgeDefinedFieldMetadataMap(Collection<DocumentFieldMetadata> documentFieldMetadataCollection) {
		Map<String, BridgeDefinedField> tmpMap = new LinkedHashMap<String, BridgeDefinedField>();
		for ( DocumentFieldMetadata documentFieldMetadata : documentFieldMetadataCollection ) {
			for ( BridgeDefinedField bridgeDefinedField : documentFieldMetadata.getBridgeDefinedFields().values() ) {
				tmpMap.put( bridgeDefinedField.getAbsoluteName(), bridgeDefinedField );
			}
		}
		// Class bridge fields, etc. are already included in documentFieldMetadataCollection
		return Collections.unmodifiableMap( tmpMap );
	}

	private Map<String, DocumentFieldMetadata> copyClassBridgeMetadata(Set<DocumentFieldMetadata> classBridgeFields) {
		Map<String, DocumentFieldMetadata> tmpMap = new LinkedHashMap<String, DocumentFieldMetadata>();
		for ( DocumentFieldMetadata fieldMetadata : classBridgeFields ) {
			tmpMap.put( fieldMetadata.getAbsoluteName(), fieldMetadata );
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	public boolean isJpaIdUsedAsDocumentId() {
		return jpaIdUsedAsDocumentId;
	}

	public static class Builder {
		protected final BackReference<TypeMetadata> resultReference = new BackReference<>();
		private final IndexedTypeIdentifier indexedType;
		private final ScopedAnalyzerReference.Builder scopedAnalyzerReferenceBuilder;
		private final MutableAnalyzerRegistry analyzerRegistry;

		private float boost;
		private BoostStrategy classBoostStrategy;
		private Discriminator discriminator;
		private XMember discriminatorGetter;
		private boolean stateInspectionOptimizationsEnabled = true;
		private final Set<PropertyMetadata> propertyMetadataSet = new LinkedHashSet<>();
		private final Set<DocumentFieldMetadata> classBridgeFields = new LinkedHashSet<DocumentFieldMetadata>();
		private final Set<EmbeddedTypeMetadata> embeddedTypeMetadata = new LinkedHashSet<EmbeddedTypeMetadata>();
		private final Set<ContainedInMetadata> containedInMetadata = new LinkedHashSet<ContainedInMetadata>();
		private final Set<XClass> optimizationClassList = new LinkedHashSet<XClass>();
		private final Set<String> collectionRoles = new TreeSet<String>();
		private PropertyMetadata idPropertyMetadata;
		private XProperty jpaProperty;
		private final Set<SortableFieldMetadata> classBridgeSortableFieldMetadata = new LinkedHashSet<>();

		public Builder(IndexedTypeIdentifier indexedType, ConfigContext configContext, ParseContext parseContext) {
			this.indexedType = indexedType;
			if ( parseContext.skipAnalyzers() ) {
				this.analyzerRegistry = null;
				this.scopedAnalyzerReferenceBuilder = null;
			}
			else {
				IndexManagerType indexManagerType = parseContext.getIndexManagerType();
				this.analyzerRegistry = configContext.forType( indexManagerType ).getAnalyzerRegistry();
				this.scopedAnalyzerReferenceBuilder = analyzerRegistry.buildScopedAnalyzerReference();
			}
		}

		public Builder(IndexedTypeIdentifier indexedType, Builder containerTypeBuilder) {
			this.indexedType = indexedType;
			this.analyzerRegistry = containerTypeBuilder.analyzerRegistry;
			this.scopedAnalyzerReferenceBuilder = containerTypeBuilder.scopedAnalyzerReferenceBuilder;
		}

		public Builder idProperty(PropertyMetadata propertyMetadata) {
			this.idPropertyMetadata = propertyMetadata;
			return this;
		}

		public Builder boost(float boost) {
			this.boost = boost;
			return this;
		}

		public Builder boostStrategy(BoostStrategy boostStrategy) {
			this.classBoostStrategy = boostStrategy;
			return this;
		}

		public Builder analyzerReference(AnalyzerReference analyzerReference) {
			this.scopedAnalyzerReferenceBuilder.setGlobalAnalyzerReference( analyzerReference );
			return this;
		}

		public Builder jpaProperty(XProperty jpaProperty) {
			this.jpaProperty = jpaProperty;
			return this;
		}

		public Builder analyzerDiscriminator(Discriminator discriminator, XMember discriminatorGetter) {
			if ( this.discriminator != null ) {
				throw new SearchException(
						"Multiple AnalyzerDiscriminator defined in the same class hierarchy: " + this.indexedType
								.getName()
				);
			}

			this.discriminator = discriminator;
			this.discriminatorGetter = discriminatorGetter;
			return this;
		}

		public Builder addProperty(PropertyMetadata propertyMetadata) {
			if ( idPropertyMetadata != null && idPropertyMetadata.getPropertyAccessorName() != null ) {
				// the id property is always a single field
				String idFieldName = idPropertyMetadata.getFieldMetadataSet().iterator().next().getAbsoluteName();
				for ( DocumentFieldMetadata fieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
					if ( idFieldName.equals( fieldMetadata.getAbsoluteName() ) ) {
						throw log.fieldTriesToOverrideIdFieldSettings(
								propertyMetadata.getPropertyAccessor().getDeclaringClass().getName(),
								propertyMetadata.getPropertyAccessor().getName()
						);
					}
				}
			}
			this.propertyMetadataSet.add( propertyMetadata );
			return this;
		}

		public void addClassBridgeField(DocumentFieldMetadata fieldMetadata) {
			classBridgeFields.add( fieldMetadata );
		}

		public void addEmbeddedType(EmbeddedTypeMetadata embeddedTypeMetadata) {
			this.embeddedTypeMetadata.add( embeddedTypeMetadata );
		}

		public void addContainedIn(ContainedInMetadata containedInMetadata) {
			this.containedInMetadata.add( containedInMetadata );
		}

		public void addCollectionRole(String role) {
			collectionRoles.add( role );
		}

		public void disableStateInspectionOptimization() {
			stateInspectionOptimizationsEnabled = false;
		}

		@SuppressWarnings( "deprecation" )
		public AnalyzerReference addToScopedAnalyzerReference(DocumentFieldPath fieldPath, AnalyzerReference analyzerReference, Field.Index index) {
			if ( analyzerReference == null ) {
				analyzerReference = scopedAnalyzerReferenceBuilder.getGlobalAnalyzerReference();
			}

			if ( !index.isAnalyzed() ) {
				// no analyzer is used, add a pass-through (i.e. no-op) analyzer for queries
				analyzerReference = analyzerRegistry.getPassThroughAnalyzerReference();
			}

			scopedAnalyzerReferenceBuilder.addAnalyzerReference( fieldPath.getAbsoluteName(), analyzerReference );

			return analyzerReference;
		}

		public void blacklistForOptimization(XClass blackListClass) {
			this.optimizationClassList.add( blackListClass );
		}

		public boolean areClassBridgesUsed() {
			return !classBridgeFields.isEmpty();
		}

		public BoostStrategy getClassBoostStrategy() {
			return classBoostStrategy;
		}

		public AnalyzerReference getAnalyzerReference() {
			return scopedAnalyzerReferenceBuilder.getGlobalAnalyzerReference();
		}

		public ScopedAnalyzerReference.Builder getScopedAnalyzerReferenceBuilder() {
			return scopedAnalyzerReferenceBuilder;
		}

		public boolean isStateInspectionOptimizationsEnabled() {
			return stateInspectionOptimizationsEnabled;
		}

		public IndexedTypeIdentifier getIndexedType() {
			return indexedType;
		}

		public PropertyMetadata getIdPropertyMetadata() {
			return idPropertyMetadata;
		}

		public BackReference<TypeMetadata> getResultReference() {
			return resultReference;
		}

		public TypeMetadata build() {
			TypeMetadata result = new TypeMetadata( this );
			resultReference.initialize( result );
			return result;
		}

		@Override
		public String toString() {
			return "TypeMetadata.Builder{indexedType=" + indexedType + "}";
		}

		public void addClassBridgeSortableFields(Iterable<String> sortableFieldsAbsoluteNames) {
			for ( String sortableFieldAbsoluteName : sortableFieldsAbsoluteNames ) {
				classBridgeSortableFieldMetadata.add( new SortableFieldMetadata.Builder( sortableFieldAbsoluteName ).build() );
			}
		}
	}
}
