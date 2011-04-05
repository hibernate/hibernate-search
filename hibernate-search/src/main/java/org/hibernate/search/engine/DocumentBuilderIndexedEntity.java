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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.SearchException;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.CacheFromIndex;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.util.ContextualException2WayBridge;
import org.hibernate.search.bridge.util.ContextualExceptionBridge;
import org.hibernate.search.engine.impl.HibernateStatelessInitializer;
import org.hibernate.search.engine.spi.EntityInitializer;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.query.collector.FieldCacheCollectorFactory;
import org.hibernate.search.query.fieldcache.ClassLoadingStrategySelector;
import org.hibernate.search.query.fieldcache.FieldCacheLoadingType;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.DirectoryProviderFactory;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.util.HibernateHelper;
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
public class DocumentBuilderIndexedEntity<T> extends AbstractDocumentBuilder<T> {
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
	private XMember idGetter;

	/**
	 * Name of the document id field.
	 */
	private String idKeywordName;

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
	private boolean idProvided;

	/**
	 * The member - if any - annotated with @DocumentId
	 */
	private XProperty documentIdAnnotatedMember; //FIXME: to remove, needed only for isIdMatchingJpaId()

	/**
	 * The member - if any - annotated with @Id
	 */
	private XProperty jpaIdAnnotatedMember; //FIXME: to remove, needed only for isIdMatchingJpaId()
	
	/**
	 * Type of allowed FieldCache usage
	 */
	private final Set<org.hibernate.search.annotations.FieldCacheType> fieldCacheUsage;

	private final String identifierName;

	/**
	 * Which strategy to use to load values from the FieldCache
	 */
	private final FieldCacheCollectorFactory idFieldCacheCollectorFactory;

	/**
	 * Creates a document builder for entities annotated with <code>@Indexed</code>.
	 *
	 * @param clazz The class for which to build a <code>DocumentBuilderContainedEntity</code>
	 * @param context Handle to default configuration settings
	 * @param providerWrapper wrapper for access to directory providers for the underlying Lucene indexes
	 * @param reflectionManager Reflection manager to use for processing the annotations
	 * @param optimizationBlackList mutable register, keeps track of types on which we need to disable collection events optimizations
	 */
	public DocumentBuilderIndexedEntity(XClass clazz, ConfigContext context, DirectoryProviderFactory.DirectoryProviders providerWrapper,
			ReflectionManager reflectionManager, Set<XClass> optimizationBlackList) {
		super( clazz, context, providerWrapper.getSimilarity(), reflectionManager, optimizationBlackList );
		// special case @ProvidedId
		ProvidedId provided = findProvidedId( clazz, reflectionManager );
		if ( provided != null ) {
			idBridge = BridgeFactory.extractTwoWayType( provided.bridge(), clazz, reflectionManager );
			idKeywordName = provided.name();
			idProvided = true;
		}
		if ( idKeywordName == null ) {
			throw new SearchException( "No document id in: " + clazz );
		}
		CacheFromIndex fieldCacheOptions = clazz.getAnnotation( CacheFromIndex.class );
		if ( fieldCacheOptions == null ) {
			this.fieldCacheUsage = Collections.unmodifiableSet( EnumSet.of( org.hibernate.search.annotations.FieldCacheType.CLASS ) );
		}
		else {
			EnumSet<org.hibernate.search.annotations.FieldCacheType> enabledTypes = EnumSet.noneOf( org.hibernate.search.annotations.FieldCacheType.class );
			for ( org.hibernate.search.annotations.FieldCacheType t : fieldCacheOptions.value() ) {
				enabledTypes.add( t );
			}
			if ( enabledTypes.size() != 1 && enabledTypes.contains( org.hibernate.search.annotations.FieldCacheType.NOTHING ) ) {
				throw new SearchException( "CacheFromIndex configured with conflicting parameters:" +
						" if FieldCacheType.NOTHING is enabled, no other options can be added" );
			}
			this.fieldCacheUsage = Collections.unmodifiableSet( enabledTypes );
		}
		checkAllowFieldSelection();
		idFieldCacheCollectorFactory = figureIdFieldCacheUsage();
		if ( log.isDebugEnabled() ) {
			log.debug(
					"Field selection in projections is set to {} for entity {}.",
					allowFieldSelectionInProjection,
					clazz
			);
		}
		this.entityState = EntityState.INDEXED;
		this.directoryProviders = providerWrapper.getProviders();
		this.shardingStrategy = providerWrapper.getSelectionStrategy();
		this.identifierName = idProvided ? null : idGetter.getName();
	}

