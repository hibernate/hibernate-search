/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.metadata.impl;

import static org.hibernate.search.engine.impl.AnnotationProcessingHelper.getFieldName;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.document.Field;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XPackage;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.FacetEncodingType;
import org.hibernate.search.annotations.Facets;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.NormalizerDefs;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.SortableFields;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Spatials;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.DefaultStringBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.impl.BridgeFactory;
import org.hibernate.search.bridge.spi.EncodingBridge;
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.bridge.util.impl.BridgeAdaptorUtils;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.bridge.util.impl.ToStringNullMarker;
import org.hibernate.search.bridge.util.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.AnnotationProcessingHelper;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.engine.impl.MappingDefinitionRegistry;
import org.hibernate.search.engine.nulls.codec.impl.NotEncodingCodec;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.SpatialFieldBridge;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A metadata provider which extracts the required information from annotations.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings( "deprecation" )
public class AnnotationMetadataProvider implements MetadataProvider {
	private static final int INFINITE_DEPTH = Integer.MAX_VALUE;

	private static final Log log = LoggerFactory.make();
	private static final StringBridge NULL_EMBEDDED_STRING_BRIDGE = DefaultStringBridge.INSTANCE;
	private static final String UNKNOWN_MAPPED_BY_ROLE = "";
	private static final String EMPTY_PREFIX = "";

	private final ReflectionManager reflectionManager;
	private final ConfigContext configContext;
	private final BridgeFactory bridgeFactory;

	private final Class<? extends Annotation> jpaIdClass;
	private final Class<? extends Annotation> jpaEmbeddedIdClass;

	public AnnotationMetadataProvider(ReflectionManager reflectionManager, ConfigContext configContext) {
		this.reflectionManager = reflectionManager;
		this.configContext = configContext;
		this.bridgeFactory = new BridgeFactory( configContext.getServiceManager() );

		if ( configContext.isJpaPresent() ) {
			this.jpaIdClass = loadAnnotationClass( "javax.persistence.Id", configContext );
			this.jpaEmbeddedIdClass = loadAnnotationClass( "javax.persistence.EmbeddedId", configContext );
		}
		else {
			this.jpaIdClass = null;
			this.jpaEmbeddedIdClass = null;
		}
	}

