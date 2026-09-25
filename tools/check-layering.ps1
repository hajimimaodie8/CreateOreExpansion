# check-layering.ps1 -- P2b architecture direction assertion for createoreexpansion.
#
# Verifies that the future module split stays acyclic:
#   CEWS  (Create: Energy Wave Studies)  is allowed to depend on COE (the material line)
#   TRANS (mechanical transmutation)     is allowed to depend on COE
#   COE   must NOT depend on CEWS or TRANS      (no reverse dependency)
#   TRANS must NOT depend on CEWS
# Anything may depend on SHARED; SHARED is never judged as a source.
#
# Layer of a FILE is decided purely by its path (see Get-FileLayer).
# Layer of an IMPORT / fully-qualified reference is decided by its package (see Get-TargetLayer).
#
# Exit code: 0 = clean, 1 = violations found.
#
# NOTE: this file is deliberately pure ASCII.  Windows PowerShell 5.1 reads a BOM-less
# .ps1 as ANSI, which mangles non-ASCII text and can even swallow quotes mid-script.
# Whitelist reasons are therefore written in English.

param(
    [switch]$NoFqn      # skip the fully-qualified-reference pass (imports only)
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$pkgRoot  = Join-Path $repoRoot 'src\main\java\com\hjmmd_8\createoreexpansion'
$prefix   = 'com.hjmmd_8.createoreexpansion.'

if (-not (Test-Path $pkgRoot)) { throw "package root not found: $pkgRoot" }

# ---------------------------------------------------------------------------
# WHITELIST -- accepted, known cross-layer dependencies.
#
# Key   : path relative to the package root, optionally with ":<line>".
# Value : one-line reason (English, ASCII).
#
# KEEP THIS SHORT.  An entry here means "this forbidden edge is on purpose";
# it does not mean the edge is good, only that it is accepted for now.
# ---------------------------------------------------------------------------
$whitelist = [ordered]@{
    # (empty -- no accepted cross-layer edge at the time of writing)
}

function Get-FileLayer {
    param([string]$rel)
    $r = $rel -replace '\\', '/'
    # explicit module paths -- must be tested BEFORE the generic "common/" SHARED rule
    if ($r -match '^common/registry/coe/')            { return 'COE' }
    if ($r -match '^common/registry/cews/')           { return 'CEWS' }
    if ($r -match '^common/registry/transmutation/')  { return 'TRANS' }
    if ($r -match '^content/(charger|wave|machine|energyfield)/') { return 'CEWS' }
    if ($r -match '^content/(transmuting|transmutation)/')        { return 'TRANS' }
    # SHARED: infrastructure, never judged as a source layer
    if ($r -notmatch '/') { return 'SHARED' }                       # mod root package
    if ($r -match '^(common|util|foundation|compat|client|data|mixin|integration)/') { return 'SHARED' }
    # everything else under content/ is the mineral line
    return 'COE'
}

function Get-TargetLayer {
    param([string]$fqn)
    if ($fqn -notlike "$prefix*") { return 'SHARED' }               # not ours: ignore
    $rest = $fqn.Substring($prefix.Length)
    if ($rest -match '^common\.registry\.coe\.')            { return 'COE' }
    if ($rest -match '^common\.registry\.cews\.')           { return 'CEWS' }
    if ($rest -match '^common\.registry\.transmutation\.')  { return 'TRANS' }
    if ($rest -match '^content\.(charger|wave|machine|energyfield)\.') { return 'CEWS' }
    if ($rest -match '^content\.(transmuting|transmutation)\.')        { return 'TRANS' }
    if ($rest -match '^(common|util|foundation|compat|client|data|mixin|integration)\.') { return 'SHARED' }
    return 'COE'
}

function Test-Forbidden {
    param([string]$from, [string]$to)
    if ($from -eq 'SHARED' -or $to -eq 'SHARED') { return $false }
    if ($from -eq $to) { return $false }
    if ($from -eq 'COE'   -and ($to -eq 'CEWS' -or $to -eq 'TRANS')) { return $true }
    if ($from -eq 'TRANS' -and $to -eq 'CEWS') { return $true }
    return $false
}

# body text with comments stripped -- used only to flag dead imports
function Get-CodeOnly {
    param([string[]]$lines)
    return (($lines | Where-Object { $_.TrimStart() -notmatch '^(\*|//|/\*)' -and $_ -notmatch '^\s*import\s' }) -join "`n")
}

$violations = New-Object System.Collections.ArrayList
$stats = @{ 'COE' = 0; 'CEWS' = 0; 'TRANS' = 0; 'SHARED' = 0 }
$edgeStats = @{}

$files = Get-ChildItem -Recurse -Path $pkgRoot -Filter *.java | Sort-Object FullName
foreach ($f in $files) {
    $rel = $f.FullName.Substring($pkgRoot.Length + 1)
    $from = Get-FileLayer $rel
    $stats[$from]++
    if ($from -eq 'SHARED') { continue }        # SHARED never judged as a source

    $lines = [System.IO.File]::ReadAllLines($f.FullName, [System.Text.Encoding]::UTF8)
    $code = Get-CodeOnly $lines

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $lineNo = $i + 1
        $isImport = $false
        $targets = New-Object System.Collections.ArrayList

        # ---- pass 1: import declarations
        $m = [regex]::Match($line, '^\s*import\s+(static\s+)?(com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+)\s*;')
        if ($m.Success) {
            $isImport = $true
            [void]$targets.Add($m.Groups[2].Value)
        }

        # ---- pass 2: fully-qualified references in code (skip comments and imports)
        if (-not $NoFqn -and -not $isImport) {
            $trim = $line.TrimStart()
            if ($trim -notmatch '^(\*|//|/\*)') {
                foreach ($mm in [regex]::Matches($line, 'com\.hjmmd_8\.createoreexpansion\.[A-Za-z0-9_.]+')) {
                    [void]$targets.Add($mm.Value)
                }
            }
        }

        foreach ($fqn in $targets) {
            $to = Get-TargetLayer $fqn
            if (-not (Test-Forbidden $from $to)) { continue }
            $key = "$rel`:$lineNo"
            $wlReason = $null
            if ($whitelist.Contains($key)) { $wlReason = $whitelist[$key] }
            elseif ($whitelist.Contains($rel)) { $wlReason = $whitelist[$rel] }
            if ($wlReason) { continue }

            $tag = ''
            if (-not $isImport) { $tag = ' [fully-qualified, not an import]' }
            $simple = ($fqn -split '\.')[-1]
            if ($isImport -and $code -notmatch ('(?<![\w.])' + [regex]::Escape($simple) + '(?![\w])')) {
                $tag = ' [dead import: simple name never used in code]'
            }
            $edge = "$from -> $to"
            if (-not $edgeStats.ContainsKey($edge)) { $edgeStats[$edge] = 0 }
            $edgeStats[$edge]++
            [void]$violations.Add(("{0}:{1} -> {2}{3}   ({4})" -f $rel, $lineNo, $to, $tag, $fqn))
        }
    }
}

Write-Host 'layering check: src/main/java/com/hjmmd_8/createoreexpansion'
Write-Host ('  files by layer : COE={0}  CEWS={1}  TRANS={2}  SHARED={3}' -f $stats['COE'], $stats['CEWS'], $stats['TRANS'], $stats['SHARED'])
Write-Host '  forbidden edge directions checked : COE->CEWS, COE->TRANS, TRANS->CEWS'
if ($NoFqn) { Write-Host '  (fully-qualified-reference pass disabled by -NoFqn)' }

if ($violations.Count -eq 0) {
    Write-Host 'OK: no forbidden cross-layer dependency found.'
    exit 0
}

Write-Host ''
Write-Host ('VIOLATIONS ({0}):' -f $violations.Count)
foreach ($v in ($violations | Sort-Object)) { Write-Host ('  ' + $v) }
Write-Host ''
Write-Host 'by direction:'
foreach ($k in ($edgeStats.Keys | Sort-Object)) { Write-Host ('  {0} : {1}' -f $k, $edgeStats[$k]) }
if ($whitelist.Count -eq 0) { Write-Host 'whitelist: (empty)' }
else { Write-Host ('whitelist entries: ' + $whitelist.Count) }
exit 1
