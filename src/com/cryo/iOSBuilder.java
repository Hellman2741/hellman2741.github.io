package com.cryo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;

public class iOSBuilder {

	private ArrayList<DebPkg> packages;

	private HashMap<String, ArrayList<String>> app_versions;

	private ArrayList<String> builder;

	private Scanner scanner;

	private String[][] CHECKSUMS;

	private int[] lengths;

	public static final int MD5 = 0, SHA1 = 1, SHA256 = 2;

	//look through /pkgfiles for new .deb
	//find new .deb, get new MD5 for it.
	//add all new packages with md5s to Packages
	//zip Packages with 7zip to bzip2
	//get MD5, SHA1, SHA-256 of Packages
	//edit Release, copy from Release_, change encryption values

	public void load() {
		packages = new ArrayList<>();
		app_versions = new HashMap<>();
		scanner = new Scanner(System.in);
		loadVersions();
		loadTemplate("Packages_");
		File dir = new File("./pkgfiles");
		for(File file : dir.listFiles()) {
			if(!file.getName().endsWith(".deb"))
				continue;
			String[] data = getData(file);
			String codename = data[0];
			String version = data[1];
			if(!app_versions.containsKey(codename))
				addToBeBuilt(new DebPkg(codename, version, file));
			else {
				ArrayList<String> versions = app_versions.get(codename);
				if(versions == null || !versions.contains(version))
					addToBeBuilt(new DebPkg(codename, version, file));
			}
		}
		System.out.println(packages.size()+" new tweaks added to be built!");
		build();
		compressPackages();
		buildChecksums();
		loadTemplate("Release_");
		writeRelease();
		scanner.close();
	}

