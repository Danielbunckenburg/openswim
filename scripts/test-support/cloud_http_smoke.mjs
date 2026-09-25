// Read-only hosted API checks using a client-safe publishable key.
import assert from 'node:assert/strict';

const baseUrl = process.env.SUPABASE_URL;
const key = process.env.SUPABASE_PUBLISHABLE_KEY;
assert.ok(baseUrl?.startsWith('https://'), 'SUPABASE_URL must be an HTTPS project URL');
assert.ok(key?.startsWith('sb_publishable_'), 'SUPABASE_PUBLISHABLE_KEY is required');

async function get(path) {
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}${path}`, {
    headers: { apikey: key, Accept: 'application/json' },
  });
  return { status: response.status, body: await response.text() };
}

const health = await get('/auth/v1/health');
assert.equal(health.status, 200, `Auth health: HTTP ${health.status}`);

const publicWorkouts = await get('/rest/v1/workouts?select=id&limit=1');
assert.equal(publicWorkouts.status, 200, `public workouts: HTTP ${publicWorkouts.status}`);
assert.ok(Array.isArray(JSON.parse(publicWorkouts.body)), 'workouts response must be an array');

const nestedWorkouts = await get('/rest/v1/workouts?select=id,title,category,workout_sections(id,name,position,workout_steps(position,repetitions,distance_amount,stroke,intensity))&limit=1');
assert.equal(nestedWorkouts.status, 200, `nested workout library: HTTP ${nestedWorkouts.status}`);
assert.ok(Array.isArray(JSON.parse(nestedWorkouts.body)), 'nested workout response must be an array');

const privateResults = await get('/rest/v1/completed_workouts?select=id&limit=1');
assert.ok([401, 403].includes(privateResults.status),
  `anonymous private results should be denied, got HTTP ${privateResults.status}`);

console.log('Cloud HTTPS smoke checks passed: Auth healthy, nested public workouts readable, private results denied.');
