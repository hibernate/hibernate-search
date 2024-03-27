/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.tenancy.spi;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class TenancyConfiguration implements AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<List<String>> MULTI_TENANCY_TENANT_IDS =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MULTI_TENANCY_TENANT_IDS )
					.asString().multivalued()
					.validate( value -> Contracts.assertNotNullNorEmpty( value, "value" ) )
					.build();

	private static final ConfigurationProperty<
			BeanReference<? extends TenantIdentifierConverter>> MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
					ConfigurationProperty
							.forKey( HibernateOrmMapperSettings.Radicals.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER )
							.asBeanReference( TenantIdentifierConverter.class )
							.withDefault( HibernateOrmMapperSettings.Defaults.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER )
							.build();

	public static TenancyConfiguration create(BeanResolver beanResolver, TenancyMode tenancyMode,
			ConfigurationPropertySource configurationPropertySource) {
		String tenantIdsConfigurationPropertyKey = MULTI_TENANCY_TENANT_IDS.resolveOrRaw( configurationPropertySource );

		BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter =
				MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER.getAndTransform(
						configurationPropertySource, beanResolver::resolve );
		switch ( tenancyMode ) {
			case SINGLE_TENANCY:
				return new TenancyConfiguration( tenantIdentifierConverter, Optional.of( Collections.emptySet() ),
						tenantIdsConfigurationPropertyKey );
			case MULTI_TENANCY:
				return new TenancyConfiguration(
						tenantIdentifierConverter,
						MULTI_TENANCY_TENANT_IDS.getAndMap( configurationPropertySource, LinkedHashSet::new ),
						tenantIdsConfigurationPropertyKey );
		}
		throw new AssertionFailure( "Unknown tenancy mode: " + tenancyMode );
	}

	// for tests:
	public static TenancyConfiguration create(BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter,
			Optional<Set<String>> tenantIds,
			String tenantIdsConfigurationPropertyKey) {
		return new TenancyConfiguration( tenantIdentifierConverter, tenantIds, tenantIdsConfigurationPropertyKey );
	}

	private final Optional<Set<String>> tenantIds;
	private final String tenantIdsConfigurationPropertyKey;
	private final BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter;

	private TenancyConfiguration(BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter,
			Optional<Set<String>> tenantIds,
			String tenantIdsConfigurationPropertyKey) {
		this.tenantIdentifierConverter = tenantIdentifierConverter;
		this.tenantIds = tenantIds;
		this.tenantIdsConfigurationPropertyKey = tenantIdsConfigurationPropertyKey;
	}

	/**
	 * @return A set of all possible tenant IDs, or an empty set if the application is single-tenant.
	 * @throws org.hibernate.search.util.common.SearchException if the application is multi-tenant
	 * and the full list of tenant IDs was not configured.
	 */
	public Set<String> tenantIdsOrFail() {
		// This will only fail when using multi-tenancy,
		// because the set is always defined when using single-tenancy.
		return tenantIds.orElseThrow( () -> log.missingTenantIdConfiguration( tenantIdsConfigurationPropertyKey ) );
	}

	public SearchException invalidTenantId(String tenantId) {
		return log.invalidTenantId( tenantId, tenantIds.orElse( Collections.emptySet() ), tenantIdsConfigurationPropertyKey );
	}

	public Object convert(String tenantIdentifier) {
		return tenantIdentifierConverter.get().fromStringValue( tenantIdentifier );
	}

	public String convert(Object tenantIdentifier) {
		return tenantIdentifierConverter.get().toStringValue( tenantIdentifier );
	}

	@Override
	public void close() {
		if ( tenantIdentifierConverter != null ) {
			tenantIdentifierConverter.close();
		}
	}
}
