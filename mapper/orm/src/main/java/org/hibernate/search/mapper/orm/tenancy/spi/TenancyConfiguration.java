/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class TenancyConfiguration {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<List<String>> MULTI_TENANCY_TENANT_IDS =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.MULTI_TENANCY_TENANT_IDS )
					.asString().multivalued()
					.validate( value -> Contracts.assertNotNullNorEmpty( value, "value" ) )
					.build();

	public static TenancyConfiguration create(TenancyMode tenancyMode,
			ConfigurationPropertySource configurationPropertySource) {
		String tenantIdsConfigurationPropertyKey = MULTI_TENANCY_TENANT_IDS.resolveOrRaw( configurationPropertySource );
		switch ( tenancyMode ) {
			case SINGLE_TENANCY:
				return new TenancyConfiguration( Optional.of( Collections.emptySet() ),
						tenantIdsConfigurationPropertyKey );
			case MULTI_TENANCY:
				return new TenancyConfiguration(
						MULTI_TENANCY_TENANT_IDS.getAndMap( configurationPropertySource, LinkedHashSet::new ),
						tenantIdsConfigurationPropertyKey );
		}
		throw new AssertionFailure( "Unknown tenancy mode: " + tenancyMode );
	}

	private final Optional<Set<String>> tenantIds;
	private final String tenantIdsConfigurationPropertyKey;

	private TenancyConfiguration(Optional<Set<String>> tenantIds, String tenantIdsConfigurationPropertyKey) {
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
}
