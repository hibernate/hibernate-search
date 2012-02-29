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
package org.hibernate.search.engine.spi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.impl.DefaultStringBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingFieldBridge;
import org.hibernate.search.bridge.impl.BridgeFactory;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.AnnotationProcessingHelper;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.engine.impl.FieldMetadata;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.hibernate.search.engine.impl.WorkPlan;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Abstract base class for the document builders.
 *
 * @author Hardy Ferentschik
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
public abstract class AbstractDocumentBuilder<T> {
	private static final Log log = LoggerFactory.make();
	private static final StringBridge NULL_EMBEDDED_STRING_BRIDGE = new DefaultStringBridge();
	private static final String EMPTY = "";

	private final XClass beanXClass;
	protected final String beanXClassName;
	protected final Class<?> beanClass;
	protected final InstanceInitializer instanceInitalizer;
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
	 * Constructor.
	 *
	 * @param xClass The class for which to build a document builder
	 * @param context Handle to default configuration settings
	 * @param similarity The index level similarity
	 * @param reflectionManager Reflection manager to use for processing the annotations
	 * @param optimizationBlackList keeps track of types on which we need to disable collection events optimizations
	 */
	public AbstractDocumentBuilder(XClass xClass, ConfigContext context, Similarity similarity,
			ReflectionManager reflectionManager, Set<XClass> optimizationBlackList, InstanceInitializer instanceInitializer) {

		if ( xClass == null ) {
			throw new AssertionFailure( "Unable to build a DocumentBuilderContainedEntity with a null class" );
		}

		this.instanceInitalizer = instanceInitializer;
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
		initializeClass( xClass, metadata, true, "", processedClasses, context, optimizationBlackList, false, null );

		this.analyzer.setGlobalAnalyzer( metadata.analyzer );

		// set the default similarity in case that after processing all classes there is still no similarity set
		if ( this.similarity == null ) {
			this.similarity = context.getDefaultSimilarity();
		}
	}

