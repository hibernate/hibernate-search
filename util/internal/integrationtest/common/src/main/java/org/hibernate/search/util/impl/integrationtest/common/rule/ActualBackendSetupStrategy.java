/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

class ActualBackendSetupStrategy implements BackendSetupStrategy {
	private final BackendConfiguration defaultBackendConfiguration;
	private final Map<String, BackendConfiguration> namedBackendConfigurations;
	private final List<BackendConfiguration> allConfigurations;

	ActualBackendSetupStrategy(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		this.defaultBackendConfiguration = defaultBackendConfiguration;
		this.namedBackendConfigurations = namedBackendConfigurations;
		allConfigurations = new ArrayList<>();
		if ( defaultBackendConfiguration != null ) {
			allConfigurations.add( defaultBackendConfiguration );
		}
		allConfigurations.removeIf( Objects::isNull );
	}

	@Override
	public String toString() {
		return namedBackendConfigurations.isEmpty() ? defaultBackendConfiguration.toString()
				: allConfigurations.toString();
	}

	@Override
	public Optional<TestRule> getTestRule() {
		RuleChain ruleChain = null;
		for ( BackendConfiguration configuration : allConfigurations ) {
			Optional<TestRule> rule = configuration.testRule();
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
	public <C extends MappingSetupHelper<C, ?, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider,
			// The mapping handle is not used by actual backends (only by BackendMock).
			CompletionStage<BackendMappingHandle> mappingHandlePromise) {
		if ( defaultBackendConfiguration != null ) {
			setupContext = defaultBackendConfiguration.setup( setupContext, null, configurationProvider );
		}
		for ( Map.Entry<String, BackendConfiguration> entry : namedBackendConfigurations.entrySet() ) {
			String name = entry.getKey();
			BackendConfiguration configuration = entry.getValue();
			setupContext = configuration.setup( setupContext, name, configurationProvider );
		}
		return setupContext;
	}
}
