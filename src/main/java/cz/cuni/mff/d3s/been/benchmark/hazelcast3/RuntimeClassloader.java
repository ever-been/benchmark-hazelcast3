package cz.cuni.mff.d3s.been.benchmark.hazelcast3;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Martin Sixta
 */
final class RuntimeClassloader extends URLClassLoader {
	/**
	 * @param urls, to carryforward the existing classpath.
	 */
	public RuntimeClassloader(URL... urls) {
		super(urls);
	}

}



