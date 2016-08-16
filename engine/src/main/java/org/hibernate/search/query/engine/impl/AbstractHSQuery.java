/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Base class for {@link HSQuery} implementations, exposing basic state needed by all implementations.
 *
 * @author Gunnar Morling
 */
public abstract class AbstractHSQuery implements HSQuery, Serializable {

	private static final Log LOG = LoggerFactory.make();

	/**
	 * Common prefix shared by all defined projection constants.
	 */
	public static final String HSEARCH_PROJECTION_FIELD_PREFIX = "__HSearch_";

	protected transient ExtendedSearchIntegrator extendedIntegrator;
	protected transient TimeoutExceptionFactory timeoutExceptionFactory;
	protected transient TimeoutManagerImpl timeoutManager;

	protected List<Class<?>> targetedEntities;
	protected Set<Class<?>> indexedTargetedEntities;

	protected Sort sort;
	protected String tenantId;
	protected String[] projectedFields;
	protected boolean hasThisProjection;
	protected int firstResult;
	protected Integer maxResults;
	protected Coordinates spatialSearchCenter = null;
	protected String spatialFieldName = null;

	/**
	 * User specified filters. Will be combined into a single chained filter {@link #filter}.
	 */
	protected Filter userFilter;

	/**
	 * The  map of currently active/enabled filters.
	 */
	protected final Map<String, FullTextFilterImpl> filterDefinitions = newHashMap();

	public AbstractHSQuery(ExtendedSearchIntegrator extendedIntegrator) {
		this.extendedIntegrator = extendedIntegrator;
		this.timeoutExceptionFactory = extendedIntegrator.getDefaultTimeoutExceptionFactory();
	}

	@Override
	public void afterDeserialise(SearchIntegrator extendedIntegrator) {
		this.extendedIntegrator = extendedIntegrator.unwrap( ExtendedSearchIntegrator.class );
	}

	// mutators

	@Override
	public HSQuery setSpatialParameters(Coordinates center, String fieldName) {
		spatialSearchCenter = center;
		spatialFieldName = fieldName;
		return this;
	}

