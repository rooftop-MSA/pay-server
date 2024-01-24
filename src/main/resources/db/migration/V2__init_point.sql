create table if not exists point(
  id bigint primary key,
  user_id bigint not null unique,
  point bigint not null check(point >= 0),
  version int not null,
  created_at TIMESTAMP(6) not null,
  modified_at TIMESTAMP(6) not null
);

create index if not exists point_idx_user_id on point(user_id);
