/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.runner.nested;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * Provides functionality similar to JUnit 5's `@Nested`.
 */
public class NestedRunner extends Suite {

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public NestedRunner(Class<?> testClass, RunnerBuilder builder) throws Throwable {
		this( testClass, builder, new ArrayList<>() );
	}

	private NestedRunner(Class<?> testClass, RunnerBuilder builder, List<Runner> childRunners) throws Throwable {
		super( testClass, childRunners );
		Object testInstance = createTest();
		childRunners.addAll( childRunners( testClass.getClasses(), builder, testInstance ) );
	}

	protected Object createTest() throws Exception {
		return getTestClass().getOnlyConstructor().newInstance();
	}

	private List<Runner> childRunners(final Class<?>[] classes, RunnerBuilder builder,
			Object enclosingTestInstance)
			throws Throwable {
		final List<Runner> childRunners = new ArrayList<>( classes.length );
		for ( Class<?> testClass : classes ) {
			if ( !testClass.isAnnotationPresent( Nested.class ) ) {
				continue;
			}
			Runner runner;
			if ( testClass.isMemberClass() && !Modifier.isStatic( testClass.getModifiers() ) ) {
				runner = runnerForNonStaticNestedClass( testClass, builder, enclosingTestInstance );
			}
			else {
				runner = builder.runnerForClass( testClass );
			}
			childRunners.add( runner );
		}
		return childRunners;
	}

	private Runner runnerForNonStaticNestedClass(Class<?> testClass, RunnerBuilder builder,
			Object enclosingTestInstance)
			throws Throwable {
		RunWith runWith = testClass.getAnnotation( RunWith.class );
		Class<? extends Runner> nestedClassRunnerClass =
				runWith == null ? BlockJUnit4ClassRunner.class : runWith.value();
		if ( BlockJUnit4ClassRunner.class.equals( nestedClassRunnerClass ) ) {
			return new BlockJUnit4ClassRunnerWithEnclosingTestClass( testClass, enclosingTestInstance );
		}
		else if ( NestedRunner.class.equals( nestedClassRunnerClass ) ) {
			return new NestedRunnerWithEnclosingTestClass( testClass, builder, enclosingTestInstance );
		}
		else {
			throw new InitializationError( "Unsupported runner for @Nested, non-static test class: "
					+ testClass + "."
					+ " Improve the implementation of " + getClass().getName()
					+ " if you want it to support more runners for @Nested, non-static test classes." );
		}
	}
}
