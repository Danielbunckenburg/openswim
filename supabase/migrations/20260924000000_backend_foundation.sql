-- Client-facing tables are private unless a policy explicitly says otherwise.
-- auth.users is managed by Supabase Auth. Never put password material in public tables.

create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text check (char_length(display_name) <= 80),
    default_pool_length integer check (default_pool_length in (25, 50)),
    distance_unit text not null default 'METERS' check (distance_unit in ('METERS', 'YARDS')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.workouts (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    title text not null check (char_length(title) between 1 and 160),
    description text,
    category text not null check (category in ('Easy', 'Technique', 'Aerobic', 'Endurance', 'Threshold', 'Sprint')),
    visibility text not null default 'PRIVATE' check (visibility in ('PRIVATE', 'UNLISTED', 'PUBLIC')),
    estimated_minutes integer not null check (estimated_minutes > 0),
    distance_unit text not null default 'METERS' check (distance_unit in ('METERS', 'YARDS')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.workout_sections (
    id uuid primary key default gen_random_uuid(),
    workout_id uuid not null references public.workouts(id) on delete cascade,
    position integer not null check (position >= 0),
    name text not null check (char_length(name) between 1 and 80),
    unique (workout_id, position)
);

create table public.workout_steps (
    id uuid primary key default gen_random_uuid(),
    section_id uuid not null references public.workout_sections(id) on delete cascade,
    position integer not null check (position >= 0),
    repetitions integer not null default 1 check (repetitions > 0),
    distance_amount integer not null check (distance_amount > 0),
    stroke text not null check (stroke in ('FREESTYLE', 'BACKSTROKE', 'BREASTSTROKE', 'BUTTERFLY', 'INDIVIDUAL_MEDLEY', 'KICK', 'DRILL', 'CHOICE')),
    intensity text not null default 'MODERATE' check (intensity in ('EASY', 'MODERATE', 'STRONG', 'SPRINT')),
    equipment text[] not null default '{}',
    rest_after_seconds integer not null default 0 check (rest_after_seconds >= 0),
    target_pace_seconds_per_100 integer check (target_pace_seconds_per_100 > 0),
    target_interval_seconds integer check (target_interval_seconds > 0),
    note text,
    unique (section_id, position),
    check (equipment <@ array['FINS', 'PADDLES', 'PULL_BUOY', 'KICKBOARD']::text[])
);

create table public.training_plans (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    title text not null check (char_length(title) between 1 and 160),
    objective text,
    duration_weeks integer not null check (duration_weeks > 0),
    workouts_per_week integer not null check (workouts_per_week > 0),
    difficulty text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.training_plan_workouts (
    id uuid primary key default gen_random_uuid(),
    plan_id uuid not null references public.training_plans(id) on delete cascade,
    workout_id uuid not null references public.workouts(id) on delete cascade,
    week_number integer not null check (week_number > 0),
    day_number integer not null check (day_number between 1 and 7),
    position integer not null default 0 check (position >= 0),
    unique (plan_id, week_number, day_number, position)
);

create table public.scheduled_workouts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    workout_id uuid references public.workouts(id) on delete set null,
    plan_id uuid references public.training_plans(id) on delete set null,
    scheduled_date date not null,
    status text not null default 'SCHEDULED' check (status in ('SCHEDULED', 'COMPLETED', 'SKIPPED')),
    created_at timestamptz not null default now()
);

create table public.completed_workouts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade default auth.uid(),
    workout_id uuid references public.workouts(id) on delete set null,
    started_at timestamptz not null,
    completed_at timestamptz not null,
    distance_amount integer not null check (distance_amount > 0),
    distance_unit text not null check (distance_unit in ('METERS', 'YARDS')),
    duration_seconds integer not null check (duration_seconds > 0),
    pool_length integer check (pool_length > 0),
    created_at timestamptz not null default now(),
    check (completed_at >= started_at)
);

create table public.completed_intervals (
    id uuid primary key default gen_random_uuid(),
    completed_workout_id uuid not null references public.completed_workouts(id) on delete cascade,
    position integer not null check (position >= 0),
    distance_amount integer not null check (distance_amount > 0),
    duration_seconds integer not null check (duration_seconds > 0),
    stroke text check (stroke in ('FREESTYLE', 'BACKSTROKE', 'BREASTSTROKE', 'BUTTERFLY', 'INDIVIDUAL_MEDLEY', 'KICK', 'DRILL', 'CHOICE')),
    unique (completed_workout_id, position)
);

create index workouts_creator_idx on public.workouts(creator_id);
create index workout_sections_workout_idx on public.workout_sections(workout_id);
create index workout_steps_section_idx on public.workout_steps(section_id);
create index training_plans_creator_idx on public.training_plans(creator_id);
create index training_plan_workouts_workout_idx on public.training_plan_workouts(workout_id);
create index scheduled_workouts_user_date_idx on public.scheduled_workouts(user_id, scheduled_date);
create index completed_workouts_user_started_idx on public.completed_workouts(user_id, started_at desc);
create index completed_workouts_workout_idx on public.completed_workouts(workout_id);

create function public.set_updated_at() returns trigger
language plpgsql set search_path = '' as $$
begin
    new.updated_at = now();
    return new;
end;
$$;
create trigger profiles_updated_at before update on public.profiles for each row execute function public.set_updated_at();
create trigger workouts_updated_at before update on public.workouts for each row execute function public.set_updated_at();
create trigger training_plans_updated_at before update on public.training_plans for each row execute function public.set_updated_at();

create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    insert into public.profiles (id) values (new.id);
    return new;
end;
$$;
revoke execute on function public.handle_new_user() from public, anon, authenticated;
revoke execute on function public.set_updated_at() from public, anon, authenticated;
create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.workouts enable row level security;
alter table public.workout_sections enable row level security;
alter table public.workout_steps enable row level security;
alter table public.training_plans enable row level security;
alter table public.training_plan_workouts enable row level security;
alter table public.scheduled_workouts enable row level security;
alter table public.completed_workouts enable row level security;
alter table public.completed_intervals enable row level security;

-- Supabase projects can have broad default grants on new public tables.
revoke all on public.profiles, public.workouts, public.workout_sections, public.workout_steps,
    public.training_plans, public.training_plan_workouts, public.scheduled_workouts,
    public.completed_workouts, public.completed_intervals from anon, authenticated;
grant select on public.workouts, public.workout_sections, public.workout_steps to anon;
grant select on public.profiles to authenticated;
grant update (display_name, default_pool_length, distance_unit) on public.profiles to authenticated;
grant select, insert, update, delete on public.workouts, public.workout_sections, public.workout_steps,
    public.training_plans, public.training_plan_workouts, public.scheduled_workouts,
    public.completed_workouts, public.completed_intervals to authenticated;

create policy profiles_select_own on public.profiles for select to authenticated
using (id = (select auth.uid()));
create policy profiles_update_own on public.profiles for update to authenticated
using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy workouts_select_own on public.workouts for select to authenticated
using (creator_id = (select auth.uid()));
create policy workouts_select_public on public.workouts for select to anon, authenticated
using (visibility = 'PUBLIC');
create policy workouts_insert_own on public.workouts for insert to authenticated
with check (creator_id = (select auth.uid()));
create policy workouts_update_own on public.workouts for update to authenticated
using (creator_id = (select auth.uid())) with check (creator_id = (select auth.uid()));
create policy workouts_delete_own on public.workouts for delete to authenticated
using (creator_id = (select auth.uid()));

create policy sections_select_visible on public.workout_sections for select to anon, authenticated
using (exists (select 1 from public.workouts w where w.id = workout_id));
create policy sections_insert_own on public.workout_sections for insert to authenticated
with check (exists (select 1 from public.workouts w where w.id = workout_id and w.creator_id = (select auth.uid())));
create policy sections_update_own on public.workout_sections for update to authenticated
using (exists (select 1 from public.workouts w where w.id = workout_id and w.creator_id = (select auth.uid())))
with check (exists (select 1 from public.workouts w where w.id = workout_id and w.creator_id = (select auth.uid())));
create policy sections_delete_own on public.workout_sections for delete to authenticated
using (exists (select 1 from public.workouts w where w.id = workout_id and w.creator_id = (select auth.uid())));

create policy steps_select_visible on public.workout_steps for select to anon, authenticated
using (exists (select 1 from public.workout_sections s join public.workouts w on w.id = s.workout_id where s.id = section_id));
create policy steps_insert_own on public.workout_steps for insert to authenticated
with check (exists (select 1 from public.workout_sections s join public.workouts w on w.id = s.workout_id where s.id = section_id and w.creator_id = (select auth.uid())));
create policy steps_update_own on public.workout_steps for update to authenticated
using (exists (select 1 from public.workout_sections s join public.workouts w on w.id = s.workout_id where s.id = section_id and w.creator_id = (select auth.uid())))
with check (exists (select 1 from public.workout_sections s join public.workouts w on w.id = s.workout_id where s.id = section_id and w.creator_id = (select auth.uid())));
create policy steps_delete_own on public.workout_steps for delete to authenticated
using (exists (select 1 from public.workout_sections s join public.workouts w on w.id = s.workout_id where s.id = section_id and w.creator_id = (select auth.uid())));

create policy plans_select_own on public.training_plans for select to authenticated
using (creator_id = (select auth.uid()));
create policy plans_insert_own on public.training_plans for insert to authenticated
with check (creator_id = (select auth.uid()));
create policy plans_update_own on public.training_plans for update to authenticated
using (creator_id = (select auth.uid())) with check (creator_id = (select auth.uid()));
create policy plans_delete_own on public.training_plans for delete to authenticated
using (creator_id = (select auth.uid()));

create policy plan_workouts_select_own on public.training_plan_workouts for select to authenticated
using (exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid())));
create policy plan_workouts_insert_own on public.training_plan_workouts for insert to authenticated
with check (
    exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid()))
    and exists (select 1 from public.workouts w where w.id = workout_id and w.creator_id = (select auth.uid()))
);
create policy plan_workouts_update_own on public.training_plan_workouts for update to authenticated
using (exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid())))
with check (
    exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid()))
    and exists (select 1 from public.workouts w where w.id = workout_id and w.creator_id = (select auth.uid()))
);
create policy plan_workouts_delete_own on public.training_plan_workouts for delete to authenticated
using (exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid())));

