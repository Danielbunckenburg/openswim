-- Local development only. These fictional users have no password and cannot sign in.
-- Never run this file against production or seed a remote project.
insert into auth.users (id, email, raw_user_meta_data) values
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'swimmer-a@example.invalid', '{}'),
    ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'swimmer-b@example.invalid', '{}');

update public.profiles set display_name = 'Sample Swimmer A', default_pool_length = 25
where id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
update public.profiles set display_name = 'Sample Swimmer B', default_pool_length = 50
where id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb';

insert into public.workouts (id, creator_id, title, description, category, visibility, estimated_minutes) values
    ('11111111-1111-4111-8111-111111111111', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
     'Private Easy Reset', 'Fictional private workout', 'Easy', 'PRIVATE', 24),
    ('22222222-2222-4222-8222-222222222222', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
     'Public Technique Builder', 'Fictional public template', 'Technique', 'PUBLIC', 30),
    ('33333333-3333-4333-8333-333333333333', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
     'Swimmer B Private Set', 'Fictional private workout', 'Aerobic', 'PRIVATE', 35),
    ('44444444-4444-4444-8444-444444444444', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
     'Unlisted Draft', 'Owner-only until link sharing exists', 'Easy', 'UNLISTED', 20);

insert into public.workout_sections (id, workout_id, position, name) values
    ('11111111-1111-4111-8111-111111111112', '11111111-1111-4111-8111-111111111111', 0, 'Warm up'),
    ('22222222-2222-4222-8222-222222222223', '22222222-2222-4222-8222-222222222222', 0, 'Main set'),
    ('33333333-3333-4333-8333-333333333334', '33333333-3333-4333-8333-333333333333', 0, 'Main set');

insert into public.workout_steps (id, section_id, position, repetitions, distance_amount, stroke, intensity, rest_after_seconds) values
    ('11111111-1111-4111-8111-111111111113', '11111111-1111-4111-8111-111111111112', 0, 4, 100, 'FREESTYLE', 'EASY', 20),
    ('22222222-2222-4222-8222-222222222224', '22222222-2222-4222-8222-222222222223', 0, 4, 50, 'DRILL', 'MODERATE', 15),
    ('33333333-3333-4333-8333-333333333335', '33333333-3333-4333-8333-333333333334', 0, 5, 100, 'FREESTYLE', 'MODERATE', 20);

insert into public.training_plans (id, creator_id, title, objective, duration_weeks, workouts_per_week, difficulty) values
    ('55555555-5555-4555-8555-555555555555', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
     'Sample Foundation Plan', 'Build a fictional routine', 4, 3, 'Beginner');
insert into public.training_plan_workouts (id, plan_id, workout_id, week_number, day_number) values
    ('66666666-6666-4666-8666-666666666666', '55555555-5555-4555-8555-555555555555',
     '11111111-1111-4111-8111-111111111111', 1, 1);

insert into public.scheduled_workouts (id, user_id, workout_id, plan_id, scheduled_date) values
    ('77777777-7777-4777-8777-777777777777', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
     '11111111-1111-4111-8111-111111111111', '55555555-5555-4555-8555-555555555555', '2026-10-05');

insert into public.completed_workouts
    (id, user_id, workout_id, started_at, completed_at, distance_amount, distance_unit, duration_seconds, pool_length)
values
    ('88888888-8888-4888-8888-888888888888', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
     '11111111-1111-4111-8111-111111111111', '2026-09-18 07:00:00+00', '2026-09-18 07:26:00+00',
     1000, 'METERS', 1560, 25);
insert into public.completed_intervals (id, completed_workout_id, position, distance_amount, duration_seconds, stroke) values
    ('99999999-9999-4999-8999-999999999999', '88888888-8888-4888-8888-888888888888', 0, 100, 150, 'FREESTYLE');
