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
insert into _Group (id, name, description)
  values (:id, :name, :description)
  on conflict (id) do
    update set
      name = :name,
      description = :description

--[ID: create_or_update_user]--
insert into _User (id, username, last_name, first_name, display_name, email, active)
  values (:id, :username, :last_name, :first_name, :display_name, :email, :active)
  on conflict (id) do
    update set
      username = :username,
      last_name = :last_name,
      first_name = :first_name,
      display_name = :display_name,
      email = :email,
      active = :active

-- -- [ID: create_or_update_user]--
--merge into _User
--using (select 1 from _User)
--  on id = :id
--when matched then
--  update set
--    last_name = :last_name,
--    first_name = :first_name,
--    display_name = :display_name,
--    email = :email,
--    active = :active
--when not matched then
--  insert (id, last_name, first_name, display_name, email, active)
--    values (:id, :last_name, :first_name, :display_name, :email, :active)

--[ID: create_group_membership_if_not_exists]--
insert into _Group_Membership (parent_group_id, member_group_id)
  values (:parent_group_id, :member_group_id)
  on conflict (parent_group_id, member_group_id) do nothing

--[ID: create_user_membership_if_not_exists]--
insert into _User_Membership (parent_group_id, member_user_id)
  values (:parent_group_id, :member_user_id)
  on conflict (parent_group_id, member_user_id) do nothing

--[ID: remove_all_groups]--
delete from _Group

--[ID: remove_all_users]--
delete from _User

--[ID: remove_all_group_memberships]--
delete from _Group_Membership

--[ID: remove_all_user_memberships]--
delete from _User_Membership

--[ID: remove_group_if_exists]--
delete from _Group
where id = :id

--[ID: remove_user_if_exists]--
delete from _User
where id = :id

--[ID: remove_group_membership_if_exists]--
delete from _Group_Membership
where parent_group_id = :parent_group_id and member_group_id = :member_group_id

--[ID: remove_user_membership_if_exists]--
delete from _User_Membership
where parent_group_id = :parent_group_id and member_user_id = :member_user_id

--[ID: find_direct_users_of_group]--
select u.*
from _User_Membership m
inner join _User u
  on u.id = m.member_user_id
where m.parent_group_id = :group_id and (u.active or :active_only = false)

--[ID: find_direct_groups_of_user]--
select g.*
from _User_Membership m
inner join _Group g
  on g.id = m.parent_group_id
inner join _User u
  on u.id = m.member_user_id
where m.member_user_id = :user_id and (u.active or :active_only = false)

--[ID: find_transitive_users_of_group]--
select u.*
from _User_Membership_Transitive m
inner join _User u
  on u.id = m.member_user_id
where m.parent_group_id = :group_id and (u.active or :active_only = false)

--[ID: find_transitive_users_of_group_non_materialized]--
select u.*
from _User_Membership_Transitive_Non_Materialized m
inner join _User u
  on u.id = m.member_user_id
where m.parent_group_id = :group_id and (u.active or :active_only = false)

--[ID: find_transitive_groups_of_user]--
select g.*
from _User_Membership_Transitive m
inner join _Group g
  on g.id = m.parent_group_id
inner join _User u
  on u.id = m.member_user_id
where m.member_user_id = :user_id and (u.active or :active_only = false)

--[ID: find_transitive_groups_of_user_non_materialized]--
select g.*
from _User_Membership_Transitive_Non_Materialized m
inner join _Group g
  on g.id = m.parent_group_id
inner join _User u
  on u.id = m.member_user_id
where m.member_user_id = :user_id and (u.active or :active_only = false)

--[ID: find_direct_child_groups_of_group]--
select g.*
from _Group_Membership m
inner join _Group g
  on g.id = m.member_group_id
where m.parent_group_id = :group_id

--[ID: find_direct_parent_groups_of_group]--
select g.*
from _Group_Membership m
inner join _Group g
  on g.id = m.parent_group_id
where m.member_group_id = :group_id

--[ID: find_transitive_child_groups_of_group]--
select g.*
from _Group_Membership_Transitive m
inner join _Group g
  on g.id = m.member_group_id
where m.parent_group_id = :group_id

--[ID: find_transitive_child_groups_of_group_non_materialized]--
select g.*
from _Group_Membership_Transitive_Non_Materialized m
inner join _Group g
  on g.id = m.member_group_id
where m.parent_group_id = :group_id

--[ID: find_transitive_parent_groups_of_group]--
select g.*
from _Group_Membership_Transitive m
inner join _Group g
  on g.id = m.parent_group_id
where m.member_group_id = :group_id

--[ID: find_transitive_parent_groups_of_group_non_materialized]--
select g.*
from _Group_Membership_Transitive_Non_Materialized m
inner join _Group g
  on g.id = m.parent_group_id
where m.member_group_id = :group_id

--[ID: find_all_direct_group_memberships]--
select m.*
from _Group_Membership m

--[ID: find_all_direct_user_memberships]--
select m.*
from _User_Membership m
inner join _User u
  on u.id = m.member_user_id
where u.active or :active_only = false

--[ID: find_all_transitive_group_memberships]--
select m.*
from _Group_Membership_Transitive m

--[ID: find_all_transitive_group_memberships_non_materialized]--
select m.*
from _Group_Membership_Transitive_Non_Materialized m

--[ID: find_all_transitive_user_memberships]--
select m.*
from _User_Membership_Transitive m
inner join _User u
  on u.id = m.member_user_id
where u.active or :active_only = false

--[ID: find_all_transitive_user_memberships_non_materialized]--
select m.*
from _User_Membership_Transitive_Non_Materialized m
inner join _User u
  on u.id = m.member_user_id
where u.active or :active_only = false

--[ID: refresh_materialized_view_for_transitive_group_memberships]--
NATIVE_SQL:refresh materialized view concurrently _Group_Membership_Transitive

--[ID: refresh_materialized_view_for_transitive_user_memberships]--
NATIVE_SQL:refresh materialized view concurrently _User_Membership_Transitive
