/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.util.ClassLoaderHelper;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.PassThroughAnalyzer;
import org.hibernate.search.util.ReflectionHelper;
import org.hibernate.search.util.ScopedAnalyzer;

/**
 * Abstract base class for the document builders.
 *
 * @author Hardy Ferentschik
 */
public abstract class AbstractDocumentBuilder<T> implements DocumentBuilder {
	private static final Logger log = LoggerFactory.make();

	private final XClass beanXClass;
	protected final String beanXClassName;
	protected final Class<?> beanClass;
	private Set<Class<?>> mappedSubclasses = new HashSet<Class<?>>();
	private int level = 0;
	private int maxLevel = Integer.MAX_VALUE;
	private final ScopedAnalyzer analyzer = new ScopedAnalyzer();
	private Similarity similarity; //there is only 1 similarity per class hierarchy, and only 1 per index
	private boolean isRoot;
	private Analyzer passThroughAnalyzer = new PassThroughAnalyzer();
	protected final Set<String> fieldCollectionRoles = new TreeSet<String>();
	protected final Set<String> indexedEmbeddedCollectionRoles = new TreeSet<String>();
	protected final Set<String> containedInCollectionRoles = new TreeSet<String>();

	protected final PropertiesMetadata metadata = new PropertiesMetadata();
	protected EntityState entityState;
	protected ReflectionManager reflectionManager; //available only during initialization and post-initialization

	private boolean stateInspectionOptimizationsEnabled = true;

	/**
	 * Constructor used on contained entities not annotated with {@code @Indexed} themselves.
	 *
	 * @param xClass The class for which to build a {@code}DocumentBuilderContainedEntity}
	 * @param context Handle to default configuration settings
	 * @param similarity The index level similarity
	 * @param reflectionManager Reflection manager to use for processing the annotations
	 */
	public AbstractDocumentBuilder(XClass xClass, ConfigContext context, Similarity similarity, ReflectionManager reflectionManager, Set<XClass> optimizationBlackList) {

		if ( xClass == null ) {
			throw new AssertionFailure( "Unable to build a DocumentBuilderContainedEntity with a null class" );
		}

		this.entityState = EntityState.CONTAINED_IN_ONLY;
		this.beanXClass = xClass;
		this.beanXClassName = xClass.getName();
		this.reflectionManager = reflectionManager;
		this.beanClass = reflectionManager.toClass( xClass );
		this.similarity = similarity; //set the index similarity before the class level one to detect conflict

		metadata.boost = getBoost( xClass );
		metadata.classBoostStrategy = getDynamicBoost( xClass );
		metadata.analyzer = context.getDefaultAnalyzer();

		Set<XClass> processedClasses = new HashSet<XClass>();
		processedClasses.add( xClass );
		initializeClass( xClass, metadata, true, "", processedClasses, context, optimizationBlackList, false );

		this.analyzer.setGlobalAnalyzer( metadata.analyzer );

		// set the default similarity in case that after processing all classes there is still no similarity set
		if ( this.similarity == null ) {
			this.similarity = context.getDefaultSimilarity();
		}
	}

