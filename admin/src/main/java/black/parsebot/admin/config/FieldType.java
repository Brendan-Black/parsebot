package black.parsebot.admin.config;

import com.google.gson.annotations.SerializedName;

public enum FieldType {
  @SerializedName("text")     TEXT,
  @SerializedName("password") PASSWORD,
  @SerializedName("file")     FILE,
  @SerializedName("folder")   FOLDER,
  @SerializedName("boolean")  BOOLEAN,
  @SerializedName("time")     TIME,
  @SerializedName("list")     LIST
}
