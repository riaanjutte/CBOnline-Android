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

test("full-width banner header with the nav under it", () => {
  const banner = html.match(/<header\b[^>]*\bclass="banner"[^>]*>[\s\S]*?<\/header>/)?.[0];
  assert.ok(banner, "no banner header");
  assert.match(banner, /<img\b[^>]*\bsrc="assets\/wordmark\.webp"[^>]*\balt="Combat Box"/);
  assert.doesNotMatch(banner, /<nav\b/, "nav belongs under the banner, not in it");
  assert.match(html.match(/<nav\b[^>]*>/)?.[0] ?? "", /\bclass="navbar"/);
  assert.ok(html.indexOf(banner) < html.indexOf("<nav"), "nav comes after the banner");
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

const meta = (key) =>
  html.match(new RegExp(`<meta\\s+(?:property|name)="${key.replace(/[:.]/g, "\\$&")}"\\s+content="([^"]*)"`))?.[1];

// Width and height from a JPEG's start-of-frame marker
const jpegSize = (buf) => {
  let i = 2;
  while (i < buf.length) {
    if (buf[i] !== 0xff) throw new Error(`bad marker at ${i}`);
    const marker = buf[i + 1];
    if (marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker)) {
      return { height: buf.readUInt16BE(i + 5), width: buf.readUInt16BE(i + 7) };
    }
    i += 2 + buf.readUInt16BE(i + 2);
  }
  throw new Error("no SOF marker");
};

test("link preview tags", () => {
  const base = "https://riaanjutte.github.io/CBOnline-Android/";
  assert.equal(meta("og:type"), "website");
  assert.equal(meta("og:title"), "CB Online: who's flying on Combat Box");
  assert.ok((meta("og:description") ?? "").length >= 20, "og:description");
  assert.equal(meta("og:url"), base);
  assert.equal(meta("og:image"), base + "assets/social-card.jpg");
  assert.equal(meta("og:image:width"), "1200");
  assert.equal(meta("og:image:height"), "630");
  assert.ok((meta("og:image:alt") ?? "").length > 0, "og:image:alt");
  assert.equal(meta("twitter:card"), "summary_large_image");
});

test("social card is 1200x630 and under 300 KB", () => {
  const file = new URL("assets/social-card.jpg", siteDir);
  assert.ok(existsSync(fileURLToPath(file)), "no social-card.jpg");
  const buf = readFileSync(file);
  assert.deepEqual(jpegSize(buf), { width: 1200, height: 630 });
  assert.ok(buf.length < 300_000, `${buf.length} bytes`);
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

// Guards from the final review

test("layout scales with the reader's text size", () => {
  const c = css();
  assert.doesNotMatch(c, /@media[^{]*\d+px/, "px breakpoints ignore the browser's font size");
  const body = c.match(/(?:^|\n)body\s*\{[^}]*\}/)?.[0] ?? "";
  assert.ok(body, "no body rule");
  assert.doesNotMatch(body, /overflow-wrap:\s*anywhere/, "anywhere on body lets columns shrink to one letter");
});

test("review-focus safeguards stay in the CSS", () => {
  const c = css();
  assert.match(c, /\[id\]\s*\{[^}]*scroll-margin-top/, "anchor targets must clear the sticky bar");
  assert.match(c.match(/\n\.hero\s*\{[^}]*\}/)?.[0] ?? "", /background-color:/, "hero needs a plain colour under the map");
  const smooth = [...c.matchAll(/scroll-behavior:\s*smooth/g)];
  assert.equal(smooth.length, 1);
  assert.match(c, /@media\s*\(prefers-reduced-motion:\s*no-preference\)\s*\{\s*html\s*\{\s*scroll-behavior:\s*smooth/);
});

test("FAQ markers are silent for screen readers", () => {
  const rules = css().match(/summary::after\s*\{[^}]*\}/g) ?? [];
  assert.equal(rules.length, 2, "open and closed markers");
  for (const r of rules) assert.match(r, /content:\s*"[^"]*"\s*\/\s*""/, `marker without empty alt text: ${r}`);
});

test("FAQ answers start closed", () => {
  for (const tag of html.match(/<details\b[^>]*>/g) ?? []) assert.doesNotMatch(tag, /\bopen\b/, tag);
});

test("scripts only talk to GitHub", () => {
  for (const name of ["main.js", "release.js"]) {
    const js = read(name);
    for (const m of js.matchAll(/\bimport\b[^"'`]*["'`]([^"'`]+)["'`]/g)) assert.match(m[1], /^\.\//, `${name} imports ${m[1]}`);
    assert.doesNotMatch(js, /\bimport\s*\(/, `${name} uses a dynamic import`);
    for (const m of js.matchAll(/https?:\/\/([^/"'`\s]+)/g)) {
      assert.ok(["api.github.com", "github.com"].includes(m[1]), `${name} talks to ${m[1]}`);
    }
  }
});

test("README and site FAQ say the same", () => {
  const readme = readFileSync(new URL("../../README.md", import.meta.url), "utf8").replace(/\r/g, "");
  const section = readme.split("\n## Questions\n")[1]?.split("\n## ")[0] ?? "";
  const fromReadme = [...section.matchAll(/\*\*(.+?)\*\*\n(.+)/g)].map((m) => [m[1], m[2].replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")]);
  const fromSite = [...(sectionById("faq") ?? "").matchAll(/<summary\b[^>]*>([\s\S]*?)<\/summary>\s*<div class="details-body">([\s\S]*?)<\/div>/g)]
    .map((m) => [text(m[1]), text(m[2])]);
  assert.equal(fromReadme.length, 7);
  assert.deepEqual(fromSite, fromReadme);
});
