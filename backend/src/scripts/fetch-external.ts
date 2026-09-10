/**
 * Standalone helper: prints how many domains the external source contains.
 * The real merge runs in the backend via the daily cron
 * (BlocklistUpdateTask.updateFromExternalSource).
 * Usage: npm run sync:external
 */
async function main() {
  const source = process.env.EXTERNAL_BLOCKLIST_SOURCE;
  if (!source) throw new Error('EXTERNAL_BLOCKLIST_SOURCE not set');
  const res = await fetch(source);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const text = await res.text();
  const domains = text
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => !l.startsWith('#') && l.length > 0)
    .map((l) => l.split(/\s+/)[1])
    .filter((d): d is string => Boolean(d && d.includes('.')));
  console.log(JSON.stringify({ source, domains: domains.length }));
}
main().catch((e) => {
  console.error(e);
  process.exit(1);
});
