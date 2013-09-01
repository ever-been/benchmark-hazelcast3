package cz.cuni.mff.d3s.been.benchmark.hazelcast3.client;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.Message;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.SkeletalTask;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.ClientResult;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.Results;
import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;

/**
 * @author Martin Sixta
 */
public class ClientTask extends SkeletalTask {
	private static final Logger log = LoggerFactory.getLogger(ClientTask.class);

	@Override
	public void run(String[] strings) throws TaskException, DAOException, MessagingException {
		log.debug("Starting client");

		Path clientJar = retrieveJar(props.getString(JAR_CLIENT));
		Path hazelcastJar = retrieveJar(props.getString(JAR_NODE));

		loadJar(clientJar);
		loadJar(hazelcastJar);

		runClient();

		log.debug("Finishing client");

	}

	private void runClient() throws TaskException {
		ClientConfig cfg = new ClientConfig();

		try (CheckpointController checkpoint = CheckpointController.create()) {
			checkpoint.latchWait(NODE_LATCH.getPropertyName(), 60000);
			String address = checkpoint.checkPointWait("hazelcast.node01.address", 60000);

			cfg.addAddress(address);

			cfg.getGroupConfig().setName(props.getString(HAZELCAST_GROUP));
			cfg.getGroupConfig().setPassword(props.getString(HAZELCAST_PASSWORD));

			log.info("Client address: {}", address);
			System.setProperty("hazelcast.system.log.enabled", "false");
			System.setProperty("hazelcast.logging.type", "none");

		} catch (MessagingException e) {
			log.error("Cannot set checkpoint");
			return;
		} catch (TimeoutException e) {
			log.error("No address to connect ...", e);
			return;
		}

		final String queueName = props.getString(HAZELCAST_QUEUE_NAME);

		final String clientId = props.getString("client.id", "");
		assert (!clientId.equals(""));

		int msgSize = props.getInteger(HAZELCAST_MSG_SIZE);
		int msgCnt = props.getInteger(HAZELCAST_CLIENT_MSG_COUNT);


		HazelcastInstance instance = HazelcastClient.newHazelcastClient(cfg);

		IQueue<Message> queue = instance.getQueue(queueName);

		final Message msg = Message.createRandomMessage(clientId, msgSize);


		final long start = System.currentTimeMillis();
		try {


			for (int i = 0; i < msgCnt; ++i) {
				queue.put(msg);
			}

		} catch (InterruptedException e) {
			log.error("Bum", e);
			return;
		} finally {
			instance.getLifecycleService().terminate();
		}

		final long end = System.currentTimeMillis();


		// Store results
		final long elapsed = end - start;

		ClientResult result = Results.createClientResult(props, elapsed, msgCnt);

		log.debug("{} finished in {}s", clientId, TimeUnit.SECONDS.convert(elapsed, TimeUnit.NANOSECONDS));


		try {
			store(result, GROUP);
		} catch (DAOException e) {
			log.error("Cannot persist client's result", e);
		}

	}

}
