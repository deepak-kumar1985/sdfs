package org.opendedup.sdfs.io.events;

import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.io.MetaDataDedupFile;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class MFileDeleted extends GenericEvent {

	public MetaDataDedupFile mf;
	public boolean dir;
	private static final int pl = Main.volume.getPath().length();

	public MFileDeleted(MetaDataDedupFile f) {
		super();
		this.mf = f;
	}

	public MFileDeleted(MetaDataDedupFile f, boolean dir) {
		super();
		this.mf = f;
		this.dir = dir;
	}

	public String toJSON() {
		JsonObject dataset = this.toJSONObject();
		dataset.addProperty("actionType", "mfileDelete");
		dataset.addProperty("object", mf.getPath().substring(pl));
		if (mf.isSymlink())
			dataset.addProperty("fileType", "symlink");
		else if (this.dir)
			dataset.addProperty("fileType", "dir");
		else
			dataset.addProperty("fileType", "file");
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.create();
		return gson.toJson(dataset);
	}

}
