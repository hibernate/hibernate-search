/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.config;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendFactory;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalysisConfigurer;
import org.hibernate.search.integrationtest.showcase.library.model.Account;
import org.hibernate.search.integrationtest.showcase.library.model.Book;
import org.hibernate.search.integrationtest.showcase.library.model.BookCopy;
import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;
import org.hibernate.search.integrationtest.showcase.library.model.Document;
import org.hibernate.search.integrationtest.showcase.library.model.DocumentCopy;
import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.integrationtest.showcase.library.model.Video;
import org.hibernate.search.integrationtest.showcase.library.model.VideoCopy;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;

public class SessionFactoryConfig {

	private static final String PREFIX = HibernateOrmMapperSettings.PREFIX;

	private SessionFactoryConfig() {
	}

	public static SessionFactory sessionFactory(boolean disableAutoIndexing) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backends.elasticsearchBackend_1.type", ElasticsearchBackendFactory.class.getName() )
				.applySetting( PREFIX + "default_backend", "elasticsearchBackend_1" )
				.applySetting( PREFIX + "backends.elasticsearchBackend_1.log.json_pretty_printing", true )
				.applySetting(
						PREFIX + "backends.elasticsearchBackend_1.index_defaults.lifecycle.strategy",
						ElasticsearchIndexLifecycleStrategyName.DROP_AND_CREATE_AND_DROP
				)
				.applySetting(
						// Make this test work even if there is only a single node in the cluster
						PREFIX + "backends.elasticsearchBackend_1.index_defaults.lifecycle.required_status",
						ElasticsearchIndexStatus.YELLOW
				)
				.applySetting(
						// TODO remove this and use an explicit refresh after initializing data instead
						PREFIX + "backends.elasticsearchBackend_1.index_defaults.refresh_after_write", true
				)
				.applySetting(
						PREFIX + "backends.elasticsearchBackend_1." + ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new LibraryAnalysisConfigurer()
				)
				.applySetting( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );

		if ( disableAutoIndexing ) {
			registryBuilder.applySetting( HibernateOrmMapperSettings.INDEXING_STRATEGY, HibernateOrmIndexingStrategyName.MANUAL );
		}

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Document.class )
				.addAnnotatedClass( Book.class )
				.addAnnotatedClass( Video.class )
				.addAnnotatedClass( Library.class )
				.addAnnotatedClass( DocumentCopy.class )
				.addAnnotatedClass( BookCopy.class )
				.addAnnotatedClass( VideoCopy.class )
				.addAnnotatedClass( Person.class )
				.addAnnotatedClass( Account.class )
				.addAnnotatedClass( Borrowal.class );

		return ms.buildMetadata().getSessionFactoryBuilder().build();
	}

}
