/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.orm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.service.ServiceRegistry;

public final class SimpleSessionFactoryBuilder {
	private final StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
	private final List<Consumer<MetadataSources>> metadataSourcesContributors = new ArrayList<>();
	private final List<Consumer<SessionFactoryBuilder>> sessionFactoryBuilderContributors = new ArrayList<>();

	public SimpleSessionFactoryBuilder setProperty(String key, Object value) {
		registryBuilder.applySetting( key, value );
		return this;
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

	public SimpleSessionFactoryBuilder onServiceRegistryBuilder(Consumer<StandardServiceRegistryBuilder> contributor) {
		contributor.accept( registryBuilder );
		return this;
	}

	public SimpleSessionFactoryBuilder onMetadataSources(Consumer<MetadataSources> contributor) {
		metadataSourcesContributors.add( contributor );
		return this;
	}

	public SimpleSessionFactoryBuilder onSessionFactoryBuilder(Consumer<SessionFactoryBuilder> contributor) {
		sessionFactoryBuilderContributors.add( contributor );
		return this;
	}

	public SessionFactory build() {
		ServiceRegistry serviceRegistry = null;
		MetadataSources metadataSources;
		SessionFactoryBuilder sessionFactoryBuilder;

		try {
			serviceRegistry = registryBuilder.build();

			metadataSources = new MetadataSources( serviceRegistry );
			metadataSourcesContributors.forEach( c -> c.accept( metadataSources ) );
			Metadata metadata = metadataSources.buildMetadata();

			sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
			sessionFactoryBuilderContributors.forEach( c -> c.accept( sessionFactoryBuilder ) );

			return sessionFactoryBuilder.build();
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( ServiceRegistry::close, serviceRegistry );
			throw e;
		}
	}

}