	@Override
	public HSQuery tenantIdentifier(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	@Override
	public HSQuery targetedEntities(List<Class<?>> classes) {
		clearCachedResults();
		this.targetedEntities = classes == null ? new ArrayList<Class<?>>( 0 ) : new ArrayList<Class<?>>( classes );
		final Class<?>[] classesAsArray = targetedEntities.toArray( new Class[targetedEntities.size()] );
		this.indexedTargetedEntities = extendedIntegrator.getIndexedTypesPolymorphic( classesAsArray );
		if ( targetedEntities.size() > 0 && indexedTargetedEntities.size() == 0 ) {
			throw LOG.targetedEntityTypesNotIndexed( StringHelper.join( targetedEntities, "," ));
		}
		return this;
	}

	@Override
	public HSQuery sort(Sort sort) {
		clearCachedResults();
		this.sort = sort;
		return this;
	}

	@Override
	public HSQuery filter(Filter filter) {
		clearCachedResults();
		this.userFilter = filter;
		return this;
	}

	@Override
	public HSQuery timeoutExceptionFactory(TimeoutExceptionFactory exceptionFactory) {
		this.timeoutExceptionFactory = exceptionFactory;
		return this;
	}

	@Override
	public HSQuery projection(String... fields) {
		if ( fields == null || fields.length == 0 ) {
			this.projectedFields = null;
		}
		else {
			this.projectedFields = fields;
			boolean hasThis = false;
			Set<String> supportedProjectionConstants = getSupportedProjectionConstants();

			for ( String field : fields ) {
				if ( ProjectionConstants.THIS.equals( field ) ) {
					hasThis = true;
				}
				if ( field != null && field.startsWith( HSEARCH_PROJECTION_FIELD_PREFIX ) && !supportedProjectionConstants.contains( field ) ) {
					throw LOG.unexpectedProjectionConstant( field );
				}
			}
			this.hasThisProjection = hasThis;
		}
		return this;
	}

	@Override
	public HSQuery firstResult(int firstResult) {
		if ( firstResult < 0 ) {
			throw new IllegalArgumentException( "'first' pagination parameter less than 0" );
		}
		this.firstResult = firstResult;
		return this;
	}

	@Override
	public HSQuery maxResults(int maxResults) {
		if ( maxResults < 0 ) {
			throw new IllegalArgumentException( "'max' pagination parameter less than 0" );
		}
		this.maxResults = maxResults;
		return this;
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		clearCachedResults();
		FullTextFilterImpl filterDefinition = filterDefinitions.get( name );
		if ( filterDefinition != null ) {
			return filterDefinition;
		}

		filterDefinition = new FullTextFilterImpl();
		filterDefinition.setName( name );
		FilterDef filterDef = extendedIntegrator.getFilterDefinition( name );
		if ( filterDef == null ) {
			throw LOG.unknownFullTextFilter( name );
		}
		filterDefinitions.put( name, filterDefinition );
		return filterDefinition;
	}

	@Override
	public void disableFullTextFilter(String name) {
		clearCachedResults();
		filterDefinitions.remove( name );
	}

	protected Object createFilterInstance(FullTextFilterImpl fullTextFilter, FilterDef def) {
		final Object instance = ClassLoaderHelper.instanceFromClass( Object.class, def.getImpl(), "@FullTextFilterDef" );
		for ( Map.Entry<String, Object> entry : fullTextFilter.getParameters().entrySet() ) {
			def.invoke( entry.getKey(), instance, entry.getValue() );
		}
		return instance;
	}

	protected boolean isPreQueryFilterOnly(FilterDef def) {
		return def.getImpl().equals( ShardSensitiveOnlyFilter.class );
	}

	// getters

	/**
	 * List of targeted entities as described by the user
	 */
	@Override
	public List<Class<?>> getTargetedEntities() {
		return targetedEntities;
	}

	/**
	 * Set of indexed entities corresponding to the class hierarchy of the targeted entities
	 */
	@Override
	public Set<Class<?>> getIndexedTargetedEntities() {
		return indexedTargetedEntities;
	}

	@Override
	public String[] getProjectedFields() {
		return projectedFields;
	}

	@Override
	public boolean hasThisProjection() {
		return hasThisProjection;
	}

	@Override
	public TimeoutManagerImpl getTimeoutManager() {
		if ( timeoutManager == null ) {
			timeoutManager = buildTimeoutManager();
		}

		return timeoutManager;
	}

	@Override
	public ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		return extendedIntegrator;
	}

	/**
	 * Returns the names of the projection constants supported by a specific implementation in addition to projecting
	 * actual field values. If a given projection name begins with {@link #HSEARCH_PROJECTION_FIELD_PREFIX} and is not
	 * part of the set of constants returned by an implementation, an exception will be raised.
	 */
	protected abstract Set<String> getSupportedProjectionConstants();

	protected void validateSortFields(ExtendedSearchIntegrator extendedIntegrator, Iterable<Class<?>> targetedEntities) {
		SortField[] sortFields = sort.getSort();
		for ( SortField sortField : sortFields ) {
			validateSortField( extendedIntegrator, targetedEntities, sortField );
		}
	}

	private void validateSortField(ExtendedSearchIntegrator extendedIntegrator, Iterable<Class<?>> targetedEntities, SortField sortField) {
		if ( sortField instanceof DistanceSortField ) {
			validateDistanceSortField( extendedIntegrator, targetedEntities, sortField );
		}
		else if ( sortField.getType() != SortField.Type.CUSTOM ) {
			if ( sortField.getField() == null ) {
				validateNullSortField( sortField );
			}
			else {
				validateCommonSortField( extendedIntegrator, targetedEntities, sortField );
			}
		}
	}

