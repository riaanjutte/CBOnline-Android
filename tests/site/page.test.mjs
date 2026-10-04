import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { FALLBACK_LINE } from "../../site/release.js";

const siteDir = new URL("../../site/", import.meta.url);
const read = (name) => readFileSync(new URL(name, siteDir), "utf8");
const html = read("index.html");
const css = () => read("style.css");

// Visible text of an HTML fragment: tags dropped, the few entities we use decoded, whitespace collapsed
const text = (fragment) =>
  fragment
    .replace(/<[^>]+>/g, "")
    .replace(/&#39;|&rsquo;|’/g, "'")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&nbsp;/g, " ")
    .replace(/\s+/g, " ")
    .trim();
const tagWithId = (id) => html.match(new RegExp(`<([a-z0-9]+)\\b[^>]*\\bid="${id}"[^>]*>([\\s\\S]*?)</\\1>`));
const sectionById = (id) => html.match(new RegExp(`<section\\b[^>]*\\bid="${id}"[^>]*>[\\s\\S]*?</section>`))?.[0];
const isExternal = (ref) => /^(https?:|mailto:|#)/.test(ref);

test("every local src/href exists and is relative", () => {
  const refs = [...html.matchAll(/\b(?:src|href)="([^"]+)"/g)].map((m) => m[1]);
  const cssRefs = [...css().matchAll(/url\(\s*["']?([^"')]+)["']?\s*\)/g)].map((m) => m[1]);
  for (const ref of [...refs, ...cssRefs].filter((r) => !isExternal(r) && !r.startsWith("data:"))) {
    assert.ok(!ref.startsWith("/"), `absolute path: ${ref}`);
    const path = ref.split(/[?#]/)[0];
    assert.ok(existsSync(fileURLToPath(new URL(path, siteDir))), `missing file: ${ref}`);
  }
});

test("no third-party scripts, styles or fonts", () => {
  for (const tag of html.match(/<script\b[^>]*>/g) ?? []) assert.doesNotMatch(tag, /\bsrc="(https?:)?\/\//);
  for (const tag of html.match(/<link\b[^>]*>/g) ?? []) {
    if (/rel="(stylesheet|preload)"/.test(tag)) assert.doesNotMatch(tag, /\bhref="(https?:)?\/\//);
  }
  assert.doesNotMatch(css(), /@import/);
  assert.doesNotMatch(css(), /url\(\s*["']?(https?:)?\/\//);
});

test("one h1 with the headline", () => {
  const h1s = [...html.matchAll(/<h1\b[^>]*>([\s\S]*?)<\/h1>/g)];
  assert.equal(h1s.length, 1);
  assert.equal(text(h1s[0][1]), "Who's flying on Combat Box?");
});

test("sections and nav", () => {
  for (const id of ["top", "features", "install", "faq"]) assert.match(html, new RegExp(`\\bid="${id}"`), `missing #${id}`);
  const nav = html.match(/<nav\b[\s\S]*?<\/nav>/)?.[0] ?? "";
  for (const id of ["features", "install", "faq"]) assert.match(nav, new RegExp(`href="#${id}"`), `nav lacks #${id}`);
  const levels = [...html.matchAll(/<h([1-6])\b/g)].map((m) => Number(m[1]));
  const firstH2 = levels.indexOf(2);
  assert.ok(firstH2 > 0, "no h2");
  assert.ok(levels.slice(0, firstH2).every((l) => l === 1), "h3+ before the first h2");
  const sections = html.match(/<section\b[\s\S]*?<\/section>/g) ?? [];
  assert.ok(sections.length >= 5, `only ${sections.length} sections`);
  for (const s of sections) assert.match(s, /<h2\b/, `section without h2: ${s.slice(0, 60)}`);
});

test("download fallback", () => {
  const button = html.match(/<a\b[^>]*\bid="download"[^>]*>[\s\S]*?<\/a>/)?.[0];
  assert.ok(button, "no #download link");
  assert.match(button, /href="https:\/\/github\.com\/riaanjutte\/CBOnline-Android\/releases\/latest"/);
  assert.match(text(button), /Download for Android/);
  assert.equal(text(tagWithId("release-line")?.[2] ?? ""), FALLBACK_LINE);
  const scripts = html.match(/<script\b[^>]*>/g) ?? [];
  assert.ok(scripts.some((s) => /type="module"/.test(s) && /src="main\.js"/.test(s)), "main.js not loaded as a module");
});

test("every img has alt text", () => {
  const imgs = html.match(/<img\b[^>]*>/g) ?? [];
  assert.ok(imgs.length >= 4, `only ${imgs.length} images`);
  for (const img of imgs) assert.match(img, /\balt="[^"]+"/, `no alt: ${img}`);
});

test("faq and verification are collapsible", () => {
  const faq = sectionById("faq") ?? "";
  assert.equal((faq.match(/<details\b/g) ?? []).length, 7);
  const install = sectionById("install") ?? "";
  const details = install.match(/<details\b[\s\S]*?<\/details>/g) ?? [];
  assert.equal(details.length, 1);
  assert.equal(text(details[0].match(/<summary\b[^>]*>([\s\S]*?)<\/summary>/)?.[1] ?? ""), "If Android says the developer is unverified");
});

test("head basics", () => {
  assert.match(html, /<html\b[^>]*\blang="en"/);
  assert.equal(text(html.match(/<title>([\s\S]*?)<\/title>/)?.[1] ?? ""), "CB Online: who's flying on Combat Box");
  assert.match(html, /<meta\s+name="description"\s+content="[^"]{20,}"/);
  assert.match(html, /<meta\s+name="viewport"\s+content="width=device-width, initial-scale=1"/);
  assert.match(html, /<meta\s+name="theme-color"\s+content="#161817"/);
  for (const icon of ["favicon.ico", "favicon-16x16.png", "favicon-32x32.png", "apple-touch-icon.png"]) {
    assert.match(html, new RegExp(`<link\\b[^>]*href="assets/${icon.replace(".", "\\.")}"`), `no link to ${icon}`);
  }
});
