/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Sort;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * Base class for {@link HSQuery} implementations, exposing basic state needed by all implementations.
 *
 * @author Gunnar Morling
 */
public abstract class AbstractHSQuery implements HSQuery, Serializable {

	private static final Log LOG = LoggerFactory.make();

	protected transient ExtendedSearchIntegrator extendedIntegrator;
	protected transient TimeoutExceptionFactory timeoutExceptionFactory;
	protected transient TimeoutManagerImpl timeoutManager;

	protected List<Class<?>> targetedEntities;
	protected Set<Class<?>> indexedTargetedEntities;

	protected Sort sort;
	protected String tenantId;
	protected String[] projectedFields;
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

	// hooks to be implemented by specific sub-classes

	protected abstract void clearCachedResults();

	protected abstract TimeoutManagerImpl buildTimeoutManager();
}
