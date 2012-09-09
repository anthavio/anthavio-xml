/**
 * 
 */
package com.anthavio.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthavio.util.ResourceUtil;

/**
 * @author vanek
 *
 */
public abstract class JaxpAbstractFactory<T> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String factoryClassName;

	private final Constructor<T> factoryConstructor;

	private boolean factoryCached = true;

	//cached

	private T factory;

	public JaxpAbstractFactory() {
		this.factoryClassName = buildDefaultFactory().getClass().getName();
		this.factoryConstructor = init();
	}

	public JaxpAbstractFactory(String factoryClassName) {
		this.factoryClassName = factoryClassName;
		this.factoryConstructor = init();
	}

	private Constructor<T> init() {
		Class<T> clazz;
		try {
			clazz = (Class<T>) Class.forName(getFactoryClassName(), true, Thread.currentThread().getContextClassLoader());
			log.debug("Loaded " + factoryClassName + " from " + ResourceUtil.which(clazz));
		} catch (ClassNotFoundException cnfx) {
			throw new JaxpConfigException(cnfx);
		}
		Constructor<T> constructor;
		try {
			constructor = clazz.getConstructor();
		} catch (SecurityException sex) {
			throw new JaxpConfigException(sex);
		} catch (NoSuchMethodException nsmx) {
			throw new JaxpConfigException(nsmx);
		}
		return constructor;
	}

	protected abstract T buildDefaultFactory();

	public T getFactory() {
		if (factoryCached) {
			if (this.factory == null) {
				//instantiate
				this.factory = newFactoryInstance();
				//configure
				configureFactoryInstance(this.factory);
			}
			return this.factory;
		} else {
			//non cached factory
			T factory = newFactoryInstance();
			configureFactoryInstance(factory);
			return factory;
		}
	}

	protected abstract void configureFactoryInstance(T factory);

	protected final T newFactoryInstance() {

		T factory;
		try {
			factory = factoryConstructor.newInstance();
		} catch (IllegalArgumentException iax) {
			throw new JaxpConfigException(iax);
		} catch (IllegalAccessException iax) {
			throw new JaxpConfigException(iax);
		} catch (InvocationTargetException itx) {
			throw new JaxpConfigException(itx.getCause());
		} catch (InstantiationException itx) {
			throw new JaxpConfigException(itx);
		}
		return factory;
	}

	/**
	 * Once created and cached, factory must NOT be reconfigured
	 */
	protected void checkState() {
		if (factoryCached && this.factory != null) {
			throw new IllegalStateException("Cached factory is already created");
		}
	}

	public String getFactoryClassName() {
		return factoryClassName;
	}

	public boolean isFactoryCached() {
		return factoryCached;
	}

	public void setFactoryCached(boolean factoryCached) {
		checkState();
		this.factoryCached = factoryCached;
	}
}
