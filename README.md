# test-hazelcast

Small tests with Hazelcast.

## Proposal for async processing

* MDB to read message and write to hazelcast queue.
* queue can be made persistent, various persitence options possible. 
  * unknown how to do this in xa.
* Otherwise, just persist message directly in mdb into db (XA) and do async fetch/process/delete without (expensive).
  
