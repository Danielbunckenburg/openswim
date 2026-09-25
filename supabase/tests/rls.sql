-- Run as a database admin with psql -v ON_ERROR_STOP=1 -f supabase/tests/rls.sql.
-- Uses fictional rows from seed.sql. Everything rolls back.
begin;

do $$
declare missing_count integer;
begin
    select count(*) into missing_count
    from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relname = any (array['profiles','workouts','workout_sections','workout_steps',
          'training_plans','training_plan_workouts','scheduled_workouts','completed_workouts','completed_intervals'])
      and not c.relrowsecurity;
    if missing_count <> 0 then raise exception '% public tables lack RLS', missing_count; end if;
    if has_table_privilege('anon', 'public.completed_workouts', 'select') then
        raise exception 'anon has completed workout SELECT grant';
    end if;
end $$;

-- Signed-out visitors may see only the public workout template and its structure.
set local role anon;
do $$
begin
    if (select count(*) from public.workouts
        where id in ('11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222',
                     '33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444444')) <> 1
        then raise exception 'anon workout visibility'; end if;
    if (select count(*) from public.workout_sections
        where workout_id in ('11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222',
                             '33333333-3333-4333-8333-333333333333')) <> 1
        then raise exception 'anon section visibility'; end if;
    if (select count(*) from public.workout_steps
        where section_id in ('11111111-1111-4111-8111-111111111112', '22222222-2222-4222-8222-222222222223',
                             '33333333-3333-4333-8333-333333333334')) <> 1
        then raise exception 'anon step visibility'; end if;
end $$;
reset role;

-- Swimmer A can read their completed swim and update their own profile.
select set_config('request.jwt.claim.sub', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', true);
set local role authenticated;
do $$
declare changed_count integer;
begin
    if (select count(*) from public.completed_workouts) <> 1 then raise exception 'owner cannot read completed workout'; end if;
    if (select count(*) from public.completed_intervals) <> 1 then raise exception 'owner cannot read interval'; end if;
    if (select count(*) from public.training_plans) <> 1 then raise exception 'owner cannot read plan'; end if;
    if (select count(*) from public.scheduled_workouts) <> 1 then raise exception 'owner cannot read schedule'; end if;
    update public.profiles set display_name = 'Sample Swimmer A Test'
    where id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
    get diagnostics changed_count = row_count;
    if changed_count <> 1 then raise exception 'owner cannot update profile'; end if;
end $$;
reset role;

-- Swimmer B may read A's public template, but no A profile, activity or plan.
select set_config('request.jwt.claim.sub', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', true);
set local role authenticated;
do $$
declare changed_count integer;
begin
    if (select count(*) from public.profiles where id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa') <> 0 then
        raise exception 'other profile exposed';
    end if;
    if (select count(*) from public.completed_workouts) <> 0 then raise exception 'other completion exposed'; end if;
    if (select count(*) from public.completed_intervals) <> 0 then raise exception 'other interval exposed'; end if;
    if (select count(*) from public.scheduled_workouts) <> 0 then raise exception 'other schedule exposed'; end if;
    if (select count(*) from public.training_plans) <> 0 then raise exception 'other plan exposed'; end if;
    if (select count(*) from public.training_plan_workouts) <> 0 then raise exception 'other plan placement exposed'; end if;
    if (select count(*) from public.workouts where id = '22222222-2222-4222-8222-222222222222') <> 1 then
        raise exception 'public workout hidden';
    end if;
    if (select count(*) from public.workouts where id = '11111111-1111-4111-8111-111111111111') <> 0 then
        raise exception 'private workout exposed';
    end if;
    if (select count(*) from public.workouts where id = '44444444-4444-4444-8444-444444444444') <> 0 then
        raise exception 'unlisted workout exposed';
    end if;
    if (select count(*) from public.workout_sections where workout_id = '11111111-1111-4111-8111-111111111111') <> 0 then
        raise exception 'private section exposed';
    end if;
    update public.profiles set display_name = 'Incorrect change'
    where id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
    get diagnostics changed_count = row_count;
    if changed_count <> 0 then raise exception 'other profile was updated'; end if;
    update public.workouts set title = 'Incorrect change'
    where id = '22222222-2222-4222-8222-222222222222';
    get diagnostics changed_count = row_count;
    if changed_count <> 0 then raise exception 'non-owner changed public workout'; end if;
    delete from public.completed_workouts where id = '88888888-8888-4888-8888-888888888888';
    get diagnostics changed_count = row_count;
    if changed_count <> 0 then raise exception 'non-owner deleted completion'; end if;
    begin
        insert into public.completed_workouts
            (id, user_id, started_at, completed_at, distance_amount, distance_unit, duration_seconds)
        values ('cccccccc-cccc-4ccc-8ccc-cccccccccccc', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
            now() - interval '30 minutes', now(), 1000, 'METERS', 1800);
        raise exception 'non-owner inserted another user completion';
    exception when insufficient_privilege then null;
    end;
    insert into public.scheduled_workouts (id, user_id, workout_id, scheduled_date)
    values ('dddddddd-dddd-4ddd-8ddd-dddddddddddd', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
        '22222222-2222-4222-8222-222222222222', '2026-10-06');
end $$;
reset role;

-- Removing A's shared template must not erase B's private schedule row.
select set_config('request.jwt.claim.sub', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', true);
set local role authenticated;
delete from public.workouts where id = '22222222-2222-4222-8222-222222222222';
reset role;
select set_config('request.jwt.claim.sub', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', true);
set local role authenticated;
do $$
begin
    if (select count(*) from public.scheduled_workouts
        where id = 'dddddddd-dddd-4ddd-8ddd-dddddddddddd' and workout_id is null) <> 1 then
        raise exception 'shared template deletion erased another user schedule';
    end if;
end $$;
reset role;

rollback;