	private void validateNullSortField(SortField sortField) {
		if ( sortField.getType() != SortField.Type.DOC && sortField.getType() != SortField.Type.SCORE ) {
			throw LOG.sortRequiresIndexedField( sortField.getClass(), sortField.getField() );
		}
	}

	private void validateDistanceSortField(ExtendedSearchIntegrator extendedIntegrator, Iterable<Class<?>> targetedEntities, SortField sortField) {
		DocumentFieldMetadata documentFieldMetadata = findFieldMetadata( extendedIntegrator, targetedEntities, sortField.getField() );
		if ( documentFieldMetadata == null ) {
			throw LOG.sortRequiresIndexedField( sortField.getClass(), sortField.getField() );
		}
		if ( !documentFieldMetadata.isSpatial() ) {
			throw LOG.distanceSortRequiresSpatialField( sortField.getField() );
		}
	}

	private void validateCommonSortField(ExtendedSearchIntegrator extendedIntegrator, Iterable<Class<?>> targetedEntities, SortField sortField) {
		DocumentFieldMetadata metadata = findFieldMetadata( extendedIntegrator, targetedEntities, sortField.getField() );
		if ( metadata != null ) {
			validateSortField( sortField, metadata );
		}
		else {
			BridgeDefinedField bridgeDefinedField = findBridgeDefinedField( extendedIntegrator, targetedEntities, sortField.getField() );
			if ( bridgeDefinedField != null ) {
				validateSortField( sortField, bridgeDefinedField );
			}
			//else the field is not known. Custom fieldbridge? Not throwing an exception to improve backwards compatibility
		}
	}

	private void validateSortField(SortField sortField, BridgeDefinedField bridgeDefinedField) {
		switch ( sortField.getType() ) {
			case INT:
				assertType( sortField, bridgeDefinedField.getType(), FieldType.INTEGER );
				break;
			case LONG:
				assertType( sortField, bridgeDefinedField.getType(), FieldType.LONG );
				break;
			case DOUBLE:
				assertType( sortField, bridgeDefinedField.getType(), FieldType.DOUBLE );
				break;
			case FLOAT:
				assertType( sortField, bridgeDefinedField.getType(), FieldType.FLOAT );
				break;
			case STRING:
			case STRING_VAL:
				assertType( sortField, bridgeDefinedField.getType(), FieldType.STRING );
				break;
			default:
				throw LOG.sortTypeDoesNotMatchFieldType( String.valueOf( sortField.getType() ), String.valueOf( bridgeDefinedField.getType() ), sortField.getField() );
		}
	}

	private void assertType(SortField sortField, FieldType actual, FieldType expected) {
		if ( actual != expected ) {
			throw LOG.sortTypeDoesNotMatchFieldType( String.valueOf( sortField.getType() ), String.valueOf( actual ), sortField.getField() );
		}
	}

	private BridgeDefinedField findBridgeDefinedField(ExtendedSearchIntegrator extendedIntegrator, Iterable<Class<?>> targetedEntities, String field) {
		if ( field == null ) {
			return null;
		}
		for ( Class<?> clazz : targetedEntities ) {
			EntityIndexBinding indexBinding = extendedIntegrator.getIndexBinding( clazz );
			TypeMetadata typeMetadata = indexBinding.getDocumentBuilder().getTypeMetadata();
			Set<BridgeDefinedField> classBridgeDefinedFields = typeMetadata.getClassBridgeDefinedFields();
			for ( BridgeDefinedField definedField : classBridgeDefinedFields ) {
				if ( definedField.getName().equals( field ) ) {
					return definedField;
				}
			}
			List<EmbeddedTypeMetadata> embeddedTypeMetadatas = typeMetadata.getEmbeddedTypeMetadata();
			for ( EmbeddedTypeMetadata embeddedMetadata : embeddedTypeMetadatas ) {
				Set<BridgeDefinedField> embeddedBridgeDefinedFields = embeddedMetadata.getClassBridgeDefinedFields();
				for ( BridgeDefinedField bridgeDefinedField : embeddedBridgeDefinedFields ) {
					if ( bridgeDefinedField.getName().equals( field ) ) {
						return bridgeDefinedField;
					}
				}
			}
			Set<PropertyMetadata> allPropertyMetadata = typeMetadata.getAllPropertyMetadata();
			for ( PropertyMetadata propertyMetadata : allPropertyMetadata ) {
				Map<String, BridgeDefinedField> bridgeDefinedFields = propertyMetadata.getBridgeDefinedFields();
				for ( BridgeDefinedField bridgeDefinedField : bridgeDefinedFields.values() ) {
					if ( bridgeDefinedField.getName().equals( field ) ) {
						return bridgeDefinedField;
					}
				}
			}
		}
		return null;
	}

