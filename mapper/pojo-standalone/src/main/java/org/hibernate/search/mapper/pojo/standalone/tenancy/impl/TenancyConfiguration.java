/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.tenancy.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;

public class TenancyConfiguration implements AutoCloseable {

	private static final ConfigurationProperty<
			BeanReference<? extends TenantIdentifierConverter>> MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
					ConfigurationProperty
							.forKey( StandalonePojoMapperSettings.Radicals.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER )
							.asBeanReference( TenantIdentifierConverter.class )
							.withDefault( StandalonePojoMapperSettings.Defaults.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER )
							.build();

	public static TenancyConfiguration create(BeanResolver beanResolver, TenancyMode tenancyMode,
			ConfigurationPropertySource configurationPropertySource) {

		BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter =
				MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER.getAndTransform( configurationPropertySource, beanResolver::resolve );
		return new TenancyConfiguration( tenancyMode, tenantIdentifierConverter );
	}

	private final TenancyMode tenancyMode;
	private final BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter;

	private TenancyConfiguration(TenancyMode tenancyMode,
			BeanHolder<? extends TenantIdentifierConverter> tenantIdentifierConverter) {
		this.tenancyMode = tenancyMode;
		this.tenantIdentifierConverter = tenantIdentifierConverter;
	}

	public Object convert(String tenantIdentifier) {
		return tenantIdentifierConverter.get().fromStringValue( tenantIdentifier );
	}

	public String convert(Object tenantIdentifier) {
		return tenantIdentifierConverter.get().toStringValue( tenantIdentifier );
	}

	public TenancyMode tenancyMode() {
		return tenancyMode;
	}

	@Override
	public void close() {
		if ( tenantIdentifierConverter != null ) {
			tenantIdentifierConverter.close();
		}
	}
}
