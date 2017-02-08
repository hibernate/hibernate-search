/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.cdi.massindexing.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.massindexing.JobContextSetupListener;

/**
 * Override of JobContextSetupListener retrieving entity manager factories
 * from a CDI context.
 * <p>
 * The only purpose of this class is to override the entity manager factory
 * registry.
 *
 * @author Yoann Rodiere
 */
/*
 * When a CDI-enabled JSR-352 implementation (like JBeret) resolves a reference,
 * it will *first* look for CDI bean with this name, and will only try to
 * interpret the reference as a fully-qualified class name if no CDI bean is found.
 *
 * Thus, when using a CDI-enabled JSR-352 implementation, naming the CDI bean this
 * way will make it override the original implementation.
 */
@Named("org.hibernate.search.jsr352.massindexing.JobContextSetupListener")
public class CDIJobContextSetupListener extends JobContextSetupListener {

	@Inject
	private EntityManagerFactoryRegistry registry;

	@Override
	protected EntityManagerFactoryRegistry getEntityManagerFactoryRegistry() {
		return registry;
	}

}
