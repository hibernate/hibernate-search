/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;

import org.hibernate.search.bridge.builtin.NumericEncodingCalendarBridge;
import org.hibernate.search.bridge.builtin.NumericEncodingDateBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.StringEncodingDateBridge;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.CacheFromIndex;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.impl.LuceneOptionsImpl;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.query.collector.impl.FieldCacheCollectorFactory;
import org.hibernate.search.query.fieldcache.impl.ClassLoadingStrategySelector;
import org.hibernate.search.query.fieldcache.impl.FieldCacheLoadingType;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Set up and provide a manager for classes which are directly annotated with {@code @Indexed}.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilderIndexedEntity extends AbstractDocumentBuilder {
	private static final Log log = LoggerFactory.make();

	private static final LuceneOptions NULL_EMBEDDED_MARKER_OPTIONS;

	static {
		DocumentFieldMetadata fieldMetadata =
				new DocumentFieldMetadata.Builder(
						null,
						Store.NO,
						Field.Index.NOT_ANALYZED_NO_NORMS,
						Field.TermVector.NO
				)
						.boost( 1F )
						.build();
		NULL_EMBEDDED_MARKER_OPTIONS = new LuceneOptionsImpl( fieldMetadata, 1f, 1f );
	}

	/**
	 * Flag indicating whether {@link org.apache.lucene.search.IndexSearcher#doc(int, org.apache.lucene.index.StoredFieldVisitor)}
	 * can be used in order to retrieve documents. This is only safe to do if we know that
	 * all involved bridges are implementing <code>TwoWayStringBridge</code>. See HSEARCH-213.
	 */
	private boolean allowFieldSelectionInProjection = false;

	/**
	 * Flag indicating whether there is an explicit id (@DocumentId or @Id) or not. When Search is used as make
	 * for example using JBoss Cache Searchable the <code>idKeywordName</code> will be provided.
	 */
	private boolean idProvided;

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
	 * The document field name of the document id
	 */
	private final String idFieldName;

	/**
	 * The property metadata for the document id (not that in the case of a provided id the id getter can be {@code null}.
	 */
	private PropertyMetadata idPropertyMetadata;

	/**
	 * Creates a document builder for entities annotated with <code>@Indexed</code>.
	 *
	 * @param clazz The class for which to build a <code>DocumentBuilderContainedEntity</code>
	 * @param context Handle to default configuration settings
	 * @param reflectionManager Reflection manager to use for processing the annotations
	 * @param optimizationBlackList mutable register, keeps track of types on which we need to disable collection events optimizations
	 * @param instanceInitializer helper class for class object graph navigation
	 */
	public DocumentBuilderIndexedEntity(XClass clazz, TypeMetadata typeMetadata, ConfigContext context,
			ReflectionManager reflectionManager, Set<XClass> optimizationBlackList, InstanceInitializer instanceInitializer) {
		super( clazz, typeMetadata, reflectionManager, optimizationBlackList, instanceInitializer );

		ProvidedId providedIdAnnotation = findProvidedId( clazz, reflectionManager );
		if ( providedIdAnnotation != null || context.isProvidedIdImplicit() ) {
			idProvided = true;
		}

		if ( idPropertyMetadata == null ) {
			idPropertyMetadata = getTypeMetadata().getIdPropertyMetadata();
		}

		if ( idPropertyMetadata == null ) {
			throw log.noDocumentIdFoundException( clazz.getName() );
		}

		idFieldName = idPropertyMetadata.getFieldMetadata().iterator().next().getName();

		CacheFromIndex fieldCacheOptions = clazz.getAnnotation( CacheFromIndex.class );
		if ( fieldCacheOptions == null ) {
			this.fieldCacheUsage = Collections.unmodifiableSet( EnumSet.of( org.hibernate.search.annotations.FieldCacheType.CLASS ) );
		}
		else {
			EnumSet<org.hibernate.search.annotations.FieldCacheType> enabledTypes = EnumSet.noneOf( org.hibernate.search.annotations.FieldCacheType.class );
			Collections.addAll( enabledTypes, fieldCacheOptions.value() );
			if ( enabledTypes.size() != 1 && enabledTypes.contains( org.hibernate.search.annotations.FieldCacheType.NOTHING ) ) {
				throw new SearchException(
						"CacheFromIndex configured with conflicting parameters:" +
								" if FieldCacheType.NOTHING is enabled, no other options can be added"
				);
			}
			this.fieldCacheUsage = Collections.unmodifiableSet( enabledTypes );
		}
		checkAllowFieldSelection();
		idFieldCacheCollectorFactory = figureIdFieldCacheUsage();
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Field selection in projections is set to %b for entity %s.",
					allowFieldSelectionInProjection,
					clazz
			);
		}
		this.entityState = EntityState.INDEXED;
		this.identifierName = idProvided ? null : idPropertyMetadata.getPropertyAccessor().getName();
	}

	private FieldCacheCollectorFactory figureIdFieldCacheUsage() {
		if ( this.fieldCacheUsage.contains( org.hibernate.search.annotations.FieldCacheType.ID ) ) {
			FieldCacheLoadingType collectorTypeForId = ClassLoadingStrategySelector.guessAppropriateCollectorType(
					getIdBridge()
			);
			if ( collectorTypeForId == null ) {
				log.cannotExtractValueForIdentifier( getBeanClass() );
				return null;
			}
			TwoWayStringBridge twoWayIdStringBridge = ClassLoadingStrategySelector.getTwoWayStringBridge( getIdBridge() );
			return new FieldCacheCollectorFactory( getIdKeywordName(), collectorTypeForId, twoWayIdStringBridge );
		}
		return null;
	}

	public XMember getIdGetter() {
		return idPropertyMetadata.getPropertyAccessor();
	}

	public FieldCacheCollectorFactory getIdFieldCacheCollectionFactory() {
		return idFieldCacheCollectorFactory;
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

	@Override
	public void addWorkToQueue(Class<?> entityClass, Object entity, Serializable id, boolean delete, boolean add, List<LuceneWork> queue, ConversionContext contextualBridge) {
		DocumentFieldMetadata idFieldMetadata = idPropertyMetadata.getFieldMetadata( idFieldName );
		String idInString = objectToString( getIdBridge(), idFieldMetadata.getName(), id, contextualBridge );
		if ( delete && !add ) {
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
		}
		else if ( add && !delete ) {
			queue.add(
					createAddWork(
							entityClass,
							entity,
							id,
							idInString,
							getInstanceInitializer(),
							contextualBridge
					)
			);
		}
		else if ( add && delete ) {
			queue.add(
					createUpdateWork(
							entityClass,
							entity,
							id,
							idInString,
							getInstanceInitializer(),
							contextualBridge
					)
			);
		}
	}

	private String objectToString(TwoWayFieldBridge bridge, String fieldName, Object value, ConversionContext conversionContext) {
		conversionContext.pushProperty( fieldName );
		String stringValue;
		try {
			stringValue = conversionContext
					.setClass( getBeanClass() )
					.twoWayConversionContext( bridge )
					.objectToString( value );
		}
		finally {
			conversionContext.popProperty();
		}
		return stringValue;
	}

	private String objectToString(StringBridge bridge, String fieldName, Object value, ConversionContext conversionContext) {
		conversionContext.pushProperty( fieldName );
		String stringValue;
		try {
			stringValue = conversionContext
					.setClass( getBeanClass() )
					.stringConversionContext( bridge )
					.objectToString( value );
		}
		finally {
			conversionContext.popProperty();
		}
		return stringValue;
	}

	public AddLuceneWork createAddWork(Class<?> entityClass, Object entity, Serializable id, String idInString, InstanceInitializer sessionInitializer, ConversionContext conversionContext) {
		Map<String, String> fieldToAnalyzerMap = new HashMap<String, String>();
		Document doc = getDocument( entity, id, fieldToAnalyzerMap, sessionInitializer, conversionContext, null );
		final AddLuceneWork addWork;
		if ( fieldToAnalyzerMap.isEmpty() ) {
			addWork = new AddLuceneWork( id, idInString, entityClass, doc );
		}
		else {
			addWork = new AddLuceneWork( id, idInString, entityClass, doc, fieldToAnalyzerMap );
		}
		return addWork;
	}

	public UpdateLuceneWork createUpdateWork(Class entityClass, Object entity, Serializable id, String idInString, InstanceInitializer sessionInitializer, ConversionContext contextualBridge) {
		Map<String, String> fieldToAnalyzerMap = new HashMap<String, String>();
		Document doc = getDocument( entity, id, fieldToAnalyzerMap, sessionInitializer, contextualBridge, null );
		final UpdateLuceneWork addWork;
		if ( fieldToAnalyzerMap.isEmpty() ) {
			addWork = new UpdateLuceneWork( id, idInString, entityClass, doc );
		}
		else {
			addWork = new UpdateLuceneWork( id, idInString, entityClass, doc, fieldToAnalyzerMap );
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
	 * @param objectInitializer used to ensure that all objects are initalized
	 * @param conversionContext a {@link org.hibernate.search.bridge.spi.ConversionContext} object.
	 * @param includedFieldNames list of field names to consider. Others can be excluded. Null if all fields are considered.
	 *
	 * @return The Lucene <code>Document</code> for the specified entity.
	 */
	public Document getDocument(Object instance, Serializable id, Map<String, String> fieldToAnalyzerMap, InstanceInitializer objectInitializer, ConversionContext conversionContext, String[] includedFieldNames) {
		// TODO as it is, includedFieldNames is not generally useful as we don't know if a fieldbridge creates specific fields or not
		// TODO only used at the moment to filter the id field and the class field

		if ( fieldToAnalyzerMap == null ) {
			throw new IllegalArgumentException( "fieldToAnalyzerMap cannot be null" );
		}
		//sensible default for outside callers
		if ( objectInitializer == null ) {
			objectInitializer = getInstanceInitializer();
		}

		Document doc = new Document();
		final Class<?> entityType = objectInitializer.getClass( instance );
		final float documentLevelBoost = getMetadata().getClassBoost( instance );

		// add the class name of the entity to the document
		if ( containsFieldName( ProjectionConstants.OBJECT_CLASS, includedFieldNames ) ) {
			Field classField =
					new Field(
							ProjectionConstants.OBJECT_CLASS,
							entityType.getName(),
							Field.Store.YES,
							Field.Index.NOT_ANALYZED_NO_NORMS,
							Field.TermVector.NO
					);
			doc.add( classField );
		}

		// now add the entity id to the document
		if ( containsFieldName( idFieldName, includedFieldNames ) ) {
			DocumentFieldMetadata idFieldMetaData = idPropertyMetadata.getFieldMetadata( idFieldName );
			LuceneOptions luceneOptions = new LuceneOptionsImpl( idFieldMetaData, idFieldMetaData.getBoost(), documentLevelBoost );
			final FieldBridge contextualizedBridge = conversionContext.oneWayConversionContext( getIdBridge() );
			conversionContext.setClass( entityType );
			conversionContext.pushProperty( idFieldMetaData.getName() );

			try {
				contextualizedBridge.set( idFieldMetaData.getName(), id, doc, luceneOptions );
			}
			finally {
				conversionContext.popProperty();
			}
		}

		// finally add all other document fields
		Set<String> processedFieldNames = new HashSet<String>();
		buildDocumentFields(
				instance,
				doc,
				getMetadata(),
				fieldToAnalyzerMap,
				processedFieldNames,
				conversionContext,
				objectInitializer,
				documentLevelBoost
		);
		return doc;
	}

	private void buildDocumentFields(Object instance,
			Document doc,
			TypeMetadata typeMetadata,
			Map<String, String> fieldToAnalyzerMap,
			Set<String> processedFieldNames,
			ConversionContext conversionContext,
			InstanceInitializer objectInitializer,
			final float documentBoost) {

		// needed for field access: I cannot work in the proxied version
		Object unproxiedInstance = unproxy( instance, objectInitializer );

		// process the class bridges
		for ( DocumentFieldMetadata fieldMetadata : typeMetadata.getClassBridgeMetadata() ) {
			FieldBridge fieldBridge = fieldMetadata.getFieldBridge();
			final String fieldName = fieldMetadata.getName();
			final FieldBridge oneWayConversionContext = conversionContext.oneWayConversionContext( fieldBridge );
			conversionContext.pushProperty( fieldName );
			try {
				oneWayConversionContext.set(
						fieldName,
						unproxiedInstance,
						doc,
						typeMetadata.getClassLuceneOptions( fieldMetadata, documentBoost )
				);
			}
			finally {
				conversionContext.popProperty();
			}
		}

		// process the indexed fields
		XMember previousMember = null;
		Object currentFieldValue = null;
		for ( PropertyMetadata propertyMetadata : typeMetadata.getAllPropertyMetadata() ) {
			XMember member = propertyMetadata.getPropertyAccessor();
			if ( previousMember != member ) {
				currentFieldValue = unproxy(
						ReflectionHelper.getMemberValue( unproxiedInstance, member ),
						objectInitializer
				);
				previousMember = member;
				if ( member.isCollection() ) {
					if ( currentFieldValue instanceof Collection ) {
						objectInitializer.initializeCollection( (Collection) currentFieldValue );
					}
					else if ( currentFieldValue instanceof Map ) {
						objectInitializer.initializeMap( (Map) currentFieldValue );
					}
				}
			}

			try {
				conversionContext.pushProperty( propertyMetadata.getPropertyAccessorName() );

				for ( DocumentFieldMetadata fieldMetadata : propertyMetadata.getFieldMetadata() ) {
					final FieldBridge fieldBridge = fieldMetadata.getFieldBridge();
					final String fieldName = fieldMetadata.getName();
					final FieldBridge oneWayConversionContext = conversionContext.oneWayConversionContext( fieldBridge );

					oneWayConversionContext.set(
							fieldName,
							currentFieldValue,
							doc,
							typeMetadata.getFieldLuceneOptions( propertyMetadata, fieldMetadata, currentFieldValue, documentBoost )
					);
				}
			}
			finally {
				conversionContext.popProperty();
			}
		}

		// allow analyzer override for the fields added by the class and field bridges
		allowAnalyzerDiscriminatorOverride(
				doc, typeMetadata, fieldToAnalyzerMap, processedFieldNames, unproxiedInstance
		);

		// recursively process embedded objects
		for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
			XMember member = embeddedTypeMetadata.getEmbeddedGetter();
			conversionContext.pushProperty( embeddedTypeMetadata.getEmbeddedFieldName() );
			try {
				Object value = ReflectionHelper.getMemberValue( unproxiedInstance, member );
				//TODO handle boost at embedded level: already stored in propertiesMedatada.boost

				if ( value == null ) {
					processEmbeddedNullValue( doc, embeddedTypeMetadata, conversionContext );
					continue;
				}

				switch ( embeddedTypeMetadata.getEmbeddedContainer() ) {
					case ARRAY:
						Object[] array = objectInitializer.initializeArray( (Object[]) value );
						for ( Object arrayValue : array ) {
							buildDocumentFields(
									arrayValue,
									doc,
									embeddedTypeMetadata,
									fieldToAnalyzerMap,
									processedFieldNames,
									conversionContext,
									objectInitializer,
									documentBoost
							);
						}
						break;
					case COLLECTION:
						Collection collection = objectInitializer.initializeCollection( (Collection) value );
						for ( Object collectionValue : collection ) {
							buildDocumentFields(
									collectionValue,
									doc,
									embeddedTypeMetadata,
									fieldToAnalyzerMap,
									processedFieldNames,
									conversionContext,
									objectInitializer,
									documentBoost
							);
						}
						break;
					case MAP:
						Map map = objectInitializer.initializeMap( (Map) value );
						for ( Object collectionValue : map.values() ) {
							buildDocumentFields(
									collectionValue,
									doc,
									embeddedTypeMetadata,
									fieldToAnalyzerMap,
									processedFieldNames,
									conversionContext,
									objectInitializer,
									documentBoost
							);
						}
						break;
					case OBJECT:
						buildDocumentFields(
								value,
								doc,
								embeddedTypeMetadata,
								fieldToAnalyzerMap,
								processedFieldNames,
								conversionContext,
								objectInitializer,
								documentBoost
						);
						break;
					default:
						throw new AssertionFailure(
								"Unknown embedded container: "
										+ embeddedTypeMetadata.getEmbeddedContainer()
						);
				}
			}
			finally {
				conversionContext.popProperty();
			}
		}
	}

	/**
	 * Check if a given value is present in an array.
	 *
	 * A array {@code null} contains all possible values.
	 * Otherwise, a value {@code null} is always considered non present.
	 * The last behavior is not currently used, it is merely an optimization.
	 */
	private boolean containsFieldName(String value, String[] array) {
		//null array means contains all possible values
		if ( array == null ) {
			return true;
		}
		//null value is meaningless
		if ( value == null ) {
			return false;
		}
		for ( String containedValue : array ) {
			if ( value.equals( containedValue ) ) {
				return true;
			}
		}
		return false;
	}

	private void processEmbeddedNullValue(Document doc, EmbeddedTypeMetadata embeddedTypeMetadata, ConversionContext conversionContext) {
		final String nullMarker = embeddedTypeMetadata.getEmbeddedNullToken();
		if ( nullMarker != null ) {
			String fieldName = embeddedTypeMetadata.getEmbeddedNullFieldName();
			FieldBridge fieldBridge = embeddedTypeMetadata.getEmbeddedNullFieldBridge();
			final FieldBridge contextualizedBridge = conversionContext.oneWayConversionContext( fieldBridge );
			conversionContext.pushProperty( fieldName );
			try {
				contextualizedBridge.set( fieldName, null, doc, NULL_EMBEDDED_MARKER_OPTIONS );
			}
			finally {
				conversionContext.popProperty();
			}
		}
	}

	private Object unproxy(Object instance, InstanceInitializer objectInitializer) {
		if ( instance == null ) {
			return null;
		}
		return objectInitializer.unproxy( instance );
	}

	/**
	 * Allows a analyzer discriminator to override the analyzer used for any field in the Lucene document.
	 *
	 * @param doc The Lucene <code>Document</code> which shall be indexed.
	 * @param typeMetadata The metadata for the entity we currently add to the document.
	 * @param fieldToAnalyzerMap This map contains the actual override data. It is a map between document fields names and
	 * analyzer definition names. This map will be added to the <code>Work</code> instance and processed at actual indexing time.
	 * @param processedFieldNames A list of field names we have already processed.
	 * @param unproxiedInstance The entity we currently "add" to the document.
	 */
	private void allowAnalyzerDiscriminatorOverride(Document doc,
			TypeMetadata typeMetadata,
			Map<String, String> fieldToAnalyzerMap,
			Set<String> processedFieldNames,
			Object unproxiedInstance) {

		Discriminator discriminator = typeMetadata.getDiscriminator();
		if ( discriminator == null ) {
			return;
		}

		Object value = null;
		if ( typeMetadata.getDiscriminatorGetter() != null ) {
			value = ReflectionHelper.getMemberValue( unproxiedInstance, typeMetadata.getDiscriminatorGetter() );
		}

		// now we give the discriminator the opportunity to specify a analyzer per field level
		for ( IndexableField field : doc.getFields() ) {
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

	public boolean allowFieldSelectionInProjection() {
		return allowFieldSelectionInProjection;
	}

	public Set<org.hibernate.search.annotations.FieldCacheType> getFieldCacheOption() {
		return fieldCacheUsage;
	}

	public TwoWayFieldBridge getIdBridge() {
		return (TwoWayFieldBridge) idPropertyMetadata.getFieldMetadata( idFieldName ).getFieldBridge();
	}

	public String getIdKeywordName() {
		return idPropertyMetadata.getFieldMetadata( idFieldName ).getName();
	}

	/**
	 * Return the id used for indexing if possible
	 * An IllegalStateException otherwise
	 * <p/>
	 * If the id is provided, we can't extract it from the entity
	 */
	@Override
	public Serializable getId(Object entity) {
		if ( entity == null || idPropertyMetadata.getPropertyAccessorName() == null || idProvided ) {
			throw new IllegalStateException( "Cannot guess id from entity" );
		}
		Object unproxiedEntity = getInstanceInitializer().unproxy( entity );
		return (Serializable) ReflectionHelper.getMemberValue(
				unproxiedEntity,
				idPropertyMetadata.getPropertyAccessor()
		);
	}

	public String objectToString(String fieldName, Object value, ConversionContext conversionContext) {
		if ( fieldName == null ) {
			throw new AssertionFailure( "Field name should not be null" );
		}

		final DocumentFieldMetadata idFieldMetaData = idPropertyMetadata.getFieldMetadata( idFieldName );
		final FieldBridge bridge = fieldName.equals( idFieldMetaData.getName() ) ?
				getIdBridge() :
				getBridge( getMetadata(), fieldName );

		if ( bridge != null ) {
			return objectToString( fieldName, bridge, value, conversionContext );
		}

		throw new SearchException( "Unable to find field " + fieldName + " in " + getBeanXClass() );
	}

	public String objectToString(String fieldName, FieldBridge bridge, Object value, ConversionContext conversionContext) {
		if ( fieldName == null ) {
			throw new AssertionFailure( "Field name should not be null" );
		}
		if ( bridge == null ) {
			throw new AssertionFailure( "Field bridge should not be null" );
		}

		final Class<? extends FieldBridge> bridgeClass = bridge.getClass();

		if ( TwoWayFieldBridge.class.isAssignableFrom( bridgeClass ) ) {
			return objectToString( (TwoWayFieldBridge) bridge, fieldName, value, conversionContext );
		}
		else if ( StringBridge.class.isAssignableFrom( bridgeClass ) ) {
			return objectToString( (StringBridge) bridge, fieldName, value, conversionContext );
		}
		else {
			throw log.fieldBridgeNotTwoWay( bridgeClass, fieldName, getBeanXClass() );
		}
	}

	private FieldBridge getNullBridge(EmbeddedTypeMetadata embeddedTypeMetadata, String fieldName) {
		if ( fieldName.equals( embeddedTypeMetadata.getEmbeddedNullFieldName() ) ) {
			return embeddedTypeMetadata.getEmbeddedNullFieldBridge();
		}
		else {
			return null;
		}
	}

	public FieldBridge getBridge(String fieldName) {
		return getBridge( getMetadata(), fieldName );
	}

	private FieldBridge getBridge(TypeMetadata typeMetadata, String fieldName) {
		// process base fields
		DocumentFieldMetadata documentFieldMetadata = typeMetadata.getDocumentFieldMetadataFor( fieldName );
		if ( documentFieldMetadata != null && documentFieldMetadata.getFieldBridge() != null ) {
			return documentFieldMetadata.getFieldBridge();
		}

		// process embedded fields
		FieldBridge fieldBridge;

		for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
			fieldBridge = getBridge( embeddedTypeMetadata, fieldName );
			if ( fieldBridge != null ) {
				return fieldBridge;
			}
		}

		// process null embedded fields
		for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
			fieldBridge = getNullBridge( embeddedTypeMetadata, fieldName );
			if ( fieldBridge != null ) {
				return fieldBridge;
			}
		}

		//process class bridges
		DocumentFieldMetadata fieldMetadata = typeMetadata.getFieldMetadataForClassBridgeField( fieldName );
		fieldBridge = fieldMetadata == null ? null : fieldMetadata.getFieldBridge();
		return fieldBridge;
	}

	/**
	 * Checks whether all involved bridges allow to optimize document retrieval by using
	 * {@code FieldSelector} (see HSEARCH-213).
	 */
	private void checkAllowFieldSelection() {
		allowFieldSelectionInProjection = true;
		if ( fieldBridgeProhibitsFieldSelectionInProjection( getIdBridge() ) ) {
			allowFieldSelectionInProjection = false;
			return;
		}
		for ( PropertyMetadata propertyMetadata : getMetadata().getAllPropertyMetadata() ) {
			for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadata() ) {
				FieldBridge bridge = documentFieldMetadata.getFieldBridge();
				if ( fieldBridgeProhibitsFieldSelectionInProjection( bridge ) ) {
					allowFieldSelectionInProjection = false;
					return;
				}
			}
		}
		for ( DocumentFieldMetadata fieldMetadata : getMetadata().getClassBridgeMetadata() ) {
			FieldBridge bridge = fieldMetadata.getFieldBridge();
			if ( fieldBridgeProhibitsFieldSelectionInProjection( bridge ) ) {
				allowFieldSelectionInProjection = false;
				return;
			}
		}
	}

	private boolean fieldBridgeProhibitsFieldSelectionInProjection(FieldBridge bridge) {
		return !( bridge instanceof TwoWayStringBridge
				|| bridge instanceof TwoWayString2FieldBridgeAdaptor
				|| bridge instanceof NumericFieldBridge
				|| bridge instanceof NumericEncodingCalendarBridge
				|| bridge instanceof NumericEncodingDateBridge
				|| bridge instanceof StringEncodingDateBridge );
	}

	/**
	 * To be removed, see org.hibernate.search.engine.DocumentBuilderIndexedEntity.isIdMatchingJpaId()
	 */
	@Override
	public boolean requiresProvidedId() {
		return this.idProvided;
	}

	/**
	 * FIXME remove the need for such a method, we should always be able to rely on Work.id,
	 * but to respect @DocumentId which is being processed in the DocumentBuilder currently
	 * finding out which id we need is tricky, and requires helpers method like this one.
	 */
	@Override
	public boolean isIdMatchingJpaId() {
		return ( !idProvided && getTypeMetadata().isJpaIdUsedAsDocumentId() );
	}
}
