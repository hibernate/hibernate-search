/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.SearchException;
import org.hibernate.search.analyzer.Discriminator;
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
	 * for example using JBoss Cache Searchable the <code>idKeywordName</code> will be provided.
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
	}

	protected void init(XClass clazz, InitContext context) {
		super.init( clazz, context );

		// special case @ProvidedId
		ProvidedId provided = findProvidedId( clazz, reflectionManager );
		if ( provided != null ) {
			idBridge = BridgeFactory.extractTwoWayType( provided.bridge() );
			idKeywordName = provided.name();
		}

		if ( idKeywordName == null ) {
			throw new SearchException( "No document id in: " + clazz.getName() );
		}

		//if composite id, use of (a, b) in ((1,2),(3,4)) fails on most database
		//a TwoWayString2FieldBridgeAdaptor is never a composite id
		safeFromTupleId = TwoWayString2FieldBridgeAdaptor.class.isAssignableFrom( idBridge.getClass() );

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
				propertiesMetadata.dynamicFieldBoosts.add( getDynamicBoost( member ) );
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
		Annotation idAnnotation = null;

		// check for explicit DocumentId
		DocumentId documentIdAnn = member.getAnnotation( DocumentId.class );
		if ( documentIdAnn != null ) {
			explicitDocumentId = true;
			idAnnotation = documentIdAnn;
		}
		// check for JPA @Id
		else if ( !explicitDocumentId && context.isJpaPresent() ) {
			Annotation jpaId;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Annotation> jpaIdClass =
						org.hibernate.annotations.common.util.ReflectHelper
							.classForName( "javax.persistence.Id", InitContext.class );
				jpaId = member.getAnnotation( jpaIdClass );
			}
			catch ( ClassNotFoundException e ) {
				throw new SearchException( "Unable to load @Id.class even though it should be present ?!" );
			}
			if ( jpaId != null ) {
				log.debug( "Found JPA id and using it as document id" );
				idAnnotation = jpaId;
			}
		}
		return idAnnotation;
	}

	private ProvidedId findProvidedId(XClass clazz, ReflectionManager reflectionManager) {
		ProvidedId id = null;
		XClass currentClass = clazz;
		while ( id == null && ( !reflectionManager.equals( currentClass, Object.class ) ) ) {
			id = currentClass.getAnnotation( ProvidedId.class );
			currentClass = currentClass.getSuperclass();
		}
		return id;
	}

	//TODO could we use T instead of EntityClass?
	public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		//TODO with the caller loop we are in a n^2: optimize it using a HashMap for work recognition

		boolean sameIdWasSetToBeDeleted = false;
		List<LuceneWork> toDelete = new ArrayList<LuceneWork>();
		boolean duplicateDelete = false;
		for ( LuceneWork luceneWork : queue ) {
			if ( luceneWork.getEntityClass() == entityClass ) {
				Serializable currentId = luceneWork.getId();
				if ( currentId != null && currentId.equals( id ) ) { //find a way to use Type.equals(x,y)
					if ( luceneWork instanceof DeleteLuceneWork ) {
						//flag this work as related to a to-be-deleted entity
						sameIdWasSetToBeDeleted = true;
					}
					else if ( luceneWork instanceof AddLuceneWork ) {
						//if a later work in the queue is adding it back, undo deletion flag:
						sameIdWasSetToBeDeleted = false;
					}
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
					if ( workType == WorkType.ADD ) {
						if ( luceneWork instanceof AddLuceneWork ) {
							//embedded objects may issue an "UPDATE" right before the "ADD",
							//leading to double insertions in the index
							toDelete.add( luceneWork );
						}
					}
					//TODO do something to avoid multiple PURGE ALL and OPTIMIZE
				}
			}
		}

		if ( sameIdWasSetToBeDeleted && workType == WorkType.COLLECTION ) {
			//avoid updating (and thus adding) objects which are going to be deleted
			return;
		}

		for ( LuceneWork luceneWork : toDelete ) {
			queue.remove( luceneWork );
		}
		if ( duplicateDelete ) {
			return;
		}

		if ( workType == WorkType.ADD ) {
			String idInString = idBridge.objectToString( id );
			queue.add( createAddWork( entityClass, entity, id, idInString, false ) );
		}
		else if ( workType == WorkType.DELETE || workType == WorkType.PURGE ) {
			String idInString = idBridge.objectToString( id );
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
		}
		else if ( workType == WorkType.PURGE_ALL ) {
			queue.add( new PurgeAllLuceneWork( entityClass ) );
		}
		else if ( workType == WorkType.UPDATE || workType == WorkType.COLLECTION ) {
			String idInString = idBridge.objectToString( id );
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			queue.add( createAddWork( entityClass, entity, id, idInString, false ) );
		}
		else if ( workType == WorkType.INDEX ) {
			String idInString = idBridge.objectToString( id );
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			queue.add( createAddWork( entityClass, entity, id, idInString, true ) );
		}
		else {
			throw new AssertionFailure( "Unknown WorkType: " + workType );
		}

		super.addWorkToQueue( entityClass, entity, id, workType, queue, searchFactoryImplementor );
	}

	public AddLuceneWork createAddWork(Class<T> entityClass, T entity, Serializable id, String idInString, boolean isBatch) {
		Map<String, String> fieldToAnalyzerMap = new HashMap<String, String>();
		Document doc = getDocument( entity, id, fieldToAnalyzerMap );
		AddLuceneWork addWork;
		if ( fieldToAnalyzerMap.isEmpty() ) {
			addWork = new AddLuceneWork( id, idInString, entityClass, doc, isBatch );
		}
		else {
			addWork = new AddLuceneWork( id, idInString, entityClass, doc, fieldToAnalyzerMap, isBatch );
		}
		return addWork;
	}

	/**
	 * Builds the Lucene <code>Document</code> for a given entity <code>instance</code> and its <code>id</code>.
	 *
	 * @param instance The entity for which to build the matching Lucene <code>Document</code>
	 * @param id the entity id.
	 * @param fieldToAnalyzerMap this maps gets populated while generating the <code>Document</code>.
	 * It allows to specify for any document field a named analyzer to use. This parameter cannot be <code>null</code>.
	 *
	 * @return The Lucene <code>Document</code> for the specified entity.
	 */
	public Document getDocument(T instance, Serializable id, Map<String, String> fieldToAnalyzerMap) {
		if ( fieldToAnalyzerMap == null ) {
			throw new IllegalArgumentException( "fieldToAnalyzerMap cannot be null" );
		}

		Document doc = new Document();
		final Class<?> entityType = Hibernate.getClass( instance );
		doc.setBoost( metadata.getClassBoost( instance ) );

		// add the class name of the entity to the document
		Field classField =
				new Field(
						CLASS_FIELDNAME,
						entityType.getName(),
						Field.Store.YES,
						Field.Index.NOT_ANALYZED,
						Field.TermVector.NO
				);
		doc.add( classField );

		// now add the entity id to the document
		LuceneOptions luceneOptions = new LuceneOptionsImpl(
				Field.Store.YES,
				Field.Index.NOT_ANALYZED, Field.TermVector.NO, idBoost
		);
		idBridge.set( idKeywordName, id, doc, luceneOptions );

		// finally add all other document fields
		Set<String> processedFieldNames = new HashSet<String>();
		buildDocumentFields( instance, doc, metadata, fieldToAnalyzerMap, processedFieldNames );
		return doc;
	}

	private void buildDocumentFields(Object instance, Document doc, PropertiesMetadata propertiesMetadata, Map<String, String> fieldToAnalyzerMap,
									 Set<String> processedFieldNames) {
		if ( instance == null ) {
			return;
		}

		// needed for field access: I cannot work in the proxied version
		Object unproxiedInstance = unproxy( instance );

		// process the class bridges
		for ( int i = 0; i < propertiesMetadata.classBridges.size(); i++ ) {
			FieldBridge fb = propertiesMetadata.classBridges.get( i );
			fb.set(
					propertiesMetadata.classNames.get( i ), unproxiedInstance,
					doc, propertiesMetadata.getClassLuceneOptions( i )
			);
		}

		// process the indexed fields
		for ( int i = 0; i < propertiesMetadata.fieldNames.size(); i++ ) {
			XMember member = propertiesMetadata.fieldGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );
			propertiesMetadata.fieldBridges.get( i ).set(
					propertiesMetadata.fieldNames.get( i ), value, doc,
					propertiesMetadata.getFieldLuceneOptions( i, value )
			);
		}

		// allow analyzer override for the fields added by the class and field bridges
		allowAnalyzerDiscriminatorOverride(
				doc, propertiesMetadata, fieldToAnalyzerMap, processedFieldNames, unproxiedInstance
		);

		// recursively process embedded objects
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
						buildDocumentFields(
								arrayValue, doc, embeddedMetadata, fieldToAnalyzerMap, processedFieldNames
						);
					}
					break;
				case COLLECTION:
					for ( Object collectionValue : ( Collection ) value ) {
						buildDocumentFields(
								collectionValue, doc, embeddedMetadata, fieldToAnalyzerMap, processedFieldNames
						);
					}
					break;
				case MAP:
					for ( Object collectionValue : ( ( Map ) value ).values() ) {
						buildDocumentFields(
								collectionValue, doc, embeddedMetadata, fieldToAnalyzerMap, processedFieldNames
						);
					}
					break;
				case OBJECT:
					buildDocumentFields( value, doc, embeddedMetadata, fieldToAnalyzerMap, processedFieldNames );
					break;
				default:
					throw new AssertionFailure(
							"Unknown embedded container: "
									+ propertiesMetadata.embeddedContainers.get( i )
					);
			}
		}
	}

	/**
	 * Allows a analyzer discriminator to override the analyzer used for any field in the Lucene document.
	 *
	 * @param doc The Lucene <code>Document</code> which shall be indexed.
	 * @param propertiesMetadata The metadata for the entity we currently add to the document.
	 * @param fieldToAnalyzerMap This map contains the actual override data. It is a map between document fields names and
	 * analyzer definition names. This map will be added to the <code>Work</code> instance and processed at actual indexing time.
	 * @param processedFieldNames A list of field names we have already processed.
	 * @param unproxiedInstance The entity we currently "add" to the document.
	 */
	private void allowAnalyzerDiscriminatorOverride(Document doc, PropertiesMetadata propertiesMetadata, Map<String, String> fieldToAnalyzerMap, Set<String> processedFieldNames, Object unproxiedInstance) {
		Discriminator discriminator = propertiesMetadata.discriminator;
		if ( discriminator == null ) {
			return;
		}

		Object value = null;
		if ( propertiesMetadata.discriminatorGetter != null ) {
			value = ReflectionHelper.getMemberValue( unproxiedInstance, propertiesMetadata.discriminatorGetter );
		}

		// now we give the discriminator the opportunity to specify a analyzer per field level
		for ( Object o : doc.getFields() ) {
			Field field = ( Field ) o;
			if ( !processedFieldNames.contains( field.name() ) ) {
				String analyzerName = discriminator.getAnalyzerDefinitionName( value, unproxiedInstance, field.name() );
				if ( analyzerName != null ) {
					fieldToAnalyzerMap.put( field.name(), analyzerName );
				}
				processedFieldNames.add( field.name() );
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

	/**
	 * Return the entity id if possible
	 * An IllegalStateException otherwise
	 *
	 * If the id is provided, we can't extract it from the entity
	 *
	 * @return entity id
	 */
	public Serializable getId(Object entity) {
		if( entity == null || idGetter == null) throw new IllegalStateException( "Cannot guess id form entity");
		return ( Serializable ) ReflectionHelper.getMemberValue( entity, idGetter );
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
        //process base fields
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

        //process fields of embedded
		final int nbrOfEmbeddedObjects = metadata.embeddedPropertiesMetadata.size();
		for ( int index = 0; index < nbrOfEmbeddedObjects; index++ ) {
			//there is nothing we can do for collections
			if ( metadata.embeddedContainers.get( index ) == PropertiesMetadata.Container.OBJECT ) {
				processFieldsForProjection(
						metadata.embeddedPropertiesMetadata.get( index ), fields, result, document
				);
			}
		}

        //process class bridges
        final int nbrOfClassBridges = metadata.classBridges.size();
		for ( int index = 0; index < nbrOfClassBridges; index++ ) {
			populateResult(
                    metadata.classNames.get(index),
                    metadata.classBridges.get(index),
                    metadata.classStores.get(index),
                    fields,
                    result,
                    document
            );
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
        for ( FieldBridge bridge : metadata.classBridges ) {
            if ( !( bridge instanceof TwoWayStringBridge || bridge instanceof TwoWayString2FieldBridgeAdaptor ) ) {
                allowFieldSelectionInProjection = false;
                return;
            }
        }
	}
}
