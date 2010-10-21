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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.util.ClassLoaderHelper;
import org.hibernate.search.util.HibernateHelper;
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

	protected final PropertiesMetadata metadata = new PropertiesMetadata();
	protected final XClass beanXClass;
	protected final Class<?> beanClass;
	protected Set<Class<?>> mappedSubclasses = new HashSet<Class<?>>();
	protected ReflectionManager reflectionManager; //available only during initialization and post-initialization
	protected int level = 0;
	protected int maxLevel = Integer.MAX_VALUE;
	protected final ScopedAnalyzer analyzer = new ScopedAnalyzer();
	protected Similarity similarity; //there is only 1 similarity per class hierarchy, and only 1 per index
	protected boolean isRoot;
	protected EntityState entityState;
	private Analyzer passThroughAnalyzer = new PassThroughAnalyzer();

	/**
	 * Constructor used on contained entities not annotated with <code>@Indexed</code> themselves.
	 *
	 * @param xClass The class for which to build a <code>DocumentBuilderContainedEntity</code>.
	 * @param context Handle to default configuration settings.
	 * @param reflectionManager Reflection manager to use for processing the annotations.
	 */
	public AbstractDocumentBuilder(XClass xClass, ConfigContext context, ReflectionManager reflectionManager) {

		if ( xClass == null ) {
			throw new AssertionFailure( "Unable to build a DocumentBuilderContainedEntity with a null class" );
		}

		this.entityState = EntityState.CONTAINED_IN_ONLY;
		this.beanXClass = xClass;
		this.reflectionManager = reflectionManager;
		this.beanClass = reflectionManager.toClass( xClass );
		init( xClass, context );

		if ( metadata.containedInGetters.size() == 0 ) {
			this.entityState = EntityState.NON_INDEXABLE;
		}
	}

	abstract public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor);

	abstract protected void subClassSpecificCheck(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context);

	abstract protected void initSubClass(XClass clazz, ConfigContext context);

	public boolean isRoot() {
		return isRoot;
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
	 *
	 * @param instance The instance to be indexed
	 * @param queue the current work queue
	 * @param metadata metadata
	 * @param searchFactoryImplementor the current session
	 */
	protected <T> void processContainedInInstances(Object instance, List<LuceneWork> queue, PropertiesMetadata metadata, SearchFactoryImplementor searchFactoryImplementor) {
		for ( int i = 0; i < metadata.containedInGetters.size(); i++ ) {
			XMember member = metadata.containedInGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( instance, member );

			if ( value == null ) {
				continue;
			}

			if ( member.isArray() ) {
				@SuppressWarnings("unchecked")
				T[] array = ( T[] ) value;
				for ( T arrayValue : array ) {
					processSingleContainedInInstance( queue, searchFactoryImplementor, arrayValue );
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
						 * leads to a LIE because the colleciton is no longer attached to the session
						 *
						 * But that's ok as the collection update event has been processed before
						 * or the fk would have been cleared and thus triggering the cleaning
						 */
						collection = null;
					}
				}
				if ( collection != null ) {
					for ( T collectionValue : collection ) {
						processSingleContainedInInstance( queue, searchFactoryImplementor, collectionValue );
					}
				}
			}
			else {
				processSingleContainedInInstance( queue, searchFactoryImplementor, value );
			}
		}
	}

	private void init(XClass clazz, ConfigContext context) {
		metadata.boost = getBoost( clazz );
		metadata.classBoostStrategy = getDynamicBoost( clazz );
		metadata.analyzer = context.getDefaultAnalyzer();

		Set<XClass> processedClasses = new HashSet<XClass>();
		processedClasses.add( clazz );
		initializeClass( clazz, metadata, true, "", processedClasses, context );

		this.analyzer.setGlobalAnalyzer( metadata.analyzer );

		// set the default similarity in case that after processing all classes there is still no similarity set
		if ( this.similarity == null ) {
			this.similarity = context.getDefaultSimilarity();
		}

		initSubClass( clazz, context );
	}

	private void initializeClass(XClass clazz, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix,
								 Set<XClass> processedClasses, ConfigContext context) {
		List<XClass> hierarchy = new ArrayList<XClass>();
		for ( XClass currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass() ) {
			hierarchy.add( currentClass );
		}

		/*
		* Iterate the class hierarchy top down. This allows to override the default analyzer for the properties if the class holds one
		*/
		for ( int index = hierarchy.size() - 1; index >= 0; index-- ) {
			XClass currentClass = hierarchy.get( index );

			initializeClassLevelAnnotations( currentClass, propertiesMetadata, isRoot, prefix, context );

			// rejecting non properties (ie regular methods) because the object is loaded from Hibernate,
			// so indexing a non property does not make sense
			List<XProperty> methods = currentClass.getDeclaredProperties( XClass.ACCESS_PROPERTY );
			for ( XProperty method : methods ) {
				initializeMemberLevelAnnotations(
						method, propertiesMetadata, isRoot, prefix, processedClasses, context
				);
			}

			List<XProperty> fields = currentClass.getDeclaredProperties( XClass.ACCESS_FIELD );
			for ( XProperty field : fields ) {
				initializeMemberLevelAnnotations(
						field, propertiesMetadata, isRoot, prefix, processedClasses, context
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
				bindClassBridgeAnnotation( prefix, propertiesMetadata, cb, context );
			}
		}

		// Check for any ClassBridge style of annotations.
		ClassBridge classBridgeAnn = clazz.getAnnotation( ClassBridge.class );
		if ( classBridgeAnn != null ) {
			bindClassBridgeAnnotation( prefix, propertiesMetadata, classBridgeAnn, context );
		}

		checkForAnalyzerDiscriminator( clazz, propertiesMetadata );

		// Get similarity
		if ( isRoot ) {
			checkForSimilarity( clazz );
		}
	}

	private void initializeMemberLevelAnnotations(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot,
												  String prefix, Set<XClass> processedClasses, ConfigContext context) {
		checkForField( member, propertiesMetadata, prefix, context );
		checkForFields( member, propertiesMetadata, prefix, context );
		checkForAnalyzerDefs( member, context );
		checkForAnalyzerDiscriminator( member, propertiesMetadata );
		checkForIndexedEmbedded( member, propertiesMetadata, prefix, processedClasses, context );
		checkForContainedIn( member, propertiesMetadata );
		subClassSpecificCheck( member, propertiesMetadata, isRoot, prefix, context );
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
						"Lucene analyzer does not implement " + Analyzer.class.getName() + ": " + analyzerClass.getName(),
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
				context.addAnalyzerDef( def );
			}
		}
		AnalyzerDef def = annotatedElement.getAnnotation( AnalyzerDef.class );
		context.addAnalyzerDef( def );
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
				propertiesMetadata.discriminatorGetter = ( XMember ) annotatedElement;
			}
		}
	}

	private void checkForFields(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, ConfigContext context) {
		org.hibernate.search.annotations.Fields fieldsAnn =
				member.getAnnotation( org.hibernate.search.annotations.Fields.class );
		if ( fieldsAnn != null ) {
			for ( org.hibernate.search.annotations.Field fieldAnn : fieldsAnn.value() ) {
				bindFieldAnnotation( member, propertiesMetadata, prefix, fieldAnn, context );
			}
		}
	}

	private void checkForSimilarity(XClass currClass) {
		org.hibernate.search.annotations.Similarity similarityAnn = currClass.getAnnotation( org.hibernate.search.annotations.Similarity.class );
		if ( similarityAnn != null ) {
			if ( similarity != null ) {
				throw new SearchException(
						"Multiple Similarities defined in the same class hierarchy: " + beanXClass.getName()
				);
			}
			Class<?> similarityClass = similarityAnn.impl();
			try {
				similarity = ( Similarity ) similarityClass.newInstance();
			}
			catch ( Exception e ) {
				log.error(
						"Exception attempting to instantiate Similarity '{}' set for {}",
						similarityClass.getName(), beanXClass.getName()
				);
			}
		}
	}

	private void checkForField(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, ConfigContext context) {
		org.hibernate.search.annotations.Field fieldAnn =
				member.getAnnotation( org.hibernate.search.annotations.Field.class );
		if ( fieldAnn != null ) {
			bindFieldAnnotation( member, propertiesMetadata, prefix, fieldAnn, context );
		}
	}

	private void checkForContainedIn(XProperty member, PropertiesMetadata propertiesMetadata) {
		ContainedIn containedAnn = member.getAnnotation( ContainedIn.class );
		if ( containedAnn != null ) {
			ReflectionHelper.setAccessible( member );
			propertiesMetadata.containedInGetters.add( member );
		}
	}

	private void checkForIndexedEmbedded(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, Set<XClass> processedClasses, ConfigContext context) {
		IndexedEmbedded embeddedAnn = member.getAnnotation( IndexedEmbedded.class );
		if ( embeddedAnn != null ) {
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
				initializeClass( elementClass, metadata, false, localPrefix, processedClasses, context );
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

	private void bindClassBridgeAnnotation(String prefix, PropertiesMetadata propertiesMetadata, ClassBridge ann, ConfigContext context) {
		String fieldName = prefix + ann.name();
		propertiesMetadata.classNames.add( fieldName );
		propertiesMetadata.classStores.add( ann.store() );
		propertiesMetadata.classIndexes.add( getIndex( ann.index() ) );
		propertiesMetadata.classTermVectors.add( getTermVector( ann.termVector() ) );
		propertiesMetadata.classBridges.add( BridgeFactory.extractType( ann ) );
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

	private void bindFieldAnnotation(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, org.hibernate.search.annotations.Field fieldAnn, ConfigContext context) {
		ReflectionHelper.setAccessible( member );
		propertiesMetadata.fieldGetters.add( member );
		String fieldName = prefix + ReflectionHelper.getAttributeName( member, fieldAnn.name() );
		propertiesMetadata.fieldNames.add( fieldName );
		propertiesMetadata.fieldStore.add( fieldAnn.store() );
		propertiesMetadata.fieldIndex.add( getIndex( fieldAnn.index() ) );
		propertiesMetadata.fieldBoosts.add( getBoost( member, fieldAnn ) );
		propertiesMetadata.dynamicFieldBoosts.add( getDynamicBoost( member ) );
		propertiesMetadata.fieldTermVectors.add( getTermVector( fieldAnn.termVector() ) );
		propertiesMetadata.fieldBridges.add( BridgeFactory.guessType( fieldAnn, member, reflectionManager ) );

		// Field > property > entity analyzer
		Analyzer analyzer = getAnalyzer( fieldAnn.analyzer(), context );
		if ( analyzer == null ) {
			analyzer = getAnalyzer( member, context );
		}
		addToScopedAnalyzer( fieldName, analyzer, fieldAnn.index() );
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
			Collection<T> tmpCollection = ( ( Map<?, T> ) value ).values();
			collection = tmpCollection;
		}
		else {
			@SuppressWarnings("unchecked")
			Collection<T> tmpCollection = ( Collection<T> ) value;
			collection = tmpCollection;
		}
		return collection;
	}

	private <T> void processSingleContainedInInstance(List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor, T value) {
		Class<T> valueClass = HibernateHelper.getClass( value );
		DocumentBuilderIndexedEntity<T> builderIndexedEntity =
				searchFactoryImplementor.getDocumentBuilderIndexedEntity( valueClass );

		// it could be we have a nested @IndexedEmbedded chain in which case we have to find the top level @Indexed entities
		if ( builderIndexedEntity == null ) {
			DocumentBuilderContainedEntity<T> builderContainedEntity =
					searchFactoryImplementor.getDocumentBuilderContainedEntity( valueClass );
			if ( builderContainedEntity != null ) {
				processContainedInInstances( value, queue, builderContainedEntity.metadata, searchFactoryImplementor );
			}
		}
		else {
			addWorkForEmbeddedValue( value, queue, valueClass, builderIndexedEntity, searchFactoryImplementor );
		}
	}

	/**
	 * Create a {@code LuceneWork} instance of the entity which needs updating due to the embedded instance change.
	 *
	 * @param value The value to index
	 * @param queue The current (Lucene) work queue
	 * @param valueClass The class of the value
	 * @param builderIndexedEntity the document builder for the entity which needs updating due to a update event of the embedded instance
	 * @param searchFactoryImplementor the search factory.
	 */
	private <T> void addWorkForEmbeddedValue(T value, List<LuceneWork> queue, Class<T> valueClass,
											 DocumentBuilderIndexedEntity<T> builderIndexedEntity, SearchFactoryImplementor searchFactoryImplementor) {
		Serializable id = ( Serializable ) ReflectionHelper.getMemberValue( value, builderIndexedEntity.idGetter );
		if ( id != null ) {
			builderIndexedEntity.addWorkToQueue(
					valueClass, value, id, WorkType.UPDATE, queue, searchFactoryImplementor
			);
		}
		else {
			//this is an indexed entity that is not yet persisted but should be reached by cascade
			// and thus raise an Hibernate Core event leading to its indexing by Hibernate Search
			// => no need to do anything here
		}
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

		public final List<String> fieldNames = new ArrayList<String>();
		public final List<XMember> fieldGetters = new ArrayList<XMember>();
		public final List<FieldBridge> fieldBridges = new ArrayList<FieldBridge>();
		public final List<Store> fieldStore = new ArrayList<Store>();
		public final List<Field.Index> fieldIndex = new ArrayList<Field.Index>();
		public final List<Float> fieldBoosts = new ArrayList<Float>();
		public final List<BoostStrategy> dynamicFieldBoosts = new ArrayList<BoostStrategy>();

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
					classIndexes.get( i ), classTermVectors.get( i ), classBoosts.get( i )
			);
		}

		protected LuceneOptions getFieldLuceneOptions(int i, Object value) {
			LuceneOptions options;
			options = new LuceneOptionsImpl(
					fieldStore.get( i ),
					fieldIndex.get( i ),
					fieldTermVectors.get( i ),
					fieldBoosts.get( i ) * dynamicFieldBoosts.get( i ).defineBoost( value )
			);
			return options;
		}

		protected float getClassBoost(Object value) {
			return boost * classBoostStrategy.defineBoost( value );
		}
	}
}
