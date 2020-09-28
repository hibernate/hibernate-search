/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public interface ElasticsearcAwsCredentialsProvider {

	AwsCredentialsProvider create(ConfigurationPropertySource propertySource);

}
