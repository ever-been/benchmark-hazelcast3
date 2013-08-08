package cz.cuni.mff.d3s.been.benchmark.hazelcast3.common;

/**
 * @author Martin Sixta
 */
public interface PropertyProvider {
	String getPropertyName();

	Object getDefaultValue();
}
