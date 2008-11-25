//$Id$
package org.hibernate.search.engine;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.impl.InitContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.ReflectionHelper;

/**
 * Set up and provide a manager for classes which are directly annotated with <code>@Indexed</code>.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilderIndexedEntity<T> extends DocumentBuilderContainedEntity<T> {
	private static final Logger log = LoggerFactory.make();

	/**
	 * Arrays of directory providers for the underlying Lucene indexes of the indexed entity.
	 */
	private final DirectoryProvider[] directoryProviders;

	/**
	 * The sharding strategy used for the indexed entity.
	 */
	private final IndexShardingStrategy shardingStrategy;

	/**
	 * Flag indicating whether <code>@DocumentId</code> was explicitly specified.
	 */
	private boolean explicitDocumentId = false;

	/**
	 * Flag indicating whether {@link org.apache.lucene.search.Searcher#doc(int, org.apache.lucene.document.FieldSelector)}
	 * can be used in order to retrieve documents. This is only safe to do if we know that
	 * all involved bridges are implementing <code>TwoWayStringBridge</code>. See HSEARCH-213.
	 */
	private boolean allowFieldSelectionInProjection = false;

	/**
	 * The class member used as document id.
	 */
	protected XMember idGetter;

	/**
	 * Name of the document id field.
	 */
	protected String idKeywordName;

	/**
	 * Boost specified on the document id.
	 */
	private Float idBoost;

	/**
	 * The bridge used for the document id.
	 */
	private TwoWayFieldBridge idBridge;

	/**
	 * Flag indicating whether there is an explicit id (@DocumentId or @Id) or not. When Search is used as make
	 * for example JBoss Cache searchable the <code>idKeywordName</code> wil be provided.
	 */
	private boolean idProvided = false;


	//if composite id, use of (a, b) in ((1,2), (3,4)) fails on most database
	private boolean safeFromTupleId;

	/**
	 * Creates a document builder for entities annotated with <code>@Indexed</code>.
	 *
	 * @param clazz The class for which to build a <code>DocumentBuilderContainedEntity</code>.
	 * @param context Handle to default configuration settings.
	 * @param directoryProviders Arrays of directory providers for the underlying Lucene indexes of the indexed entity.
	 * @param shardingStrategy The sharding strategy used for the indexed entity.
	 * @param reflectionManager Reflection manager to use for processing the annotations.
	 */
	public DocumentBuilderIndexedEntity(XClass clazz, InitContext context, DirectoryProvider[] directoryProviders,
										IndexShardingStrategy shardingStrategy, ReflectionManager reflectionManager) {

		super( clazz, context, reflectionManager );

		this.entityState = EntityState.INDEXED;
		this.directoryProviders = directoryProviders;
		this.shardingStrategy = shardingStrategy;

		if ( idKeywordName == null ) {
			// if no DocumentId then check if we have a ProvidedId instead
			ProvidedId provided = findProvidedId( clazz, reflectionManager );
			if ( provided == null ) {
				throw new SearchException( "No document id in: " + clazz.getName() );
			}

			idBridge = BridgeFactory.extractTwoWayType( provided.bridge() );
			idKeywordName = provided.name();
		}
	}

	protected void init(XClass clazz, InitContext context) {
		super.init( clazz, context );

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


	protected void checkDocumentId(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, InitContext context) {
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

	private ProvidedId findProvidedId(XClass clazz, ReflectionManager reflectionManager) {
		ProvidedId id = null;
		XClass currentClass = clazz;
		while ( id == null && ( !reflectionManager.equals( currentClass, Object.class ) ) ) {
			id = currentClass.getAnnotation( ProvidedId.class );
			currentClass = clazz.getSuperclass();
		}
		return id;
	}

	//TODO could we use T instead of EntityClass?
	public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		//TODO with the caller loop we are in a n^2: optimize it using a HashMap for work recognition

		List<LuceneWork> toDelete = new ArrayList<LuceneWork>();
		boolean duplicateDelete = false;
		for ( LuceneWork luceneWork : queue ) {
			//avoid unecessary duplicated work
			if ( luceneWork.getEntityClass() == entityClass
					) {
				Serializable currentId = luceneWork.getId();
				//currentId != null => either ADD or Delete work
				if ( currentId != null && currentId.equals( id ) ) { //find a way to use Type.equals(x,y)
					if ( workType == WorkType.DELETE ) { //TODO add PURGE?
						//DELETE should have precedence over any update before (HSEARCH-257)
						//if an Add work is here, remove it
						//if an other delete is here remember but still search for Add
						if ( luceneWork instanceof AddLuceneWork ) {
							toDelete.add( luceneWork );
						}
						else if ( luceneWork instanceof DeleteLuceneWork ) {
							duplicateDelete = true;
						}
					}
					else {
						//we can safely say we are out, the other work is an ADD
						return;
					}
				}
				//TODO do something to avoid multiple PURGE ALL and OPTIMIZE
			}
		}
		for ( LuceneWork luceneWork : toDelete ) {
			queue.remove( luceneWork );
		}
		if ( duplicateDelete ) {
			return;
		}

		String idInString = idBridge.objectToString( id );
		if ( workType == WorkType.ADD ) {
			Document doc = getDocument( entity, id );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc ) );
		}
		else if ( workType == WorkType.DELETE || workType == WorkType.PURGE ) {
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
		}
		else if ( workType == WorkType.PURGE_ALL ) {
			queue.add( new PurgeAllLuceneWork( entityClass ) );
		}
		else if ( workType == WorkType.UPDATE || workType == WorkType.COLLECTION ) {
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
		}
		else if ( workType == WorkType.INDEX ) {
			Document doc = getDocument( entity, id );
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc, true ) );
		}
		else {
			throw new AssertionFailure( "Unknown WorkType: " + workType );
		}

		super.addWorkToQueue( entityClass, entity, id, workType, queue, searchFactoryImplementor );
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

	public String getIdentifierName() {
		return idGetter.getName();
	}

	public DirectoryProvider[] getDirectoryProviders() {
		if ( entityState != EntityState.INDEXED ) {
			throw new AssertionFailure( "Contained in only entity: getDirectoryProvider should not have been called." );
		}
		return directoryProviders;
	}

	public IndexShardingStrategy getDirectoryProviderSelectionStrategy() {
		if ( entityState != EntityState.INDEXED ) {
			throw new AssertionFailure(
					"Contained in only entity: getDirectoryProviderSelectionStrategy should not have been called."
			);
		}
		return shardingStrategy;
	}

	public boolean allowFieldSelectionInProjection() {
		return allowFieldSelectionInProjection;
	}

	/**
	 * @return <code>false</code> if there is a risk of composite id. If composite id, use of (a, b) in ((1,2), (3,4)) fails on most database
	 */
	public boolean isSafeFromTupleId() {
		return safeFromTupleId;
	}

	public Term getTerm(Serializable id) {
		if ( idProvided ) {
			return new Term( idKeywordName, ( String ) id );
		}

		return new Term( idKeywordName, idBridge.objectToString( id ) );
	}

	public TwoWayFieldBridge getIdBridge() {
		return idBridge;
	}

	public static Class getDocumentClass(Document document) {
		String className = document.get( CLASS_FIELDNAME );
		try {
			return ReflectHelper.classForName( className );
		}
		catch ( ClassNotFoundException e ) {
			throw new SearchException( "Unable to load indexed class: " + className, e );
		}
	}

	public String getIdKeywordName() {
		return idKeywordName;
	}

	public static Serializable getDocumentId(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				clazz
		);
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz.getName() );
		}
		return ( Serializable ) builderIndexedEntity.getIdBridge()
				.get( builderIndexedEntity.getIdKeywordName(), document );
	}

	public static Object[] getDocumentFields(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document, String[] fields) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				clazz
		);
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz.getName() );
		}
		final int fieldNbr = fields.length;
		Object[] result = new Object[fieldNbr];

		if ( builderIndexedEntity.idKeywordName != null ) {
			populateResult(
					builderIndexedEntity.idKeywordName,
					builderIndexedEntity.idBridge,
					Field.Store.YES,
					fields,
					result,
					document
			);
		}

		final PropertiesMetadata metadata = builderIndexedEntity.metadata;
		processFieldsForProjection( metadata, fields, result, document );
		return result;
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

	private static int getFieldPosition(String[] fields, String fieldName) {
		int fieldNbr = fields.length;
		for ( int index = 0; index < fieldNbr; index++ ) {
			if ( fieldName.equals( fields[index] ) ) {
				return index;
			}
		}
		return -1;
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
}
