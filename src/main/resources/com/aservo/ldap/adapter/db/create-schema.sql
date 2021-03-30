create table _Group (
  id varchar(255) not null,
  description text null,
  primary key (id)
);

create table _User (
  id varchar(255) not null,
  last_name text null,
  first_name text null,
  display_name text null,
  email text null,
  active boolean not null,
  primary key (id)
);

create table _Group_Membership (
  parent_group_id varchar(255) not null,
  member_group_id varchar(255) not null,
  primary key (parent_group_id, member_group_id),
  foreign key (parent_group_id) references _Group(id) on delete cascade,
  foreign key (member_group_id) references _Group(id) on delete cascade
);

create table _User_Membership (
  parent_group_id varchar(255) not null,
  member_user_id varchar(255) not null,
  primary key (parent_group_id, member_user_id),
  foreign key (parent_group_id) references _Group(id) on delete cascade,
  foreign key (member_user_id) references _User(id) on delete cascade
);
