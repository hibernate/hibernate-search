/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.singleinstance;

import java.util.List;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

/**
 * A runner that creates a single instance of the test for each set of parameters,
 * and allows the use of {@link BeforeAll}, {@link AfterAll} and {@link InstanceRule}.
 * <p>
 * <strong>CAUTION:</strong> this should only be used if test methods are read-only,
 * otherwise one test could have side-effects on others.
 */
public final class SingleInstanceRunnerWithParameters extends BlockJUnit4ClassRunnerWithParameters {

	public static class Factory implements ParametersRunnerFactory {
		@Override
		public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
			return new SingleInstanceRunnerWithParameters( test );
		}
	}

	private Object testInstance;

	private SingleInstanceRunnerWithParameters(TestWithParameters test) throws InitializationError {
		super( test );
	}

	@Override
	public Object createTest() throws Exception {
		return getOrCreateTestUnsafe();
	}

	/**
	 * @return A description that uses an actually unique display name including name of the test class,
	 * so that this name can be used by rules such as TestConfigurationProvider in the backend TCK.
	 */
	protected Description getDescriptionForInstanceRule() {
		Description description = Description.createSuiteDescription(
				getTestClass().getName() + getName(),
				getRunnerAnnotations()
		);
		for ( Description child : getDescription().getChildren() ) {
			description.addChild( child );
		}
		return description;
	}

	protected Object getOrCreateTestUnsafe() throws Exception {
		if ( testInstance == null ) {
			testInstance = super.createTest();
		}
		return testInstance;
	}

	protected Object getOrCreateTestSafe() {
		try {
			return new ReflectiveCallable() {
				@Override
				protected Object runReflectiveCall() throws Throwable {
					return getOrCreateTestUnsafe();
				}
			}.run();
		}
		catch (Throwable e) {
			return new Fail( e );
		}
	}

	@Override
	protected Statement childrenInvoker(RunNotifier notifier) {
		Statement statement = super.childrenInvoker( notifier );
		Object test = getOrCreateTestSafe();
		if ( !areAllChildrenIgnored() ) {
			statement = withBeforeAlls( test, statement );
			statement = withAfterAlls( test, statement );
			statement = withInstanceRules( test, statement );
		}
		return statement;
	}

	protected Statement withBeforeAlls(Object test, Statement statement) {
		List<FrameworkMethod> befores = getTestClass()
				.getAnnotatedMethods( BeforeAll.class );
		return befores.isEmpty() ? statement :
				new RunBefores( statement, befores, test );
	}

	protected Statement withAfterAlls(Object test, Statement statement) {
		List<FrameworkMethod> afters = getTestClass()
				.getAnnotatedMethods( AfterAll.class );
		return afters.isEmpty() ? statement :
				new RunAfters( statement, afters, test );
	}

	protected List<TestRule> instanceRules(Object test) {
		List<TestRule> result = getTestClass().getAnnotatedMethodValues( test, InstanceRule.class, TestRule.class );
		result.addAll( getTestClass().getAnnotatedFieldValues( test, InstanceRule.class, TestRule.class ) );
		return result;
	}

	private Statement withInstanceRules(Object test, Statement statement) {
		List<TestRule> instanceRules = instanceRules( test );
		return instanceRules.isEmpty() ? statement :
				new RunRules( statement, instanceRules, getDescriptionForInstanceRule() );
	}

	private boolean areAllChildrenIgnored() {
		for ( FrameworkMethod child : getChildren() ) {
			if ( !isIgnored( child ) ) {
				return false;
			}
		}
		return true;
	}

}
