# test-hazelcast

Small tests with Hazelcast.

## Proposal for async processing

* MDB to read message and write to hazelcast queue.
  * Could write in parallel to hazelcast queue and DB.
  * If queue gets lost (only in memory), we can always scan DB for lost items.
* Queue can be made persistent, various persistence options exist. Only write to queue, nothing to db yet.
  * Simpler solution.
  * XA is possible, see refman [11.2](http://docs.hazelcast.org/docs/3.5/manual/html-single/hazelcast-documentation.html#xa-transactions).
  * However, [local transactions](http://docs.hazelcast.org/docs/3.5/manual/html-single/hazelcast-documentation.html#local-versus-two-phase) + compensation mechanism should do. Duplicates should not be a problem, can be dealt with.
    * No: in 11.1 it is mentioned that MapStore and QueueStore do not participate in transactions. Obvious in a way, since XA is required for that. This should not be a problem.
  * Must test persistent queue, crash all nodes (or too many, resulting in lost partitions), then see if lost data are restored from store, or just silently lost.
* Otherwise, just persist message directly in mdb into db (XA) and do async fetch/process/delete without (expensive).

* Every entry processor call also verifies on the DB:
  * remove work just done.
  * check for lost items (older than the one just done).
  * not necessary if we make a persistent queue.

* Can scale nodes with MDB, with hazelcast and with both as required.
* It may be better to separate, and use hazelcast-client in the MDB's, separate hazelcast nodes for processing.
* Take data partitioning into account to get the processing on the node that contains the data.
* [Execution](http://docs.hazelcast.org/docs/3.5/manual/html-single/hazelcast-documentation.html#execution): `executorService.submitToKeyOwner( task, key );`
  * How do we get the key of the queue entry just submitted?
* Can also use [execution member selection](http://docs.hazelcast.org/docs/3.5/manual/html-single/hazelcast-documentation.html#execution-member-selection).
* Don't need to be notified on task finish; would be possible with [execution callback](http://docs.hazelcast.org/docs/3.5/manual/html-single/hazelcast-documentation.html#execution-callback).

* Define `executor-pool-size` to set the # of threads per JVM/node.
  * Don't overdo, take care that enough capacity remains for WLS itself.
  * Recommend to start testing with 2 threads per available virtual core.
  * Separate MDB and Hazelcast nodes reduce the risk for exhausting resource capacity, garbage collection.
  * But more network communication will result. Hard to predict how significant the difference is.
* Use enough nodes to handle the total load.

* Alternatives/options: 
  * Just use a bunch of message consumers as part of the hazelcast cluster.
    * Use multiple threads/consumers per instance.
  * Use persistent map instead of queue, with entry processor? I don't think it helps in our case.
  * Distributed queries or indexing not required (?).

* How to run dedicated Hazelcast nodes?
  * In WLS or java-SE?
  * Standard but overkill.
  * 
  

* The Runnable, or message consumer, does:
  * Start hz transaction.
  * Read message.
  * Process.
  * Write to DB + commit db.
  * Commit hz.
    * DB write might be double in case of crash. I.e. processing + write to DB must be idempotent.
    * Otherwise, XA would be required including processing that can be rolled back.
    * 
