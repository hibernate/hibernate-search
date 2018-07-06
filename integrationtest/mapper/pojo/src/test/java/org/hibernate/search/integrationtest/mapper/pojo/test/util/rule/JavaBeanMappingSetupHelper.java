/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.util.rule;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingInitiator;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class JavaBeanMappingSetupHelper implements TestRule {

	private final MappingSetupHelper delegate = new MappingSetupHelper();

	public SetupContext withBackendMock(BackendMock backendMock) {
		return new SetupContext( delegate.withBackendMock( backendMock ) );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return delegate.apply( base, description );
	}

	public class SetupContext {

		private final MappingSetupHelper.SetupContext delegate;

		SetupContext(MappingSetupHelper.SetupContext delegate) {
			this.delegate = delegate;
		}

		public SetupContext withProperty(String key, String value) {
			delegate.withProperty( key, value );
			return this;
		}

		/**
		 * Add a mapping initiator to the setup. Note this method must not be called if you use {@link #setup(Function)}.
		 * @param beforeBuild A function called before Hibernate Search is bootstrapped.
		 * @param afterBuild A consumer called after Hibernate Search is bootstrapped. Gets passed the resulting mapping.
		 * @return The setup context, for method chaining.
		 */
		public SetupContext withMapping(Function<SearchMappingRepositoryBuilder, JavaBeanMappingInitiator> beforeBuild,
				BiConsumer<SearchMappingRepository, JavaBeanMapping> afterBuild) {
			delegate.withMapping(
					beforeBuild,
					(mappingRepository, initiator) -> afterBuild.accept( mappingRepository, initiator.getResult() )
			);
			return this;
		}

		/**
		 * Setup Hibernate Search, returning the mapping for the given initiator.
		 * @param beforeBuild A function called before Hibernate Search is bootstrapped.
		 * @return The resulting mapping.
		 */
		public JavaBeanMapping setup(Function<SearchMappingRepositoryBuilder, JavaBeanMappingInitiator> beforeBuild) {
			return delegate.setup( beforeBuild ).getResult();
		}

		/**
		 * Setup Hibernate Search, returning the {@link SearchMappingRepository}.
		 * @return The created {@link SearchMappingRepository}
		 */
		public SearchMappingRepository setup() {
			return delegate.setup();
		}
	}
}
