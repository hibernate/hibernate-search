/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.analyzer.Discriminator;
import org.hibernate.search.annotations.FieldCacheType;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;

public class IndexedMapping {

	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String, Object> indexed;

	public IndexedMapping(SearchMapping mapping, EntityDescriptor entity) {
		this.mapping = mapping;
		this.entity = entity;
		indexed = new HashMap<String, Object>();
		entity.setIndexed( indexed );
	}

	public IndexedMapping indexName(String indexName) {
		this.indexed.put( "index", indexName );
		return this;
	}

	public IndexedMapping interceptor(Class<? extends EntityIndexingInterceptor> interceptor) {
		this.indexed.put( "interceptor", interceptor );
		return this;
	}

	/**
	 * This feature will be removed, with no replacement
	 * as caching fields is no longer effective.
	 * @param type this argument will be ignored
	 * @return {@code this} for method chaining
	 * @deprecated This will be removed with no replacement.
	 */
	@Deprecated
	public IndexedMapping cacheFromIndex(FieldCacheType... type) {
		return this;
	}

	public IndexedMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		entity.setBoost( boostAnn );
		return this;
	}

	public IndexedMapping dynamicBoost(Class<? extends BoostStrategy> impl) {
		final Map<String, Object> dynamicBoost = new HashMap<String, Object>();
		dynamicBoost.put( "impl", impl );
		entity.setDynamicBoost( dynamicBoost );
		return this;
	}

	public IndexedMapping analyzerDiscriminator(Class<? extends Discriminator> discriminator) {
		final Map<String, Object> discriminatorAnn = new HashMap<String, Object>();
		discriminatorAnn.put( "impl", discriminator );
		entity.setAnalyzerDiscriminator( discriminatorAnn );
		return this;
	}

	public IndexedClassBridgeMapping classBridge(Class<?> impl) {
		return new IndexedClassBridgeMapping( mapping, entity, impl, this );
	}

	/**
	 * Registers the given class bridge for the currently configured entity type. Any subsequent analyzer, parameter
	 * etc. configurations apply to this class bridge.
	 *
	 * @param instance a class bridge instance
	 * @return a new {@link ClassBridgeMapping} following the method chaining pattern
	 * @hsearch.experimental This method is considered experimental and it may be altered or removed in future releases
	 * @throws org.hibernate.search.exception.SearchException in case the same bridge instance is passed more than once for the
	 * currently configured entity type
	 */
	public IndexedClassBridgeMapping classBridgeInstance(FieldBridge instance) {
		return new IndexedClassBridgeMapping( mapping, entity, instance, this );
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping( mapping, name, impl );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

	public ProvidedIdMapping providedId() {
		return new ProvidedIdMapping( mapping, entity );
	}

	public EntitySpatialMapping spatial() {
		return new EntitySpatialMapping( mapping, entity );
	}

}
