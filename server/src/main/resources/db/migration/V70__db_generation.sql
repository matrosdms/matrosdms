-- Save-point counter for the rollback guard (DatabaseGeneration): raised at every start and with
-- every periodic checkpoint, and mirrored into matros.generation next to the database file. A
-- database that is behind that file fell back to an older state, and MatrosDMS refuses to start.
create table db_generation (
    id integer not null,
    generation bigint not null,
    date_updated timestamp(6),
    primary key (id)
);
insert into db_generation (id, generation, date_updated) values (1, 0, current_timestamp);
