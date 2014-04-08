/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.engine.metadata.impl;

import static org.hibernate.search.engine.impl.AnnotationProcessingHelper.getFieldName;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.SearchException;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Spatials;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.DefaultStringBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.impl.BridgeFactory;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.AnnotationProcessingHelper;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.spatial.Coordinates;
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
public class AnnotationMetadataProvider implements MetadataProvider {
	private static final Log log = LoggerFactory.make();
	private static final StringBridge NULL_EMBEDDED_STRING_BRIDGE = DefaultStringBridge.INSTANCE;
	private static final String UNKNOWN_MAPPED_BY_ROLE = "";
	private static final String EMPTY_PREFIX = "";

	private final ReflectionManager reflectionManager;
	private final ConfigContext configContext;

	public AnnotationMetadataProvider(ReflectionManager reflectionManager, ConfigContext configContext) {
		this.reflectionManager = reflectionManager;
		this.configContext = configContext;
	}

	@Override
	public TypeMetadata getTypeMetadataFor(Class<?> clazz) {
		XClass xClass = reflectionManager.toXClass( clazz );
		TypeMetadata.Builder typeMetadataBuilder = new TypeMetadata.Builder( clazz, configContext )
				.boost( getBoost( xClass ) )
				.boostStrategy( AnnotationProcessingHelper.getDynamicBoost( xClass ) )
				.analyzer( configContext.getDefaultAnalyzer() );

		ParseContext parseContext = new ParseContext();
		parseContext.processingClass( xClass );
		parseContext.setCurrentClass( xClass );

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

	@Override
	public boolean containsSearchMetadata(Class<?> clazz) {
		XClass xClass = reflectionManager.toXClass( clazz );
		return ReflectionHelper.containsSearchAnnotations( xClass );
	}

	private void checkDocumentId(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			boolean isRoot,
			String prefix,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {
		Annotation idAnnotation = getIdAnnotation( member, typeMetadataBuilder, configContext );
		NumericField numericFieldAnn = member.getAnnotation( NumericField.class );
		if ( idAnnotation != null ) {
			String attributeName = getIdAttributeName( member, idAnnotation );
			if ( pathsContext != null ) {
				pathsContext.markEncounteredPath( prefix + attributeName );
			}
			if ( isRoot ) {
				if ( parseContext.isExplicitDocumentId() ) {
					if ( idAnnotation instanceof DocumentId ) {
						throw log.duplicateDocumentIdFound( typeMetadataBuilder.getIndexedType().getName() );
					}
					else {
						//If it's not a DocumentId it's a JPA @Id: ignore it as we already have a @DocumentId
						return;
					}
				}
				if ( idAnnotation instanceof DocumentId ) {
					parseContext.setExplicitDocumentId( true );
				}

				FieldBridge idBridge = BridgeFactory.guessType( null, numericFieldAnn, member, reflectionManager );
				if ( !( idBridge instanceof TwoWayFieldBridge ) ) {
					throw new SearchException(
							"Bridge for document id does not implement TwoWayFieldBridge: " + member.getName()
					);
				}

				Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( TermVector.NO );
				DocumentFieldMetadata fieldMetadata =
						new DocumentFieldMetadata.Builder(
								prefix + attributeName,
								Store.YES,
								Field.Index.NOT_ANALYZED_NO_NORMS,
								termVector
						)
								.id()
								.boost( AnnotationProcessingHelper.getBoost( member, null ) )
								.fieldBridge( idBridge )
								.build();
				PropertyMetadata idPropertyMetadata = new PropertyMetadata.Builder( member )
						.addDocumentField( fieldMetadata )
						.build();
				typeMetadataBuilder.idProperty( idPropertyMetadata );
			}
			else {
				//component should index their document id
				String fieldName = prefix + attributeName;
				Field.Index index = AnnotationProcessingHelper.getIndex( Index.YES, Analyze.NO, Norms.YES );
				Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( TermVector.NO );
				FieldBridge fieldBridge = BridgeFactory.guessType( null, null, member, reflectionManager );

				DocumentFieldMetadata fieldMetadata =
						new DocumentFieldMetadata.Builder( fieldName, Store.YES, index, termVector )
								.boost( AnnotationProcessingHelper.getBoost( member, null ) )
								.fieldBridge( fieldBridge )
								.build();

				PropertyMetadata propertyMetadata = new PropertyMetadata.Builder( member )
						.addDocumentField( fieldMetadata )
						.dynamicBoostStrategy( AnnotationProcessingHelper.getDynamicBoost( member ) )
						.build();

				typeMetadataBuilder.addProperty( propertyMetadata );

				// property > entity analyzer (no field analyzer)
				Analyzer analyzer = AnnotationProcessingHelper.getAnalyzer(
						member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
						configContext
				);
				if ( analyzer == null ) {
					analyzer = typeMetadataBuilder.getAnalyzer();
				}
				if ( analyzer == null ) {
					throw new AssertionFailure( "Analyzer should not be undefined" );
				}
				typeMetadataBuilder.addToScopedAnalyzer( fieldName, analyzer, index );
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
	private Annotation getIdAnnotation(XProperty member, TypeMetadata.Builder typeMetadataBuilder, ConfigContext context) {
		Annotation idAnnotation = null;

		// check for explicit DocumentId
		DocumentId documentIdAnnotation = member.getAnnotation( DocumentId.class );
		if ( documentIdAnnotation != null ) {
			idAnnotation = documentIdAnnotation;
		}
		// check for JPA @Id
		if ( context.isJpaPresent() ) {
			Annotation jpaId;
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Annotation> jpaIdClass =
						ClassLoaderHelper.classForName( "javax.persistence.Id", ConfigContext.class.getClassLoader() );
				jpaId = member.getAnnotation( jpaIdClass );
			}
			catch (ClassNotFoundException e) {
				throw new SearchException( "Unable to load @Id.class even though it should be present ?!" );
			}
			if ( jpaId != null ) {
				typeMetadataBuilder.jpaProperty( member );
				if ( documentIdAnnotation == null ) {
					log.debug( "Found JPA id and using it as document id" );
					idAnnotation = jpaId;
				}
			}
		}
		return idAnnotation;
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
		catch (Exception e) {
			// ignore
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
		for ( XClass currentClass : hierarchy ) {
			parseContext.setCurrentClass( currentClass );
			initializeClassLevelAnnotations( typeMetadataBuilder, prefix, configContext, parseContext );
			initializeClassBridgeInstances( typeMetadataBuilder, prefix, configContext, currentClass );
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
						configContext,
						pathsContext,
						parseContext
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
						configContext,
						pathsContext,
						parseContext
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
		Analyzer analyzer = AnnotationProcessingHelper.getAnalyzer(
				clazz.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
				configContext
		);
		if ( analyzer != null ) {
			typeMetadataBuilder.analyzer( analyzer );
		}

		// check for AnalyzerDefs annotations
		checkForAnalyzerDefs( clazz, configContext );

		// Check for any ClassBridges annotation.
		ClassBridges classBridgesAnnotation = clazz.getAnnotation( ClassBridges.class );
		if ( classBridgesAnnotation != null ) {
			ClassBridge[] classBridges = classBridgesAnnotation.value();
			for ( ClassBridge cb : classBridges ) {
				bindClassBridgeAnnotation( prefix, typeMetadataBuilder, cb, clazz, configContext );
			}
		}

		// Check for any ClassBridge style of annotations.
		ClassBridge classBridgeAnnotation = clazz.getAnnotation( ClassBridge.class );
		if ( classBridgeAnnotation != null ) {
			bindClassBridgeAnnotation( prefix, typeMetadataBuilder, classBridgeAnnotation, clazz, configContext );
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
			XClass clazz) {

		Map<FieldBridge, ClassBridge> classBridgeInstances = configContext.getClassBridgeInstances( reflectionManager.toClass( clazz ) );

		for ( Entry<FieldBridge, ClassBridge> classBridge : classBridgeInstances.entrySet() ) {
			FieldBridge instance = classBridge.getKey();
			ClassBridge configuration = classBridge.getValue();

			bindClassBridgeAnnotation( prefix, typeMetadataBuilder, configuration, instance, configContext );
		}
	}

	private void bindClassBridgeAnnotation(String prefix,
			TypeMetadata.Builder typeMetadataBuilder,
			ClassBridge classBridgeAnnotation,
			XClass clazz,
			ConfigContext configContext) {
		FieldBridge fieldBridge = BridgeFactory.extractType( classBridgeAnnotation, reflectionManager.toClass( clazz ) );
		bindClassBridgeAnnotation( prefix, typeMetadataBuilder, classBridgeAnnotation, fieldBridge, configContext );
	}

	private void bindClassBridgeAnnotation(String prefix,
			TypeMetadata.Builder typeMetadataBuilder,
			ClassBridge classBridgeAnnotation,
			FieldBridge fieldBridge,
			ConfigContext configContext) {

		BridgeFactory.injectParameters( classBridgeAnnotation, fieldBridge );

		String fieldName = prefix + classBridgeAnnotation.name();
		Store store = classBridgeAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex(
				classBridgeAnnotation.index(),
				classBridgeAnnotation.analyze(),
				classBridgeAnnotation.norms()
		);
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( classBridgeAnnotation.termVector() );

		DocumentFieldMetadata fieldMetadata = new DocumentFieldMetadata.Builder( fieldName, store, index, termVector )
				.boost( classBridgeAnnotation.boost().value() )
				.fieldBridge( fieldBridge )
				.build();

		typeMetadataBuilder.addClassBridgeField( fieldMetadata );

		Analyzer analyzer = AnnotationProcessingHelper.getAnalyzer( classBridgeAnnotation.analyzer(), configContext );
		typeMetadataBuilder.addToScopedAnalyzer( fieldName, analyzer, index );
	}

	private void bindSpatialAnnotation(Spatial spatialAnnotation,
			String prefix,
			XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			ParseContext parseContext) {

		if ( parseContext.isSpatialNameUsed( spatialAnnotation.name() ) ) {
			throw log.cannotHaveTwoSpatialsWithDefaultOrSameName( member.getType().getName() );
		}
		parseContext.markSpatialNameAsUsed( spatialAnnotation.name() );

		String fieldName = prefix + ReflectionHelper.getAttributeName( member, spatialAnnotation.name() );
		Store store = spatialAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex( Index.YES, Analyze.NO, Norms.NO );
		Field.TermVector termVector = Field.TermVector.NO;
		FieldBridge fieldBridge = BridgeFactory.guessType(
				null,
				null,
				member,
				reflectionManager
		);

		DocumentFieldMetadata fieldMetadata = new DocumentFieldMetadata.Builder( fieldName, store, index, termVector )
				.boost( AnnotationProcessingHelper.getBoost( member, spatialAnnotation ) )
				.fieldBridge( fieldBridge )
				.build();

		PropertyMetadata propertyMetadata = new PropertyMetadata.Builder( member )
				.addDocumentField( fieldMetadata )
				.build();

		typeMetadataBuilder.addProperty( propertyMetadata );
		if ( member.isCollection() ) {
			parseContext.collectUnqualifiedCollectionRole( member.getName() );
		}
	}

	private void bindSpatialAnnotation(Spatial spatialAnnotation,
			String prefix,
			TypeMetadata.Builder typeMetadataBuilder,
			ParseContext parseContext) {
		String fieldName;
		if ( !spatialAnnotation.name().isEmpty() ) {
			fieldName = prefix + spatialAnnotation.name();
		}
		else {
			fieldName = Spatial.COORDINATES_DEFAULT_FIELD;
		}

		if ( parseContext.isSpatialNameUsed( spatialAnnotation.name() ) ) {
			throw log.cannotHaveTwoSpatialsWithDefaultOrSameName( parseContext.getCurrentClass().getName() );
		}
		parseContext.markSpatialNameAsUsed( spatialAnnotation.name() );

		Store store = spatialAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex( Index.YES, Analyze.NO, Norms.NO );
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( TermVector.NO );
		FieldBridge spatialBridge = determineSpatialFieldBridge( spatialAnnotation, parseContext );

		DocumentFieldMetadata fieldMetadata = new DocumentFieldMetadata.Builder( fieldName, store, index, termVector )
				.boost( spatialAnnotation.boost().value() )
				.fieldBridge( spatialBridge )
				.build();

		typeMetadataBuilder.addClassBridgeField( fieldMetadata );

		Analyzer analyzer = typeMetadataBuilder.getAnalyzer();
		if ( analyzer == null ) {
			throw new AssertionFailure( "Analyzer should not be undefined" );
		}
	}

	private FieldBridge determineSpatialFieldBridge(Spatial spatialAnnotation, ParseContext parseContext) {
		final FieldBridge spatialBridge;
		XClass clazz = parseContext.getCurrentClass();
		if ( reflectionManager.toXClass( Coordinates.class ).isAssignableFrom( clazz ) ) {
			spatialBridge = BridgeFactory.buildSpatialBridge( spatialAnnotation, clazz, null, null );
		}
		else {
			String latitudeField = null;
			String longitudeField = null;

			List<XProperty> fieldList = clazz.getDeclaredProperties( XClass.ACCESS_FIELD );

			for ( XProperty property : fieldList ) {
				if ( property.isAnnotationPresent( Latitude.class ) && ( property.getAnnotation( Latitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( latitudeField != null ) {
						throw log.ambiguousLatitudeDefinition( clazz.getName(), latitudeField, property.getName() );
					}
					latitudeField = property.getName();
				}
				if ( property.isAnnotationPresent( Longitude.class ) && ( property.getAnnotation( Longitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( longitudeField != null ) {
						throw log.ambiguousLongitudeDefinition(
								clazz.getName(),
								longitudeField,
								property.getName()
						);
					}
					longitudeField = property.getName();
				}
			}

			List<XProperty> propertyList = clazz.getDeclaredProperties( XClass.ACCESS_PROPERTY );

			for ( XProperty property : propertyList ) {
				if ( property.isAnnotationPresent( Latitude.class ) && ( property.getAnnotation( Latitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( latitudeField != null ) {
						throw log.ambiguousLatitudeDefinition( clazz.getName(), latitudeField, property.getName() );
					}
					latitudeField = property.getName();
				}
				if ( property.isAnnotationPresent( Longitude.class ) && ( property.getAnnotation( Longitude.class ) ).of()
						.equals( spatialAnnotation.name() ) ) {
					if ( longitudeField != null ) {
						throw log.ambiguousLongitudeDefinition(
								clazz.getName(),
								longitudeField,
								property.getName()
						);
					}
					longitudeField = property.getName();
				}
			}

			if ( latitudeField != null && longitudeField != null ) {
				spatialBridge = BridgeFactory.buildSpatialBridge(
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
							.isEmpty() ? "default" : spatialAnnotation.name(), clazz.getName()
			);
		}
		return spatialBridge;
	}

	private void initializeMemberLevelAnnotations(String prefix,
			XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			boolean disableOptimizations,
			boolean isRoot,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {
		checkDocumentId( member, typeMetadataBuilder, isRoot, prefix, configContext, pathsContext, parseContext );
		checkForField( member, typeMetadataBuilder, prefix, configContext, pathsContext, parseContext );
		checkForFields( member, typeMetadataBuilder, prefix, configContext, pathsContext, parseContext );
		checkForSpatial( member, typeMetadataBuilder, prefix, pathsContext, parseContext );
		checkForSpatialsAnnotation( member, typeMetadataBuilder, prefix, pathsContext, parseContext );
		checkForAnalyzerDefs( member, configContext );
		checkForAnalyzerDiscriminator( member, typeMetadataBuilder, configContext );
		checkForIndexedEmbedded(
				member,
				prefix,
				disableOptimizations,
				typeMetadataBuilder,
				configContext,
				pathsContext,
				parseContext
		);
		checkForContainedIn( member, typeMetadataBuilder, parseContext );

	}

	private void checkForContainedIn(XProperty member, TypeMetadata.Builder typeMetadataBuilder, ParseContext parseContext) {
		ContainedIn containedInAnnotation = member.getAnnotation( ContainedIn.class );
		if ( containedInAnnotation == null ) {
			return;
		}

		ContainedInMetadataBuilder containedInMetadataBuilder = new ContainedInMetadataBuilder( member );
		updateContainedInMaxDepths( containedInMetadataBuilder, member );
		typeMetadataBuilder.addContainedIn( containedInMetadataBuilder.createContainedInMetadata() );

		parseContext.collectUnqualifiedCollectionRole( member.getName() );
	}

	private void updateContainedInMaxDepths(ContainedInMetadataBuilder containedInMetadataBuilder, XProperty member) {
		updateContainedInMaxDepth( containedInMetadataBuilder, member, XClass.ACCESS_FIELD );
		updateContainedInMaxDepth( containedInMetadataBuilder, member, XClass.ACCESS_PROPERTY );
	}

	private void updateContainedInMaxDepth(ContainedInMetadataBuilder containedInMetadataBuilder, XMember memberWithContainedIn, String accessType) {
		XClass memberReturnedType = memberWithContainedIn.getElementClass();
		String mappedBy = mappedBy( memberWithContainedIn );
		List<XProperty> returnedTypeProperties = memberReturnedType.getDeclaredProperties( accessType );
		for ( XProperty property : returnedTypeProperties ) {
			if ( isCorrespondingIndexedEmbedded( mappedBy, property ) ) {
				updateDepthProperties( containedInMetadataBuilder, property );
				break;
			}
		}
	}

	private boolean isCorrespondingIndexedEmbedded(String mappedBy, XProperty property) {
		if ( !property.isAnnotationPresent( IndexedEmbedded.class ) ) {
			return false;
		}
		if ( mappedBy.isEmpty() ) {
			return true;
		}
		if ( mappedBy.equals( property.getName() ) ) {
			return true;
		}
		return false;
	}

	private void updateDepthProperties(ContainedInMetadataBuilder containedInMetadataBuilder, XProperty property) {
		containedInMetadataBuilder.maxDepth( property.getAnnotation( IndexedEmbedded.class ).depth() );
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

	private void checkForField(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
			String prefix,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {

		org.hibernate.search.annotations.Field fieldAnnotation =
				member.getAnnotation( org.hibernate.search.annotations.Field.class );
		NumericField numericFieldAnnotation = member.getAnnotation( NumericField.class );
		DocumentId idAnn = member.getAnnotation( DocumentId.class );
		if ( fieldAnnotation != null ) {
			if ( isFieldInPath( fieldAnnotation, member, pathsContext, prefix ) || !parseContext.isMaxLevelReached() ) {
				PropertyMetadata.Builder propertyMetadataBuilder = new PropertyMetadata.Builder( member )
						.dynamicBoostStrategy( AnnotationProcessingHelper.getDynamicBoost( member ) );

				bindFieldAnnotation(
						prefix,
						fieldAnnotation,
						numericFieldAnnotation,
						typeMetadataBuilder,
						propertyMetadataBuilder,
						configContext,
						parseContext
				);

				typeMetadataBuilder.addProperty( propertyMetadataBuilder.build() );
			}
		}
		if ( ( fieldAnnotation == null && idAnn == null ) && numericFieldAnnotation != null ) {
			throw new SearchException( "@NumericField without a @Field on property '" + member.getName() + "'" );
		}
	}

	private void bindFieldAnnotation(
			String prefix,
			org.hibernate.search.annotations.Field fieldAnnotation,
			NumericField numericFieldAnnotation,
			TypeMetadata.Builder typeMetadataBuilder,
			PropertyMetadata.Builder propertyMetadataBuilder,
			ConfigContext configContext,
			ParseContext parseContext) {

		XProperty member = propertyMetadataBuilder.getPropertyAccessor();

		if ( isPropertyTransient( member, configContext ) ) {
			//If the indexed values are derived from a Transient field, we can't rely on dirtyness of properties.
			//Only applies on JPA mapped entities.
			typeMetadataBuilder.disableStateInspectionOptimization();
		}

		String fieldName = prefix + ReflectionHelper.getAttributeName( member, fieldAnnotation.name() );
		Store store = fieldAnnotation.store();
		Field.Index index = AnnotationProcessingHelper.getIndex(
				fieldAnnotation.index(),
				fieldAnnotation.analyze(),
				fieldAnnotation.norms()
		);
		Field.TermVector termVector = AnnotationProcessingHelper.getTermVector( fieldAnnotation.termVector() );

		FieldBridge fieldBridge = BridgeFactory.guessType(
				fieldAnnotation,
				numericFieldAnnotation,
				member,
				reflectionManager
		);

		String nullToken = determineNullToken( fieldAnnotation, configContext );
		if ( nullToken != null && fieldBridge instanceof TwoWayFieldBridge ) {
			fieldBridge = new NullEncodingTwoWayFieldBridge( (TwoWayFieldBridge) fieldBridge, nullToken );
		}
		Analyzer analyzer = determineAnalyzer( fieldAnnotation, member, configContext );

		// adjust the type analyzer
		analyzer = typeMetadataBuilder.addToScopedAnalyzer(
				fieldName,
				analyzer,
				index
		);

		DocumentFieldMetadata.Builder fieldMetadataBuilder = new DocumentFieldMetadata.Builder(
				fieldName,
				store,
				index,
				termVector
		)
				.boost( AnnotationProcessingHelper.getBoost( member, fieldAnnotation ) )
				.fieldBridge( fieldBridge )
				.analyzer( analyzer )
				.indexNullAs( nullToken );

		if ( numericFieldAnnotation != null ) {
			fieldMetadataBuilder.numeric()
					.precisionStep( AnnotationProcessingHelper.getPrecisionStep( numericFieldAnnotation ) );
		}

		DocumentFieldMetadata fieldMetadata = fieldMetadataBuilder.build();
		propertyMetadataBuilder.addDocumentField( fieldMetadata );


		// keep track of collection role names for ORM integration optimization based on collection update events
		parseContext.collectUnqualifiedCollectionRole( member.getName() );
	}

	private String determineNullToken(org.hibernate.search.annotations.Field fieldAnnotation, ConfigContext context) {
		if ( fieldAnnotation == null ) {
			return null;
		}

		String indexNullAs = fieldAnnotation.indexNullAs();
		if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DO_NOT_INDEX_NULL ) ) {
			indexNullAs = null;
		}
		else if ( indexNullAs.equals( org.hibernate.search.annotations.Field.DEFAULT_NULL_TOKEN ) ) {
			indexNullAs = context.getDefaultNullToken();
		}
		return indexNullAs;
	}

	private Analyzer determineAnalyzer(org.hibernate.search.annotations.Field fieldAnnotation,
			XProperty member,
			ConfigContext context) {
		Analyzer analyzer = null;
		// check for a nested @Analyzer annotation with @Field
		if ( fieldAnnotation != null ) {
			analyzer = AnnotationProcessingHelper.getAnalyzer( fieldAnnotation.analyzer(), context );
		}

		// if there was no analyzer specified as part of @Field, try a stand alone @Analyzer annotation
		if ( analyzer == null ) {
			analyzer = AnnotationProcessingHelper.getAnalyzer(
					member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
					context
			);
		}

		return analyzer;
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
								ConfigContext.class.getClassLoader()
						);
				transientAnnotation = member.getAnnotation( transientAnnotationClass );
			}
			catch (ClassNotFoundException e) {
				throw new SearchException( "Unable to load @Transient.class even though it should be present ?!" );
			}
			return transientAnnotation != null;
		}
	}


	private void checkForSpatialsAnnotation(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
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
							parseContext
					);
				}
			}
		}
	}

	private void checkForSpatial(XProperty member,
			TypeMetadata.Builder typeMetadataBuilder,
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
				bindSpatialAnnotation( spatialAnnotation, prefix, member, typeMetadataBuilder, parseContext );
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
			String prefix,
			ConfigContext configContext,
			PathsContext pathsContext,
			ParseContext parseContext) {
		org.hibernate.search.annotations.Fields fieldsAnnotation = member.getAnnotation( org.hibernate.search.annotations.Fields.class );
		NumericFields numericFieldsAnnotations = member.getAnnotation( NumericFields.class );
		if ( fieldsAnnotation != null ) {
			for ( org.hibernate.search.annotations.Field fieldAnnotation : fieldsAnnotation.value() ) {
				if ( isFieldInPath(
						fieldAnnotation,
						member,
						pathsContext,
						prefix
				) || !parseContext.isMaxLevelReached() ) {
					PropertyMetadata.Builder propertyMetadataBuilder = new PropertyMetadata.Builder( member )
							.dynamicBoostStrategy( AnnotationProcessingHelper.getDynamicBoost( member ) );

					bindFieldAnnotation(
							prefix,
							fieldAnnotation,
							getNumericExtension( fieldAnnotation, numericFieldsAnnotations ),
							typeMetadataBuilder,
							propertyMetadataBuilder,
							configContext,
							parseContext
					);

					typeMetadataBuilder.addProperty( propertyMetadataBuilder.build() );
				}
			}
		}
	}

	private NumericField getNumericExtension(org.hibernate.search.annotations.Field fieldAnnotation, NumericFields numericFields) {
		if ( numericFields == null ) {
			return null;
		}
		for ( NumericField numericField : numericFields.value() ) {
			if ( numericField.forField().equals( fieldAnnotation.name() ) ) {
				return numericField;
			}
		}
		return null;
	}

	private boolean isFieldInPath(Annotation fieldAnnotation,
			XProperty member,
			PathsContext pathsContext,
			String prefix) {
		if ( pathsContext != null ) {
			String path = prefix + fieldName( fieldAnnotation, member );
			if ( pathsContext.containsPath( path ) ) {
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
		int potentialLevel = depth( indexedEmbeddedAnnotation ) + parseContext.getLevel();
		// This is really catching a possible int overflow. depth() can return Integer.MAX_VALUE, which then can
		// overflow in case level > 0. Really this code should be rewritten (HF)
		if ( potentialLevel < 0 ) {
			potentialLevel = Integer.MAX_VALUE;
		}
		// HSEARCH-1442 recreating the behavior prior to PropertiesMetadata refactoring
		// not sure whether this is algorithmically correct though. @IndexedEmbedded processing should be refactored (HF)
		if ( potentialLevel < oldMaxLevel ) {
			parseContext.setMaxLevel( potentialLevel );
		}
		parseContext.incrementLevel();

		XClass elementClass;
		if ( void.class == indexedEmbeddedAnnotation.targetElement() ) {
			elementClass = member.getElementClass();
		}
		else {
			elementClass = reflectionManager.toXClass( indexedEmbeddedAnnotation.targetElement() );
		}

		if ( parseContext.getMaxLevel() == Integer.MAX_VALUE //infinite
				&& parseContext.hasBeenProcessed( elementClass ) ) {
			throw new SearchException(
					"Circular reference. Duplicate use of "
							+ elementClass.getName()
							+ " in root entity " + typeMetadataBuilder.getClass().getName()
							+ "#" + buildEmbeddedPrefix( prefix, indexedEmbeddedAnnotation, member )
			);
		}

		String localPrefix = buildEmbeddedPrefix( prefix, indexedEmbeddedAnnotation, member );
		PathsContext updatedPathsContext = updatePaths( localPrefix, pathsContext, indexedEmbeddedAnnotation );

		boolean pathsCreatedAtThisLevel = false;
		if ( pathsContext == null && updatedPathsContext != null ) {
			//after this level if not all paths are traversed, then the paths
			//either don't exist in the object graph, or aren't indexed paths
			pathsCreatedAtThisLevel = true;
		}

		if ( !parseContext.isMaxLevelReached() || isInPath(
				localPrefix,
				updatedPathsContext,
				indexedEmbeddedAnnotation
		) ) {
			parseContext.processingClass( elementClass ); //push

			EmbeddedTypeMetadata.Builder embeddedTypeMetadataBuilder =
					new EmbeddedTypeMetadata.Builder(
							reflectionManager.toClass( elementClass ),
							member,
							configContext,
							typeMetadataBuilder.getScopedAnalyzer()
					);

			embeddedTypeMetadataBuilder.boost( AnnotationProcessingHelper.getBoost( member, null ) );
			//property > entity analyzer
			Analyzer analyzer = AnnotationProcessingHelper.
					getAnalyzer(
							member.getAnnotation( org.hibernate.search.annotations.Analyzer.class ),
							configContext
					);
			if ( analyzer == null ) {
				analyzer = typeMetadataBuilder.getAnalyzer();
			}
			embeddedTypeMetadataBuilder.analyzer( analyzer );

			if ( disableOptimizations ) {
				typeMetadataBuilder.blacklistForOptimization( elementClass );
			}

			XClass previousClass = parseContext.getCurrentClass();
			parseContext.setCurrentClass( elementClass );
			initializeClass(
					embeddedTypeMetadataBuilder,
					false,
					localPrefix,
					parseContext,
					configContext,
					disableOptimizations,
					updatedPathsContext
			);
			parseContext.setCurrentClass( previousClass );

			final String indexNullAs = embeddedNullToken( configContext, indexedEmbeddedAnnotation );
			if ( indexNullAs != null ) {
				EmbeddedTypeMetadata.Container container = embeddedTypeMetadataBuilder.getEmbeddedContainerType();
				FieldBridge fieldBridge = guessNullEmbeddedBridge( member, container, indexNullAs );
				embeddedTypeMetadataBuilder.indexNullToken(
						indexNullAs,
						embeddedNullField( localPrefix ),
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
			log.tracef( "depth reached, ignoring %s", localPrefix );
		}

		parseContext.decrementLevel();
		parseContext.setMaxLevel( oldMaxLevel ); //set back the the old max level

		if ( pathsCreatedAtThisLevel ) {
			validateAllPathsEncountered( member, updatedPathsContext );
		}
	}

	private int depth(IndexedEmbedded embeddedAnn) {
		if ( isDepthNotSet( embeddedAnn ) && embeddedAnn.includePaths().length > 0 ) {
			return 0;
		}
		return embeddedAnn.depth();
	}

	private boolean isDepthNotSet(IndexedEmbedded embeddedAnn) {
		return Integer.MAX_VALUE == embeddedAnn.depth();
	}

	private boolean isInPath(String localPrefix, PathsContext pathsContext, IndexedEmbedded embeddedAnn) {
		if ( pathsContext != null ) {
			boolean defaultPrefix = isDefaultPrefix( embeddedAnn );
			for ( String path : pathsContext.pathsEncounteredState.keySet() ) {
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

	private PathsContext updatePaths(String localPrefix, PathsContext pathsContext, IndexedEmbedded embeddedAnn) {
		if ( pathsContext != null ) {
			return pathsContext;
		}
		PathsContext newPathsContext = new PathsContext( embeddedAnn );
		for ( String path : embeddedAnn.includePaths() ) {
			newPathsContext.addPath( localPrefix + path );
		}
		return newPathsContext;
	}

	private String buildEmbeddedPrefix(String prefix, IndexedEmbedded indexedEmbeddedAnnotation, XProperty member) {
		String localPrefix = prefix;
		if ( isDefaultPrefix( indexedEmbeddedAnnotation ) ) {
			//default to property name
			localPrefix += member.getName() + '.';
		}
		else {
			localPrefix += indexedEmbeddedAnnotation.prefix();
		}
		return localPrefix;
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

	private FieldBridge guessNullEmbeddedBridge(XProperty member,
			EmbeddedTypeMetadata.Container container,
			final String indexNullAs) {
		if ( indexNullAs == null ) {
			return null;
		}

		if ( EmbeddedTypeMetadata.Container.OBJECT == container ) {
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

	private void validateAllPathsEncountered(XProperty member, PathsContext updatedPathsContext) {
		Set<String> unEncounteredPaths = updatedPathsContext.getUnencounteredPaths();
		if ( unEncounteredPaths.size() > 0 ) {
			StringBuilder sb = new StringBuilder( "Found invalid @IndexedEmbedded->paths configured on class " );
			sb.append( member.getType().getName() );
			sb.append( ", member " );
			sb.append( member.getName() );
			sb.append( ": " );

			String prefix = updatedPathsContext.embeddedAnn.prefix();
			for ( String path : unEncounteredPaths ) {
				sb.append( removeLeadingPrefixFromPath( path, prefix ) );
				sb.append( ',' );
			}
			String message = sb.substring( 0, sb.length() - 1 );
			throw new SearchException( message );
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

	public static final class ParseContext {
		private final Set<XClass> processedClasses = new HashSet<XClass>();
		private final Set<String> spatialNames = new TreeSet<String>();

		private XClass currentClass;
		private int level = 0;
		private int maxLevel = Integer.MAX_VALUE;
		private boolean explicitDocumentId = false;
		private final Set<String> unqualifiedCollectedCollectionRoles = new HashSet<String>();

		boolean hasBeenProcessed(XClass processedClass) {
			return processedClasses.contains( processedClass );
		}

		void processingClass(XClass processedClass) {
			processedClasses.add( processedClass );
		}

		void removeProcessedClass(XClass processedClass) {
			processedClasses.remove( processedClass );
		}

		boolean isSpatialNameUsed(String name) {
			return spatialNames.contains( name );
		}

		void markSpatialNameAsUsed(String name) {
			spatialNames.add( name );
		}

		public XClass getCurrentClass() {
			return currentClass;
		}

		public void setCurrentClass(XClass currentClass) {
			this.currentClass = currentClass;
		}

		boolean isMaxLevelReached() {
			return level > maxLevel;
		}

		public int getMaxLevel() {
			return maxLevel;
		}

		public void setMaxLevel(int newMaxLevel) {
			this.maxLevel = newMaxLevel;
		}

		public int getLevel() {
			return level;
		}

		public void incrementLevel() {
			this.level++;
		}

		public void decrementLevel() {
			this.level--;
		}

		public boolean isExplicitDocumentId() {
			return explicitDocumentId;
		}

		public void setExplicitDocumentId(boolean explicitDocumentId) {
			this.explicitDocumentId = explicitDocumentId;
		}

		public Set<String> getCollectedUnqualifiedCollectionRoles() {
			return unqualifiedCollectedCollectionRoles;
		}

		public void collectUnqualifiedCollectionRole(String unqualifiedCollectionRole) {
			unqualifiedCollectedCollectionRoles.add( unqualifiedCollectionRole );
		}
	}
}