	private Class<? extends Annotation> loadAnnotationClass(String className, ConfigContext configContext) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> idClass = ClassLoaderHelper.classForName( className, configContext.getServiceManager() );
			return idClass;
		}
		catch (ClassLoadingException e) {
			throw new SearchException( "Unable to load class " + className + " even though it should be present?!" );
		}
	}

	@Override
	public TypeMetadata getTypeMetadataForContainedIn(IndexedTypeIdentifier type) {
		final Class<?> clazz = type.getPojoType();
		XClass xClass = reflectionManager.toXClass( clazz );

		ParseContext parseContext = new ParseContext();
		parseContext.processingClass( xClass );
		parseContext.setCurrentClass( xClass );

		return doGetTypeMetadataFor( clazz, xClass, parseContext );
	}

	@Override
	public TypeMetadata getTypeMetadataFor(IndexedTypeIdentifier type, IndexManagerType indexManagerType) {
		final Class<?> clazz = type.getPojoType();
		return getTypeMetadataFor( clazz, indexManagerType );
	}

	/**
	 * Use {@link #getTypeMetadataFor(IndexedTypeIdentifier, IndexManagerType)} instead.
	 * @deprecated
	 */
	@Deprecated
	public TypeMetadata getTypeMetadataFor(Class<?> clazz, IndexManagerType indexManagerType) {
		XClass xClass = reflectionManager.toXClass( clazz );

		ParseContext parseContext = new ParseContext();
		parseContext.setIndexManagerType( indexManagerType );
		parseContext.processingClass( xClass );
		parseContext.setCurrentClass( xClass );

		return doGetTypeMetadataFor( clazz, xClass, parseContext );
	}

	private TypeMetadata doGetTypeMetadataFor(Class<?> clazz, XClass xClass, ParseContext parseContext) {
		IndexedTypeIdentifier classTypeId = new PojoIndexedTypeIdentifier( clazz );
		TypeMetadata.Builder typeMetadataBuilder = new TypeMetadata.Builder( classTypeId, configContext, parseContext )
				.boost( getBoost( xClass ) )
				.boostStrategy( AnnotationProcessingHelper.getDynamicBoost( xClass ) );

		initializePackageLevelAnnotations( packageInfo( clazz ), configContext );

		initializeClass(
				typeMetadataBuilder,
				true,
				EMPTY_PREFIX,
				parseContext,
				configContext,
				false,
				null
		);

		return typeMetadataBuilder.build();
	}

	private XPackage packageInfo(final Class<?> clazz) {
		if ( clazz == null ) {
			return null;
		}
		final Package packageClazz = clazz.getPackage();
		if ( packageClazz == null ) {
			return null;
		}
		final String packageName = packageClazz.getName();
		try {
			return reflectionManager.packageForName( packageName );
		}
		catch (ClassNotFoundException | ClassLoadingException e) {
			// ClassNotFoundException: should not happen at this point
			// ClassLoadingExceptionn: Package does not contain a package-info.java
			log.debugf( "package-info not found for package '%s'", packageName, clazz );
			return null;
		}
	}

	@Override
	public boolean containsSearchMetadata(IndexedTypeIdentifier type) {
		final Class<?> clazz = type.getPojoType();
		XClass xClass = reflectionManager.toXClass( clazz );
		return ReflectionHelper.containsSearchAnnotations( xClass );
	}

	private void checkDocumentId(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			NumericFieldsConfiguration numericFields,
			boolean isRoot,
			String prefix,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext,
			boolean hasExplicitDocumentId) {
		Annotation idAnnotation = getIdAnnotation( member, typeMetadataBuilder, configContext );
		if ( idAnnotation == null ) {
			return;
		}

		// Ignore JPA @Id/@DocumentId if @DocumentId is present at another property
		if ( hasExplicitDocumentId && idAnnotation.annotationType() != DocumentId.class ) {
			return;
		}

		// Make sure we'll be able to access values
		ReflectionHelper.setAccessible( member );

		final String relativeFieldName = getIdAttributeName( member, idAnnotation );
		final DocumentFieldPath fieldPath = new DocumentFieldPath( prefix, relativeFieldName );
		if ( isRoot ) {
			createIdPropertyMetadata(
					member,
					typeMetadataBuilder,
					numericFields,
					configContext,
					parseContext,
					idAnnotation,
					fieldPath
			);
		}
		else {
			if ( parseContext.includeEmbeddedObjectId() || pathsContext != null && pathsContext.isIncluded( fieldPath ) ) {
				createPropertyMetadataForEmbeddedId( member, typeMetadataBuilder, propertyMetadataBuilder,
						numericFields, configContext, parseContext, fieldPath );
			}
		}

		if ( pathsContext != null ) {
			pathsContext.markEncounteredPath( fieldPath );
		}
	}

	private void createPropertyMetadataForEmbeddedId(XProperty member, TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder, NumericFieldsConfiguration numericFields,
			ConfigContext configContext, ParseContext parseContext, DocumentFieldPath fieldPath) {
		Field.Index index = AnnotationProcessingHelper.getIndex( Index.YES, Analyze.NO, Norms.YES );
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( TermVector.NO );

		FieldBridge fieldBridge;
		if ( parseContext.skipFieldBridges() ) {
			fieldBridge = null;
		}
		else {
			fieldBridge = bridgeFactory.buildFieldBridge(
					member,
					true,
					numericFields.isNumericField( fieldPath ),
					parseContext.getIndexManagerType(),
					reflectionManager,
					configContext.getServiceManager()
			);
		}

		DocumentFieldMetadata.Builder fieldMetadataBuilder =
				new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						propertyMetadataBuilder.getResultReference(), propertyMetadataBuilder,
						fieldPath, Store.YES, index, termVector
						)
						.boost( AnnotationProcessingHelper.getBoost( member, null ) )
						.fieldBridge( fieldBridge )
						.idInEmbedded();

		NumericEncodingType numericEncodingType = determineNumericFieldEncoding( fieldBridge );
		if ( numericEncodingType != NumericEncodingType.UNKNOWN ) {
			fieldMetadataBuilder.numeric();
			fieldMetadataBuilder.numericEncodingType( numericEncodingType );
		}

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();

		propertyMetadataBuilder.addDocumentField( fieldMetadata );

		// property > entity analyzer (no field analyzer)
		if ( !parseContext.skipAnalyzers() ) {
			AnalyzerReference analyzerReference = AnnotationProcessingHelper.getAnalyzerReference(
					member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
					configContext,
					parseContext.getIndexManagerType()
			);
			if ( analyzerReference == null ) {
				analyzerReference = typeMetadataBuilder.getAnalyzerReference();
			}
			if ( analyzerReference == null ) {
				throw new AssertionFailure( "Analyzer should not be undefined" );
			}
			typeMetadataBuilder.addToScopedAnalyzerReference( fieldPath, analyzerReference, index );
		}
	}

	private void createIdPropertyMetadata(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			NumericFieldsConfiguration numericFields,
			ConfigContext configContext,
			ParseContext parseContext,
			Annotation idAnnotation,
			DocumentFieldPath fieldPath) {
		if ( parseContext.isExplicitDocumentId() ) {
			if ( idAnnotation instanceof DocumentId ) {
				throw log.duplicateDocumentIdFound( typeMetadataBuilder.getIndexedType().getName() );
			}
			else {
				// If it's not a DocumentId it's a JPA @Id/@EmbeddedId: ignore it as we already have a @DocumentId
				return;
			}
		}
		if ( idAnnotation instanceof DocumentId ) {
			parseContext.setExplicitDocumentId( true );
		}

		NumericField numericFieldAnnotation = numericFields.getNumericFieldAnnotation( fieldPath );

		// Don't apply @NumericField if it is given with the default name and there is another custom @Field
		if ( numericFieldAnnotation != null && numericFieldAnnotation.forField().isEmpty()
				&& ( member.isAnnotationPresent( org.hibernate.search.annotations.Field.class ) || member.isAnnotationPresent( Fields.class ) ) ) {
			numericFieldAnnotation = null;
		}

		FieldBridge idBridge;
		if ( parseContext.skipFieldBridges() ) {
			idBridge = null;
		}
		else {
			idBridge = bridgeFactory.buildFieldBridge(
					member,
					true,
					numericFieldAnnotation != null,
					parseContext.getIndexManagerType(),
					reflectionManager,
					configContext.getServiceManager()
			);
			if ( !( idBridge instanceof TwoWayFieldBridge ) ) {
				throw new SearchException(
						"Bridge for document id does not implement TwoWayFieldBridge: " + member.getName()
				);
			}
		}

		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( TermVector.NO );

		PropertyMetadata.Builder propertyMetadataBuilder = new PropertyMetadata.Builder(
				typeMetadataBuilder.getResultReference(), member,
				reflectionManager.toClass( member.getType() ) );

		DocumentFieldMetadata.Builder idMetadataBuilder = new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						propertyMetadataBuilder.getResultReference(), propertyMetadataBuilder,
						fieldPath,
						Store.YES,
						Field.Index.NOT_ANALYZED_NO_NORMS,
						termVector
				)
				.id()
				.boost( AnnotationProcessingHelper.getBoost( member, null ) )
				.fieldBridge( idBridge );

		parseContext.setIdFieldPath( fieldPath );

		NumericEncodingType numericEncodingType = determineNumericFieldEncoding( idBridge );
		if ( numericEncodingType != NumericEncodingType.UNKNOWN ) {
			idMetadataBuilder.numeric();
			idMetadataBuilder.numericEncodingType( numericEncodingType );
		}
		checkForSortableField( member, typeMetadataBuilder, propertyMetadataBuilder, "", true, null, parseContext );
		checkForSortableFields( member, typeMetadataBuilder, propertyMetadataBuilder, "", true, null, parseContext );

		if ( idBridge instanceof MetadataProvidingFieldBridge ) {
			FieldMetadataBuilderImpl bridgeDefinedMetadata = getBridgeContributedFieldMetadata(
					idMetadataBuilder, (MetadataProvidingFieldBridge) idBridge );

			for ( BridgeDefinedField bridgeDefinedField : bridgeDefinedMetadata.getBridgeDefinedFields() ) {
				idMetadataBuilder.addBridgeDefinedField( bridgeDefinedField );
			}
		}

		DocumentFieldMetadata fieldMetadata = idMetadataBuilder.build();
		propertyMetadataBuilder.addDocumentField( fieldMetadata );

		PropertyMetadata idPropertyMetadata = propertyMetadataBuilder
				.build();

		typeMetadataBuilder.idProperty( idPropertyMetadata );
	}

	/**
	 * Checks whether the specified property contains an annotation used as document id. This can either be an explicit
	 * {@code @DocumentId} or if no {@code @DocumentId} is specified a JPA {@code @Id} / {@code @EmbeddedId} annotation.
	 * The check for the JPA annotations is indirectly to avoid a hard dependency to Hibernate Annotations.
	 *
	 * @param member the property to check for the id annotation.
	 * @param context Handle to default configuration settings.
	 * @return the annotation used as document id or {@code null} if no id annotation is specified on the property.
	 */
	private Annotation getIdAnnotation(XProperty member, TypeMetadata.Builder typeMetadataBuilder, ConfigContext context) {
		Annotation idAnnotation = null;

		// check for explicit DocumentId
		DocumentId documentIdAnnotation = member.getAnnotation( DocumentId.class );
		if ( documentIdAnnotation != null ) {
			idAnnotation = documentIdAnnotation;
		}
		// check for JPA @Id/@EmbeddedId
		if ( context.isJpaPresent() ) {
			Annotation jpaId = member.getAnnotation( jpaIdClass );

			if ( jpaId == null ) {
				jpaId = member.getAnnotation( jpaEmbeddedIdClass );
			}

			if ( jpaId != null ) {
				typeMetadataBuilder.jpaProperty( member );

				if ( idAnnotation == null ) {
					log.debug( "Found JPA id and using it as document id" );
					idAnnotation = jpaId;
				}
			}
		}

		return idAnnotation;
	}

	private void initializeProvidedIdMetadata(String prefix, ProvidedId providedId, XClass clazz, TypeMetadata.Builder typeMetadataBuilder,
			boolean isRoot, PathsContext pathsContext, ParseContext parseContext) {
		FieldBridge providedIdFieldBridge = null;

		String relativeFieldName;
		if ( providedId != null ) {
			relativeFieldName = providedId.name();
		}
		else {
			relativeFieldName = ProvidedId.defaultFieldName;
		}

		DocumentFieldPath fieldPath = new DocumentFieldPath( prefix, relativeFieldName );

		if ( !parseContext.includeEmbeddedObjectId() && pathsContext != null && !pathsContext.isIncluded( fieldPath ) ) {
			return;
		}

		if ( isRoot || !parseContext.skipFieldBridges() ) {
			if ( providedId != null ) {
				providedIdFieldBridge = bridgeFactory.extractTwoWayType( providedId.bridge(), clazz, reflectionManager );
			}
			else {
				providedIdFieldBridge = new TwoWayString2FieldBridgeAdaptor( org.hibernate.search.bridge.builtin.StringBridge.INSTANCE );
			}
		}

		PropertyMetadata.Builder propertyMetadataBuilder = new PropertyMetadata.Builder( typeMetadataBuilder.getResultReference(), null, null );

		DocumentFieldMetadata.Builder fieldMetadataBuilder =
				new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						propertyMetadataBuilder.getResultReference(), propertyMetadataBuilder,
						fieldPath,
						Store.YES,
						Field.Index.NOT_ANALYZED_NO_NORMS,
						Field.TermVector.NO
				)
						.fieldBridge( providedIdFieldBridge )
						.boost( 1.0f );

		if ( !isRoot ) {
			fieldMetadataBuilder.idInEmbedded();
		}

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();

		propertyMetadataBuilder.addDocumentField( fieldMetadata );
		PropertyMetadata propertyMetadata = propertyMetadataBuilder.build();

		if ( isRoot ) {
			typeMetadataBuilder.idProperty( propertyMetadata );
		}
		else {
			typeMetadataBuilder.addProperty( propertyMetadata );
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

		if ( idAnnotation.annotationType() == DocumentId.class ) {
			name = ( (DocumentId) idAnnotation ).name();
		}

		return ReflectionHelper.getAttributeName( member, name );
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

	private void initializeClass(TypeMetadata.Builder typeMetadataBuilder,
			boolean isRoot,
			String prefix,
			ParseContext parseContext,
			ConfigContext configContext,
			boolean disableOptimizationsArg,
			PathsContext pathsContext) {
		List<XClass> hierarchy = ReflectionHelper.createXClassHierarchy( parseContext.getCurrentClass() );

		// Iterate the class hierarchy top down. This allows to override the default analyzer for the properties if the class holds one
		ProvidedId explicitProvidedIdAnnotation = null;
		XClass providedIdHostingClass = null;
		for ( XClass currentClass : hierarchy ) {
			ProvidedId providedId = currentClass.getAnnotation( ProvidedId.class );
			if ( providedId != null ) {
				explicitProvidedIdAnnotation = providedId;
				providedIdHostingClass = currentClass;
			}

			parseContext.setCurrentClass( currentClass );
			initializeClassLevelAnnotations( typeMetadataBuilder, prefix, configContext, parseContext );
			initializeClassBridgeInstances( typeMetadataBuilder, prefix, configContext, currentClass, parseContext );
		}

		boolean isProvidedId = false;
		if ( explicitProvidedIdAnnotation != null || configContext.isProvidedIdImplicit() ) {
			initializeProvidedIdMetadata( prefix, explicitProvidedIdAnnotation, providedIdHostingClass, typeMetadataBuilder,
					isRoot, pathsContext, parseContext );
			isProvidedId = true;
		}

		// if optimizations are enabled, we allow for state in indexed embedded objects which are not
		// explicitly indexed to skip index update triggering.
		// we don't allow this if the reference is reachable via a custom Field- or ClassBridge,
		// as state changes from values out of our control could affect the index.
		boolean disableOptimizations = disableOptimizationsArg
				|| !stateInspectionOptimizationsEnabled( typeMetadataBuilder );

		// iterate again for the properties and fields
		for ( XClass currentClass : hierarchy ) {
			parseContext.setCurrentClass( currentClass );
			boolean hasExplicitDocumentId = hasExplicitDocumentId( currentClass );

			// rejecting non properties (ie regular methods) because the object is loaded from Hibernate,
			// so indexing a non property does not make sense
			List<XProperty> methods = currentClass.getDeclaredProperties( XClass.ACCESS_PROPERTY );
			for ( XProperty method : methods ) {
				initializeMemberLevelAnnotations(
						prefix,
						method,
						typeMetadataBuilder,
						disableOptimizations,
						isRoot,
						isProvidedId,
						configContext,
						pathsContext,
						parseContext,
						hasExplicitDocumentId
				);
			}

			List<XProperty> fields = currentClass.getDeclaredProperties( XClass.ACCESS_FIELD );
			for ( XProperty field : fields ) {
				initializeMemberLevelAnnotations(
						prefix,
						field,
						typeMetadataBuilder,
						disableOptimizations,
						isRoot,
						isProvidedId,
						configContext,
						pathsContext,
						parseContext,
						hasExplicitDocumentId
				);
			}

			// Indexed collection roles are collected in the parse context while traversing the hierarchy.
			// We add all the collection roles of the parent classes and the current class to the current class
			// so that we can work around the fact that for @MappedSuperClass the collection role is considered
			// attached to the first @Entity sub class by ORM.
			// While it is not quite right, it's the best solution we have found to fix HSEARCH-1583 as there is no
			// reliable way to unqualify a collection role at the moment.
			// This might change in ORM 5 and we might reconsider this decision then.
			for ( String unqualifiedCollectionRole : parseContext.getCollectedUnqualifiedCollectionRoles() ) {
				typeMetadataBuilder.addCollectionRole(
						StringHelper.qualify(
								parseContext.getCurrentClass().getName(), unqualifiedCollectionRole
						)
				);
			}
		}
	}

	private void initializePackageLevelAnnotations(XPackage xPackage, ConfigContext configContext) {
		if ( xPackage != null ) {
			checkForAnalyzerDefs( xPackage, configContext );
			checkForNormalizerDefs( xPackage, configContext );
			checkForFullTextFilterDefs( xPackage, configContext );
		}
	}

	/**
	 * Check and initialize class level annotations.
	 *
	 * @param typeMetadataBuilder The meta data holder.
	 * @param prefix The current prefix used for the <code>Document</code> field names.
	 * @param configContext Handle to default configuration settings.
	 */
	private void initializeClassLevelAnnotations(TypeMetadata.Builder typeMetadataBuilder,
			String prefix,
			ConfigContext configContext,
			ParseContext parseContext) {

		XClass clazz = parseContext.getCurrentClass();
		// check for a class level specified analyzer
		if ( !parseContext.skipAnalyzers() ) {
			AnalyzerReference analyzerReference = AnnotationProcessingHelper.getAnalyzerReference(
					clazz.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
					configContext,
					parseContext.getIndexManagerType()
			);
			if ( analyzerReference != null ) {
				typeMetadataBuilder.analyzerReference( analyzerReference );
			}
		}

		checkForAnalyzerDefs( clazz, configContext );
		checkForNormalizerDefs( clazz, configContext );
		checkForFullTextFilterDefs( clazz, configContext );

		// Check for any ClassBridges annotation.
		ClassBridges classBridgesAnnotation = clazz.getAnnotation( ClassBridges.class );
		if ( classBridgesAnnotation != null ) {
			ClassBridge[] classBridges = classBridgesAnnotation.value();
			for ( ClassBridge cb : classBridges ) {
				bindClassBridgeAnnotation( prefix, typeMetadataBuilder, cb, clazz, configContext, parseContext );
			}
		}

		// Check for any ClassBridge style of annotations.
		ClassBridge classBridgeAnnotation = clazz.getAnnotation( ClassBridge.class );
		if ( classBridgeAnnotation != null ) {
			bindClassBridgeAnnotation( prefix, typeMetadataBuilder, classBridgeAnnotation, clazz, configContext,
					parseContext );
		}

		//Check for Spatial annotation on class level
		Spatial spatialAnnotation = clazz.getAnnotation( Spatial.class );
		if ( spatialAnnotation != null ) {
			bindSpatialAnnotation( spatialAnnotation, prefix, typeMetadataBuilder, parseContext );
		}
		Spatials spatialsAnnotation = clazz.getAnnotation( Spatials.class );
		if ( spatialsAnnotation != null ) {
			Spatial[] spatials = spatialsAnnotation.value();
			for ( Spatial innerSpatialAnnotation : spatials ) {
				bindSpatialAnnotation( innerSpatialAnnotation, prefix, typeMetadataBuilder, parseContext );
			}
		}

		checkForAnalyzerDiscriminator( clazz, typeMetadataBuilder, configContext );
	}

	/**
	 * Initializes metadata contributed by class bridge instances set up through the programmatic config API.
	 */
	private void initializeClassBridgeInstances(TypeMetadata.Builder typeMetadataBuilder,
			String prefix,
			ConfigContext configContext,
			XClass clazz,
			ParseContext parseContext) {

		Map<FieldBridge, ClassBridge> classBridgeInstances = configContext.getClassBridgeInstances(
				reflectionManager.toClass(
						clazz
				)
		);

		for ( Entry<FieldBridge, ClassBridge> classBridge : classBridgeInstances.entrySet() ) {
			FieldBridge instance = classBridge.getKey();
			ClassBridge configuration = classBridge.getValue();

			bindClassBridgeAnnotation( prefix, typeMetadataBuilder, configuration, instance, configContext, parseContext );
		}
	}

	private void bindClassBridgeAnnotation(String prefix,
			TypeMetadata.Builder typeMetadataBuilder,
			ClassBridge classBridgeAnnotation,
			XClass clazz,
			ConfigContext configContext,
			ParseContext parseContext) {
		FieldBridge fieldBridge = bridgeFactory.extractType( classBridgeAnnotation, reflectionManager.toClass( clazz ) );
		bindClassBridgeAnnotation( prefix, typeMetadataBuilder, classBridgeAnnotation, fieldBridge, configContext, parseContext );
	}

	private void bindClassBridgeAnnotation(String prefix,
			TypeMetadata.Builder typeMetadataBuilder,
			ClassBridge classBridgeAnnotation,
			FieldBridge fieldBridge,
			ConfigContext configContext,
			ParseContext parseContext) {
		bridgeFactory.injectParameters( classBridgeAnnotation, fieldBridge );

		String relativeFieldName = classBridgeAnnotation.name();
		DocumentFieldPath fieldPath = new DocumentFieldPath( prefix, relativeFieldName );
		Store store = classBridgeAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex(
				classBridgeAnnotation.index(),
				classBridgeAnnotation.analyze(),
				classBridgeAnnotation.norms()
		);
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( classBridgeAnnotation.termVector() );

		DocumentFieldMetadata.Builder fieldMetadataBuilder =
				new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						// Class bridge, there's no related property
						BackReference.<PropertyMetadata>empty(),
						null,
						fieldPath, store, index, termVector
				)
				.boost( classBridgeAnnotation.boost().value() )
				.fieldBridge( fieldBridge );


		contributeClassBridgeDefinedFields( typeMetadataBuilder, fieldMetadataBuilder, fieldBridge );

		if ( !parseContext.skipAnalyzers() ) {
			AnalyzerReference analyzerReference = AnnotationProcessingHelper.getAnalyzerReference(
					typeMetadataBuilder.getIndexedType(), fieldPath,
					classBridgeAnnotation.analyzer(),
					classBridgeAnnotation.normalizer(),
					configContext,
					parseContext.getIndexManagerType() );
			typeMetadataBuilder.addToScopedAnalyzerReference( fieldPath, analyzerReference, index );
			fieldMetadataBuilder.analyzerReference( analyzerReference );
		}

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();
		typeMetadataBuilder.addClassBridgeField( fieldMetadata );
	}

	private void bindSpatialAnnotation(Spatial spatialAnnotation,
			String prefix,
			XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			ParseContext parseContext) {
		String relativeFieldName = ReflectionHelper.getAttributeName( member, spatialAnnotation.name() );
		DocumentFieldPath fieldPath = new DocumentFieldPath( prefix, relativeFieldName );

		if ( parseContext.isSpatialNameUsed( fieldPath ) ) {
			throw log.cannotHaveTwoSpatialsWithDefaultOrSameName( member.getType().getName() );
		}
		parseContext.markSpatialNameAsUsed( fieldPath );

		Store store = spatialAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex( Index.YES, Analyze.NO, Norms.NO );
		Field.TermVector termVector = Field.TermVector.NO;
		FieldBridge fieldBridge = bridgeFactory.buildFieldBridge(
				member,
				false,
				false,
				parseContext.getIndexManagerType(),
				reflectionManager,
				configContext.getServiceManager()
		);

		DocumentFieldMetadata.Builder fieldMetadataBuilder =
				new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						propertyMetadataBuilder.getResultReference(), propertyMetadataBuilder,
						fieldPath, store, index, termVector
				)
				.boost( AnnotationProcessingHelper.getBoost( member, spatialAnnotation ) )
				.fieldBridge( fieldBridge )
				.spatial();

		if ( fieldBridge instanceof MetadataProvidingFieldBridge ) {
			MetadataProvidingFieldBridge metadataProvidingFieldBridge = (MetadataProvidingFieldBridge) fieldBridge;
			FieldMetadataBuilderImpl bridgeContributedMetadata = getBridgeContributedFieldMetadata(
					fieldMetadataBuilder, metadataProvidingFieldBridge );
			for ( BridgeDefinedField field : bridgeContributedMetadata.getBridgeDefinedFields() ) {
				fieldMetadataBuilder.addBridgeDefinedField( field );
			}
		}

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();
		propertyMetadataBuilder.addDocumentField( fieldMetadata );

		if ( member.isCollection() ) {
			parseContext.collectUnqualifiedCollectionRole( member.getName() );
		}
	}

	private void bindSpatialAnnotation(Spatial spatialAnnotation,
			String prefix,
			TypeMetadata.Builder typeMetadataBuilder,
			ParseContext parseContext) {
		String relativeFieldName = spatialAnnotation.name();
		if ( relativeFieldName.isEmpty() ) {
			relativeFieldName = Spatial.COORDINATES_DEFAULT_FIELD;
		}

		DocumentFieldPath fieldPath = new DocumentFieldPath( prefix, relativeFieldName );

		if ( parseContext.isSpatialNameUsed( fieldPath ) ) {
			throw log.cannotHaveTwoSpatialsWithDefaultOrSameName( parseContext.getCurrentClass().getName() );
		}
		parseContext.markSpatialNameAsUsed( fieldPath );

		Store store = spatialAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex( Index.YES, Analyze.NO, Norms.NO );
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( TermVector.NO );
		FieldBridge spatialBridge = determineSpatialFieldBridge( spatialAnnotation, parseContext );

		DocumentFieldMetadata.Builder fieldMetadataBuilder =
				new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						BackReference.<PropertyMetadata>empty(), null, // Class-level spatial annotation, there's no related property
						fieldPath, store, index, termVector
				)
				.boost( spatialAnnotation.boost().value() )
				.fieldBridge( spatialBridge )
				.spatial();

		contributeClassBridgeDefinedFields( typeMetadataBuilder, fieldMetadataBuilder, spatialBridge );

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();
		typeMetadataBuilder.addClassBridgeField( fieldMetadata );

		if ( ! parseContext.skipAnalyzers() ) {
			AnalyzerReference analyzerReference = typeMetadataBuilder.getAnalyzerReference();
			if ( analyzerReference == null ) {
				throw new AssertionFailure( "Analyzer should not be undefined" );
			}
		}
	}

	private void contributeClassBridgeDefinedFields(TypeMetadata.Builder typeMetadataBuilder,
			DocumentFieldMetadata.Builder fieldMetadataBuilder, FieldBridge fieldBridge) {
		if ( fieldBridge instanceof MetadataProvidingFieldBridge ) {
			MetadataProvidingFieldBridge metadataProvidingFieldBridge = (MetadataProvidingFieldBridge) fieldBridge;

			FieldMetadataBuilderImpl classBridgeContributedFieldMetadata =
					getBridgeContributedFieldMetadata( fieldMetadataBuilder, metadataProvidingFieldBridge );

			typeMetadataBuilder.addClassBridgeSortableFields( classBridgeContributedFieldMetadata.getSortableFieldsAbsoluteNames() );
			for ( BridgeDefinedField bridgeDefinedField : classBridgeContributedFieldMetadata.getBridgeDefinedFields() ) {
				fieldMetadataBuilder.addBridgeDefinedField( bridgeDefinedField );
			}
		}
	}

	private FieldBridge determineSpatialFieldBridge(Spatial spatialAnnotation, ParseContext parseContext) {
		final FieldBridge spatialBridge;
		XClass xClazz = parseContext.getCurrentClass();
		Class<?> clazz = reflectionManager.toClass( xClazz );
		if ( reflectionManager.toXClass( Coordinates.class ).isAssignableFrom( xClazz ) ) {
			spatialBridge = bridgeFactory.buildSpatialBridge( spatialAnnotation, clazz, null, null );
		}
		else {
			String latitudeField = null;
			String longitudeField = null;

			List<XProperty> fieldList = xClazz.getDeclaredProperties( XClass.ACCESS_FIELD );

			for ( XProperty property : fieldList ) {
				if ( property.isAnnotationPresent( Latitude.class ) && ( property.getAnnotation( Latitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( latitudeField != null ) {
						throw log.ambiguousLatitudeDefinition( xClazz.getName(), latitudeField, property.getName() );
					}
					latitudeField = property.getName();
				}
				if ( property.isAnnotationPresent( Longitude.class ) && ( property.getAnnotation( Longitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( longitudeField != null ) {
						throw log.ambiguousLongitudeDefinition(
								xClazz.getName(),
								longitudeField,
								property.getName()
						);
					}
					longitudeField = property.getName();
				}
			}

			List<XProperty> propertyList = xClazz.getDeclaredProperties( XClass.ACCESS_PROPERTY );

			for ( XProperty property : propertyList ) {
				if ( property.isAnnotationPresent( Latitude.class ) && ( property.getAnnotation( Latitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( latitudeField != null ) {
						throw log.ambiguousLatitudeDefinition( xClazz.getName(), latitudeField, property.getName() );
					}
					latitudeField = property.getName();
				}
				if ( property.isAnnotationPresent( Longitude.class ) && ( property.getAnnotation( Longitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( longitudeField != null ) {
						throw log.ambiguousLongitudeDefinition(
								xClazz.getName(),
								longitudeField,
								property.getName()
						);
					}
					longitudeField = property.getName();
				}
			}

			if ( latitudeField != null && longitudeField != null ) {
				spatialBridge = bridgeFactory.buildSpatialBridge(
						spatialAnnotation,
						clazz,
						latitudeField,
						longitudeField
				);
			}
			else {
				spatialBridge = null;
			}
		}
		if ( spatialBridge == null ) {
			throw log.cannotFindCoordinatesNorLatLongForSpatial(
					spatialAnnotation.name()
							.isEmpty() ? "default" : spatialAnnotation.name(), xClazz.getName()
			);
		}
		return spatialBridge;
	}

	private void bindSortableFieldAnnotation(SortableField sortableFieldAnnotation,
			String prefix,
			XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			boolean isIdProperty,
			ParseContext parseContext) {

		String sortedFieldRelativeName = ReflectionHelper.getAttributeName( member, sortableFieldAnnotation.forField() );
		String sortedFieldAbsoluteName = prefix + sortedFieldRelativeName;
		DocumentFieldPath idFieldPath = parseContext.getIdFieldPath();
		String idFieldAbsoluteName = idFieldPath == null ? null : idFieldPath.getAbsoluteName();

		// Make sure a sort on the id field is only added to the idPropertyMetadata
		if ( isIdProperty && !sortedFieldAbsoluteName.equals( idFieldAbsoluteName )
				|| !isIdProperty && sortedFieldAbsoluteName.equals( idFieldAbsoluteName ) ) {
			return;
		}

		DocumentFieldMetadata targetField = getField( propertyMetadataBuilder, sortedFieldAbsoluteName );
		if ( !sortedFieldAbsoluteName.equals( idFieldAbsoluteName ) && targetField == null ) {
			if ( parseContext.getLevel() != 0 ) {
				// Sortable defined on a property not indexed when the entity is embedded. We can skip it.
				return;
			}
			throw log.sortableFieldRefersToUndefinedField( typeMetadataBuilder.getIndexedType(), propertyMetadataBuilder.getPropertyAccessor().getName(), sortedFieldRelativeName );
		}
		if ( targetField != null ) {
			AnalyzerReference analyzerReference = targetField.getAnalyzerReference();
			if ( targetField.getIndex().isAnalyzed() && analyzerReference != null
					&& !analyzerReference.isNormalizer( sortedFieldAbsoluteName ) ) {
				log.sortableFieldWithNonNormalizerAnalyzer( typeMetadataBuilder.getIndexedType(), sortedFieldRelativeName );
			}
		}

		SortableFieldMetadata fieldMetadata = new SortableFieldMetadata.Builder( sortedFieldAbsoluteName ).build();

		propertyMetadataBuilder.addSortableField( fieldMetadata );
	}

	private DocumentFieldMetadata getField(PropertyMetadata.Builder propertyMetadataBuilder, String fieldName) {
		for ( DocumentFieldMetadata field : propertyMetadataBuilder.getFieldMetadata() ) {
			if ( field.getAbsoluteName().equals( fieldName ) ) {
				return field;
			}
		}

		return null;
	}

	private void initializeMemberLevelAnnotations(String prefix,
			XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			boolean disableOptimizations,
			boolean isRoot,
			boolean isProvidedId,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext,
			boolean hasExplicitDocumentId) {

		PropertyMetadata.Builder propertyMetadataBuilder =
				new PropertyMetadata.Builder(
						typeMetadataBuilder.getResultReference(), member,
						reflectionManager.toClass( member.getType() )
				)
				.dynamicBoostStrategy( AnnotationProcessingHelper.getDynamicBoost( member ) );

		NumericFieldsConfiguration numericFields = buildNumericFieldsConfiguration( typeMetadataBuilder.getIndexedType(), member, prefix, pathsContext, parseContext );

		if ( !isProvidedId ) {
			checkDocumentId( member, typeMetadataBuilder, propertyMetadataBuilder, numericFields, isRoot, prefix, configContext, pathsContext, parseContext, hasExplicitDocumentId );
		}

		checkForField( member, typeMetadataBuilder, propertyMetadataBuilder, numericFields, prefix, configContext, pathsContext, parseContext );
		checkForFields( member, typeMetadataBuilder, propertyMetadataBuilder, numericFields, prefix, configContext, pathsContext, parseContext );
		checkForSpatial( member, typeMetadataBuilder, propertyMetadataBuilder, prefix, pathsContext, parseContext );
		checkForSpatialsAnnotation( member, typeMetadataBuilder, propertyMetadataBuilder, prefix, pathsContext, parseContext );
		checkForSortableField( member, typeMetadataBuilder, propertyMetadataBuilder, prefix, false, pathsContext, parseContext );
		checkForSortableFields( member, typeMetadataBuilder, propertyMetadataBuilder, prefix, false, pathsContext, parseContext );
		checkForAnalyzerDefs( member, configContext );
		checkForNormalizerDefs( member, configContext );
		checkForAnalyzerDiscriminator( member, typeMetadataBuilder, configContext );
		checkForIndexedEmbedded(
				member,
				propertyMetadataBuilder,
				prefix,
				disableOptimizations,
				typeMetadataBuilder,
				configContext,
				pathsContext,
				parseContext
		);
		checkForContainedIn( member, typeMetadataBuilder, parseContext );

		/*
		 * When we skip field bridges, the numeric fields configuration may not
		 * be aware of all the processed fields, because in this case we actually
		 * don't care about which field is numeric.
		 * Thus the validation cannot be performed reliably when we skip field bridges.
		 * It's not important anyway, because field bridges are only skipped when
		 * building contained-in metadata, and when we build contained-in metadata
		 * we always have another pass on the same annotations to build the indexed
		 * entity metadata.
		 */
		if ( !parseContext.skipFieldBridges() ) {
			numericFields.validate();
		}

		PropertyMetadata property = propertyMetadataBuilder.build();
		if ( !property.getFieldMetadataSet().isEmpty() ) {
			/*
			 * Make sure we'll be able to access values.
			 * This should only be called when we're absolutely sure we'll have to access values,
			 * because this call may fail (in JDK 9 in particular),
			 * which would make the whole bootstrapping process fail.
			 * See HSEARCH-2697 (fixed).
			 */
			ReflectionHelper.setAccessible( member );

			typeMetadataBuilder.addProperty( property );
		}
	}

	private void checkForContainedIn(XProperty member, TypeMetadata.Builder typeMetadataBuilder, ParseContext parseContext) {
		if ( !member.isAnnotationPresent( ContainedIn.class ) ) {
			return;
		}

		ContainedInMetadata containedInMetadata = createContainedInMetadata( member );
		typeMetadataBuilder.addContainedIn( containedInMetadata );

		/*
		 * Do NOT add the collection role to the parse context here:
		 * @ContainedIn annotations are information about what depends on this entity's index,
		 * and in parse context we collect exactly the opposite (what this entity's index depends on).
		 */
	}

	private ContainedInMetadata createContainedInMetadata(XProperty member) {
		ContainedInMetadataBuilder containedInMetadataBuilder = new ContainedInMetadataBuilder( member );
		updateContainedInMetadata( containedInMetadataBuilder, member, XClass.ACCESS_FIELD );
		updateContainedInMetadata( containedInMetadataBuilder, member, XClass.ACCESS_PROPERTY );
		return containedInMetadataBuilder.createContainedInMetadata();
	}

	private void updateContainedInMetadata(ContainedInMetadataBuilder containedInMetadataBuilder, XProperty propertyWithContainedIn, String accessType) {
		XClass memberReturnedType = returnedType( propertyWithContainedIn );
		String mappedBy = mappedBy( propertyWithContainedIn );
		List<XProperty> returnedTypeProperties = memberReturnedType.getDeclaredProperties( accessType );
		for ( XProperty property : returnedTypeProperties ) {
			if ( isCorrespondingIndexedEmbedded( propertyWithContainedIn, mappedBy, property ) ) {
				updateContainedInMetadataForProperty( containedInMetadataBuilder, property );
				break;
			}
		}
	}

	private boolean isCorrespondingIndexedEmbedded(XProperty memberWithContainedIn, String mappedBy, XProperty candidateProperty) {
		if ( !candidateProperty.isAnnotationPresent( IndexedEmbedded.class ) ) {
			return false;
		}
		else if ( mappedBy.equals( candidateProperty.getName() ) ) {
			return true;
		}
		else if ( mappedBy.isEmpty() ) { // Last chance: the mappedBy may be on the other side
			String reverseMappedBy = mappedBy( candidateProperty );
			return reverseMappedBy.equals( memberWithContainedIn.getName() );
		}
		else {
			return false;
		}
	}

	private void updateContainedInMetadataForProperty(ContainedInMetadataBuilder containedInMetadataBuilder, XProperty property) {
		IndexedEmbedded indexedEmbeddedAnnotation = property.getAnnotation( IndexedEmbedded.class );
		containedInMetadataBuilder.maxDepth( indexedEmbeddedAnnotation.depth() );
		containedInMetadataBuilder.prefix( buildEmbeddedPrefix( indexedEmbeddedAnnotation, property ) );
		containedInMetadataBuilder.includePaths( indexedEmbeddedAnnotation.includePaths() );
	}

	private String mappedBy(XMember member) {
		Annotation[] annotations = member.getAnnotations();
		for ( Annotation annotation : annotations ) {
			String mappedBy = mappedBy( annotation );
			if ( StringHelper.isNotEmpty( mappedBy ) ) {
				return mappedBy;
			}
		}
		return UNKNOWN_MAPPED_BY_ROLE;
	}

	private String mappedBy(Annotation annotation) {
		try {
			Method declaredMethod = annotation.annotationType().getDeclaredMethod( "mappedBy" );
			return (String) declaredMethod.invoke( annotation );
		}
		catch (SecurityException e) {
			return UNKNOWN_MAPPED_BY_ROLE;
		}
		catch (NoSuchMethodException e) {
			return UNKNOWN_MAPPED_BY_ROLE;
		}
		catch (IllegalArgumentException e) {
			return UNKNOWN_MAPPED_BY_ROLE;
		}
		catch (IllegalAccessException e) {
			return UNKNOWN_MAPPED_BY_ROLE;
		}
		catch (InvocationTargetException e) {
			return UNKNOWN_MAPPED_BY_ROLE;
		}
	}

	private NumericFieldsConfiguration buildNumericFieldsConfiguration(IndexedTypeIdentifier indexedTypeIdentifier,
			XProperty member,
			String prefix,
			PathsContext pathsContext,
			ParseContext parseContext) {
		Map<String, NumericField> fieldsMarkedAsNumeric = new LinkedHashMap<>();

		NumericField numericFieldAnnotation = member.getAnnotation( NumericField.class );
		if ( numericFieldAnnotation != null ) {
			if ( isFieldInPath(
					numericFieldAnnotation,
					member,
					pathsContext,
					prefix
			) || !parseContext.isMaxLevelReached() ) {
				fieldsMarkedAsNumeric.put( numericFieldAnnotation.forField(), numericFieldAnnotation );
			}
		}

		NumericFields numericFieldsAnnotation = member.getAnnotation( NumericFields.class );
		if ( numericFieldsAnnotation != null ) {
			for ( NumericField numericField : numericFieldsAnnotation.value() ) {
				if ( isFieldInPath(
						numericFieldAnnotation,
						member,
						pathsContext,
						prefix
				) || !parseContext.isMaxLevelReached() ) {
					NumericField existing = fieldsMarkedAsNumeric.put( numericField.forField(), numericField );
					if ( existing != null ) {
						throw log.severalNumericFieldAnnotationsForSameField( indexedTypeIdentifier, member.getName() );
					}
				}
			}
		}

		return new NumericFieldsConfiguration( indexedTypeIdentifier, member.getName(), fieldsMarkedAsNumeric );
	}

	private void checkForField(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			NumericFieldsConfiguration numericFields,
			String prefix,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {

		org.hibernate.search.annotations.Field fieldAnnotation =
				member.getAnnotation( org.hibernate.search.annotations.Field.class );

		if ( fieldAnnotation != null ) {
			if ( isFieldInPath( fieldAnnotation, member, pathsContext, prefix ) || !parseContext.isMaxLevelReached() ) {
				bindFieldAnnotation(
						prefix,
						fieldAnnotation,
						numericFields,
						typeMetadataBuilder,
						propertyMetadataBuilder,
						configContext,
						parseContext
				);
			}
		}
	}

	private Set<Facet> findMatchingFacetAnnotations(XMember member, String fieldName) {
		Facet facetAnnotation = member.getAnnotation( Facet.class );
		Facets facetsAnnotation = member.getAnnotation( Facets.class );

		if ( facetAnnotation == null && facetsAnnotation == null ) {
			return Collections.emptySet();
		}

		Set<Facet> matchingFacetAnnotations = new LinkedHashSet<>( 1 );

		if ( facetAnnotation != null ) {
			String forField = ReflectionHelper.getAttributeName( member, facetAnnotation.forField() );
			if ( forField.equals( fieldName ) ) {
				matchingFacetAnnotations.add( facetAnnotation );
			}
		}

		if ( facetsAnnotation != null ) {
			for ( Facet annotation : facetsAnnotation.value() ) {
				String forField = ReflectionHelper.getAttributeName( member, annotation.forField() );
				if ( forField.equals( fieldName ) ) {
					matchingFacetAnnotations.add( annotation );
				}
			}
		}
		return matchingFacetAnnotations;
	}

	private void bindFieldAnnotation(
			String prefix,
			org.hibernate.search.annotations.Field fieldAnnotation,
			NumericFieldsConfiguration numericFields,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			ConfigContext configContext,
			ParseContext parseContext) {

		XProperty member = propertyMetadataBuilder.getPropertyAccessor();

		if ( isPropertyTransient( member, configContext ) ) {
			// If the indexed values are derived from a Transient field, we can't rely on dirtiness of properties.
			// Only applies on JPA mapped entities.
			typeMetadataBuilder.disableStateInspectionOptimization();
		}

		final String relativeFieldName = ReflectionHelper.getAttributeName( member, fieldAnnotation.name() );
		final DocumentFieldPath fieldPath = new DocumentFieldPath( prefix, relativeFieldName );
		Store store = fieldAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex(
				fieldAnnotation.index(),
				fieldAnnotation.analyze(),
				fieldAnnotation.norms()
		);
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( fieldAnnotation.termVector() );

		NumericField numericFieldAnnotation = numericFields.getNumericFieldAnnotation( fieldPath );

		Set<Facet> facetAnnotations = findMatchingFacetAnnotations( member, relativeFieldName );

		FieldBridge fieldBridge;
		if ( parseContext.skipFieldBridges() ) {
			fieldBridge = null;
		}
		else {
			fieldBridge = bridgeFactory.buildFieldBridge(
					fieldAnnotation,
					member,
					false,
					numericFieldAnnotation != null,
					parseContext.getIndexManagerType(),
					reflectionManager,
					configContext.getServiceManager()
			);
		}

		final NumericEncodingType numericEncodingType = determineNumericFieldEncoding( fieldBridge );

		AnalyzerReference analyzerReference;
		if ( parseContext.skipAnalyzers() ) {
			analyzerReference = null;
		}
		else {
			analyzerReference = determineAnalyzer(
					typeMetadataBuilder, fieldPath,
					fieldAnnotation, member, configContext, parseContext );
			// adjust the type analyzer
			analyzerReference = typeMetadataBuilder.addToScopedAnalyzerReference(
					fieldPath,
					analyzerReference,
					index
			);
		}

		DocumentFieldMetadata.Builder fieldMetadataBuilder = new DocumentFieldMetadata.Builder(
						typeMetadataBuilder.getResultReference(),
						propertyMetadataBuilder.getResultReference(), propertyMetadataBuilder,
						fieldPath,
						store,
						index,
						termVector
				)
				.boost( AnnotationProcessingHelper.getBoost( member, fieldAnnotation ) )
				.fieldBridge( fieldBridge )
				.analyzerReference( analyzerReference );

		if ( fieldBridge instanceof MetadataProvidingFieldBridge ) {
			MetadataProvidingFieldBridge metadataProvidingFieldBridge = (MetadataProvidingFieldBridge) fieldBridge;
			FieldMetadataBuilderImpl bridgeContributedMetadata = getBridgeContributedFieldMetadata( fieldMetadataBuilder, metadataProvidingFieldBridge );
			for ( String sortableFieldAbsoluteName : bridgeContributedMetadata.getSortableFieldsAbsoluteNames() ) {
				SortableFieldMetadata sortableFieldMetadata = new SortableFieldMetadata.Builder( sortableFieldAbsoluteName ).build();
				propertyMetadataBuilder.addSortableField( sortableFieldMetadata );
			}

			for ( BridgeDefinedField field : bridgeContributedMetadata.getBridgeDefinedFields() ) {
				fieldMetadataBuilder.addBridgeDefinedField( field );
			}
		}

		if ( fieldBridge instanceof SpatialFieldBridge ) {
			fieldMetadataBuilder.spatial();
		}
		// if we are having a numeric value make sure to mark the metadata and set the precision
		// also numeric values don't need to be analyzed and norms are omitted (see also org.apache.lucene.document.LongField)
		else if ( isNumericField( numericFieldAnnotation, fieldBridge, fieldMetadataBuilder ) ) {
			fieldMetadataBuilder
					.index( Field.Index.NO.equals( index ) ? index : Field.Index.NOT_ANALYZED_NO_NORMS )
					.numeric()
					.precisionStep( AnnotationProcessingHelper.getPrecisionStep( numericFieldAnnotation ) )
					.numericEncodingType( numericEncodingType );
		}

		for ( Facet facetAnnotation : facetAnnotations ) {
			if ( Analyze.YES.equals( fieldAnnotation.analyze() ) ) {
				throw log.attemptToFacetOnAnalyzedField( fieldPath.getAbsoluteName(), member.getDeclaringClass().getName() );
			}
			String relativeFacetFieldName = facetAnnotation.name();
			if ( relativeFacetFieldName.isEmpty() ) {
				relativeFacetFieldName = relativeFieldName; // if not explicitly set the facet name is the same as the field name
			}
			DocumentFieldPath facetFieldPath = new DocumentFieldPath( prefix, relativeFacetFieldName );
			FacetMetadata.Builder facetMetadataBuilder = new FacetMetadata.Builder(
					fieldMetadataBuilder.getResultReference(), facetFieldPath
					);
			FacetEncodingType facetEncodingType = determineFacetEncodingType( member, facetAnnotation );
			facetMetadataBuilder.setFacetEncoding( facetEncodingType );
			facetMetadataBuilder.setFacetEncodingAuto( facetAnnotation.encoding().equals( FacetEncodingType.AUTO ) );
			fieldMetadataBuilder.addFacetMetadata( facetMetadataBuilder.build() );
		}

		final NullMarkerCodec nullTokenCodec = determineNullMarkerCodec( fieldMetadataBuilder,
				fieldBridge, fieldAnnotation, configContext,
				parseContext, typeMetadataBuilder.getIndexedType() );
		if ( nullTokenCodec != NotEncodingCodec.SINGLETON && fieldBridge instanceof TwoWayFieldBridge ) {
			fieldBridge = new NullEncodingTwoWayFieldBridge( (TwoWayFieldBridge) fieldBridge, nullTokenCodec );
			fieldMetadataBuilder.fieldBridge( fieldBridge );
		}
		fieldMetadataBuilder.indexNullAs( nullTokenCodec );

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();
		propertyMetadataBuilder.addDocumentField( fieldMetadata );

		// keep track of collection role names for ORM integration optimization based on collection update events
		parseContext.collectUnqualifiedCollectionRole( member.getName() );
	}

	private FacetEncodingType determineFacetEncodingType(XProperty member, Facet facetAnnotation) {
		FacetEncodingType facetEncodingType = facetAnnotation.encoding();
		if ( !facetEncodingType.equals( FacetEncodingType.AUTO ) ) {
			return facetEncodingType; // encoding type explicitly set
		}

		Class<?> indexedType = reflectionManager.toClass( returnedType( member ) );
		if ( ReflectionHelper.isIntegerType( indexedType ) ) {
			facetEncodingType = FacetEncodingType.LONG;
		}
		else if ( Date.class.isAssignableFrom( indexedType ) || Calendar.class.isAssignableFrom( indexedType ) ) {
			facetEncodingType = FacetEncodingType.LONG;
		}
		else if ( ReflectionHelper.isFloatingPointType( indexedType ) ) {
			facetEncodingType = FacetEncodingType.DOUBLE;
		}
		else if ( String.class.isAssignableFrom( indexedType ) ) {
			facetEncodingType = FacetEncodingType.STRING;
		}
		else {
			throw log.unsupportedFieldTypeForFaceting(
					indexedType.getName(),
					member.getDeclaringClass().getName(),
					member.getName()
			);
		}
		return facetEncodingType;
	}

	private boolean isNumericField(NumericField numericFieldAnnotation, FieldBridge fieldBridge, DocumentFieldMetadata.Builder fieldMetadataBuilder) {
		if ( numericFieldAnnotation != null ) {
			// @NumericField is specified explicitly
			return true;
		}
		if ( NumericFieldUtils.isNumericContainerOrNumericFieldBridge( fieldBridge ) ) {
			// An implicit numeric value is encoded via a numeric field bridge
			return true;
		}
		BridgeDefinedField bridgeDefinedField = fieldMetadataBuilder.getBridgeDefinedFields().get( fieldMetadataBuilder.getAbsoluteName() );
		if ( bridgeDefinedField != null ) {
			// The field bridge explicitly declared the default field as numeric
			switch ( bridgeDefinedField.getType() ) {
				case DOUBLE:
				case FLOAT:
				case INTEGER:
				case LONG:
					return true;
				case BOOLEAN:
				case DATE:
				case OBJECT:
				case STRING:
				default:
					return false;

			}
		}
		return false;
	}

	private NumericEncodingType determineNumericFieldEncoding(FieldBridge fieldBridge) {
		EncodingBridge encodingBridge = BridgeAdaptorUtils.unwrapAdaptorAndContainer( fieldBridge, EncodingBridge.class );

		if ( encodingBridge != null ) {
			return encodingBridge.getEncodingType();
		}
		else {
			return NumericEncodingType.UNKNOWN;
		}
	}

	private NullMarkerCodec determineNullMarkerCodec(PartialDocumentFieldMetadata fieldMetadata,
			FieldBridge fieldBridge, org.hibernate.search.annotations.Field fieldAnnotation,
			ConfigContext context, ParseContext parseContext, IndexedTypeIdentifier indexedTypeIdentifier) {
		if ( parseContext.skipNullMarkerCodec() ) {
			return NotEncodingCodec.SINGLETON;
		}

		if ( fieldAnnotation == null ) {
			// The option of null-markers is not being used
			return NotEncodingCodec.SINGLETON;
		}

		String indexNullAs = fieldAnnotation.indexNullAs();
		if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DO_NOT_INDEX_NULL ) ) {
			// The option is explicitly disabled
			return NotEncodingCodec.SINGLETON;
		}
		else {
			NullMarker nullMarker;
			if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DEFAULT_NULL_TOKEN ) ) {
				// Use the default null token
				// This will require the global default to be an encodable value
				nullMarker = createNullMarker( fieldBridge, context.getDefaultNullToken(), fieldMetadata.getPath() );
			}
			else {
				// Use the default null token
				// This will require 'indexNullAs' to be an encodable value
				nullMarker = createNullMarker( fieldBridge, indexNullAs, fieldMetadata.getPath() );
			}

			IndexManagerType indexManagerType = parseContext.getIndexManagerType();
			MissingValueStrategy missingValueStrategy = context.forType( indexManagerType ).getMissingValueStrategy();

			return missingValueStrategy.createNullMarkerCodec( indexedTypeIdentifier, fieldMetadata, nullMarker );
		}
	}

	private NullMarker createNullMarker(FieldBridge fieldBridge, String marker, DocumentFieldPath path) {
		EncodingBridge encodingBridge = BridgeAdaptorUtils.unwrapAdaptorOnly( fieldBridge, EncodingBridge.class );
		if ( encodingBridge != null ) {
			try {
				return encodingBridge.createNullMarker( marker );
			}
			catch (IllegalArgumentException e) {
				throw log.nullMarkerInvalidFormat( marker, path.getAbsoluteName(), e.getLocalizedMessage(), e );
			}
		}
		else {
			return new ToStringNullMarker( marker );
		}
	}

	private AnalyzerReference determineAnalyzer(
			TypeMetadata.Builder typeMetadataBuilder, DocumentFieldPath fieldPath,
			org.hibernate.search.annotations.Field fieldAnnotation,
			XProperty member,
			ConfigContext context,
			ParseContext parseContext) {
		AnalyzerReference analyzerReference = null;

		if ( !parseContext.skipAnalyzers() ) {
			// check for a nested @Analyzer/@Normalizer annotation with @Field
			if ( fieldAnnotation != null ) {
				analyzerReference = AnnotationProcessingHelper.getAnalyzerReference(
						typeMetadataBuilder.getIndexedType(), fieldPath,
						fieldAnnotation.analyzer(),
						fieldAnnotation.normalizer(),
						context,
						parseContext.getIndexManagerType() );
			}

			// if there was no analyzer specified as part of @Field, try a stand alone @Analyzer annotation
			if ( analyzerReference == null ) {
				analyzerReference = AnnotationProcessingHelper.getAnalyzerReference(
						member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
						context,
						parseContext.getIndexManagerType()
				);
			}
		}

		return analyzerReference;
	}

	private boolean isPropertyTransient(XProperty member, ConfigContext context) {
		if ( !context.isJpaPresent() ) {
			return false;
		}
		else {
			Annotation transientAnnotation;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Annotation> transientAnnotationClass =
						ClassLoaderHelper.classForName(
								"javax.persistence.Transient",
								configContext.getServiceManager()
						);
				transientAnnotation = member.getAnnotation( transientAnnotationClass );
			}
			catch (ClassLoadingException e) {
				throw new SearchException( "Unable to load @Transient.class even though it should be present ?!" );
			}
			return transientAnnotation != null;
		}
	}


	private void checkForSpatialsAnnotation(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			String prefix,
			PathsContext pathsContext,
			ParseContext parseContext) {
		org.hibernate.search.annotations.Spatials spatialsAnnotation = member.getAnnotation( org.hibernate.search.annotations.Spatials.class );
		if ( spatialsAnnotation != null ) {
			for ( org.hibernate.search.annotations.Spatial spatialAnnotation : spatialsAnnotation.value() ) {
				if ( isFieldInPath(
						spatialAnnotation,
						member,
						pathsContext,
						prefix
				) || !parseContext.isMaxLevelReached() ) {
					bindSpatialAnnotation(
							spatialAnnotation,
							prefix,
							member,
							typeMetadataBuilder,
							propertyMetadataBuilder,
							parseContext
					);
				}
			}
		}
	}

	private void checkForSpatial(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			String prefix,
			PathsContext pathsContext,
			ParseContext parseContext) {
		Spatial spatialAnnotation = member.getAnnotation( Spatial.class );
		if ( spatialAnnotation != null ) {
			if ( isFieldInPath(
					spatialAnnotation,
					member,
					pathsContext,
					prefix
			) || !parseContext.isMaxLevelReached() ) {
				bindSpatialAnnotation( spatialAnnotation, prefix, member, typeMetadataBuilder, propertyMetadataBuilder, parseContext );
			}
		}
	}

	private void checkForSortableField(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			String prefix,
			boolean isIdProperty,
			PathsContext pathsContext,
			ParseContext parseContext) {
		SortableField sortableFieldAnnotation = member.getAnnotation( SortableField.class );
		if ( sortableFieldAnnotation != null ) {
			if ( isFieldInPath(
					sortableFieldAnnotation,
					member,
					pathsContext,
					prefix
			) || !parseContext.isMaxLevelReached() ) {
				bindSortableFieldAnnotation( sortableFieldAnnotation, prefix, member, typeMetadataBuilder, propertyMetadataBuilder, isIdProperty, parseContext );
			}
		}
	}

	private void checkForSortableFields(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			String prefix,
			boolean isIdProperty,
			PathsContext pathsContext,
			ParseContext parseContext) {
		SortableFields sortableFieldsAnnotation = member.getAnnotation( SortableFields.class );
		if ( sortableFieldsAnnotation != null ) {
			for ( SortableField sortableFieldAnnotation : sortableFieldsAnnotation.value() ) {
				if ( isFieldInPath(
						sortableFieldAnnotation,
						member,
						pathsContext,
						prefix
				) || !parseContext.isMaxLevelReached() ) {
					bindSortableFieldAnnotation(
							sortableFieldAnnotation,
							prefix,
							member,
							typeMetadataBuilder,
							propertyMetadataBuilder,
							isIdProperty,
							parseContext
					);
				}
			}
		}
	}

	private void checkForAnalyzerDefs(XAnnotatedElement annotatedElement, ConfigContext context) {
		MappingDefinitionRegistry<AnalyzerDef, ?> registry = context.getAnalyzerDefinitionRegistry();

		AnalyzerDefs defs = annotatedElement.getAnnotation( AnalyzerDefs.class );
		if ( defs != null ) {
			for ( AnalyzerDef def : defs.value() ) {
				registry.registerFromAnnotation( def.name(), def, annotatedElement );
			}
		}
		AnalyzerDef def = annotatedElement.getAnnotation( AnalyzerDef.class );
		if ( def != null ) {
			registry.registerFromAnnotation( def.name(), def, annotatedElement );
		}
	}

	private void checkForNormalizerDefs(XAnnotatedElement annotatedElement, ConfigContext context) {
		MappingDefinitionRegistry<NormalizerDef, ?> registry = context.getNormalizerDefinitionRegistry();

		NormalizerDefs defs = annotatedElement.getAnnotation( NormalizerDefs.class );
		if ( defs != null ) {
			for ( NormalizerDef def : defs.value() ) {
				registry.registerFromAnnotation( def.name(), def, annotatedElement );
			}
		}
		NormalizerDef def = annotatedElement.getAnnotation( NormalizerDef.class );
		if ( def != null ) {
			registry.registerFromAnnotation( def.name(), def, annotatedElement );
		}
	}

	private void checkForFullTextFilterDefs(XAnnotatedElement annotatedElement, ConfigContext context) {
		MappingDefinitionRegistry<FullTextFilterDef, ?> registry = context.getFullTextFilterDefinitionRegistry();

		FullTextFilterDefs defs = annotatedElement.getAnnotation( FullTextFilterDefs.class );
		if ( defs != null ) {
			for ( FullTextFilterDef def : defs.value() ) {
				registry.registerFromAnnotation( def.name(), def, annotatedElement );
			}
		}
		FullTextFilterDef def = annotatedElement.getAnnotation( FullTextFilterDef.class );
		if ( def != null ) {
			registry.registerFromAnnotation( def.name(), def, annotatedElement );
		}
	}

	private void checkForAnalyzerDiscriminator(XAnnotatedElement annotatedElement,
			TypeMetadata.Builder typeMetadataBuilder,
			ConfigContext context) {

		AnalyzerDiscriminator discriminatorAnnotation = annotatedElement.getAnnotation( AnalyzerDiscriminator.class );
		if ( discriminatorAnnotation == null ) {
			return;
		}

		if ( annotatedElement instanceof XProperty && isPropertyTransient( (XProperty) annotatedElement, context ) ) {
			//if the discriminator is calculated on a @Transient field, we can't trust field level dirtyness
			typeMetadataBuilder.disableStateInspectionOptimization();
		}

		Class<? extends Discriminator> discriminatorClass = discriminatorAnnotation.impl();
		Discriminator discriminator = ClassLoaderHelper.instanceFromClass( Discriminator.class, discriminatorClass, "analyzer discriminator implementation" );

		if ( annotatedElement instanceof XMember ) {
			typeMetadataBuilder.analyzerDiscriminator( discriminator, (XMember) annotatedElement );
		}
		else {
			typeMetadataBuilder.analyzerDiscriminator( discriminator, null );
		}
	}

	private void checkForFields(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			NumericFieldsConfiguration numericFields,
			String prefix,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {
		Fields fieldsAnnotation = member.getAnnotation( Fields.class );
		if ( fieldsAnnotation != null && fieldsAnnotation.value().length > 0 ) {
			for ( org.hibernate.search.annotations.Field fieldAnnotation : fieldsAnnotation.value() ) {
				if ( isFieldInPath(
						fieldAnnotation,
						member,
						pathsContext,
						prefix
				) || !parseContext.isMaxLevelReached() ) {
					bindFieldAnnotation(
							prefix,
							fieldAnnotation,
							numericFields,
							typeMetadataBuilder,
							propertyMetadataBuilder,
							configContext,
							parseContext
					);
				}
			}
		}
	}

	private boolean isFieldInPath(Annotation fieldAnnotation,
			XProperty member,
			PathsContext pathsContext,
			String prefix) {
		if ( pathsContext != null ) {
			DocumentFieldPath path = new DocumentFieldPath( prefix, fieldName( fieldAnnotation, member ) );
			if ( pathsContext.isIncluded( path ) ) {
				pathsContext.markEncounteredPath( path );
				return true;
			}
		}
		return false;
	}

	private String fieldName(Annotation fieldAnnotation, XProperty member) {
		if ( fieldAnnotation == null ) {
			return member.getName();
		}
		final String fieldName = getFieldName( fieldAnnotation );
		if ( fieldName == null || fieldName.isEmpty() ) {
			return member.getName();
		}
		return fieldName;
	}

	private void checkForIndexedEmbedded(
			XProperty member,
			PropertyMetadata.Builder propertyMetadataBuilder,
			String prefix,
			boolean disableOptimizations,
			TypeMetadata.Builder typeMetadataBuilder,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {
		IndexedEmbedded indexedEmbeddedAnnotation = member.getAnnotation( IndexedEmbedded.class );
		if ( indexedEmbeddedAnnotation == null ) {
			return;
		}

		parseContext.collectUnqualifiedCollectionRole( member.getName() );

		int oldMaxLevel = parseContext.getMaxLevel();
		int potentialLevel = potentialLevel( parseContext, indexedEmbeddedAnnotation, member );
		// HSEARCH-1442 recreating the behaviour prior to PropertiesMetadata refactoring
		// not sure whether this is algorithmically correct though. @IndexedEmbedded processing should be refactored (HF)
		if ( potentialLevel < oldMaxLevel ) {
			parseContext.setMaxLevel( potentialLevel );
		}
		parseContext.incrementLevel();

		XClass elementClass = elementClass( member, indexedEmbeddedAnnotation );
		String localPrefix = buildEmbeddedPrefix( indexedEmbeddedAnnotation, member );
		String fullPrefix = prefix + localPrefix;
		boolean includeEmbeddedObjectId = indexedEmbeddedAnnotation.includeEmbeddedObjectId();

		if ( parseContext.getMaxLevel() == INFINITE_DEPTH
				&& parseContext.hasBeenProcessed( elementClass ) ) {
			throw log.detectInfiniteTypeLoopInIndexedEmbedded(
					elementClass.getName(),
					typeMetadataBuilder.getIndexedType().getName(),
					fullPrefix );
		}

		PathsContext updatedPathsContext = updatePaths( fullPrefix, pathsContext, indexedEmbeddedAnnotation );

		boolean pathsCreatedAtThisLevel = false;
		if ( pathsContext == null && updatedPathsContext != null ) {
			//after this level if not all paths are traversed, then the paths
			//either don't exist in the object graph, or aren't indexed paths
			pathsCreatedAtThisLevel = true;
		}

		if ( !parseContext.isMaxLevelReached() || isInPath(
				fullPrefix,
				updatedPathsContext,
				indexedEmbeddedAnnotation
		) ) {
			parseContext.processingClass( elementClass ); //push
			final IndexedTypeIdentifier elementTypeIdentifier = new PojoIndexedTypeIdentifier( reflectionManager.toClass( elementClass ) );

			EmbeddedTypeMetadata.Builder embeddedTypeMetadataBuilder =
					new EmbeddedTypeMetadata.Builder(
							typeMetadataBuilder,
							elementTypeIdentifier,
							propertyMetadataBuilder.getResultReference(),
							member,
							localPrefix
					);

			embeddedTypeMetadataBuilder.boost( getBoost( elementClass ) * AnnotationProcessingHelper.getBoost( member, null ) );
			//property > entity analyzer
			if ( !parseContext.skipAnalyzers() ) {
				AnalyzerReference analyzerReference = AnnotationProcessingHelper.
						getAnalyzerReference(
								member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
								configContext,
								parseContext.getIndexManagerType()
						);
				if ( analyzerReference == null ) {
					analyzerReference = typeMetadataBuilder.getAnalyzerReference();
				}
				embeddedTypeMetadataBuilder.analyzerReference( analyzerReference );
			}

			if ( disableOptimizations ) {
				typeMetadataBuilder.blacklistForOptimization( elementClass );
			}

			// about to do a recursion, keep parse state which needs resetting
			XClass previousClass = parseContext.getCurrentClass();
			parseContext.setCurrentClass( elementClass );
			boolean previousIncludeEmbeddedObjectId = parseContext.includeEmbeddedObjectId();
			parseContext.setIncludeEmbeddedObjectId( includeEmbeddedObjectId );
			initializeClass(
					embeddedTypeMetadataBuilder,
					false,
					fullPrefix,
					parseContext,
					configContext,
					disableOptimizations,
					updatedPathsContext
			);

			// reset the context state
			parseContext.setCurrentClass( previousClass );
			parseContext.setIncludeEmbeddedObjectId( previousIncludeEmbeddedObjectId );

			final String indexNullAs = embeddedNullToken( configContext, indexedEmbeddedAnnotation );
			if ( indexNullAs != null ) {
				FieldBridge fieldBridge = new NullEncodingFieldBridge( NULL_EMBEDDED_STRING_BRIDGE, indexNullAs );
				embeddedTypeMetadataBuilder.indexNullToken(
						indexNullAs,
						embeddedNullField( fullPrefix ),
						fieldBridge
				);
			}

			EmbeddedTypeMetadata embeddedTypeMetadata = embeddedTypeMetadataBuilder.build();
			for ( XClass xClass : embeddedTypeMetadata.getOptimizationBlackList() ) {
				typeMetadataBuilder.blacklistForOptimization( xClass );
			}
			typeMetadataBuilder.addEmbeddedType( embeddedTypeMetadata );

			parseContext.removeProcessedClass( elementClass ); //pop
		}
		else if ( log.isTraceEnabled() ) {
			log.tracef( "depth reached, ignoring %s", fullPrefix );
		}

		parseContext.decrementLevel();
		parseContext.setMaxLevel( oldMaxLevel ); //set back the old max level

		if ( pathsCreatedAtThisLevel ) {
			validateAllPathsEncountered( member, updatedPathsContext, indexedEmbeddedAnnotation );
		}
	}

	private int potentialLevel(ParseContext parseContext, IndexedEmbedded indexedEmbeddedAnnotation, XProperty member) {
		int potentialLevel = depth( indexedEmbeddedAnnotation ) + parseContext.getLevel();
		// This is really catching a possible int overflow. depth() can return Integer.MAX_VALUE, which then can
		// overflow in case level > 0. Really this code should be rewritten (HF)
		if ( potentialLevel < 0 ) {
			potentialLevel = INFINITE_DEPTH;
		}
		return potentialLevel;
	}

	private XClass elementClass(XProperty member, IndexedEmbedded indexedEmbeddedAnnotation) {
		if ( void.class == indexedEmbeddedAnnotation.targetElement() ) {
			return returnedType( member );
		}
		else {
			return reflectionManager.toXClass( indexedEmbeddedAnnotation.targetElement() );
		}
	}

	private XClass returnedType(XProperty member) {
		return member.getElementClass();
	}

	private int depth(IndexedEmbedded embeddedAnn) {
		if ( !isDepthSet( embeddedAnn ) && embeddedAnn.includePaths().length > 0 ) {
			return 0;
		}
		return embeddedAnn.depth();
	}

	private boolean isDepthSet(IndexedEmbedded embeddedAnn) {
		return INFINITE_DEPTH != embeddedAnn.depth();
	}

	private boolean isInPath(String localPrefix, PathsContext pathsContext, IndexedEmbedded embeddedAnn) {
		if ( pathsContext != null ) {
			boolean defaultPrefix = isDefaultPrefix( embeddedAnn );
			for ( String path : pathsContext.getEncounteredPaths() ) {
				String app = path;
				if ( defaultPrefix ) {
					app += ".";
				}
				if ( app.startsWith( localPrefix ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private PathsContext updatePaths(String localPrefix, PathsContext pathsContext, IndexedEmbedded indexedEmbeddedAnnotation) {
		if ( pathsContext != null ) {
			// There is an upper-level path restriction: let it override any nested restriction
			return pathsContext;
		}
		else if ( indexedEmbeddedAnnotation.includePaths().length == 0 ) {
			// No upper-level restriction and no restriction on this level, either
			return null;
		}

		PathsContext newPathsContext = new PathsContext();
		for ( String path : indexedEmbeddedAnnotation.includePaths() ) {
			newPathsContext.addIncludedPath( localPrefix + path );
		}
		return newPathsContext;
	}

	private String buildEmbeddedPrefix(IndexedEmbedded indexedEmbeddedAnnotation, XProperty member) {
		if ( isDefaultPrefix( indexedEmbeddedAnnotation ) ) {
			//default to property name
			return defaultPrefix( member );
		}
		else {
			return indexedEmbeddedAnnotation.prefix();
		}
	}

	private String defaultPrefix(XProperty member) {
		return member.getName() + '.';
	}

	private boolean isDefaultPrefix(IndexedEmbedded indexedEmbeddedAnnotation) {
		return ".".equals( indexedEmbeddedAnnotation.prefix() );
	}

	private String embeddedNullField(String localPrefix) {
		if ( localPrefix.endsWith( "." ) ) {
			return localPrefix.substring( 0, localPrefix.length() - 1 );
		}
		return localPrefix;
	}

	private void validateAllPathsEncountered(XProperty member, PathsContext updatedPathsContext, IndexedEmbedded indexedEmbeddedAnnotation) {
		Set<String> unEncounteredPaths = updatedPathsContext.getUnEncounteredPaths();
		if ( unEncounteredPaths.size() > 0 ) {
			StringBuilder sb = new StringBuilder( );
			String prefix = indexedEmbeddedAnnotation.prefix();
			for ( String path : unEncounteredPaths ) {
				sb.append( removeLeadingPrefixFromPath( path, prefix ) );
				sb.append( ',' );
			}
			String invalidPaths = sb.substring( 0, sb.length() - 1 );
			throw log.invalidIncludePathConfiguration( member.getName(), member.getDeclaringClass().getName(), invalidPaths );
		}
	}

	private String removeLeadingPrefixFromPath(String path, String prefix) {
		if ( path.startsWith( prefix ) ) {
			return path.substring( prefix.length() );
		}
		return path;
	}

	private String embeddedNullToken(ConfigContext context, IndexedEmbedded indexedEmbeddedAnnotation) {
		String indexNullAs = indexedEmbeddedAnnotation.indexNullAs();
		if ( org.hibernate.search.annotations.IndexedEmbedded.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			return null;
		}
		if ( org.hibernate.search.annotations.IndexedEmbedded.DEFAULT_NULL_TOKEN.equals( indexNullAs ) ) {
			return context.getDefaultNullToken();
		}
		return indexNullAs;
	}

	/**
	 * Verifies entity level preconditions to know if it's safe to skip index updates based
	 * on specific field or collection updates.
	 *
	 * @return true if it seems safe to apply such optimizations
	 */
	boolean stateInspectionOptimizationsEnabled(TypeMetadata.Builder typeMetadataBuilder) {
		if ( !typeMetadataBuilder.isStateInspectionOptimizationsEnabled() ) {
			return false;
		}
		if ( typeMetadataBuilder.areClassBridgesUsed() ) {
			log.tracef(
					"State inspection optimization disabled as entity %s uses class bridges",
					typeMetadataBuilder.getIndexedType().getName()
			);
			return false; // can't know what a class bridge is going to look at -> reindex
		}
		BoostStrategy boostStrategy = typeMetadataBuilder.getClassBoostStrategy();
		if ( boostStrategy != null && !( boostStrategy instanceof DefaultBoostStrategy ) ) {
			log.tracef(
					"State inspection optimization disabled as DynamicBoost is enabled on entity %s",
					typeMetadataBuilder.getIndexedType().getName()
			);
			return false; // as with class bridge: might be affected by any field
		}
		return true;
	}

	private FieldMetadataBuilderImpl getBridgeContributedFieldMetadata(DocumentFieldMetadata.Builder fieldMetadataBuilder,
			MetadataProvidingFieldBridge metadataProvidingFieldBridge) {
		FieldMetadataBuilderImpl builder = new FieldMetadataBuilderImpl( fieldMetadataBuilder.getResultReference() );
		metadataProvidingFieldBridge.configureFieldMetadata( fieldMetadataBuilder.getAbsoluteName(), builder );
		return builder;
	}

	private boolean hasExplicitDocumentId(XClass type) {
		List<XProperty> methods = type.getDeclaredProperties( XClass.ACCESS_PROPERTY );
		for ( XProperty method : methods ) {
			if ( method.isAnnotationPresent( DocumentId.class ) ) {
				return true;
			}
		}

		List<XProperty> fields = type.getDeclaredProperties( XClass.ACCESS_FIELD );
		for ( XProperty field : fields ) {
			if ( field.isAnnotationPresent( DocumentId.class ) ) {
				return true;
			}
		}

		return false;
	}

}
