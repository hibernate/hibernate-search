/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

class ActualBackendSetupStrategy implements BackendSetupStrategy {
	private final String defaultBackendName;
	private final Map<String, BackendConfiguration> backendConfigurations;

	ActualBackendSetupStrategy(String defaultBackendName, Map<String, BackendConfiguration> backendConfigurations) {
		this.defaultBackendName = defaultBackendName;
		this.backendConfigurations = backendConfigurations;
	}

	@Override
	public Optional<TestRule> getTestRule() {
		RuleChain ruleChain = null;
		for ( BackendConfiguration configuration : backendConfigurations.values() ) {
			Optional<TestRule> rule = configuration.getTestRule();
			if ( !rule.isPresent() ) {
				continue;
			}
			if ( ruleChain == null ) {
				ruleChain = RuleChain.emptyRuleChain();
			}
			ruleChain = ruleChain.around( rule.get() );
		}
		return Optional.ofNullable( ruleChain );
	}

	@Override
	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider) {
		for ( Map.Entry<String, BackendConfiguration> entry : backendConfigurations.entrySet() ) {
			String name = entry.getKey();
			BackendConfiguration configuration = entry.getValue();
			setupContext = configuration.setupWithName( setupContext, name, configurationProvider );
		}
		return setupContext.withProperty( EngineSettings.DEFAULT_BACKEND, defaultBackendName );
	}
}
