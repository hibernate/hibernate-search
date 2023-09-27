/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.jberet.impl;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.inject.scope.spi.HibernateSearchJobScoped;
import org.hibernate.search.jakarta.batch.core.inject.scope.spi.HibernateSearchPartitionScoped;
import org.hibernate.search.jakarta.batch.core.massindexing.spi.JobContextSetupListener;
import org.hibernate.search.jakarta.batch.core.massindexing.step.spi.EntityIdReader;

import org.jberet.cdi.JobScoped;
import org.jberet.cdi.PartitionScoped;

/**
 * A CDI extension to explicitly add hibernate-search-mapper-orm-jakarta-batch-core types to the CDI context.
 * <p>
 * These types must be registered into the CDI context because they expect one of their
 * attributes to be injected, thus they need to be instantiated by CDI.
 * <p>
 * Ideally we should use component scanning to that effect, but:
 * <ul>
 * <li>In CDI, component scanning is configured per-module in META-INF/beans.xml,
 * with no way to add a package from another module.
 * <li>In Wildfly, component scanning in dependencies is always done with the "all" discovery type,
 * meaning that even components without CDI annotations will be discovered.
 * Besides exposing private types unnecessarily,
 * this is annoying because it puts default {@link EntityManagerFactoryRegistry} implementations into the CDI context,
 * leading to conflicts when injecting such types
 * (since this module, hibernate-search-mapper-orm-jakarta-batch-jberet, also provides an implementation).
 * <br>See <a href="https://issues.jboss.org/browse/WFLY-8656">WFLY-8656</a>.
 * <li>Even if we could solve the above, JBeret requires specific scope annotations to be set on the
 * beans so that they are correctly scoped. Since the core isn't dependent on JBeret, we must specify these
 * scopes manually when registering the beans.
 * </ul>
 * Thus we use explicit type and scope registration as a workaround.
 *
 * @author Yoann Rodiere
 */
public class HibernateSearchJakartaBatchExtension implements Extension {

	public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
		registerType( event, beanManager, JobContextSetupListener.class );
		registerType( event, beanManager, EntityIdReader.class );
	}

	public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
		addScopeAlias( event, beanManager, HibernateSearchJobScoped.class, JobScoped.class );
		addScopeAlias( event, beanManager, HibernateSearchPartitionScoped.class, PartitionScoped.class );
	}

	private void registerType(AfterTypeDiscovery event, BeanManager beanManager, Class<?> clazz) {
		AnnotatedType<?> annotatedType = beanManager.createAnnotatedType( clazz );
		event.addAnnotatedType( annotatedType, clazz.getName() );
	}

	private void addScopeAlias(AfterBeanDiscovery event, BeanManager beanManager,
			Class<? extends Annotation> alias, Class<? extends Annotation> target) {
		event.addContext( new AliasedContext( alias, beanManager, target ) );
	}

	private static class AliasedContext implements Context {

		private final Class<? extends Annotation> scopeType;
		private final BeanManager targetBeanManager;
		private final Class<? extends Annotation> targetScopeType;

		public AliasedContext(Class<? extends Annotation> scopeType,
				BeanManager targetBeanManager, Class<? extends Annotation> targetScopeType) {
			super();
			this.scopeType = scopeType;
			this.targetBeanManager = targetBeanManager;
			this.targetScopeType = targetScopeType;
		}

		private Context delegate() {
			return targetBeanManager.getContext( targetScopeType );
		}

		@Override
		public Class<? extends Annotation> getScope() {
			return scopeType;
		}

		@Override
		public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
			return delegate().get( contextual, creationalContext );
		}

		@Override
		public <T> T get(Contextual<T> contextual) {
			return delegate().get( contextual );
		}

		@Override
		public boolean isActive() {
			try {
				delegate();
				return true;
			}
			catch (ContextNotActiveException e) {
				return false;
			}
		}

	}

}
