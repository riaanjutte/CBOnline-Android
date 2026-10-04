// Points the download button straight at the latest APK. The HTML already links to the latest
// release page, so on any failure this changes nothing and stays quiet.
import { pickApk, releaseLine } from "./release.js";

export const RELEASE_API = "https://api.github.com/repos/riaanjutte/CBOnline-Android/releases/latest";

export async function wireDownload(doc = document, fetchImpl = (url, init) => fetch(url, init), timeoutMs = 5000) {
  const button = doc.getElementById("download");
  const line = doc.getElementById("release-line");
  if (!button || !line) return false;

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetchImpl(RELEASE_API, {
      headers: { Accept: "application/vnd.github+json" },
      signal: controller.signal,
    });
    if (!response.ok) return false;
    const result = pickApk(await response.json());
    if (!result) return false;
    button.href = result.url;
    line.textContent = releaseLine(result);
    return true;
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

if (typeof document !== "undefined") wireDownload();
