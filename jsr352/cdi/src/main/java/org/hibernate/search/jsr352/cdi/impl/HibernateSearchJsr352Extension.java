/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.cdi.impl;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.massindexing.impl.JobContextSetupListener;
import org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader;


/**
 * A CDI extension to explicitly add hibernate-search-jsr352-core types to the CDI context.
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
 * (since this module, hibernate-search-jsr352-cdi, also provides an implementation).
 * <br>See <a href="https://issues.jboss.org/browse/WFLY-8656">WFLY-8656</a>.
 * </ul>
 * Thus we use explicit type registration as a workaround.
 *
 * @author Yoann Rodiere
 */
public class HibernateSearchJsr352Extension implements Extension {

	public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
		registerType( event, beanManager, JobContextSetupListener.class );
		registerType( event, beanManager, EntityReader.class );
	}

	private void registerType(AfterTypeDiscovery event, BeanManager beanManager, Class<?> clazz) {
		AnnotatedType<?> annotatedType = beanManager.createAnnotatedType( clazz );
		event.addAnnotatedType( annotatedType, clazz.getName() );
	}

}
