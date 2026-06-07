package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;

public abstract class SqlTable {
    public abstract String getTableName();

    public abstract String getCreateCommand();

    public abstract int getVersion();

    public abstract void onCreate(Handle handle);

    public abstract void onUpgrade(Handle handle, int oldVersion, int newVersion);
}