	private FieldCacheCollectorFactory figureIdFieldCacheUsage() {
		if ( this.fieldCacheUsage.contains( org.hibernate.search.annotations.FieldCacheType.ID ) ) {
			FieldCacheLoadingType collectorTypeForId = ClassLoadingStrategySelector.guessAppropriateCollectorType( idBridge );
			if ( collectorTypeForId == null ) {
				log.warn( "FieldCache was enabled on class " + this.beanClass + " but for this type of identifier we can't extract values from the FieldCache: cache disabled" );
				return null;
			}
			TwoWayStringBridge twoWayIdStringBridge = ClassLoadingStrategySelector.getTwoWayStringBridge( idBridge );
			return new FieldCacheCollectorFactory( getIdKeywordName(), collectorTypeForId, twoWayIdStringBridge );
		}
		return null;
	}

	public XMember getIdGetter() {
		return idGetter;
	}
	
	public FieldCacheCollectorFactory getIdFieldCacheCollectionFactory() {
		return idFieldCacheCollectorFactory;
	}

	protected void documentBuilderSpecificChecks(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context) {
		checkDocumentId( member, propertiesMetadata, isRoot, prefix, context );
	}

	protected void checkDocumentId(XProperty member, PropertiesMetadata propertiesMetadata, boolean isRoot, String prefix, ConfigContext context) {
		Annotation idAnnotation = getIdAnnotation( member, context );
		NumericField numericFieldAnn = member.getAnnotation( NumericField.class );
		if ( idAnnotation != null ) {
			String attributeName = getIdAttributeName( member, idAnnotation );
			if ( isRoot ) {
				if ( explicitDocumentId ) {
					throw new SearchException( "More than one @DocumentId specified on entity " + getBeanClass().getName() );
				}
				if ( idAnnotation instanceof DocumentId ) {
					explicitDocumentId = true;
				}
				idKeywordName = prefix + attributeName;

				FieldBridge fieldBridge = BridgeFactory.guessType( null, numericFieldAnn, member, reflectionManager );
				if ( fieldBridge instanceof TwoWayFieldBridge ) {
					idBridge = (TwoWayFieldBridge) fieldBridge;
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
				propertiesMetadata.fieldStore.add( Store.YES );
				propertiesMetadata.fieldIndex.add( getIndex( Index.UN_TOKENIZED ) );
				propertiesMetadata.fieldTermVectors.add( getTermVector( TermVector.NO ) );
				propertiesMetadata.fieldNullTokens.add( null );
				propertiesMetadata.fieldBridges.add( BridgeFactory.guessType( null, null, member, reflectionManager ) );
				propertiesMetadata.fieldBoosts.add( getBoost( member, null ) );
				propertiesMetadata.precisionSteps.add( getPrecisionStep( null ) );
				propertiesMetadata.dynamicFieldBoosts.add( getDynamicBoost( member ) );
				// property > entity analyzer (no field analyzer)
				Analyzer analyzer = getAnalyzer( member, context );
				if ( analyzer == null ) {
					analyzer = propertiesMetadata.analyzer;
				}
				if ( analyzer == null ) {
					throw new AssertionFailure( "Analyzer should not be undefined" );
				}
				addToScopedAnalyzer( fieldName, analyzer, Index.UN_TOKENIZED );
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
	private Annotation getIdAnnotation(XProperty member, ConfigContext context) {
		Annotation idAnnotation = null;

		// check for explicit DocumentId
		DocumentId documentIdAnn = member.getAnnotation( DocumentId.class );
		if ( documentIdAnn != null ) {
			idAnnotation = documentIdAnn;
			documentIdAnnotatedMember = member;
		}
		// check for JPA @Id
		if ( context.isJpaPresent() ) {
			Annotation jpaId;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Annotation> jpaIdClass =
						org.hibernate.annotations.common.util.ReflectHelper
								.classForName( "javax.persistence.Id", ConfigContext.class );
				jpaId = member.getAnnotation( jpaIdClass );
			}
			catch ( ClassNotFoundException e ) {
				throw new SearchException( "Unable to load @Id.class even though it should be present ?!" );
			}
			if ( jpaId != null ) {
				jpaIdAnnotatedMember = member;
				if ( documentIdAnn == null ) {
					log.debug( "Found JPA id and using it as document id" );
					idAnnotation = jpaId;
				}
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
	
	public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, boolean delete, boolean add, boolean batch, List<LuceneWork> queue) {
		String idInString = objectToString( idBridge, idKeywordName, id );
		if ( delete ) {
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
		}
		if ( add ) {
			queue.add( createAddWork( entityClass, entity, id, idInString, HibernateStatelessInitializer.INSTANCE, batch ) );
		}
	}
	
	private String objectToString(TwoWayFieldBridge bridge, String fieldName, Object value) {
		ContextualException2WayBridge contextualBridge = new ContextualException2WayBridge()
				.setClass( getBeanClass() )
				.setFieldBridge( bridge )
				.setFieldName( fieldName );
		return contextualBridge.objectToString( value );
	}

	private String objectToString(StringBridge bridge, String fieldName, Object value) {
		ContextualException2WayBridge contextualBridge = new ContextualException2WayBridge()
				.setClass( getBeanClass() )
				.setStringBridge( bridge )
				.setFieldName( fieldName );
		return contextualBridge.objectToString( value );
	}

	public AddLuceneWork createAddWork(Class<T> entityClass, T entity, Serializable id, String idInString, EntityInitializer sessionInitializer, boolean isBatch) {
		Map<String, String> fieldToAnalyzerMap = new HashMap<String, String>();
		Document doc = getDocument( entity, id, fieldToAnalyzerMap, sessionInitializer );
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
	 * @param objectInitializer 
	 *
	 * @return The Lucene <code>Document</code> for the specified entity.
	 */
	public Document getDocument(T instance, Serializable id, Map<String, String> fieldToAnalyzerMap, EntityInitializer objectInitializer) {
		if ( fieldToAnalyzerMap == null ) {
			throw new IllegalArgumentException( "fieldToAnalyzerMap cannot be null" );
		}

		Document doc = new Document();
		final Class<?> entityType = objectInitializer.getClass( instance );
		doc.setBoost( getMetadata().getClassBoost( instance ) );

		// add the class name of the entity to the document
		Field classField =
				new Field(
						CLASS_FIELDNAME,
						entityType.getName(),
						Field.Store.YES,
						Field.Index.NOT_ANALYZED_NO_NORMS,
						Field.TermVector.NO
				);
		doc.add( classField );

		// now add the entity id to the document
		LuceneOptions luceneOptions = new LuceneOptionsImpl(
				Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS,
				Field.TermVector.NO,
				idBoost
		);
		final ContextualExceptionBridge contextualBridge = new ContextualExceptionBridge()
				.setFieldBridge( idBridge )
				.setClass( entityType )
				.setFieldName( idKeywordName );
		if ( idGetter != null ) {
			contextualBridge.pushMethod( idGetter );
		}
		contextualBridge.set( idKeywordName, id, doc, luceneOptions );
		if ( idGetter != null ) {
			contextualBridge.popMethod();
		}

		// finally add all other document fields
		Set<String> processedFieldNames = new HashSet<String>();
		buildDocumentFields( instance, doc, getMetadata(), fieldToAnalyzerMap, processedFieldNames, contextualBridge, objectInitializer );
		return doc;
	}

	private void buildDocumentFields(Object instance,
									 Document doc,
									 PropertiesMetadata propertiesMetadata,
									 Map<String, String> fieldToAnalyzerMap,
									 Set<String> processedFieldNames,
									 ContextualExceptionBridge contextualBridge,
									 EntityInitializer objectInitializer) {
		if ( instance == null ) {
			return;
		}

		// needed for field access: I cannot work in the proxied version
		Object unproxiedInstance = objectInitializer.unproxy( instance );

		// process the class bridges
		for ( int i = 0; i < propertiesMetadata.classBridges.size(); i++ ) {
			FieldBridge fb = propertiesMetadata.classBridges.get( i );
			final String fieldName = propertiesMetadata.classNames.get( i );
			contextualBridge
					.setFieldBridge( fb )
					.setFieldName( fieldName )
					.set(
							fieldName, unproxiedInstance,
							doc, propertiesMetadata.getClassLuceneOptions( i )
					);
		}

		// process the indexed fields
		for ( int i = 0; i < propertiesMetadata.fieldNames.size(); i++ ) {
			XMember member = propertiesMetadata.fieldGetters.get( i );
			Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );

			final FieldBridge fieldBridge = propertiesMetadata.fieldBridges.get( i );
			final String fieldName = propertiesMetadata.fieldNames.get( i );
			contextualBridge
					.setFieldBridge( fieldBridge )
					.pushMethod( member )
					.setFieldName( fieldName )
					.set(
							fieldName, value, doc,
							propertiesMetadata.getFieldLuceneOptions( i, value )
					);
			contextualBridge.popMethod();
		}

		// allow analyzer override for the fields added by the class and field bridges
		allowAnalyzerDiscriminatorOverride(
				doc, propertiesMetadata, fieldToAnalyzerMap, processedFieldNames, unproxiedInstance
		);

		// recursively process embedded objects
		for ( int i = 0; i < propertiesMetadata.embeddedGetters.size(); i++ ) {
			XMember member = propertiesMetadata.embeddedGetters.get( i );
			contextualBridge.pushMethod( member );
			Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );
			//TODO handle boost at embedded level: already stored in propertiesMedatada.boost

			if ( value == null ) {
				continue;
			}
			PropertiesMetadata embeddedMetadata = propertiesMetadata.embeddedPropertiesMetadata.get( i );
			switch ( propertiesMetadata.embeddedContainers.get( i ) ) {
				case ARRAY:
					Object[] array = objectInitializer.initializeArray( (Object[]) value );
					for ( Object arrayValue : (Object[]) value ) {
						buildDocumentFields(
								arrayValue,
								doc,
								embeddedMetadata,
								fieldToAnalyzerMap,
								processedFieldNames,
								contextualBridge,
								objectInitializer
						);
					}
					break;
				case COLLECTION:
					Collection collection = objectInitializer.initializeCollection( (Collection) value );
					for ( Object collectionValue : collection ) {
						buildDocumentFields(
								collectionValue,
								doc,
								embeddedMetadata,
								fieldToAnalyzerMap,
								processedFieldNames,
								contextualBridge,
								objectInitializer
						);
					}
					break;
				case MAP:
					Map map = objectInitializer.initializeMap( (Map) value );
					for ( Object collectionValue : map.values() ) {
						buildDocumentFields(
								collectionValue,
								doc,
								embeddedMetadata,
								fieldToAnalyzerMap,
								processedFieldNames,
								contextualBridge,
								objectInitializer
						);
					}
					break;
				case OBJECT:
					buildDocumentFields(
							value,
							doc,
							embeddedMetadata,
							fieldToAnalyzerMap,
							processedFieldNames,
							contextualBridge,
							objectInitializer
					);
					break;
				default:
					throw new AssertionFailure(
							"Unknown embedded container: "
									+ propertiesMetadata.embeddedContainers.get( i )
					);
			}
			contextualBridge.popMethod();
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
			Fieldable field = (Fieldable) o;
			if ( !processedFieldNames.contains( field.name() ) ) {
				String analyzerName = discriminator.getAnalyzerDefinitionName( value, unproxiedInstance, field.name() );
				if ( analyzerName != null ) {
					fieldToAnalyzerMap.put( field.name(), analyzerName );
				}
				processedFieldNames.add( field.name() );
			}
		}
	}

	public String getIdentifierName() {
		return identifierName;
	}
	
	public DirectoryProvider[] getDirectoryProviders() {
		if ( getEntityState() != EntityState.INDEXED ) {
			throw new AssertionFailure( "Contained in only entity: getDirectoryProvider should not have been called." );
		}
		return directoryProviders;
	}

	public IndexShardingStrategy getDirectoryProviderSelectionStrategy() {
		if ( getEntityState() != EntityState.INDEXED ) {
			throw new AssertionFailure(
					"Contained in only entity: getDirectoryProviderSelectionStrategy should not have been called."
			);
		}
		return shardingStrategy;
	}

	public boolean allowFieldSelectionInProjection() {
		return allowFieldSelectionInProjection;
	}
	
	public Set<org.hibernate.search.annotations.FieldCacheType> getFieldCacheOption() {
		return fieldCacheUsage;
	}

	public Term getTerm(Serializable id) {
		return new Term( idKeywordName, objectToString( idBridge, idKeywordName, id ) );
	}

	public TwoWayFieldBridge getIdBridge() {
		return idBridge;
	}

	public String getIdKeywordName() {
		return idKeywordName;
	}

	/**
	 * Return the id used for indexing if possible
	 * An IllegalStateException otherwise
	 * <p/>
	 * If the id is provided, we can't extract it from the entity
	 *
	 * @param entity The entity for which to return the id. Cannot be {@code null}.
	 *
	 * @return entity id
	 */
	@Override
	public Serializable getId(Object entity) {
		if ( entity == null || idGetter == null || idProvided ) {
			throw new IllegalStateException( "Cannot guess id from entity" );
		}
		Object unproxiedEntity = HibernateHelper.unproxy( entity );
		return (Serializable) ReflectionHelper.getMemberValue( unproxiedEntity, idGetter );
	}
	
	public String objectToString(String fieldName, Object value) {
		if ( fieldName == null ) {
			throw new AssertionFailure( "Field name should not be null" );
		}
		if ( fieldName.equals( idKeywordName ) ) {
			return objectToString( idBridge, idKeywordName, value );
		}
		else {
			FieldBridge bridge = getBridge( getMetadata(), fieldName );
			if ( bridge != null ) {
				final Class<? extends FieldBridge> bridgeClass = bridge.getClass();
				if ( TwoWayFieldBridge.class.isAssignableFrom( bridgeClass ) ) {
					return objectToString( (TwoWayFieldBridge) bridge, fieldName, value );
				}
				else if ( StringBridge.class.isAssignableFrom( bridgeClass ) ) {
					return objectToString( (StringBridge) bridge, fieldName, value );
				}
				throw new SearchException(
						"FieldBridge " + bridgeClass + " does not have a objectToString method: field "
								+ fieldName + " in " + getBeanXClass()
				);
			}
		}
		throw new SearchException( "Unable to find field " + fieldName + " in " + getBeanXClass() );
	}

	private FieldBridge getBridge(List<String> names, List<FieldBridge> bridges, String fieldName) {
		int index = names.indexOf( fieldName );
		if ( index != -1 ) {
			return bridges.get( index );
		}
		else {
			return null;
		}
	}

	public FieldBridge getBridge(String fieldName) {
		return getBridge( getMetadata(), fieldName );
	}

	private FieldBridge getBridge(PropertiesMetadata metadata, String fieldName) {
		//process base fields
		FieldBridge fieldBridge = getBridge( metadata.fieldNames, metadata.fieldBridges, fieldName );
		if ( fieldBridge != null ) {
			return fieldBridge;
		}

		//process fields of embedded
		final int nbrOfEmbeddedObjects = metadata.embeddedPropertiesMetadata.size();
		for ( int index = 0; index < nbrOfEmbeddedObjects; index++ ) {
			fieldBridge = getBridge( metadata.embeddedPropertiesMetadata.get( index ), fieldName );
			if ( fieldBridge != null ) {
				return fieldBridge;
			}
		}

		//process class bridges
		fieldBridge = getBridge( metadata.classNames, metadata.classBridges, fieldName );
		if ( fieldBridge != null ) {
			return fieldBridge;
		}
		return null;
	}

	/**
	 * Checks whether all involved bridges are two way string bridges. If so we can optimize document retrieval
	 * by using <code>FieldSelector</code>. See HSEARCH-213.
	 */
	private void checkAllowFieldSelection() {
		allowFieldSelectionInProjection = true;
		if ( !( idBridge instanceof TwoWayStringBridge
				|| idBridge instanceof TwoWayString2FieldBridgeAdaptor
				|| idBridge instanceof NumericFieldBridge ) ) {
			allowFieldSelectionInProjection = false;
			return;
		}
		for ( FieldBridge bridge : getMetadata().fieldBridges ) {
			if ( !( bridge instanceof TwoWayStringBridge
					|| bridge instanceof TwoWayString2FieldBridgeAdaptor
					|| bridge instanceof NumericFieldBridge ) ) {
				allowFieldSelectionInProjection = false;
				return;
			}
		}
		for ( FieldBridge bridge : getMetadata().classBridges ) {
			if ( !( bridge instanceof TwoWayStringBridge
					|| bridge instanceof TwoWayString2FieldBridgeAdaptor
					|| bridge instanceof NumericFieldBridge ) ) {
				allowFieldSelectionInProjection = false;
				return;
			}
		}
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
			name = (String) m.invoke( idAnnotation );
		}
		catch ( Exception e ) {
			// ignore
		}
		return ReflectionHelper.getAttributeName( member, name );
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 */
	@Override
	boolean requiresProvidedId() {
		return this.idProvided;
	}
	
	/**
	 * FIXME remove the need for such a method, we should always be able to rely on Work.id,
	 * but to respect @DocumentId which is being processed in the DocumentBuilder currently
	 * finding out which id we need is tricky, and requires helpers method like this one.
	 */
	@Override
	boolean isIdMatchingJpaId() {
		return ( ! idProvided &&
				( documentIdAnnotatedMember == null || documentIdAnnotatedMember.equals( jpaIdAnnotatedMember ) )
								);
	}
}
