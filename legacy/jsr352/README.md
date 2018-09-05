# Hibernate Search JSR-352 integration

## Purpose

This module provides a JSR-352 batch job executing a mass indexing.

The main advantage over the classic `fullTextSession.createIndexer(Class<?>)`
is that batch jobs can be suspended and resumed on demand,
and even be resumed after a failure.

## Main components

### `MassIndexingJob`

Provides the mass indexing job name, and a builder of parameters for a mass indexing job

### `EntityReader`

Reads entities from the database.

### `LuceneDocProducer`

Takes entities from `EntityReader` as an input,
and transforms them to documents.

### `LuceneDocWriter`

Takes documents from `LuceneDocProducer` as an input,
and writes them to the index.

### `PartitionMapper`

Creates a partition plan based on the job parameters.

### `JobContextData`

Contains the main context accessible from most job components,
mainly the `EntityManagerFactory` and `SearchIntegrator`.

Access to this component is a bit unusual,
because the only state the JSR-352 spec allows is strings injected into components
and a object which can be serialized (not ideal for `EntityManagerFactory`)
and may be reset during the job process (when starting a partition for instance).  

To work around those limitations, each component requiring access
to the `JobContextData` is injected with all the information needed to rebuild it,
and will use a utility (`JobContextUtil`) to either retrieve the current instance
(if possible) or rebuild it (if necessary).

### `EntityManagerFactoryRegistry`

Retrieves an entity manager factory for use during the job execution.

This component has two default implementations in the core,
one relying on a static registry populated using a Hibernate Integrator,
and one relying on JNDI.

Another implementation is 

### `JobContextSetupListener`

Validates the job parameters and the `EntityManagerFactory` retrieval
at the start of the job execution.

## Workflow

For information about how a mass indexing job is executed,
please refer to [the JSR-352 specification](https://jcp.org/en/jsr/detail?id=352),
in particular section 11.7 ("Partitioned Chunk Processing").
