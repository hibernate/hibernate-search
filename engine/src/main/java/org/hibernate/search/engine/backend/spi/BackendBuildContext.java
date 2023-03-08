/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.spi;

import java.util.Optional;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;

/**
 * A build context for backends.
 */
public interface BackendBuildContext {

	ClassResolver classResolver();

	ResourceResolver resourceResolver();

	BeanResolver beanResolver();

	ThreadPoolProvider threadPoolProvider();

	FailureHandler failureHandler();

	TimingSource timingSource();

	boolean multiTenancyEnabled();

	Optional<String> backendName();

}
