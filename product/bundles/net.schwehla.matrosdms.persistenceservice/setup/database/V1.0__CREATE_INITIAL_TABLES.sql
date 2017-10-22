CREATE TABLE Context (CONTEXT_ID BIGINT NOT NULL UNIQUE, DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, ICON VARCHAR, NAME VARCHAR NOT NULL, UUID VARCHAR NOT NULL UNIQUE, PRIMARY KEY (CONTEXT_ID));
CREATE TABLE FileMetadata (FILE_ID BIGINT NOT NULL UNIQUE, CRYPTSETTINGS VARCHAR, FILENAME VARCHAR NOT NULL, FILESIZE BIGINT, MIMETYPE VARCHAR, SHA256CRYPTED VARCHAR NOT NULL UNIQUE, SHA256ORIGINAL VARCHAR NOT NULL UNIQUE, PRIMARY KEY (FILE_ID));
CREATE TABLE Item (ITEM_ID BIGINT NOT NULL UNIQUE, ISSUEDATE TIMESTAMP, DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, ICON VARCHAR, LASTINDEXRUN TIMESTAMP, INDEXSTATE INTEGER, NAME VARCHAR NOT NULL, STAGE INTEGER, STORAGEITEMIDENTIFIER VARCHAR, UUID VARCHAR NOT NULL UNIQUE, CONTEXT_ID BIGINT NOT NULL, FILE_ID BIGINT UNIQUE, STORE_STORE_ID BIGINT, USER_ID BIGINT NOT NULL, PRIMARY KEY (ITEM_ID));
CREATE TABLE Kategory (KATEGORY_ID BIGINT NOT NULL UNIQUE, DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, ICON VARCHAR, NAME VARCHAR NOT NULL, OBJECT BOOLEAN, UUID VARCHAR NOT NULL UNIQUE, PARENT_KATEGORY_ID BIGINT, ORDINAL INT, PRIMARY KEY (KATEGORY_ID));
CREATE TABLE Store (STORE_ID BIGINT NOT NULL UNIQUE, DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, ICON VARCHAR, NAME VARCHAR NOT NULL, SHORTNAME VARCHAR, UUID VARCHAR NOT NULL UNIQUE, ORDINAL INT, PRIMARY KEY (STORE_ID));
CREATE TABLE Event (EVENT_ID BIGINT NOT NULL UNIQUE, ACTIONSCRIPT VARCHAR, DATEARCHIVED TIMESTAMP, DATECOMPLETED TIMESTAMP, DATECREATED TIMESTAMP, DATESCHEDULED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, ICON VARCHAR, NAME VARCHAR NOT NULL, UUID VARCHAR NOT NULL UNIQUE, ITEM_ID BIGINT NOT NULL, PRIMARY KEY (EVENT_ID));
CREATE TABLE Attribute (ATTRIBUTE_ID BIGINT NOT NULL UNIQUE, ATTR_SUBTPYE VARCHAR(31), DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, ICON VARCHAR, NAME VARCHAR NOT NULL, RELEVANCEFROM TIMESTAMP, RELEVANCETO TIMESTAMP, UUID VARCHAR NOT NULL UNIQUE, ITEM_ID BIGINT NOT NULL, ATTRIBUTETYPE_ATTRIBUTETYPE_ID BIGINT, BOOLEANVALUE BOOLEAN, DATEVALUE TIMESTAMP, INTERNALURL BOOLEAN, URL VARCHAR, NUMBERVALUE DOUBLE, TEXTVALUE VARCHAR, PRIMARY KEY (ATTRIBUTE_ID));
CREATE TABLE Attributetype (ATTRIBUTETYPE_ID BIGINT NOT NULL UNIQUE, DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DEFAULTVALUESCRIPT VARCHAR, DESCRIPTION VARCHAR, ICON VARCHAR, KEY VARCHAR NOT NULL, NAME VARCHAR NOT NULL, PATTERN VARCHAR, TYPE VARCHAR NOT NULL, UNIT VARCHAR, UUID VARCHAR NOT NULL UNIQUE, VALIDATESCRIPT VARCHAR, ORDINAL INT, PRIMARY KEY (ATTRIBUTETYPE_ID));
CREATE TABLE User (USER_ID BIGINT NOT NULL UNIQUE, DATEARCHIVED TIMESTAMP, DATECREATED TIMESTAMP, DATEUPDATED TIMESTAMP, DESCRIPTION VARCHAR, EMAIL VARCHAR, ICON VARCHAR, NAME VARCHAR NOT NULL, PASSWORDHASH VARCHAR, UUID VARCHAR NOT NULL UNIQUE, PRIMARY KEY (USER_ID));
CREATE TABLE Permission (PERMISSION_ID BIGINT NOT NULL UNIQUE, KEY VARCHAR, NAME VARCHAR, USER_ID BIGINT NOT NULL, PRIMARY KEY (PERMISSION_ID));
CREATE TABLE CONFIG (CONFIG_ID BIGINT NOT NULL UNIQUE, KEY VARCHAR, VALUE VARCHAR, PRIMARY KEY (CONFIG_ID));
CREATE TABLE Context_Kategorie (CONTEXT_ID BIGINT NOT NULL, KATEGORY_ID BIGINT NOT NULL, PRIMARY KEY (CONTEXT_ID, KATEGORY_ID));
CREATE TABLE Item_Kategorie (ITEM_ID BIGINT NOT NULL, KATEGORY_ID BIGINT NOT NULL, PRIMARY KEY (ITEM_ID, KATEGORY_ID));