create policy scheduled_select_own on public.scheduled_workouts for select to authenticated
using (user_id = (select auth.uid()));
create policy scheduled_insert_own on public.scheduled_workouts for insert to authenticated
with check (
    user_id = (select auth.uid())
    and (workout_id is null or exists (select 1 from public.workouts w where w.id = workout_id))
    and (plan_id is null or exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid())))
);
create policy scheduled_update_own on public.scheduled_workouts for update to authenticated
using (user_id = (select auth.uid()))
with check (
    user_id = (select auth.uid())
    and (workout_id is null or exists (select 1 from public.workouts w where w.id = workout_id))
    and (plan_id is null or exists (select 1 from public.training_plans p where p.id = plan_id and p.creator_id = (select auth.uid())))
);
create policy scheduled_delete_own on public.scheduled_workouts for delete to authenticated
using (user_id = (select auth.uid()));

create policy completed_select_own on public.completed_workouts for select to authenticated
using (user_id = (select auth.uid()));
create policy completed_insert_own on public.completed_workouts for insert to authenticated
with check (user_id = (select auth.uid()) and (workout_id is null or exists (select 1 from public.workouts w where w.id = workout_id)));
create policy completed_update_own on public.completed_workouts for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()) and (workout_id is null or exists (select 1 from public.workouts w where w.id = workout_id)));
create policy completed_delete_own on public.completed_workouts for delete to authenticated
using (user_id = (select auth.uid()));

create policy intervals_select_own on public.completed_intervals for select to authenticated
using (exists (select 1 from public.completed_workouts c where c.id = completed_workout_id and c.user_id = (select auth.uid())));
create policy intervals_insert_own on public.completed_intervals for insert to authenticated
with check (exists (select 1 from public.completed_workouts c where c.id = completed_workout_id and c.user_id = (select auth.uid())));
create policy intervals_update_own on public.completed_intervals for update to authenticated
using (exists (select 1 from public.completed_workouts c where c.id = completed_workout_id and c.user_id = (select auth.uid())))
with check (exists (select 1 from public.completed_workouts c where c.id = completed_workout_id and c.user_id = (select auth.uid())));
create policy intervals_delete_own on public.completed_intervals for delete to authenticated
using (exists (select 1 from public.completed_workouts c where c.id = completed_workout_id and c.user_id = (select auth.uid())));
