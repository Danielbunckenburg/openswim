// Run against disposable local Supabase Auth and PostgREST services.
// AUTH_URL and REST_URL default to the ports used by docs/backend/SECURITY.md.
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';

const authUrl = process.env.AUTH_URL ?? 'http://127.0.0.1:9999';
const restUrl = process.env.REST_URL ?? 'http://127.0.0.1:3001';
const mailpitUrl = process.env.MAILPIT_URL;
const apiKey = process.env.SUPABASE_ANON_KEY;
const suffix = randomBytes(8).toString('hex');

async function request(url, options = {}) {
  const response = await fetch(url, options);
  const raw = await response.text();
  let body;
  try { body = raw ? JSON.parse(raw) : null; } catch { body = raw; }
  return { status: response.status, body };
}

async function signUp(label) {
  const email = `openswim-${label}-${suffix}@example.test`;
  const password = randomBytes(24).toString('base64url');
  const signup = await request(`${authUrl}/signup`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', ...(apiKey ? { apikey: apiKey } : {}) },
    body: JSON.stringify({ email, password }),
  });
  assert.equal(signup.status, 200, `signup ${label}: ${JSON.stringify(signup.body)}`);
  if (!signup.body.access_token && mailpitUrl) await confirmEmail(email);
  const login = await request(`${authUrl}/token?grant_type=password`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', ...(apiKey ? { apikey: apiKey } : {}) },
    body: JSON.stringify({ email, password }),
  });
  assert.equal(login.status, 200, `login ${label}: ${JSON.stringify(login.body)}`);
  assert.ok(login.body.access_token);
  return { id: login.body.user.id, token: login.body.access_token };
}

async function confirmEmail(email) {
  let message;
  for (let attempt = 0; attempt < 20; attempt++) {
    const inbox = await request(`${mailpitUrl}/api/v1/messages`);
    assert.equal(inbox.status, 200, 'Mailpit message list');
    message = inbox.body.messages.find(item => item.To.some(recipient => recipient.Address === email));
    if (message) break;
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  assert.ok(message, `confirmation email not received for ${email}`);
  const detail = await request(`${mailpitUrl}/api/v1/message/${encodeURIComponent(message.ID)}`);
  assert.equal(detail.status, 200, 'Mailpit message detail');
  const link = detail.body.Text.match(/https?:\/\/[^\s<>"']+\/verify\?[^\s<>"']+/)?.[0];
  assert.ok(link, 'confirmation link missing from test email');
  const response = await fetch(link, { redirect: 'manual', headers: apiKey ? { apikey: apiKey } : {} });
  assert.ok(response.status >= 200 && response.status < 400,
    `email confirmation failed: HTTP ${response.status}`);
}

async function api(path, token, method = 'GET', data) {
  const headers = { Accept: 'application/json', ...(apiKey ? { apikey: apiKey } : {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (data !== undefined) headers['Content-Type'] = 'application/json';
  if (method === 'POST' || method === 'PATCH') headers.Prefer = 'return=representation';
  return request(`${restUrl}/${path}`, {
    method, headers, ...(data !== undefined ? { body: JSON.stringify(data) } : {}),
  });
}

function ok(result, label) {
  assert.ok(result.status >= 200 && result.status < 300,
    `${label}: HTTP ${result.status} ${JSON.stringify(result.body)}`);
  return result.body;
}

const alice = await signUp('alice');
const bob = await signUp('bob');
const publicWorkout = ok(await api('workouts', alice.token, 'POST', {
  title: 'Public API test', category: 'Easy', visibility: 'PUBLIC', estimated_minutes: 30,
}), 'create public workout')[0];
const privateWorkout = ok(await api('workouts', alice.token, 'POST', {
  title: 'Private API test', category: 'Easy', visibility: 'PRIVATE', estimated_minutes: 30,
}), 'create private workout')[0];
assert.equal(publicWorkout.creator_id, alice.id);
assert.equal(privateWorkout.creator_id, alice.id);

const selectCreated = `workouts?id=in.(${publicWorkout.id},${privateWorkout.id})&select=id`;
const aliceRows = ok(await api(selectCreated, alice.token), 'Alice select');
assert.equal(aliceRows.length, 2);
const bobRows = ok(await api(selectCreated, bob.token), 'Bob select');
assert.deepEqual(bobRows.map(row => row.id), [publicWorkout.id]);
const anonymousRows = ok(await api(selectCreated, null), 'anonymous select');
assert.deepEqual(anonymousRows.map(row => row.id), [publicWorkout.id]);

const deniedUpdate = await api(`workouts?id=eq.${publicWorkout.id}`, bob.token, 'PATCH', { title: 'Stolen' });
assert.ok(deniedUpdate.status === 403 || (deniedUpdate.status === 200 && deniedUpdate.body.length === 0),
  `cross-owner update: HTTP ${deniedUpdate.status} ${JSON.stringify(deniedUpdate.body)}`);
const deniedInsert = await api('workouts', bob.token, 'POST', {
  creator_id: alice.id, title: 'Stolen', category: 'Easy', estimated_minutes: 30,
});
assert.ok(deniedInsert.status === 403 || deniedInsert.status === 401,
  `cross-owner insert: HTTP ${deniedInsert.status} ${JSON.stringify(deniedInsert.body)}`);

const section = ok(await api('workout_sections', alice.token, 'POST', {
  workout_id: publicWorkout.id, position: 0, name: 'Warmup',
}), 'create section')[0];
assert.equal(ok(await api(`workout_sections?workout_id=eq.${publicWorkout.id}`, null), 'anonymous section').length, 1);
const deniedSection = await api('workout_sections', bob.token, 'POST', {
  workout_id: publicWorkout.id, position: 1, name: 'Stolen',
});
assert.ok(deniedSection.status === 403 || deniedSection.status === 401);
assert.ok(section.id);

console.log('Live Auth + PostgREST checks passed: two logins, own/public/private visibility, and cross-owner writes.');
