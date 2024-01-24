create table if not exists payment(
  id BIGINT primary key,
  user_id BIGINT not null,
  order_id BIGINT not null unique,
  price BIGINT not null check(price > 0),
  state varchar(10) not null check(state in ('PENDING', 'SUCCESS', 'FAILED', 'CANCELED')),
  version int not null,
  created_at TIMESTAMP(6) not null,
  modified_at TIMESTAMP(6) not null
);

create table if not exists point(
  id bigint primary key,
  user_id bigint not null unique,
  point bigint not null check(point >= 0),
  version int not null,
  created_at TIMESTAMP(6) not null,
  modified_at TIMESTAMP(6) not null
);

create index if not exists point_idx_user_id on point(user_id);
