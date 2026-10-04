import test from "node:test";
import assert from "node:assert/strict";
import { FALLBACK_LINE, pickApk, releaseLine } from "../../site/release.js";

const APK_URL = "https://github.com/riaanjutte/CBOnline-Android/releases/download/v1.0.0/CBOnline-1.0.0.apk";

const rel = (overrides = {}) => ({
  tag_name: "v1.0.0",
  assets: [{ name: "CBOnline-1.0.0.apk", size: 2321394, browser_download_url: APK_URL }],
  ...overrides,
});

test("normal release", () => assert.deepEqual(pickApk(rel()), { version: "1.0.0", url: APK_URL, sizeLabel: "2.3" }));

test("tag without v", () => assert.equal(pickApk(rel({ tag_name: "1.2.3" })).version, "1.2.3"));

test("tag with capital V", () => assert.equal(pickApk(rel({ tag_name: "V2.0.0" })).version, "2.0.0"));

test("missing or empty tag", () => {
  assert.equal(pickApk(rel({ tag_name: undefined })), null);
  assert.equal(pickApk(rel({ tag_name: "" })), null);
});

test("no apk asset", () =>
  assert.equal(pickApk(rel({ assets: [{ name: "notes.txt", size: 10, browser_download_url: "u" }] })), null));

test("empty or missing assets", () => {
  assert.equal(pickApk(rel({ assets: [] })), null);
  assert.equal(pickApk({ tag_name: "v1.0.0" }), null);
});

test("apk not first, capitals accepted", () =>
  assert.equal(
    pickApk(rel({ assets: [
      { name: "a.txt", size: 1, browser_download_url: "x" },
      { name: "App.APK", size: 1000000, browser_download_url: "y" },
    ] })).url,
    "y"
  ));

test("apk without download url is skipped", () =>
  assert.equal(pickApk(rel({ assets: [{ name: "a.apk", size: 1 }] })), null));

test("size labels", () => {
  const size = (n) => pickApk(rel({ assets: [{ name: "a.apk", size: n, browser_download_url: "u" }] }));
  assert.equal(size(2321394).sizeLabel, "2.3");
  assert.equal(size(1000000).sizeLabel, "1.0");
  assert.equal(size(40000).sizeLabel, "0.1");
  assert.equal(size(0), null);
  assert.equal(size(undefined), null);
  assert.equal(size("2321394"), null);
});

test("not a release", () => {
  for (const v of [null, undefined, "x", 42, []]) assert.equal(pickApk(v), null);
});

test("release line", () => {
  assert.equal(releaseLine({ version: "1.0.0", url: "u", sizeLabel: "2.3" }), "Version 1.0.0 · 2.3 MB · Android 8.0 or newer");
  assert.equal(releaseLine(null), "Latest version on GitHub · Android 8.0 or newer");
  assert.equal(FALLBACK_LINE, releaseLine(null));
});
