/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.orm;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public final class OrmSetupHelper
		extends MappingSetupHelper<OrmSetupHelper.SetupContext, SimpleSessionFactoryBuilder, SessionFactory> {

	public OrmSetupHelper() {
		// Nothing to do
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext();
	}

	@Override
	protected String getPropertiesPath(String configurationId) {
		return "/hibernate-test-" + configurationId + ".properties";
	}

	@Override
	protected void close(SessionFactory toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends MappingSetupHelper<SetupContext, SimpleSessionFactoryBuilder, SessionFactory>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();

		SetupContext() {
			// Ensure overridden properties will be applied
			withConfiguration( builder -> overriddenProperties.forEach( builder::setProperty ) );
		}

		public SetupContext withTcclLookupPrecedenceBefore() {
			withConfiguration( builder -> builder.setTcclLookupPrecedenceBefore() );
			return thisAsC();
		}

		@Override
		public SetupContext withPropertyRadical(String keyRadical, Object value) {
			return withProperty( HibernateOrmMapperSettings.PREFIX + keyRadical, value );
		}

		@Override
		public SetupContext withProperty(String key, Object value) {
			overriddenProperties.put( key, value );
			return thisAsC();
		}

		public SessionFactory setup(Class<?> ... annotatedTypes) {
			return withConfiguration( builder -> builder.addAnnotatedClasses( Arrays.asList( annotatedTypes ) ) )
					.setup();
		}

		@Override
		protected SimpleSessionFactoryBuilder createBuilder() {
			return new SimpleSessionFactoryBuilder();
		}

		@Override
		protected SessionFactory build(SimpleSessionFactoryBuilder builder) {
			return builder.build();
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}

}
