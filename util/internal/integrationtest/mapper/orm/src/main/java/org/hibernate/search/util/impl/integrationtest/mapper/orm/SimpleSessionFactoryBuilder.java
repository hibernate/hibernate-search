/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

public final class SimpleSessionFactoryBuilder {
	private static final Logger log = Logger.getLogger( SimpleSessionFactoryBuilder.class.getName() );

	private final List<Consumer<BootstrapServiceRegistryBuilder>> bootstrapServiceRegistryBuilderContributors = new ArrayList<>();
	private final List<Consumer<StandardServiceRegistryBuilder>> serviceRegistryBuilderContributors = new ArrayList<>();
	private final List<Consumer<MetadataSources>> metadataSourcesContributors = new ArrayList<>();
	private final List<Consumer<MetadataImplementor>> metadataContributors = new ArrayList<>();
	private final List<Consumer<SessionFactoryBuilder>> sessionFactoryBuilderContributors = new ArrayList<>();

	@SuppressForbiddenApis(reason = "Strangely, this API involves the internal TcclLookupPrecedence class,"
			+ " and there's nothing we can do about it")
	public SimpleSessionFactoryBuilder setTcclLookupPrecedenceBefore() {
		return onBootstrapServiceRegistryBuilder( builder -> builder.applyTcclLookupPrecedence( TcclLookupPrecedence.BEFORE ) );
	}

	public SimpleSessionFactoryBuilder setProperty(String key, Object value) {
		return onServiceRegistryBuilder( builder -> {
			if ( value == null ) {
				log.infof( "Not setting the property with key '%s' because value is null.", key );
			}
			else {
				builder.applySetting( key, value );
			}
		} );
	}

	public SimpleSessionFactoryBuilder addAnnotatedClass(Class<?> clazz) {
		return onMetadataSources( sources -> sources.addAnnotatedClass( clazz ) );
	}

	public SimpleSessionFactoryBuilder addAnnotatedClasses(Class<?> firstClass, Class<?> ... otherClasses) {
		return addAnnotatedClasses( CollectionHelper.asList( firstClass, otherClasses ) );
	}

	public SimpleSessionFactoryBuilder addAnnotatedClasses(Iterable<Class<?>> classes) {
		return onMetadataSources( sources -> classes.forEach( sources::addAnnotatedClass ) );
	}

	public SimpleSessionFactoryBuilder addHbmFromClassPath(String firstPath, String ... otherPaths) {
		return addHbmFromClassPath( CollectionHelper.asList( firstPath, otherPaths ) );
	}

	public SimpleSessionFactoryBuilder addHbmFromClassPath(Iterable<String> paths) {
		return onMetadataSources( sources -> paths.forEach( sources::addResource ) );
	}

	public SimpleSessionFactoryBuilder onBootstrapServiceRegistryBuilder(Consumer<BootstrapServiceRegistryBuilder> contributor) {
		bootstrapServiceRegistryBuilderContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onServiceRegistryBuilder(Consumer<StandardServiceRegistryBuilder> contributor) {
		serviceRegistryBuilderContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onMetadataSources(Consumer<MetadataSources> contributor) {
		metadataSourcesContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onMetadata(Consumer<MetadataImplementor> contributor) {
		metadataContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onSessionFactoryBuilder(Consumer<SessionFactoryBuilder> contributor) {
		sessionFactoryBuilderContributors.add( contributor );
		return this;
	}

	public SessionFactory build() {
		BootstrapServiceRegistry bootstrapServiceRegistry = null;
		ServiceRegistry serviceRegistry = null;
		MetadataSources metadataSources;
		SessionFactoryBuilder sessionFactoryBuilder;

		try {
			BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
			bootstrapServiceRegistryBuilderContributors.forEach( c -> c.accept( bootstrapServiceRegistryBuilder ) );
			bootstrapServiceRegistry = bootstrapServiceRegistryBuilder.build();

			StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootstrapServiceRegistry );
			serviceRegistryBuilderContributors.forEach( c -> c.accept( registryBuilder ) );
			serviceRegistry = registryBuilder.build();

			metadataSources = new MetadataSources( serviceRegistry );
			metadataSourcesContributors.forEach( c -> c.accept( metadataSources ) );
			Metadata metadata = metadataSources.buildMetadata();

			metadataContributors.forEach( c -> c.accept( (MetadataImplementor) metadata ) );

			sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
			sessionFactoryBuilderContributors.forEach( c -> c.accept( sessionFactoryBuilder ) );

			return sessionFactoryBuilder.build();
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( bootstrapServiceRegistry )
					.push( serviceRegistry );
			throw e;
		}
	}

}
