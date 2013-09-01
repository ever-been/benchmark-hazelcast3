package cz.cuni.mff.d3s.been.benchmark.hazelcast3.node;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.Message;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.common.SkeletalTask;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.NodeResult;
import cz.cuni.mff.d3s.been.benchmark.hazelcast3.result.Results;
import cz.cuni.mff.d3s.been.mq.MessagingException;
import cz.cuni.mff.d3s.been.persistence.DAOException;
import cz.cuni.mff.d3s.been.taskapi.CheckpointController;
import cz.cuni.mff.d3s.been.taskapi.TaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static cz.cuni.mff.d3s.been.benchmark.hazelcast3.BenchmarkProperty.*;


/**
 * The Task which implements the Hazelcast node.
 *
 * @author Martin Sixta
 */
public class NodeTask extends SkeletalTask {
	private static final Logger log = LoggerFactory.getLogger(NodeTask.class);

	/**
	 * Full class name of Hazelcast ClasspathXmlConfig
	 */
	private static final String CONFIG_CLASS_CLASSPATH = "com.hazelcast.config.ClasspathXmlConfig";

	/**
	 * Full class name of Hazelcast UrlXmlConfig
	 */
	private static final String CONFIG_CLASS_URL = "com.hazelcast.config.UrlXmlConfig";

	/**
	 * Full class name of Hazelcast InMemoryXmlConfig
	 */
	private static final String CONFIG_CLASS_STRING = "com.hazelcast.config.InMemoryXmlConfig";

	/**
	 * Name of default Hazelcast config from resource
	 */
	private static final String CONFIG_DEFAULT_RESOURCE_NAME = "hazelcast-node-config.xml";

	@Override
	public void run(String[] strings) throws TaskException, MessagingException, DAOException {
		log.debug("Starting node '{}'", getProperty("node.id"));

		Path clientJar = retrieveJar(props.getString(JAR_CLIENT));
		Path hazelcastJar = retrieveJar(props.getString(JAR_NODE));

		loadJar(clientJar);
		loadJar(hazelcastJar);

		runNode();


		log.debug("Finishing node");

	}

	private void runNode() throws TaskException, MessagingException, DAOException {

		Config cfg = createConfig();

		HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);


		IQueue<Message> queue = instance.getQueue(props.getString(HAZELCAST_QUEUE_NAME));

		long msgReceivedCnt = 0;
		long msgCorruptedCnt = 0;
		boolean isFirstMsg = true;
		long firstMsgTime = 0;
		long lastMsgTime = 0;
		long startRetries = 2;

		boolean checkMessage = props.getBoolean(NODE_CHECK_MESSAGE);

		setAddressCheckPoint(instance);


		try (CheckpointController checkpoint = CheckpointController.create()) {
			checkpoint.latchCountDown(NODE_LATCH.getPropertyName());
			checkpoint.latchWait(NODE_LATCH.getPropertyName());
		}


		try {
			while (true) {
				// TODO consider timeout as a parameter
				Message msg = queue.poll(10, TimeUnit.SECONDS);
				if (msg == null) {

					if (isFirstMsg && --startRetries > 0) {
						log.debug("Waiting for work");
						continue;
					} else if (!isFirstMsg) {
						break;
					} else {
						log.debug("Giving up");
						return;
					}
				}

				if (isFirstMsg) {
					firstMsgTime = System.nanoTime();
					isFirstMsg = false;
				}

				++msgReceivedCnt;

				if (checkMessage) {
					// "simulate work"
					if (!Message.checkMessage(msg)) {
						++msgCorruptedCnt;
					}
				}

				lastMsgTime = System.nanoTime();

			}


		} catch (InterruptedException e) {
			log.error("No value polled", e);
		} finally {
			closeHazelcastInstance(instance);
		}

		final long elapsed = lastMsgTime - firstMsgTime;
		final String nodeId = props.getString(NODE_ID);

		NodeResult result = Results.createNodeResult(props, elapsed, msgReceivedCnt);
		log.debug("{} finished in {}s", nodeId, TimeUnit.SECONDS.convert(elapsed, TimeUnit.NANOSECONDS));


		store(result, GROUP);

	}


	private Config createConfig() throws TaskException {

		String configString = props.getString(HAZELCAST_NODE_CONFIG);
		String configUrl = props.getString(HAZELCAST_NODE_CONFIG_URL);

		String className;
		String constructorParameter;

		if (!configUrl.isEmpty()) {
			className = CONFIG_CLASS_URL;
			constructorParameter = configUrl;
		} else if (!configString.isEmpty()) {
			className = CONFIG_CLASS_STRING;
			constructorParameter = configString;
		} else {
			className = CONFIG_CLASS_CLASSPATH;
			constructorParameter = CONFIG_DEFAULT_RESOURCE_NAME;
		}


		Config cfg;
		try {
			final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			final Class<?> classpathConfig = classLoader.loadClass(className);
			final Constructor<?> constructor = classpathConfig.getConstructor(String.class);
			cfg = (Config) constructor.newInstance(constructorParameter);
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			log.error("Cannot load Hazelcast config", e);
			throw new TaskException("Cannot load Hazelcast config", e);
		}

		cfg.setProperty("hazelcast.system.log.enabled", "false");
		cfg.setProperty("hazelcast.logging.type", "none");
		cfg.getGroupConfig().setName(props.getString(HAZELCAST_GROUP));
		cfg.getGroupConfig().setPassword(props.getString(HAZELCAST_PASSWORD));

		return cfg;

	}

	private void setAddressCheckPoint(final HazelcastInstance instance) throws TaskException {
		String addressProperty = String.format("hazelcast.%s.address", getProperty("node.id"));
		InetSocketAddress inetAddress = instance.getCluster().getLocalMember().getInetSocketAddress();
		String address = String.format("%s:%s", inetAddress.getHostString(), inetAddress.getPort());


		try (CheckpointController checkpoint = CheckpointController.create()) {
			checkpoint.checkPointSet(addressProperty, address);
		} catch (MessagingException e) {
			throw new TaskException("Cannot set " + addressProperty + " check point", e);
		}


	}

}
