const fs = require('node:fs');
const path = require('node:path');

const repoRoot = path.resolve(__dirname, '..');
const javaRoot = path.join(repoRoot, 'Apex-Gestordemo', 'src', 'main', 'java');
const forbiddenPatterns = [
  { label: '@Query', regex: /@Query\s*\(/ },
  { label: 'nativeQuery', regex: /nativeQuery\s*=/ },
  { label: 'EntityManager', regex: /\bEntityManager\b/ },
  { label: 'createQuery', regex: /\bcreate(Query|NativeQuery)\s*\(/ },
  { label: 'JdbcTemplate', regex: /\bJdbcTemplate\b/ }
];

function listJavaFiles(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return listJavaFiles(fullPath);
    }
    return entry.isFile() && entry.name.endsWith('.java') ? [fullPath] : [];
  });
}

const findings = [];

for (const file of listJavaFiles(javaRoot)) {
  const source = fs.readFileSync(file, 'utf8');
  const lines = source.split(/\r?\n/);
  lines.forEach((line, index) => {
    for (const pattern of forbiddenPatterns) {
      if (pattern.regex.test(line)) {
        findings.push({
          file: path.relative(repoRoot, file),
          line: index + 1,
          pattern: pattern.label,
          text: line.trim()
        });
      }
    }
  });
}

if (findings.length > 0) {
  console.error('Spring Data JPA audit failed. Prefer derived repository methods before manual queries.');
  for (const finding of findings) {
    console.error(`${finding.file}:${finding.line} [${finding.pattern}] ${finding.text}`);
  }
  process.exit(1);
}

console.log('Spring Data JPA audit passed: no manual query patterns found.');
