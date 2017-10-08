-- download related
CREATE TABLE [downloads] (
  [_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  [key] TEXT NOT NULL ON CONFLICT ABORT,
  [title] TEXT NOT NULL,
  [url] TEXT,
  [totalSizeInBytes] BIGINT DEFAULT 0,
  [downloadedSizeInBytes] BIGINT DEFAULT 0,
  [downloadProgress] INTEGER DEFAULT 0,
  [downloadMode] SMALLINT DEFAULT 0,
  [downloadState] SMALLINT DEFAULT 0,
  [localFilePath] TEXT,
  [resourceKey] TEXT,
  [resourceModelId] INTEGER NOT NULL DEFAULT 0,
  [startTime] BIGINT DEFAULT 0,
  [endTime] BIGINT DEFAULT 0,
  [connectNetworkUsedTime] INTEGER DEFAULT 0,
  [usedTime] INTEGER DEFAULT 0,
  [avgSpeed] BIGINT DEFAULT 0,
  [maxSpeed] BIGINT DEFAULT 0,
  [tryTimes] INTEGER DEFAULT 0,
  [downloaderType] SMALLINT NOT NULL DEFAULT 0,
  [ranges] TEXT);

CREATE INDEX [index_resource] ON [downloads] ([resourceKey], [resourceModelId]);

CREATE INDEX [index_title] ON [downloads] ([title]);

CREATE UNIQUE INDEX [unique_index_key] ON [downloads] ([key]);

CREATE TABLE [models] (
  [_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  [name] TEXT,
  [tableName] TEXT NOT NULL,
  [modelClass] TEXT NOT NULL,
  [version] INTEGER NOT NULL DEFAULT 0);

CREATE UNIQUE INDEX [unique_table_name] ON [models] ([tableName] ASC);

CREATE UNIQUE INDEX [unique_model_class] ON [models] ([modelClass]);

CREATE VIEW [download_info] AS
SELECT d.[_id], d.[key], d.[title], d.[url], d.[totalSizeInBytes], d.[downloadedSizeInBytes],
    d.[downloadProgress], d.[downloadMode], d.[downloadState], d.[localFilePath], m.[modelClass] AS resourceClassName,
    d.[resourceKey], d.[startTime], d.[endTime], d.[connectNetworkUsedTime], d.[usedTime], d.[avgSpeed],
    d.[maxSpeed], d.[tryTimes], d.[downloaderType], d.[ranges]
FROM downloads d join models m on (d.[resourceModelId] == m.[_id]);

CREATE TABLE "simples" (
  [_id] INTEGER NOT NULL PRIMARY KEY,
  [key] TEXT NOT NULL,
  [title] TEXT,
  [url] TEXT NOT NULL,
  [ext] TEXT,
  [mime] TEXT);

CREATE UNIQUE INDEX [unique_simple_key] ON "simples" ([key]);


