package cz.cuni.mff.d3s.been.benchmark.hazelcast3.common;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.Serializable;
import java.util.Random;

/**
 * @author Martin Sixta
 */
public final class Message implements Serializable {

	private static Random random = new Random();

	private final String clientId;
	private final String md5Hex;
	private final byte[] payload;


	private Message(final String clientId, final byte[] payload) {
		this.payload = payload;
		this.clientId = clientId;
		this.md5Hex = DigestUtils.md5Hex(payload);
	}


	public static Message createRandomMessage(final String clientId, final int size) {
		final byte[] payload = new byte[size];
		random.nextBytes(payload);

		return new Message(clientId, payload);
	}

	public static boolean checkMessage(final Message msg) {
		String md5Hex = DigestUtils.md5Hex(msg.getPayload());

		return md5Hex != null && (md5Hex.equals(msg.getMd5Hex()));

	}

	byte[] getPayload() {
		return payload;
	}

	String getMd5Hex() {
		return md5Hex;
	}

	String getClientId() {
		return clientId;
	}
}
