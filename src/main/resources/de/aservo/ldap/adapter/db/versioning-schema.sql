--[ID: set_schema_version]--
insert into _Schema_Version (hash, created_at)
values (:hash, :created_at)

--[ID: get_schema_version]--
select a.*
from _Schema_Version a
order by a.created_at desc

--[ID: create_schema_version_table]--
create table if not exists _Schema_Version (
  hash binary not null,
  created_at datetime not null,
  primary key (hash)
)
