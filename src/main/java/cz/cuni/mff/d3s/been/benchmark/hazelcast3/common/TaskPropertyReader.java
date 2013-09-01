package cz.cuni.mff.d3s.been.benchmark.hazelcast3.common;

import cz.cuni.mff.d3s.been.util.PropertyReader;

/**
 * @author Martin Sixta
 */
public final class TaskPropertyReader {
	private final PropertyReader propertyReader;

	public TaskPropertyReader(PropertyReader propertyReader) {
		this.propertyReader = propertyReader;
	}


	/**
	 * Get a {@link String} from properties
	 * <p/>
	 * Default value is returned if the property cannot be found
	 *
	 * @return String value of wanted property
	 */
	public String getString(PropertyProvider provider) {
		return propertyReader.getString(provider.getPropertyName(), provider.getDefaultValue().toString());
	}

	public String getString(String name, String defaultValue) {
		return propertyReader.getString(name, defaultValue);
	}

	/**
	 * Get an {@link Integer} from properties
	 * <p/>
	 * The default value will be returned if
	 * <ul>
	 * <li>the system property has no value associated with it</li>
	 * <li>the value cannot be parsed as an integer</li>
	 * </ul>
	 *
	 * @return Integer value of wanted property
	 */
	public Integer getInteger(PropertyProvider provider) {
		if (provider.getDefaultValue() instanceof Integer) {
			return propertyReader.getInteger(provider.getPropertyName(), (Integer) provider.getDefaultValue());
		} else {
			throw new IllegalArgumentException("Wrong defaultValue type!");
		}

	}


	/**
	 * Get a {@link Long} from properties
	 * <p/>
	 * The default value will be returned if
	 * <ul>
	 * <li>the system property has no value associated with it</li>
	 * <li>the value cannot be parsed as a long</li>
	 * </ul>
	 *
	 * @return Long value of wanted property
	 */
	public Long getLong(PropertyProvider provider) {
		if (provider.getDefaultValue() instanceof Long) {
			return propertyReader.getLong(provider.getPropertyName(), (Long) provider.getDefaultValue());
		} else {
			throw new IllegalArgumentException("Wrong defaultValue type!");
		}
	}

	public Boolean getBoolean(PropertyProvider provider) {
		if (provider.getDefaultValue() instanceof Boolean) {
			return propertyReader.getBoolean(provider.getPropertyName(), (Boolean) provider.getDefaultValue());
		} else {
			throw new IllegalArgumentException("Wrong defaultValue type!");
		}
	}


}
