import test from "node:test";
import assert from "node:assert/strict";
import { RELEASE_API, wireDownload } from "../../site/main.js";
import { FALLBACK_LINE } from "../../site/release.js";

const FALLBACK_URL = "https://github.com/riaanjutte/CBOnline-Android/releases/latest";
const APK_URL = "https://github.com/riaanjutte/CBOnline-Android/releases/download/v1.0.0/CBOnline-1.0.0.apk";
const release = {
  tag_name: "v1.0.0",
  assets: [{ name: "CBOnline-1.0.0.apk", size: 2321394, browser_download_url: APK_URL }],
};

const fakeDoc = () => ({
  els: { download: { href: FALLBACK_URL }, "release-line": { textContent: FALLBACK_LINE } },
  getElementById(id) { return this.els[id] ?? null; },
});
const ok = (json) => async () => ({ ok: true, json: async () => json });
const assertFallback = (d) => {
  assert.equal(d.els.download.href, FALLBACK_URL);
  assert.equal(d.els["release-line"].textContent, FALLBACK_LINE);
};

test("success updates link and line", async () => {
  const d = fakeDoc();
  assert.equal(await wireDownload(d, ok(release), 50), true);
  assert.equal(d.els.download.href, APK_URL);
  assert.equal(d.els["release-line"].textContent, "Version 1.0.0 · 2.3 MB · Android 8.0 or newer");
});

test("sends the GitHub accept header to the release API", async () => {
  const calls = [];
  await wireDownload(fakeDoc(), async (url, init) => { calls.push([url, init]); return { ok: true, json: async () => release }; }, 50);
  assert.equal(calls.length, 1);
  const [url, init] = calls[0];
  assert.equal(url, RELEASE_API);
  assert.equal(RELEASE_API, "https://api.github.com/repos/riaanjutte/CBOnline-Android/releases/latest");
  assert.equal(init.headers.Accept, "application/vnd.github+json");
  assert.ok(init.signal instanceof AbortSignal);
});

for (const [name, impl] of [
  ["network error", async () => { throw new TypeError("offline"); }],
  ["rate limited", async () => ({ ok: false, status: 403, json: async () => ({}) })],
  ["bad json", async () => ({ ok: true, json: async () => { throw new SyntaxError("x"); } })],
  ["no apk", ok({ tag_name: "v1.0.0", assets: [] })],
]) {
  test(`${name} keeps the fallback`, async () => {
    const d = fakeDoc();
    assert.equal(await wireDownload(d, impl, 50), false);
    assertFallback(d);
  });
}

test("timeout keeps the fallback", async () => {
  const hang = (url, init) =>
    new Promise((_, reject) => init.signal.addEventListener("abort", () => reject(init.signal.reason)));
  const d = fakeDoc();
  assert.equal(await wireDownload(d, hang, 20), false);
  assertFallback(d);
});

test("missing elements resolve false without fetching", async () => {
  let called = false;
  const empty = { getElementById: () => null };
  assert.equal(await wireDownload(empty, async () => { called = true; return { ok: true, json: async () => release }; }, 50), false);
  assert.equal(called, false);
});
