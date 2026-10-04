// Turns GitHub's "latest release" answer into what the download button shows.
// No DOM access, never throws: anything unexpected gives null, and the page keeps its fallback.

export const FALLBACK_LINE = "Latest version on GitHub · Android 8.0 or newer";

/** @returns {{ version: string, url: string, sizeLabel: string } | null} */
export function pickApk(release) {
  if (release === null || typeof release !== "object" || !Array.isArray(release.assets)) return null;

  const tag = release.tag_name;
  if (typeof tag !== "string") return null;
  const version = tag.replace(/^[vV]/, "");
  if (version === "") return null;

  const apk = release.assets.find(
    (a) => a !== null && typeof a === "object" &&
      typeof a.name === "string" && a.name.toLowerCase().endsWith(".apk") &&
      typeof a.browser_download_url === "string" && a.browser_download_url.startsWith("https://github.com/")
  );
  if (!apk) return null;

  const size = apk.size;
  if (typeof size !== "number" || !Number.isFinite(size) || size <= 0) return null;

  // Decimal megabytes with one decimal place, the way Android shows file sizes
  const sizeLabel = Math.max(size / 1e6, 0.1).toFixed(1);
  return { version, url: apk.browser_download_url, sizeLabel };
}

export function releaseLine(result) {
  return result ? `Version ${result.version} · ${result.sizeLabel} MB · Android 8.0 or newer` : FALLBACK_LINE;
}
