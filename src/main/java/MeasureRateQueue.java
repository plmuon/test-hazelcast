import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.QueueStore;

public class MeasureRateQueue extends TimerTask {
	private static final long MAPSIZE = 10000;
	private static final int VSIZE = 1024;

	private long count;
	private long lastCount = 0;
	private String fixed;
	
	private HazelcastInstance hz;
	private IQueue<String> test;
	
	@Override
	public void run() {
		// System.out.printf("%12d%12d\n", count - lastCount,
		// Runtime.getRuntime().totalMemory());
		System.out.printf("%,12d %,12d\n", count - lastCount, test.size());
		lastCount = count;
	}

	private void init() throws InterruptedException {
		System.setProperty("java.util.logging.config.file", "logging.properties");

		Config config = new Config();
		QueueConfig qc = config.getQueueConfig("test");
		qc.setMaxSize(100000);
		QueueStoreConfig queueStoreConfig = new QueueStoreConfig();
		queueStoreConfig.setStoreImplementation(new QueueStore<String>() {

			@Override
			public void store(Long key, String value) {
				System.out.printf("store key %d\n", key);
			}

			@Override
			public void storeAll(Map<Long, String> map) {
				System.out.printf("storeAll\n");				
			}

			@Override
			public void delete(Long key) {
				System.out.printf("delete key %d\n", key);
			}

			@Override
			public void deleteAll(Collection<Long> keys) {
				System.out.printf("deleteAll key\n");				
			}

			@Override
			public String load(Long key) {
				System.out.printf("load key %d\n", key);
				return null;
			}

			@Override
			public Map<Long, String> loadAll(Collection<Long> keys) {
				System.out.printf("loadAll\n");
				return null;
			}

			@Override
			public Set<Long> loadAllKeys() {
				System.out.printf("loadAllKeys\n");
				return null;
			}
		});
		qc.setQueueStoreConfig(queueStoreConfig);
		hz = Hazelcast.newHazelcastInstance(config);
		test = hz.getQueue("test");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < VSIZE / 32; i++)
			sb.append("abcdefghijklmnbop");

		fixed = sb.toString();
	}

	private void produce() throws InterruptedException {
		for (count = 0; count < Long.MAX_VALUE; count++) {
			test.put(fixed);
		}
	}

	private void consume() throws InterruptedException {
		for (count = 0; count < Long.MAX_VALUE; count++) {
			String s = test.take();
			if (s == null) {
				System.out.println("got null");
			}
		}
	}

	public static void main(String args[]) throws InterruptedException {
		MeasureRateQueue measure = new MeasureRateQueue();
		measure.init();

		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(measure, 0, 1000);

		if (args.length == 0)
			measure.produce();
		else
			measure.consume();
	}
}