	public abstract void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, boolean delete, boolean add, boolean batch, List<LuceneWork> queue);

	abstract protected void documentBuilderSpecificChecks(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context);

	/**
	 * In case of an indexed entity, return the value of it's identifier: what is marked as @Id or @DocumentId;
	 * in case the entity uses @ProvidedId, it's illegal to call this method.
	 *
	 * @param entity the instance for which to retrieve the id
	 *
	 * @return the value, or null if it's not an indexed entity
	 *
	 * @throws IllegalStateException when used with a @ProvidedId annotated entity
	 */
	abstract public Serializable getId(Object entity);

	public boolean isRoot() {
		return isRoot;
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public XClass getBeanXClass() {
		return beanXClass;
	}

	public PropertiesMetadata getMetadata() {
		return metadata;
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public EntityState getEntityState() {
		return entityState;
	}

	public Set<Class<?>> getMappedSubclasses() {
		return mappedSubclasses;
	}

	public void postInitialize(Set<Class<?>> indexedClasses) {
		//we initialize only once because we no longer have a reference to the reflectionManager
		//in theory
		Class<?> plainClass = beanClass;
		if ( entityState == EntityState.NON_INDEXABLE ) {
			throw new AssertionFailure( "A non indexed entity is post processed" );
		}
		Set<Class<?>> tempMappedSubclasses = new HashSet<Class<?>>();
		//together with the caller this creates a o(2), but I think it's still faster than create the up hierarchy for each class
		for ( Class<?> currentClass : indexedClasses ) {
			if ( plainClass != currentClass && plainClass.isAssignableFrom( currentClass ) ) {
				tempMappedSubclasses.add( currentClass );
			}
		}
		this.mappedSubclasses = Collections.unmodifiableSet( tempMappedSubclasses );
		Class<?> superClass = plainClass.getSuperclass();
		this.isRoot = true;
		while ( superClass != null ) {
			if ( indexedClasses.contains( superClass ) ) {
				this.isRoot = false;
				break;
			}
			superClass = superClass.getSuperclass();
		}
		this.reflectionManager = null;
	}

	protected Analyzer getAnalyzer(XAnnotatedElement annotatedElement, ConfigContext context) {
		org.hibernate.search.annotations.Analyzer analyzerAnn =
				annotatedElement.getAnnotation( org.hibernate.search.annotations.Analyzer.class );
		return getAnalyzer( analyzerAnn, context );
	}

	protected void addToScopedAnalyzer(String fieldName, Analyzer analyzer, Index index) {
		if ( index == Index.TOKENIZED ) {
			if ( analyzer != null ) {
				this.analyzer.addScopedAnalyzer( fieldName, analyzer );
			}
		}
		else {
			//no analyzer is used, add a fake one for queries
			this.analyzer.addScopedAnalyzer( fieldName, passThroughAnalyzer );
		}
	}

	protected Float getBoost(XProperty member, org.hibernate.search.annotations.Field fieldAnn) {
		float computedBoost = 1.0f;
		Boost boostAnn = member.getAnnotation( Boost.class );
		if ( boostAnn != null ) {
			computedBoost = boostAnn.value();
		}
		if ( fieldAnn != null ) {
			computedBoost *= fieldAnn.boost().value();
		}
		return computedBoost;
	}

	protected BoostStrategy getDynamicBoost(XProperty member) {
		DynamicBoost boostAnnotation = member.getAnnotation( DynamicBoost.class );
		if ( boostAnnotation == null ) {
			return new DefaultBoostStrategy();
		}

		Class<? extends BoostStrategy> boostStrategyClass = boostAnnotation.impl();
		BoostStrategy strategy;
		try {
			strategy = boostStrategyClass.newInstance();
		}
		catch ( Exception e ) {
			throw new SearchException(
					"Unable to instantiate boost strategy implementation: " + boostStrategyClass.getName()
			);
		}
		return strategy;
	}

	protected Field.TermVector getTermVector(TermVector vector) {
		switch ( vector ) {
			case NO:
				return Field.TermVector.NO;
			case YES:
				return Field.TermVector.YES;
			case WITH_OFFSETS:
				return Field.TermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return Field.TermVector.WITH_POSITIONS;
			case WITH_POSITION_OFFSETS:
				return Field.TermVector.WITH_POSITIONS_OFFSETS;
			default:
				throw new AssertionFailure( "Unexpected TermVector: " + vector );
		}
	}

	protected Field.Index getIndex(Index index) {
		switch ( index ) {
			case NO:
				return Field.Index.NO;
			case NO_NORMS:
				return Field.Index.NOT_ANALYZED_NO_NORMS;
			case TOKENIZED:
				return Field.Index.ANALYZED;
			case UN_TOKENIZED:
				return Field.Index.NOT_ANALYZED;
			default:
				throw new AssertionFailure( "Unexpected Index: " + index );
		}
	}

	/**
	 * If we have a work instance we have to check whether the instance to be indexed is contained in any other indexed entities.
	 */
	public void appendContainedInWorkForInstance(Object instance, WorkPlan workplan) {
		for ( int i = 0; i < metadata.containedInGetters.size(); i++ ) {
			XMember member = metadata.containedInGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( instance, member );

			if ( value == null ) {
				continue;
			}

			if ( member.isArray() ) {
				@SuppressWarnings("unchecked")
				T[] array = (T[]) value;
				for ( T arrayValue : array ) {
					processSingleContainedInInstance( workplan, arrayValue );
				}
			}
			else if ( member.isCollection() ) {
				Collection<T> collection = null;
				try {
					collection = getActualCollection( member, value );
					collection.size(); //load it
				}
				catch ( Exception e ) {
					if ( e.getClass().getName().contains( "org.hibernate.LazyInitializationException" ) ) {
						/* A deleted entity not having its collection initialized
						 * leads to a LIE because the collection is no longer attached to the session
						 *
						 * But that's ok as the collection update event has been processed before
						 * or the fk would have been cleared and thus triggering the cleaning
						 */
						collection = null;
					}
				}
				if ( collection != null ) {
					for ( T collectionValue : collection ) {
						processSingleContainedInInstance( workplan, collectionValue );
					}
				}
			}
			else {
				processSingleContainedInInstance( workplan, value );
			}
		}
	}

	private void initializeClass(XClass clazz, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix,
								 Set<XClass> processedClasses, ConfigContext context, Set<XClass> optimizationBlackList, boolean disableOptimizationsArg) {
		List<XClass> hierarchy = new LinkedList<XClass>();
		XClass next = null;
		for ( XClass previousClass = clazz; previousClass != null; previousClass = next ) {
			next = previousClass.getSuperclass();
			if ( next != null ) {
				hierarchy.add( 0, previousClass ); // append to head to create a list in top-down iteration order
			}
		}

		// Iterate the class hierarchy top down. This allows to override the default analyzer for the properties if the class holds one
		for ( XClass currentClass : hierarchy ) {
			initializeClassLevelAnnotations( currentClass, propertiesMetadata, isRoot, prefix, context );
		}
		
		// if optimizations are enabled, we allow for state in indexedEmbedded objects which are not
		// explicitly indexed (Field or indexedembedded) to skip index update triggering.
		// we don't allow this if the reference is reachable via a custom fieldbridge or classbridge,
		// as state changes from values out of our control could affect the index.
		boolean disableOptimizations = disableOptimizationsArg || ! stateInspectionOptimizationsEnabled();

		// iterate again for the properties and fields
		for ( XClass currentClass : hierarchy ) {
			// rejecting non properties (ie regular methods) because the object is loaded from Hibernate,
			// so indexing a non property does not make sense
			List<XProperty> methods = currentClass.getDeclaredProperties( XClass.ACCESS_PROPERTY );
			for ( XProperty method : methods ) {
				initializeMemberLevelAnnotations(
						currentClass, method, propertiesMetadata, isRoot, prefix, processedClasses, context, optimizationBlackList, disableOptimizations
				);
			}

			List<XProperty> fields = currentClass.getDeclaredProperties( XClass.ACCESS_FIELD );
			for ( XProperty field : fields ) {
				initializeMemberLevelAnnotations(
						currentClass, field, propertiesMetadata, isRoot, prefix, processedClasses, context, optimizationBlackList, disableOptimizations
				);
			}
		}
	}

	/**
	 * Check and initialize class level annotations.
	 *
	 * @param clazz The class to process.
	 * @param propertiesMetadata The meta data holder.
	 * @param isRoot Flag indicating if the specified class is a root entity, meaning the start of a chain of indexed
	 * entities.
	 * @param prefix The current prefix used for the <code>Document</code> field names.
	 * @param context Handle to default configuration settings.
	 */
	private void initializeClassLevelAnnotations(XClass clazz, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context) {

		// check for a class level specified analyzer
		Analyzer analyzer = getAnalyzer( clazz, context );
		if ( analyzer != null ) {
			propertiesMetadata.analyzer = analyzer;
		}

		// check for AnalyzerDefs annotations
		checkForAnalyzerDefs( clazz, context );

		// Check for any ClassBridges annotation.
		ClassBridges classBridgesAnn = clazz.getAnnotation( ClassBridges.class );
		if ( classBridgesAnn != null ) {
			ClassBridge[] classBridges = classBridgesAnn.value();
			for ( ClassBridge cb : classBridges ) {
				bindClassBridgeAnnotation( prefix, propertiesMetadata, cb, clazz, context );
			}
		}

		// Check for any ClassBridge style of annotations.
		ClassBridge classBridgeAnn = clazz.getAnnotation( ClassBridge.class );
		if ( classBridgeAnn != null ) {
			bindClassBridgeAnnotation( prefix, propertiesMetadata, classBridgeAnn, clazz, context );
		}

		checkForAnalyzerDiscriminator( clazz, propertiesMetadata );

		// Get similarity
		if ( isRoot ) {
			checkForSimilarity( clazz );
		}
	}

	private void initializeMemberLevelAnnotations(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot,
					String prefix, Set<XClass> processedClasses, ConfigContext context, Set<XClass> optimizationBlackList, boolean disableOptimizations) {
		checkForField( classHostingMember, member, propertiesMetadata, prefix, context );
		checkForFields( classHostingMember, member, propertiesMetadata, prefix, context );
		checkForAnalyzerDefs( member, context );
		checkForAnalyzerDiscriminator( member, propertiesMetadata );
		checkForIndexedEmbedded( classHostingMember, member, propertiesMetadata, prefix, processedClasses, context, optimizationBlackList, disableOptimizations );
		checkForContainedIn( classHostingMember, member, propertiesMetadata );
		documentBuilderSpecificChecks( member, propertiesMetadata, isRoot, prefix, context );
	}

	private Analyzer getAnalyzer(org.hibernate.search.annotations.Analyzer analyzerAnn, ConfigContext context) {
		Class<?> analyzerClass = analyzerAnn == null ? void.class : analyzerAnn.impl();
		if ( analyzerClass == void.class ) {
			String definition = analyzerAnn == null ? "" : analyzerAnn.definition();
			if ( StringHelper.isEmpty( definition ) ) {
				return null;
			}
			else {
				return context.buildLazyAnalyzer( definition );
			}
		}
		else {
			try {
				return ClassLoaderHelper.analyzerInstanceFromClass( analyzerClass, context.getLuceneMatchVersion() );
			}
			catch ( ClassCastException e ) {
				throw new SearchException(
						"Lucene analyzer does not extend " + Analyzer.class.getName() + ": " + analyzerClass.getName(),
						e
				);
			}
			catch ( Exception e ) {
				throw new SearchException(
						"Failed to instantiate lucene analyzer with type " + analyzerClass.getName(), e
				);
			}
		}
	}

	private void checkForAnalyzerDefs(XAnnotatedElement annotatedElement, ConfigContext context) {
		AnalyzerDefs defs = annotatedElement.getAnnotation( AnalyzerDefs.class );
		if ( defs != null ) {
			for ( AnalyzerDef def : defs.value() ) {
				context.addAnalyzerDef( def, annotatedElement );
			}
		}
		AnalyzerDef def = annotatedElement.getAnnotation( AnalyzerDef.class );
		context.addAnalyzerDef( def, annotatedElement );
	}

	private void checkForAnalyzerDiscriminator(XAnnotatedElement annotatedElement, PropertiesMetadata propertiesMetadata) {
		AnalyzerDiscriminator discriminatorAnn = annotatedElement.getAnnotation( AnalyzerDiscriminator.class );
		if ( discriminatorAnn != null ) {
			if ( propertiesMetadata.discriminator != null ) {
				throw new SearchException(
						"Multiple AnalyzerDiscriminator defined in the same class hierarchy: " + beanXClass.getName()
				);
			}

			Class<? extends Discriminator> discriminatorClass = discriminatorAnn.impl();
			try {
				propertiesMetadata.discriminator = discriminatorClass.newInstance();
			}
			catch ( Exception e ) {
				throw new SearchException(
						"Unable to instantiate analyzer discriminator implementation: " + discriminatorClass.getName()
				);
			}

			if ( annotatedElement instanceof XMember ) {
				propertiesMetadata.discriminatorGetter = (XMember) annotatedElement;
			}
		}
	}

	private void checkForFields(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata,
			String prefix, ConfigContext context) {
		org.hibernate.search.annotations.Fields fieldsAnn =
				member.getAnnotation( org.hibernate.search.annotations.Fields.class );
		NumericFields numericAnns = member.getAnnotation( NumericFields.class );
		if ( fieldsAnn != null ) {
			for ( org.hibernate.search.annotations.Field fieldAnn : fieldsAnn.value() ) {
				bindFieldAnnotation(
						classHostingMember,
						member,
						propertiesMetadata,
						prefix,
						fieldAnn,
						getNumericExtension( fieldAnn, numericAnns ),
						context
				);
			}
		}
	}

	private NumericField getNumericExtension(org.hibernate.search.annotations.Field fieldAnn, NumericFields numericFields) {
		if ( numericFields == null ) {
			return null;
		}
		for ( NumericField numericField : numericFields.value() ) {
			if ( numericField.forField().equals( fieldAnn.name() ) ) {
				return numericField;
			}
		}
		return null;
	}

	private void checkForSimilarity(XClass currClass) {
		org.hibernate.search.annotations.Similarity similarityAnn = currClass.getAnnotation( org.hibernate.search.annotations.Similarity.class );
		if ( similarityAnn != null ) {
			if ( similarity != null ) {
				throw new SearchException(
						"Multiple similarities defined in the same class hierarchy or on the index settings: " + beanXClass
								.getName()
				);
			}
			Class<?> similarityClass = similarityAnn.impl();
			try {
				similarity = (Similarity) similarityClass.newInstance();
			}
			catch ( Exception e ) {
				log.error(
						"Exception attempting to instantiate Similarity '{}' set for {}",
						similarityClass.getName(), beanXClass.getName()
				);
			}
		}
	}

	private void checkForField(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata,
			String prefix, ConfigContext context) {
		org.hibernate.search.annotations.Field fieldAnn =
				member.getAnnotation( org.hibernate.search.annotations.Field.class );
		NumericField numericFieldAnn = member.getAnnotation( NumericField.class );
		DocumentId idAnn = member.getAnnotation( DocumentId.class );
		if ( fieldAnn != null ) {
			bindFieldAnnotation( classHostingMember, member, propertiesMetadata, prefix, fieldAnn, numericFieldAnn, context );
		}
		if ( ( fieldAnn == null && idAnn == null ) && numericFieldAnn != null ) {
			throw new SearchException( "@NumericField without a @Field on property '" + member.getName() + "'" );
		}
	}

	private void checkForContainedIn(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata) {
		ContainedIn containedAnn = member.getAnnotation( ContainedIn.class );
		if ( containedAnn != null ) {
			ReflectionHelper.setAccessible( member );
			propertiesMetadata.containedInGetters.add( member );
			//collection role in Hibernate is made of the actual hosting class of the member (see HSEARCH-780)
			this.containedInCollectionRoles.add( StringHelper.qualify( classHostingMember.getName(), member.getName() ) );
		}
	}

	private void checkForIndexedEmbedded(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata, String prefix,
			Set<XClass> processedClasses, ConfigContext context, Set<XClass> optimizationBlackList, boolean disableOptimizations) {
		IndexedEmbedded embeddedAnn = member.getAnnotation( IndexedEmbedded.class );
		if ( embeddedAnn != null ) {
			//collection role in Hibernate is made of the actual hosting class of the member (see HSEARCH-780)
			this.indexedEmbeddedCollectionRoles.add( StringHelper.qualify( classHostingMember.getName(), member.getName() ) );
			int oldMaxLevel = maxLevel;
			int potentialLevel = embeddedAnn.depth() + level;
			if ( potentialLevel < 0 ) {
				potentialLevel = Integer.MAX_VALUE;
			}
			maxLevel = potentialLevel > maxLevel ? maxLevel : potentialLevel;
			level++;

			XClass elementClass;
			if ( void.class == embeddedAnn.targetElement() ) {
				elementClass = member.getElementClass();
			}
			else {
				elementClass = reflectionManager.toXClass( embeddedAnn.targetElement() );
			}
			
			if ( maxLevel == Integer.MAX_VALUE //infinite
					&& processedClasses.contains( elementClass ) ) {
				throw new SearchException(
						"Circular reference. Duplicate use of "
								+ elementClass.getName()
								+ " in root entity " + beanXClass.getName()
								+ "#" + buildEmbeddedPrefix( prefix, embeddedAnn, member )
				);
			}
			if ( level <= maxLevel ) {
				processedClasses.add( elementClass ); //push

				ReflectionHelper.setAccessible( member );
				propertiesMetadata.embeddedGetters.add( member );
				PropertiesMetadata metadata = new PropertiesMetadata();
				propertiesMetadata.embeddedPropertiesMetadata.add( metadata );
				metadata.boost = getBoost( member, null );
				//property > entity analyzer
				Analyzer analyzer = getAnalyzer( member, context );
				metadata.analyzer = analyzer != null ? analyzer : propertiesMetadata.analyzer;
				String localPrefix = buildEmbeddedPrefix( prefix, embeddedAnn, member );
				
				if ( disableOptimizations ) {
					optimizationBlackList.add( elementClass );
				}
				
				initializeClass( elementClass, metadata, false, localPrefix, processedClasses, context, optimizationBlackList, disableOptimizations );
				/**
				 * We will only index the "expected" type but that's OK, HQL cannot do down-casting either
				 */
				if ( member.isArray() ) {
					propertiesMetadata.embeddedContainers.add( PropertiesMetadata.Container.ARRAY );
				}
				else if ( member.isCollection() ) {
					if ( Map.class.equals( member.getCollectionClass() ) ) {
						//hum subclasses etc etc??
						propertiesMetadata.embeddedContainers.add( PropertiesMetadata.Container.MAP );
					}
					else {
						propertiesMetadata.embeddedContainers.add( PropertiesMetadata.Container.COLLECTION );
					}
				}
				else {
					propertiesMetadata.embeddedContainers.add( PropertiesMetadata.Container.OBJECT );
				}

				processedClasses.remove( elementClass ); //pop
			}
			else if ( log.isTraceEnabled() ) {
				String localPrefix = buildEmbeddedPrefix( prefix, embeddedAnn, member );
				log.trace( "depth reached, ignoring {}", localPrefix );
			}

			level--;
			maxLevel = oldMaxLevel; //set back the the old max level
		}
	}

	private void bindClassBridgeAnnotation(String prefix, PropertiesMetadata propertiesMetadata, ClassBridge ann, XClass clazz, ConfigContext context) {
		String fieldName = prefix + ann.name();
		propertiesMetadata.classNames.add( fieldName );
		propertiesMetadata.classStores.add( ann.store() );
		propertiesMetadata.classIndexes.add( getIndex( ann.index() ) );
		propertiesMetadata.classTermVectors.add( getTermVector( ann.termVector() ) );
		propertiesMetadata.classBridges.add( BridgeFactory.extractType( ann, clazz ) );
		propertiesMetadata.classBoosts.add( ann.boost().value() );

		Analyzer analyzer = getAnalyzer( ann.analyzer(), context );
		if ( analyzer == null ) {
			analyzer = propertiesMetadata.analyzer;
		}
		if ( analyzer == null ) {
			throw new AssertionFailure( "Analyzer should not be undefined" );
		}
		addToScopedAnalyzer( fieldName, analyzer, ann.index() );
	}

	private void bindFieldAnnotation(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata, String prefix, org.hibernate.search.annotations.Field fieldAnn, NumericField numericFieldAnn, ConfigContext context) {
		ReflectionHelper.setAccessible( member );
		propertiesMetadata.fieldGetters.add( member );
		String fieldName = prefix + ReflectionHelper.getAttributeName( member, fieldAnn.name() );
		propertiesMetadata.fieldNames.add( fieldName );
		propertiesMetadata.fieldNameToPositionMap.put(
				member.getName(), Integer.valueOf( propertiesMetadata.fieldNames.size() )
		);
		propertiesMetadata.fieldStore.add( fieldAnn.store() );
		propertiesMetadata.fieldIndex.add( getIndex( fieldAnn.index() ) );
		propertiesMetadata.fieldBoosts.add( getBoost( member, fieldAnn ) );
		propertiesMetadata.dynamicFieldBoosts.add( getDynamicBoost( member ) );
		propertiesMetadata.fieldTermVectors.add( getTermVector( fieldAnn.termVector() ) );
		propertiesMetadata.precisionSteps.add( getPrecisionStep( numericFieldAnn ) );

		// null token
		String indexNullAs = fieldAnn.indexNullAs();
		if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DO_NOT_INDEX_NULL ) ) {
			indexNullAs = null;
		}
		else if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DEFAULT_NULL_TOKEN ) ) {
			indexNullAs = context.getDefaultNullToken();
		}
		propertiesMetadata.fieldNullTokens.add( indexNullAs );

		FieldBridge fieldBridge = BridgeFactory.guessType( fieldAnn, numericFieldAnn, member, reflectionManager );
		if ( indexNullAs != null && fieldBridge instanceof TwoWayFieldBridge ) {
			fieldBridge = new NullEncodingTwoWayFieldBridge( (TwoWayFieldBridge) fieldBridge, indexNullAs );
		}
		propertiesMetadata.fieldBridges.add( fieldBridge );

		// Field > property > entity analyzer
		Analyzer analyzer = getAnalyzer( fieldAnn.analyzer(), context );
		if ( analyzer == null ) {
			analyzer = getAnalyzer( member, context );
		}
		addToScopedAnalyzer( fieldName, analyzer, fieldAnn.index() );
		if ( member.isCollection() ) {
			fieldCollectionRoles.add( StringHelper.qualify( classHostingMember.getName(), member.getName() ) );
		}
	}

	protected Integer getPrecisionStep(NumericField numericFieldAnn) {
		return numericFieldAnn == null ? NumericField.PRECISION_STEP_DEFAULT : numericFieldAnn.precisionStep();
	}

	private String buildEmbeddedPrefix(String prefix, IndexedEmbedded embeddedAnn, XProperty member) {
		String localPrefix = prefix;
		if ( ".".equals( embeddedAnn.prefix() ) ) {
			//default to property name
			localPrefix += member.getName() + '.';
		}
		else {
			localPrefix += embeddedAnn.prefix();
		}
		return localPrefix;
	}

	private float getBoost(XClass element) {
		float boost = 1.0f;
		if ( element == null ) {
			return boost;
		}
		Boost boostAnnotation = element.getAnnotation( Boost.class );
		if ( boostAnnotation != null ) {
			boost = boostAnnotation.value();
		}
		return boost;
	}

	private BoostStrategy getDynamicBoost(XClass element) {
		if ( element == null ) {
			return null;
		}
		DynamicBoost boostAnnotation = element.getAnnotation( DynamicBoost.class );
		if ( boostAnnotation == null ) {
			return new DefaultBoostStrategy();
		}

		Class<? extends BoostStrategy> boostStrategyClass = boostAnnotation.impl();
		BoostStrategy strategy;
		try {
			strategy = boostStrategyClass.newInstance();
		}
		catch ( Exception e ) {
			throw new SearchException(
					"Unable to instantiate boost strategy implementation: " + boostStrategyClass.getName()
			);
		}
		return strategy;
	}

	/**
	 * A {@code XMember } instance treats a map as a collection as well in which case the map values are returned as
	 * collection.
	 *
	 * @param member The member instance
	 * @param value The value
	 *
	 * @return The {@code value} casted to collection or in case of {@code value} being a map the map values as collection.
	 */
	private <T> Collection<T> getActualCollection(XMember member, Object value) {
		Collection<T> collection;
		if ( Map.class.equals( member.getCollectionClass() ) ) {
			//hum
			@SuppressWarnings("unchecked")
			Collection<T> tmpCollection = ( (Map<?, T>) value ).values();
			collection = tmpCollection;
		}
		else {
			@SuppressWarnings("unchecked")
			Collection<T> tmpCollection = (Collection<T>) value;
			collection = tmpCollection;
		}
		return collection;
	}

	private <T> void processSingleContainedInInstance(WorkPlan workplan, T value) {
		workplan.recurseContainedIn( value );
	}

	/**
	 * Hibernate entities might be considered dirty, but still have only changes that
	 * don't affect indexing. So this isDirty() implementation will return true only
	 * if the proposed change is possibly affecting the index.
	 *
	 * @param dirtyPropertyNames Contains the property name of each value which changed, or null for everything.
	 *
	 * @return true if it can't make sure the index doesn't need an update
	 *
	 * @since 3.4
	 */
	public boolean isDirty(String[] dirtyPropertyNames) {
		if ( dirtyPropertyNames == null || dirtyPropertyNames.length == 0 ) {
			return true; // it appears some collection work has no oldState -> reindex
		}
		if ( ! stateInspectionOptimizationsEnabled() ) {
			return true;
		}

		for ( String dirtyPropertyName : dirtyPropertyNames ) {
			// Hibernate core will do an in-depth comparison of collections, taking care of creating new values,
			// so it looks like we can rely on reference equality comparisons, or at least that seems a safe way:
			Integer propertyIndexInteger = metadata.fieldNameToPositionMap.get( dirtyPropertyName );
			if ( propertyIndexInteger != null ) {
				int propertyIndex = propertyIndexInteger.intValue() - 1;

				// take care of indexed fields:
				if ( metadata.fieldIndex.get( propertyIndex ).isIndexed() ) {
					return true;
				}

				// take care of stored fields:
				Store store = metadata.fieldStore.get( propertyIndex );
				if ( store.equals( Store.YES ) || store.equals( Store.COMPRESS ) ) {
					// unless Store.NO, which doesn't affect the index
					return true;
				}
			}

			// consider IndexedEmbedded:
			for ( XMember embeddedMember : metadata.embeddedGetters ) {
				String name = embeddedMember.getName();
				if ( name.equals( dirtyPropertyName ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Wrapper class containing all the meta data extracted out of a single entity.
	 * All field/property related properties are kept in lists. Retrieving all metadata for a given
	 * property/field means accessing all the lists with the same index.
	 */
	protected static class PropertiesMetadata {
		public float boost;
		public Analyzer analyzer;
		public Discriminator discriminator;
		public XMember discriminatorGetter;
		public BoostStrategy classBoostStrategy;
		public final Map<String, Integer> fieldNameToPositionMap = new HashMap<String, Integer>();

		public final List<String> fieldNames = new ArrayList<String>();
		public final List<XMember> fieldGetters = new ArrayList<XMember>();
		public final List<FieldBridge> fieldBridges = new ArrayList<FieldBridge>();
		public final List<Store> fieldStore = new ArrayList<Store>();
		public final List<Field.Index> fieldIndex = new ArrayList<Field.Index>();
		public final List<Float> fieldBoosts = new ArrayList<Float>();
		public final List<BoostStrategy> dynamicFieldBoosts = new ArrayList<BoostStrategy>();
		public final List<Integer> precisionSteps = new ArrayList<Integer>();
		public final List<String> fieldNullTokens = new LinkedList<String>();

		public final List<Field.TermVector> fieldTermVectors = new ArrayList<Field.TermVector>();
		public final List<XMember> embeddedGetters = new ArrayList<XMember>();
		public final List<PropertiesMetadata> embeddedPropertiesMetadata = new ArrayList<PropertiesMetadata>();
		public final List<Container> embeddedContainers = new ArrayList<Container>();
		public final List<XMember> containedInGetters = new ArrayList<XMember>();

		public final List<String> classNames = new ArrayList<String>();
		public final List<Store> classStores = new ArrayList<Store>();
		public final List<Field.Index> classIndexes = new ArrayList<Field.Index>();
		public final List<FieldBridge> classBridges = new ArrayList<FieldBridge>();
		public final List<Field.TermVector> classTermVectors = new ArrayList<Field.TermVector>();
		public final List<Float> classBoosts = new ArrayList<Float>();

		public enum Container {
			OBJECT,
			COLLECTION,
			MAP,
			ARRAY
		}

		protected LuceneOptions getClassLuceneOptions(int i) {
			return new LuceneOptionsImpl(
					classStores.get( i ),
					classIndexes.get( i ),
					classTermVectors.get( i ),
					classBoosts.get( i )
			);
		}

		protected LuceneOptions getFieldLuceneOptions(int i, Object value) {
			LuceneOptions options;
			options = new LuceneOptionsImpl(
					fieldStore.get( i ),
					fieldIndex.get( i ),
					fieldTermVectors.get( i ),
					fieldBoosts.get( i ) * dynamicFieldBoosts.get( i ).defineBoost( value ),
					fieldNullTokens.get( i ),
					precisionSteps.get( i )
			);
			return options;
		}

		protected float getClassBoost(Object value) {
			return boost * classBoostStrategy.defineBoost( value );
		}
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 *
	 * @return true if a providedId needs to be provided for indexing
	 */
	boolean requiresProvidedId() {
		return false;
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 *
	 * @return true if @DocumentId and @Id are found on the same property
	 */
	boolean isIdMatchingJpaId() {
		return true;
	}

	/**
	 * Returns true if the collection event is not going to affect the index state,
	 * so that the indexing event can be skipped.
	 * @param collectionRole
	 * @return true if the collection Role does not affect index state
	 * @since 3.4
	 */
	public boolean isCollectionRoleExcluded(String collectionRole) {
		if ( collectionRole == null ) {
			return false;
		}
		else {
			// don't check stateInspectionOptimizationsEnabled() as it might ignore depth limit:
			// it will disable optimization even if we have classbridges, but we're too deep
			// to be reachable. The evaluation of stateInspectionOptimizationsEnabled() was
			// actually stored in stateInspectionOptimizationsEnabled, but limiting to depth recursion.
			if ( stateInspectionOptimizationsEnabled ) {
				return ! ( this.indexedEmbeddedCollectionRoles.contains( collectionRole )
						|| this.fieldCollectionRoles.contains( collectionRole )
						|| this.containedInCollectionRoles.contains( collectionRole ) );
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * Verifies entity level preconditions to know if it's safe to skip index updates based
	 * on specific field or collection updates.
	 * @return true if it seems safe to apply such optimizations
	 */
	boolean stateInspectionOptimizationsEnabled() {
		if ( ! stateInspectionOptimizationsEnabled ) {
			return false;
		}
		if ( metadata.classBridges.size() > 0 ) {
			log.trace( "State inspection optimization disabled as ClassBridges are enabled on entity {}", this.beanXClassName );
			return false; // can't know what a classBridge is going to look at -> reindex //TODO nice new feature to have?
		}
		if ( !( metadata.classBoostStrategy instanceof DefaultBoostStrategy ) ) {
			log.trace( "State inspection optimization disabled as DynamicBoost is enabled on entity {}", this.beanXClassName );
			return false; // as with classbridge: might be affected by any field //TODO nice new feature to have?
		}
		return true;
	}
	
	/**
	 * Makes sure isCollectionRoleExcluded will always return false, so that
	 * collection update events are always processed.
	 * @see #isCollectionRoleExcluded(String)
	 */
	public void forceStateInspectionOptimizationsDisabled() {
		this.stateInspectionOptimizationsEnabled = false;
	}
}
