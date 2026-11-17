-- Google Tasks / Calendar sync, both ways (opt-in, see doc/google-sync.md)

-- One Google account per MatrosDMS user. refresh_token is AES-GCM encrypted with a
-- key derived from the store key; NULL means disconnected (sync paused, rows kept
-- so a reconnect to the same Google account does not duplicate anything).
-- last_pulled_at: changes are fetched from Google incrementally (updatedMin); persisted
-- so changes made while MatrosDMS was not running are picked up on the next start.
create sequence google_account_seq start with 1 increment by 50;

create table google_account (
    id bigint not null,
    user_uuid varchar(16) not null unique,
    google_email varchar(320),
    refresh_token varchar(4000),
    granted_scopes varchar(1000),
    tasks_enabled boolean not null,
    calendar_enabled boolean not null,
    task_list_id varchar(255),
    calendar_id varchar(255),
    last_error varchar(1000),
    last_error_at timestamp(6),
    date_connected timestamp(6),
    last_pulled_at timestamp(6),
    primary key (id)
);

-- Where each action lives remotely, per target. Keyed by uuid, not by FK: the row
-- has to outlive the action so the delete job still knows what to remove.
-- synced_completed / synced_due: the state last written to / read from Google, the
-- common base of a three-way merge, so a change made in Google can be told apart
-- from the echo of our own push. synced_completed NULL = no base recorded yet
-- (nothing is pulled until the next push).
create sequence action_sync_seq start with 1 increment by 50;

create table action_sync (
    id bigint not null,
    version bigint,
    action_uuid varchar(16) not null,
    target varchar(32) not null,
    account_user_uuid varchar(16) not null,
    container_id varchar(255) not null,
    external_id varchar(255) not null,
    external_etag varchar(255),
    date_synced timestamp(6),
    synced_completed boolean,
    synced_due timestamp(6),
    primary key (id),
    constraint uk_action_sync_target unique (action_uuid, target)
);

create index idx_action_sync_account on action_sync (account_user_uuid);
