# test-hazelcast

Small tests with Hazelcast.

## Proposal for async processing

* MDB to read message and write to hazelcast queue.
  * Could write in parallel to hazelcast queue and DB.
  * If queue gets lost (only in memory), we can always scan DB for lost items.
* queue can be made persistent, various persitence options possible.
  * unknown how to do this in xa.
* Otherwise, just persist message directly in mdb into db (XA) and do async fetch/process/delete without (expensive).

* Every entry processor call also verifies on the DB:
  * remove work just done.
  * check for lost items (older than the one just done).
  * 
  
* Can scale nodes with MDB, with hazelcast and with both as required.
* It may be better to separate, and use hazelcast-client in the MDB's, separate hazelcast nodes for processing.
* Take data partitioning into account to get the processing on the node that contains the data.
