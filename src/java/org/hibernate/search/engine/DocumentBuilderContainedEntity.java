//$Id$
package org.hibernate.search.engine;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Similarity;
import org.slf4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.impl.InitContext;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.ReflectionHelper;
import org.hibernate.search.util.ScopedAnalyzer;

/**
 * Set up and provide a manager for indexed classes.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilderContainedEntity<T> {
	private static final Logger log = LoggerFactory.make();

	public static final String CLASS_FIELDNAME = "_hibernate_class";

	protected final PropertiesMetadata metadata = new PropertiesMetadata();
	protected final XClass beanClass;
	protected String idKeywordName;

	/**
	 * Flag indicating whether <code>@DocumentId</code> was explicitly specified.
	 */
	protected boolean explicitDocumentId = false;

	/**
	 * Flag indicating whether {@link org.apache.lucene.search.Searcher#doc(int, org.apache.lucene.document.FieldSelector)}
	 * can be used in order to retrieve documents. This is only safe to do if we know that
	 * all involved bridges are implementing <code>TwoWayStringBridge</code>. See HSEARCH-213.
	 */
	private boolean allowFieldSelectionInProjection = false;

	protected XMember idGetter;
	protected Float idBoost;
	protected TwoWayFieldBridge idBridge;
	protected Set<Class<?>> mappedSubclasses = new HashSet<Class<?>>();
	private ReflectionManager reflectionManager; //available only during initializationa nd post-initialization
	protected int level = 0;
	protected int maxLevel = Integer.MAX_VALUE;
	protected final ScopedAnalyzer analyzer = new ScopedAnalyzer();
	protected Similarity similarity;
	protected boolean isRoot;
	//if composite id, use of (a, b) in ((1,2), (3,4)) fails on most database
	private boolean safeFromTupleId;
	protected boolean idProvided = false;
	protected EntityState entityState;

	/**
	 * Constructor used on contained entities not annotated with <code>@Indexed</code> themselves.
	 *
	 * @param clazz The class for which to build a <code>DocumentBuilderContainedEntity</code>.
	 * @param context Handle to default configuration settings.
	 * @param reflectionManager Reflection manager to use for processing the annotations.
	 */
	public DocumentBuilderContainedEntity(XClass clazz, InitContext context, ReflectionManager reflectionManager) {

		if ( clazz == null ) {
			throw new AssertionFailure( "Unable to build a DocumentBuilderContainedEntity with a null class" );
		}

		this.entityState = EntityState.CONTAINED_IN_ONLY;
		this.beanClass = clazz;
		this.reflectionManager = reflectionManager;

		init( clazz, context );

		if ( this.similarity == null ) {
			this.similarity = context.getDefaultSimilarity();
		}

		if ( metadata.containedInGetters.size() == 0 ) {
			this.entityState = EntityState.NON_INDEXABLE;
		}
	}

	private void init(XClass clazz, InitContext context) {
		metadata.boost = getBoost( clazz );
		metadata.analyzer = context.getDefaultAnalyzer();

		Set<XClass> processedClasses = new HashSet<XClass>();
		processedClasses.add( clazz );
		initializeMembers( clazz, metadata, true, "", processedClasses, context );

		this.analyzer.setGlobalAnalyzer( metadata.analyzer );

		//if composite id, use of (a, b) in ((1,2)TwoWayString2FieldBridgeAdaptor, (3,4)) fails on most database
		//a TwoWayString2FieldBridgeAdaptor is never a composite id
		safeFromTupleId = entityState != EntityState.INDEXED || TwoWayString2FieldBridgeAdaptor.class.isAssignableFrom(
				idBridge.getClass()
		);

		checkAllowFieldSelection();
		if ( log.isDebugEnabled() ) {
			log.debug(
					"Field selection in projections is set to {} for entity {}.",
					allowFieldSelectionInProjection,
					clazz.getName()
			);
		}
	}

	/**
	 * Checks whether all involved bridges are two way string bridges. If so we can optimize document retrieval
	 * by using <code>FieldSelector</code>. See HSEARCH-213.
	 */
	private void checkAllowFieldSelection() {
		allowFieldSelectionInProjection = true;
		if ( !( idBridge instanceof TwoWayStringBridge || idBridge instanceof TwoWayString2FieldBridgeAdaptor ) ) {
			allowFieldSelectionInProjection = false;
			return;
		}
		for ( FieldBridge bridge : metadata.fieldBridges ) {
			if ( !( bridge instanceof TwoWayStringBridge || bridge instanceof TwoWayString2FieldBridgeAdaptor ) ) {
				allowFieldSelectionInProjection = false;
				return;
			}
		}
	}

	public boolean isRoot() {
		return isRoot;
	}

	public boolean allowFieldSelectionInProjection() {
		return allowFieldSelectionInProjection;
	}

	private Analyzer getAnalyzer(XAnnotatedElement annotatedElement, InitContext context) {
		org.hibernate.search.annotations.Analyzer analyzerAnn =
				annotatedElement.getAnnotation( org.hibernate.search.annotations.Analyzer.class );
		return getAnalyzer( analyzerAnn, context );
	}

	private Analyzer getAnalyzer(org.hibernate.search.annotations.Analyzer analyzerAnn, InitContext context) {
		Class analyzerClass = analyzerAnn == null ? void.class : analyzerAnn.impl();
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
				return ( Analyzer ) analyzerClass.newInstance();
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

	private void initializeMembers(XClass clazz, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix,
								   Set<XClass> processedClasses, InitContext context) {
		List<XClass> hierarchy = new ArrayList<XClass>();
		for ( XClass currClass = clazz; currClass != null; currClass = currClass.getSuperclass() ) {
			hierarchy.add( currClass );
		}
		for ( int index = hierarchy.size() - 1; index >= 0; index-- ) {
			XClass currClass = hierarchy.get( index );
			/*
			 * Override the default analyzer for the properties if the class hold one
			 * That's the reason we go down the hierarchy
			 */
			Analyzer analyzer = getAnalyzer( currClass, context );

			if ( analyzer != null ) {
				propertiesMetadata.analyzer = analyzer;
			}
			checkForAnalyzerDefs( currClass, context );

			// Check for any ClassBridges annotation.
			ClassBridges classBridgesAnn = currClass.getAnnotation( ClassBridges.class );
			if ( classBridgesAnn != null ) {
				ClassBridge[] cbs = classBridgesAnn.value();
				for ( ClassBridge cb : cbs ) {
					bindClassAnnotation( prefix, propertiesMetadata, cb, context );
				}
			}

			// Check for any ClassBridge style of annotations.
			ClassBridge classBridgeAnn = currClass.getAnnotation( ClassBridge.class );
			if ( classBridgeAnn != null ) {
				bindClassAnnotation( prefix, propertiesMetadata, classBridgeAnn, context );
			}

			//Get similarity
			//TODO: similarity form @IndexedEmbedded are not taken care of. Exception??
			if ( isRoot ) {
				checkForSimilarity( currClass );
			}

			// rejecting non properties (ie regular methods) because the object is loaded from Hibernate,
			// so indexing a non property does not make sense
			List<XProperty> methods = currClass.getDeclaredProperties( XClass.ACCESS_PROPERTY );
			for ( XProperty method : methods ) {
				initializeMember( method, propertiesMetadata, isRoot, prefix, processedClasses, context );
			}

			List<XProperty> fields = currClass.getDeclaredProperties( XClass.ACCESS_FIELD );
			for ( XProperty field : fields ) {
				initializeMember( field, propertiesMetadata, isRoot, prefix, processedClasses, context );
			}
		}
	}

	private void initializeMember(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot,
								  String prefix, Set<XClass> processedClasses, InitContext context) {
		checkDocumentId( member, propertiesMetadata, isRoot, prefix, context );
		checkForField( member, propertiesMetadata, prefix, context );
		checkForFields( member, propertiesMetadata, prefix, context );
		checkForAnalyzerDefs( member, context );
		checkForIndexedEmbedded( member, propertiesMetadata, prefix, processedClasses, context );
		checkForContainedIn( member, propertiesMetadata );
	}

	private void checkForAnalyzerDefs(XAnnotatedElement annotatedElement, InitContext context) {
		AnalyzerDefs defs = annotatedElement.getAnnotation( AnalyzerDefs.class );
		if ( defs != null ) {
			for ( AnalyzerDef def : defs.value() ) {
				context.addAnalyzerDef( def );
			}
		}
		AnalyzerDef def = annotatedElement.getAnnotation( AnalyzerDef.class );
		context.addAnalyzerDef( def );
	}

	public String getIdentifierName() {
		return idGetter.getName();
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	private void checkForFields(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, InitContext context) {
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
						"Multiple Similarities defined in the same class hierarchy: " + beanClass.getName()
				);
			}
			Class similarityClass = similarityAnn.impl();
			try {
				similarity = ( Similarity ) similarityClass.newInstance();
			}
			catch ( Exception e ) {
				log.error(
						"Exception attempting to instantiate Similarity '{}' set for {}",
						similarityClass.getName(), beanClass.getName()
				);
			}
		}
	}

	private void checkForField(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, InitContext context) {
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

	private void checkForIndexedEmbedded(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, Set<XClass> processedClasses, InitContext context) {
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
								+ " in root entity " + beanClass.getName()
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
				initializeMembers( elementClass, metadata, false, localPrefix, processedClasses, context );
				/**
				 * We will only index the "expected" type but that's OK, HQL cannot do downcasting either
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

	private void checkDocumentId(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, InitContext context) {
		Annotation idAnnotation = getIdAnnotation( member, context );
		if ( idAnnotation != null ) {
			String attributeName = getIdAttributeName( member, idAnnotation );
			if ( isRoot ) {
				if ( idKeywordName != null && explicitDocumentId ) {
					throw new AssertionFailure(
							"Two document id assigned: "
									+ idKeywordName + " and " + attributeName
					);
				}
				idKeywordName = prefix + attributeName;
				FieldBridge fieldBridge = BridgeFactory.guessType( null, member, reflectionManager );
				if ( fieldBridge instanceof TwoWayFieldBridge ) {
					idBridge = ( TwoWayFieldBridge ) fieldBridge;
				}
				else {
					throw new SearchException(
							"Bridge for document id does not implement TwoWayFieldBridge: " + member.getName()
					);
				}
				idBoost = getBoost( member, null );
				ReflectionHelper.setAccessible( member );
				idGetter = member;
			}
			else {
				//component should index their document id
				ReflectionHelper.setAccessible( member );
				propertiesMetadata.fieldGetters.add( member );
				String fieldName = prefix + attributeName;
				propertiesMetadata.fieldNames.add( fieldName );
				propertiesMetadata.fieldStore.add( getStore( Store.YES ) );
				propertiesMetadata.fieldIndex.add( getIndex( Index.UN_TOKENIZED ) );
				propertiesMetadata.fieldTermVectors.add( getTermVector( TermVector.NO ) );
				propertiesMetadata.fieldBridges.add( BridgeFactory.guessType( null, member, reflectionManager ) );
				propertiesMetadata.fieldBoosts.add( getBoost( member, null ) );
				// property > entity analyzer (no field analyzer)
				Analyzer analyzer = getAnalyzer( member, context );
				if ( analyzer == null ) {
					analyzer = propertiesMetadata.analyzer;
				}
				if ( analyzer == null ) {
					throw new AssertionFailure( "Analizer should not be undefined" );
				}
				this.analyzer.addScopedAnalyzer( fieldName, analyzer );
			}
		}
	}

	/**
	 * Checks whether the specified property contains an annotation used as document id.
	 * This can either be an explicit <code>@DocumentId</code> or if no <code>@DocumentId</code> is specified a
	 * JPA <code>@Id</code> annotation. The check for the JPA annotation is indirectly to avoid a hard dependency
	 * to Hibernate Annotations.
	 *
	 * @param member the property to check for the id annotation.
	 * @param context Handle to default configuration settings.
	 *
	 * @return the annotation used as document id or <code>null</code> if id annotation is specified on the property.
	 */
	private Annotation getIdAnnotation(XProperty member, InitContext context) {
		// check for explicit DocumentId
		Annotation documentIdAnn = member.getAnnotation( DocumentId.class );
		if ( documentIdAnn != null ) {
			explicitDocumentId = true;
			return documentIdAnn;
		}

		// check for JPA @Id
		if ( !explicitDocumentId && context.isJpaPresent() ) {
			Class idClass;
			try {
				idClass = org.hibernate.util.ReflectHelper.classForName( "javax.persistence.Id", InitContext.class );
			}
			catch ( ClassNotFoundException e ) {
				throw new SearchException( "Unable to load @Id.class even though it should be present ?!" );
			}
			documentIdAnn = member.getAnnotation( idClass );
			if ( documentIdAnn != null ) {
				log.debug( "Found JPA id and using it as document id" );
			}
		}
		return documentIdAnn;
	}

	/**
	 * Determines the property name for the document id. It is either the name of the property itself or the
	 * value of the name attribute of the <code>idAnnotation</code>.
	 *
	 * @param member the property used as id property.
	 * @param idAnnotation the id annotation
	 *
	 * @return property name to be used as document id.
	 */
	private String getIdAttributeName(XProperty member, Annotation idAnnotation) {
		String name = null;
		try {
			Method m = idAnnotation.getClass().getMethod( "name" );
			name = ( String ) m.invoke( idAnnotation );
		}
		catch ( Exception e ) {
			// ignore
		}

		return ReflectionHelper.getAttributeName( member, name );
	}

	private void bindClassAnnotation(String prefix, PropertiesMetadata propertiesMetadata, ClassBridge ann, InitContext context) {
		//FIXME name should be prefixed
		String fieldName = prefix + ann.name();
		propertiesMetadata.classNames.add( fieldName );
		propertiesMetadata.classStores.add( getStore( ann.store() ) );
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
		this.analyzer.addScopedAnalyzer( fieldName, analyzer );
	}

	private void bindFieldAnnotation(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, org.hibernate.search.annotations.Field fieldAnn, InitContext context) {
		ReflectionHelper.setAccessible( member );
		propertiesMetadata.fieldGetters.add( member );
		String fieldName = prefix + ReflectionHelper.getAttributeName( member, fieldAnn.name() );
		propertiesMetadata.fieldNames.add( fieldName );
		propertiesMetadata.fieldStore.add( getStore( fieldAnn.store() ) );
		propertiesMetadata.fieldIndex.add( getIndex( fieldAnn.index() ) );
		propertiesMetadata.fieldBoosts.add( getBoost( member, fieldAnn ) );
		propertiesMetadata.fieldTermVectors.add( getTermVector( fieldAnn.termVector() ) );
		propertiesMetadata.fieldBridges.add( BridgeFactory.guessType( fieldAnn, member, reflectionManager ) );

		// Field > property > entity analyzer
		Analyzer analyzer = getAnalyzer( fieldAnn.analyzer(), context );
		if ( analyzer == null ) {
			analyzer = getAnalyzer( member, context );
		}
		if ( analyzer != null ) {
			this.analyzer.addScopedAnalyzer( fieldName, analyzer );
		}
	}

	private Float getBoost(XProperty member, org.hibernate.search.annotations.Field fieldAnn) {
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

	private Field.Store getStore(Store store) {
		switch ( store ) {
			case NO:
				return Field.Store.NO;
			case YES:
				return Field.Store.YES;
			case COMPRESS:
				return Field.Store.COMPRESS;
			default:
				throw new AssertionFailure( "Unexpected Store: " + store );
		}
	}

	private Field.TermVector getTermVector(TermVector vector) {
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

	private Field.Index getIndex(Index index) {
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

	private Float getBoost(XClass element) {
		if ( element == null ) {
			return null;
		}
		Boost boost = element.getAnnotation( Boost.class );
		return boost != null ?
				boost.value() :
				null;
	}

	//TODO could we use T instead of EntityClass?
	public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		/**
		 * When references are changed, either null or another one, we expect dirty checking to be triggered (both sides
		 * have to be updated)
		 * When the internal object is changed, we apply the {Add|Update}Work on containedIns
		 */
		if ( workType.searchForContainers() ) {
			processContainedIn( entity, queue, metadata, searchFactoryImplementor );
		}
	}

	protected void processContainedIn(Object instance, List<LuceneWork> queue, PropertiesMetadata metadata, SearchFactoryImplementor searchFactoryImplementor) {
		for ( int i = 0; i < metadata.containedInGetters.size(); i++ ) {
			XMember member = metadata.containedInGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( instance, member );
			if ( value == null ) {
				continue;
			}

			if ( member.isArray() ) {
				for ( Object arrayValue : ( Object[] ) value ) {
					//highly inneficient but safe wrt the actual targeted class
					Class<?> valueClass = Hibernate.getClass( arrayValue );
					DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity( valueClass );
					if ( builderIndexedEntity == null ) {
						continue;
					}
					processContainedInValue( arrayValue, queue, valueClass,
							builderIndexedEntity, searchFactoryImplementor );
				}
			}
			else if ( member.isCollection() ) {
				Collection collection;
				if ( Map.class.equals( member.getCollectionClass() ) ) {
					//hum
					collection = ( ( Map ) value ).values();
				}
				else {
					collection = ( Collection ) value;
				}
				for ( Object collectionValue : collection ) {
					//highly inneficient but safe wrt the actual targeted class
					Class<?> valueClass = Hibernate.getClass( collectionValue );
					DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity( valueClass );
					if ( builderIndexedEntity == null ) {
						continue;
					}
					processContainedInValue( collectionValue, queue, valueClass,
							builderIndexedEntity, searchFactoryImplementor );
				}
			}
			else {
				Class<?> valueClass = Hibernate.getClass( value );
				DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity( valueClass );
				if ( builderIndexedEntity == null ) {
					continue;
				}
				processContainedInValue( value, queue, valueClass, builderIndexedEntity, searchFactoryImplementor );
			}
		}
		//an embedded cannot have a useful @ContainedIn (no shared reference)
		//do not walk through them
	}

	private void processContainedInValue(Object value, List<LuceneWork> queue, Class<?> valueClass,
										 DocumentBuilderIndexedEntity builderIndexedEntity, SearchFactoryImplementor searchFactoryImplementor) {
		Serializable id = ( Serializable ) ReflectionHelper.getMemberValue( value, builderIndexedEntity.idGetter );
		builderIndexedEntity.addWorkToQueue( valueClass, value, id, WorkType.UPDATE, queue, searchFactoryImplementor );
	}

	public Document getDocument(T instance, Serializable id) {
		Document doc = new Document();
		final Class<?> entityType = Hibernate.getClass( instance );
		//XClass instanceClass = reflectionManager.toXClass( entityType );
		if ( metadata.boost != null ) {
			doc.setBoost( metadata.boost );
		}
		{
			Field classField =
					new Field(
							CLASS_FIELDNAME,
							entityType.getName(),
							Field.Store.YES,
							Field.Index.NOT_ANALYZED,
							Field.TermVector.NO
					);
			doc.add( classField );
			LuceneOptions luceneOptions = new LuceneOptionsImpl(
					Field.Store.YES,
					Field.Index.NOT_ANALYZED, Field.TermVector.NO, idBoost
			);
			idBridge.set( idKeywordName, id, doc, luceneOptions );
		}
		buildDocumentFields( instance, doc, metadata );
		return doc;
	}

	private void buildDocumentFields(Object instance, Document doc, PropertiesMetadata propertiesMetadata) {
		if ( instance == null ) {
			return;
		}
		//needed for field access: I cannot work in the proxied version
		Object unproxiedInstance = unproxy( instance );
		for ( int i = 0; i < propertiesMetadata.classBridges.size(); i++ ) {
			FieldBridge fb = propertiesMetadata.classBridges.get( i );
			fb.set(
					propertiesMetadata.classNames.get( i ), unproxiedInstance,
					doc, propertiesMetadata.getClassLuceneOptions( i )
			);
		}
		for ( int i = 0; i < propertiesMetadata.fieldNames.size(); i++ ) {
			XMember member = propertiesMetadata.fieldGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );
			propertiesMetadata.fieldBridges.get( i ).set(
					propertiesMetadata.fieldNames.get( i ), value, doc,
					propertiesMetadata.getFieldLuceneOptions( i )
			);
		}
		for ( int i = 0; i < propertiesMetadata.embeddedGetters.size(); i++ ) {
			XMember member = propertiesMetadata.embeddedGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );
			//TODO handle boost at embedded level: already stored in propertiesMedatada.boost

			if ( value == null ) {
				continue;
			}
			PropertiesMetadata embeddedMetadata = propertiesMetadata.embeddedPropertiesMetadata.get( i );
			switch ( propertiesMetadata.embeddedContainers.get( i ) ) {
				case ARRAY:
					for ( Object arrayValue : ( Object[] ) value ) {
						buildDocumentFields( arrayValue, doc, embeddedMetadata );
					}
					break;
				case COLLECTION:
					for ( Object collectionValue : ( Collection ) value ) {
						buildDocumentFields( collectionValue, doc, embeddedMetadata );
					}
					break;
				case MAP:
					for ( Object collectionValue : ( ( Map ) value ).values() ) {
						buildDocumentFields( collectionValue, doc, embeddedMetadata );
					}
					break;
				case OBJECT:
					buildDocumentFields( value, doc, embeddedMetadata );
					break;
				default:
					throw new AssertionFailure(
							"Unknown embedded container: "
									+ propertiesMetadata.embeddedContainers.get( i )
					);
			}
		}
	}

	private Object unproxy(Object value) {
		//FIXME this service should be part of Core?
		if ( value instanceof HibernateProxy ) {
			// .getImplementation() initializes the data by side effect
			value = ( ( HibernateProxy ) value ).getHibernateLazyInitializer()
					.getImplementation();
		}
		return value;
	}

	public Term getTerm(Serializable id) {
		if ( idProvided ) {
			return new Term( idKeywordName, ( String ) id );
		}

		return new Term( idKeywordName, idBridge.objectToString( id ) );
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public TwoWayFieldBridge getIdBridge() {
		return idBridge;
	}

	public String getIdKeywordName() {
		return idKeywordName;
	}

	public static Class getDocumentClass(Document document) {
		String className = document.get( DocumentBuilderContainedEntity.CLASS_FIELDNAME );
		try {
			return ReflectHelper.classForName( className );
		}
		catch ( ClassNotFoundException e ) {
			throw new SearchException( "Unable to load indexed class: " + className, e );
		}
	}

	public static Serializable getDocumentId(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity( clazz );
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz.getName() );
		}
		return ( Serializable ) builderIndexedEntity.getIdBridge().get( builderIndexedEntity.getIdKeywordName(), document );
	}

	public static Object[] getDocumentFields(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document, String[] fields) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity( clazz );
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz.getName() );
		}
		final int fieldNbr = fields.length;
		Object[] result = new Object[fieldNbr];

		if ( builderIndexedEntity.idKeywordName != null ) {
			populateResult( builderIndexedEntity.idKeywordName, builderIndexedEntity.idBridge, Field.Store.YES, fields, result, document );
		}

		final PropertiesMetadata metadata = builderIndexedEntity.metadata;
		processFieldsForProjection( metadata, fields, result, document );
		return result;
	}

	private static void processFieldsForProjection(PropertiesMetadata metadata, String[] fields, Object[] result, Document document) {
		final int nbrFoEntityFields = metadata.fieldNames.size();
		for ( int index = 0; index < nbrFoEntityFields; index++ ) {
			populateResult(
					metadata.fieldNames.get( index ),
					metadata.fieldBridges.get( index ),
					metadata.fieldStore.get( index ),
					fields,
					result,
					document
			);
		}
		final int nbrOfEmbeddedObjects = metadata.embeddedPropertiesMetadata.size();
		for ( int index = 0; index < nbrOfEmbeddedObjects; index++ ) {
			//there is nothing we can do for collections
			if ( metadata.embeddedContainers.get( index ) == PropertiesMetadata.Container.OBJECT ) {
				processFieldsForProjection(
						metadata.embeddedPropertiesMetadata.get( index ), fields, result, document
				);
			}
		}
	}

	private static void populateResult(String fieldName, FieldBridge fieldBridge, Field.Store store,
									   String[] fields, Object[] result, Document document) {
		int matchingPosition = getFieldPosition( fields, fieldName );
		if ( matchingPosition != -1 ) {
			//TODO make use of an isTwoWay() method
			if ( store != Field.Store.NO && TwoWayFieldBridge.class.isAssignableFrom( fieldBridge.getClass() ) ) {
				result[matchingPosition] = ( ( TwoWayFieldBridge ) fieldBridge ).get( fieldName, document );
				if ( log.isTraceEnabled() ) {
					log.trace( "Field {} projected as {}", fieldName, result[matchingPosition] );
				}
			}
			else {
				if ( store == Field.Store.NO ) {
					throw new SearchException( "Projecting an unstored field: " + fieldName );
				}
				else {
					throw new SearchException( "FieldBridge is not a TwoWayFieldBridge: " + fieldBridge.getClass() );
				}
			}
		}
	}

	private static int getFieldPosition(String[] fields, String fieldName) {
		int fieldNbr = fields.length;
		for ( int index = 0; index < fieldNbr; index++ ) {
			if ( fieldName.equals( fields[index] ) ) {
				return index;
			}
		}
		return -1;
	}

	public void postInitialize(Set<Class<?>> indexedClasses) {
		if ( entityState == EntityState.NON_INDEXABLE ) {
			throw new AssertionFailure( "A non indexed entity is post processed" );
		}
		//this method does not requires synchronization
		Class plainClass = reflectionManager.toClass( beanClass );
		Set<Class<?>> tempMappedSubclasses = new HashSet<Class<?>>();
		//together with the caller this creates a o(2), but I think it's still faster than create the up hierarchy for each class
		for ( Class currentClass : indexedClasses ) {
			if ( plainClass.isAssignableFrom( currentClass ) ) {
				tempMappedSubclasses.add( currentClass );
			}
		}
		this.mappedSubclasses = Collections.unmodifiableSet( tempMappedSubclasses );
		Class superClass = plainClass.getSuperclass();
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

	public EntityState getEntityState() {
		return entityState;
	}

	public Set<Class<?>> getMappedSubclasses() {
		return mappedSubclasses;
	}

	/**
	 * Make sure to return false if there is a risk of composite id
	 * if composite id, use of (a, b) in ((1,2), (3,4)) fails on most database
	 */
	public boolean isSafeFromTupleId() {
		return safeFromTupleId;
	}

	/**
	 * Wrapper class containing all the meta data extracted out of the entities.
	 */
	protected static class PropertiesMetadata {
		public Float boost;
		public Analyzer analyzer;
		public final List<String> fieldNames = new ArrayList<String>();
		public final List<XMember> fieldGetters = new ArrayList<XMember>();
		public final List<FieldBridge> fieldBridges = new ArrayList<FieldBridge>();
		public final List<Field.Store> fieldStore = new ArrayList<Field.Store>();
		public final List<Field.Index> fieldIndex = new ArrayList<Field.Index>();
		public final List<Float> fieldBoosts = new ArrayList<Float>();
		public final List<Field.TermVector> fieldTermVectors = new ArrayList<Field.TermVector>();
		public final List<XMember> embeddedGetters = new ArrayList<XMember>();
		public final List<PropertiesMetadata> embeddedPropertiesMetadata = new ArrayList<PropertiesMetadata>();
		public final List<Container> embeddedContainers = new ArrayList<Container>();
		public final List<XMember> containedInGetters = new ArrayList<XMember>();
		public final List<String> classNames = new ArrayList<String>();
		public final List<Field.Store> classStores = new ArrayList<Field.Store>();
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

		protected LuceneOptions getFieldLuceneOptions(int i) {
			LuceneOptions options;
			options = new LuceneOptionsImpl(
					fieldStore.get( i ),
					fieldIndex.get( i ), fieldTermVectors.get( i ), fieldBoosts.get( i )
			);
			return options;
		}
	}
}