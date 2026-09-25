-- Read-only checks for a newly deployed OpenSwim schema.
-- Run as the project database admin; no seed data or test users are required.
begin transaction read only;

do $$
declare
    app_tables text[] := array[
        'profiles', 'workouts', 'workout_sections', 'workout_steps',
        'training_plans', 'training_plan_workouts', 'scheduled_workouts',
        'completed_workouts', 'completed_intervals'
    ];
    protected_count integer;
    policy_count integer;
begin
    select count(*) into protected_count
    from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relname = any(app_tables)
      and c.relkind = 'r' and c.relrowsecurity;
    if protected_count <> 9 then
        raise exception 'Expected 9 OpenSwim tables with RLS; found %', protected_count;
    end if;

    select count(*) into policy_count from pg_policies
    where schemaname = 'public' and tablename = any(app_tables);
    if policy_count <> 35 then
        raise exception 'Expected 35 OpenSwim RLS policies; found %', policy_count;
    end if;

    if not has_table_privilege('anon', 'public.workouts', 'select')
       or not has_table_privilege('anon', 'public.workout_sections', 'select')
       or not has_table_privilege('anon', 'public.workout_steps', 'select') then
        raise exception 'Anonymous template SELECT grants are missing';
    end if;

    if has_table_privilege('anon', 'public.profiles', 'select')
       or has_table_privilege('anon', 'public.training_plans', 'select')
       or has_table_privilege('anon', 'public.scheduled_workouts', 'select')
       or has_table_privilege('anon', 'public.completed_workouts', 'select')
       or has_table_privilege('anon', 'public.completed_intervals', 'select') then
        raise exception 'Anonymous role can read private OpenSwim tables';
    end if;

    if not exists (
        select 1 from pg_trigger
        where tgrelid = 'auth.users'::regclass and tgname = 'on_auth_user_created'
          and not tgisinternal
    ) then
        raise exception 'New-user profile trigger is missing';
    end if;
end $$;

rollback;
