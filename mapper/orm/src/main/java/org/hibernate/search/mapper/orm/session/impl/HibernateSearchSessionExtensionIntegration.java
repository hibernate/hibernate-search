package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.engine.extension.spi.ExtensionIntegration;
import org.hibernate.engine.extension.spi.ExtensionIntegrationContext;

public class HibernateSearchSessionExtensionIntegration implements ExtensionIntegration<HibernateOrmSearchSessionExtension> {
	@Override
	public Class<HibernateOrmSearchSessionExtension> getExtensionType() {
		return HibernateOrmSearchSessionExtension.class;
	}

	@Override
	public HibernateOrmSearchSessionExtension createExtension(ExtensionIntegrationContext extensionIntegrationContext) {
		return HibernateOrmSearchSessionExtension.init();
	}
}
