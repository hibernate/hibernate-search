/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateSearchOrmMappingProducer;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationConfigurationContext;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.session.impl.ConfiguredAutomaticIndexingStrategy;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public final class CoordinationConfigurationContextImpl implements CoordinationConfigurationContext, AutoCloseable {

	private static final ConfigurationProperty<BeanReference<? extends CoordinationStrategy>> COORDINATION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.COORDINATION_STRATEGY )
					.asBeanReference( CoordinationStrategy.class )
					.withDefault( HibernateOrmMapperSettings.Defaults.COORDINATION_STRATEGY )
					.build();

	public static CoordinationConfigurationContextImpl configure(ConfigurationPropertySource propertySource,
			BeanResolver beanResolver) {
		BeanHolder<? extends CoordinationStrategy> strategyHolder =
				COORDINATION_STRATEGY.getAndTransform( propertySource, beanResolver::resolve );
		CoordinationConfigurationContextImpl context = new CoordinationConfigurationContextImpl( strategyHolder );
		try {
			strategyHolder.get().configure( context );
			return context;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( CoordinationConfigurationContextImpl::close, context );
			throw e;
		}
	}

	private final BeanHolder<? extends CoordinationStrategy> strategyHolder;

	private final List<HibernateSearchOrmMappingProducer> mappingProducers = new ArrayList<>();
	private Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory;
	private boolean enlistsInTransaction = false;

	public CoordinationConfigurationContextImpl(BeanHolder<? extends CoordinationStrategy> strategyHolder) {
		this.strategyHolder = strategyHolder;
	}

	@Override
	public void close() {
		strategyHolder.close();
	}

	@Override
	public void reindexInSession() {
		this.senderFactory = null;
		this.enlistsInTransaction = false;
	}

	@Override
	public void sendIndexingEventsTo(
			Function<AutomaticIndexingEventSendingSessionContext, AutomaticIndexingQueueEventSendingPlan> senderFactory,
			boolean enlistsInTransaction) {
		this.senderFactory = senderFactory;
		this.enlistsInTransaction = enlistsInTransaction;
	}

	@Override
	public void mappingProducer(HibernateSearchOrmMappingProducer producer) {
		mappingProducers.add( producer );
	}

	public BeanHolder<? extends CoordinationStrategy> strategyHolder() {
		return strategyHolder;
	}

	public ConfiguredAutomaticIndexingStrategy createAutomaticIndexingStrategy() {
		return new ConfiguredAutomaticIndexingStrategy( senderFactory, enlistsInTransaction );
	}

	public List<HibernateSearchOrmMappingProducer> mappingProducers() {
		return mappingProducers;
	}
}