	public void build() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("Packages", true));
			for(int i = 0; i < packages.size(); i++) {
				DebPkg deb = packages.get(i);
				if(deb == null)
					continue;
				if(i != 0) writer.newLine();
				for(String parameter : builder) {
					writer.write(deb.replaceInfo(parameter));
					writer.newLine();
				}
				addVersion(deb.codename, deb.version);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeRelease() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("Release", true));
			for(String parameter : builder) {
				String[] data = parameter.split(" ");
				for(int i = 0; i < data.length; i++) {
					String s = data[i];
					if(s.contains("%")) {
						if(s.contains("size")) {
							String size = s.split("_")[0];
							if(size.equals("%compress")) data[i] = Integer.toString(lengths[1]);
							else if(size.equals("%uncompress")) data[i] = Integer.toString(lengths[0]);
							continue;
						}
						String[] checksum = s.split("-");
						String compression = checksum[0];
						int index = compression.equals("%uncompress") ? 0 : 1;
						String digest = checksum[1];
						switch(digest) {
						case "md5%":
							data[i] = CHECKSUMS[MD5][index];
							break;
						case "sha1%":
							data[i] = CHECKSUMS[SHA1][index];
							break;
						case "sha256%":
							data[i] = CHECKSUMS[SHA256][index];
							break;
						}
						continue;
					}
				}
				StringBuilder line = new StringBuilder();
				boolean first = true;
				for(int i = 0; i < data.length; i++) {
					if(parameter.contains("Packages") && first) {
						line.append(" ");
						first = false;
					}
					line.append(data[i]);
					if(i != data.length - 1) {
						line.append(" ");
					} else
						first = true;
				}
				writer.write(line.toString());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void buildChecksums() {
		File uncompress = new File("Packages");
		File compress = new File("Packages.bz2");
		CHECKSUMS = new String[3][2];
		for(int i = 0; i < 3; i++) {
			CHECKSUMS[i][0] = buildHex(uncompress, i);
			CHECKSUMS[i][1] = buildHex(compress, i);
		}
		lengths = new int[2];
		lengths[0] = (int) uncompress.length();
		lengths[1] = (int) compress.length();
	}

	public void compressPackages() {
		File file = new File("Packages");
		try {

			byte[] bytes;

			bytes = IOUtils.toByteArray(new FileInputStream(file));

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();

			InputStream input = new ByteArrayInputStream(bytes);
			BZip2CompressorOutputStream output = new BZip2CompressorOutputStream(baos);

			Streams.copy(input, output, false);

			output.close();
			input.close();

		   final byte[] compressedByteArray = baos.toByteArray();

		   File compressed = new File("Packages.bz2");
		   if(compressed.exists())
			   compressed.delete();

		   FileOutputStream out = new FileOutputStream(compressed);
		   out.write(compressedByteArray);
		   out.close();
		   input.close();
		   output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addToBeBuilt(DebPkg deb) {
		deb.buildMD5();
		deb.loadPackageInfo();
		packages.add(deb);
	}

	public void loadTemplate(String file) {
		builder = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
			String line;
			while((line = reader.readLine()) != null)
				builder.add(line);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addVersion(String codename, String version) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("versions", true));
			writer.newLine();
			writer.write(codename+"_"+version);
			writer.close();
		} catch(Exception e) { }
	}

	public void loadVersions() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File("versions")));
			String line;
			while((line = reader.readLine()) != null) {
				String[] data = line.split("_");
				String codename = data[0];
				String version = data[1];
				ArrayList<String> versions;
				if(app_versions.containsKey(codename))
					versions = app_versions.get(codename);
				else
					versions = new ArrayList<String>();
				if(!versions.contains(version))
					versions.add(version);
				app_versions.put(codename, versions);
			}
			reader.close();
		} catch(Exception e) { e.printStackTrace(); }
	}

	public String[] getData(File deb) {
		String[] data = deb.getName().split("_");
		String codename = data[0];
		String version = data[1].replace(".deb", "");
		return new String[] { codename, version };
	}

	public static void main(String[] args) {
		new iOSBuilder().load();
	}

	public static String buildHex(File file, int digest) {
		try {
			switch(digest) {
			case MD5: return DigestUtils.md5Hex(new FileInputStream(file)).toLowerCase();
			case SHA1: return DigestUtils.sha1Hex(new FileInputStream(file)).toLowerCase();
			case SHA256: return DigestUtils.sha256Hex(new FileInputStream(file)).toLowerCase();
			default: return null;
			}
		} catch(IOException e) { e.printStackTrace(); return null; }
	}

	public class DebPkg {

		private String codename, version, MD5Sum;

		private File file;

		private HashMap<String, String> parameters;

		public DebPkg(String codename, String version, File file) {
			this.codename = codename;
			this.version = version;
			this.file = file;
		}

		public String replaceInfo(String string) {
			string = string.replace("%name", parameters.get("name"));
			string = string.replace("%codename", parameters.get("codename"));
			string = string.replace("%version", parameters.get("version"));
			string = string.replace("%section", parameters.get("section"));
			string = string.replace("%deb_size", parameters.get("deb_size"));
			string = string.replace("%deb_path", parameters.get("deb_path"));
			string = string.replace("%installed_size", parameters.get("installed_size"));
			string = string.replace("%md5_sum", parameters.get("md5_sum"));
			string = string.replace("%description", parameters.get("description"));
			return string;
		}

		public void loadPackageInfo() {
			parameters = new HashMap<>();
			System.out.println("Input name for: "+codename+"_"+version+":");
			String name = scanner.nextLine();
			System.out.println("Input description for: "+codename+"_"+version+":");
			String description = scanner.nextLine();
			System.out.println("Input installed_size for: "+codename+"_"+version+":");
			String installed_size = scanner.nextLine();
			parameters.put("codename", codename);
			parameters.put("version", version);
			parameters.put("section", "Tweaks");
			parameters.put("deb_path", "./pkgfiles/"+file.getName());
			parameters.put("deb_size", Long.toString(file.length()));
			parameters.put("installed_size", installed_size);
			parameters.put("md5_sum", MD5Sum);
			parameters.put("description", description);
			parameters.put("name", name);

		}

		public void buildMD5() {
			MD5Sum = iOSBuilder.buildHex(file, MD5);
		}

	}

}
