/* OpenSwim's web companion. All writes use the signed-in user's JWT and database RLS. */
(() => {
  'use strict';

  const config = window.OPEN_SWIM_CONFIG;
  const root = document.getElementById('view-content');
  const message = document.getElementById('message');
  if (!config?.url || !config?.publishableKey || !window.supabase?.createClient) {
    root.innerHTML = '<div class="empty-state"><h3>Connection unavailable</h3><p>The OpenSwim cloud client could not start. Please reload the page.</p></div>';
    return;
  }

  const client = window.supabase.createClient(config.url, config.publishableKey, {
    auth: { autoRefreshToken: true, persistSession: true, detectSessionInUrl: true }
  });
  const state = {
    view: 'overview', user: null, profile: null, workouts: [], plans: [], completed: [], scheduled: [],
    category: 'All', detailId: null, showWorkoutForm: false, showPlanForm: false, showScheduleForm: false,
    showResultForm: false, loading: true
  };
  const meta = {
    overview: ['YOUR DASHBOARD', 'Overview', 'Your swimming, all in one place.'],
    workouts: ['THE LIBRARY', 'Workouts', 'Find a session or create one of your own.'],
    plans: ['TRAIN WITH PURPOSE', 'Plans', 'Shape the weeks ahead.'],
    progress: ['LOOK BACK, MOVE FORWARD', 'Progress', 'A clear record of every swim.'],
    account: ['YOUR SPACE', 'Account', 'One account across web and Android.']
  };
  const categories = ['All', 'Easy', 'Technique', 'Aerobic', 'Endurance', 'Threshold', 'Sprint'];
  const strokes = ['FREESTYLE', 'BACKSTROKE', 'BREASTSTROKE', 'BUTTERFLY', 'INDIVIDUAL_MEDLEY', 'KICK', 'DRILL', 'CHOICE'];
  const intensities = ['EASY', 'MODERATE', 'STRONG', 'SPRINT'];
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

  const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
  const pretty = value => String(value ?? '').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, char => char.toUpperCase());
  const distance = (amount, unit = 'METERS') => `${Number(amount || 0).toLocaleString()} ${unit === 'YARDS' ? 'yd' : 'm'}`;
  const loggedDistance = swims => {
    const meters = swims.filter(swim => swim.distance_unit === 'METERS').reduce((sum, swim) => sum + swim.distance_amount, 0);
    const yards = swims.filter(swim => swim.distance_unit === 'YARDS').reduce((sum, swim) => sum + swim.distance_amount, 0);
    return [meters ? distance(meters) : '', yards ? distance(yards, 'YARDS') : ''].filter(Boolean).join(' · ') || '0 m';
  };
  const dateText = value => value ? new Intl.DateTimeFormat('en', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value)) : 'Date unavailable';
  const owned = () => state.workouts.filter(workout => workout.creator_id === state.user?.id);
  const workoutName = id => state.workouts.find(workout => workout.id === id)?.title || 'Swim session';
  const totalDistance = workout => (workout.workout_sections || []).reduce((total, section) => total + (section.workout_steps || []).reduce((sum, step) => sum + step.repetitions * step.distance_amount, 0), 0);
  const sortedSections = workout => [...(workout.workout_sections || [])].sort((a, b) => a.position - b.position);
  const requireData = result => { if (result.error) throw result.error; return result.data; };
  const selected = () => state.workouts.find(workout => workout.id === state.detailId);

  function notify(text, isError = false) {
    message.textContent = text;
    message.classList.toggle('is-error', isError);
    message.hidden = !text;
  }
  function setView(view) {
    if (!meta[view]) return;
    state.view = view;
    state.detailId = null;
    document.querySelectorAll('[data-view]').forEach(button => {
      button.classList.toggle('is-active', button.dataset.view === view);
      if (button.dataset.view === view) button.setAttribute('aria-current', 'page');
      else button.removeAttribute('aria-current');
    });
    render();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  function render() {
    const [eyebrow, title, subtitle] = meta[state.view];
    document.getElementById('page-eyebrow').textContent = eyebrow;
    document.getElementById('page-title').textContent = title;
    document.getElementById('page-subtitle').textContent = subtitle;
    document.getElementById('account-label').textContent = state.user?.email || 'Guest';
    document.getElementById('header-account').textContent = state.user ? 'My account' : 'Sign in';
    document.getElementById('refresh-button').disabled = state.loading;
    root.innerHTML = state.loading ? '<div class="loading-card">Loading your swimming data…</div>' : ({
      overview: overviewView, workouts: workoutsView, plans: plansView, progress: progressView, account: accountView
    })[state.view]();
  }

  async function loadData() {
    state.loading = true;
    render();
    try {
      const user = state.user;
      const requests = [client.from('workouts').select('id,creator_id,title,description,category,visibility,estimated_minutes,distance_unit,created_at,workout_sections(id,name,position,workout_steps(position,repetitions,distance_amount,stroke,intensity,equipment,rest_after_seconds,target_pace_seconds_per_100,target_interval_seconds,note))').order('created_at', { ascending: false })];
      if (user) requests.push(
        client.from('training_plans').select('id,title,objective,duration_weeks,workouts_per_week,difficulty,training_plan_workouts(id,workout_id,week_number,day_number,position)').order('created_at', { ascending: false }),
        client.from('completed_workouts').select('id,workout_id,started_at,completed_at,distance_amount,distance_unit,duration_seconds,pool_length').order('started_at', { ascending: false }),
        client.from('scheduled_workouts').select('id,workout_id,plan_id,scheduled_date,status').order('scheduled_date', { ascending: true }),
        client.from('profiles').select('id,display_name,default_pool_length,distance_unit').eq('id', user.id).maybeSingle()
      );
      const results = await Promise.all(requests);
      if (user?.id !== state.user?.id) return;
      results.forEach(requireData);
      state.workouts = results[0].data || [];
      state.plans = user ? results[1].data || [] : [];
      state.completed = user ? results[2].data || [] : [];
      state.scheduled = user ? results[3].data || [] : [];
      state.profile = user ? results[4].data : null;
      notify('');
    } catch (error) {
      notify(error.message || 'Could not load cloud data. Try Refresh.', true);
    } finally {
      state.loading = false;
      render();
    }
  }

  function empty(title, text, action = '') {
    return `<div class="empty-state"><h3>${escapeHtml(title)}</h3><p>${escapeHtml(text)}</p>${action}</div>`;
  }
  function sectionHead(title, subtitle = '', action = '') {
    return `<div class="section-head"><div><h2>${escapeHtml(title)}</h2>${subtitle ? `<p>${escapeHtml(subtitle)}</p>` : ''}</div>${action}</div>`;
  }
  function workoutCard(workout) {
    const count = totalDistance(workout);
    return `<button type="button" class="list-card" data-action="workout-detail" data-id="${escapeHtml(workout.id)}"><span><h3>${escapeHtml(workout.title)}</h3><p>${escapeHtml(workout.category)} · ${escapeHtml(distance(count, workout.distance_unit))} · ~${Number(workout.estimated_minutes)} min</p></span><span class="badge ${workout.visibility === 'PUBLIC' ? 'public' : 'private'}">${workout.visibility === 'PUBLIC' ? 'Public' : 'Private'}</span><span class="arrow" aria-hidden="true">→</span></button>`;
  }

  function overviewView() {
    const upcoming = state.scheduled.find(item => item.status === 'SCHEDULED' && item.scheduled_date >= new Date().toISOString().slice(0, 10));
    const recent = state.completed.slice(0, 3);
    return `<section class="hero-card"><div><span class="eyebrow" style="color:#74e4d3">OPEN WATER. OPEN POSSIBILITIES.</span><h2>${state.user ? `Welcome back${state.profile?.display_name ? `, ${escapeHtml(state.profile.display_name)}` : ''}.` : 'Your next swim starts here.'}</h2><p>${state.user ? 'Your workouts, plans and swims are ready wherever you train.' : 'Browse public workouts now. Sign in to see your plans and progress across devices.'}</p></div><div class="hero-side"><span>≈</span></div></section>
      <div class="stats-grid"><div class="stat"><span class="stat-label">Workouts available</span><strong>${state.workouts.length}</strong><small>public and yours</small></div><div class="stat"><span class="stat-label">Completed swims</span><strong>${state.completed.length}</strong><small>${state.user ? 'in your account' : 'sign in to view'}</small></div><div class="stat"><span class="stat-label">Distance logged</span><strong>${escapeHtml(loggedDistance(state.completed))}</strong><small>${state.user ? 'from recorded swims' : 'sign in to view'}</small></div></div>
      <div class="two-col"><div>${sectionHead('Up next', 'Your next scheduled session', '<button class="text-button" data-view="plans">View plans →</button>')}${upcoming ? `<div class="card"><span class="badge">${escapeHtml(dateText(upcoming.scheduled_date))}</span><h2 style="margin-top:15px">${escapeHtml(workoutName(upcoming.workout_id))}</h2><p>Scheduled swim</p></div>` : empty('A clear lane ahead', state.user ? 'No swim is scheduled yet. Add one from Plans.' : 'Sign in to see your schedule.')}</div><div>${sectionHead('Recent activity', 'Your latest completed swims', '<button class="text-button" data-view="progress">View progress →</button>')}${recent.length ? `<div class="card activity-list">${recent.map(swim => activityRow(swim)).join('')}</div>` : empty('No swims logged yet', state.user ? 'Record a swim to start your history.' : 'Sign in to see your training history.')}</div></div>
      ${sectionHead('Explore workouts', 'Built for the next session', '<button class="text-button" data-view="workouts">Open library →</button>')}${state.workouts.length ? `<div class="stack">${state.workouts.slice(0, 3).map(workoutCard).join('')}</div>` : empty('The library is ready', 'No public workouts have been published yet. Sign in to create one.')}`;
  }

  function workoutDetail(workout) {
    return `<article class="card detail-panel"><div class="detail-header"><div><button class="text-button" data-action="close-detail">← Back to library</button><h2>${escapeHtml(workout.title)}</h2><div class="detail-meta">${escapeHtml(workout.category)} · ${escapeHtml(distance(totalDistance(workout), workout.distance_unit))} · ~${Number(workout.estimated_minutes)} min</div></div><span class="badge ${workout.visibility === 'PUBLIC' ? 'public' : 'private'}">${escapeHtml(pretty(workout.visibility))}</span></div>${workout.description ? `<p style="margin-top:17px">${escapeHtml(workout.description)}</p>` : ''}${sortedSections(workout).map(section => `<div class="section-block"><h3>${escapeHtml(section.name)}</h3>${[...(section.workout_steps || [])].sort((a, b) => a.position - b.position).map(step => `<div class="step-row"><span><strong>${Number(step.repetitions)} × ${escapeHtml(distance(step.distance_amount, workout.distance_unit))}</strong> ${escapeHtml(pretty(step.stroke))}</span><small>${escapeHtml(pretty(step.intensity))}${step.rest_after_seconds ? ` · ${Number(step.rest_after_seconds)}s rest` : ''}${step.note ? `<br>${escapeHtml(step.note)}` : ''}</small></div>`).join('') || '<p class="muted">No steps yet.</p>'}</div>`).join('') || '<div class="section-block"><p class="muted">This workout has no steps yet.</p></div>'}</article>`;
  }
  function workoutForm() {
    return `<form id="workout-form" class="card form-card"><h2>Create a workout</h2><div class="form-grid"><div class="field wide"><label for="workout-title">Name</label><input id="workout-title" name="title" required maxlength="160" placeholder="Morning technique set"></div><div class="field"><label for="workout-category">Category</label><select id="workout-category" name="category">${categories.slice(1).map(item => `<option>${item}</option>`).join('')}</select></div><div class="field"><label for="workout-minutes">Estimated minutes</label><input id="workout-minutes" name="minutes" type="number" min="1" max="600" value="30" required></div><div class="field"><label for="workout-unit">Distance unit</label><select id="workout-unit" name="unit"><option value="METERS">Meters</option><option value="YARDS">Yards</option></select></div><div class="field"><label for="workout-visibility">Visibility</label><select id="workout-visibility" name="visibility"><option value="PRIVATE">Private</option><option value="PUBLIC">Public</option></select></div><div class="field wide"><label for="workout-description">Description (optional)</label><textarea id="workout-description" name="description" maxlength="1000" placeholder="What is this session for?"></textarea></div><div class="field wide"><label for="workout-section">Set name</label><input id="workout-section" name="section" maxlength="80" value="Main set" required></div><div class="field"><label for="workout-reps">Repetitions</label><input id="workout-reps" name="repetitions" type="number" min="1" max="100" value="4" required></div><div class="field"><label for="workout-distance">Distance per repetition</label><input id="workout-distance" name="stepDistance" type="number" min="1" max="10000" value="100" required></div><div class="field"><label for="workout-stroke">Stroke</label><select id="workout-stroke" name="stroke">${strokes.map(value => `<option value="${value}">${pretty(value)}</option>`).join('')}</select></div><div class="field"><label for="workout-intensity">Intensity</label><select id="workout-intensity" name="intensity">${intensities.map(value => `<option value="${value}">${pretty(value)}</option>`).join('')}</select></div><div class="field"><label for="workout-rest">Rest after each repetition (seconds)</label><input id="workout-rest" name="rest" type="number" min="0" max="1800" value="20" required></div></div><div class="form-actions"><button class="primary-button" type="submit">Save workout</button><button class="secondary-button" type="button" data-action="toggle-workout-form">Cancel</button></div><p class="notice-line">New workouts start with one set. Private is visible only to your account; public workouts can be viewed by everyone.</p></form>`;
  }
  function workoutsView() {
    const detail = selected();
    const filtered = state.workouts.filter(workout => state.category === 'All' || workout.category === state.category);
    return `${detail ? workoutDetail(detail) : ''}<div class="toolbar"><div class="filters" role="group" aria-label="Workout category">${categories.map(category => `<button class="filter ${state.category === category ? 'is-active' : ''}" data-category="${category}" type="button">${category}</button>`).join('')}</div>${state.user ? '<button class="primary-button" type="button" data-action="toggle-workout-form">+ Create workout</button>' : '<button class="secondary-button" type="button" data-view="account">Sign in to create</button>'}</div>${state.showWorkoutForm && state.user ? workoutForm() : ''}${sectionHead('Workout library', `${filtered.length} ${filtered.length === 1 ? 'workout' : 'workouts'}`)}${filtered.length ? `<div class="stack">${filtered.map(workoutCard).join('')}</div>` : empty('No workouts here yet', state.category === 'All' ? 'Create a workout to start the library.' : 'Try another category or create a workout.')}`;
  }

  function planForm() {
    const choices = owned();
    if (!choices.length) return empty('Create your first workout', 'Plans can use workouts you own. Add a workout from the library first.', '<button class="primary-button" type="button" data-view="workouts" style="margin-top:17px">Go to workouts</button>');
    return `<form id="plan-form" class="card form-card"><h2>Create a plan</h2><div class="form-grid"><div class="field wide"><label for="plan-title">Plan name</label><input id="plan-title" name="title" maxlength="160" required placeholder="Four weeks of steady swimming"></div><div class="field wide"><label for="plan-objective">Goal</label><input id="plan-objective" name="objective" placeholder="Build endurance"></div><div class="field"><label for="plan-weeks">Duration (weeks)</label><input id="plan-weeks" name="weeks" type="number" min="1" max="52" value="4" required></div><div class="field"><label for="plan-per-week">Swims per week</label><input id="plan-per-week" name="perWeek" type="number" min="1" max="7" value="2" required></div><div class="field"><label for="plan-difficulty">Difficulty</label><select id="plan-difficulty" name="difficulty"><option>Beginner</option><option>Intermediate</option><option>Advanced</option></select></div><div class="field"><label for="plan-workout">First workout</label><select id="plan-workout" name="workoutId">${choices.map(workout => `<option value="${escapeHtml(workout.id)}">${escapeHtml(workout.title)}</option>`).join('')}</select></div><div class="field"><label for="plan-day">Day of week</label><select id="plan-day" name="day">${days.map((day, index) => `<option value="${index + 1}">${day}</option>`).join('')}</select></div></div><div class="form-actions"><button class="primary-button" type="submit">Save plan</button><button class="secondary-button" type="button" data-action="toggle-plan-form">Cancel</button></div><p class="notice-line">The first workout goes in week 1. More editing options will follow.</p></form>`;
  }
  function scheduleForm() {
    if (!state.workouts.length) return empty('No workouts to schedule', 'Create a workout first.');
    return `<form id="schedule-form" class="card form-card"><h2>Schedule a swim</h2><div class="form-grid"><div class="field"><label for="schedule-date">Date</label><input id="schedule-date" name="date" type="date" min="${new Date().toISOString().slice(0, 10)}" required></div><div class="field"><label for="schedule-workout">Workout</label><select id="schedule-workout" name="workoutId">${state.workouts.map(workout => `<option value="${escapeHtml(workout.id)}">${escapeHtml(workout.title)}</option>`).join('')}</select></div></div><div class="form-actions"><button class="primary-button" type="submit">Add to schedule</button><button class="secondary-button" type="button" data-action="toggle-schedule-form">Cancel</button></div></form>`;
  }
  function plansView() {
    if (!state.user) return empty('Your plans are private', 'Sign in to view or create a training plan.', '<button class="primary-button" type="button" data-view="account" style="margin-top:17px">Sign in</button>');
    return `<div class="toolbar"><div class="muted">Build your own rhythm in the water.</div><div><button class="secondary-button" type="button" data-action="toggle-schedule-form">+ Schedule swim</button> <button class="primary-button" type="button" data-action="toggle-plan-form">+ Create plan</button></div></div>${state.showPlanForm ? planForm() : ''}${state.showScheduleForm ? scheduleForm() : ''}${sectionHead('Training plans', `${state.plans.length} ${state.plans.length === 1 ? 'plan' : 'plans'}`)}${state.plans.length ? `<div class="stack">${state.plans.map(plan => `<article class="card"><span class="badge private">${Number(plan.duration_weeks)} weeks</span><h2 style="margin-top:12px">${escapeHtml(plan.title)}</h2><p>${escapeHtml(plan.objective || 'Your training plan')} · ${Number(plan.workouts_per_week)} swims per week · ${escapeHtml(plan.difficulty || 'Flexible')}</p><div class="section-block"><h3>Plan workouts</h3>${[...(plan.training_plan_workouts || [])].sort((a, b) => a.week_number - b.week_number || a.day_number - b.day_number).map(item => `<div class="step-row"><span>${escapeHtml(days[item.day_number - 1] || 'Day')} · ${escapeHtml(workoutName(item.workout_id))}</span><small>Week ${Number(item.week_number)}</small></div>`).join('') || '<p class="muted">No workouts added yet.</p>'}</div></article>`).join('')}</div>` : empty('No plans yet', 'Create a plan using one of your own workouts.')}${sectionHead('Schedule', 'Upcoming swims')}${state.scheduled.length ? `<div class="stack">${state.scheduled.map(item => `<div class="list-card"><span><h3>${escapeHtml(workoutName(item.workout_id))}</h3><p>${escapeHtml(dateText(item.scheduled_date))}</p></span><span class="badge ${item.status === 'SCHEDULED' ? '' : 'private'}">${escapeHtml(pretty(item.status))}</span></div>`).join('')}</div>` : empty('Nothing scheduled', 'Choose a workout and set a date to see it here.')}`;
  }

  function activityRow(swim) {
    return `<div class="activity-row"><span><strong>${escapeHtml(workoutName(swim.workout_id))}</strong><small>${escapeHtml(dateText(swim.started_at))} · ${Math.ceil(swim.duration_seconds / 60)} min</small></span><span class="distance">${escapeHtml(distance(swim.distance_amount, swim.distance_unit))}</span></div>`;
  }
  function resultForm() {
    return `<form id="result-form" class="card form-card"><h2>Log a swim</h2><div class="form-grid"><div class="field"><label for="result-distance">Distance</label><input id="result-distance" name="distance" type="number" min="1" max="100000" required placeholder="1200"></div><div class="field"><label for="result-unit">Unit</label><select id="result-unit" name="unit"><option value="METERS">Meters</option><option value="YARDS">Yards</option></select></div><div class="field"><label for="result-duration">Duration (minutes)</label><input id="result-duration" name="minutes" type="number" min="1" max="1440" required placeholder="30"></div><div class="field"><label for="result-workout">Workout (optional)</label><select id="result-workout" name="workoutId"><option value="">No workout</option>${state.workouts.map(workout => `<option value="${escapeHtml(workout.id)}">${escapeHtml(workout.title)}</option>`).join('')}</select></div></div><div class="form-actions"><button class="primary-button" type="submit">Save swim</button><button class="secondary-button" type="button" data-action="toggle-result-form">Cancel</button></div><p class="notice-line">Manual entries include only the distance and time you provide. No sensor data is invented.</p></form>`;
  }
  function progressView() {
    if (!state.user) return empty('Your swims are private', 'Sign in to view and log your training history.', '<button class="primary-button" type="button" data-view="account" style="margin-top:17px">Sign in</button>');
    const minutes = state.completed.reduce((sum, swim) => sum + Math.ceil(swim.duration_seconds / 60), 0);
    return `<div class="toolbar"><div class="muted">Every swim adds to the story.</div><button class="primary-button" type="button" data-action="toggle-result-form">+ Log a swim</button></div>${state.showResultForm ? resultForm() : ''}<div class="stats-grid"><div class="stat"><span class="stat-label">Total swims</span><strong>${state.completed.length}</strong><small>recorded sessions</small></div><div class="stat"><span class="stat-label">Distance</span><strong>${escapeHtml(loggedDistance(state.completed))}</strong><small>across all sessions</small></div><div class="stat"><span class="stat-label">Time in water</span><strong>${minutes}</strong><small>minutes logged</small></div></div>${sectionHead('Completed workouts', 'Your most recent swims first')}${state.completed.length ? `<div class="card activity-list">${state.completed.map(activityRow).join('')}</div>` : empty('No swims logged yet', 'Use “Log a swim” to record your first session.')}`;
  }

  function accountView() {
    if (!state.user) return `<div class="auth-layout"><form id="sign-in-form" class="card auth-card"><span class="eyebrow">WELCOME BACK</span><h2>Sign in</h2><p>Your workouts and history follow your account across web and Android.</p><div class="field"><label for="sign-in-email">Email</label><input id="sign-in-email" name="email" type="email" autocomplete="email" required></div><div class="field"><label for="sign-in-password">Password</label><input id="sign-in-password" name="password" type="password" autocomplete="current-password" required></div><div class="form-actions"><button class="primary-button" type="submit">Sign in</button></div></form><form id="sign-up-form" class="card auth-card"><span class="eyebrow">JOIN OPEN SWIM</span><h2>Create an account</h2><p>Start a private space for your training.</p><div class="field"><label for="sign-up-email">Email</label><input id="sign-up-email" name="email" type="email" autocomplete="email" required></div><div class="field"><label for="sign-up-password">Password</label><input id="sign-up-password" name="password" type="password" autocomplete="new-password" minlength="6" required></div><div class="form-actions"><button class="primary-button" type="submit">Create account</button></div><p class="notice-line">Early access: confirmation emails currently reach project team addresses only.</p></form></div>`;
    return `<div class="card account-summary"><div class="avatar" aria-hidden="true">${escapeHtml((state.profile?.display_name || state.user.email || 'S').slice(0, 1).toUpperCase())}</div><div><h2>${escapeHtml(state.profile?.display_name || 'OpenSwim account')}</h2><p>${escapeHtml(state.user.email)}</p></div><button type="button" class="secondary-button" data-action="sign-out">Sign out</button></div><form id="profile-form" class="card form-card"><h2>Swimming preferences</h2><div class="form-grid"><div class="field wide"><label for="profile-name">Display name</label><input id="profile-name" name="name" maxlength="80" value="${escapeHtml(state.profile?.display_name || '')}" placeholder="What should we call you?"></div><div class="field"><label for="profile-unit">Distance unit</label><select id="profile-unit" name="unit"><option value="METERS" ${state.profile?.distance_unit !== 'YARDS' ? 'selected' : ''}>Meters</option><option value="YARDS" ${state.profile?.distance_unit === 'YARDS' ? 'selected' : ''}>Yards</option></select></div><div class="field"><label for="profile-pool">Default pool</label><select id="profile-pool" name="pool"><option value="">Not set</option><option value="25" ${state.profile?.default_pool_length === 25 ? 'selected' : ''}>25 m</option><option value="50" ${state.profile?.default_pool_length === 50 ? 'selected' : ''}>50 m</option></select></div></div><div class="form-actions"><button class="primary-button" type="submit">Save preferences</button></div></form><div class="card form-card"><h2>Your data</h2><p>Your private plans, schedule and swim history are stored in OpenSwim Cloud. Public workouts are visible to everyone. The phone and web app use the same account.</p><p class="notice-line">Watch pairing and automatic swim sync are still in development.</p></div>`;
  }

  async function saveWorkout(form) {
    const values = Object.fromEntries(new FormData(form));
    const minutes = Number(values.minutes), repetitions = Number(values.repetitions), stepDistance = Number(values.stepDistance), rest = Number(values.rest);
    if (!values.title.trim() || !values.section.trim() || !Number.isInteger(minutes) || minutes < 1 || !Number.isInteger(repetitions) || repetitions < 1 || !Number.isInteger(stepDistance) || stepDistance < 1 || !Number.isInteger(rest) || rest < 0) throw new Error('Please enter a valid workout.');
    const workout = requireData(await client.from('workouts').insert({ title: values.title.trim(), description: values.description.trim() || null, category: values.category, estimated_minutes: minutes, distance_unit: values.unit, visibility: 'PRIVATE' }).select('id').single());
    try {
      const section = requireData(await client.from('workout_sections').insert({ workout_id: workout.id, position: 0, name: values.section.trim() }).select('id').single());
      requireData(await client.from('workout_steps').insert({ section_id: section.id, position: 0, repetitions, distance_amount: stepDistance, stroke: values.stroke, intensity: values.intensity, rest_after_seconds: rest }));
      if (values.visibility === 'PUBLIC') requireData(await client.from('workouts').update({ visibility: 'PUBLIC' }).eq('id', workout.id));
    } catch (error) {
      await client.from('workouts').delete().eq('id', workout.id);
      throw error;
    }
    state.showWorkoutForm = false;
    await loadData();
    notify('Workout saved. It is available in the web and Android apps.');
  }
  async function savePlan(form) {
    const values = Object.fromEntries(new FormData(form));
    const weeks = Number(values.weeks), perWeek = Number(values.perWeek), day = Number(values.day);
    if (!values.title.trim() || !owned().some(workout => workout.id === values.workoutId) || !Number.isInteger(weeks) || weeks < 1 || !Number.isInteger(perWeek) || perWeek < 1 || !Number.isInteger(day) || day < 1 || day > 7) throw new Error('Please enter a valid plan.');
    const plan = requireData(await client.from('training_plans').insert({ title: values.title.trim(), objective: values.objective.trim() || null, duration_weeks: weeks, workouts_per_week: perWeek, difficulty: values.difficulty }).select('id').single());
    try { requireData(await client.from('training_plan_workouts').insert({ plan_id: plan.id, workout_id: values.workoutId, week_number: 1, day_number: day, position: 0 })); }
    catch (error) { await client.from('training_plans').delete().eq('id', plan.id); throw error; }
    state.showPlanForm = false;
    await loadData();
    notify('Plan saved to your account.');
  }
  async function saveSchedule(form) {
    const values = Object.fromEntries(new FormData(form));
    if (!/^\d{4}-\d{2}-\d{2}$/.test(values.date) || !state.workouts.some(workout => workout.id === values.workoutId)) throw new Error('Please choose a valid date and workout.');
    requireData(await client.from('scheduled_workouts').insert({ scheduled_date: values.date, workout_id: values.workoutId }));
    state.showScheduleForm = false;
    await loadData();
    notify('Swim added to your schedule.');
  }
  async function saveResult(form) {
    const values = Object.fromEntries(new FormData(form));
    const amount = Number(values.distance), minutes = Number(values.minutes);
    if (!Number.isInteger(amount) || amount < 1 || !Number.isInteger(minutes) || minutes < 1 || (values.workoutId && !state.workouts.some(workout => workout.id === values.workoutId))) throw new Error('Please enter a valid distance and duration.');
    const completed = new Date();
    const started = new Date(completed.getTime() - minutes * 60_000);
    requireData(await client.from('completed_workouts').insert({ workout_id: values.workoutId || null, started_at: started.toISOString(), completed_at: completed.toISOString(), distance_amount: amount, distance_unit: values.unit, duration_seconds: minutes * 60 }));
    state.showResultForm = false;
    await loadData();
    notify('Swim saved. It will appear in the Android app for this account.');
  }

  document.addEventListener('click', async event => {
    const nav = event.target.closest('[data-view]');
    if (nav) { setView(nav.dataset.view); return; }
    const category = event.target.closest('[data-category]');
    if (category) { state.category = category.dataset.category; render(); return; }
    const button = event.target.closest('[data-action]');
    if (!button) return;
    const action = button.dataset.action;
    if (action === 'workout-detail') { state.detailId = button.dataset.id; render(); window.scrollTo({ top: 0, behavior: 'smooth' }); }
    else if (action === 'close-detail') { state.detailId = null; render(); }
    else if (action.startsWith('toggle-')) { const property = { 'toggle-workout-form': 'showWorkoutForm', 'toggle-plan-form': 'showPlanForm', 'toggle-schedule-form': 'showScheduleForm', 'toggle-result-form': 'showResultForm' }[action]; if (property) { state[property] = !state[property]; render(); } }
    else if (action === 'sign-out') { button.disabled = true; const { error } = await client.auth.signOut(); if (error) { button.disabled = false; notify(error.message, true); } else notify('Signed out.'); }
  });
  document.addEventListener('submit', async event => {
    const form = event.target;
    if (!['sign-in-form', 'sign-up-form', 'workout-form', 'plan-form', 'schedule-form', 'result-form', 'profile-form'].includes(form.id)) return;
    event.preventDefault();
    const button = form.querySelector('[type="submit"]');
    button.disabled = true;
    try {
      if (form.id === 'sign-in-form') {
        const values = Object.fromEntries(new FormData(form));
        requireData(await client.auth.signInWithPassword({ email: values.email.trim(), password: values.password }));
        notify('Signed in. Your data is loading.');
      } else if (form.id === 'sign-up-form') {
        const values = Object.fromEntries(new FormData(form));
        requireData(await client.auth.signUp({ email: values.email.trim(), password: values.password, options: { emailRedirectTo: new URL('app.html', location.href).href } }));
        notify('Check your email to confirm your account, then return here to sign in.');
      } else if (!state.user) throw new Error('Sign in to save your data.');
      else if (form.id === 'workout-form') await saveWorkout(form);
      else if (form.id === 'plan-form') await savePlan(form);
      else if (form.id === 'schedule-form') await saveSchedule(form);
      else if (form.id === 'result-form') await saveResult(form);
      else if (form.id === 'profile-form') {
        const values = Object.fromEntries(new FormData(form));
        requireData(await client.from('profiles').update({ display_name: values.name.trim() || null, distance_unit: values.unit, default_pool_length: values.pool ? Number(values.pool) : null }).eq('id', state.user.id));
        await loadData();
        notify('Preferences saved.');
      }
    } catch (error) { notify(error.message || 'Could not save your changes.', true); }
    finally { if (button.isConnected) button.disabled = false; }
  });
  document.getElementById('header-account').addEventListener('click', () => setView('account'));
  document.getElementById('refresh-button').addEventListener('click', loadData);

  client.auth.onAuthStateChange((event, session) => {
    if (event === 'SIGNED_IN' || event === 'SIGNED_OUT' || event === 'USER_UPDATED') {
      setTimeout(() => { state.user = session?.user || null; loadData(); }, 0);
    }
  });
  client.auth.getSession().then(({ data, error }) => {
    if (error) notify(error.message, true);
    state.user = data?.session?.user || null;
    loadData();
  }).catch(error => { state.loading = false; render(); notify(error.message, true); });
})();
