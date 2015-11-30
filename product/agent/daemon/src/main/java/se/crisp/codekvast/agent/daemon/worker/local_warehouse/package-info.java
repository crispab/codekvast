/**
 * Code dealing with the local warehouse.
 *
 * The local warehouse aggregates data from all collectors reporting to the same daemon.
 *
 * The daemon then prepares an aggregated zip file for upload to the central warehouse where the
 * data is merged with data from daemons executing in other environments.
 */
package se.crisp.codekvast.agent.daemon.worker.local_warehouse;
