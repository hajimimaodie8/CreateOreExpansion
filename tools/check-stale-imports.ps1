# Find imports of our own types that no longer exist anywhere in the source tree.
# WHY: javac resolves imports LAZILY -- an import of a deleted class that is not otherwise
# referenced compiles silently. It then breaks the moment anyone mentions the type, and it
# makes "did we really delete that class?" unknowable by reading the file.
# Pure ASCII. Usage: check-stale-imports.ps1 [-Repo <path>]
param([string]$Repo = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$roots = @('src\main\java', 'core\src\main\java', 'coe\src\main\java', 'cews\src\main\java', 'transmutation\src\main\java')
$prefix = 'com.hjmmd_8.createoreexpansion'

$files = @()
foreach ($r in $roots) {
    $dir = Join-Path $Repo $r
    if (Test-Path $dir) { $files += Get-ChildItem $dir -Recurse -File -Filter *.java -ErrorAction SilentlyContinue }
}

# index every declared top-level/inner type name by simple name
$declared = @{}
foreach ($f in $files) {
    foreach ($l in (Get-Content $f.FullName -Encoding UTF8)) {
        if ($l -match '^\s*(public\s+|final\s+|abstract\s+|sealed\s+|non-sealed\s+|static\s+)*(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)') {
            $declared[$Matches[3]] = $true
        }
    }
}

$stale = @()
foreach ($f in $files) {
    $n = 0
    foreach ($l in (Get-Content $f.FullName -Encoding UTF8)) {
        $n++
        if ($l -match "^\s*import\s+$([regex]::Escape($prefix))\.[\w\.]*\.([A-Za-z_][A-Za-z0-9_]*);") {
            $simple = $Matches[1]
            if (-not $declared.ContainsKey($simple)) {
                $stale += ("{0}:{1}: {2}" -f $f.FullName.Replace("$Repo\", ''), $n, $l.Trim())
            }
        }
    }
}

Write-Output ("java files scanned: {0}   declared type names: {1}" -f $files.Count, $declared.Count)
if ($declared.Count -lt 200) {
    Write-Output "FAIL(anti-vacuum): only $($declared.Count) declared types indexed; expected >= 200 -- scan is broken."
    exit 1
}
if ($stale.Count -eq 0) {
    Write-Output "OK: no stale import of a deleted own-package type."
    exit 0
}
Write-Output "FAIL: $($stale.Count) stale import(s) of types that no longer exist:"
$stale | ForEach-Object { Write-Output "  $_" }
exit 1
