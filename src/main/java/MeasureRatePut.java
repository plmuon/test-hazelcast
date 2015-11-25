import java.util.Timer;
import java.util.TimerTask;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class MeasureRatePut extends TimerTask {
	private static long count;
	private long lastCount = 0;
	private Runtime runtime = Runtime.getRuntime();

	@Override
	public void run() {
		System.out.printf("%12d%12d\n", count - lastCount, runtime.totalMemory());
		lastCount = count;
	}

	public static void main(String args[]) {
		TimerTask timerTask = new MeasureRatePut();
		// running timer task as daemon thread
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(timerTask, 0, 1000);

		Config config = new Config();
		HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
		IMap<Long, String> test = hazelcastInstance.getMap("test");

		StringBuilder sb = new StringBuilder();
		// 32 bytes times 32 is 1k. make 1k values
		for (int k = 0; k < 1; k++)
			for (int i = 0; i < 32; i++)
				sb.append("abcdefghijklmnbop");
		String fixed = sb.toString();

		for (count = 0; count < Long.MAX_VALUE; count++) {
			long b = count % 10000;
			test.put(b, fixed + b);
		}
	}
}
