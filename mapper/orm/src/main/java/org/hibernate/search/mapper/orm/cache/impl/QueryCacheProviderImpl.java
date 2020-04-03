/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cache.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.util.Version;
import org.hibernate.boot.registry.selector.spi.StrategyCreator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.search.backend.lucene.cache.spi.QueryCacheProvider;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceException;

public class QueryCacheProviderImpl implements QueryCacheProvider {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public QueryCache getQueryCache(BackendBuildContext context, ConfigurationPropertySource properties, Version luceneVersion) {
		StrategySelector selector = context.getServiceResolver().loadJmxService( StrategySelector.class );

		Object setting = properties.get( HibernateOrmMapperSettings.QUERY_CACHING_POLICY_CLASS ).orElse( null );
		if ( setting == null ) {
			return null;
		}

		QueryCache queryCache = selector.resolveStrategy(
			QueryCache.class,
			setting,
			(QueryCache) null,
			new StrategyCreatorQueryCacheImpl( properties, luceneVersion )
		);

		return queryCache;
	}

	private static class StrategyCreatorQueryCacheImpl implements StrategyCreator<QueryCache> {
		private final ConfigurationPropertySource properties;
		private final Version luceneVersion;

		public StrategyCreatorQueryCacheImpl(ConfigurationPropertySource properties, Version luceneVersion) {
			this.properties = properties;
			this.luceneVersion = luceneVersion;
		}

		@Override
		public QueryCache create(Class<? extends QueryCache> strategyClass) {
			// first look for a constructor accepting Properties
			try {
				final Constructor<? extends QueryCache> ctor = strategyClass.getConstructor(
					ConfigurationPropertySource.class,
					Version.class
				);
				return ctor.newInstance( properties, luceneVersion );
			}
			catch (NoSuchMethodException e) {
				log.debugf( "QueryCache impl [%s] did not provide constructor accepting ConfigurationPropertySource, Version", strategyClass.getName() );
			}
			catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
				throw new ServiceException( "Unable to call constructor of QueryCache impl [" + strategyClass.getName() + "]", e );
			}

			// finally try no-arg
			try {
				final Constructor<? extends QueryCache> ctor = strategyClass.getConstructor();
				return ctor.newInstance();
			}
			catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
				throw new ServiceException( "Unable to call constructor of QueryCache impl [" + strategyClass.getName() + "]", e );
			}
		}
	}
}
