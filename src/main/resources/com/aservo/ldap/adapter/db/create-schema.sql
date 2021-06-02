create table _Group (
  id varchar(255) not null,
  name varchar(255) not null,
  description text null,
  primary key (id)
);

create table _User (
  id varchar(255) not null,
  username varchar(255) not null,
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

create index _Group_Membership_parent_group_id on _Group_Membership (parent_group_id);
create index _Group_Membership_member_group_id on _Group_Membership (member_group_id);

create table _User_Membership (
  parent_group_id varchar(255) not null,
  member_user_id varchar(255) not null,
  primary key (parent_group_id, member_user_id),
  foreign key (parent_group_id) references _Group(id) on delete cascade,
  foreign key (member_user_id) references _User(id) on delete cascade
);

create index _User_Membership_parent_group_id on _User_Membership (parent_group_id);
create index _User_Membership_member_user_id on _User_Membership (member_user_id);

create view _Group_Membership_Transitive_Non_Materialized (parent_group_id, member_group_id) as
  with recursive ParentRelationship (group_id, member_group_id) as (
    select m.parent_group_id, m.member_group_id
    from _Group_Membership m
    union
    select m.parent_group_id, p.member_group_id
    from _Group_Membership m
    join ParentRelationship p
    on p.group_id = m.member_group_id
  )
  select p.group_id, p.member_group_id
  from ParentRelationship p;

create view _User_Membership_Transitive_Non_Materialized (parent_group_id, member_user_id) as
  with recursive ParentRelationship (group_id, member_user_id) as (
    select m.parent_group_id, m.member_user_id
    from _User_Membership m
    union
    select m.parent_group_id, p.member_user_id
    from _Group_Membership m
    inner join ParentRelationship p
      on p.group_id = m.member_group_id
  )
  select p.group_id, p.member_user_id
  from ParentRelationship p;
  
NATIVE_SQL:create materialized view _Group_Membership_Transitive (parent_group_id, member_group_id) as
  select * from _Group_Membership_Transitive_Non_Materialized;

create index _Group_Membership_Transitive_parent_group_id on _Group_Membership_Transitive (parent_group_id);
create index _Group_Membership_Transitive_member_group_id on _Group_Membership_Transitive (member_group_id);
create unique index _Group_Membership_Transitive_parent_group_id_member_group_id 
  on _Group_Membership_Transitive (parent_group_id, member_group_id);

NATIVE_SQL:create materialized view _User_Membership_Transitive (parent_group_id, member_user_id) as
  select * from _User_Membership_Transitive_Non_Materialized;
  
create index _User_Membership_Transitive_parent_group_id on _User_Membership_Transitive (parent_group_id);
create index _User_Membership_Transitive_member_user_id on _User_Membership_Transitive (member_user_id);
create unique index _User_Membership_Transitive_parent_group_id_member_user_id
  on _User_Membership_Transitive (parent_group_id, member_user_id);
