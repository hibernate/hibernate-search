# ORM 6 integration

## What is this?

This directory contains modules that provide integration between Hibernate Search and Hibernate ORM 6,
as well as their integration tests.

## Where's the source code?

Modules in this directory are generated from the source code of the main modules.

During the build:

* The source code for the original module is copied `target/copied-sources` in the `-orm6` module.
* A set of search-and-replaces are applied in order to adapt the code to Jakarta APIs (instead of Java EE).
* Additional patches are applied to the source code for more complex changes
  that are required because of breaking changes in Hibernate ORM 6.

## What to do if compilation/tests fail?

You probably need to update patches.

First, check out the branch where all the changes related to Hibernate ORM 6 live:
https://github.com/hibernate/hibernate-search/tree/wip/main/dependency-update/orm6-in-main-code

Then:

* Rebase on `main` and fix conflicts as necessary.
* If the last few commits are there to upgrade to the latest snapshot of ORM 6, revert them.
* Try to build that branch as you would usually build Hibernate Search.
  WARNING: Do not forget to also enable the dependency-update profile (`-Pdependency-update`)
  to disable unwanted checks (deprecations, ...).
* Fix compilation/test errors by updating the code as necessary.
* Commit your changes, rebase and squash them with the relevant commit
  (probably the one that upgrades to ORM 6.0.0.Beta3 or something similar).
* Rebase again to remove the "revert" commits that you added earlier.
* Identify the first and last commit that are necessary to upgrade from ORM 5
  to the version of ORM 6 that you want to target.
  Copy their SHA somewhere.
* Checkout the branch where you originally witnessed the compilation failures.
* Execute `./orm6/extract-patches <first commit SHA>~1..<last commit SHA>` from the root of your local git repository.
* Commit the resulting changes.