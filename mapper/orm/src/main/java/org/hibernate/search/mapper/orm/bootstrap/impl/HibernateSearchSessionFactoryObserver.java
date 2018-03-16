/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.ReflectionBeanResolver;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.orm.event.impl.FullTextIndexEventListener;
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingContributor;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingContributor;
import org.hibernate.search.mapper.orm.spi.BeanResolver;
import org.hibernate.search.mapper.orm.spi.EnvironmentSynchronizer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;

/**
 * A {@code SessionFactoryObserver} registered with Hibernate ORM during the integration phase.
 * This observer will initialize Hibernate Search once the {@code SessionFactory} is built.
 *
 * @author Hardy Ferentschik
 * @see HibernateSearchIntegrator
 */
public class HibernateSearchSessionFactoryObserver implements SessionFactoryObserver {

	private static final ConfigurationProperty<Boolean> ENABLE_ANNOTATION_MAPPING =
			ConfigurationProperty.forKey( SearchOrmSettings.Radicals.ENABLE_ANNOTATION_MAPPING )
					.asBoolean()
					.withDefault( true )
					.build();

	private final ConfigurationPropertySource propertySource;
	private final JndiService namingService;
	private final ClassLoaderService classLoaderService;
	private final EnvironmentSynchronizer environmentSynchronizer;
	private final BeanResolver beanResolver;
	private final FullTextIndexEventListener listener;
	private final Metadata metadata;

	private final CompletableFuture<HibernateSearchContextService> contextFuture = new CompletableFuture<>();

	//Guarded by synchronization on this
	// TODO JMX
//	private JMXHook jmx;

	HibernateSearchSessionFactoryObserver(
			Metadata metadata,
			ConfigurationPropertySource propertySource,
			FullTextIndexEventListener listener,
			ClassLoaderService classLoaderService,
			EnvironmentSynchronizer environmentSynchronizer,
			BeanResolver beanResolver,
			JndiService namingService) {
		this.metadata = metadata;
		this.propertySource = propertySource;
		this.listener = listener;
		this.classLoaderService = classLoaderService;
		this.environmentSynchronizer = environmentSynchronizer;
		this.beanResolver = beanResolver;
		this.namingService = namingService;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		boolean failedBootScheduling = true;
		try {
			SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
			listener.initialize( contextFuture );

			if ( environmentSynchronizer != null ) {
				environmentSynchronizer.whenEnvironmentReady( () -> boot( sessionFactoryImplementor ) );
			}
			else {
				boot( sessionFactoryImplementor );
			}

			failedBootScheduling = false;
		}
		finally {
			if ( failedBootScheduling ) {
				cancelBoot();
			}
		}
	}

	/**
	 * Boot Hibernate Search if it hasn't booted already,
	 * and complete {@link #contextFuture}.
	 * <p>
	 * This method is synchronized in order to avoid booting Hibernate Search
	 * after (or while) the boot has been canceled.
	 *
	 * @param sessionFactoryImplementor The factory on which to graft Hibernate Search.
	 *
	 * @see #cancelBoot()
	 */
	private synchronized void boot(SessionFactoryImplementor sessionFactoryImplementor) {
		if ( contextFuture.isDone() ) {
			return;
		}
		boolean failedBoot = true;
		try {
			SearchMappingRepositoryBuilder builder = SearchMappingRepository.builder( propertySource );

			boolean enableAnnotationMapping = ENABLE_ANNOTATION_MAPPING.get( propertySource );

			HibernateOrmMappingContributor mappingContributor = new HibernateOrmMappingContributor(
					builder, metadata, sessionFactoryImplementor, enableAnnotationMapping
			);

			org.hibernate.search.engine.common.spi.BeanResolver searchBeanResolver;
			if ( beanResolver != null ) {
				searchBeanResolver = new DelegatingBeanResolver( beanResolver );
			}
			else {
				searchBeanResolver = new ReflectionBeanResolver();
			}
			builder.setBeanResolver( searchBeanResolver );

			if ( enableAnnotationMapping ) {
				AnnotationMappingDefinition annotationMapping = mappingContributor.annotationMapping();
				metadata.getEntityBindings().stream()
						.map( PersistentClass::getMappedClass )
						// getMappedClass() can return null, which should be ignored
						.filter( Objects::nonNull )
						.forEach( annotationMapping::add );
			}

			ConfigurationProperty<Optional<HibernateOrmSearchMappingContributor>> userMappingContributorProperty =
					ConfigurationProperty.forKey( SearchOrmSettings.Radicals.MAPPING_CONTRIBUTOR )
							.as(
									HibernateOrmSearchMappingContributor.class,
									reference -> searchBeanResolver.resolve( reference, HibernateOrmSearchMappingContributor.class )
							)
							.build();
			userMappingContributorProperty.get( propertySource )
					.ifPresent( userContributor -> userContributor.contribute( mappingContributor ) );

			// TODO namingService (JMX)
			// TODO ClassLoaderService

			SearchMappingRepository mappingRepository = builder.build();
			HibernateOrmMapping mapping = mappingContributor.getResult();

			// TODO JMX
//			this.jmx = new JMXHook( propertySource );
//			this.jmx.registerIfEnabled( extendedIntegrator, factory );

			//Register the SearchFactory in the ORM ServiceRegistry (for convenience of lookup)
			HibernateSearchContextService contextService =
					sessionFactoryImplementor.getServiceRegistry().getService( HibernateSearchContextService.class );
			contextService.initialize( mappingRepository, mapping );
			contextFuture.complete( contextService );

			failedBoot = false;
		}
		catch (RuntimeException e) {
			contextFuture.completeExceptionally( e );
			throw e;
		}
		finally {
			if ( failedBoot ) {
				sessionFactoryImplementor.close();
			}
		}
	}

	@Override
	public synchronized void sessionFactoryClosing(SessionFactory factory) {
		cancelBoot();
	}

	/**
	 * Cancel the planned boot if it hasn't happened already.
	 * <p>
	 * This method is synchronized in order to avoid canceling the boot while it is ongoing,
	 * which could lead to resource leaks.
	 *
	 * @see #boot(SessionFactoryImplementor)
	 */
	private synchronized void cancelBoot() {
		contextFuture.cancel( false );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		contextFuture.thenAccept( this::cleanup );
	}

	private synchronized void cleanup(HibernateSearchContextService context) {
		if ( context != null ) {
			context.getMappingRepository().close();
		}
		// TODO JMX
//		jmx.unRegisterIfRegistered();
	}

}

