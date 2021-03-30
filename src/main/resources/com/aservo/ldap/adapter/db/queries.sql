--[ID: find_all_groups]--
select a.*
from _Group a

--[ID: find_all_users]--
select a.*
from _User a
where a.active or not (:active_only = true)

--[ID: find_group]--
select a.*
from _Group a
where a.id = :id

--[ID: find_user]--
select a.*
from _User a
where a.id = :id and (a.active or not (:active_only = true))

--[ID: find_group_memberships]--
select a.*
from _Group_Membership a
where a.parent_group_id = :parent_group_id and a.member_group_id = :member_group_id

--[ID: find_user_memberships]--
select a.*
from _User_Membership a
inner join _User b
  on a.member_user_id = b.id
where a.parent_group_id = :parent_group_id and a.member_user_id = :member_user_id and
  (b.active or not (:active_only = true))

--[ID: create_or_update_group]--
insert into _Group (id, description)
  values (:id, :description)

-- [ID: create_or_update_group]--
--merge into _Group
--using (select * from _Group)
--  on id = :id
--when not matched then
--insert (id, description)
--  values (:id, :description)
--when matched then
--update set
--  description = :description

--[ID: create_or_update_user]--
insert into _User (id, last_name, first_name, display_name, email, active)
  values (:id, :last_name, :first_name, :display_name, :email, :active)

-- [ID: create_or_update_user]--
--merge into _User
--using (select * from _User)
--  on id = :id
--when not matched then
--insert (id, last_name, first_name, display_name, email, active)
--  values (:id, :last_name, :first_name, :display_name, :email, :active)
--when matched then
--update set
--  last_name = :last_name,
--  first_name = :first_name,
--  display_name = :display_name,
--  email = :email,
--  active = :active

--[ID: create_group_membership_if_not_exists]--
insert into _Group_Membership (parent_group_id, member_group_id)
  values (:parent_group_id, :member_group_id)

-- [ID: create_group_membership_if_not_exists]--
--merge _Group_Membership with (holdlock) as a
--using (:parent_group_id, :member_group_id) as b (parent_group_id, member_group_id)
--  on a.id = b.id
--when not matched
--insert values (:parent_group_id, :member_group_id)

--[ID: create_user_membership_if_not_exists]--
insert into _User_Membership (parent_group_id, member_user_id)
  values (:parent_group_id, :member_user_id)

-- [ID: create_user_membership_if_not_exists]--
--merge _User_Membership with (holdlock) as a
--using (:parent_group_id, :member_user_id) as b (parent_group_id, member_user_id)
--  on a.id = b.id
--when not matched
--insert values (:parent_group_id, :member_user_id)

--[ID: remove_all_groups]--
delete from _Group

--[ID: remove_all_users]--
delete from _User

--[ID: remove_all_group_memberships]--
delete from _Group_Membership

--[ID: remove_all_user_memberships]--
delete from _User_Membership

--[ID: remove_group_if_exists]--
delete from _Group a
where a.id = :id

--[ID: remove_user_if_exists]--
delete from _User a
where a.id = :id

--[ID: remove_group_membership_if_exists]--
delete from _Group_Membership a
where a.parent_group_id = :parent_group_id and a.member_group_id = :member_group_id

--[ID: remove_user_membership_if_exists]--
delete from _User_Membership a
where a.parent_group_id = :parent_group_id and a.member_user_id = :member_user_id

--[ID: find_direct_users_of_group]--
select distinct b.*
from _User_Membership a
inner join _User b
  on a.member_user_id = b.id
where a.parent_group_id = :group_id and (b.active or not (:active_only = true))

--[ID: find_direct_groups_of_user]--
select distinct c.*
from _User_Membership a
inner join _User b
  on a.member_user_id = b.id
inner join _Group c
  on a.parent_group_id = c.id
where a.member_user_id = :user_id and (b.active or not (:active_only = true))

--[ID: find_transitive_users_of_group]--
with recursive ChildRelationship (id, origin_id) as (
  select a.id, a.id
  from _Group a
  union all
  select b.member_group_id, c.origin_id
  from _Group_Membership b
  inner join ChildRelationship c
    on b.parent_group_id = c.id
)
select distinct b.*
from _User_Membership a
inner join _User b
  on a.member_user_id = b.id
where (b.active or not (:active_only = true)) and a.parent_group_id in (
  select c.id
  from ChildRelationship c
  where c.origin_id = :group_id
)

--[ID: find_transitive_groups_of_user]--
with recursive ParentRelationship (id, origin_id) as (
  select a.id, a.id
  from _Group a
  union all
  select b.parent_group_id, c.origin_id
  from _Group_Membership b
  inner join ParentRelationship c
    on b.member_group_id = c.id
)
select distinct c.*
from _User_Membership a
inner join _User b
  on a.member_user_id = b.id
inner join _Group c
  on a.parent_group_id = c.id
where b.id = :user_id and (b.active or not (:active_only = true)) and c.id in (
  select d.id
  from ParentRelationship d
  where d.origin_id = :group_id
)

--[ID: find_direct_child_groups_of_group]--
select distinct b.*
from _Group_Membership a
inner join _Group b
  on a.member_group_id = b.id
where a.parent_group_id = :group_id

--[ID: find_direct_parent_groups_of_group]--
select distinct b.*
from _Group_Membership a
inner join _Group b
  on a.parent_group_id = b.id
where a.member_group_id = :group_id

--[ID: find_transitive_child_groups_of_group]--
with recursive ChildRelationship (id, origin_id) as (
  select a.id, a.id
  from _Group a
  union all
  select b.member_group_id, c.origin_id
  from _Group_Membership b
  inner join ChildRelationship c
    on b.parent_group_id = c.id
)
select distinct a.*
from _Group a
where a.id <> :group_id and a.id in (
  select b.id
  from ChildRelationship b
  where b.origin_id = :group_id
)

--[ID: find_transitive_parent_groups_of_group]--
with recursive ParentRelationship (id, origin_id) as (
  select a.id, a.id
  from _Group a
  union all
  select b.parent_group_id, c.origin_id
  from _Group_Membership b
  inner join ParentRelationship c
    on b.member_group_id = c.id
)
select distinct a.*
from _Group a
where a.id <> :group_id and a.id in (
  select b.id
  from ParentRelationship b
  where b.origin_id = :group_id
)
