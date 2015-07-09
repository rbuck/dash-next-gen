package com.nuodb.dash.services;

/**
 * A marker interface for per thread context.
 * <p/>
 * It is frequently advantageous to demonstrate a high degree of concurrent
 * load against large data set sizes. But some challenges to this include high
 * degrees of conflict, either client side or server side, as well as finding
 * ways to systematically pick records pseudo-randomly to perform a simulated
 * user action against without requiring record keys be kept in memory.
 * <p/>
 * There are a variety of approaches to implement these, it is the purpose of
 * the Context class to hide the details of implementation defined approaches
 * to the models and simulators written.
 * <p/>
 * A common model employed herein are as follows:
 * <p/>
 * - first, with regards to attaining high degrees of concurrency, the database
 * is partitioned by tenant (account); to prevent conflict between threads caused
 * by each operating in the same tenant, each thread has an associated Context
 * that defines a unique key-range prefix statically allocated at startup; each
 * thread may operate in its own key range, and each key range supports multiple
 * tenants; never are client threads or server side transactions in conflict.
 * <p/>
 * - second, with regards to pseudo-randomly selecting records to act against,
 * the model employed implements rand columns within the database which permit
 * use of a simple trick to randomly select records -- that is to select a record
 * whose rand column naturally sorts as the next most greater record than a
 * given randomly generated value.
 * <p/>
 * - lastly, because the Context is permissively stateful, transitions between
 * states are possible, so simulating a user workflow is elementary.
 */
public interface Context {
}
