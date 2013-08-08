package cz.cuni.mff.d3s.been.benchmark.hazelcast3.result;

import cz.cuni.mff.d3s.been.results.Result;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author Martin Sixta
 */
public final class FileResult extends Result {
	String name;
	byte[] content;
	String md5Hex;

	FileResult() {
		//makes Jackson happy
	}

	FileResult(String name, byte[] content) {
		this.name = name;
		this.content = content;
		this.md5Hex = DigestUtils.md5Hex(content);
	}


	public String getName() {
		return name;
	}

	public byte[] getContent() {
		return content;
	}

	public String getMd5Hex() {
		return md5Hex;
	}


}
