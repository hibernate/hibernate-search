/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.jberet.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;

import org.jberet.spi.JobXmlResolver;
import org.jberet.tools.AbstractJobXmlResolver;

public final class HibernateSearchJobXmlResolver extends AbstractJobXmlResolver implements JobXmlResolver {

	private static final String XML_SUFFIX = ".xml";

	private static final Map<String, String> JOB_XML_NAMES_TO_JOB_NAMES = Collections.unmodifiableMap(
			Arrays.asList( MassIndexingJob.NAME ).stream()
					.collect( Collectors.toMap( s -> s.concat( XML_SUFFIX ), Function.identity() ) )
	);

	@Override
	public Collection<String> getJobXmlNames(ClassLoader classLoader) {
		return JOB_XML_NAMES_TO_JOB_NAMES.keySet();
	}

	@Override
	public String resolveJobName(String jobXml, ClassLoader classLoader) {
		return JOB_XML_NAMES_TO_JOB_NAMES.get( jobXml );
	}

	@Override
	public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) throws IOException {
		if ( JOB_XML_NAMES_TO_JOB_NAMES.containsKey( jobXml ) ) {
			final String path = DEFAULT_PATH + jobXml;
			return classLoader.getResourceAsStream( path );
		}
		else {
			return null;
		}
	}
}
