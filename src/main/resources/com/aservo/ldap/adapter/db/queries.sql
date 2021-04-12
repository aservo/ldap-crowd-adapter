--[ID: find_all_groups]--
select g.*
from _Group g

--[ID: find_all_users]--
select u.*
from _User u
where u.active or :active_only = false

--[ID: find_group]--
select g.*
from _Group g
where g.id = :id

--[ID: find_user]--
select u.*
from _User u
where u.id = :id and (u.active or :active_only = false)

--[ID: find_group_memberships]--
select m.*
from _Group_Membership m
where m.parent_group_id = :parent_group_id and m.member_group_id = :member_group_id

--[ID: find_user_memberships]--
select m.*
from _User_Membership m
inner join _User u
  on u.id = m.member_user_id
where m.parent_group_id = :parent_group_id and m.member_user_id = :member_user_id and (u.active or :active_only = false)

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
delete from _Group g
where g.id = :id

--[ID: remove_user_if_exists]--
delete from _User u
where u.id = :id

--[ID: remove_group_membership_if_exists]--
delete from _Group_Membership m
where m.parent_group_id = :parent_group_id and m.member_group_id = :member_group_id

--[ID: remove_user_membership_if_exists]--
delete from _User_Membership m
where m.parent_group_id = :parent_group_id and m.member_user_id = :member_user_id

--[ID: find_direct_users_of_group]--
select distinct u.*
from _User_Membership m
inner join _User u
  on u.id = m.member_user_id
where m.parent_group_id = :group_id and (u.active or :active_only = false)

--[ID: find_direct_groups_of_user]--
select distinct g.*
from _User_Membership m
inner join _Group g
  on g.id = m.parent_group_id
inner join _User u
  on u.id = m.member_user_id
where m.member_user_id = :user_id and (u.active or :active_only = false)

--[ID: find_transitive_users_of_group]--
select distinct u.*
from _User_Membership_Transitive m
inner join _User u
  on u.id = m.member_user_id
where m.parent_group_id = :group_id and (u.active or :active_only = false)

--[ID: find_transitive_groups_of_user]--
select distinct g.*
from _User_Membership_Transitive m
inner join _Group g
  on g.id = m.parent_group_id
inner join _User u
  on u.id = m.member_user_id
where m.member_user_id = :user_id and (u.active or :active_only = false)

--[ID: find_direct_child_groups_of_group]--
select distinct g.*
from _Group_Membership m
inner join _Group g
  on g.id = m.member_group_id
where m.parent_group_id = :group_id

--[ID: find_direct_parent_groups_of_group]--
select distinct g.*
from _Group_Membership m
inner join _Group g
  on g.id = m.parent_group_id
where m.member_group_id = :group_id

--[ID: find_transitive_child_groups_of_group]--
select distinct g.*
from _Group_Membership_Transitive m
inner join _Group g
  on g.id = m.member_group_id
where m.parent_group_id = :group_id

--[ID: find_transitive_parent_groups_of_group]--
select distinct g.*
from _Group_Membership_Transitive m
inner join _Group g
  on g.id = m.parent_group_id
where m.member_group_id = :group_id

--[ID: find_all_direct_group_memberships]--
select distinct m.*
from _Group_Membership m

--[ID: find_all_direct_user_memberships]--
select distinct m.*
from _User_Membership m
inner join _User u
  on u.id = m.member_user_id
where u.active or :active_only = false

--[ID: find_all_transitive_group_memberships]--
select distinct m.*
from _Group_Membership_Transitive m

--[ID: find_all_transitive_user_memberships]--
select distinct m.*
from _User_Membership_Transitive m
inner join _User u
  on u.id = m.member_user_id
where u.active or :active_only = false
