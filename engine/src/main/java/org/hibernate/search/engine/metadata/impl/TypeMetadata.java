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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import org.hibernate.search.util.impl.ScopedAnalyzer;
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
	private final Class<?> indexedType;

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
	 * Metadata for a document field keyed against the field name
	 */
	private final Map<String, DocumentFieldMetadata> documentFieldNameToFieldMetadata;

	/**
	 * Metadata for a Java property (field or getter) keyed  the property name
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
	 * Document field metadata keyed against the field name. These fields are contributed by class bridges
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
	 * The scoped analyzer for this entity
	 */
	private final ScopedAnalyzer scopedAnalyzer;

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
		this.indexedType = builder.indexedType;
		this.boost = builder.boost;
		this.scopedAnalyzer = builder.scopedAnalyzer;
		this.scopedAnalyzer.setGlobalAnalyzer( builder.analyzer );
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
		this.propertyGetterNameToPropertyMetadata = keyPropertyMetadata( builder.propertyMetadataSet );
		this.documentFieldNameToFieldMetadata = keyFieldMetadata( builder.propertyMetadataSet );
		this.classBridgeFieldNameToDocumentFieldMetadata = copyClassBridgeMetadata( builder.classBridgeFields );
		this.classBridgeSortableFieldMetadata = Collections.unmodifiableSet( builder.classBridgeSortableFieldMetadata );
	}

	public Class<?> getType() {
		return indexedType;
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

	/**
	 * Return all {@link DocumentFieldMetadata}.
	 * Instances are not duplicated in the collection. We use {@code Collection} instead of {@code Set} for
	 * implementation reasons.
	 * @return a all {@link DocumentFieldMetadata}
	 */
	public Collection<DocumentFieldMetadata> getAllDocumentFieldMetadata() {
		if ( embeddedTypeMetadata.isEmpty() ) {
			//no need to wrap as unmodifiable collections, the instances are already safe.
			return documentFieldNameToFieldMetadata.values();
		}
		else {
			Collection<DocumentFieldMetadata> allMetadata = new ArrayList<DocumentFieldMetadata>(
					documentFieldNameToFieldMetadata.size()
			);
			for ( EmbeddedTypeMetadata element : embeddedTypeMetadata ) {
				allMetadata.addAll( element.getAllDocumentFieldMetadata() );
			}
			allMetadata.addAll( documentFieldNameToFieldMetadata.values() );
			return allMetadata;
		}
	}

	// TODO HSEARCH-1867 change return type to set
	public List<EmbeddedTypeMetadata> getEmbeddedTypeMetadata() {
		return new ArrayList<EmbeddedTypeMetadata>( embeddedTypeMetadata );
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

	public ScopedAnalyzer getDefaultAnalyzer() {
		return scopedAnalyzer;
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
		sb.append( ", scopedAnalyzer=" ).append( scopedAnalyzer );
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

	private Map<String, PropertyMetadata> keyPropertyMetadata(Set<PropertyMetadata> propertyMetadataSet) {
		Map<String, PropertyMetadata> tmpMap = new HashMap<String, PropertyMetadata>();
		for ( PropertyMetadata propertyMetadata : propertyMetadataSet ) {
			tmpMap.put( propertyMetadata.getPropertyAccessorName(), propertyMetadata );
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	private Map<String, DocumentFieldMetadata> keyFieldMetadata(Set<PropertyMetadata> propertyMetadataSet) {
		Map<String, DocumentFieldMetadata> tmpMap = new HashMap<String, DocumentFieldMetadata>();
		for ( PropertyMetadata propertyMetadata : propertyMetadataSet ) {
			for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
				DocumentFieldMetadata oldFieldMetadata = tmpMap.put(
						documentFieldMetadata.getName(),
						documentFieldMetadata
				);
				if ( oldFieldMetadata != null ) {
					if ( !documentFieldMetadata.getIndex().equals( oldFieldMetadata.getIndex() ) ) {
						log.inconsistentFieldConfiguration(
								propertyMetadata.getPropertyAccessor().getDeclaringClass().getName(),
								documentFieldMetadata.getName() );
					}
				}
			}
		}

		for ( DocumentFieldMetadata documentFieldMetadata : classBridgeFields ) {
			tmpMap.put( documentFieldMetadata.getName(), documentFieldMetadata );
		}

		if ( idPropertyMetadata != null ) {
			for ( DocumentFieldMetadata documentFieldMetadata : idPropertyMetadata.getFieldMetadataSet() ) {
				tmpMap.put( documentFieldMetadata.getName(), documentFieldMetadata );
			}
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	private Map<String, DocumentFieldMetadata> copyClassBridgeMetadata(Set<DocumentFieldMetadata> classBridgeFields) {
		Map<String, DocumentFieldMetadata> tmpMap = new HashMap<String, DocumentFieldMetadata>();
		for ( DocumentFieldMetadata fieldMetadata : classBridgeFields ) {
			tmpMap.put( fieldMetadata.getName(), fieldMetadata );
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	public boolean isJpaIdUsedAsDocumentId() {
		return jpaIdUsedAsDocumentId;
	}

	public static class Builder {
		private final Class<?> indexedType;
		private final ScopedAnalyzer scopedAnalyzer;
		private final PassThroughAnalyzer passThroughAnalyzer;

		private float boost;
		private BoostStrategy classBoostStrategy;
		private Analyzer analyzer;
		private Discriminator discriminator;
		private XMember discriminatorGetter;
		private boolean stateInspectionOptimizationsEnabled = true;
		private final Set<PropertyMetadata> propertyMetadataSet = new HashSet<>();
		private final Set<DocumentFieldMetadata> classBridgeFields = new HashSet<DocumentFieldMetadata>();
		private final Set<EmbeddedTypeMetadata> embeddedTypeMetadata = new HashSet<EmbeddedTypeMetadata>();
		private final Set<ContainedInMetadata> containedInMetadata = new HashSet<ContainedInMetadata>();
		private final Set<XClass> optimizationClassList = new HashSet<XClass>();
		private final Set<String> collectionRoles = new TreeSet<String>();
		private PropertyMetadata idPropertyMetadata;
		private XProperty jpaProperty;
		private final Set<SortableFieldMetadata> classBridgeSortableFieldMetadata = new HashSet<>();

		public Builder(Class<?> indexedType, ConfigContext configContext) {
			this( indexedType, new ScopedAnalyzer( configContext.getDefaultAnalyzer() ) );
		}

		public Builder(Class<?> indexedType, ScopedAnalyzer scopedAnalyzer) {
			this.indexedType = indexedType;
			this.scopedAnalyzer = scopedAnalyzer;
			this.passThroughAnalyzer = PassThroughAnalyzer.INSTANCE;
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

		public Builder analyzer(Analyzer analyzer) {
			this.analyzer = analyzer;
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
				String idFieldName = idPropertyMetadata.getFieldMetadataSet().iterator().next().getName();
				for ( DocumentFieldMetadata fieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
					if ( idFieldName.equals( fieldMetadata.getName() ) ) {
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
		public Analyzer addToScopedAnalyzer(String fieldName, Analyzer analyzer, Field.Index index) {
			if ( analyzer == null ) {
				analyzer = this.getAnalyzer();
			}

			if ( Field.Index.ANALYZED.equals( index ) || Field.Index.ANALYZED_NO_NORMS.equals( index ) ) {
				if ( analyzer != null ) {
					scopedAnalyzer.addScopedAnalyzer( fieldName, analyzer );
				}
			}
			else {
				// no analyzer is used, add a fake one for queries
				scopedAnalyzer.addScopedAnalyzer( fieldName, passThroughAnalyzer );
			}
			return analyzer;
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

		public Analyzer getAnalyzer() {
			return analyzer;
		}

		public ScopedAnalyzer getScopedAnalyzer() {
			return scopedAnalyzer;
		}

		public boolean isStateInspectionOptimizationsEnabled() {
			return stateInspectionOptimizationsEnabled;
		}

		public Class<?> getIndexedType() {
			return indexedType;
		}

		public PropertyMetadata getIdPropertyMetadata() {
			return idPropertyMetadata;
		}

		public TypeMetadata build() {
			return new TypeMetadata( this );
		}

		@Override
		public String toString() {
			return "TypeMetadata.Builder{indexedType=" + indexedType + "}";
		}

		public void addClassBridgeSortableFields(Iterable<String> sortableFieldNames) {
			for ( String sortableFieldName : sortableFieldNames ) {
				classBridgeSortableFieldMetadata.add( new SortableFieldMetadata.Builder().fieldName( sortableFieldName ).build() );
			}
		}
	}
}


