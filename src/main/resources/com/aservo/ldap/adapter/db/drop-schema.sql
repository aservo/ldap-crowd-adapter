drop index if exists _Group_Membership_Transitive_parent_group_id;
drop index if exists _Group_Membership_Transitive_member_group_id;
drop index if exists _User_Membership_Transitive_parent_group_id;
drop index if exists _User_Membership_Transitive_member_user_id;

drop index if exists _Group_Membership_Transitive_parent_group_id_member_group_id;
drop index if exists _User_Membership_Transitive_parent_group_id_member_user_id;

NATIVE_SQL:drop materialized view if exists _Group_Membership_Transitive;
NATIVE_SQL:drop materialized view if exists _User_Membership_Transitive;

drop view if exists _Group_Membership_Transitive_Non_Materialized;
drop view if exists _User_Membership_Transitive_Non_Materialized;

drop index if exists _Group_Membership_parent_group_id;
drop index if exists _Group_Membership_member_group_id;
drop index if exists _User_Membership_parent_group_id;
drop index if exists _User_Membership_member_user_id;

drop table if exists _Group_Membership;
drop table if exists _User_Membership;
drop table if exists _Group;
drop table if exists _User;