	private DocumentFieldMetadata findFieldMetadata(ExtendedSearchIntegrator extendedIntegrator, Iterable<Class<?>> targetedEntities, String field) {
		if ( field == null ) {
			return null;
		}
		for ( Class<?> clazz : targetedEntities ) {
			EntityIndexBinding indexBinding = extendedIntegrator.getIndexBinding( clazz );
			DocumentFieldMetadata metadata = indexBinding.getDocumentBuilder().getTypeMetadata().getDocumentFieldMetadataFor( field );
			if ( metadata != null ) {
				return metadata;
			}
		}
		return null;
	}

	private void validateSortField(SortField sortField, DocumentFieldMetadata fieldMetadata) {
			if ( fieldMetadata.isNumeric() ) {
				NumericEncodingType numericEncodingType = fieldMetadata.getNumericEncodingType();
				validateNumericSortField( sortField, numericEncodingType );
			}
			else {
				if ( sortField.getType() != SortField.Type.STRING && sortField.getType() != SortField.Type.STRING_VAL ) {
					throw LOG.sortTypeDoesNotMatchFieldType( String.valueOf( sortField.getType() ), "string", sortField.getField() );
				}
			}
	}

	private void validateNumericSortField(SortField sortField, NumericEncodingType indexNumericEncodingType) {
		final NumericEncodingType sortNumericEncodingType;
		switch ( sortField.getType() ) {
			case BYTES:
			case INT:
				sortNumericEncodingType = NumericEncodingType.INTEGER;
				break;
			case LONG:
				sortNumericEncodingType = NumericEncodingType.LONG;
				break;
			case DOUBLE:
				sortNumericEncodingType = NumericEncodingType.DOUBLE;
				break;
			case FLOAT:
				sortNumericEncodingType = NumericEncodingType.FLOAT;
				break;
			default:
				throw LOG.sortTypeDoesNotMatchFieldType( String.valueOf( sortField.getType() ),
						String.valueOf( indexNumericEncodingType ), sortField.getField() );
		}
		if ( NumericEncodingType.UNKNOWN.equals( indexNumericEncodingType ) ) {
			/*
			 * The actual encoding type is unknown, so we can't validate more.
			 * This happens most notably when using custom numeric field bridges that do not implement
			 * MetadataProvidingFieldBridge. Even when implementing it, there are some quirks, see HSEARCH-2330.
			 * Anyway, the simplest solution until HS6 and mandatory metadata is to skip the rest
			 * of the validation in this particular case.
			 */
			return;
		}
		validateNumericEncodingType( sortField, indexNumericEncodingType, sortNumericEncodingType );
	}

	private void validateNumericEncodingType(SortField sortField, NumericEncodingType sortEncodingType,
			NumericEncodingType indexEncodingType) {
		if ( sortEncodingType != indexEncodingType ) {
			throw LOG.sortTypeDoesNotMatchFieldType(
					String.valueOf( sortField.getType() ), String.valueOf( indexEncodingType ), sortField.getField()
			);
		}
	}

	// hooks to be implemented by specific sub-classes

	protected abstract void extractFacetResults();

	protected abstract void clearCachedResults();

	protected abstract TimeoutManagerImpl buildTimeoutManager();
}
