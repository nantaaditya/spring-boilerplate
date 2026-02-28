create table if not exists dead_letter_process (
    id bigserial primary key,
    created_by varchar(25),
    updated_by varchar(25),
    created_date timestamp,
    updated_date timestamp,
    version int,
    process_type varchar(50),
    process_name varchar(100),
    idempotency_key varchar(100),
    client_name varchar(50),
    method varchar(10),
    path text,
    headers text,
    last_error text,
    payload bytea,
    retry_count int default 0,
    max_retry int,
    status varchar(10),
    retry_histories bytea
);

create index if not exists idx_deadletterprocess_type_name_status on dead_letter_process(process_type, process_name, status);