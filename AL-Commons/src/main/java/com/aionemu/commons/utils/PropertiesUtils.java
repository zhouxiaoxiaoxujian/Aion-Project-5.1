/*
 * This file is part of aion-emu <aion-emu.com>.
 *
 * aion-emu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aion-emu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import sun.misc.Launcher;

/**
 * This class is designed to simplify routine job with properties
 * 
 * @author SoulKeeper
 */
public class PropertiesUtils {

	/**
	 * Loads properties by given file
	 * 
	 * @param path
	 *          filename
	 * @return loaded properties
	 * @throws java.io.IOException
	 *           if can't load file
	 */
	public static Properties load(String path) throws IOException {
		File file = new File(path);

		if (file.exists() == false) {
			//try load file from classpath
			InputStream inputStream = PropertiesUtils.class.getResourceAsStream(path);
			return load(inputStream);
		} else {
			return load(file);
		}
	}

	public static  Properties load(InputStream inputStream) throws  IOException {
		Properties p = new Properties();
		p.load(inputStream);
		inputStream.close();
		return p;
	}

	/**
	 * Loads properties by given file
	 * 
	 * @param file
	 *          filename
	 * @return loaded properties
	 * @throws java.io.IOException
	 *           if can't load file
	 */
	public static Properties load(File file) throws IOException {
		return load(new FileInputStream(file));
	}

	/**
	 * Loades properties from given files
	 * 
	 * @param files
	 *          list of string that represents files
	 * @return array of loaded properties
	 * @throws IOException
	 *           if was unable to read properties
	 */
	public static Properties[] load(String... files) throws IOException {
		Properties[] result = new Properties[files.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = load(files[i]);
		}
		return result;
	}

	/**
	 * Loades properties from given files
	 * 
	 * @param files
	 *          list of files
	 * @return array of loaded properties
	 * @throws IOException
	 *           if was unable to read properties
	 */
	public static Properties[] load(File... files) throws IOException {
		Properties[] result = new Properties[files.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = load(files[i]);
		}
		return result;
	}

	/**
	 * Loads non-recursively all .property files form directory
	 * 
	 * @param dir
	 *          string that represents directory
	 * @return array of loaded properties
	 * @throws IOException
	 *           if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(String dir) throws IOException {

		ArrayList<Properties> propertiesLoaded = new ArrayList<>();

		CodeSource codeSource = Properties.class.getProtectionDomain().getCodeSource();

		if (codeSource != null) {
			final File jarFile = new File(codeSource.getLocation().getPath());

			if(jarFile.isFile()) {  // Run with JAR file
				final JarFile jar = new JarFile(jarFile);
				final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
				while(entries.hasMoreElements()) {
					final String name = entries.nextElement().getName();
					if (name.startsWith(dir + "/")) { //filter according to the path
						propertiesLoaded.add(load(name));
					}
				}
				jar.close();
		}
		} else { // Run with IDE
			final URL url = Launcher.class.getResource("/" + dir);
			if (url != null) {
				try {
					final File apps = new File(url.toURI());
					for (File app : apps.listFiles()) {
						propertiesLoaded.add(load(app));
					}
				} catch (URISyntaxException ex) {
					// never happens
				}
			}
		}
		return propertiesLoaded.toArray(new Properties[0]);
	}

	/**
	 * Loads non-recursively all .property files form directory
	 * 
	 * @param dir
	 *          directory
	 * @return array of loaded properties
	 * @throws IOException
	 *           if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(File dir) throws IOException {
		return loadAllFromDirectory(dir, false);
	}

	/**
	 * Loads all .property files form directory
	 * 
	 * @param dir
	 *          string that represents directory
	 * @param recursive
	 *          parse subdirectories or not
	 * @return array of loaded properties
	 * @throws IOException
	 *           if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(String dir, boolean recursive) throws IOException {

		File file = new File(dir);
		if (file.exists() == false) {
			// try load file from classpath
			URL dirPath = PropertiesUtils.class.getResource(dir);
			file = new File(dirPath.getPath());
		}
		return loadAllFromDirectory(file, recursive);
	}

	/**
	 * Loads all .property files form directory
	 * 
	 * @param dir
	 *          directory
	 * @param recursive
	 *          parse subdirectories or not
	 * @return array of loaded properties
	 * @throws IOException
	 *           if was unable to read properties
	 */
	public static Properties[] loadAllFromDirectory(File dir, boolean recursive) throws IOException {
		Collection<File> files = FileUtils.listFiles(dir, new String[] { "properties" }, recursive);
		return load(files.toArray(new File[files.size()]));
	}

	/**
	 * All initial properties will be overriden with properties supplied as second argument
	 * 
	 * @param initialProperties
	 *          to be overriden
	 * @param properties
	 * @return merged properties
	 */
	public static Properties[] overrideProperties(Properties[] initialProperties, Properties[] properties) {
		if (properties != null) {
			for (Properties props : properties) {
				overrideProperties(initialProperties, props);
			}
		}
		return initialProperties;
	}

	/**
	 * All initial properties will be overriden with properties supplied as second argument
	 * 
	 * @param initialProperties
	 * @param properties
	 * @return
	 */
	public static Properties[] overrideProperties(Properties[] initialProperties, Properties properties) {
		if (properties != null) {
			for (Properties initialProps : initialProperties) {
				initialProps.putAll(properties);
			}
		}
		return initialProperties;
	}


	public  static  Properties overrideProperty(Properties initialProperty, Properties properties) {
		if (properties != null) {
			initialProperty.putAll(properties);
		}
		return initialProperty;
	}
}
