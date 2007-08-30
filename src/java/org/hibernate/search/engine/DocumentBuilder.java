//$Id$
package org.hibernate.search.engine;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.BinderHelper;
import org.hibernate.search.util.ScopedAnalyzer;

/**
 * Set up and provide a manager for indexes classes
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilder<T> {
	private static final Log log = LogFactory.getLog( DocumentBuilder.class );

	private final PropertiesMetadata rootPropertiesMetadata;
	private final XClass beanClass;
	private final DirectoryProvider[] directoryProviders;
	private final IndexShardingStrategy shardingStrategy;
	private String idKeywordName;
	private XMember idGetter;
	private Float idBoost;
	public static final String CLASS_FIELDNAME = "_hibernate_class";
	private TwoWayFieldBridge idBridge;
	private Set<Class> mappedSubclasses = new HashSet<Class>();
	private ReflectionManager reflectionManager;
	private int level = 0;
	private int maxLevel = Integer.MAX_VALUE;
	private ScopedAnalyzer analyzer;


	public DocumentBuilder(XClass clazz, Analyzer defaultAnalyzer, DirectoryProvider[] directoryProviders,
						   IndexShardingStrategy shardingStrategy, ReflectionManager reflectionManager) {
		this.analyzer = new ScopedAnalyzer();
		this.beanClass = clazz;
		this.directoryProviders = directoryProviders;
		this.shardingStrategy = shardingStrategy;
		//FIXME get rid of it when boost is stored?
		this.reflectionManager = reflectionManager;

		if ( clazz == null ) throw new AssertionFailure( "Unable to build a DocumemntBuilder with a null class" );
		rootPropertiesMetadata = new PropertiesMetadata();
		rootPropertiesMetadata.boost = getBoost( clazz );
		rootPropertiesMetadata.analyzer = defaultAnalyzer;
		Set<XClass> processedClasses = new HashSet<XClass>();
		processedClasses.add( clazz );
		initializeMembers( clazz, rootPropertiesMetadata, true, "", processedClasses );
		//processedClasses.remove( clazz ); for the sake of completness
		this.analyzer.setGlobalAnalyzer( rootPropertiesMetadata.analyzer );
		if ( idKeywordName == null ) {
			throw new SearchException( "No document id in: " + clazz.getName() );
		}
	}

	private Analyzer getAnalyzer(XAnnotatedElement annotatedElement) {
		org.hibernate.search.annotations.Analyzer analyzerAnn =
				annotatedElement.getAnnotation( org.hibernate.search.annotations.Analyzer.class );
		return getAnalyzer( analyzerAnn );
	}

	private Analyzer getAnalyzer(org.hibernate.search.annotations.Analyzer analyzerAnn) {
		Class analyzerClass = analyzerAnn == null ? void.class : analyzerAnn.impl();
		if ( analyzerClass == void.class ) {
			return null;
		}
		else {
			try {
				return (Analyzer) analyzerClass.newInstance();
			}
			catch (ClassCastException e) {
				throw new SearchException(
						"Lucene analyzer does not implement " + Analyzer.class.getName() + ": " + analyzerClass.getName(), e
				);
			}
			catch (Exception e) {
				throw new SearchException( "Failed to instantiate lucene analyzer with type " + analyzerClass.getName(), e );
			}
		}
	}

	private void initializeMembers(XClass clazz, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix,
								   Set<XClass> processedClasses) {
		List<XClass> hierarchy = new ArrayList<XClass>();
		for (XClass currClass = clazz; currClass != null; currClass = currClass.getSuperclass()) {
			hierarchy.add( currClass );
		}
		for (int index = hierarchy.size() - 1; index >= 0; index--) {
			XClass currClass = hierarchy.get( index );
			/**
			 * Override the default analyzer for the properties if the class hold one
			 * That's the reason we go down the hierarchy
			 */
			Analyzer analyzer = getAnalyzer( currClass );
			if ( analyzer != null ) {
				propertiesMetadata.analyzer = analyzer;
			}
			//rejecting non properties because the object is loaded from Hibernate, so indexing a non property does not make sense
			List<XProperty> methods = currClass.getDeclaredProperties( XClass.ACCESS_PROPERTY );
			for (XProperty method : methods) {
				initializeMember( method, propertiesMetadata, isRoot, prefix, processedClasses );
			}

			List<XProperty> fields = currClass.getDeclaredProperties( XClass.ACCESS_FIELD );
			for (XProperty field : fields) {
				initializeMember( field, propertiesMetadata, isRoot, prefix, processedClasses );
			}
		}
	}

	public String getIdentifierName() {
		return idGetter.getName();
	}

	private void initializeMember(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot,
								  String prefix, Set<XClass> processedClasses) {

		DocumentId documentIdAnn = member.getAnnotation( DocumentId.class );
		if ( documentIdAnn != null ) {
			if ( isRoot ) {
				if ( idKeywordName != null ) {
					throw new AssertionFailure( "Two document id assigned: "
							+ idKeywordName + " and " + BinderHelper.getAttributeName( member, documentIdAnn.name() ) );
				}
				idKeywordName = prefix + BinderHelper.getAttributeName( member, documentIdAnn.name() );
				FieldBridge fieldBridge = BridgeFactory.guessType( null, member );
				if ( fieldBridge instanceof TwoWayFieldBridge ) {
					idBridge = (TwoWayFieldBridge) fieldBridge;
				}
				else {
					throw new SearchException(
							"Bridge for document id does not implement IdFieldBridge: " + member.getName() );
				}
				idBoost = getBoost( member );
				setAccessible( member );
				idGetter = member;
			}
			else {
				//component should index their document id
				setAccessible( member );
				propertiesMetadata.fieldGetters.add( member );
				String fieldName = prefix + BinderHelper.getAttributeName( member, documentIdAnn.name() );
				propertiesMetadata.fieldNames.add( fieldName );
				propertiesMetadata.fieldStore.add( getStore( Store.YES ) );
				propertiesMetadata.fieldIndex.add( getIndex( Index.UN_TOKENIZED ) );
				propertiesMetadata.fieldBridges.add( BridgeFactory.guessType( null, member ) );
				// Field > property > entity analyzer
				Analyzer analyzer = null; //no field analyzer
				if ( analyzer == null ) analyzer = getAnalyzer( member );
				if ( analyzer == null ) analyzer = propertiesMetadata.analyzer;
				if ( analyzer == null ) throw new AssertionFailure( "Analizer should not be undefined" );
				this.analyzer.addScopedAnalyzer( fieldName, analyzer );
			}
		}
		{
			org.hibernate.search.annotations.Field fieldAnn =
					member.getAnnotation( org.hibernate.search.annotations.Field.class );
			if ( fieldAnn != null ) {
				bindFieldAnnotation( member, propertiesMetadata, prefix, fieldAnn );
			}
		}
		{
			org.hibernate.search.annotations.Fields fieldsAnn =
					member.getAnnotation( org.hibernate.search.annotations.Fields.class );
			if ( fieldsAnn != null ) {
				for (org.hibernate.search.annotations.Field fieldAnn : fieldsAnn.value()) {
					bindFieldAnnotation( member, propertiesMetadata, prefix, fieldAnn );
				}
			}
		}

		IndexedEmbedded embeddedAnn = member.getAnnotation( IndexedEmbedded.class );
		if ( embeddedAnn != null ) {
			int oldMaxLevel = maxLevel;
			maxLevel = embeddedAnn.depth() + level > maxLevel ? maxLevel : embeddedAnn.depth() + level;
			level++;

			XClass elementClass = member.getElementClass();
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

				setAccessible( member );
				propertiesMetadata.embeddedGetters.add( member );
				PropertiesMetadata metadata = new PropertiesMetadata();
				propertiesMetadata.embeddedPropertiesMetadata.add( metadata );
				metadata.boost = getBoost( member );
				//property > entity analyzer
				Analyzer analyzer = getAnalyzer( member );
				metadata.analyzer = analyzer != null ? analyzer : propertiesMetadata.analyzer;
				String localPrefix = buildEmbeddedPrefix( prefix, embeddedAnn, member );
				initializeMembers( elementClass, metadata, false, localPrefix, processedClasses );
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
				log.trace( "depth reached, ignoring " + localPrefix );
			}

			level--;
			maxLevel = oldMaxLevel; //set back the the old max level
		}

		ContainedIn containedAnn = member.getAnnotation( ContainedIn.class );
		if ( containedAnn != null ) {
			setAccessible( member );
			propertiesMetadata.containedInGetters.add( member );
		}
	}

	private void bindFieldAnnotation(XProperty member, PropertiesMetadata propertiesMetadata, String prefix, org.hibernate.search.annotations.Field fieldAnn) {
		setAccessible( member );
		propertiesMetadata.fieldGetters.add( member );
		String fieldName = prefix + BinderHelper.getAttributeName( member, fieldAnn.name() );
		propertiesMetadata.fieldNames.add( fieldName );
		propertiesMetadata.fieldStore.add( getStore( fieldAnn.store() ) );
		propertiesMetadata.fieldIndex.add( getIndex( fieldAnn.index() ) );
		propertiesMetadata.fieldBridges.add( BridgeFactory.guessType( fieldAnn, member ) );
		// Field > property > entity analyzer
		Analyzer analyzer = getAnalyzer( fieldAnn.analyzer() );
		if ( analyzer == null ) analyzer = getAnalyzer( member );
		if ( analyzer == null ) analyzer = propertiesMetadata.analyzer;
		if ( analyzer == null ) throw new AssertionFailure( "Analizer should not be undefined" );
		this.analyzer.addScopedAnalyzer( fieldName, analyzer );
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

	private Field.Index getIndex(Index index) {
		switch ( index ) {
			case NO:
				return Field.Index.NO;
			case NO_NORMS:
				return Field.Index.NO_NORMS;
			case TOKENIZED:
				return Field.Index.TOKENIZED;
			case UN_TOKENIZED:
				return Field.Index.UN_TOKENIZED;
			default:
				throw new AssertionFailure( "Unexpected Index: " + index );
		}
	}

	private Float getBoost(XAnnotatedElement element) {
		if ( element == null ) return null;
		Boost boost = element.getAnnotation( Boost.class );
		return boost != null ?
				boost.value() :
				null;
	}

	private Object getMemberValue(Object bean, XMember getter) {
		Object value;
		try {
			value = getter.invoke( bean );
		}
		catch (Exception e) {
			throw new IllegalStateException( "Could not get property value", e );
		}
		return value;
	}

	//TODO could we use T instead of EntityClass?
	public void addWorkToQueue(Class entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		//TODO with the caller loop we are in a n^2: optimize it using a HashMap for work recognition 
		for (LuceneWork luceneWork : queue) {
			//any work on the same entity should be ignored
			if ( luceneWork.getEntityClass() == entityClass
					 ) {
				Serializable currentId = luceneWork.getId();
				if ( currentId != null  && currentId.equals( id ) ) { //find a way to use Type.equals(x,y)
					return;
				}
				//TODO do something to avoid multiple PURGE ALL and OPTIMIZE
			}

		}
		boolean searchForContainers = false;
		String idInString = idBridge.objectToString( id );
		if ( workType == WorkType.ADD ) {
			Document doc = getDocument( entity, id );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc ) );
			searchForContainers = true;
		}
		else if ( workType == WorkType.DELETE || workType == WorkType.PURGE ) {
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
		}
		else if ( workType == WorkType.PURGE_ALL ) {
			queue.add( new PurgeAllLuceneWork( entityClass ) );
		}
		else if ( workType == WorkType.UPDATE ) {
			Document doc = getDocument( entity, id );
			/**
			 * even with Lucene 2.1, use of indexWriter to update is not an option
			 * We can only delete by term, and the index doesn't have a term that
			 * uniquely identify the entry.
			 * But essentially the optimization we are doing is the same Lucene is doing, the only extra cost is the
			 * double file opening.
			 */
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc ) );
			searchForContainers = true;
		}
		else if ( workType == WorkType.INDEX ) {
			Document doc = getDocument( entity, id );
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			LuceneWork work = new AddLuceneWork( id, idInString, entityClass, doc );
			work.setBatch( true );
			queue.add( work );
			searchForContainers = true;
		}

		else {
			throw new AssertionFailure( "Unknown WorkType: " + workType );
		}

		/**
		 * When references are changed, either null or another one, we expect dirty checking to be triggered (both sides
		 * have to be updated)
		 * When the internal object is changed, we apply the {Add|Update}Work on containedIns
		 */
		if ( searchForContainers ) {
			processContainedIn( entity, queue, rootPropertiesMetadata, searchFactoryImplementor );
		}
	}

	private void processContainedIn(Object instance, List<LuceneWork> queue, PropertiesMetadata metadata, SearchFactoryImplementor searchFactoryImplementor) {
		for (int i = 0; i < metadata.containedInGetters.size(); i++) {
			XMember member = metadata.containedInGetters.get( i );
			Object value = getMemberValue( instance, member );
			if ( value == null ) continue;

			if ( member.isArray() ) {
				for (Object arrayValue : (Object[]) value) {
					//highly inneficient but safe wrt the actual targeted class
					Class valueClass = Hibernate.getClass( arrayValue );
					DocumentBuilder builder = searchFactoryImplementor.getDocumentBuilders().get( valueClass );
					if ( builder == null ) continue;
					processContainedInValue( arrayValue, queue, valueClass, builder, searchFactoryImplementor );
				}
			}
			else if ( member.isCollection() ) {
				Collection collection;
				if ( Map.class.equals( member.getCollectionClass() ) ) {
					//hum
					collection = ( (Map) value ).values();
				}
				else {
					collection = (Collection) value;
				}
				for (Object collectionValue : collection) {
					//highly inneficient but safe wrt the actual targeted class
					Class valueClass = Hibernate.getClass( collectionValue );
					DocumentBuilder builder = searchFactoryImplementor.getDocumentBuilders().get( valueClass );
					if ( builder == null ) continue;
					processContainedInValue( collectionValue, queue, valueClass, builder, searchFactoryImplementor );
				}
			}
			else {
				Class valueClass = Hibernate.getClass( value );
				DocumentBuilder builder = searchFactoryImplementor.getDocumentBuilders().get( valueClass );
				if ( builder == null ) continue;
				processContainedInValue( value, queue, valueClass, builder, searchFactoryImplementor );
			}
		}
		//an embedded cannot have a useful @ContainedIn (no shared reference)
		//do not walk through them
	}

	private void processContainedInValue(Object value, List<LuceneWork> queue, Class valueClass,
										 DocumentBuilder builder, SearchFactoryImplementor searchFactoryImplementor) {
		Serializable id = (Serializable) builder.getMemberValue( value, builder.idGetter );
		builder.addWorkToQueue( valueClass, value, id, WorkType.UPDATE, queue, searchFactoryImplementor );
	}

	public Document getDocument(T instance, Serializable id) {
		Document doc = new Document();
		XClass instanceClass = reflectionManager.toXClass( Hibernate.getClass( instance ) );
		if ( rootPropertiesMetadata.boost != null ) {
			doc.setBoost( rootPropertiesMetadata.boost );
		}
		{
			Field classField =
					new Field( CLASS_FIELDNAME, instanceClass.getName(), Field.Store.YES, Field.Index.UN_TOKENIZED );
			doc.add( classField );
			idBridge.set( idKeywordName, id, doc, Field.Store.YES, Field.Index.UN_TOKENIZED, idBoost );
		}
		buildDocumentFields( instance, doc, rootPropertiesMetadata );
		return doc;
	}

	private void buildDocumentFields(Object instance, Document doc, PropertiesMetadata propertiesMetadata) {
		if ( instance == null ) return;
		//needed for field access: I cannot work in the proxied version
		Object unproxiedInstance = unproxy( instance );
		for (int i = 0; i < propertiesMetadata.keywordNames.size(); i++) {
			XMember member = propertiesMetadata.keywordGetters.get( i );
			Object value = getMemberValue( unproxiedInstance, member );
			propertiesMetadata.keywordBridges.get( i ).set(
					propertiesMetadata.keywordNames.get( i ), value, doc, Field.Store.YES,
					Field.Index.UN_TOKENIZED, getBoost( member )
			);
		}
		for (int i = 0; i < propertiesMetadata.textNames.size(); i++) {
			XMember member = propertiesMetadata.textGetters.get( i );
			Object value = getMemberValue( unproxiedInstance, member );
			propertiesMetadata.textBridges.get( i ).set(
					propertiesMetadata.textNames.get( i ), value, doc, Field.Store.YES,
					Field.Index.TOKENIZED, getBoost( member )
			);
		}
		for (int i = 0; i < propertiesMetadata.unstoredNames.size(); i++) {
			XMember member = propertiesMetadata.unstoredGetters.get( i );
			Object value = getMemberValue( unproxiedInstance, member );
			propertiesMetadata.unstoredBridges.get( i ).set(
					propertiesMetadata.unstoredNames.get( i ), value, doc, Field.Store.NO,
					Field.Index.TOKENIZED, getBoost( member )
			);
		}
		for (int i = 0; i < propertiesMetadata.fieldNames.size(); i++) {
			XMember member = propertiesMetadata.fieldGetters.get( i );
			Object value = getMemberValue( unproxiedInstance, member );
			propertiesMetadata.fieldBridges.get( i ).set(
					propertiesMetadata.fieldNames.get( i ), value, doc, propertiesMetadata.fieldStore.get( i ),
					propertiesMetadata.fieldIndex.get( i ), getBoost( member )
			);
		}
		for (int i = 0; i < propertiesMetadata.embeddedGetters.size(); i++) {
			XMember member = propertiesMetadata.embeddedGetters.get( i );
			Object value = getMemberValue( unproxiedInstance, member );
			//TODO handle boost at embedded level: already stored in propertiesMedatada.boost

			if ( value == null ) continue;
			PropertiesMetadata embeddedMetadata = propertiesMetadata.embeddedPropertiesMetadata.get( i );
			switch ( propertiesMetadata.embeddedContainers.get( i ) ) {
				case ARRAY:
					for (Object arrayValue : (Object[]) value) {
						buildDocumentFields( arrayValue, doc, embeddedMetadata );
					}
					break;
				case COLLECTION:
					for (Object collectionValue : (Collection) value) {
						buildDocumentFields( collectionValue, doc, embeddedMetadata );
					}
					break;
				case MAP:
					for (Object collectionValue : ( (Map) value ).values()) {
						buildDocumentFields( collectionValue, doc, embeddedMetadata );
					}
					break;
				case OBJECT:
					buildDocumentFields( value, doc, embeddedMetadata );
					break;
				default:
					throw new AssertionFailure( "Unknown embedded container: "
							+ propertiesMetadata.embeddedContainers.get( i ) );
			}
		}
	}

	private Object unproxy(Object value) {
		//FIXME this service should be part of Core?
		if ( value instanceof HibernateProxy ) {
			// .getImplementation() initializes the data by side effect
			value = ( (HibernateProxy) value ).getHibernateLazyInitializer()
					.getImplementation();
		}
		return value;
	}

	public Term getTerm(Serializable id) {
		return new Term( idKeywordName, idBridge.objectToString( id ) );
	}

	public DirectoryProvider[] getDirectoryProviders() {
		return directoryProviders;
	}

	public IndexShardingStrategy getDirectoryProviderSelectionStrategy() {
		return shardingStrategy;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	private static void setAccessible(XMember member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			member.setAccessible( true );
		}
	}

	public TwoWayFieldBridge getIdBridge() {
		return idBridge;
	}

	public String getIdKeywordName() {
		return idKeywordName;
	}

	public static Class getDocumentClass(Document document) {
		String className = document.get( DocumentBuilder.CLASS_FIELDNAME );
		try {
			return ReflectHelper.classForName( className );
		}
		catch (ClassNotFoundException e) {
			throw new SearchException( "Unable to load indexed class: " + className, e );
		}
	}

	public static Serializable getDocumentId(SearchFactoryImplementor searchFactoryImplementor, Class clazz, Document document) {
		DocumentBuilder builder = searchFactoryImplementor.getDocumentBuilders().get( clazz );
		if ( builder == null ) throw new SearchException( "No Lucene configuration set up for: " + clazz.getName() );
		return (Serializable) builder.getIdBridge().get( builder.getIdKeywordName(), document );
	}

	public static Object[] getDocumentFields(SearchFactoryImplementor searchFactoryImplementor, Class clazz, Document document, String[] fields) {
		DocumentBuilder builder = searchFactoryImplementor.getDocumentBuilders().get( clazz );
		if ( builder == null ) throw new SearchException( "No Lucene configuration set up for: " + clazz.getName() );
		final int fieldNbr = fields.length;
		Object[] result = new Object[fieldNbr];

		if ( builder.idKeywordName != null ) {
			populateResult( builder.idKeywordName, builder.idBridge, Field.Store.YES, fields, result, document );
		}

		final PropertiesMetadata metadata = builder.rootPropertiesMetadata;
		processFieldsForProjection( metadata, fields, result, document );
		return result;
	}

	private static void processFieldsForProjection(PropertiesMetadata metadata, String[] fields, Object[] result, Document document) {
		final int nbrFoEntityFields = metadata.fieldNames.size();
		for (int index = 0; index < nbrFoEntityFields; index++) {
			populateResult( metadata.fieldNames.get( index ),
					metadata.fieldBridges.get( index ),
					metadata.fieldStore.get( index ),
					fields,
					result,
					document
			);
		}
		final int nbrOfEmbeddedObjects = metadata.embeddedPropertiesMetadata.size();
		for (int index = 0; index < nbrOfEmbeddedObjects; index++) {
			//there is nothing we can do for collections
			if ( metadata.embeddedContainers.get( index ) == PropertiesMetadata.Container.OBJECT ) {
				processFieldsForProjection( metadata.embeddedPropertiesMetadata.get( index ), fields, result, document );
			}
		}
	}

	private static void populateResult(String fieldName, FieldBridge fieldBridge, Field.Store store,
									   String[] fields, Object[] result, Document document) {
		int matchingPosition = getFieldPosition( fields, fieldName );
		if ( matchingPosition != -1 ) {
			//TODO make use of an isTwoWay() method
			if ( store != Field.Store.NO && TwoWayFieldBridge.class.isAssignableFrom( fieldBridge.getClass() ) ) {
				result[matchingPosition] = ( (TwoWayFieldBridge) fieldBridge ).get( fieldName, document );
				if ( log.isTraceEnabled() ) {
					log.trace( "Field " + fieldName + " projected as " + result[matchingPosition] );
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
		for (int index = 0; index < fieldNbr; index++) {
			if ( fieldName.equals( fields[index] ) ) return index;
		}
		return -1;
	}

	public void postInitialize(Set<Class> indexedClasses) {
		//this method does not requires synchronization
		Class plainClass = reflectionManager.toClass( beanClass );
		Set<Class> tempMappedSubclasses = new HashSet<Class>();
		//together with the caller this creates a o(2), but I think it's still faster than create the up hierarchy for each class
		for (Class currentClass : indexedClasses) {
			if ( plainClass.isAssignableFrom( currentClass ) ) tempMappedSubclasses.add( currentClass );
		}
		mappedSubclasses = Collections.unmodifiableSet( tempMappedSubclasses );
	}


	public Set<Class> getMappedSubclasses() {
		return mappedSubclasses;
	}

	private static class PropertiesMetadata {
		public Float boost;
		public Analyzer analyzer;
		public final List<XMember> keywordGetters = new ArrayList<XMember>();
		public final List<String> keywordNames = new ArrayList<String>();
		public final List<FieldBridge> keywordBridges = new ArrayList<FieldBridge>();
		public final List<XMember> unstoredGetters = new ArrayList<XMember>();
		public final List<String> unstoredNames = new ArrayList<String>();
		public final List<FieldBridge> unstoredBridges = new ArrayList<FieldBridge>();
		public final List<XMember> textGetters = new ArrayList<XMember>();
		public final List<String> textNames = new ArrayList<String>();
		public final List<FieldBridge> textBridges = new ArrayList<FieldBridge>();
		public final List<String> fieldNames = new ArrayList<String>();
		public final List<XMember> fieldGetters = new ArrayList<XMember>();
		public final List<FieldBridge> fieldBridges = new ArrayList<FieldBridge>();
		public final List<Field.Store> fieldStore = new ArrayList<Field.Store>();
		public final List<Field.Index> fieldIndex = new ArrayList<Field.Index>();
		public final List<XMember> embeddedGetters = new ArrayList<XMember>();
		public final List<PropertiesMetadata> embeddedPropertiesMetadata = new ArrayList<PropertiesMetadata>();
		public final List<Container> embeddedContainers = new ArrayList<Container>();
		public final List<XMember> containedInGetters = new ArrayList<XMember>();

		public enum Container {
			OBJECT,
			COLLECTION,
			MAP,
			ARRAY
		}
	}
}
