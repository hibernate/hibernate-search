/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.ReflectionUtil;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.AnalyzerDefs;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Facets;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.NormalizerDefs;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.SortableFields;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.Spatials;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.cfg.EntityDescriptor;
import org.hibernate.search.cfg.PropertyDescriptor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class MappingModelMetadataProvider implements MetadataProvider {

	private static final Log LOG = LoggerFactory.make();

	private static final Filter FILTER = new Filter() {
		@Override
		public boolean returnStatic() {
			return false;
		}

		@Override
		public boolean returnTransient() {
			return true;
		}
	};

	private final MetadataProvider delegate;
	private final SearchMapping mapping;
	private final Map<AnnotatedElement, AnnotationReader> cache = new HashMap<AnnotatedElement, AnnotationReader>( 100 );
	private Map<Object, Object> defaults;

	public MappingModelMetadataProvider(MetadataProvider delegate, SearchMapping mapping) {
		this.delegate = delegate;
		this.mapping = mapping;
	}

	@Override
	public Map<Object, Object> getDefaults() {
		if ( defaults == null ) {
			final Map<Object, Object> delegateDefaults = delegate.getDefaults();
			defaults = delegateDefaults == null ?
					new HashMap<Object, Object>() :
					new HashMap<Object, Object>( delegateDefaults );
			defaults.put( AnalyzerDefs.class, createAnalyzerDefArray() );
			defaults.put( NormalizerDefs.class, createNormalizerDefArray() );
			if ( !mapping.getFullTextFilterDefs().isEmpty() ) {
				defaults.put( FullTextFilterDefs.class, createFullTextFilterDefsForMapping() );
			}
		}
		return defaults;
	}

	@Override
	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		AnnotationReader reader = cache.get( annotatedElement );
		if ( reader == null ) {
			reader = new MappingModelAnnotationReader( mapping, delegate, annotatedElement );
			cache.put( annotatedElement, reader );
		}
		return reader;
	}

	private AnalyzerDef[] createAnalyzerDefArray() {
		List<String> globalAnalyzerDefNames = new ArrayList<String>();
		AnalyzerDef[] defs = new AnalyzerDef[mapping.getAnalyzerDefs().size()];
		int index = 0;
		for ( Map<String, Object> analyzerDef : mapping.getAnalyzerDefs() ) {
			AnalyzerDef def = createAnalyzerDef( analyzerDef );
			if ( globalAnalyzerDefNames.contains( def.name() ) ) {
				throw LOG.analyzerDefinitionNamingConflict( def.name() );
			}
			globalAnalyzerDefNames.add( def.name() );
			defs[index] = def;
			index++;
		}
		return defs;
	}

	private NormalizerDef[] createNormalizerDefArray() {
		List<String> globalNormalizerDefNames = new ArrayList<String>();
		NormalizerDef[] defs = new NormalizerDef[mapping.getNormalizerDefs().size()];
		int index = 0;
		for ( Map<String, Object> normalizerDef : mapping.getNormalizerDefs() ) {
			NormalizerDef def = createNormalizerDef( normalizerDef );
			if ( globalNormalizerDefNames.contains( def.name() ) ) {
				throw LOG.normalizerDefinitionNamingConflict( def.name() );
			}
			globalNormalizerDefNames.add( def.name() );
			defs[index] = def;
			index++;
		}
		return defs;
	}

	private FullTextFilterDef[] createFullTextFilterDefsForMapping() {
		Set<Map<String, Object>> fullTextFilterDefs = mapping.getFullTextFilterDefs();
		FullTextFilterDef[] filters = new FullTextFilterDef[fullTextFilterDefs.size()];
		int index = 0;
		for ( Map<String, Object> filterDef : fullTextFilterDefs ) {
			filters[index] = createFullTextFilterDef( filterDef );
			index++;
		}
		return filters;
	}

	private static FullTextFilterDef createFullTextFilterDef(Map<String, Object> filterDef) {
		AnnotationDescriptor fullTextFilterDefAnnotation = new AnnotationDescriptor( FullTextFilterDef.class );
		for ( Entry<String, Object> entry : filterDef.entrySet() ) {
			fullTextFilterDefAnnotation.setValue( entry.getKey(), entry.getValue() );
		}

		return (FullTextFilterDef) createAnnotation( fullTextFilterDefAnnotation );
	}

	private static FullTextFilterDef[] createFullTextFilterDefArray(Set<Map<String, Object>> fullTextFilterDefs) {
		FullTextFilterDef[] filters = new FullTextFilterDef[fullTextFilterDefs.size()];
		int index = 0;
		for ( Map<String, Object> filterDef : fullTextFilterDefs ) {
			filters[index] = createFullTextFilterDef( filterDef );
			index++;
		}
		return filters;
	}

	private AnalyzerDef createAnalyzerDef(Map<String, Object> analyzerDef) {
		AnnotationDescriptor analyzerDefAnnotation = new AnnotationDescriptor( AnalyzerDef.class );
		for ( Map.Entry<String, Object> entry : analyzerDef.entrySet() ) {
			if ( "tokenizer".equals( entry.getKey() ) ) {
				AnnotationDescriptor tokenizerAnnotation = new AnnotationDescriptor( TokenizerDef.class );
				@SuppressWarnings("unchecked")
				Map<String, Object> tokenizer = (Map<String, Object>) entry.getValue();
				for ( Map.Entry<String, Object> tokenizerEntry : tokenizer.entrySet() ) {
					if ( "params".equals( tokenizerEntry.getKey() ) ) {
						addParamsToAnnotation( tokenizerAnnotation, tokenizerEntry );
					}
					else {
						tokenizerAnnotation.setValue( tokenizerEntry.getKey(), tokenizerEntry.getValue() );
					}
				}
				analyzerDefAnnotation.setValue( "tokenizer", createAnnotation( tokenizerAnnotation ) );
			}
			else if ( "filters".equals( entry.getKey() ) ) {
				@SuppressWarnings("unchecked") TokenFilterDef[] filtersArray = createFilters(
						(List<Map<String, Object>>) entry.getValue()
				);
				analyzerDefAnnotation.setValue( "filters", filtersArray );
			}
			else if ( "charFilters".equals( entry.getKey() ) ) {
				@SuppressWarnings("unchecked") CharFilterDef[] charFiltersArray = createCharFilters(
						(List<Map<String, Object>>) entry.getValue()
				);
				analyzerDefAnnotation.setValue( "charFilters", charFiltersArray );
			}
			else {
				analyzerDefAnnotation.setValue( entry.getKey(), entry.getValue() );
			}
		}
		return (AnalyzerDef) createAnnotation( analyzerDefAnnotation );
	}

	private NormalizerDef createNormalizerDef(Map<String, Object> analyzerDef) {
		AnnotationDescriptor normalizerDefAnnotation = new AnnotationDescriptor( NormalizerDef.class );
		for ( Map.Entry<String, Object> entry : analyzerDef.entrySet() ) {
			if ( "filters".equals( entry.getKey() ) ) {
				@SuppressWarnings("unchecked") TokenFilterDef[] filtersArray = createFilters(
						(List<Map<String, Object>>) entry.getValue()
				);
				normalizerDefAnnotation.setValue( "filters", filtersArray );
			}
			else if ( "charFilters".equals( entry.getKey() ) ) {
				@SuppressWarnings("unchecked") CharFilterDef[] charFiltersArray = createCharFilters(
						(List<Map<String, Object>>) entry.getValue()
				);
				normalizerDefAnnotation.setValue( "charFilters", charFiltersArray );
			}
			else {
				normalizerDefAnnotation.setValue( entry.getKey(), entry.getValue() );
			}
		}
		return (NormalizerDef) createAnnotation( normalizerDefAnnotation );
	}

	private static void addParamsToAnnotation(AnnotationDescriptor annotationDescriptor, Map.Entry<String, Object> entry) {
		@SuppressWarnings("unchecked") Parameter[] paramsArray = createParams( (List<Map<String, Object>>) entry.getValue() );
		annotationDescriptor.setValue( "params", paramsArray );
	}

	private TokenFilterDef[] createFilters(List<Map<String, Object>> filters) {
		TokenFilterDef[] filtersArray = new TokenFilterDef[filters.size()];
		int index = 0;
		for ( Map<String, Object> filter : filters ) {
			AnnotationDescriptor filterAnn = new AnnotationDescriptor( TokenFilterDef.class );
			for ( Map.Entry<String, Object> filterEntry : filter.entrySet() ) {
				if ( "params".equals( filterEntry.getKey() ) ) {
					addParamsToAnnotation( filterAnn, filterEntry );
				}
				else {
					filterAnn.setValue( filterEntry.getKey(), filterEntry.getValue() );
				}
			}
			filtersArray[index] = (TokenFilterDef) createAnnotation( filterAnn );
			index++;
		}
		return filtersArray;
	}

	private CharFilterDef[] createCharFilters(List<Map<String, Object>> charFilters) {
		CharFilterDef[] charFiltersArray = new CharFilterDef[charFilters.size()];
		int index = 0;
		for ( Map<String, Object> charFilter : charFilters ) {
			AnnotationDescriptor charFilterAnn = new AnnotationDescriptor( CharFilterDef.class );
			for ( Map.Entry<String, Object> charFilterEntry : charFilter.entrySet() ) {
				if ( "params".equals( charFilterEntry.getKey() ) ) {
					addParamsToAnnotation( charFilterAnn, charFilterEntry );
				}
				else {
					charFilterAnn.setValue( charFilterEntry.getKey(), charFilterEntry.getValue() );
				}
			}
			charFiltersArray[index] = (CharFilterDef) createAnnotation( charFilterAnn );
			index++;
		}
		return charFiltersArray;
	}

	private static Parameter[] createParams(List<Map<String, Object>> params) {
		Parameter[] paramArray = new Parameter[params.size()];
		int index = 0;
		for ( Map<String, Object> entry : params ) {
			AnnotationDescriptor paramAnnotation = new AnnotationDescriptor( Parameter.class );
			paramAnnotation.setValue( "name", entry.get( "name" ) );
			paramAnnotation.setValue( "value", entry.get( "value" ) );
			paramArray[index] = (Parameter) createAnnotation( paramAnnotation );
			index++;
		}
		return paramArray;
	}

	/**
	 * Creates the proxy for an annotation using Hibernate Commons Annotations
	 *
	 * @param annotation the AnnotationDescriptor
	 *
	 * @return the proxy
	 */
	private static Annotation createAnnotation(AnnotationDescriptor annotation) {
		//The return type of this method could be "<T extends Annotation> T"
		//but that would fail to compile on some JVMs: see HSEARCH-1106.

		//This is a filthy workaround for the Annotations proxy generation,
		//which is using the ContextClassLoader to define the proxy classes
		//(not working fine in modular environments when Search is used by
		//other services such as CapeDwarf).
		//See HSEARCH-1084

		//use annotation's own classloader
		try {
			return AnnotationFactory.create( annotation, annotation.type().getClassLoader() );
		}
		catch (Exception e) {
			//first try, but we have another trick
		}
		//Use TCCL
		return org.hibernate.annotations.common.annotationfactory.AnnotationFactory.create( annotation );
	}

	private static class MappingModelAnnotationReader implements AnnotationReader {
		private final AnnotationReader delegate;
		private final SearchMapping mapping;
		private transient Annotation[] annotationsArray;
		private transient Map<Class<? extends Annotation>, Annotation> annotations;
		private Class<?> entityType;
		private ElementType elementType;
		private String propertyName;

		public MappingModelAnnotationReader(SearchMapping mapping, MetadataProvider delegate, AnnotatedElement el) {
			this.delegate = delegate.getAnnotationReader( el );
			this.mapping = mapping;
			if ( el instanceof Class ) {
				entityType = (Class<?>) el;
			}
			else if ( el instanceof Field ) {
				Field field = (Field) el;
				entityType = field.getDeclaringClass();
				propertyName = field.getName();
				elementType = ElementType.FIELD;
			}
			else if ( el instanceof Method ) {
				Method method = (Method) el;
				entityType = method.getDeclaringClass();
				propertyName = method.getName();
				if ( ReflectionUtil.isProperty(
						method,
						null, //this is yukky!! we'd rather get the TypeEnvironment()
						FILTER
				) ) {
					if ( propertyName.startsWith( "get" ) ) {
						propertyName = Introspector.decapitalize( propertyName.substring( "get".length() ) );
					}
					else if ( propertyName.startsWith( "is" ) ) {
						propertyName = Introspector.decapitalize( propertyName.substring( "is".length() ) );
					}
					else {
						throw new RuntimeException( "Method " + propertyName + " is not a property getter" );
					}
					elementType = ElementType.METHOD;
				}
				else {
					//this is a non getter method, so let it go and delegate
					entityType = null;
					propertyName = null;
				}
			}
			else {
				//this is a non supported element, so let it go and delegate
				entityType = null;
				propertyName = null;
			}
		}

		/**
		 * Consider the class to be free of Hibernate Search annotations. Does nto attempt to merge
		 * data.
		 * TODO do merge data? or safe-guard against errors
		 */
		private void initAnnotations() {
			if ( annotationsArray == null ) {
				annotations = new HashMap<Class<? extends Annotation>, Annotation>();
				delegatesAnnotationReading();
				if ( entityType != null ) {
					final EntityDescriptor entity = mapping.getEntityDescriptor( entityType );
					if ( entity != null ) {
						if ( propertyName == null ) {
							//entityType overriding
							createIndexed( entity );
						}
						else {
							final PropertyDescriptor property = entity.getPropertyDescriptor(
									propertyName, elementType
							);
							if ( property != null ) {
								// property name overriding
								createDocumentId( property );
								createAnalyzerDiscriminator( property );
								createFields( property );
								createIndexEmbedded( property );
								createContainedIn( property );

							}
						}
					}
				}
				else {
					delegatesAnnotationReading();
				}

				populateAnnotationArray();
			}
		}


		private void createDateBridge(PropertyDescriptor property) {
			Map<String, Object> map = property.getDateBridge();
			for ( Map.Entry<String, Object> entry : map.entrySet() ) {
				AnnotationDescriptor dateBridgeAnnotation = new AnnotationDescriptor( DateBridge.class );
				dateBridgeAnnotation.setValue( entry.getKey(), entry.getValue() );
				annotations.put( DateBridge.class, createAnnotation( dateBridgeAnnotation ) );
			}
		}

		private void createCalendarBridge(PropertyDescriptor property) {
			Map<String, Object> map = property.getCalendarBridge();
			for ( Map.Entry<String, Object> entry : map.entrySet() ) {
				AnnotationDescriptor calendarBrigeAnnotation = new AnnotationDescriptor( CalendarBridge.class );
				calendarBrigeAnnotation.setValue( entry.getKey(), entry.getValue() );
				annotations.put( CalendarBridge.class, createAnnotation( calendarBrigeAnnotation ) );
			}
		}

		private void createDocumentId(PropertyDescriptor property) {
			Map<String, Object> documentId = property.getDocumentId();
			if ( documentId != null ) {
				AnnotationDescriptor documentIdAnnotation = new AnnotationDescriptor( DocumentId.class );
				for ( Map.Entry<String, Object> entry : documentId.entrySet() ) {
					documentIdAnnotation.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( DocumentId.class, createAnnotation( documentIdAnnotation ) );
			}
		}

		private void createAnalyzerDiscriminator(PropertyDescriptor property) {
			Map<String, Object> analyzerDiscriminator = property.getAnalyzerDiscriminator();
			if ( analyzerDiscriminator != null ) {
				AnnotationDescriptor analyzerDiscriminatorAnn = new AnnotationDescriptor( AnalyzerDiscriminator.class );
				for ( Map.Entry<String, Object> entry : analyzerDiscriminator.entrySet() ) {
					analyzerDiscriminatorAnn.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( AnalyzerDiscriminator.class, createAnnotation( analyzerDiscriminatorAnn ) );
			}
		}


		private void createFields(PropertyDescriptor property) {
			final Collection<Map<String, Object>> fields = property.getFields();
			final Map<String, Object> spatial = property.getSpatial();
			final Map<String, Object> latitude = property.getLatitude();
			final Map<String, Object> longitude = property.getLongitude();
			final Collection<Map<String, Object>> numericFields = property.getNumericFields();
			final Collection<Map<String, Object>> sortableFields = property.getSortableFields();
			final Collection<Map<String, Object>> facets = property.getFacets();

			List<org.hibernate.search.annotations.Field> fieldAnnotations =
					new ArrayList<org.hibernate.search.annotations.Field>( fields.size() );

			List<NumericField> numericFieldAnnotations = new ArrayList<NumericField>( numericFields.size() );
			for ( Map<String, Object> numericField : numericFields ) {
				AnnotationDescriptor fieldAnnotation = new AnnotationDescriptor( NumericField.class );
				for ( Map.Entry<String, Object> entry : numericField.entrySet() ) {
					fieldAnnotation.setValue( entry.getKey(), entry.getValue() );
				}
				numericFieldAnnotations.add( (NumericField) createAnnotation( fieldAnnotation ) );
			}

			List<SortableField> sortableFieldAnnotations = new ArrayList<SortableField>( sortableFields.size() );
			for ( Map<String, Object> sortableField : sortableFields ) {
				AnnotationDescriptor sortableFieldAnnotation = new AnnotationDescriptor( SortableField.class );
				for ( Map.Entry<String, Object> entry : sortableField.entrySet() ) {
					sortableFieldAnnotation.setValue( entry.getKey(), entry.getValue() );
				}
				sortableFieldAnnotations.add( (SortableField) createAnnotation( sortableFieldAnnotation ) );
			}

			List<Facet> facetAnnotations = new ArrayList<Facet>( facets.size() );
			for ( Map<String, Object> facet : facets ) {
				AnnotationDescriptor sortableFieldAnnotation = new AnnotationDescriptor( Facet.class );
				for ( Map.Entry<String, Object> entry : facet.entrySet() ) {
					sortableFieldAnnotation.setValue( entry.getKey(), entry.getValue() );
				}
				facetAnnotations.add( (Facet) createAnnotation( sortableFieldAnnotation ) );
			}

			for ( Map<String, Object> field : fields ) {
				AnnotationDescriptor fieldAnnotation = new AnnotationDescriptor( org.hibernate.search.annotations.Field.class );
				for ( Map.Entry<String, Object> entry : field.entrySet() ) {
					if ( "analyzer".equals( entry.getKey() ) ) {
						addAnalyzerAnnotationTo( fieldAnnotation, entry );
					}
					else if ( "normalizer".equals( entry.getKey() ) ) {
						addNormalizerAnnotationTo( fieldAnnotation, entry );
					}
					else if ( "boost".equals( entry.getKey() ) ) {
						AnnotationDescriptor boostAnnotation = new AnnotationDescriptor( Boost.class );
						@SuppressWarnings("unchecked")
						Map<String, Object> boost = (Map<String, Object>) entry.getValue();
						for ( Map.Entry<String, Object> boostEntry : boost.entrySet() ) {
							boostAnnotation.setValue( boostEntry.getKey(), boostEntry.getValue() );
						}
						fieldAnnotation.setValue( "boost", createAnnotation( boostAnnotation ) );
					}
					else if ( "bridge".equals( entry.getKey() ) ) {
						AnnotationDescriptor bridgeAnnotation = new AnnotationDescriptor( FieldBridge.class );
						@SuppressWarnings("unchecked")
						Map<String, Object> bridge = (Map<String, Object>) entry.getValue();
						for ( Map.Entry<String, Object> bridgeEntry : bridge.entrySet() ) {
							if ( "params".equals( bridgeEntry.getKey() ) ) {
								addParamsToAnnotation( bridgeAnnotation, bridgeEntry );
							}
							else {
								bridgeAnnotation.setValue( bridgeEntry.getKey(), bridgeEntry.getValue() );
							}
						}
						fieldAnnotation.setValue( "bridge", createAnnotation( bridgeAnnotation ) );
					}
					else {
						fieldAnnotation.setValue( entry.getKey(), entry.getValue() );
					}
				}
				fieldAnnotations.add( (org.hibernate.search.annotations.Field) createAnnotation( fieldAnnotation ) );
			}

			if ( spatial != null && spatial.size() > 0 ) {
				AnnotationDescriptor spatialAnnotation = new AnnotationDescriptor( Spatial.class );
				for ( Map.Entry<String, Object> entry : spatial.entrySet() ) {
					if ( "boost".equals( entry.getKey() ) ) {
						AnnotationDescriptor boostAnnotation = new AnnotationDescriptor( Boost.class );
						@SuppressWarnings("unchecked")
						Map<String, Object> boost = (Map<String, Object>) entry.getValue();
						for ( Map.Entry<String, Object> boostEntry : boost.entrySet() ) {
							boostAnnotation.setValue( boostEntry.getKey(), boostEntry.getValue() );
						}
						spatialAnnotation.setValue( "boost", createAnnotation( boostAnnotation ) );
					}
					else {
						spatialAnnotation.setValue( entry.getKey(), entry.getValue() );
					}
				}
				annotations.put( Spatial.class, createAnnotation( spatialAnnotation ) );
			}

			if ( latitude != null && latitude.size() > 0 ) {
				AnnotationDescriptor latitudeAnnotation = new AnnotationDescriptor( Latitude.class );
				for ( Map.Entry<String, Object> entry : latitude.entrySet() ) {
					latitudeAnnotation.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( Latitude.class, createAnnotation( latitudeAnnotation ) );
			}

			if ( longitude != null && longitude.size() > 0 ) {
				AnnotationDescriptor longitudeAnnotation = new AnnotationDescriptor( Longitude.class );
				for ( Map.Entry<String, Object> entry : longitude.entrySet() ) {
					longitudeAnnotation.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( Longitude.class, createAnnotation( longitudeAnnotation ) );
			}

			AnnotationDescriptor fieldsAnnotation = new AnnotationDescriptor( Fields.class );
			AnnotationDescriptor numericFieldsAnnotation = new AnnotationDescriptor( NumericFields.class );
			AnnotationDescriptor sortableFieldsAnnotation = new AnnotationDescriptor( SortableFields.class );
			AnnotationDescriptor facetsAnnotation = new AnnotationDescriptor( Facets.class );

			final org.hibernate.search.annotations.Field[] fieldArray =
					new org.hibernate.search.annotations.Field[fieldAnnotations.size()];
			final org.hibernate.search.annotations.Field[] fieldAsArray = fieldAnnotations.toArray( fieldArray );

			final NumericField[] numericFieldArray = new NumericField[numericFieldAnnotations.size()];
			final NumericField[] numericFieldAsArray = numericFieldAnnotations.toArray( numericFieldArray );
			numericFieldsAnnotation.setValue( "value", numericFieldAsArray );
			annotations.put( NumericFields.class, createAnnotation( numericFieldsAnnotation ) );

			final SortableField[] sortableFieldArray = new SortableField[sortableFieldAnnotations.size()];
			final SortableField[] sortableFieldAsArray = sortableFieldAnnotations.toArray( sortableFieldArray );
			sortableFieldsAnnotation.setValue( "value", sortableFieldAsArray );
			annotations.put( SortableFields.class, createAnnotation( sortableFieldsAnnotation ) );

			final Facet[] facetArray = new Facet[facetAnnotations.size()];
			final Facet[] facetAsArray = facetAnnotations.toArray( facetArray );
			facetsAnnotation.setValue( "value", facetAsArray );
			annotations.put( Facets.class, createAnnotation( facetsAnnotation ) );

			fieldsAnnotation.setValue( "value", fieldAsArray );
			annotations.put( Fields.class, createAnnotation( fieldsAnnotation ) );
			createDateBridge( property );
			createCalendarBridge( property );
			createDynamicBoost( property );
			createFieldBridge( property );
		}

		private void addAnalyzerAnnotationTo(AnnotationDescriptor fieldAnnotation, Entry<String, Object> entry) {
			AnnotationDescriptor analyzerAnnotation = new AnnotationDescriptor( Analyzer.class );
			@SuppressWarnings("unchecked")
			Map<String, Object> analyzer = (Map<String, Object>) entry.getValue();
			for ( Map.Entry<String, Object> analyzerEntry : analyzer.entrySet() ) {
				analyzerAnnotation.setValue( analyzerEntry.getKey(), analyzerEntry.getValue() );
			}
			fieldAnnotation.setValue( "analyzer", createAnnotation( analyzerAnnotation ) );
		}

		private void addNormalizerAnnotationTo(AnnotationDescriptor fieldAnnotation, Entry<String, Object> entry) {
			AnnotationDescriptor normalizerAnnotation = new AnnotationDescriptor( Normalizer.class );
			@SuppressWarnings("unchecked")
			Map<String, Object> analyzer = (Map<String, Object>) entry.getValue();
			for ( Map.Entry<String, Object> analyzerEntry : analyzer.entrySet() ) {
				normalizerAnnotation.setValue( analyzerEntry.getKey(), analyzerEntry.getValue() );
			}
			fieldAnnotation.setValue( "normalizer", createAnnotation( normalizerAnnotation ) );
		}

		private void createFieldBridge(PropertyDescriptor property) {
			if ( property.getFieldBridge() != null ) {
				AnnotationDescriptor fieldBridgeAnn = new AnnotationDescriptor( FieldBridge.class );
				Set<Entry<String, Object>> entrySet = property.getFieldBridge().entrySet();
				for ( Entry<String, Object> entry : entrySet ) {
					fieldBridgeAnn.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( FieldBridge.class, createAnnotation( fieldBridgeAnn ) );
			}
		}

		private void createDynamicBoost(PropertyDescriptor property) {
			if ( property.getDynamicBoost() != null ) {
				AnnotationDescriptor dynamicBoostAnn = new AnnotationDescriptor( DynamicBoost.class );
				Set<Entry<String, Object>> entrySet = property.getDynamicBoost().entrySet();
				for ( Entry<String, Object> entry : entrySet ) {
					dynamicBoostAnn.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( DynamicBoost.class, createAnnotation( dynamicBoostAnn ) );
			}
		}

		private void createContainedIn(PropertyDescriptor property) {
			if ( property.getContainedIn() != null ) {
				Map<String, Object> containedIn = property.getContainedIn();
				AnnotationDescriptor containedInAnn = new AnnotationDescriptor( ContainedIn.class );
				Set<Entry<String, Object>> entrySet = containedIn.entrySet();
				for ( Entry<String, Object> entry : entrySet ) {
					containedInAnn.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( ContainedIn.class, createAnnotation( containedInAnn ) );
			}
		}

		private void createIndexEmbedded(PropertyDescriptor property) {
			Map<String, Object> indexEmbedded = property.getIndexEmbedded();
			if ( indexEmbedded != null ) {
				AnnotationDescriptor indexEmbeddedAnn = new AnnotationDescriptor( IndexedEmbedded.class );
				Set<Entry<String, Object>> entrySet = indexEmbedded.entrySet();
				for ( Entry<String, Object> entry : entrySet ) {
					indexEmbeddedAnn.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( IndexedEmbedded.class, createAnnotation( indexEmbeddedAnn ) );
			}
		}

		private void createIndexed(EntityDescriptor entity) {
			{
				Class<? extends Annotation> annotationType = Indexed.class;
				AnnotationDescriptor annotation = new AnnotationDescriptor( annotationType );
				if ( entity.getIndexed() != null ) {
					for ( Map.Entry<String, Object> entry : entity.getIndexed().entrySet() ) {
						annotation.setValue( entry.getKey(), entry.getValue() );
					}
					annotations.put( annotationType, createAnnotation( annotation ) );
				}
			}
			{
				if ( entity.getBoost() != null ) {
					AnnotationDescriptor annotation = new AnnotationDescriptor( Boost.class );
					for ( Map.Entry<String, Object> entry : entity.getBoost().entrySet() ) {
						annotation.setValue( entry.getKey(), entry.getValue() );
					}
					annotations.put( Boost.class, createAnnotation( annotation ) );
				}
			}
			{
				if ( entity.getAnalyzerDiscriminator() != null ) {
					AnnotationDescriptor annotation = new AnnotationDescriptor( AnalyzerDiscriminator.class );
					for ( Map.Entry<String, Object> entry : entity.getAnalyzerDiscriminator().entrySet() ) {
						annotation.setValue( entry.getKey(), entry.getValue() );
					}
					annotations.put( AnalyzerDiscriminator.class, createAnnotation( annotation ) );
				}
			}
			{
				if ( entity.getFullTextFilterDefs().size() > 0 ) {
					AnnotationDescriptor fullTextFilterDefsAnnotation = new AnnotationDescriptor( FullTextFilterDefs.class );
					FullTextFilterDef[] fullTextFilterDefArray = createFullTextFilterDefArray( entity.getFullTextFilterDefs() );
					fullTextFilterDefsAnnotation.setValue( "value", fullTextFilterDefArray );
					annotations.put( FullTextFilterDefs.class, createAnnotation( fullTextFilterDefsAnnotation ) );
				}
			}
			if ( entity.getProvidedId() != null ) {
				createProvidedId( entity );
			}

			if ( entity.getClassBridgeDefs().size() > 0 ) {
				AnnotationDescriptor classBridgesAnn = new AnnotationDescriptor( ClassBridges.class );
				ClassBridge[] classBridesDefArray = createClassBridgesDefArray( entity.getClassBridgeDefs() );
				classBridgesAnn.setValue( "value", classBridesDefArray );
				annotations.put( ClassBridges.class, createAnnotation( classBridgesAnn ) );
			}

			if ( entity.getSpatials().size() > 0 ) {
				AnnotationDescriptor spatialsAnn = new AnnotationDescriptor( Spatials.class );
				Spatial[] spatialsArray = createSpatialsArray( entity.getSpatials() );
				spatialsAnn.setValue( "value", spatialsArray );
				annotations.put( Spatials.class, createAnnotation( spatialsAnn ) );
			}

			if ( entity.getDynamicBoost() != null ) {
				AnnotationDescriptor dynamicBoostAnn = new AnnotationDescriptor( DynamicBoost.class );
				Set<Entry<String, Object>> entrySet = entity.getDynamicBoost().entrySet();
				for ( Entry<String, Object> entry : entrySet ) {
					dynamicBoostAnn.setValue( entry.getKey(), entry.getValue() );
				}
				annotations.put( DynamicBoost.class, createAnnotation( dynamicBoostAnn ) );
			}

			configureClassBridgeInstances( entity );
		}

		private ClassBridge[] createClassBridgesDefArray(Set<Map<String, Object>> classBridgeDefs) {
			ClassBridge[] classBridgeDefArray = new ClassBridge[classBridgeDefs.size()];
			int index = 0;
			for ( Map<String, Object> classBridgeDef : classBridgeDefs ) {
				classBridgeDefArray[index] = createClassBridge( classBridgeDef );
				index++;
			}

			return classBridgeDefArray;
		}

		/**
		 * Configures the class bridge instances of this descriptors if such exist. The map based configuration is
		 * transformed into an equivalent {@code ClassBridge} instance and written back to the given descriptor.
		 *
		 * @param entity the entity for which to configure the class bridge instances
		 */
		private void configureClassBridgeInstances(EntityDescriptor entity) {
			Map<org.hibernate.search.bridge.FieldBridge, Map<String, Object>> classBridges = entity.getClassBridgeInstanceDefs();

			for ( Entry<org.hibernate.search.bridge.FieldBridge, Map<String, Object>> classBridgeInstanceDef : classBridges
					.entrySet() ) {
				Map<String, Object> configuration = classBridgeInstanceDef.getValue();
				org.hibernate.search.bridge.FieldBridge instance = classBridgeInstanceDef.getKey();

				ClassBridge classBridgeAnnotation = createClassBridge( configuration );
				entity.addClassBridgeInstanceConfiguration( instance, classBridgeAnnotation );
			}
		}

		private Spatial[] createSpatialsArray(Set<Map<String, Object>> spatials) {
			Spatial[] spatialsArray = new Spatial[spatials.size()];
			int index = 0;
			for ( Map<String, Object> spatial : spatials ) {
				spatialsArray[index] = createSpatial( spatial );
				index++;
			}

			return spatialsArray;
		}

		private ClassBridge createClassBridge(Map<String, Object> classBridgeDef) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( ClassBridge.class );
			Set<Entry<String, Object>> entrySet = classBridgeDef.entrySet();
			for ( Entry<String, Object> entry : entrySet ) {
				if ( "analyzer".equals( entry.getKey() ) ) {
					addAnalyzerAnnotationTo( annotation, entry );
				}
				else if ( "params".equals( entry.getKey() ) ) {
					addParamsToAnnotation( annotation, entry );
				}
				else {
					annotation.setValue( entry.getKey(), entry.getValue() );
				}
			}

			return (ClassBridge) createAnnotation( annotation );
		}

		private Spatial createSpatial(Map<String, Object> spatial) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( Spatial.class );
			Set<Entry<String, Object>> entrySet = spatial.entrySet();
			for ( Entry<String, Object> entry : entrySet ) {
				annotation.setValue( entry.getKey(), entry.getValue() );
			}
			return (Spatial) createAnnotation( annotation );
		}

		private void createProvidedId(EntityDescriptor entity) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( ProvidedId.class );
			Set<Entry<String, Object>> entrySet = entity.getProvidedId().entrySet();
			for ( Entry<String, Object> entry : entrySet ) {
				if ( "bridge".equals( entry.getKey() ) ) {
					AnnotationDescriptor bridgeAnnotation = new AnnotationDescriptor( FieldBridge.class );
					@SuppressWarnings("unchecked")
					Map<String, Object> bridge = (Map<String, Object>) entry.getValue();
					for ( Map.Entry<String, Object> bridgeEntry : bridge.entrySet() ) {
						if ( "params".equals( bridgeEntry.getKey() ) ) {
							addParamsToAnnotation( bridgeAnnotation, bridgeEntry );
						}
						else {
							bridgeAnnotation.setValue( bridgeEntry.getKey(), bridgeEntry.getValue() );
						}
					}
					annotation.setValue( "bridge", createAnnotation( bridgeAnnotation ) );
				}
				else {
					annotation.setValue( entry.getKey(), entry.getValue() );
				}
			}
			annotations.put( ProvidedId.class, createAnnotation( annotation ) );
		}

		private void populateAnnotationArray() {
			annotationsArray = new Annotation[annotations.size()];
			int index = 0;
			for ( Annotation ann : annotations.values() ) {
				annotationsArray[index] = ann;
				index++;
			}
		}

		private void delegatesAnnotationReading() {
			for ( Annotation a : delegate.getAnnotations() ) {
				annotations.put( a.annotationType(), a );
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			initAnnotations();
			return (T) annotations.get( annotationType );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
			initAnnotations();
			return (T) annotations.get( annotationType ) != null;
		}

		@Override
		public Annotation[] getAnnotations() {
			initAnnotations();
			Collection<Annotation> tmpCollection = annotations.values();
			return tmpCollection.toArray( new Annotation[tmpCollection.size()] );
		}
	}
}