	public abstract void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, boolean delete, boolean add, List<LuceneWork> queue, ConversionContext contextualBridge);

	abstract protected void documentBuilderSpecificChecks(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context, PathsContext pathsContext);

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

	public ScopedAnalyzer getAnalyzer() {
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

	protected void addToScopedAnalyzer(String fieldName, Analyzer analyzer, Field.Index index) {
		if ( Field.Index.ANALYZED.equals( index ) || Field.Index.ANALYZED_NO_NORMS.equals( index ) ) {
			if ( analyzer != null ) {
				this.analyzer.addScopedAnalyzer( fieldName, analyzer );
			}
		}
		else {
			// no analyzer is used, add a fake one for queries
			this.analyzer.addScopedAnalyzer( fieldName, passThroughAnalyzer );
		}
	}

	/**
	 * If we have a work instance we have to check whether the instance to be indexed is contained in any other indexed entities.
	 *
	 * @param instance the instance to be indexed
	 * @param workplan the current work plan
	 * @param currentDepth the current {@link DepthValidator} object used to check the graph traversal
	 */
	public void appendContainedInWorkForInstance(Object instance, WorkPlan workplan, DepthValidator currentDepth) {
		for ( int i = 0; i < metadata.containedInGetters.size(); i++ ) {
			XMember member = metadata.containedInGetters.get( i );

			DepthValidator depth = updateDepth( instance, member, currentDepth );
			depth.increaseDepth();
			
			if (depth.isMaxDepthReached())
				return;

			Object value = ReflectionHelper.getMemberValue( instance, member );

			if ( value == null ) {
				continue;
			}

			if ( member.isArray() ) {
				@SuppressWarnings("unchecked")
				T[] array = (T[]) value;
				for ( T arrayValue : array ) {
					processSingleContainedInInstance( workplan, arrayValue, depth );
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
						processSingleContainedInInstance( workplan, collectionValue, depth );
					}
				}
			}
			else {
				processSingleContainedInInstance( workplan, value, depth );
			}
		}
	}

	private DepthValidator updateDepth(Object instance, XMember member, DepthValidator currentDepth) {
		Integer maxDepth = null;
		if ( instance != null ) {
			Map<String, Integer> maxDepths = metadata.containedInDepths;
			String key = depthKey( instance.getClass(), member.getName() );
			maxDepth = maxDepths.get( key );
		}
		if ( maxDepth != null ) {
			if ( currentDepth == null ) {
				return new DepthValidator( maxDepth );
			}
			else {
				int depth = currentDepth.getDepth();
				if ( depth <= maxDepth ) {
					return currentDepth;
				}
				else {
					return new DepthValidator( maxDepth );
				}
			}
		}
		else {
			if ( currentDepth != null ) {
				return currentDepth;
			}
			else {
				return new DepthValidator( Integer.MAX_VALUE );
			}
		}
	}

	private void initializeClass(XClass clazz, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix,
								 Set<XClass> processedClasses, ConfigContext context, Set<XClass> optimizationBlackList,
								 boolean disableOptimizationsArg, PathsContext pathsContext) {
		List<XClass> hierarchy = new LinkedList<XClass>();
		XClass next;
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
		boolean disableOptimizations = disableOptimizationsArg || !stateInspectionOptimizationsEnabled();

		// iterate again for the properties and fields
		for ( XClass currentClass : hierarchy ) {
			// rejecting non properties (ie regular methods) because the object is loaded from Hibernate,
			// so indexing a non property does not make sense
			List<XProperty> methods = currentClass.getDeclaredProperties( XClass.ACCESS_PROPERTY );
			for ( XProperty method : methods ) {
				initializeMemberLevelAnnotations(
						currentClass,
						method,
						propertiesMetadata,
						isRoot,
						prefix,
						processedClasses,
						context,
						optimizationBlackList,
						disableOptimizations,
						pathsContext
				);
			}

			List<XProperty> fields = currentClass.getDeclaredProperties( XClass.ACCESS_FIELD );
			for ( XProperty field : fields ) {
				initializeMemberLevelAnnotations(
						currentClass,
						field,
						propertiesMetadata,
						isRoot,
						prefix,
						processedClasses,
						context,
						optimizationBlackList,
						disableOptimizations,
						pathsContext
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
		Analyzer analyzer = AnnotationProcessingHelper.getAnalyzer( clazz.getAnnotation( org.hibernate.search.annotations.Analyzer.class ), context );
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
												  String prefix, Set<XClass> processedClasses, ConfigContext context, Set<XClass> optimizationBlackList,
												  boolean disableOptimizations, PathsContext pathsContext) {
		checkForField( classHostingMember, member, propertiesMetadata, prefix, context, pathsContext );
		checkForFields( classHostingMember, member, propertiesMetadata, prefix, context, pathsContext );
		checkForAnalyzerDefs( member, context );
		checkForAnalyzerDiscriminator( member, propertiesMetadata );
		checkForIndexedEmbedded(
				classHostingMember,
				member,
				propertiesMetadata,
				prefix,
				processedClasses,
				context,
				optimizationBlackList,
				disableOptimizations,
				pathsContext
		);
		checkForContainedIn( classHostingMember, member, propertiesMetadata );
		documentBuilderSpecificChecks( member, propertiesMetadata, isRoot, prefix, context, pathsContext );
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

	private void checkForFields(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata, String prefix, ConfigContext context, PathsContext pathsContext) {
		org.hibernate.search.annotations.Fields fieldsAnn = member.getAnnotation( org.hibernate.search.annotations.Fields.class );
		NumericFields numericAnns = member.getAnnotation( NumericFields.class );
		if ( fieldsAnn != null ) {
			for ( org.hibernate.search.annotations.Field fieldAnn : fieldsAnn.value() ) {
				if ( isFieldInPath( fieldAnn, member, pathsContext, prefix ) || level <= maxLevel ) {
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
				log.similarityInstantiationException( similarityClass.getName(), beanXClass.getName() );
			}
		}
	}

	private void checkForField(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata, String prefix, ConfigContext context, PathsContext pathsContext) {
		org.hibernate.search.annotations.Field fieldAnn =
				member.getAnnotation( org.hibernate.search.annotations.Field.class );
		NumericField numericFieldAnn = member.getAnnotation( NumericField.class );
		DocumentId idAnn = member.getAnnotation( DocumentId.class );
		if ( fieldAnn != null ) {
			if ( isFieldInPath( fieldAnn, member, pathsContext, prefix ) || level <= maxLevel ) {
				bindFieldAnnotation( classHostingMember, member, propertiesMetadata, prefix, fieldAnn, numericFieldAnn, context );
			}
		}
		if ( ( fieldAnn == null && idAnn == null ) && numericFieldAnn != null ) {
			throw new SearchException( "@NumericField without a @Field on property '" + member.getName() + "'" );
		}
	}

	private boolean isFieldInPath(org.hibernate.search.annotations.Field fieldAnn, XProperty member,
			PathsContext pathsContext, String prefix) {
		if ( pathsContext != null ) {
			String path = prefix + fieldName( fieldAnn, member );
			if ( pathsContext.containsPath( path ) ) {
				pathsContext.markEncounteredPath( path );
				return true;
			}
		}
		return false;
	}

	private String fieldName(org.hibernate.search.annotations.Field fieldAnn, XProperty member) {
		if (fieldAnn == null)
			return member.getName();

		if (fieldAnn.name().isEmpty())
			return member.getName();

		return fieldAnn.name();
	}

	private void checkForContainedIn(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata) {
		ContainedIn containedAnn = member.getAnnotation( ContainedIn.class );
		if ( containedAnn != null ) {
			updateContainedInMaxDepths( member, propertiesMetadata);
			ReflectionHelper.setAccessible( member );
			propertiesMetadata.containedInGetters.add( member );
			//collection role in Hibernate is made of the actual hosting class of the member (see HSEARCH-780)
			this.containedInCollectionRoles
					.add( StringHelper.qualify( classHostingMember.getName(), member.getName() ) );
		}
	}

	private void updateContainedInMaxDepths(XProperty member, PropertiesMetadata propertiesMetadata) {
		updateContainedInMaxDepth( member, propertiesMetadata, XClass.ACCESS_FIELD );
		updateContainedInMaxDepth( member, propertiesMetadata, XClass.ACCESS_PROPERTY );
	}

	private String mappedBy(XMember member) {
		Annotation[] annotations = member.getAnnotations();
		for ( Annotation annotation : annotations ) {
			String mappedBy = mappedBy( annotation );
			if ( StringHelper.isNotEmpty( mappedBy ) )
				return mappedBy;
		}
		return EMPTY;
	}

	private String mappedBy(Annotation annotation) {
		try {
			Method declaredMethod = annotation.annotationType().getDeclaredMethod( "mappedBy" );
			return (String) declaredMethod.invoke( annotation );
		}
		catch ( SecurityException e ) {
			return EMPTY;
		}
		catch ( NoSuchMethodException e ) {
			return EMPTY;
		}
		catch ( IllegalArgumentException e ) {
			return EMPTY;
		}
		catch ( IllegalAccessException e ) {
			return EMPTY;
		}
		catch ( InvocationTargetException e ) {
			return EMPTY;
		}
	}

	private void updateContainedInMaxDepth(XMember memberWithContainedIn, PropertiesMetadata propertiesMetadata, String accessType) {
		XClass memberReturnedType = memberWithContainedIn.getElementClass();
		String mappedBy = mappedBy( memberWithContainedIn );
		List<XProperty> returnedTypeProperties = memberReturnedType.getDeclaredProperties( accessType );
		for ( XProperty property : returnedTypeProperties ) {
			if ( isCorrespondingIndexedEmbedded( mappedBy, property ) ) {
					updateDepthProperties( memberWithContainedIn, propertiesMetadata, memberReturnedType, property );
					break;
				}
			}
		}

	private boolean isCorrespondingIndexedEmbedded(String mappedBy, XProperty property) {
		if ( !property.isAnnotationPresent( IndexedEmbedded.class ) )
			return false;

		if ( mappedBy.isEmpty() )
			return true;

		if ( mappedBy.equals( property.getName() ) )
			return true;

		return false;
	}

	private void updateDepthProperties(XMember memberWithContainedIn, PropertiesMetadata propertiesMetadata, XClass memberReturnedType, XProperty property) {
		int depth = property.getAnnotation( IndexedEmbedded.class ).depth();
		propertiesMetadata.containedInDepths.put( depthKey( memberReturnedType, memberWithContainedIn.getName() ), depth );
	}

	private String depthKey(XClass clazz, String mappedBy) {
		return key( clazz.getName(), mappedBy );
	}

	private String depthKey(Class<?> clazz, String mappedBy) {
		return key( clazz.getName(), mappedBy );
	}

	private String key(String className, String mappedBy) {
		return className + "#" + mappedBy;
	}

	private void checkForIndexedEmbedded(XClass classHostingMember, XProperty member, PropertiesMetadata propertiesMetadata, String prefix,
										 Set<XClass> processedClasses, ConfigContext context, Set<XClass> optimizationBlackList,
										 boolean disableOptimizations, PathsContext pathsContext ) {
		IndexedEmbedded embeddedAnn = member.getAnnotation( IndexedEmbedded.class );
		if ( embeddedAnn != null ) {
			//collection role in Hibernate is made of the actual hosting class of the member (see HSEARCH-780)
			this.indexedEmbeddedCollectionRoles
					.add( StringHelper.qualify( classHostingMember.getName(), member.getName() ) );
			int oldMaxLevel = maxLevel;
			int potentialLevel = depth( embeddedAnn ) + level;
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

			String localPrefix = buildEmbeddedPrefix( prefix, embeddedAnn, member );
			PathsContext updatedPathsContext = updatePaths( localPrefix, pathsContext, embeddedAnn );

			boolean pathsCreatedAtThisLevel = false;
			if ( pathsContext == null && updatedPathsContext != null ) {
				//after this level if not all paths are traversed, then the paths
				//either don't exist in the object graph, or aren't indexed paths
				pathsCreatedAtThisLevel = true;
			}

			if ( level <= maxLevel || isInPath( localPrefix, updatedPathsContext, embeddedAnn ) ) {
				processedClasses.add( elementClass ); //push

				ReflectionHelper.setAccessible( member );
				propertiesMetadata.embeddedGetters.add( member );
				propertiesMetadata.embeddedFieldNames.add( member.getName() );
				PropertiesMetadata metadata = new PropertiesMetadata();
				propertiesMetadata.embeddedPropertiesMetadata.add( metadata );
				metadata.boost = AnnotationProcessingHelper.getBoost( member, null );
				//property > entity analyzer
				Analyzer analyzer = AnnotationProcessingHelper.
						getAnalyzer( member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ), context );
				metadata.analyzer = analyzer != null ? analyzer : propertiesMetadata.analyzer;

				if ( disableOptimizations ) {
					optimizationBlackList.add( elementClass );
				}

				initializeClass(
						elementClass,
						metadata,
						false,
						localPrefix,
						processedClasses,
						context,
						optimizationBlackList,
						disableOptimizations,
						updatedPathsContext
				);
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

				final String indexNullAs = embeddedNullToken( context, embeddedAnn );
				PropertiesMetadata.Container container = propertiesMetadata.embeddedContainers
						.get( propertiesMetadata.embeddedContainers.size() - 1 );
				propertiesMetadata.embeddedNullTokens.add( indexNullAs );
				propertiesMetadata.embeddedNullFields.add( embeddedNullField( localPrefix ) );
				propertiesMetadata.embeddedNullFieldBridges
						.add( guessNullEmbeddedBridge( member, container, indexNullAs ) );
				processedClasses.remove( elementClass ); //pop
			}
			else if ( log.isTraceEnabled() ) {
				log.tracef( "depth reached, ignoring %s", localPrefix );
			}

			level--;
			maxLevel = oldMaxLevel; //set back the the old max level

			if ( pathsCreatedAtThisLevel ) {
				validateAllPathsEncountered( classHostingMember, member, updatedPathsContext );
			}
		}
	}

	private void validateAllPathsEncountered(XClass classHostingMember, XProperty member,
			PathsContext updatedPathsContext) {
		Set<String> unencounteredPaths = updatedPathsContext.getUnencounteredPaths();
		if ( unencounteredPaths.size() > 0 ) {
			StringBuilder sb = new StringBuilder( "Found invalid @IndexedEmbedded->paths configured on class " );
			sb.append( classHostingMember.getName() );
			sb.append( ", member " );
			sb.append( member.getName() );
			sb.append( ": " );

			String prefix = updatedPathsContext.embeddedAnn.prefix();
			for ( String path : unencounteredPaths ) {
				sb.append( removeLeadingPrefixFromPath( path, prefix ) );
				sb.append( ',' );
			}
			String message = sb.substring( 0, sb.length() - 1 );
			throw new SearchException( message );
		}
	}

	private String removeLeadingPrefixFromPath(String path, String prefix) {
		if ( path.startsWith( prefix ) )
			return path.substring( prefix.length() );

		return path;
	}

	private int depth(IndexedEmbedded embeddedAnn) {
		if ( isDepthNotSet( embeddedAnn ) && embeddedAnn.includePaths().length > 0 )
			return 0;

		return embeddedAnn.depth();
	}

	private boolean isDepthNotSet(IndexedEmbedded embeddedAnn) {
		return Integer.MAX_VALUE == embeddedAnn.depth();
	}

	private PathsContext updatePaths(String localPrefix, PathsContext pathsContext, IndexedEmbedded embeddedAnn) {
		if ( pathsContext != null )
			return pathsContext;

		PathsContext newPathsContext = new PathsContext( embeddedAnn );
		for ( String path : embeddedAnn.includePaths() ) {
			newPathsContext.addPath( localPrefix + path );
		}
		return newPathsContext;
	}

	private boolean isInPath(String localPrefix, PathsContext pathsContext, IndexedEmbedded embeddedAnn) {
		if ( pathsContext != null ) {
			boolean defaultPrefix = isDefaultPrefix( embeddedAnn );
			for ( String path : pathsContext.pathsEncounteredState.keySet() ) {
				String app = path;
				if ( defaultPrefix )
					app += ".";
				if ( app.startsWith( localPrefix ) )
					return true;
			}
		}
		return false;
	}

	private FieldBridge guessNullEmbeddedBridge(XProperty member, PropertiesMetadata.Container container, final String indexNullAs) {
		if ( indexNullAs == null ) {
			return null;
		}

		if ( PropertiesMetadata.Container.OBJECT == container ) {
			return new NullEncodingFieldBridge( NULL_EMBEDDED_STRING_BRIDGE, indexNullAs );
		}
		else {
			NumericField numericField = member.getAnnotation( NumericField.class );
			FieldBridge fieldBridge = BridgeFactory.guessType( null, numericField, member, reflectionManager );
			if ( fieldBridge instanceof StringBridge ) {
				fieldBridge = new NullEncodingFieldBridge( (StringBridge) fieldBridge, indexNullAs );
			}
			return fieldBridge;
		}
	}

	private void bindClassBridgeAnnotation(String prefix, PropertiesMetadata propertiesMetadata, ClassBridge ann, XClass clazz, ConfigContext context) {
		String fieldName = prefix + ann.name();
		propertiesMetadata.classNames.add( fieldName );
		propertiesMetadata.classStores.add( ann.store() );
		Field.Index index = AnnotationProcessingHelper.getIndex( ann.index(), ann.analyze(), ann.norms() );
		propertiesMetadata.classIndexes.add( index );
		propertiesMetadata.classTermVectors.add( AnnotationProcessingHelper.getTermVector( ann.termVector() ) );
		propertiesMetadata.classBridges.add( BridgeFactory.extractType( ann, clazz ) );
		propertiesMetadata.classBoosts.add( ann.boost().value() );

		Analyzer analyzer = AnnotationProcessingHelper.getAnalyzer( ann.analyzer(), context );
		if ( analyzer == null ) {
			analyzer = propertiesMetadata.analyzer;
		}
		if ( analyzer == null ) {
			throw new AssertionFailure( "Analyzer should not be undefined" );
		}
		addToScopedAnalyzer( fieldName, analyzer, index );
	}

	private void bindFieldAnnotation(XClass classHostingMember,
									 XProperty member,
									 PropertiesMetadata propertiesMetadata,
									 String prefix,
									 org.hibernate.search.annotations.Field fieldAnnotation,
									 NumericField numericFieldAnnotation,
									 ConfigContext context) {
		FieldMetadata fieldMetadata = new FieldMetadata(  prefix, member, fieldAnnotation, numericFieldAnnotation, context, reflectionManager );
		fieldMetadata.appendToPropertiesMetadata(propertiesMetadata);
		addToScopedAnalyzer( fieldMetadata.getFieldName(), fieldMetadata.getAnalyzer(), fieldMetadata.getIndex() );
		
		if ( member.isCollection() ) {
			fieldCollectionRoles.add( StringHelper.qualify( classHostingMember.getName(), member.getName() ) );
		}
	}

	protected Integer getPrecisionStep(NumericField numericFieldAnn) {
		return numericFieldAnn == null ? NumericField.PRECISION_STEP_DEFAULT : numericFieldAnn.precisionStep();
	}

	private String buildEmbeddedPrefix(String prefix, IndexedEmbedded embeddedAnn, XProperty member) {
		String localPrefix = prefix;
		if ( isDefaultPrefix( embeddedAnn ) ) {
			//default to property name
			localPrefix += member.getName() + '.';
		}
		else {
			localPrefix += embeddedAnn.prefix();
		}
		return localPrefix;
	}

	private boolean isDefaultPrefix(IndexedEmbedded embeddedAnn) {
		return ".".equals( embeddedAnn.prefix() );
	}

	private String embeddedNullField(String localPrefix) {
		if ( localPrefix.endsWith( "." ) ) {
			return localPrefix.substring( 0, localPrefix.length() - 1 );
		}
		return localPrefix;
	}

	private String embeddedNullToken(ConfigContext context, IndexedEmbedded embeddedAnn) {
		String indexNullAs = embeddedAnn.indexNullAs();
		if ( org.hibernate.search.annotations.IndexedEmbedded.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			return null;
		}
		if ( org.hibernate.search.annotations.IndexedEmbedded.DEFAULT_NULL_TOKEN.equals( indexNullAs ) ) {
			return context.getDefaultNullToken();
		}
		return indexNullAs;
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
	 * @return The {@code value} cast to collection or in case of {@code value} being a map the map values as collection.
	 */
	@SuppressWarnings("unchecked")
	private <T> Collection<T> getActualCollection(XMember member, Object value) {
		Collection<T> collection;
		if ( Map.class.equals( member.getCollectionClass() ) ) {
			collection = ( (Map<?, T>) value ).values();
		}
		else {
			collection = (Collection<T>) value;
		}
		return collection;
	}

	private <T> void processSingleContainedInInstance(WorkPlan workplan, T value, DepthValidator depth) {
		workplan.recurseContainedIn( value, depth );
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
		if ( !stateInspectionOptimizationsEnabled() ) {
			return true;
		}

		for ( String dirtyPropertyName : dirtyPropertyNames ) {
			// Hibernate core will do an in-depth comparison of collections, taking care of creating new values,
			// so it looks like we can rely on reference equality comparisons, or at least that seems a safe way:
			Integer propertyIndexInteger = metadata.fieldNameToPositionMap.get( dirtyPropertyName );
			if ( propertyIndexInteger != null ) {
				int propertyIndex = propertyIndexInteger - 1;

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
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 *
	 * @return true if a providedId needs to be provided for indexing
	 */
	public boolean requiresProvidedId() {
		return false;
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 *
	 * @return true if @DocumentId and @Id are found on the same property
	 */
	public boolean isIdMatchingJpaId() {
		return true;
	}

	/**
	 * Returns true if the collection event is not going to affect the index state,
	 * so that the indexing event can be skipped.
	 *
	 * @param collectionRole
	 *
	 * @return true if the collection Role does not affect index state
	 *
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
				return !( this.fieldCollectionRoles.contains( collectionRole )
						|| this.indexedEmbeddedCollectionRoles.contains( collectionRole )
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
	 *
	 * @return true if it seems safe to apply such optimizations
	 */
	boolean stateInspectionOptimizationsEnabled() {
		if ( !stateInspectionOptimizationsEnabled ) {
			return false;
		}
		if ( metadata.classBridges.size() > 0 ) {
			log.tracef(
					"State inspection optimization disabled as ClassBridges are enabled on entity %s",
					this.beanXClassName
			);
			return false; // can't know what a classBridge is going to look at -> reindex //TODO nice new feature to have?
		}
		if ( !( metadata.classBoostStrategy instanceof DefaultBoostStrategy ) ) {
			log.tracef(
					"State inspection optimization disabled as DynamicBoost is enabled on entity %s",
					this.beanXClassName
			);
			return false; // as with classbridge: might be affected by any field //TODO nice new feature to have?
		}
		return true;
	}

	/**
	 * Makes sure isCollectionRoleExcluded will always return false, so that
	 * collection update events are always processed.
	 *
	 * @see #isCollectionRoleExcluded(String)
	 */
	public void forceStateInspectionOptimizationsDisabled() {
		this.stateInspectionOptimizationsEnabled = false;
	}

	/**
	 * Closes any resource
	 */
	public void close() {
		analyzer.close();
	}

	/**
	 * Container class for information about the current set of paths as
	 * well as tracking which paths have been encountered to validate the
	 * existence of all configured paths.
	 */
	static class PathsContext {

		private final IndexedEmbedded embeddedAnn;
		private final Map<String, Boolean> pathsEncounteredState = new HashMap<String, Boolean>();

		public PathsContext(IndexedEmbedded embeddedAnn) {
			this.embeddedAnn = embeddedAnn;
		}

		public boolean containsPath(String path) {
			return pathsEncounteredState.keySet().contains( path );
		}

		public void addPath(String path) {
			pathsEncounteredState.put( path, Boolean.FALSE );
		}

		public void markEncounteredPath(String path) {
			pathsEncounteredState.put( path, Boolean.TRUE );
		}

		public Set<String> getUnencounteredPaths() {
			Set<String> unencounteredPaths = new HashSet<String>();
			for ( String path : pathsEncounteredState.keySet() ) {
				if ( notEncountered( path ) ) {
					unencounteredPaths.add( path );
				}
			}
			return unencounteredPaths;
		}

		private boolean notEncountered(String path) {
			return !pathsEncounteredState.get( path );
		}
	}

	/**
	 * Wrapper class containing all the meta data extracted out of a single entity.
	 * All field/property related properties are kept in lists. Retrieving all metadata for a given
	 * property/field means accessing all the lists with the same index.
	 */
	public static class PropertiesMetadata {
		public float boost;
		public Analyzer analyzer;
		public Discriminator discriminator;
		public XMember discriminatorGetter;
		public BoostStrategy classBoostStrategy;
		public final Map<String, Integer> fieldNameToPositionMap = new HashMap<String, Integer>();

		public final List<String> fieldNames = new ArrayList<String>();
		public final List<XMember> fieldGetters = new ArrayList<XMember>();
		public final List<String> fieldGetterNames = new ArrayList<String>();
		public final List<FieldBridge> fieldBridges = new ArrayList<FieldBridge>();
		public final List<Store> fieldStore = new ArrayList<Store>();
		public final List<Field.Index> fieldIndex = new ArrayList<Field.Index>();
		public final List<Float> fieldBoosts = new ArrayList<Float>();
		public final List<BoostStrategy> dynamicFieldBoosts = new ArrayList<BoostStrategy>();
		public final List<Integer> precisionSteps = new ArrayList<Integer>();
		public final List<String> fieldNullTokens = new LinkedList<String>();

		public final List<Field.TermVector> fieldTermVectors = new ArrayList<Field.TermVector>();
		public final List<XMember> embeddedGetters = new ArrayList<XMember>();
		public final List<String> embeddedFieldNames = new ArrayList<String>();
		public final List<String> embeddedNullTokens = new ArrayList<String>();
		public final List<String> embeddedNullFields = new ArrayList<String>();
		public final List<FieldBridge> embeddedNullFieldBridges = new ArrayList<FieldBridge>();
		public final List<PropertiesMetadata> embeddedPropertiesMetadata = new ArrayList<PropertiesMetadata>();
		public final List<Container> embeddedContainers = new ArrayList<Container>();
		public final List<XMember> containedInGetters = new ArrayList<XMember>();
		public final Map<String, Integer> containedInDepths = new HashMap<String, Integer>();

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
}

