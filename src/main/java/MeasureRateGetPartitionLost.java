import java.util.Timer;
import java.util.TimerTask;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.MapPartitionLostEvent;
import com.hazelcast.map.listener.MapPartitionLostListener;

public class MeasureRateGetPartitionLost extends TimerTask {
	private static final long MAPSIZE = 10000;
	private static final int VSIZE = 1024;

	private long count;
	private long lastCount = 0;

	private HazelcastInstance hz;

	@Override
	public void run() {
		// System.out.printf("%12d%12d\n", count - lastCount,
		// Runtime.getRuntime().totalMemory());
		System.out.printf("%,12d\n", count - lastCount);
		lastCount = count;
	}

	private void init() {
		System.setProperty("java.util.logging.config.file", "logging.properties");

		MapConfig mapConfig = new MapConfig();
		mapConfig.setName("test");
		mapConfig.setBackupCount(0);
		mapConfig.setAsyncBackupCount(0);

		Config config = new Config();
		config.addMapConfig(mapConfig);
		hz = Hazelcast.newHazelcastInstance(config);
		IMap<Long, String> test = hz.getMap("test");

		test.addPartitionLostListener(new MapPartitionLostListener() {
			@Override
			public void partitionLost(MapPartitionLostEvent event) {
				System.out.println(event);
			}
		});

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < VSIZE / 32; i++)
			sb.append("abcdefghijklmnbop");

		String fixed = sb.toString();

		for (count = 0; count < MAPSIZE; count++) {
			long b = count % MAPSIZE;
			test.put(b, fixed + b);
		}
	}

	private void doit() {
		IMap<Long, String> test = hz.getMap("test");
		for (count = 0; count < Long.MAX_VALUE; count++) {
			long b = count % MAPSIZE;
			String s = test.get(b);
			if (s == null) {
				System.out.printf("key %d missing\n", b);
			}
		}
	}

	public static void main(String args[]) {
		MeasureRateGetPartitionLost measure = new MeasureRateGetPartitionLost();
		measure.init();

		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(measure, 0, 1000);

		measure.doit();
	}
}
