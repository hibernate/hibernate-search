/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.Version;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.SearchException;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.hibernate.search.impl.ConfigContext;
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
	private final Set<PropertyMetadata> propertyMetadataSet;

	/**
	 * Metadata for a document field keyed against the field name
	 */
	private final Map<String, DocumentFieldMetadata> documentFieldNameFieldMetadata;

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
	 * The similarity defined this this type. {@code null} is not explicitly set.
	 */
	private final Similarity similarity;

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

	protected TypeMetadata(Builder builder) {
		this.boost = builder.boost;
		this.scopedAnalyzer = builder.scopedAnalyzer;
		this.scopedAnalyzer.setGlobalAnalyzer( builder.analyzer );
		this.discriminator = builder.discriminator;
		this.discriminatorGetter = builder.discriminatorGetter;
		this.classBoostStrategy = builder.classBoostStrategy;
		this.stateInspectionOptimizationsEnabled = builder.stateInspectionOptimizationsEnabled;
		this.similarity = builder.similarity;
		this.idPropertyMetadata = builder.idPropertyMetadata;
		this.embeddedTypeMetadata = Collections.unmodifiableSet( builder.embeddedTypeMetadata );
		this.containedInMetadata = Collections.unmodifiableSet( builder.containedInMetadata );
		this.optimizationBlackList = Collections.unmodifiableSet( builder.optimizationClassList );
		this.collectionRoles = Collections.unmodifiableSet( builder.collectionRoles );
		this.jpaIdUsedAsDocumentId = determineWhetherDocumentIdPropertyIsTheSameAsJpaIdProperty( builder.jpaProperty );
		this.classBridgeFields = Collections.unmodifiableSet( builder.classBridgeFields );
		this.propertyMetadataSet = Collections.unmodifiableSet( builder.propertyMetadataList );
		this.propertyGetterNameToPropertyMetadata = keyPropertyMetadata( builder.propertyMetadataList );
		this.documentFieldNameFieldMetadata = keyFieldMetadata( builder.propertyMetadataList );
		this.classBridgeFieldNameToDocumentFieldMetadata = copyClassBridgeMetadata( builder.classBridgeFields );
	}

	public Set<PropertyMetadata> getPropertyMetadata() {
		return propertyMetadataSet;
	}

	public Set<DocumentFieldMetadata> getClassBridgeMetadata() {
		return classBridgeFields;
	}

	public Set<EmbeddedTypeMetadata> getEmbeddedTypeMetadata() {
		return embeddedTypeMetadata;
	}

	public Set<ContainedInMetadata> getContainedInMetadata() {
		return containedInMetadata;
	}

	public Collection<XClass> getOptimizationBlackList() {
		return optimizationBlackList;
	}

	public boolean containsCollectionRole(String role) {
		return collectionRoles.contains( role );
	}

	public boolean areClassBridgesUsed() {
		return !classBridgeFieldNameToDocumentFieldMetadata.isEmpty();
	}

	public PropertyMetadata getIdPropertyMetadata() {
		return idPropertyMetadata;
	}

	public DocumentFieldMetadata getDocumentFieldMetadataFor(String fieldName) {
		return documentFieldNameFieldMetadata.get( fieldName );
	}

	public PropertyMetadata getPropertyMetadataForProperty(String propertyName) {
		return propertyGetterNameToPropertyMetadata.get( propertyName );
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

	public BoostStrategy getClassBoostStrategy() {
		return classBoostStrategy;
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	public boolean areStateInspectionOptimizationsEnabled() {
		return stateInspectionOptimizationsEnabled;
	}

	public void disableStateInspectionOptimizations() {
		stateInspectionOptimizationsEnabled = false;
	}

	public LuceneOptions getClassLuceneOptions(DocumentFieldMetadata fieldMetadata) {
		return new LuceneOptionsImpl( fieldMetadata );
	}

	public LuceneOptions getFieldLuceneOptions(PropertyMetadata propertyMetadata,
			DocumentFieldMetadata fieldMetadata,
			Object value) {
		return new LuceneOptionsImpl(
				fieldMetadata,
				fieldMetadata.getBoost() * propertyMetadata.getDynamicBoostStrategy().defineBoost( value ),
				propertyMetadata.getNullToken(),
				propertyMetadata.getPrecisionStep()
		);
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
		sb.append( ", documentFieldNameToFieldMetadata=" ).append( documentFieldNameFieldMetadata );
		sb.append( ", propertyGetterNameToFieldMetadata=" ).append( propertyGetterNameToPropertyMetadata );
		sb.append( ", idPropertyMetadata=" ).append( idPropertyMetadata );
		sb.append( ", classBridgeFields=" ).append( classBridgeFieldNameToDocumentFieldMetadata );
		sb.append( ", embeddedTypeMetadata=" ).append( embeddedTypeMetadata );
		sb.append( ", containedInMetadata=" ).append( containedInMetadata );
		sb.append( ", optimizationBlackList=" ).append( optimizationBlackList );
		sb.append( ", similarity=" ).append( similarity );
		sb.append( ", stateInspectionOptimizationsEnabled=" ).append( stateInspectionOptimizationsEnabled );
		sb.append( ", scopedAnalyzer=" ).append( scopedAnalyzer );
		sb.append( ", collectionRoles=" ).append( collectionRoles );
		sb.append( '}' );
		return sb.toString();
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
			for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadata() ) {
				DocumentFieldMetadata oldFieldMetadata = tmpMap.put(
						documentFieldMetadata.getName(),
						documentFieldMetadata
				);
				if ( oldFieldMetadata != null ) {
					if ( !documentFieldMetadata.getIndex().equals( oldFieldMetadata.getIndex() ) ) {
						log.inconsistentFieldConfiguration( documentFieldMetadata.getName() );
					}
				}
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
		private Similarity similarity;
		private Set<PropertyMetadata> propertyMetadataList = new HashSet<PropertyMetadata>();
		private Set<DocumentFieldMetadata> classBridgeFields = new HashSet<DocumentFieldMetadata>();
		private Set<EmbeddedTypeMetadata> embeddedTypeMetadata = new HashSet<EmbeddedTypeMetadata>();
		private Set<ContainedInMetadata> containedInMetadata = new HashSet<ContainedInMetadata>();
		private Set<XClass> optimizationClassList = new HashSet<XClass>();
		private Set<String> collectionRoles = new TreeSet<String>();
		private PropertyMetadata idPropertyMetadata;
		private XProperty jpaProperty;

		public Builder(Class<?> indexedType, ConfigContext configContext) {
			this( indexedType, configContext, new ScopedAnalyzer() );
		}

		public Builder(Class<?> indexedType, ConfigContext configContext, ScopedAnalyzer scopedAnalyzer) {
			this.indexedType = indexedType;
			this.scopedAnalyzer = scopedAnalyzer;
			Version luceneVersion = configContext.getLuceneMatchVersion();
			this.passThroughAnalyzer = new PassThroughAnalyzer( luceneVersion );
		}

		public Builder idProperty(PropertyMetadata propertyMetadata) {
			this.idPropertyMetadata = propertyMetadata;
			return this;
		}

		public Builder similarity(Similarity similarity) {
			if ( this.similarity != null ) {
				throw new SearchException(
						"Multiple similarities defined in the same class hierarchy or on the index settings: "
								+ indexedType.getName()
				);
			}
			this.similarity = similarity;
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
			this.propertyMetadataList.add( propertyMetadata );
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

		public void addToScopedAnalyzer(String fieldName, Analyzer analyzer, Field.Index index) {
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

		public TypeMetadata build() {
			return new TypeMetadata( this );
		}
	}
}


