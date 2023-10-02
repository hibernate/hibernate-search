/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm.bytecodeenhacement.extension.engine;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.bytecodeenhacement.extension.engine.BytecodeEnhancedClassUtils.enhanceTestClass;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.bytecodeenhacement.extension.BytecodeEnhanced;

import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

public class BytecodeEnhancedTestEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {

	@Override
	public String getId() {
		return "bytecode-enhanced-engine";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		JupiterConfiguration configuration = new CachingJupiterConfiguration(
				new DefaultJupiterConfiguration( discoveryRequest.getConfigurationParameters() ) );
		JupiterEngineDescriptor engineDescriptor = new BytecodeEnhancedEngineDescriptor( uniqueId, configuration );
		new DiscoverySelectorResolver().resolveSelectors( discoveryRequest, engineDescriptor );

		for ( TestDescriptor testDescriptor : new HashSet<>( engineDescriptor.getChildren() ) ) {
			if ( testDescriptor instanceof ClassBasedTestDescriptor ) {
				try {
					ClassBasedTestDescriptor descriptor = (ClassBasedTestDescriptor) testDescriptor;
					// if the test class is annotated with @BytecodeEnhanced
					// we replace the descriptor with the new one that will point to an enhanced test class,
					// this also means that we need to add all the child descriptors back as well...
					// Then on the extension side we set the classloader that contains the enhanced test class
					// and set it back to the original once the test class is destroyed.
					if ( isAnnotated( descriptor.getTestClass(), BytecodeEnhanced.class ) ) {
						TestDescriptor parent = descriptor.getParent().orElseThrow( IllegalStateException::new );
						Class<?> klass = descriptor.getTestClass();

						JupiterConfiguration jc = ( (JupiterEngineDescriptor) parent ).getConfiguration();

						Set<? extends TestDescriptor> children = new HashSet<>( descriptor.getChildren() );
						descriptor.removeFromHierarchy();

						Class<?>[] classes = enhanceTestClass( klass );
						if ( classes.length == 1 ) {
							replaceWithEnhanced( classes[0], descriptor, jc, children, parent );
						}
						else {
							for ( int i = 0; i < classes.length; i++ ) {
								String index = Integer.toString( i );
								replaceWithEnhanced(
										classes[i], descriptor, jc, children, parent,
										id -> id.append( "enhacementContext:", index )
								);
							}
						}
					}
					else {
						testDescriptor.removeFromHierarchy();
					}
				}
				catch (ClassNotFoundException | NoSuchMethodException e) {
					throw new RuntimeException( e );
				}
			}
		}

		return engineDescriptor;
	}

	private void replaceWithEnhanced(Class<?> enhanced, ClassBasedTestDescriptor descriptor, JupiterConfiguration jc,
			Set<? extends TestDescriptor> children, TestDescriptor parent)
			throws NoSuchMethodException {
		replaceWithEnhanced( enhanced, descriptor, jc, children, parent, Function.identity() );
	}

	private void replaceWithEnhanced(Class<?> enhanced, ClassBasedTestDescriptor descriptor, JupiterConfiguration jc,
			Set<? extends TestDescriptor> children, TestDescriptor parent, Function<UniqueId, UniqueId> uniqueIdTransformer)
			throws NoSuchMethodException {
		ClassTestDescriptor updated = new ClassTestDescriptor(
				uniqueIdTransformer.apply( descriptor.getUniqueId() ),
				enhanced,
				jc
		);

		for ( TestDescriptor child : children ) {
			// this needs more cases for parameterized tests, test templates and so on ...
			// for now it'll only work with simple @Test tests
			if ( child instanceof TestMethodTestDescriptor ) {
				Method testMethod = ( (TestMethodTestDescriptor) child ).getTestMethod();
				updated.addChild(
						new TestMethodTestDescriptor(
								uniqueIdTransformer.apply( child.getUniqueId() ),
								updated.getTestClass(),
								findMethodReplacement( updated, testMethod ),
								jc
						) {

						}
				);

			}
			if ( child instanceof TestTemplateTestDescriptor ) {
				Method testMethod = ( (TestTemplateTestDescriptor) child ).getTestMethod();
				updated.addChild( new TestTemplateTestDescriptor(
						uniqueIdTransformer.apply( child.getUniqueId() ),
						updated.getTestClass(),
						findMethodReplacement( updated, testMethod ),
						jc
				) );
			}
		}
		parent.addChild( updated );
	}

	private Method findMethodReplacement(ClassTestDescriptor updated, Method testMethod) throws NoSuchMethodException {
		String name = testMethod.getDeclaringClass().getName();

		Class<?> testClass = updated.getTestClass();
		while ( !testClass.getName().equals( name ) ) {
			testClass = testClass.getSuperclass();
			if ( Object.class.equals( testClass ) ) {
				throw new IllegalStateException( "Wasn't able to find a test method " + testMethod );
			}
		}
		return testClass.getDeclaredMethod(
				testMethod.getName(),
				testMethod.getParameterTypes()
		);
	}

	@Override
	protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
		return new JupiterEngineExecutionContext( request.getEngineExecutionListener(),
				this.getJupiterConfiguration( request ) );
	}

	private JupiterConfiguration getJupiterConfiguration(ExecutionRequest request) {
		JupiterEngineDescriptor engineDescriptor = (JupiterEngineDescriptor) request.getRootTestDescriptor();
		return engineDescriptor.getConfiguration();
	}

	public Optional<String> getGroupId() {
		return Optional.of( "org.junit.jupiter" );
	}

	public Optional<String> getArtifactId() {
		return Optional.of( "junit-jupiter-engine" );
	}

	public static class Context implements EngineExecutionContext {
		private final ExecutionRequest request;

		public Context(ExecutionRequest request) {
			this.request = request;
		}
	}

}
