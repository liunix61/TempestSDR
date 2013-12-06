package martin.tempest.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import martin.tempest.core.exceptions.TSDRException;
import martin.tempest.core.exceptions.TSDRLibraryNotCompatible;

/**
 * This is a Java wrapper library for TSDRLibrary
 * 
 * @author Martin
 *
 */
public class TSDRLibrary {
	
	private static TSDRSource[] PLUGINS = new TSDRSource[] {new TSDRSource("TSDRPlugin_RawFile")};
	
	// If the binaries weren't loaded, this will go off
	private static TSDRLibraryNotCompatible m_e = null;
	
	/**
	 * Extracts a library to a temporary path and prays for the OS to delete it after the app closes.
	 * @param name
	 * @return
	 * @throws IOException
	 */
	static final File extractLibrary(final String name) throws TSDRLibraryNotCompatible {
		final String rawOSNAME = System.getProperty("os.name").toLowerCase();
		final String rawARCHNAME = System.getProperty("os.arch").toLowerCase();

		String OSNAME = null, EXT = null, ARCHNAME = null;

		if (rawOSNAME.contains("win")) {
			OSNAME = "WINDOWS";
			EXT = ".dll";
		} else if (rawOSNAME.contains("nix") || rawOSNAME.contains("nux") || rawOSNAME.contains("aix")) {
			OSNAME = "LINUX";
			EXT = ".so";
		} else if (rawOSNAME.contains("mac")) {
			OSNAME = "MAC";
			EXT = ".a";
		}

		if (rawARCHNAME.contains("arm"))
			ARCHNAME = "ARM";
		else if (rawARCHNAME.contains("64"))
			ARCHNAME = "X64";
		else
			ARCHNAME = "X86";

		if (OSNAME == null || EXT == null || ARCHNAME == null)
			throw new TSDRLibraryNotCompatible("Your OS or CPU is not yet supported, sorry.");

		final String relative_path = "lib/"+OSNAME+"/"+ARCHNAME+"/"+name+EXT;

		InputStream in = TSDRLibrary.class.getClassLoader().getResourceAsStream(relative_path);

		if (in == null)
			try {
				in = new FileInputStream(relative_path);
			} catch (FileNotFoundException e) {}

		if (in == null) throw new TSDRLibraryNotCompatible("The library has not been compiled for your OS/Architecture yet ("+OSNAME+"/"+ARCHNAME+").");

		File temp;
		try {
			byte[] buffer = new byte[in.available()];

			int read = -1;
			temp = new File(System.getProperty("java.io.tmpdir"), name+EXT);
			temp.deleteOnExit();
			final FileOutputStream fos = new FileOutputStream(temp);

			while((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();
		} catch (IOException e) {
			throw new TSDRLibraryNotCompatible(e);
		}

		return temp;
	}

	/**
	 * Loads a dll library on the fly based on OS and ARCH. Do not supply extension.
	 * @param name
	 * @throws IOException 
	 */
	static final void loadLibrary(final String name) throws TSDRLibraryNotCompatible {
		try {
			// try traditional method
			System.loadLibrary(name); 
		} catch (Throwable t) {
				final File library = extractLibrary(name);
				System.load(library.getAbsolutePath());
				library.delete();
		}
	}

	/**
	 * Load the libraries statically and detect errors
	 */
	static {
		try {
			loadLibrary("TSDRLibrary");
			loadLibrary("TSDRLibraryNDK");
			
		} catch (TSDRLibraryNotCompatible e) {
			m_e = e;
		} 
	}
	
	public TSDRLibrary() throws TSDRLibraryNotCompatible {
		if (m_e != null) throw m_e;
	}

	private native void nativeLoadPlugin(String filepath) throws TSDRException;
	public native void pluginParams(String params) throws TSDRException;
	public native void setSampleRate(long rate) throws TSDRException;
	public native void setBaseFreq(long freq) throws TSDRException;
	public native void start() throws TSDRException;
	public native void stop() throws TSDRException;
	public native void setGain(float gain) throws TSDRException;
	public native void readAsync() throws TSDRException;
	public native void unloadPlugin() throws TSDRException;
	
	public void loadSource(final TSDRSource plugin) throws TSDRException {
		nativeLoadPlugin(extractLibrary(plugin.libname).getAbsolutePath());
	}
	
	public static TSDRSource[] getAllSources() {
		return PLUGINS;
	}
	
}
